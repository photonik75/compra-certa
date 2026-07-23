package br.leobarros.compracerta.autenticacao.recuperacao;

public class TokenRecuperacaoInvalidoException extends RuntimeException {

	public TokenRecuperacaoInvalidoException() {
		super("Link de recuperação inválido, usado ou expirado");
	}
}
