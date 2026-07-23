package br.leobarros.compracerta.autenticacao.recuperacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedefinicaoSenhaRequest(
		@NotNull(message = "Informe o token de recuperação")
		@Size(min = 1, message = "Informe o token de recuperação")
		String token,
		@NotNull(message = "Informe a nova senha")
		@Size(min = 8, max = 128, message = "A nova senha deve ter entre 8 e 128 caracteres")
		String newPassword,
		@NotNull(message = "Confirme a nova senha")
		String passwordConfirmation) {
}
