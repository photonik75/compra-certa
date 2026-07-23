package br.leobarros.compracerta.autenticacao.cadastro.idempotencia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

@Service
public class IdempotenciaCadastroService {

	private static final int TAMANHO_MAXIMO_CHAVE = 255;

	private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();

	public <T> T executar(String chave, String conteudo, Supplier<T> processamento) {
		validar(chave);
		var fingerprint = fingerprint(conteudo);
		var registro = registros.computeIfAbsent(chave, valor -> new Registro(fingerprint));
		synchronized (registro) {
			if (!registro.fingerprint.equals(fingerprint)) {
				throw new ChaveIdempotenciaReutilizadaException();
			}
			if (registro.resultado == null) {
				processar(chave, registro, processamento);
			}
			return (T) registro.resultado;
		}
	}

	private void validar(String chave) {
		if (chave == null || chave.isBlank() || chave.length() > TAMANHO_MAXIMO_CHAVE) {
			throw new ChaveIdempotenciaInvalidaException();
		}
	}

	private <T> void processar(String chave, Registro registro, Supplier<T> processamento) {
		try {
			registro.resultado = processamento.get();
		} catch (RuntimeException exception) {
			registros.remove(chave, registro);
			throw exception;
		}
	}

	private String fingerprint(String conteudo) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(conteudo.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Não foi possível validar a idempotência", exception);
		}
	}

	private static class Registro {

		private final String fingerprint;
		private Object resultado;

		Registro(String fingerprint) {
			this.fingerprint = fingerprint;
		}
	}
}
