package br.leobarros.compracerta.autenticacao.login;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LoginController.class)
public class LoginExceptionHandler {

	private static final String CODIGO_CREDENCIAIS_INVALIDAS = "INVALID_CREDENTIALS";
	private static final String CODIGO_MUITAS_TENTATIVAS = "TOO_MANY_ATTEMPTS";

	@ExceptionHandler(CredenciaisInvalidasException.class)
	ResponseEntity<ApiError> tratarCredenciaisInvalidas(CredenciaisInvalidasException exception) {
		return resposta(HttpStatus.UNAUTHORIZED, CODIGO_CREDENCIAIS_INVALIDAS, exception.getMessage());
	}

	@ExceptionHandler(LoginBloqueadoException.class)
	ResponseEntity<ApiError> tratarLoginBloqueado(LoginBloqueadoException exception) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(new ApiError(CODIGO_MUITAS_TENTATIVAS, exception.getMessage()));
	}

	private ResponseEntity<ApiError> resposta(HttpStatus status, String codigo, String detalhe) {
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(new ApiError(codigo, detalhe));
	}

	record ApiError(String code, String detail) {
	}
}
