package br.leobarros.compracerta.autenticacao.cadastro;

public class EmailJaCadastradoException extends IllegalArgumentException {

	private static final String MENSAGEM = "E-mail já foi cadastrado";

	public EmailJaCadastradoException() {
		super(MENSAGEM);
	}
}
