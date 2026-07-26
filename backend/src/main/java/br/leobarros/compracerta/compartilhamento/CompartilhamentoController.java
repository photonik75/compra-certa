package br.leobarros.compracerta.compartilhamento;

import java.util.UUID;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.AcceptResult;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.EmailInput;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Invitation;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.ListAccess;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Preview;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.ShareResult;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.TokenInput;
import br.leobarros.compracerta.comum.ApiSupport;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompartilhamentoController {
	private static final String LIST = "/api/v1/lists/{listId}";
	private final CompartilhamentoService service;
	private final SessaoService sessions;
	CompartilhamentoController(CompartilhamentoService service, SessaoService sessions) {
		this.service = service;
		this.sessions = sessions;
	}
	@GetMapping(LIST + "/access")
	ResponseEntity<ListAccess> access(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@PathVariable UUID listId) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(service.access(sessions.obterContaAutenticada(token), listId));
	}
	@PostMapping(LIST + "/invitations")
	ResponseEntity<ShareResult> invite(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@PathVariable UUID listId,
			@RequestBody(required = false) EmailInput input) {
		sessions.validarCsrf(token, csrf);
		return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.invite(
				sessions.obterContaAutenticada(token), listId, input == null ? null : input.email(), key));
	}
	@PostMapping(LIST + "/invitations/{invitationId}/resend")
	ResponseEntity<Invitation> resend(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId,
			@PathVariable UUID invitationId) {
		sessions.validarCsrf(token, csrf);
		var invitation = service.resend(
				sessions.obterContaAutenticada(token), listId, invitationId,
				ApiSupport.version(etag), key);
		return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
				.eTag(ApiSupport.etag(invitation.version())).body(invitation);
	}
	@DeleteMapping(LIST + "/invitations/{invitationId}")
	ResponseEntity<Void> cancel(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId,
			@PathVariable UUID invitationId) {
		sessions.validarCsrf(token, csrf);
		service.cancel(
				sessions.obterContaAutenticada(token), listId, invitationId,
				ApiSupport.version(etag), key);
		return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
	}
	@PostMapping("/api/v1/invitations/preview")
	ResponseEntity<Preview> preview(@RequestBody(required = false) TokenInput input) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(service.preview(input == null ? null : input.token()));
	}
	@PostMapping("/api/v1/invitations/accept")
	ResponseEntity<AcceptResult> accept(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestBody(required = false) TokenInput input) {
		sessions.validarCsrf(token, csrf);
		return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.accept(
				sessions.obterContaAutenticada(token), input == null ? null : input.token(), key));
	}
	@DeleteMapping(LIST + "/members/{userId}")
	ResponseEntity<Void> remove(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId,
			@PathVariable UUID userId) {
		sessions.validarCsrf(token, csrf);
		service.remove(
				sessions.obterContaAutenticada(token), listId, userId,
				ApiSupport.version(etag), key, false);
		return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
	}
	@DeleteMapping(LIST + "/members/me")
	ResponseEntity<Void> leave(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId) {
		sessions.validarCsrf(token, csrf);
		var account = sessions.obterContaAutenticada(token);
		service.remove(account, listId, account.getId(), ApiSupport.version(etag), key, true);
		return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
	}
}
