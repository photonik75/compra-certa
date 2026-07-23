package br.leobarros.compracerta.autenticacao.cadastro;

import br.leobarros.compracerta.autenticacao.cadastro.idempotencia.IdempotenciaCadastroService;
import br.leobarros.compracerta.autenticacao.sessao.SessionResponse;
import br.leobarros.compracerta.autenticacao.sessao.SessaoCriada;
import br.leobarros.compracerta.autenticacao.sessao.SessaoHttpResponseService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import jakarta.validation.Valid;
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

	public static final String ENDPOINT_CADASTRO = "/api/v1/auth/registrations";

	private final CadastroService cadastroService;
	private final SessaoService sessaoService;
	private final SessaoHttpResponseService sessaoHttpResponseService;
	private final IdempotenciaCadastroService idempotenciaService;

	public CadastroController(
			CadastroService cadastroService,
			SessaoService sessaoService,
			SessaoHttpResponseService sessaoHttpResponseService,
			IdempotenciaCadastroService idempotenciaService) {
		this.cadastroService = cadastroService;
		this.sessaoService = sessaoService;
		this.sessaoHttpResponseService = sessaoHttpResponseService;
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
		return sessaoHttpResponseService.criar(HttpStatus.CREATED, sessao);
	}

	private SessaoCriada criarContaESessao(CadastroRequest request) {
		var conta = cadastroService.cadastrar(request.toDadosCadastro());
		return sessaoService.criarParaCadastro(conta);
	}
}
