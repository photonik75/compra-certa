package br.leobarros.compracerta.autenticacao.sessao;

import java.time.Instant;

public record SessaoEstado(
		String tokenHash,
		String csrfTokenHash,
		Instant expiraPorInatividade,
		Instant expiraEmDefinitivo) {
}
