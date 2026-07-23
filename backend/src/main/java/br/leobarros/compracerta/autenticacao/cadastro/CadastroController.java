package br.leobarros.compracerta.autenticacao.cadastro;

import br.leobarros.compracerta.autenticacao.idempotencia.IdempotenciaCadastroService;
import br.leobarros.compracerta.autenticacao.sessao.SessionResponse;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCriada;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCookieService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CadastroController.ENDPOINT_CADASTRO)
public class CadastroController {

	static final String ENDPOINT_CADASTRO = "/api/v1/auth/registrations";

	private final CadastroService cadastroService;
	private final SessaoService sessaoService;
	private final SessaoCookieService sessaoCookieService;
	private final IdempotenciaCadastroService idempotenciaService;

	public CadastroController(
			CadastroService cadastroService,
			SessaoService sessaoService,
			SessaoCookieService sessaoCookieService,
			IdempotenciaCadastroService idempotenciaService) {
		this.cadastroService = cadastroService;
		this.sessaoService = sessaoService;
		this.sessaoCookieService = sessaoCookieService;
		this.idempotenciaService = idempotenciaService;
	}

	@PostMapping
	public ResponseEntity<SessionResponse> cadastrar(
			@RequestHeader(name = "Idempotency-Key", required = false) String chaveIdempotencia,
			@Valid @RequestBody CadastroRequest request) {
		var sessao = idempotenciaService.executar(
				chaveIdempotencia,
				request.toString(),
				() -> criarContaESessao(request));
		var cookie = sessaoCookieService.criar(sessao.token());
		return ResponseEntity.status(HttpStatus.CREATED)
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(sessao.response());
	}

	private SessaoCriada criarContaESessao(CadastroRequest request) {
		var conta = cadastroService.cadastrar(request.toDadosCadastro());
		return sessaoService.criarParaCadastro(conta);
	}
}
