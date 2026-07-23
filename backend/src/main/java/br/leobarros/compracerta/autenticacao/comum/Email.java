package br.leobarros.compracerta.autenticacao.comum;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Email {

	private static final int TAMANHO_MAXIMO = 254;
	private static final Pattern FORMATO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final String MENSAGEM_INVALIDO = "Por favor, informe um e-mail válido";

	private Email() {
	}

	public static String validarENormalizar(String email) {
		if (email == null || email.length() > TAMANHO_MAXIMO || !FORMATO.matcher(email).matches()) {
			throw new IllegalArgumentException(MENSAGEM_INVALIDO);
		}
		return email.toLowerCase(Locale.ROOT);
	}
}
