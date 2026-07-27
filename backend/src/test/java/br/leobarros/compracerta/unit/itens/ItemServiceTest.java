package br.leobarros.compracerta.itens;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.leobarros.compracerta.itens.ItemTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
	@Mock ItemRepository repository;
	@Mock IdempotenciaRepository idempotency;
	@Mock ListaEventService events;
	private ItemService service;

	@BeforeEach
	void setUp() {
		service = new ItemService(Clock.fixed(NOW, ZoneOffset.UTC), repository, idempotency, events);
		lenient().when(repository.list(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new ItemRepository.ListState(LIST_ID, "ACTIVE", 1)));
		lenient().when(repository.summary(LIST_ID)).thenReturn(SUMMARY);
		lenient().when(repository.listVersion(LIST_ID)).thenReturn(2L);
	}

	@Test
	void beItem02AcessoParticipanteEListaConcluida() {
		assertThat(service.access(ACCOUNT, LIST_ID).status()).isEqualTo("ACTIVE");
		when(repository.list(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new ItemRepository.ListState(LIST_ID, "COMPLETED", 1)));
		assertThatThrownBy(() -> service.create(ACCOUNT, LIST_ID, input("1", "UNIT", null), "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("LIST_COMPLETED"));
	}

	@Test
	void beItem04ValidaProdutoQuantidadeUnidadeCategoriaENota() {
		assertThatThrownBy(() -> service.create(ACCOUNT, LIST_ID, input("0", "UNIT", null), "key"))
				.isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.create(ACCOUNT, LIST_ID, input("1", "INVALID", null), "key"))
				.isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.create(ACCOUNT, LIST_ID,
				new ItemDtos.Input(PRODUCT_ID, "1", "UNIT", CATEGORY_ID, "x".repeat(241), null, null), "key"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void beItem05CriacaoCopiaSnapshotsEIniciaDesmarcado() {
		when(repository.product(PRODUCT_ID, CATEGORY_ID, ACCOUNT_ID)).thenReturn(Optional.of(PRODUCT));
		when(repository.duplicate(LIST_ID, "arroz", null)).thenReturn(Optional.empty());
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		when(repository.find(eq(LIST_ID), any())).thenReturn(Optional.of(item(ITEM_ID, "1", "UNIT", 1)));
		service.create(ACCOUNT, LIST_ID, input("1", "UNIT", null), "key");
		var data = org.mockito.ArgumentCaptor.forClass(ItemRepository.InputData.class);
		verify(repository).create(any(), eq(LIST_ID), eq(ACCOUNT_ID), data.capture(), eq(NOW));
		assertThat(data.getValue().product().name()).isEqualTo("Arroz");
		assertThat(data.getValue().product().categoryName()).isEqualTo("Mercearia");
		assertThat(item(ITEM_ID, "1", "UNIT", 1).checked()).isFalse();
	}

	@Test
	void beItem06DuplicidadeNormalizadaRetornaConflito() {
		when(repository.product(PRODUCT_ID, CATEGORY_ID, ACCOUNT_ID)).thenReturn(Optional.of(PRODUCT));
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		when(repository.duplicate(LIST_ID, "arroz", null))
				.thenReturn(Optional.of(item(TARGET_ID, "2", "UNIT", 1)));
		assertThatThrownBy(() -> service.create(ACCOUNT, LIST_ID, input("1", "UNIT", null), "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("DUPLICATE_ITEM"));
	}

	@Test
	void beItem07SomaSomenteUnidadesIguaisEDentroDoLimite() {
		when(repository.product(PRODUCT_ID, CATEGORY_ID, ACCOUNT_ID)).thenReturn(Optional.of(PRODUCT));
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		when(repository.duplicate(LIST_ID, "arroz", null))
				.thenReturn(Optional.of(item(TARGET_ID, "2", "KILOGRAM", 1)));
		assertThatThrownBy(() -> service.create(ACCOUNT, LIST_ID, input("1", "UNIT", "MERGE"), "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("INCOMPATIBLE_UNITS"));
		when(repository.duplicate(LIST_ID, "arroz", null))
				.thenReturn(Optional.of(item(TARGET_ID, "999999.99", "UNIT", 1)));
		assertThatThrownBy(() -> service.create(ACCOUNT, LIST_ID, input("0.01", "UNIT", "MERGE"), "limit"))
				.satisfies(error -> assertThat(((ApiException) error).code())
						.isEqualTo("QUANTITY_LIMIT_EXCEEDED"));
		when(repository.duplicate(LIST_ID, "arroz", null))
				.thenReturn(Optional.of(item(TARGET_ID, "2", "UNIT", 1)));
		when(repository.find(LIST_ID, TARGET_ID)).thenReturn(Optional.of(item(TARGET_ID, "3", "UNIT", 2)));
		service.create(ACCOUNT, LIST_ID, input("1", "UNIT", "MERGE"), "merge");
		verify(repository).merge(TARGET_ID, new java.math.BigDecimal("1"), NOW, 1);
	}

	@Test
	void beItem08PatchExigeVersaoMudancaEPreservaMarcacaoAutoria() {
		when(repository.find(LIST_ID, ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID, "1", "UNIT", 3)));
		assertThatThrownBy(() -> service.update(
				ACCOUNT, LIST_ID, ITEM_ID, input("2", "UNIT", null), 2, "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).etag()).isEqualTo("\"3\""));
		verify(repository, never()).update(any(), any(), any(), anyLong());
	}

	@Test
	void beItem09MesclagemAtualizaDestinoEExcluiOrigem() {
		when(repository.find(LIST_ID, ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID, "1", "UNIT", 1)));
		when(repository.find(LIST_ID, TARGET_ID)).thenReturn(Optional.of(item(TARGET_ID, "3", "UNIT", 2)));
		when(repository.product(PRODUCT_ID, CATEGORY_ID, ACCOUNT_ID)).thenReturn(Optional.of(PRODUCT));
		when(repository.duplicate(LIST_ID, "arroz", ITEM_ID))
				.thenReturn(Optional.of(item(TARGET_ID, "2", "UNIT", 2)));
		when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
		var request = new ItemDtos.Input(PRODUCT_ID, "1", "UNIT", CATEGORY_ID, null, "MERGE", 2L);
		service.update(ACCOUNT, LIST_ID, ITEM_ID, request, 1, "key");
		verify(repository).merge(TARGET_ID, new java.math.BigDecimal("1"), NOW, 2);
		verify(repository).delete(ITEM_ID, 1);
		verify(repository, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	void beItem10RemocaoIdempotenteRetornaResumoSemNovaMudanca() {
		when(idempotency.replay(ACCOUNT_ID, "ITEM_DELETE_" + ITEM_ID, "key",
				LIST_ID + "|" + ITEM_ID + "|1"))
				.thenReturn(Optional.of(new IdempotenciaRepository.Result("x", ITEM_ID, "DELETED")));
		assertThat(service.delete(ACCOUNT, LIST_ID, ITEM_ID, 1, "key").listSummary()).isEqualTo(SUMMARY);
		verify(repository, never()).delete(any(), anyLong());
	}

	@Test
	void beItem13IdempotenciaEConcorrenciaRetornamConflitosNormativos() {
		when(repository.find(LIST_ID, ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID, "1", "UNIT", 4)));
		assertThatThrownBy(() -> service.delete(ACCOUNT, LIST_ID, ITEM_ID, 3, "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).etag()).isEqualTo("\"4\""));
	}
}
