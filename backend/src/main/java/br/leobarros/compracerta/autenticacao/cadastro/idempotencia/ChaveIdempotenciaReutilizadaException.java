package br.leobarros.compracerta.autenticacao.cadastro.idempotencia;

public class ChaveIdempotenciaReutilizadaException extends IllegalArgumentException {

	public ChaveIdempotenciaReutilizadaException() {
		super("Esta chave de idempotência já foi utilizada com outros dados");
	}
}
