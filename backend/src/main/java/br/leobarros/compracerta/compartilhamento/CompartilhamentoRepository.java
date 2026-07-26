package br.leobarros.compracerta.compartilhamento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Invitation;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Membership;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.UserContact;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CompartilhamentoRepository {
	private final JdbcTemplate jdbc;
	CompartilhamentoRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
	Optional<ListState> list(UUID id, UUID accountId) {
		return jdbc.query(
				"SELECT l.id,l.nome,l.estado,l.proprietario_id,l.versao,c.nome proprietario_nome,"
						+ "c.email proprietario_email FROM listas l JOIN contas c ON c.id=l.proprietario_id "
						+ "WHERE l.id=? AND NOT l.excluida AND (l.proprietario_id=? OR EXISTS("
						+ "SELECT 1 FROM participantes_lista p WHERE p.lista_id=l.id AND p.conta_id=?))",
				(rs, row) -> new ListState(
						rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("estado"),
						rs.getObject("proprietario_id", UUID.class), rs.getString("proprietario_nome"),
						rs.getString("proprietario_email"), rs.getLong("versao")),
				id, accountId, accountId).stream().findFirst();
	}
	Optional<Account> account(String email) {
		return jdbc.query(
				"SELECT id,nome,email FROM contas WHERE email=? AND ativa",
				(rs, row) -> new Account(
						rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("email")),
				email).stream().findFirst();
	}
	List<Membership> members(UUID listId) {
		return jdbc.query(
				"SELECT c.id,c.nome,c.email,p.entrou_em,p.versao FROM participantes_lista p "
						+ "JOIN contas c ON c.id=p.conta_id WHERE p.lista_id=? ORDER BY lower(c.nome),c.id",
				(rs, row) -> new Membership(
						new UserContact(
								rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("email")),
						"EDITOR", rs.getTimestamp("entrou_em").toInstant(), rs.getLong("versao")),
				listId);
	}
	Optional<Membership> member(UUID listId, UUID userId) {
		return members(listId).stream().filter(member -> member.user().id().equals(userId)).findFirst();
	}
	List<Invitation> invitations(UUID listId) {
		return jdbc.query(
				"SELECT * FROM convites_lista WHERE lista_id=? AND estado IN ('PENDING','EXPIRED') "
						+ "ORDER BY lower(email),id",
				this::mapInvitation, listId);
	}
	Optional<InvitationData> invitation(UUID listId, UUID invitationId) {
		return jdbc.query(
				"SELECT c.*,l.nome lista_nome,l.estado lista_estado,l.proprietario_id,o.nome proprietario_nome "
						+ "FROM convites_lista c JOIN listas l ON l.id=c.lista_id "
						+ "JOIN contas o ON o.id=l.proprietario_id WHERE c.id=? AND c.lista_id=? AND NOT l.excluida",
				this::mapInvitationData, invitationId, listId).stream().findFirst();
	}
	Optional<InvitationData> invitationByToken(String tokenHash) {
		return jdbc.query(
				"SELECT c.*,l.nome lista_nome,l.estado lista_estado,l.proprietario_id,o.nome proprietario_nome "
						+ "FROM convites_lista c JOIN listas l ON l.id=c.lista_id "
						+ "JOIN contas o ON o.id=l.proprietario_id "
						+ "WHERE c.token_hash=? AND NOT l.excluida",
				this::mapInvitationData, tokenHash).stream().findFirst();
	}
	boolean pending(UUID listId, String email) {
		var count = jdbc.queryForObject(
				"SELECT count(*) FROM convites_lista WHERE lista_id=? AND lower(email)=? AND estado='PENDING'",
				Integer.class, listId, email);
		return count != null && count > 0;
	}
	void addMember(UUID listId, UUID userId, Instant now) {
		jdbc.update(
				"INSERT INTO participantes_lista(lista_id,conta_id,entrou_em) VALUES (?,?,?) "
						+ "ON CONFLICT DO NOTHING",
				listId, userId, Timestamp.from(now));
	}
	void createInvitation(
			UUID id, UUID listId, String email, String tokenHash, Instant expires, Instant now) {
		jdbc.update(
				"INSERT INTO convites_lista(id,lista_id,email,token_hash,expira_em,criado_em,atualizado_em) "
						+ "VALUES (?,?,?,?,?,?,?)",
				id, listId, email, tokenHash, Timestamp.from(expires), Timestamp.from(now), Timestamp.from(now));
	}
	void resend(UUID id, String tokenHash, Instant expires, Instant now, long version) {
		jdbc.update(
				"UPDATE convites_lista SET token_hash=?,estado='PENDING',estado_entrega='SENT',expira_em=?,"
						+ "atualizado_em=?,versao=versao+1 WHERE id=? AND versao=?",
				tokenHash, Timestamp.from(expires), Timestamp.from(now), id, version);
	}
	void deliveryFailed(UUID id) {
		jdbc.update("UPDATE convites_lista SET estado_entrega='FAILED' WHERE id=?", id);
	}
	void cancel(UUID id, long version) {
		jdbc.update(
				"UPDATE convites_lista SET estado='CANCELLED',atualizado_em=now(),versao=versao+1 "
						+ "WHERE id=? AND versao=?",
				id, version);
	}
	void accept(UUID id, UUID listId, UUID accountId, Instant now) {
		addMember(listId, accountId, now);
		jdbc.update(
				"UPDATE convites_lista SET estado='ACCEPTED',atualizado_em=?,versao=versao+1 "
						+ "WHERE id=? AND estado='PENDING'",
				Timestamp.from(now), id);
	}
	int removeMember(UUID listId, UUID userId, long version) {
		return jdbc.update(
				"DELETE FROM participantes_lista WHERE lista_id=? AND conta_id=? AND versao=?",
				listId, userId, version);
	}
	long touch(UUID listId, Instant now) {
		jdbc.update(
				"UPDATE listas SET atualizada_em=?,versao=versao+1 WHERE id=?",
				Timestamp.from(now), listId);
		return jdbc.queryForObject("SELECT versao FROM listas WHERE id=?", Long.class, listId);
	}
	private Invitation mapInvitation(ResultSet rs, int row) throws SQLException {
		return new Invitation(
				rs.getObject("id", UUID.class), rs.getString("email"), effectiveStatus(rs),
				rs.getString("estado_entrega"), rs.getTimestamp("expira_em").toInstant(),
				rs.getTimestamp("criado_em").toInstant(), rs.getTimestamp("atualizado_em").toInstant(),
				rs.getLong("versao"));
	}
	private InvitationData mapInvitationData(ResultSet rs, int row) throws SQLException {
		return new InvitationData(
				mapInvitation(rs, row), rs.getObject("lista_id", UUID.class), rs.getString("lista_nome"),
				rs.getString("lista_estado"), rs.getObject("proprietario_id", UUID.class),
				rs.getString("proprietario_nome"));
	}
	private String effectiveStatus(ResultSet rs) throws SQLException {
		return "PENDING".equals(rs.getString("estado"))
				&& rs.getTimestamp("expira_em").toInstant().isBefore(Instant.now())
				? "EXPIRED" : rs.getString("estado");
	}
	record ListState(
			UUID id, String name, String status, UUID ownerId, String ownerName, String ownerEmail, long version) {
	}
	record Account(UUID id, String name, String email) {
	}
	record InvitationData(
			Invitation invitation, UUID listId, String listName, String listStatus,
			UUID ownerId, String ownerName) {
	}
}
