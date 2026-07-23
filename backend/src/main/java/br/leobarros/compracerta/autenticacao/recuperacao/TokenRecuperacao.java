package br.leobarros.compracerta.autenticacao.recuperacao;

import java.time.Instant;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;

class TokenRecuperacao {

	private final String hash;
	private final Conta conta;
	private final Instant expiraEm;
	private boolean usado;
	private boolean invalidado;

	TokenRecuperacao(String hash, Conta conta, Instant expiraEm) {
		this.hash = hash;
		this.conta = conta;
		this.expiraEm = expiraEm;
	}

	String hash() {
		return hash;
	}

	Conta conta() {
		return conta;
	}

	Instant expiraEm() {
		return expiraEm;
	}

	boolean usado() {
		return usado;
	}

	boolean invalidado() {
		return invalidado;
	}

	void usar() {
		usado = true;
	}

	void restaurarUso() {
		usado = false;
	}

	void invalidar() {
		invalidado = true;
	}
}
