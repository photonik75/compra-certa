package br.leobarros.compracerta.autenticacao.sessao;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessaoController {

	public static final String ENDPOINT_CONSULTA = "/api/v1/auth/session";
	public static final String ENDPOINT_LOGOUT = "/api/v1/auth/sessions/current";
	private static final String NOME_COOKIE_SESSAO = "cc_session";
	private static final String HEADER_CSRF = "X-CSRF-Token";

	private final SessaoService sessaoService;
	private final SessaoCookieService sessaoCookieService;

	public SessaoController(SessaoService sessaoService, SessaoCookieService sessaoCookieService) {
		this.sessaoService = sessaoService;
		this.sessaoCookieService = sessaoCookieService;
	}

	@GetMapping(ENDPOINT_CONSULTA)
	ResponseEntity<SessionResponse> consultar(
			@CookieValue(name = NOME_COOKIE_SESSAO, required = false) String token) {
		var response = sessaoService.consultar(token);
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, sessaoCookieService.criar(token).toString())
				.body(response);
	}

	@DeleteMapping(ENDPOINT_LOGOUT)
	ResponseEntity<Void> sair(
			@CookieValue(name = NOME_COOKIE_SESSAO, required = false) String token,
			@RequestHeader(name = HEADER_CSRF, required = false) String csrfToken) {
		sessaoService.encerrar(token, csrfToken);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, sessaoCookieService.expirar().toString())
				.build();
	}
}
