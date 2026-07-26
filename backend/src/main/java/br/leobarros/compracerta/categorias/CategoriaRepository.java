package br.leobarros.compracerta.categorias;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.categorias.CategoriaDtos.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CategoriaRepository {

	private static final String FIELDS = "SELECT c.*, (SELECT count(*) FROM produtos p "
			+ "WHERE p.categoria_id=c.id AND p.ativo) produtos_ativos FROM categorias c ";
	private final JdbcTemplate jdbc;

	CategoriaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	List<Category> list(UUID accountId, String search, String afterName, UUID afterId, int limit) {
		var sql = new StringBuilder(FIELDS)
				.append("WHERE c.conta_id=? AND NOT c.excluida ");
		var args = new java.util.ArrayList<>();
		args.add(accountId);
		if (search != null) {
			sql.append("AND lower(translate(c.nome,'áàâãäéèêëíìîïóòôõöúùûüç',"
					+ "'aaaaaeeeeiiiiooooouuuuc')) LIKE ? ");
			args.add("%" + search + "%");
		}
		if (afterName != null) {
			sql.append("AND (lower(c.nome)>? OR (lower(c.nome)=? AND c.id>?)) ");
			args.add(afterName);
			args.add(afterName);
			args.add(afterId);
		}
		sql.append("ORDER BY lower(c.nome),c.id LIMIT ?");
		args.add(limit + 1);
		return jdbc.query(sql.toString(), this::map, args.toArray());
	}

	Optional<Category> find(UUID id, UUID accountId) {
		return jdbc.query(
				FIELDS + "WHERE c.id=? AND c.conta_id=? AND NOT c.excluida",
				this::map, id, accountId).stream().findFirst();
	}

	boolean nameExists(UUID accountId, String normalizedName, UUID ignored) {
		var sql = "SELECT count(*) FROM categorias WHERE conta_id=? AND NOT excluida "
				+ "AND lower(translate(nome,'áàâãäéèêëíìîïóòôõöúùûüç','aaaaaeeeeiiiiooooouuuuc'))=?"
				+ (ignored == null ? "" : " AND id<>?");
		var count = ignored == null
				? jdbc.queryForObject(sql, Integer.class, accountId, normalizedName)
				: jdbc.queryForObject(sql, Integer.class, accountId, normalizedName, ignored);
		return count != null && count > 0;
	}

	void create(UUID id, UUID accountId, String name, String icon, Instant now) {
		jdbc.update(
				"INSERT INTO categorias(id,conta_id,nome,icone,criada_em,atualizada_em) VALUES (?,?,?,?,?,?)",
				id, accountId, name, icon, Timestamp.from(now), Timestamp.from(now));
	}

	int update(UUID id, UUID accountId, String name, String icon, Instant now, long version) {
		var changed = jdbc.update(
				"UPDATE categorias SET nome=?,icone=?,atualizada_em=?,versao=versao+1 "
						+ "WHERE id=? AND conta_id=? AND NOT excluida AND versao=?",
				name, icon, Timestamp.from(now), id, accountId, version);
		if (changed == 1) {
			jdbc.update(
					"UPDATE produtos SET categoria_nome=?,categoria_icone=?,atualizado_em=?,versao=versao+1 "
							+ "WHERE categoria_id=? AND ativo",
					name, icon, Timestamp.from(now), id);
		}
		return changed;
	}

	int delete(UUID id, UUID accountId, long version) {
		return jdbc.update(
				"UPDATE categorias SET excluida=TRUE,atualizada_em=now(),versao=versao+1 "
						+ "WHERE id=? AND conta_id=? AND NOT excluida AND versao=?",
				id, accountId, version);
	}

	private Category map(ResultSet rs, int row) throws SQLException {
		return new Category(
				rs.getObject("id", UUID.class),
				rs.getString("nome"),
				rs.getString("icone"),
				rs.getInt("produtos_ativos"),
				rs.getTimestamp("criada_em").toInstant(),
				rs.getTimestamp("atualizada_em").toInstant(),
				rs.getLong("versao"));
	}
}
