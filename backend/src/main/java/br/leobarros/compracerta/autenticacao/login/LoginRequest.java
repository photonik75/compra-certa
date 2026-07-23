package br.leobarros.compracerta.autenticacao.login;

public record LoginRequest(String email, String password, Object manterConectado) {

	DadosLogin toDadosLogin() {
		return new DadosLogin(email, password, manterConectado);
	}
}
