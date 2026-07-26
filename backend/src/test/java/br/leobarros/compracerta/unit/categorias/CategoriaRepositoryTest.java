package br.leobarros.compracerta.categorias;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.CATEGORIA_ID;
import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.CONTA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaRepositoryTest {

	@Mock
	private JdbcTemplate jdbc;

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void beCat03PesquisaNormalizadaLimitadaEOrdenacaoDeterministica() {
		when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
		new CategoriaRepository(jdbc).list(CONTA_ID, "acucar", null, null, 30);
		var sql = ArgumentCaptor.forClass(String.class);
		verify(jdbc).query(sql.capture(), any(RowMapper.class), any(Object[].class));
		assertThat(sql.getValue()).contains("translate").contains("ORDER BY lower(c.nome),c.id").contains("LIMIT ?");
	}

	@Test
	void beCat09AtualizaCategoriaEProdutosAtivosSemAlterarInativosOuItens() {
		when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
		new CategoriaRepository(jdbc).update(
				CATEGORIA_ID, CONTA_ID, "Pães", "🍞", CategoriaTestFixtures.AGORA, 1);
		var sql = ArgumentCaptor.forClass(String.class);
		verify(jdbc, org.mockito.Mockito.times(2)).update(sql.capture(), any(Object[].class));
		assertThat(sql.getAllValues().get(1)).contains("UPDATE produtos").contains("AND ativo");
		assertThat(sql.getAllValues()).noneMatch(value -> value.contains("itens"));
	}
}
