package br.leobarros.compracerta.itens;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.itens.ItemDtos.CategorySnapshot;
import br.leobarros.compracerta.itens.ItemDtos.ListItem;
import br.leobarros.compracerta.itens.ItemDtos.ProductSnapshot;
import br.leobarros.compracerta.listas.ListaDtos.ListSummary;
import br.leobarros.compracerta.listas.ListaDtos.UserReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ItemRepository {
	private static final String FIELDS = "SELECT i.*,criador.nome criador_nome,marcador.nome marcador_nome "
			+ "FROM itens_lista i LEFT JOIN contas criador ON criador.id=i.criado_por "
			+ "LEFT JOIN contas marcador ON marcador.id=i.marcado_por ";
	private final JdbcTemplate jdbc;
	ItemRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
	Optional<ListState> list(UUID listId, UUID accountId) {
		return jdbc.query(
				"SELECT id,estado,versao FROM listas l WHERE id=? AND NOT excluida AND "
						+ "(proprietario_id=? OR EXISTS(SELECT 1 FROM participantes_lista p "
						+ "WHERE p.lista_id=l.id AND p.conta_id=?))",
				(rs, row) -> new ListState(
						rs.getObject("id", UUID.class), rs.getString("estado"), rs.getLong("versao")),
				listId, accountId, accountId).stream().findFirst();
	}
	List<ListItem> listItems(UUID listId, int limit, int offset) {
		return jdbc.query(
				FIELDS + "WHERE i.lista_id=? AND NOT i.excluido ORDER BY i.criado_em,i.id LIMIT ? OFFSET ?",
				this::map, listId, limit + 1, offset);
	}
	Optional<ListItem> find(UUID listId, UUID itemId) {
		return jdbc.query(
				FIELDS + "WHERE i.lista_id=? AND i.id=? AND NOT i.excluido",
				this::map, listId, itemId).stream().findFirst();
	}
	Optional<ListItem> duplicate(UUID listId, String productName, UUID ignored) {
		var sql = FIELDS + "WHERE i.lista_id=? AND NOT i.excluido AND "
				+ "lower(translate(i.produto_nome,'áàâãäéèêëíìîïóòôõöúùûüç','aaaaaeeeeiiiiooooouuuuc'))=?"
				+ (ignored == null ? "" : " AND i.id<>?") + " LIMIT 1";
		return (ignored == null
				? jdbc.query(sql, this::map, listId, productName)
				: jdbc.query(sql, this::map, listId, productName, ignored)).stream().findFirst();
	}
	Optional<ProductData> product(UUID productId, UUID categoryId, UUID accountId) {
		return jdbc.query(
				"SELECT p.id,p.nome,p.unidade,c.id categoria_id,c.nome categoria_nome,c.icone "
						+ "FROM produtos p JOIN categorias c ON c.id=? AND c.conta_id=? AND NOT c.excluida "
						+ "WHERE p.id=? AND p.conta_id=? AND p.ativo",
				(rs, row) -> new ProductData(
						rs.getObject("id", UUID.class), rs.getString("nome"),
						rs.getObject("categoria_id", UUID.class), rs.getString("categoria_nome"),
						rs.getString("icone")),
				categoryId, accountId, productId, accountId).stream().findFirst();
	}
	void create(UUID id, UUID listId, UUID actorId, InputData input, Instant now) {
		jdbc.update(
				"INSERT INTO itens_lista(id,lista_id,produto_id,produto_nome,categoria_id,categoria_nome,"
						+ "categoria_icone,quantidade,unidade,observacoes,criado_por,criado_em,atualizado_em) "
						+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
				id, listId, input.product().id(), input.product().name(), input.product().categoryId(),
				input.product().categoryName(), input.product().categoryIcon(), input.quantity(), input.unit(),
				input.notes(), actorId, Timestamp.from(now), Timestamp.from(now));
	}
	int update(UUID id, InputData input, Instant now, long version) {
		return jdbc.update(
				"UPDATE itens_lista SET produto_id=?,produto_nome=?,categoria_id=?,categoria_nome=?,"
						+ "categoria_icone=?,quantidade=?,unidade=?,observacoes=?,atualizado_em=?,versao=versao+1 "
						+ "WHERE id=? AND NOT excluido AND versao=?",
				input.product().id(), input.product().name(), input.product().categoryId(),
				input.product().categoryName(), input.product().categoryIcon(), input.quantity(), input.unit(),
				input.notes(), Timestamp.from(now), id, version);
	}
	int merge(UUID target, BigDecimal quantity, Instant now, long version) {
		return jdbc.update(
				"UPDATE itens_lista SET quantidade=quantidade+?,atualizado_em=?,versao=versao+1 "
						+ "WHERE id=? AND NOT excluido AND versao=?",
				quantity, Timestamp.from(now), target, version);
	}
	int delete(UUID id, long version) {
		return jdbc.update(
				"UPDATE itens_lista SET excluido=TRUE,atualizado_em=now(),versao=versao+1 "
						+ "WHERE id=? AND NOT excluido AND versao=?",
				id, version);
	}
	int check(UUID id, boolean checked, UUID actorId, Instant now, long version) {
		return jdbc.update(
				"UPDATE itens_lista SET marcado=?,marcado_em=?,marcado_por=?,atualizado_em=?,versao=versao+1 "
						+ "WHERE id=? AND NOT excluido AND versao=?",
				checked, checked ? Timestamp.from(now) : null, checked ? actorId : null,
				Timestamp.from(now), id, version);
	}
	long touchList(UUID listId, Instant now) {
		jdbc.update(
				"UPDATE listas SET atualizada_em=?,versao=versao+1 WHERE id=?",
				Timestamp.from(now), listId);
		return jdbc.queryForObject("SELECT versao FROM listas WHERE id=?", Long.class, listId);
	}
	long listVersion(UUID listId) {
		return jdbc.queryForObject("SELECT versao FROM listas WHERE id=?", Long.class, listId);
	}
	ListSummary summary(UUID listId) {
		return jdbc.queryForObject(
				"SELECT count(*) total,count(*) FILTER(WHERE marcado) checked FROM itens_lista "
						+ "WHERE lista_id=? AND NOT excluido",
				(rs, row) -> {
					var total = rs.getInt("total");
					var checked = rs.getInt("checked");
					return new ListSummary(
							total, checked, total - checked, total == 0 ? 0 : checked * 100 / total);
				}, listId);
	}
	private ListItem map(ResultSet rs, int row) throws SQLException {
		var markerId = rs.getObject("marcado_por", UUID.class);
		var creatorId = rs.getObject("criado_por", UUID.class);
		return new ListItem(
				rs.getObject("id", UUID.class),
				new ProductSnapshot(rs.getObject("produto_id", UUID.class), rs.getString("produto_nome")),
				new CategorySnapshot(
						rs.getObject("categoria_id", UUID.class), rs.getString("categoria_nome"),
						rs.getString("categoria_icone")),
				rs.getBigDecimal("quantidade").stripTrailingZeros().toPlainString(), rs.getString("unidade"),
				rs.getString("observacoes"), rs.getBoolean("marcado"), instant(rs, "marcado_em"),
				markerId == null ? null : new UserReference(markerId, rs.getString("marcador_nome")),
				creatorId == null ? null : new UserReference(creatorId, rs.getString("criador_nome")),
				rs.getTimestamp("criado_em").toInstant(), rs.getTimestamp("atualizado_em").toInstant(),
				rs.getLong("versao"));
	}
	private Instant instant(ResultSet rs, String field) throws SQLException {
		var value = rs.getTimestamp(field);
		return value == null ? null : value.toInstant();
	}
	record ListState(UUID id, String status, long version) {
	}
	record ProductData(UUID id, String name, UUID categoryId, String categoryName, String categoryIcon) {
	}
	record InputData(ProductData product, BigDecimal quantity, String unit, String notes) {
	}
}
