package br.leobarros.compracerta.produtos;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.produtos.ProdutoDtos.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.leobarros.compracerta.produtos.ProdutoTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
	@Mock ProdutoRepository repository;
	@Mock IdempotenciaRepository idempotency;
	private ProdutoService service;

	@BeforeEach
	void setUp() {
		service = new ProdutoService(Clock.fixed(NOW, ZoneOffset.UTC), repository, idempotency);
	}

	@Test
	void beProd04CursorVinculaTodosOsFiltros() {
		var product = product("Arroz", true, 1);
		when(repository.category(CATEGORY_ID, ACCOUNT_ID)).thenReturn(Optional.of(CATEGORY));
		when(repository.list(ACCOUNT_ID, "arroz", CATEGORY_ID, "ACTIVE", 1, 0))
				.thenReturn(List.of(product, product));
		var page = service.list(ACCOUNT, "Arroz", CATEGORY_ID, "ACTIVE", null, 1);
		assertThatThrownBy(() -> service.list(ACCOUNT, "Feijão", CATEGORY_ID, "ACTIVE",
				page.page().nextCursor(), 1)).isInstanceOf(ApiException.class);
	}

	@Test
	void beProd06ValidaCamposDuplicidadeAtivaEPermiteInativo() {
		when(idempotency.replay(eq(ACCOUNT_ID), any(), any(), any())).thenReturn(Optional.empty());
		when(repository.nameExists(ACCOUNT_ID, "arroz", null)).thenReturn(true);
		assertThatThrownBy(() -> service.create(ACCOUNT, new Input("Arroz", CATEGORY_ID, "UNIT"), "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code())
						.isEqualTo("PRODUCT_NAME_ALREADY_IN_USE"));
		assertThatThrownBy(() -> service.create(ACCOUNT, new Input("", CATEGORY_ID, "INVALID"), "other"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void beProd07IdempotenciaRepeteSemDuplicarERejeitaCargaDivergente() {
		when(idempotency.replay(ACCOUNT_ID, "PRODUCT_CREATE", "same", "Arroz|" + CATEGORY_ID + "|UNIT"))
				.thenReturn(Optional.of(new IdempotenciaRepository.Result("x", PRODUCT_ID, "CREATED")));
		when(repository.find(PRODUCT_ID, ACCOUNT_ID)).thenReturn(Optional.of(product("Arroz", true, 1)));
		assertThat(service.create(ACCOUNT, new Input("Arroz", CATEGORY_ID, "UNIT"), "same").id())
				.isEqualTo(PRODUCT_ID);
		verify(repository, never()).create(any(), any(), any(), any(), any());
		when(idempotency.replay(ACCOUNT_ID, "PRODUCT_CREATE", "same", "Feijão|" + CATEGORY_ID + "|UNIT"))
				.thenThrow(new ApiException(
						org.springframework.http.HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "reutilizada"));
		assertThatThrownBy(() -> service.create(ACCOUNT, new Input("Feijão", CATEGORY_ID, "UNIT"), "same"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void beProd08PatchParcialExigeMudancaEImpedeInativo() {
		when(repository.find(PRODUCT_ID, ACCOUNT_ID)).thenReturn(Optional.of(product("Arroz", true, 1)));
		assertThatThrownBy(() -> service.update(ACCOUNT, PRODUCT_ID, new Input(null, null, null), 1))
				.isInstanceOf(ApiException.class);
		when(repository.find(PRODUCT_ID, ACCOUNT_ID)).thenReturn(Optional.of(product("Arroz", false, 1)));
		assertThatThrownBy(() -> service.update(ACCOUNT, PRODUCT_ID, new Input("Feijão", null, null), 1))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("PRODUCT_INACTIVE"));
	}

	@Test
	void beProd10DistingueCategoriaAlheiaDeExcluida() {
		var unavailableId = java.util.UUID.randomUUID();
		when(repository.find(PRODUCT_ID, ACCOUNT_ID)).thenReturn(Optional.of(product("Arroz", true, 1)));
		when(repository.category(unavailableId, ACCOUNT_ID)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.update(
				ACCOUNT, PRODUCT_ID, new Input(null, unavailableId, null), 1))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("NOT_FOUND"));
		when(repository.category(unavailableId, ACCOUNT_ID))
				.thenReturn(Optional.of(new ProdutoDtos.CategoryReference(unavailableId, "X", "🥬", false)));
		assertThatThrownBy(() -> service.update(
				ACCOUNT, PRODUCT_ID, new Input("Feijão", unavailableId, null), 1))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("CATEGORY_UNAVAILABLE"));
	}

	@Test
	void beProd11DesativacaoIdempotentePreservaHistorico() {
		when(repository.find(PRODUCT_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(product("Arroz", true, 1)))
				.thenReturn(Optional.of(product("Arroz", false, 2)));
		when(repository.deactivate(PRODUCT_ID, ACCOUNT_ID, 1)).thenReturn(1);
		service.deactivate(ACCOUNT, PRODUCT_ID, 1);
		service.deactivate(ACCOUNT, PRODUCT_ID, 2);
		verify(repository, times(1)).deactivate(PRODUCT_ID, ACCOUNT_ID, 1);
	}

	@Test
	void beProd13ConflitoEIsolamentoNaoSobrescrevem() {
		when(repository.find(PRODUCT_ID, ACCOUNT_ID)).thenReturn(Optional.of(product("Arroz", true, 3)));
		assertThatThrownBy(() -> service.update(ACCOUNT, PRODUCT_ID, new Input("Feijão", null, null), 2))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).etag()).isEqualTo("\"3\""));
		verify(repository, never()).update(any(), any(), any(), any(), any(), anyLong());
	}
}
