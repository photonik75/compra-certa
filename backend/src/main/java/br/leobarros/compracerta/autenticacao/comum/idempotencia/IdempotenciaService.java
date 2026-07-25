package br.leobarros.compracerta.autenticacao.comum.idempotencia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class IdempotenciaService {

	private static final int TAMANHO_MAXIMO_CHAVE = 263;

	private final Map<String, Registro> registros = new ConcurrentHashMap<>();
	private final Map<String, Object> bloqueios = new ConcurrentHashMap<>();
	private final JdbcTemplate jdbc;
	private final ObjectMapper objectMapper;

	public IdempotenciaService(
			ObjectProvider<JdbcTemplate> jdbcProvider,
			ObjectProvider<ObjectMapper> objectMapperProvider) {
		this.jdbc = jdbcProvider.getIfAvailable();
		this.objectMapper = objectMapperProvider.getIfAvailable();
	}

	public <T> T executar(String chave, String conteudo, Supplier<T> processamento) {
		validar(chave);
		var bloqueio = bloqueios.computeIfAbsent(chave, valor -> new Object());
		try {
			synchronized (bloqueio) {
				return jdbc == null
						? executarEmMemoria(chave, conteudo, processamento)
						: executarNoBanco(chave, conteudo, processamento);
			}
		} finally {
			bloqueios.remove(chave, bloqueio);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T executarEmMemoria(String chave, String conteudo, Supplier<T> processamento) {
		var hash = fingerprint(conteudo);
		var registro = registros.computeIfAbsent(chave, valor -> new Registro(hash));
		if (!registro.fingerprint.equals(hash)) {
			throw new ChaveIdempotenciaReutilizadaException();
		}
		if (registro.resultado == null) {
			processarEmMemoria(chave, registro, processamento);
		}
		return (T) registro.resultado;
	}

	@SuppressWarnings("unchecked")
	private <T> T executarNoBanco(String chave, String conteudo, Supplier<T> processamento) {
		var hash = fingerprint(conteudo);
		var registro = jdbc.query(
				"SELECT fingerprint, resultado_tipo, resultado_json FROM idempotencias WHERE chave = ?",
				(resultado, linha) -> new RegistroPersistido(
						resultado.getString("fingerprint"),
						resultado.getString("resultado_tipo"),
						resultado.getString("resultado_json")),
				chave).stream().findFirst();
		if (registro.isPresent()) {
			validarFingerprint(registro.orElseThrow().fingerprint(), hash);
			return (T) desserializar(registro.orElseThrow());
		}
		jdbc.update(
				"INSERT INTO idempotencias (chave, fingerprint) VALUES (?, ?)",
				chave,
				hash);
		try {
			var resultado = processamento.get();
			jdbc.update(
					"UPDATE idempotencias SET resultado_tipo = ?, resultado_json = ? WHERE chave = ?",
					resultado.getClass().getName(),
					objectMapper.writeValueAsString(resultado),
					chave);
			return resultado;
		} catch (RuntimeException exception) {
			try {
				jdbc.update("DELETE FROM idempotencias WHERE chave = ?", chave);
			} catch (RuntimeException limpezaException) {
				exception.addSuppressed(limpezaException);
			}
			throw exception;
		}
	}

	private Object desserializar(RegistroPersistido registro) {
		if (registro.resultadoTipo() == null) {
			throw new IllegalStateException("O processamento idempotente anterior ainda não foi concluído.");
		}
		try {
			return objectMapper.readValue(registro.resultadoJson(), Class.forName(registro.resultadoTipo()));
		} catch (ClassNotFoundException exception) {
			throw new IllegalStateException("Não foi possível recuperar o resultado idempotente.", exception);
		}
	}

	private void validar(String chave) {
		if (chave == null || chave.isBlank() || chave.length() > TAMANHO_MAXIMO_CHAVE) {
			throw new ChaveIdempotenciaInvalidaException();
		}
	}

	private void validarFingerprint(String atual, String recebido) {
		if (!atual.equals(recebido)) {
			throw new ChaveIdempotenciaReutilizadaException();
		}
	}

	private <T> void processarEmMemoria(String chave, Registro registro, Supplier<T> processamento) {
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

	private record RegistroPersistido(String fingerprint, String resultadoTipo, String resultadoJson) {
	}

	private static class Registro {

		private final String fingerprint;
		private Object resultado;

		Registro(String fingerprint) {
			this.fingerprint = fingerprint;
		}
	}
}
