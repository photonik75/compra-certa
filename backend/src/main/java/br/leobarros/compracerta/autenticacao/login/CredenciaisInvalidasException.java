package br.leobarros.compracerta.autenticacao.login;

public class CredenciaisInvalidasException extends RuntimeException {

	private static final String MENSAGEM = "E-mail ou senha inválidos";

	public CredenciaisInvalidasException() {
		super(MENSAGEM);
	}
}
