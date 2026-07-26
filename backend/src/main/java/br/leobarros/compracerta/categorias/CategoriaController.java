package br.leobarros.compracerta.categorias;

import java.net.URI;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.categorias.CategoriaDtos.Category;
import br.leobarros.compracerta.categorias.CategoriaDtos.Collection;
import br.leobarros.compracerta.categorias.CategoriaDtos.Input;
import br.leobarros.compracerta.comum.ApiSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoriaController {

	private static final String ROOT = "/api/v1/categories";
	private final CategoriaService service;
	private final SessaoService sessions;

	CategoriaController(CategoriaService service, SessaoService sessions) {
		this.service = service;
		this.sessions = sessions;
	}

	@GetMapping(ROOT)
	ResponseEntity<Collection> list(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer limit) {
		return ResponseEntity.ok(service.list(sessions.obterContaAutenticada(token), search, cursor, limit));
	}

	@PostMapping(ROOT)
	ResponseEntity<Category> create(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestBody(required = false) Input input) {
		sessions.validarCsrf(token, csrf);
		var created = service.create(sessions.obterContaAutenticada(token), input, key);
		return ResponseEntity.created(URI.create(ROOT + "/" + created.id()))
				.eTag(ApiSupport.etag(created.version())).body(created);
	}

	@GetMapping(ROOT + "/{id}")
	ResponseEntity<Category> get(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@PathVariable UUID id) {
		var category = service.get(sessions.obterContaAutenticada(token), id);
		return ResponseEntity.ok().eTag(ApiSupport.etag(category.version())).body(category);
	}

	@PatchMapping(ROOT + "/{id}")
	ResponseEntity<Category> update(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID id,
			@RequestBody(required = false) Input input) {
		sessions.validarCsrf(token, csrf);
		var category = service.update(
				sessions.obterContaAutenticada(token), id, input, ApiSupport.version(etag));
		return ResponseEntity.ok().eTag(ApiSupport.etag(category.version())).body(category);
	}

	@DeleteMapping(ROOT + "/{id}")
	ResponseEntity<Void> delete(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID id) {
		sessions.validarCsrf(token, csrf);
		service.delete(sessions.obterContaAutenticada(token), id, ApiSupport.version(etag));
		return ResponseEntity.noContent().build();
	}
}
