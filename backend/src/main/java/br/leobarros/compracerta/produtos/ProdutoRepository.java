package br.leobarros.compracerta.produtos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.produtos.ProdutoDtos.CategoryReference;
import br.leobarros.compracerta.produtos.ProdutoDtos.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ProdutoRepository {
	private static final String FIELDS = "SELECT p.*,NOT c.excluida categoria_disponivel "
			+ "FROM produtos p JOIN categorias c ON c.id=p.categoria_id ";
	private final JdbcTemplate jdbc;
	ProdutoRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
	List<Product> list(UUID accountId, String search, UUID categoryId, String status, int limit, int offset) {
		var sql = new StringBuilder(FIELDS).append("WHERE p.conta_id=? ");
		var args = new java.util.ArrayList<>();
		args.add(accountId);
		if (!"ALL".equals(status)) {
			sql.append("AND p.ativo=? ");
			args.add("ACTIVE".equals(status));
		}
		if (categoryId != null) {
			sql.append("AND p.categoria_id=? ");
			args.add(categoryId);
		}
		if (search != null) {
			sql.append("AND lower(translate(p.nome,'áàâãäéèêëíìîïóòôõöúùûüç',"
					+ "'aaaaaeeeeiiiiooooouuuuc')) LIKE ? ");
			args.add("%" + search + "%");
		}
		sql.append("ORDER BY lower(p.nome),p.id LIMIT ? OFFSET ?");
		args.add(limit + 1);
		args.add(offset);
		return jdbc.query(sql.toString(), this::map, args.toArray());
	}
	Optional<Product> find(UUID id, UUID accountId) {
		return jdbc.query(FIELDS + "WHERE p.id=? AND p.conta_id=?", this::map, id, accountId)
				.stream().findFirst();
	}
	Optional<CategoryReference> category(UUID id, UUID accountId) {
		return jdbc.query(
				"SELECT id,nome,icone,NOT excluida disponivel FROM categorias WHERE id=? AND conta_id=?",
				(rs, row) -> new CategoryReference(
						rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("icone"),
						rs.getBoolean("disponivel")),
				id, accountId).stream().findFirst();
	}
	boolean nameExists(UUID accountId, String name, UUID ignored) {
		var sql = "SELECT count(*) FROM produtos WHERE conta_id=? AND ativo AND "
				+ "lower(translate(nome,'áàâãäéèêëíìîïóòôõöúùûüç','aaaaaeeeeiiiiooooouuuuc'))=?"
				+ (ignored == null ? "" : " AND id<>?");
		var count = ignored == null
				? jdbc.queryForObject(sql, Integer.class, accountId, name)
				: jdbc.queryForObject(sql, Integer.class, accountId, name, ignored);
		return count != null && count > 0;
	}
	void create(UUID id, UUID accountId, ProdutoDtos.Input input, CategoryReference category, Instant now) {
		jdbc.update(
				"INSERT INTO produtos(id,conta_id,categoria_id,nome,unidade,categoria_nome,categoria_icone,"
						+ "criado_em,atualizado_em) VALUES (?,?,?,?,?,?,?,?,?)",
				id, accountId, input.categoryId(), input.name(), input.defaultUnit(), category.name(),
				category.icon(), Timestamp.from(now), Timestamp.from(now));
	}
	int update(UUID id, UUID accountId, ProdutoDtos.Input input, CategoryReference category, Instant now, long version) {
		return jdbc.update(
				"UPDATE produtos SET nome=?,categoria_id=?,unidade=?,categoria_nome=?,categoria_icone=?,"
						+ "atualizado_em=?,versao=versao+1 WHERE id=? AND conta_id=? AND ativo AND versao=?",
				input.name(), input.categoryId(), input.defaultUnit(), category.name(), category.icon(),
				Timestamp.from(now), id, accountId, version);
	}
	int deactivate(UUID id, UUID accountId, long version) {
		return jdbc.update(
				"UPDATE produtos SET ativo=FALSE,atualizado_em=now(),versao=versao+1 "
						+ "WHERE id=? AND conta_id=? AND ativo AND versao=?",
				id, accountId, version);
	}
	private Product map(ResultSet rs, int row) throws SQLException {
		return new Product(
				rs.getObject("id", UUID.class), rs.getString("nome"),
				new CategoryReference(
						rs.getObject("categoria_id", UUID.class), rs.getString("categoria_nome"),
						rs.getString("categoria_icone"), rs.getBoolean("categoria_disponivel")),
				rs.getString("unidade"), rs.getBoolean("ativo"), rs.getTimestamp("criado_em").toInstant(),
				rs.getTimestamp("atualizado_em").toInstant(), rs.getLong("versao"));
	}
}
