package br.leobarros.compracerta.autenticacao.login;

import br.leobarros.compracerta.autenticacao.erro.ApiError;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LoginController.class)
public class LoginExceptionHandler {

	private static final String CODIGO_CREDENCIAIS_INVALIDAS = "INVALID_CREDENTIALS";
	private static final String CODIGO_MUITAS_TENTATIVAS = "TOO_MANY_ATTEMPTS";

	private final ApiErrorResponseService responseService;

	public LoginExceptionHandler(ApiErrorResponseService responseService) {
		this.responseService = responseService;
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	ResponseEntity<ApiError> tratarCredenciaisInvalidas(CredenciaisInvalidasException exception) {
		return responseService.criar(
				HttpStatus.UNAUTHORIZED,
				CODIGO_CREDENCIAIS_INVALIDAS,
				exception.getMessage());
	}

	@ExceptionHandler(LoginBloqueadoException.class)
	ResponseEntity<ApiError> tratarLoginBloqueado(LoginBloqueadoException exception) {
		return responseService.criarComHeader(
				HttpStatus.TOO_MANY_REQUESTS,
				CODIGO_MUITAS_TENTATIVAS,
				exception.getMessage(),
				HttpHeaders.RETRY_AFTER,
				String.valueOf(exception.retryAfterSeconds()));
	}
}
