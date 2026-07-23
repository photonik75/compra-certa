package br.leobarros.compracerta.autenticacao.login;

import br.leobarros.compracerta.autenticacao.sessao.SessionResponse;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCookieService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(LoginController.ENDPOINT_LOGIN)
public class LoginController {

	public static final String ENDPOINT_LOGIN = "/api/v1/auth/sessions";

	private final LoginService loginService;
	private final SessaoService sessaoService;
	private final SessaoCookieService sessaoCookieService;

	public LoginController(
			LoginService loginService,
			SessaoService sessaoService,
			SessaoCookieService sessaoCookieService) {
		this.loginService = loginService;
		this.sessaoService = sessaoService;
		this.sessaoCookieService = sessaoCookieService;
	}

	@PostMapping
	public ResponseEntity<SessionResponse> autenticar(@RequestBody LoginRequest request) {
		var conta = loginService.autenticar(request.toDadosLogin());
		var sessao = sessaoService.criarParaLogin(conta, (Boolean) request.manterConectado());
		var cookie = sessaoCookieService.criar(sessao.token());
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(sessao.response());
	}
}
