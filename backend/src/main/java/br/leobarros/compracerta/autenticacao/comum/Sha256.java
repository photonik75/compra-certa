package br.leobarros.compracerta.autenticacao.comum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

public final class Sha256 {

	private static final String ALGORITMO = "SHA-256";
	private static final String MENSAGEM_ERRO = "Não foi possível proteger os dados informados.";

	private Sha256() {
	}

	public static String base64(String valor) {
		return Base64.getEncoder().encodeToString(digerir(valor));
	}

	public static String hex(String valor) {
		return HexFormat.of().formatHex(digerir(valor));
	}

	private static byte[] digerir(String valor) {
		try {
			return MessageDigest.getInstance(ALGORITMO).digest(valor.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(MENSAGEM_ERRO, exception);
		}
	}
}
