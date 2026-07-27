package br.leobarros.compracerta.listas;

import br.leobarros.compracerta.autenticacao.comum.idempotencia.ChaveIdempotenciaReutilizadaException;
import br.leobarros.compracerta.autenticacao.erro.ApiError;
import br.leobarros.compracerta.autenticacao.erro.ApiErrorResponseService;
import br.leobarros.compracerta.autenticacao.sessao.CsrfInvalidoException;
import br.leobarros.compracerta.autenticacao.sessao.SessaoExpiradaException;
import br.leobarros.compracerta.autenticacao.sessao.SessaoInvalidaException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = ListaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ListaExceptionHandler {

	private final ApiErrorResponseService respostas;

	ListaExceptionHandler(ApiErrorResponseService respostas) {
		this.respostas = respostas;
	}

	@ExceptionHandler(ListaExceptions.Validacao.class)
	ResponseEntity<ApiError> validacao(ListaExceptions.Validacao exception) {
		return respostas.criar(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Revise os dados informados.",
				exception.erros());
	}

	@ExceptionHandler(ListaExceptions.NaoEncontrada.class)
	ResponseEntity<ApiError> naoEncontrada() {
		return respostas.criar(
				HttpStatus.NOT_FOUND,
				"NOT_FOUND",
				"Lista não encontrada ou indisponível para sua conta.");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiError> identificadorInvalido() {
		return naoEncontrada();
	}

	@ExceptionHandler(ListaExceptions.NomeEmUso.class)
	ResponseEntity<ApiError> nomeEmUso() {
		return respostas.criar(
				HttpStatus.CONFLICT,
				"LIST_NAME_ALREADY_IN_USE",
				"Você já possui uma lista com este nome.");
	}

	@ExceptionHandler(ListaExceptions.Proibida.class)
	ResponseEntity<ApiError> proibida() {
		return respostas.criar(
				HttpStatus.FORBIDDEN,
				"FORBIDDEN",
				"Somente o proprietário pode editar esta lista.");
	}

	@ExceptionHandler(ListaExceptions.Concluida.class)
	ResponseEntity<ApiError> concluida() {
		return respostas.criar(
				HttpStatus.CONFLICT,
				"LIST_COMPLETED",
				"Esta lista está concluída e não pode ser editada.");
	}

	@ExceptionHandler(ListaExceptions.Conflito.class)
	ResponseEntity<ApiError> conflito(ListaExceptions.Conflito exception) {
		return respostas.criarComHeader(
				HttpStatus.CONFLICT,
				"CONFLICT",
				"Esta lista foi alterada em outro lugar. Recarregue os dados para continuar.",
				HttpHeaders.ETAG,
				"\"" + exception.versao() + "\"");
	}

	@ExceptionHandler(ChaveIdempotenciaReutilizadaException.class)
	ResponseEntity<ApiError> idempotencia() {
		return respostas.criar(
				HttpStatus.CONFLICT,
				"IDEMPOTENCY_KEY_REUSED",
				"A chave de idempotência já foi usada com outros dados.");
	}

	@ExceptionHandler({SessaoInvalidaException.class, SessaoExpiradaException.class})
	ResponseEntity<ApiError> sessaoInvalida() {
		return respostas.criar(
				HttpStatus.UNAUTHORIZED,
				"SESSION_INVALID",
				"Sua sessão expirou. Entre novamente para continuar.");
	}

	@ExceptionHandler(CsrfInvalidoException.class)
	ResponseEntity<ApiError> csrfInvalido() {
		return respostas.criar(
				HttpStatus.FORBIDDEN,
				"CSRF_INVALID",
				"Não foi possível validar a segurança da solicitação. Atualize a página e tente novamente.");
	}
}
