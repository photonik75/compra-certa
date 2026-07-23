package br.leobarros.compracerta.autenticacao.cadastro;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UserSummary user, String csrfToken, Instant expiresAt) {

	public record UserSummary(UUID id, String name, String email, String status, Instant createdAt) {
	}
}
