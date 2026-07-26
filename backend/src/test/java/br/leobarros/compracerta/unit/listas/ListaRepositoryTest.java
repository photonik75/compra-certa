package br.leobarros.compracerta.listas;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static br.leobarros.compracerta.listas.ListaTestFixtures.CONTA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaRepositoryTest {

	@Mock
	private JdbcTemplate jdbc;
	@Mock
	private NamedParameterJdbcTemplate namedJdbc;

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void beLis03CombinaFiltrosLimiteEOrdenacaoDeterministica() {
		when(namedJdbc.query(anyString(), any(Map.class), any(RowMapper.class))).thenReturn(List.of());
		when(namedJdbc.queryForObject(anyString(), any(Map.class), any(RowMapper.class)))
				.thenReturn(new ListaDtos.CollectionSummary(0, 0));
		var repository = new ListaPostgresqlRepository(jdbc, namedJdbc);
		repository.listar(CONTA_ID, "ACTIVE", "farmacia", null, null, 30);
		var sql = ArgumentCaptor.forClass(String.class);
		var parameters = ArgumentCaptor.forClass(Map.class);
		org.mockito.Mockito.verify(namedJdbc).query(sql.capture(), parameters.capture(), any(RowMapper.class));
		assertThat(sql.getValue())
				.contains("NOT l.excluida")
				.contains("l.estado=:estado")
				.contains("LIKE")
				.contains("ORDER BY l.atualizada_em DESC, l.id ASC")
				.contains("LIMIT :limite");
		assertThat(parameters.getValue()).containsEntry("limite", 31);
	}
}
