package br.leobarros.compracerta.autenticacao.idempotencia;

public class ChaveIdempotenciaInvalidaException extends IllegalArgumentException {

	public ChaveIdempotenciaInvalidaException() {
		super("Por favor, informe uma chave de idempotência válida");
	}
}
