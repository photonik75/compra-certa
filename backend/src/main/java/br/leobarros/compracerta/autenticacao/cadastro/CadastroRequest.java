package br.leobarros.compracerta.autenticacao.cadastro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
		@NotNull(message = "Por favor, informe seu nome")
		@Pattern(
				regexp = "^(?=.*\\S).{2,100}$",
				message = "O nome deve ter entre 2 e 100 caracteres e não pode conter somente espaços")
		String name,
		@NotNull(message = "Por favor, informe seu e-mail")
		@Email(message = "Por favor, informe um e-mail válido")
		@Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
		String email,
		@NotNull(message = "Por favor, informe uma senha")
		@Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres")
		String password,
		@NotNull(message = "Por favor, confirme sua senha")
		@Size(min = 8, max = 128, message = "A confirmação deve ter entre 8 e 128 caracteres")
		String passwordConfirmation) {

	DadosCadastro toDadosCadastro() {
		return new DadosCadastro(name, email, password, passwordConfirmation);
	}
}
