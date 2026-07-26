package br.leobarros.compracerta.itens;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static br.leobarros.compracerta.itens.ItemTestFixtures.LIST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemRepositoryTest {
	@Mock JdbcTemplate jdbc;

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void beItem11ResumoConsideraSomenteNaoExcluidos() {
		when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(LIST_ID)))
				.thenReturn(ItemTestFixtures.SUMMARY);
		new ItemRepository(jdbc).summary(LIST_ID);
		var sql = ArgumentCaptor.forClass(String.class);
		verify(jdbc).queryForObject(sql.capture(), any(RowMapper.class), eq(LIST_ID));
		assertThat(sql.getValue()).contains("NOT excluido").contains("FILTER(WHERE marcado)");
	}
}
