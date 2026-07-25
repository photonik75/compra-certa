package br.leobarros.compracerta.autenticacao.comum.idempotencia;

interface IdempotenciaResultadoCodec<T> {

	String codigo();

	boolean suporta(Object resultado);

	String serializar(T resultado);

	T desserializar(String json);
}
