package br.leobarros.compracerta.autenticacao.cadastro;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import br.leobarros.compracerta.autenticacao.login.LoginContaRepository;
import br.leobarros.compracerta.autenticacao.recuperacao.ContaRecuperacaoRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContaPostgresqlRepository
		implements ContaRepository, LoginContaRepository, ContaRecuperacaoRepository {

	private static final String CONSULTA_POR_EMAIL = """
			SELECT id, nome, email, senha_hash, ativa
			FROM contas
			WHERE email = ?
			""";
	private static final String EXISTE_POR_EMAIL = "SELECT EXISTS(SELECT 1 FROM contas WHERE email = ?)";
	private static final String REMOVER = "DELETE FROM contas WHERE id = ?";
	private static final String SALVAR = """
			INSERT INTO contas (id, nome, email, senha_hash, ativa)
			VALUES (?, ?, ?, ?, ?)
			ON CONFLICT (id) DO UPDATE
			SET nome = EXCLUDED.nome,
			    email = EXCLUDED.email,
			    senha_hash = EXCLUDED.senha_hash,
			    ativa = EXCLUDED.ativa
			""";

	private final JdbcTemplate jdbcTemplate;

	public ContaPostgresqlRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean existePorEmail(String email) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXISTE_POR_EMAIL, Boolean.class, email));
	}

	@Override
	public Optional<Conta> buscarPorEmail(String email) {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject(CONSULTA_POR_EMAIL, this::mapear, email));
		} catch (EmptyResultDataAccessException exception) {
			return Optional.empty();
		}
	}

	@Override
	public void salvar(Conta conta) {
		jdbcTemplate.update(
				SALVAR,
				conta.getId(),
				conta.getNome(),
				conta.getEmail(),
				conta.getSenhaHash(),
				conta.isAtiva());
	}

	@Override
	public void remover(Conta conta) {
		jdbcTemplate.update(REMOVER, conta.getId());
	}

	private Conta mapear(ResultSet resultado, int numeroLinha) throws SQLException {
		return new Conta(
				resultado.getObject("id", java.util.UUID.class),
				resultado.getString("nome"),
				resultado.getString("email"),
				resultado.getString("senha_hash"),
				resultado.getBoolean("ativa"));
	}
}
