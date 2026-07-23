package br.leobarros.compracerta.autenticacao.sessao;

import br.leobarros.compracerta.autenticacao.erro.ApiError;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SessaoController.class)
public class SessaoExceptionHandler {

	private static final String CODIGO_SESSAO_INVALIDA = "SESSION_INVALID";
	private static final String DETALHE_SESSAO_INVALIDA = "Sua sessão expirou. Entre novamente para continuar.";
	private static final String CODIGO_CSRF_INVALIDO = "CSRF_INVALID";
	private static final String DETALHE_CSRF_INVALIDO =
			"Não foi possível validar a segurança da solicitação. Atualize a página e tente novamente.";

	private final ApiErrorResponseService apiErrorResponseService;

	public SessaoExceptionHandler(ApiErrorResponseService apiErrorResponseService) {
		this.apiErrorResponseService = apiErrorResponseService;
	}

	@ExceptionHandler(SessaoInvalidaException.class)
	ResponseEntity<ApiError> tratarSessaoInvalida() {
		return apiErrorResponseService.criar(
				HttpStatus.UNAUTHORIZED,
				CODIGO_SESSAO_INVALIDA,
				DETALHE_SESSAO_INVALIDA);
	}

	@ExceptionHandler(CsrfInvalidoException.class)
	ResponseEntity<ApiError> tratarCsrfInvalido() {
		return apiErrorResponseService.criar(
				HttpStatus.FORBIDDEN,
				CODIGO_CSRF_INVALIDO,
				DETALHE_CSRF_INVALIDO);
	}
}
