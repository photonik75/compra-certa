package br.leobarros.compracerta.autenticacao.comum.idempotencia;

import br.leobarros.compracerta.autenticacao.sessao.SessaoCriada;
import tools.jackson.databind.ObjectMapper;

final class SessaoCriadaIdempotenciaCodec implements IdempotenciaResultadoCodec<SessaoCriada> {

	static final String CODIGO = "SESSAO_CRIADA";

	private final ObjectMapper objectMapper;

	SessaoCriadaIdempotenciaCodec(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String codigo() {
		return CODIGO;
	}

	@Override
	public boolean suporta(Object resultado) {
		return resultado instanceof SessaoCriada;
	}

	@Override
	public String serializar(SessaoCriada resultado) {
		return objectMapper.writeValueAsString(resultado);
	}

	@Override
	public SessaoCriada desserializar(String json) {
		return objectMapper.readValue(json, SessaoCriada.class);
	}
}
