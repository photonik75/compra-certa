package br.leobarros.compracerta.itens;

import java.net.URI;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.itens.ItemDtos.CheckInput;
import br.leobarros.compracerta.itens.ItemDtos.CheckResult;
import br.leobarros.compracerta.itens.ItemDtos.Collection;
import br.leobarros.compracerta.itens.ItemDtos.Deletion;
import br.leobarros.compracerta.itens.ItemDtos.Input;
import br.leobarros.compracerta.itens.ItemDtos.ListItem;
import br.leobarros.compracerta.itens.ItemDtos.Mutation;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ItemController {
	private static final String ROOT = "/api/v1/lists/{listId}/items";
	private final ItemService service;
	private final SessaoService sessions;
	private final ListaEventService events;
	public ItemController(ItemService service, SessaoService sessions, ListaEventService events) {
		this.service = service;
		this.sessions = sessions;
		this.events = events;
	}
	@GetMapping(ROOT)
	public ResponseEntity<Collection> list(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@PathVariable UUID listId,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer limit) {
		return ResponseEntity.ok(service.list(sessions.obterContaAutenticada(token), listId, cursor, limit));
	}
	@PostMapping(ROOT)
	public ResponseEntity<Mutation> create(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@PathVariable UUID listId,
			@RequestBody(required = false) Input input) {
		sessions.validarCsrf(token, csrf);
		var result = service.create(sessions.obterContaAutenticada(token), listId, input, key);
		var status = "CREATED".equals(result.outcome()) ? 201 : 200;
		return ResponseEntity.status(status)
				.location(URI.create("/api/v1/lists/" + listId + "/items/" + result.item().id()))
				.eTag(ApiSupport.etag(result.item().version())).body(result);
	}
	@GetMapping(ROOT + "/{itemId}")
	public ResponseEntity<ListItem> get(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@PathVariable UUID listId,
			@PathVariable UUID itemId) {
		var item = service.get(sessions.obterContaAutenticada(token), listId, itemId);
		return ResponseEntity.ok().eTag(ApiSupport.etag(item.version())).body(item);
	}
	@PatchMapping(ROOT + "/{itemId}")
	public ResponseEntity<Mutation> update(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId,
			@PathVariable UUID itemId,
			@RequestBody(required = false) Input input) {
		sessions.validarCsrf(token, csrf);
		var result = service.update(
				sessions.obterContaAutenticada(token), listId, itemId, input, ApiSupport.version(etag), key);
		return ResponseEntity.ok().eTag(ApiSupport.etag(result.item().version())).body(result);
	}
	@DeleteMapping(ROOT + "/{itemId}")
	public ResponseEntity<Deletion> delete(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId,
			@PathVariable UUID itemId) {
		sessions.validarCsrf(token, csrf);
		return ResponseEntity.ok(service.delete(
				sessions.obterContaAutenticada(token), listId, itemId, ApiSupport.version(etag), key));
	}
	@PutMapping(ROOT + "/{itemId}/checked")
	public ResponseEntity<CheckResult> check(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID listId,
			@PathVariable UUID itemId,
			@RequestBody(required = false) CheckInput input) {
		sessions.validarCsrf(token, csrf);
		var result = service.check(
				sessions.obterContaAutenticada(token), listId, itemId,
				input == null ? null : input.checked(), ApiSupport.version(etag), key);
		return ResponseEntity.ok().eTag(ApiSupport.etag(result.item().version())).body(result);
	}
	@GetMapping(path = "/api/v1/lists/{listId}/events", produces = "text/event-stream")
	public ResponseEntity<SseEmitter> events(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@PathVariable UUID listId) {
		service.access(sessions.obterContaAutenticada(token), listId);
		return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(events.subscribe(listId));
	}
}
