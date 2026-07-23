package br.leobarros.compracerta.autenticacao.sessao;

import java.time.Duration;
import java.time.Instant;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;

class SessaoRegistro {

	private final String tokenHash;
	private String csrfTokenHash;
	private final Conta conta;
	private final Instant criadaEm;
	private final Duration duracaoInatividade;
	private Instant expiraPorInatividade;
	private final Instant expiraEmDefinitivo;
	private boolean revogada;

	SessaoRegistro(
			String tokenHash,
			String csrfTokenHash,
			Conta conta,
			Instant criadaEm,
			Duration duracaoInatividade,
			Instant expiraPorInatividade,
			Instant expiraEmDefinitivo) {
		this.tokenHash = tokenHash;
		this.csrfTokenHash = csrfTokenHash;
		this.conta = conta;
		this.criadaEm = criadaEm;
		this.duracaoInatividade = duracaoInatividade;
		this.expiraPorInatividade = expiraPorInatividade;
		this.expiraEmDefinitivo = expiraEmDefinitivo;
	}

	String tokenHash() {
		return tokenHash;
	}

	String csrfTokenHash() {
		return csrfTokenHash;
	}

	void atualizarCsrfTokenHash(String csrfTokenHash) {
		this.csrfTokenHash = csrfTokenHash;
	}

	Conta conta() {
		return conta;
	}

	Instant criadaEm() {
		return criadaEm;
	}

	Duration duracaoInatividade() {
		return duracaoInatividade;
	}

	Instant expiraPorInatividade() {
		return expiraPorInatividade;
	}

	void atualizarExpiracaoPorInatividade(Instant expiracao) {
		expiraPorInatividade = expiracao;
	}

	Instant expiraEmDefinitivo() {
		return expiraEmDefinitivo;
	}

	boolean revogada() {
		return revogada;
	}

	void revogar() {
		revogada = true;
	}
}
