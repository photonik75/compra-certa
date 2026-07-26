package br.leobarros.compracerta.comum;

import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.comum.Sha256;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotenciaRepository {

	private final JdbcTemplate jdbc;

	public IdempotenciaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Result> find(UUID accountId, String scope, String key) {
		return jdbc.query(
				"SELECT fingerprint,recurso_id,resultado FROM operacoes_idempotentes "
						+ "WHERE conta_id=? AND escopo=? AND chave=? FOR UPDATE",
				(rs, row) -> new Result(
						rs.getString("fingerprint"),
						rs.getObject("recurso_id", UUID.class),
						rs.getString("resultado")),
				accountId, scope, key).stream().findFirst();
	}

	public Optional<Result> replay(UUID accountId, String scope, String key, String content) {
		ApiSupport.idempotency(key);
		var result = find(accountId, scope, key);
		if (result.isPresent() && !result.orElseThrow().fingerprint().equals(Sha256.hex(content))) {
			throw new ApiException(
					org.springframework.http.HttpStatus.CONFLICT,
					"IDEMPOTENCY_KEY_REUSED",
					"A chave de idempotência já foi usada com outros dados.");
		}
		return result;
	}

	public void begin(UUID accountId, String scope, String key, String content) {
		jdbc.update(
				"INSERT INTO operacoes_idempotentes(conta_id,escopo,chave,fingerprint) VALUES (?,?,?,?)",
				accountId, scope, key, Sha256.hex(content));
	}

	public void finish(UUID accountId, String scope, String key, UUID resourceId, String result) {
		jdbc.update(
				"UPDATE operacoes_idempotentes SET recurso_id=?,resultado=? "
						+ "WHERE conta_id=? AND escopo=? AND chave=?",
				resourceId, result, accountId, scope, key);
	}

	public record Result(String fingerprint, UUID resourceId, String result) {
	}
}
