package br.leobarros.compracerta.autenticacao.sessao;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessaoRepository {

	private final Map<String, SessaoRegistro> sessoes = new ConcurrentHashMap<>();
	private JdbcTemplate jdbc;

	public SessaoRepository() {
	}

	@Autowired
	public SessaoRepository(ObjectProvider<JdbcTemplate> jdbcProvider) {
		this.jdbc = jdbcProvider.getIfAvailable();
	}

	Optional<SessaoRegistro> buscarPorTokenHash(String tokenHash) {
		if (jdbc != null) {
			return jdbc.query("""
					SELECT s.*, c.nome, c.email, c.senha_hash, c.ativa
					FROM sessoes s
					JOIN contas c ON c.id = s.conta_id
					WHERE s.token_hash = ?
					""", (resultado, linha) -> new SessaoRegistro(
					resultado.getString("token_hash"),
					resultado.getString("csrf_token_hash"),
					new Conta(
							resultado.getObject("conta_id", UUID.class),
							resultado.getString("nome"),
							resultado.getString("email"),
							resultado.getString("senha_hash"),
							resultado.getBoolean("ativa")),
					resultado.getTimestamp("criada_em").toInstant(),
					java.time.Duration.ofSeconds(resultado.getLong("duracao_inatividade_segundos")),
					resultado.getTimestamp("expira_por_inatividade").toInstant(),
					resultado.getTimestamp("expira_em_definitivo").toInstant(),
					resultado.getBoolean("revogada")), tokenHash).stream().findFirst();
		}
		return Optional.ofNullable(sessoes.get(tokenHash));
	}

	void salvar(SessaoRegistro sessao) {
		if (jdbc != null) {
			jdbc.update("""
					INSERT INTO sessoes (
					    token_hash, csrf_token_hash, conta_id, criada_em, duracao_inatividade_segundos,
					    expira_por_inatividade, expira_em_definitivo, revogada
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					ON CONFLICT (token_hash) DO UPDATE SET
					    csrf_token_hash = EXCLUDED.csrf_token_hash,
					    expira_por_inatividade = EXCLUDED.expira_por_inatividade,
					    revogada = EXCLUDED.revogada
					""",
					sessao.tokenHash(),
					sessao.csrfTokenHash(),
					sessao.conta().getId(),
					java.sql.Timestamp.from(sessao.criadaEm()),
					sessao.duracaoInatividade().toSeconds(),
					java.sql.Timestamp.from(sessao.expiraPorInatividade()),
					java.sql.Timestamp.from(sessao.expiraEmDefinitivo()),
					sessao.revogada());
			return;
		}
		sessoes.put(sessao.tokenHash(), sessao);
	}

	void revogarDaConta(UUID contaId) {
		if (jdbc != null) {
			jdbc.update("UPDATE sessoes SET revogada = true WHERE conta_id = ?", contaId);
			return;
		}
		sessoes.values().stream()
				.filter(sessao -> sessao.conta().getId().equals(contaId))
				.forEach(SessaoRegistro::revogar);
	}

	void atualizar(SessaoRegistro sessao) {
		salvar(sessao);
	}
}
