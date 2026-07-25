package br.leobarros.compracerta.autenticacao.sessao;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessaoPostgresqlRepository extends SessaoRepository {

	private static final String BUSCAR_POR_TOKEN = """
			SELECT s.*, c.nome, c.email, c.senha_hash, c.ativa
			FROM sessoes s
			JOIN contas c ON c.id = s.conta_id
			WHERE s.token_hash = ?
			""";
	private static final String REVOGAR_DA_CONTA =
			"UPDATE sessoes SET revogada = true WHERE conta_id = ?";
	private static final String SALVAR = """
			INSERT INTO sessoes (
			    token_hash, csrf_token_hash, conta_id, criada_em, duracao_inatividade_segundos,
			    expira_por_inatividade, expira_em_definitivo, revogada
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (token_hash) DO UPDATE SET
			    csrf_token_hash = EXCLUDED.csrf_token_hash,
			    expira_por_inatividade = EXCLUDED.expira_por_inatividade,
			    revogada = EXCLUDED.revogada
			""";

	private final JdbcTemplate jdbc;

	public SessaoPostgresqlRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	Optional<SessaoRegistro> buscarPorTokenHash(String tokenHash) {
		return jdbc.query(
				BUSCAR_POR_TOKEN,
				(resultado, linha) -> mapear(resultado),
				tokenHash).stream().findFirst();
	}

	@Override
	void salvar(SessaoRegistro sessao) {
		jdbc.update(
				SALVAR,
				sessao.tokenHash(),
				sessao.csrfTokenHash(),
				sessao.conta().getId(),
				java.sql.Timestamp.from(sessao.criadaEm()),
				sessao.duracaoInatividade().toSeconds(),
				java.sql.Timestamp.from(sessao.expiraPorInatividade()),
				java.sql.Timestamp.from(sessao.expiraEmDefinitivo()),
				sessao.revogada());
	}

	@Override
	void revogarDaConta(UUID contaId) {
		jdbc.update(REVOGAR_DA_CONTA, contaId);
	}

	private SessaoRegistro mapear(java.sql.ResultSet resultado) throws java.sql.SQLException {
		var conta = new Conta(
				resultado.getObject("conta_id", UUID.class),
				resultado.getString("nome"),
				resultado.getString("email"),
				resultado.getString("senha_hash"),
				resultado.getBoolean("ativa"));
		return new SessaoRegistro(
				resultado.getString("token_hash"),
				resultado.getString("csrf_token_hash"),
				conta,
				resultado.getTimestamp("criada_em").toInstant(),
				Duration.ofSeconds(resultado.getLong("duracao_inatividade_segundos")),
				resultado.getTimestamp("expira_por_inatividade").toInstant(),
				resultado.getTimestamp("expira_em_definitivo").toInstant(),
				resultado.getBoolean("revogada"));
	}
}
