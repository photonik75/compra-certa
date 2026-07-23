package br.leobarros.compracerta.autenticacao.recuperacao;

import br.leobarros.compracerta.autenticacao.comum.idempotencia.IdempotenciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecuperacaoSenhaController {

	public static final String ENDPOINT_SOLICITACAO = "/api/v1/auth/password-reset-requests";
	public static final String ENDPOINT_REDEFINICAO = "/api/v1/auth/password-resets";
	private static final String HEADER_IDEMPOTENCIA = "Idempotency-Key";

	private final SolicitacaoRecuperacaoService solicitacaoService;
	private final RedefinicaoSenhaService redefinicaoService;
	private final IdempotenciaService idempotenciaService;

	public RecuperacaoSenhaController(
			SolicitacaoRecuperacaoService solicitacaoService,
			RedefinicaoSenhaService redefinicaoService,
			IdempotenciaService idempotenciaService) {
		this.solicitacaoService = solicitacaoService;
		this.redefinicaoService = redefinicaoService;
		this.idempotenciaService = idempotenciaService;
	}

	@PostMapping(ENDPOINT_SOLICITACAO)
	ResponseEntity<Void> solicitar(
			@RequestHeader(name = HEADER_IDEMPOTENCIA, required = false) String chave,
			@Valid @RequestBody SolicitacaoRecuperacaoRequest request) {
		idempotenciaService.executar(
				chaveComEscopo(chave, "request:"),
				request.email(),
				() -> {
					solicitacaoService.solicitar(request.email());
					return Boolean.TRUE;
				});
		return ResponseEntity.accepted().build();
	}

	@PostMapping(ENDPOINT_REDEFINICAO)
	ResponseEntity<Void> redefinir(
			@RequestHeader(name = HEADER_IDEMPOTENCIA, required = false) String chave,
			@Valid @RequestBody RedefinicaoSenhaRequest request) {
		var conteudo = request.token() + '\0' + request.newPassword() + '\0' + request.passwordConfirmation();
		idempotenciaService.executar(
				chaveComEscopo(chave, "reset:"),
				conteudo,
				() -> {
					redefinicaoService.redefinir(
							request.token(),
							request.newPassword(),
							request.passwordConfirmation());
					return Boolean.TRUE;
				});
		return ResponseEntity.noContent().build();
	}

	private String chaveComEscopo(String chave, String escopo) {
		return chave == null ? null : escopo + chave;
	}
}
