package br.leobarros.compracerta.ciclodevida;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CicloVidaRepository {
	private final JdbcTemplate jdbc;
	CicloVidaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
	Optional<State> find(UUID listId, UUID accountId) {
		return jdbc.query(
				"SELECT id,proprietario_id,estado,versao FROM listas l WHERE id=? AND NOT excluida AND "
						+ "(proprietario_id=? OR EXISTS(SELECT 1 FROM participantes_lista p "
						+ "WHERE p.lista_id=l.id AND p.conta_id=?))",
				(rs, row) -> new State(
						rs.getObject("id", UUID.class), rs.getObject("proprietario_id", UUID.class),
						rs.getString("estado"), rs.getLong("versao")),
				listId, accountId, accountId).stream().findFirst();
	}
	int change(UUID listId, String status, Instant now, long version) {
		return jdbc.update(
				"UPDATE listas SET estado=?,concluida_em=?,atualizada_em=?,versao=versao+1 "
						+ "WHERE id=? AND NOT excluida AND versao=?",
				status, "COMPLETED".equals(status) ? Timestamp.from(now) : null,
				Timestamp.from(now), listId, version);
	}
	int delete(UUID listId, Instant now, long version) {
		var changed = jdbc.update(
				"UPDATE listas SET excluida=TRUE,atualizada_em=?,versao=versao+1 "
						+ "WHERE id=? AND NOT excluida AND versao=?",
				Timestamp.from(now), listId, version);
		if (changed == 1) {
			jdbc.update("DELETE FROM participantes_lista WHERE lista_id=?", listId);
			jdbc.update(
					"UPDATE convites_lista SET estado='CANCELLED',atualizado_em=?,versao=versao+1 "
							+ "WHERE lista_id=? AND estado='PENDING'",
					Timestamp.from(now), listId);
		}
		return changed;
	}
	record State(UUID id, UUID ownerId, String status, long version) {
	}
}
