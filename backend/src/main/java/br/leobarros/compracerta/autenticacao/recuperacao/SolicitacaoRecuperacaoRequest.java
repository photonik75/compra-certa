package br.leobarros.compracerta.autenticacao.recuperacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitacaoRecuperacaoRequest(
		@NotNull(message = "Por favor, informe seu e-mail")
		@Email(message = "Por favor, informe um e-mail válido")
		@Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
		String email) {
}
