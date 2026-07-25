package br.leobarros.compracerta.autenticacao.recuperacao;

import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TokenRecuperacaoPostgresqlRepository extends TokenRecuperacaoRepository {

	private static final String BUSCAR = """
			SELECT t.*, c.nome, c.email, c.senha_hash, c.ativa
			FROM tokens_recuperacao t
			JOIN contas c ON c.id = t.conta_id
			WHERE t.token_hash = ?
			""";
	private static final String INVALIDAR_DA_CONTA = """
			UPDATE tokens_recuperacao
			SET invalidado = true
			WHERE conta_id = ? AND usado = false
			""";
	private static final String SALVAR = """
			INSERT INTO tokens_recuperacao (token_hash, conta_id, expira_em, usado, invalidado)
			VALUES (?, ?, ?, ?, ?)
			ON CONFLICT (token_hash) DO UPDATE SET
			    usado = EXCLUDED.usado,
			    invalidado = EXCLUDED.invalidado
			""";

	private final JdbcTemplate jdbc;

	public TokenRecuperacaoPostgresqlRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	void salvar(TokenRecuperacao token) {
		jdbc.update(
				SALVAR,
				token.hash(),
				token.conta().getId(),
				java.sql.Timestamp.from(token.expiraEm()),
				token.usado(),
				token.invalidado());
	}

	@Override
	Optional<TokenRecuperacao> buscar(String hash) {
		return jdbc.query(BUSCAR, (resultado, linha) -> mapear(resultado), hash).stream().findFirst();
	}

	@Override
	void invalidarDaConta(UUID contaId) {
		jdbc.update(INVALIDAR_DA_CONTA, contaId);
	}

	@Override
	public boolean contemValor(String valor) {
		return Boolean.TRUE.equals(jdbc.queryForObject(
				"SELECT EXISTS(SELECT 1 FROM tokens_recuperacao WHERE token_hash = ?)",
				Boolean.class,
				valor));
	}

	@Override
	public long quantidade() {
		return jdbc.queryForObject("SELECT count(*) FROM tokens_recuperacao", Long.class);
	}

	private TokenRecuperacao mapear(java.sql.ResultSet resultado) throws java.sql.SQLException {
		var conta = new Conta(
				resultado.getObject("conta_id", UUID.class),
				resultado.getString("nome"),
				resultado.getString("email"),
				resultado.getString("senha_hash"),
				resultado.getBoolean("ativa"));
		return new TokenRecuperacao(
				resultado.getString("token_hash"),
				conta,
				resultado.getTimestamp("expira_em").toInstant(),
				resultado.getBoolean("usado"),
				resultado.getBoolean("invalidado"));
	}
}
