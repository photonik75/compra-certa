package br.leobarros.compracerta.categorias;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.categorias.CategoriaDtos.Input;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.AGORA;
import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.CATEGORIA_ID;
import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.CONTA;
import static br.leobarros.compracerta.categorias.CategoriaTestFixtures.CONTA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

	@Mock
	private CategoriaRepository repository;
	@Mock
	private IdempotenciaRepository idempotency;
	private CategoriaService service;

	@BeforeEach
	void setUp() {
		service = new CategoriaService(Clock.fixed(AGORA, ZoneOffset.UTC), repository, idempotency);
	}

	@Test
	void beCat04CursorVinculaPesquisaENaoRepeteRegistros() {
		var category = CategoriaTestFixtures.categoria("Água", "🥬", 0, 1);
		when(repository.list(CONTA_ID, "agua", null, null, 1)).thenReturn(List.of(category, category));
		var first = service.list(CONTA, "Água", null, 1);
		assertThat(first.page().hasMore()).isTrue();
		assertThatThrownBy(() -> service.list(CONTA, "outra", first.page().nextCursor(), 1))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("VALIDATION_ERROR"));
	}

	@Test
	void beCat06UnicidadeIsoladaPorUsuario() {
		when(idempotency.replay(eq(CONTA_ID), any(), any(), any())).thenReturn(Optional.empty());
		when(repository.nameExists(CONTA_ID, "padaria", null)).thenReturn(true);
		assertThatThrownBy(() -> service.create(CONTA, new Input("Padaria", "🍞"), "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code())
						.isEqualTo("CATEGORY_NAME_ALREADY_IN_USE"));
		verify(repository).nameExists(CONTA_ID, "padaria", null);
	}

	@Test
	void beCat07IdempotenciaRepeteSemDuplicarERejeitaOutraCarga() {
		var result = new IdempotenciaRepository.Result("fingerprint", CATEGORIA_ID, "CREATED");
		when(idempotency.replay(CONTA_ID, "CATEGORY_CREATE", "same", "Padaria|🍞"))
				.thenReturn(Optional.of(result));
		when(repository.find(CATEGORIA_ID, CONTA_ID))
				.thenReturn(Optional.of(CategoriaTestFixtures.categoria("Padaria", "🍞", 0, 1)));
		assertThat(service.create(CONTA, new Input("Padaria", "🍞"), "same").id()).isEqualTo(CATEGORIA_ID);
		verify(repository, never()).create(any(), any(), any(), any(), any());
		when(idempotency.replay(CONTA_ID, "CATEGORY_CREATE", "same", "Feira|🍞"))
				.thenThrow(new ApiException(
						org.springframework.http.HttpStatus.CONFLICT,
						"IDEMPOTENCY_KEY_REUSED", "Chave reutilizada."));
		assertThatThrownBy(() -> service.create(CONTA, new Input("Feira", "🍞"), "same"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
	}

	@Test
	void beCat08PatchParcialAceitaMudancaERejeitaAusencia() {
		when(repository.find(CATEGORIA_ID, CONTA_ID))
				.thenReturn(Optional.of(CategoriaTestFixtures.categoria("Padaria", "🍞", 0, 1)))
				.thenReturn(Optional.of(CategoriaTestFixtures.categoria("Pães", "🍞", 0, 2)));
		when(repository.update(CATEGORIA_ID, CONTA_ID, "Pães", "🍞", AGORA, 1)).thenReturn(1);
		assertThat(service.update(CONTA, CATEGORIA_ID, new Input("Pães", null), 1).name()).isEqualTo("Pães");
		assertThatThrownBy(() -> service.update(CONTA, CATEGORIA_ID, new Input(null, null), 2))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void beCat12IdAlheioOuExcluidoRetornaNotFound() {
		when(repository.find(CATEGORIA_ID, CONTA_ID)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.get(CONTA, CATEGORIA_ID))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("NOT_FOUND"));
	}

	@Test
	void beCat13VersaoAntigaNaoSobrescreveEInformaEtagAtual() {
		when(repository.find(CATEGORIA_ID, CONTA_ID))
				.thenReturn(Optional.of(CategoriaTestFixtures.categoria("Padaria", "🍞", 0, 3)));
		assertThatThrownBy(() -> service.update(CONTA, CATEGORIA_ID, new Input("Pães", null), 2))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).etag()).isEqualTo("\"3\""));
		verify(repository, never()).update(any(), any(), any(), any(), any(), any(Long.class));
	}

	@Test
	void beCat11ExclusaoProtegeCategoriaEmUsoESuportaCategoriaVazia() {
		when(repository.find(CATEGORIA_ID, CONTA_ID))
				.thenReturn(Optional.of(CategoriaTestFixtures.categoria("Padaria", "🍞", 2, 1)));
		assertThatThrownBy(() -> service.delete(CONTA, CATEGORIA_ID, 1))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("CATEGORY_IN_USE"))
				.hasMessageContaining("2 produto(s)");
		verify(repository, never()).delete(any(), any(), any(Long.class));
	}
}
