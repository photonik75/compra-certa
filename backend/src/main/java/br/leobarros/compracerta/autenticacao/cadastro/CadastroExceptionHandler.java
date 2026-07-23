package br.leobarros.compracerta.autenticacao.cadastro;

import java.util.List;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CadastroController.class)
public class CadastroExceptionHandler {

	private static final String CODIGO_ERRO_VALIDACAO = "VALIDATION_ERROR";
	private static final String DETALHE_ERRO_VALIDACAO = "Verifique os campos informados e tente novamente.";
	private static final String CODIGO_CONFLITO = "CONFLICT";

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> tratarErroDeValidacao(MethodArgumentNotValidException exception) {
		var erros = exception.getBindingResult().getFieldErrors().stream()
				.map(erro -> new FieldError(erro.getField(), mensagem(erro)))
				.toList();
		var corpo = new ApiError(CODIGO_ERRO_VALIDACAO, DETALHE_ERRO_VALIDACAO, erros);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(corpo);
	}

	@ExceptionHandler(EmailJaCadastradoException.class)
	ResponseEntity<ApiError> tratarEmailJaCadastrado(EmailJaCadastradoException exception) {
		var erro = new FieldError("email", exception.getMessage());
		var corpo = new ApiError(CODIGO_CONFLITO, exception.getMessage(), List.of(erro));
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(corpo);
	}

	private String mensagem(DefaultMessageSourceResolvable erro) {
		return erro.getDefaultMessage();
	}

	record ApiError(String code, String detail, List<FieldError> fieldErrors) {
	}

	record FieldError(String field, String message) {
	}
}
