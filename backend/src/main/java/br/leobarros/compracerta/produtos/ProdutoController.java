package br.leobarros.compracerta.produtos;

import java.net.URI;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.produtos.ProdutoDtos.Collection;
import br.leobarros.compracerta.produtos.ProdutoDtos.Input;
import br.leobarros.compracerta.produtos.ProdutoDtos.Product;
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
public class ProdutoController {
	private static final String ROOT = "/api/v1/products";
	private final ProdutoService service;
	private final SessaoService sessions;
	ProdutoController(ProdutoService service, SessaoService sessions) {
		this.service = service;
		this.sessions = sessions;
	}
	@GetMapping(ROOT)
	ResponseEntity<Collection> list(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer limit) {
		return ResponseEntity.ok(service.list(
				sessions.obterContaAutenticada(token), search, categoryId, status, cursor, limit));
	}
	@PostMapping(ROOT)
	ResponseEntity<Product> create(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = ApiSupport.IDEMPOTENCY, required = false) String key,
			@RequestBody(required = false) Input input) {
		sessions.validarCsrf(token, csrf);
		var product = service.create(sessions.obterContaAutenticada(token), input, key);
		return ResponseEntity.created(URI.create(ROOT + "/" + product.id()))
				.eTag(ApiSupport.etag(product.version())).body(product);
	}
	@GetMapping(ROOT + "/{id}")
	ResponseEntity<Product> get(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@PathVariable UUID id) {
		var product = service.get(sessions.obterContaAutenticada(token), id);
		return ResponseEntity.ok().eTag(ApiSupport.etag(product.version())).body(product);
	}
	@PatchMapping(ROOT + "/{id}")
	ResponseEntity<Product> update(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID id,
			@RequestBody(required = false) Input input) {
		sessions.validarCsrf(token, csrf);
		var product = service.update(
				sessions.obterContaAutenticada(token), id, input, ApiSupport.version(etag));
		return ResponseEntity.ok().eTag(ApiSupport.etag(product.version())).body(product);
	}
	@DeleteMapping(ROOT + "/{id}")
	ResponseEntity<Void> deactivate(
			@CookieValue(name = ApiSupport.COOKIE, required = false) String token,
			@RequestHeader(name = ApiSupport.CSRF, required = false) String csrf,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String etag,
			@PathVariable UUID id) {
		sessions.validarCsrf(token, csrf);
		service.deactivate(sessions.obterContaAutenticada(token), id, ApiSupport.version(etag));
		return ResponseEntity.noContent().build();
	}
}
