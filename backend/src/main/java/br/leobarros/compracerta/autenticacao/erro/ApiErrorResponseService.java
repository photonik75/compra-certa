package br.leobarros.compracerta.autenticacao.erro;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ApiErrorResponseService {

	public ResponseEntity<ApiError> criar(HttpStatus status, String codigo, String detalhe) {
		return criar(status, codigo, detalhe, List.of());
	}

	public ResponseEntity<ApiError> criar(
			HttpStatus status,
			String codigo,
			String detalhe,
			List<ApiFieldError> erros) {
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(new ApiError(codigo, detalhe, erros));
	}

	public ResponseEntity<ApiError> criarComHeader(
			HttpStatus status,
			String codigo,
			String detalhe,
			String nomeHeader,
			String valorHeader) {
		return ResponseEntity.status(status)
				.header(nomeHeader, valorHeader)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(new ApiError(codigo, detalhe, List.of()));
	}
}
