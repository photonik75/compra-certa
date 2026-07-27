package br.leobarros.compracerta.produtos;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static br.leobarros.compracerta.produtos.ProdutoTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoRepositoryTest {
	@Mock JdbcTemplate jdbc;

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void beProd02FiltraAtivosCombinaBuscaCategoriaEOrdenaEstavelmente() {
		when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
		new ProdutoRepository(jdbc).list(ACCOUNT_ID, "acucar", CATEGORY_ID, "ACTIVE", 30, 0);
		var sql = ArgumentCaptor.forClass(String.class);
		verify(jdbc).query(sql.capture(), any(RowMapper.class), any(Object[].class));
		assertThat(sql.getValue()).contains("p.ativo=?").contains("p.categoria_id=?")
				.contains("translate").contains("lower(p.nome),p.id");
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void beProd03SelecaoLimitaDezAtivosEOrdenaPorRelevanciaDeterministica() {
		when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
		new ProdutoRepository(jdbc).list(ACCOUNT_ID, "arroz", null, "ACTIVE", 10, 0);
		var arguments = ArgumentCaptor.forClass(Object[].class);
		var sql = ArgumentCaptor.forClass(String.class);
		verify(jdbc).query(sql.capture(), any(RowMapper.class), arguments.capture());
		assertThat(arguments.getValue()).contains(11, 0);
		assertThat(sql.getValue()).contains("CASE").contains("THEN 0").contains("THEN 1")
				.contains("ELSE 2").contains("lower(p.nome),p.id");
	}

	@Test
	void beProd09AtualizaCatalogoSemTocarSnapshotsDeItens() {
		when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
		new ProdutoRepository(jdbc).update(
				PRODUCT_ID, ACCOUNT_ID, new ProdutoDtos.Input("Feijão", CATEGORY_ID, "BAG"),
				CATEGORY, NOW, 1);
		var sql = ArgumentCaptor.forClass(String.class);
		verify(jdbc).update(sql.capture(), any(Object[].class));
		assertThat(sql.getValue()).contains("UPDATE produtos").doesNotContain("itens_lista");
	}
}
