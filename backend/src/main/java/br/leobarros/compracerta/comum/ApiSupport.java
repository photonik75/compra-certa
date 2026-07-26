package br.leobarros.compracerta.comum;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.http.HttpStatus;

public final class ApiSupport {

	public static final String COOKIE = "cc_session";
	public static final String CSRF = "X-CSRF-Token";
	public static final String IDEMPOTENCY = "Idempotency-Key";

	private ApiSupport() {
	}

	public static String normalizeSpaces(String value) {
		return value == null ? "" : value.trim().replaceAll("\\s+", " ");
	}

	public static String normalize(String value) {
		return Normalizer.normalize(normalizeSpaces(value), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
	}

	public static long version(String etag) {
		if (etag == null) throw validation("If-Match", "Informe a versão atual do recurso.");
		try {
			return Long.parseLong(etag.replace("W/", "").replace("\"", ""));
		} catch (NumberFormatException exception) {
			throw validation("If-Match", "Informe uma versão atual válida.");
		}
	}

	public static String etag(long version) {
		return "\"" + version + "\"";
	}

	public static void idempotency(String key) {
		if (key == null || key.isBlank() || key.length() > 263) {
			throw validation(IDEMPOTENCY, "Informe uma chave de idempotência válida.");
		}
	}

	public static ApiException validation(String field, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, field);
	}

	public static ApiException notFound() {
		return new ApiException(
				HttpStatus.NOT_FOUND,
				"NOT_FOUND",
				"Recurso não encontrado ou indisponível para sua conta.");
	}

	public static ApiException conflict(long version) {
		return new ApiException(
				HttpStatus.CONFLICT,
				"CONFLICT",
				"Este recurso foi alterado em outro lugar. Recarregue os dados para continuar.",
				version);
	}
}
