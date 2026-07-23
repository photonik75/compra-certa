package br.leobarros.compracerta.autenticacao.recuperacao;

import java.util.List;

import br.leobarros.compracerta.autenticacao.comum.idempotencia.ChaveIdempotenciaInvalidaException;
import br.leobarros.compracerta.autenticacao.comum.idempotencia.ChaveIdempotenciaReutilizadaException;
import br.leobarros.compracerta.autenticacao.erro.ApiError;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.erro.ApiFieldError;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RecuperacaoSenhaController.class)
public class RecuperacaoSenhaExceptionHandler {

	private static final String DETALHE_VALIDACAO = "Verifique os campos informados e tente novamente.";
	private final ApiErrorResponseService responseService;

	public RecuperacaoSenhaExceptionHandler(ApiErrorResponseService responseService) {
		this.responseService = responseService;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> tratarValidacao(MethodArgumentNotValidException exception) {
		var erros = exception.getBindingResult().getFieldErrors().stream()
				.map(erro -> new ApiFieldError(erro.getField(), mensagem(erro)))
				.toList();
		return responseService.criar(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", DETALHE_VALIDACAO, erros);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiError> tratarArgumentoInvalido(IllegalArgumentException exception) {
		var erro = new ApiFieldError("request", exception.getMessage());
		return responseService.criar(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				DETALHE_VALIDACAO,
				List.of(erro));
	}

	@ExceptionHandler(TokenRecuperacaoInvalidoException.class)
	ResponseEntity<ApiError> tratarTokenInvalido(TokenRecuperacaoInvalidoException exception) {
		return responseService.criar(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_RESET_TOKEN", exception.getMessage());
	}

	@ExceptionHandler(RecuperacaoSenhaException.class)
	ResponseEntity<ApiError> tratarFalhaEntrega(RecuperacaoSenhaException exception) {
		return responseService.criar(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"PASSWORD_RESET_DELIVERY_FAILED",
				exception.getMessage());
	}

	@ExceptionHandler(ChaveIdempotenciaReutilizadaException.class)
	ResponseEntity<ApiError> tratarChaveReutilizada(ChaveIdempotenciaReutilizadaException exception) {
		return responseService.criar(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", exception.getMessage());
	}

	@ExceptionHandler(ChaveIdempotenciaInvalidaException.class)
	ResponseEntity<ApiError> tratarChaveInvalida(ChaveIdempotenciaInvalidaException exception) {
		return tratarArgumentoInvalido(exception);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> tratarErroInesperado() {
		return responseService.criar(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR",
				"Não foi possível concluir a solicitação. Tente novamente mais tarde.");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ApiError> tratarTipoNaoSuportado() {
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(new ApiError(
						"UNSUPPORTED_MEDIA_TYPE",
						"Envie os dados no formato JSON.",
						List.of()));
	}

	private String mensagem(DefaultMessageSourceResolvable erro) {
		return erro.getDefaultMessage();
	}
}
