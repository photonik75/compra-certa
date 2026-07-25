package br.leobarros.compracerta.autenticacao.recuperacao;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TokenRecuperacaoRepository {

	private final Map<String, TokenRecuperacao> tokens = new ConcurrentHashMap<>();
	private JdbcTemplate jdbc;

	public TokenRecuperacaoRepository() {
	}

	@Autowired
	public TokenRecuperacaoRepository(ObjectProvider<JdbcTemplate> jdbcProvider) {
		this.jdbc = jdbcProvider.getIfAvailable();
	}

	void salvar(TokenRecuperacao token) {
		if (jdbc != null) {
			jdbc.update("""
					INSERT INTO tokens_recuperacao (token_hash, conta_id, expira_em, usado, invalidado)
					VALUES (?, ?, ?, ?, ?)
					ON CONFLICT (token_hash) DO UPDATE SET
					    usado = EXCLUDED.usado,
					    invalidado = EXCLUDED.invalidado
					""",
					token.hash(),
					token.conta().getId(),
					java.sql.Timestamp.from(token.expiraEm()),
					token.usado(),
					token.invalidado());
			return;
		}
		tokens.put(token.hash(), token);
	}

	Optional<TokenRecuperacao> buscar(String hash) {
		if (jdbc != null) {
			return jdbc.query("""
					SELECT t.*, c.nome, c.email, c.senha_hash, c.ativa
					FROM tokens_recuperacao t
					JOIN contas c ON c.id = t.conta_id
					WHERE t.token_hash = ?
					""", (resultado, linha) -> new TokenRecuperacao(
					resultado.getString("token_hash"),
					new Conta(
							resultado.getObject("conta_id", UUID.class),
							resultado.getString("nome"),
							resultado.getString("email"),
							resultado.getString("senha_hash"),
							resultado.getBoolean("ativa")),
					resultado.getTimestamp("expira_em").toInstant(),
					resultado.getBoolean("usado"),
					resultado.getBoolean("invalidado")), hash).stream().findFirst();
		}
		return Optional.ofNullable(tokens.get(hash));
	}

	void invalidarDaConta(UUID contaId) {
		if (jdbc != null) {
			jdbc.update("""
					UPDATE tokens_recuperacao
					SET invalidado = true
					WHERE conta_id = ? AND usado = false
					""", contaId);
			return;
		}
		tokens.values().stream()
				.filter(token -> token.conta().getId().equals(contaId) && !token.usado())
				.forEach(TokenRecuperacao::invalidar);
	}

	public boolean contemValor(String valor) {
		if (jdbc != null) {
			return Boolean.TRUE.equals(jdbc.queryForObject(
					"SELECT EXISTS(SELECT 1 FROM tokens_recuperacao WHERE token_hash = ?)",
					Boolean.class,
					valor));
		}
		return tokens.containsKey(valor);
	}

	public long quantidade() {
		if (jdbc != null) {
			return jdbc.queryForObject("SELECT count(*) FROM tokens_recuperacao", Long.class);
		}
		return tokens.size();
	}

	public Instant expiracaoDoToken(String token) {
		return buscar(HashSeguro.gerar(token))
				.map(TokenRecuperacao::expiraEm)
				.orElseThrow();
	}

	void atualizar(TokenRecuperacao token) {
		salvar(token);
	}
}
