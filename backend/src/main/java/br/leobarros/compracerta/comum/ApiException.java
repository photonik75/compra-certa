package br.leobarros.compracerta.comum;

import java.util.List;

import br.leobarros.compracerta.autenticacao.erro.ApiFieldError;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final List<ApiFieldError> fieldErrors;
	private final String etag;

	public ApiException(HttpStatus status, String code, String detail) {
		this(status, code, detail, List.of(), null);
	}

	public ApiException(HttpStatus status, String code, String detail, String field) {
		this(status, code, detail, List.of(new ApiFieldError(field, detail)), null);
	}

	public ApiException(HttpStatus status, String code, String detail, long version) {
		this(status, code, detail, List.of(), "\"" + version + "\"");
	}

	private ApiException(
			HttpStatus status,
			String code,
			String detail,
			List<ApiFieldError> fieldErrors,
			String etag) {
		super(detail);
		this.status = status;
		this.code = code;
		this.fieldErrors = fieldErrors;
		this.etag = etag;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public List<ApiFieldError> fieldErrors() {
		return fieldErrors;
	}

	public String etag() {
		return etag;
	}
}
