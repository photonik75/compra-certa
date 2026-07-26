package br.leobarros.compracerta.listas;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.listas.ListaDtos.CreateListRequest;
import br.leobarros.compracerta.listas.ListaDtos.ListCollection;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class ListaController {

	public static final String ENDPOINT_LISTAS = "/api/v1/lists";
	public static final String ENDPOINT_LISTA = "/api/v1/lists/{listId}";
	private static final String COOKIE_SESSAO = "cc_session";
	private static final String HEADER_CSRF = "X-CSRF-Token";
	private static final String HEADER_IDEMPOTENCIA = "Idempotency-Key";
	private static final Set<String> CAMPOS_PATCH = Set.of("name", "description");

	private final ListaService listaService;
	private final SessaoService sessaoService;

	public ListaController(ListaService listaService, SessaoService sessaoService) {
		this.listaService = listaService;
		this.sessaoService = sessaoService;
	}

	@GetMapping(ENDPOINT_LISTAS)
	ResponseEntity<ListCollection> listar(
			@CookieValue(name = COOKIE_SESSAO, required = false) String token,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer limit) {
		var conta = sessaoService.obterContaAutenticada(token);
		return ResponseEntity.ok(listaService.listar(conta, status, search, cursor, limit));
	}

	@PostMapping(ENDPOINT_LISTAS)
	ResponseEntity<ListDetail> criar(
			@CookieValue(name = COOKIE_SESSAO, required = false) String token,
			@RequestHeader(name = HEADER_CSRF, required = false) String csrf,
			@RequestHeader(name = HEADER_IDEMPOTENCIA, required = false) String chave,
			@RequestBody(required = false) CreateListRequest request) {
		sessaoService.validarCsrf(token, csrf);
		var conta = sessaoService.obterContaAutenticada(token);
		var detalhe = listaService.criar(conta, request, chave);
		return ResponseEntity.created(URI.create(ENDPOINT_LISTAS + "/" + detalhe.id()))
				.eTag(etag(detalhe.version()))
				.body(detalhe);
	}

	@GetMapping(ENDPOINT_LISTA)
	ResponseEntity<ListDetail> buscar(
			@CookieValue(name = COOKIE_SESSAO, required = false) String token,
			@PathVariable UUID listId) {
		var detalhe = listaService.buscar(sessaoService.obterContaAutenticada(token), listId);
		return ResponseEntity.ok().eTag(etag(detalhe.version())).body(detalhe);
	}

	@PatchMapping(ENDPOINT_LISTA)
	ResponseEntity<ListDetail> atualizar(
			@CookieValue(name = COOKIE_SESSAO, required = false) String token,
			@RequestHeader(name = HEADER_CSRF, required = false) String csrf,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
			@PathVariable UUID listId,
			@RequestBody(required = false) JsonNode body) {
		sessaoService.validarCsrf(token, csrf);
		validarCorpo(body);
		var detalhe = listaService.atualizar(
				sessaoService.obterContaAutenticada(token),
				listId,
				versao(ifMatch),
				body.has("name"),
				texto(body, "name"),
				body.has("description"),
				texto(body, "description"));
		return ResponseEntity.ok().eTag(etag(detalhe.version())).body(detalhe);
	}

	private void validarCorpo(JsonNode body) {
		if (body == null || !body.isObject()) {
			throw new ListaExceptions.Validacao("body", "Informe ao menos uma alteração.");
		}
		body.propertyNames().forEach(campo -> {
			if (!CAMPOS_PATCH.contains(campo)) {
				throw new ListaExceptions.Validacao(campo, "Este campo não pode ser alterado.");
			}
		});
	}

	private String texto(JsonNode body, String campo) {
		if (!body.has(campo) || body.get(campo).isNull()) return null;
		if (!body.get(campo).isString()) {
			throw new ListaExceptions.Validacao(campo, "Informe um texto válido.");
		}
		return body.get(campo).asString();
	}

	private Long versao(String ifMatch) {
		if (ifMatch == null) return null;
		try {
			var valor = ifMatch.replace("W/", "").replace("\"", "");
			return Long.parseLong(valor);
		} catch (NumberFormatException exception) {
			throw new ListaExceptions.Validacao("If-Match", "Informe uma versão atual válida.");
		}
	}

	private String etag(long versao) {
		return "\"" + versao + "\"";
	}
}
