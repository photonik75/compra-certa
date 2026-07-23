package br.leobarros.compracerta.autenticacao.cadastro;

import java.util.List;

import br.leobarros.compracerta.autenticacao.cadastro.idempotencia.ChaveIdempotenciaInvalidaException;
import br.leobarros.compracerta.autenticacao.cadastro.idempotencia.ChaveIdempotenciaReutilizadaException;
import br.leobarros.compracerta.autenticacao.erro.ApiError;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.erro.ApiFieldError;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CadastroController.class)
public class CadastroExceptionHandler {

	private static final String CODIGO_ERRO_VALIDACAO = "VALIDATION_ERROR";
	private static final String DETALHE_ERRO_VALIDACAO = "Verifique os campos informados e tente novamente.";
	private static final String CODIGO_CONFLITO = "CONFLICT";
	private static final String CODIGO_CHAVE_REUTILIZADA = "IDEMPOTENCY_KEY_REUSED";

	private final ApiErrorResponseService responseService;

	public CadastroExceptionHandler(ApiErrorResponseService responseService) {
		this.responseService = responseService;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> tratarErroDeValidacao(MethodArgumentNotValidException exception) {
		var erros = exception.getBindingResult().getFieldErrors().stream()
				.map(erro -> new ApiFieldError(erro.getField(), mensagem(erro)))
				.toList();
		return responseService.criar(
				HttpStatus.BAD_REQUEST,
				CODIGO_ERRO_VALIDACAO,
				DETALHE_ERRO_VALIDACAO,
				erros);
	}

	@ExceptionHandler(EmailJaCadastradoException.class)
	ResponseEntity<ApiError> tratarEmailJaCadastrado(EmailJaCadastradoException exception) {
		var erro = new ApiFieldError("email", exception.getMessage());
		return responseService.criar(
				HttpStatus.CONFLICT,
				CODIGO_CONFLITO,
				exception.getMessage(),
				List.of(erro));
	}

	@ExceptionHandler(ChaveIdempotenciaInvalidaException.class)
	ResponseEntity<ApiError> tratarChaveIdempotenciaInvalida(ChaveIdempotenciaInvalidaException exception) {
		var erro = new ApiFieldError("Idempotency-Key", exception.getMessage());
		return responseService.criar(
				HttpStatus.BAD_REQUEST,
				CODIGO_ERRO_VALIDACAO,
				DETALHE_ERRO_VALIDACAO,
				List.of(erro));
	}

	@ExceptionHandler(ChaveIdempotenciaReutilizadaException.class)
	ResponseEntity<ApiError> tratarChaveIdempotenciaReutilizada(ChaveIdempotenciaReutilizadaException exception) {
		return responseService.criar(
				HttpStatus.CONFLICT,
				CODIGO_CHAVE_REUTILIZADA,
				exception.getMessage());
	}

	private String mensagem(DefaultMessageSourceResolvable erro) {
		return erro.getDefaultMessage();
	}
}
