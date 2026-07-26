package br.leobarros.compracerta.listas;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.listas.ListaDtos.CollectionSummary;
import br.leobarros.compracerta.listas.ListaDtos.ListCard;
import br.leobarros.compracerta.listas.ListaDtos.ListCollection;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import br.leobarros.compracerta.listas.ListaDtos.ListSummary;
import br.leobarros.compracerta.listas.ListaDtos.PageInfo;
import br.leobarros.compracerta.listas.ListaDtos.UserReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ListaPostgresqlRepository implements ListaRepository {

	private static final String NORMALIZAR_SQL =
			"lower(translate(%s, 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇáàâãäéèêëíìîïóòôõöúùûüç', "
					+ "'AAAAAEEEEIIIIOOOOOUUUUCaaaaaeeeeiiiiooooouuuuc'))";
	private static final String ACESSO_SQL =
			"(l.proprietario_id = :contaId OR EXISTS (SELECT 1 FROM participantes_lista p "
					+ "WHERE p.lista_id = l.id AND p.conta_id = :contaId))";
	private static final String CAMPOS_SQL =
			"SELECT l.*, c.nome proprietario_nome, "
					+ "(SELECT count(*) FROM participantes_lista p WHERE p.lista_id=l.id) participantes, "
					+ "(SELECT count(*) FROM itens_lista i WHERE i.lista_id=l.id AND NOT i.excluido) total, "
					+ "(SELECT count(*) FROM itens_lista i WHERE i.lista_id=l.id AND NOT i.excluido AND i.marcado) marcados "
					+ "FROM listas l JOIN contas c ON c.id=l.proprietario_id ";

	private final JdbcTemplate jdbc;
	private final NamedParameterJdbcTemplate namedJdbc;

	ListaPostgresqlRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
		this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
	}

	@Override
	public ListCollection listar(
			UUID contaId,
			String status,
			String search,
			Instant cursorData,
			UUID cursorId,
			int limit) {
		var sql = new StringBuilder(CAMPOS_SQL).append("WHERE NOT l.excluida AND ").append(ACESSO_SQL);
		var parametros = new HashMap<String, Object>();
		parametros.put("contaId", contaId);
		if (!"ALL".equals(status)) {
			sql.append(" AND l.estado=:estado");
			parametros.put("estado", status);
		}
		if (search != null) {
			sql.append(" AND ").append(NORMALIZAR_SQL.formatted("l.nome"))
					.append(" LIKE '%' || ").append(NORMALIZAR_SQL.formatted(":pesquisa")).append(" || '%'");
			parametros.put("pesquisa", search);
		}
		if (cursorData != null) {
			sql.append(" AND (l.atualizada_em < :cursorData OR "
					+ "(l.atualizada_em=:cursorData AND l.id > :cursorId))");
			parametros.put("cursorData", Timestamp.from(cursorData));
			parametros.put("cursorId", cursorId);
		}
		sql.append(" ORDER BY l.atualizada_em DESC, l.id ASC LIMIT :limite");
		parametros.put("limite", limit + 1);
		var encontrados = namedJdbc.query(sql.toString(), parametros, (rs, row) -> mapearCartao(rs, contaId));
		var hasMore = encontrados.size() > limit;
		var itens = new ArrayList<>(encontrados.subList(0, Math.min(limit, encontrados.size())));
		return new ListCollection(itens, new PageInfo(null, hasMore), resumirAtivas(contaId));
	}

	@Override
	public CollectionSummary resumirAtivas(UUID contaId) {
		var sql = "SELECT count(DISTINCT l.id) listas, count(i.id) FILTER (WHERE NOT i.marcado) pendentes "
				+ "FROM listas l LEFT JOIN itens_lista i ON i.lista_id=l.id AND NOT i.excluido "
				+ "WHERE NOT l.excluida AND l.estado='ACTIVE' AND " + ACESSO_SQL;
		return namedJdbc.queryForObject(sql, java.util.Map.of("contaId", contaId), (rs, row) ->
				new CollectionSummary(rs.getInt("listas"), rs.getInt("pendentes")));
	}

	@Override
	public Optional<ListDetail> buscarAcessivel(UUID listaId, UUID contaId) {
		var sql = CAMPOS_SQL + "WHERE l.id=:id AND NOT l.excluida AND " + ACESSO_SQL;
		return namedJdbc.query(
				sql,
				java.util.Map.of("id", listaId, "contaId", contaId),
				(rs, row) -> mapearDetalhe(rs, contaId))
				.stream().findFirst();
	}

	@Override
	public boolean nomeEmUso(UUID contaId, String nome, UUID listaIgnorada) {
		var sql = "SELECT count(*) FROM listas WHERE proprietario_id=? AND NOT excluida AND "
				+ NORMALIZAR_SQL.formatted("nome") + "=" + NORMALIZAR_SQL.formatted("?")
				+ (listaIgnorada == null ? "" : " AND id<>?");
		var quantidade = listaIgnorada == null
				? jdbc.queryForObject(sql, Integer.class, contaId, nome)
				: jdbc.queryForObject(sql, Integer.class, contaId, nome, listaIgnorada);
		return quantidade != null && quantidade > 0;
	}

	@Override
	public void criar(UUID id, UUID contaId, String nome, String descricao, Instant agora) {
		jdbc.update(
				"INSERT INTO listas(id,proprietario_id,nome,descricao,estado,criada_em,atualizada_em) "
						+ "VALUES (?,?,?,?, 'ACTIVE', ?,?)",
				id, contaId, nome, descricao, Timestamp.from(agora), Timestamp.from(agora));
	}

	@Override
	public int atualizar(UUID id, String nome, String descricao, Instant agora, long versao) {
		return jdbc.update(
				"UPDATE listas SET nome=?,descricao=?,atualizada_em=?,versao=versao+1 WHERE id=? AND versao=?",
				nome, descricao, Timestamp.from(agora), id, versao);
	}

	@Override
	public Optional<Idempotencia> buscarIdempotencia(UUID contaId, String chave) {
		return jdbc.query(
				"SELECT fingerprint,lista_id FROM criacoes_lista_idempotentes "
						+ "WHERE conta_id=? AND chave=? FOR UPDATE",
				(rs, row) -> new Idempotencia(rs.getString("fingerprint"), rs.getObject("lista_id", UUID.class)),
				contaId, chave).stream().findFirst();
	}

	@Override
	public boolean iniciarIdempotencia(UUID contaId, String chave, String fingerprint) {
		return jdbc.update(
				"INSERT INTO criacoes_lista_idempotentes(conta_id,chave,fingerprint) VALUES (?,?,?) "
						+ "ON CONFLICT DO NOTHING",
				contaId, chave, fingerprint) == 1;
	}

	@Override
	public void concluirIdempotencia(UUID contaId, String chave, UUID listaId) {
		jdbc.update(
				"UPDATE criacoes_lista_idempotentes SET lista_id=? WHERE conta_id=? AND chave=?",
				listaId, contaId, chave);
	}

	private ListCard mapearCartao(ResultSet rs, UUID contaId) throws SQLException {
		var resumo = resumo(rs);
		return new ListCard(
				rs.getObject("id", UUID.class),
				rs.getString("nome"),
				rs.getString("estado"),
				papel(rs, contaId),
				proprietario(rs),
				rs.getInt("participantes") > 0,
				resumo,
				rs.getTimestamp("atualizada_em").toInstant(),
				instant(rs, "concluida_em"),
				rs.getLong("versao"));
	}

	private ListDetail mapearDetalhe(ResultSet rs, UUID contaId) throws SQLException {
		return new ListDetail(
				rs.getObject("id", UUID.class),
				rs.getString("nome"),
				rs.getString("descricao"),
				rs.getString("estado"),
				proprietario(rs),
				papel(rs, contaId),
				rs.getInt("participantes") > 0,
				resumo(rs),
				rs.getTimestamp("criada_em").toInstant(),
				rs.getTimestamp("atualizada_em").toInstant(),
				instant(rs, "concluida_em"),
				rs.getLong("versao"));
	}

	private ListSummary resumo(ResultSet rs) throws SQLException {
		var total = rs.getInt("total");
		var marcados = rs.getInt("marcados");
		return new ListSummary(total, marcados, total - marcados, total == 0 ? 0 : marcados * 100 / total);
	}

	private UserReference proprietario(ResultSet rs) throws SQLException {
		return new UserReference(rs.getObject("proprietario_id", UUID.class), rs.getString("proprietario_nome"));
	}

	private String papel(ResultSet rs, UUID contaId) throws SQLException {
		return rs.getObject("proprietario_id", UUID.class).equals(contaId) ? "OWNER" : "EDITOR";
	}

	private Instant instant(ResultSet rs, String coluna) throws SQLException {
		var timestamp = rs.getTimestamp(coluna);
		return timestamp == null ? null : timestamp.toInstant();
	}
}
