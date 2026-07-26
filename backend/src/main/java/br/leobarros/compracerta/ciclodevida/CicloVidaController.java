package br.leobarros.compracerta.ciclodevida;

import java.util.UUID;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CicloVidaController {
	private static final String ROOT = "/api/v1/lists/{listId}";
	private final CicloVidaService service;
	private final SessaoService sessions;
	public CicloVidaController(CicloVidaService service, SessaoService sessions) {
		this.service = service;
		this.sessions = sessions;
	}
	@PutMapping(ROOT + "/status")
	public ResponseEntity<ListDetail> change(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId,
			@RequestBody(required = false) ChangeStatus input) {
		sessions.validarCsrf(token, csrf);
		var result = service.change(
				sessions.obterContaAutenticada(token), listId,
				input == null ? null : input.status(), ApiSupport.version(etag), key);
		return ResponseEntity.ok().eTag(ApiSupport.etag(result.version())).body(result);
	}
	@DeleteMapping(ROOT)
	public ResponseEntity<Void> delete(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId) {
		sessions.validarCsrf(token, csrf);
		service.delete(
				sessions.obterContaAutenticada(token), listId, ApiSupport.version(etag), key);
		return ResponseEntity.noContent().build();
	}
	public record ChangeStatus(String status) {
	}
}
