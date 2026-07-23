package br.leobarros.compracerta.autenticacao.login;

public class LoginBloqueadoException extends RuntimeException {

	private static final String MENSAGEM = "Muitas tentativas de acesso. Tente novamente em 15 minutos";

	private final long retryAfterSeconds;

	public LoginBloqueadoException(long retryAfterSeconds) {
		super(MENSAGEM);
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}
}
