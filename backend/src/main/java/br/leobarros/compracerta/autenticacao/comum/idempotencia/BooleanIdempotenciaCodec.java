package br.leobarros.compracerta.autenticacao.comum.idempotencia;

import tools.jackson.databind.ObjectMapper;

final class BooleanIdempotenciaCodec implements IdempotenciaResultadoCodec<Boolean> {

	static final String CODIGO = "BOOLEAN";

	private final ObjectMapper objectMapper;

	BooleanIdempotenciaCodec(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String codigo() {
		return CODIGO;
	}

	@Override
	public boolean suporta(Object resultado) {
		return resultado instanceof Boolean;
	}

	@Override
	public String serializar(Boolean resultado) {
		return objectMapper.writeValueAsString(resultado);
	}

	@Override
	public Boolean desserializar(String json) {
		return objectMapper.readValue(json, Boolean.class);
	}
}
