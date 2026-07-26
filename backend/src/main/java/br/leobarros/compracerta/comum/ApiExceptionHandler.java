package br.leobarros.compracerta.comum;

import br.leobarros.compracerta.autenticacao.erro.ApiError;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.sessao.CsrfInvalidoException;
import br.leobarros.compracerta.autenticacao.sessao.SessaoExpiradaException;
import br.leobarros.compracerta.autenticacao.sessao.SessaoInvalidaException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = {
		"br.leobarros.compracerta.categorias",
		"br.leobarros.compracerta.produtos",
		"br.leobarros.compracerta.itens",
		"br.leobarros.compracerta.ciclodevida",
		"br.leobarros.compracerta.compartilhamento"
})
class ApiExceptionHandler {

	private final ApiErrorResponseService responses;

	ApiExceptionHandler(ApiErrorResponseService responses) {
		this.responses = responses;
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> api(ApiException exception) {
		if (exception.etag() != null) {
			return responses.criarComHeader(
					exception.status(),
					exception.code(),
					exception.getMessage(),
					HttpHeaders.ETAG,
					exception.etag());
		}
		return responses.criar(
				exception.status(),
				exception.code(),
				exception.getMessage(),
				exception.fieldErrors());
	}

	@ExceptionHandler({SessaoInvalidaException.class, SessaoExpiradaException.class})
	ResponseEntity<ApiError> session() {
		return responses.criar(
				HttpStatus.UNAUTHORIZED,
				"SESSION_INVALID",
				"Sua sessão expirou. Entre novamente para continuar.");
	}

	@ExceptionHandler(CsrfInvalidoException.class)
	ResponseEntity<ApiError> csrf() {
		return responses.criar(
				HttpStatus.FORBIDDEN,
				"CSRF_INVALID",
				"Não foi possível validar a segurança da solicitação. Atualize a página e tente novamente.");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiError> invalidId() {
		return responses.criar(
				HttpStatus.NOT_FOUND,
				"NOT_FOUND",
				"Recurso não encontrado ou indisponível para sua conta.");
	}
}
