package br.leobarros.compracerta.autenticacao.recuperacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class HashSeguro {

	private HashSeguro() {
	}

	static String gerar(String valor) {
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Não foi possível proteger o token.", exception);
		}
	}
}
