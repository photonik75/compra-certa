package br.leobarros.compracerta.autenticacao.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ErroInesperadoHandler {

	private static final String CODIGO = "INTERNAL_ERROR";
	private static final String DETALHE = "Não foi possível concluir a solicitação. Tente novamente mais tarde.";
	private static final Logger LOGGER = LoggerFactory.getLogger(ErroInesperadoHandler.class);

	private final ApiErrorResponseService responseService;

	public ErroInesperadoHandler(ApiErrorResponseService responseService) {
		this.responseService = responseService;
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> tratar(Exception exception) {
		LOGGER.error("Falha inesperada ao processar a solicitação.", exception);
		return responseService.criar(HttpStatus.INTERNAL_SERVER_ERROR, CODIGO, DETALHE);
	}
}
