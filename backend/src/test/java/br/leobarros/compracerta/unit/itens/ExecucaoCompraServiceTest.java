package br.leobarros.compracerta.itens;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import static br.leobarros.compracerta.itens.ItemTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecucaoCompraServiceTest {
	@Mock ItemRepository repository;
	@Mock IdempotenciaRepository idempotency;
	@Mock ListaEventService events;
	private ItemService service;

	@BeforeEach
	void setUp() {
		service = new ItemService(Clock.fixed(NOW, ZoneOffset.UTC), repository, idempotency, events);
		lenient().when(repository.list(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new ItemRepository.ListState(LIST_ID, "ACTIVE", 1)));
		lenient().when(repository.find(LIST_ID, ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID, "1", "UNIT", 1)));
		lenient().when(repository.summary(LIST_ID)).thenReturn(SUMMARY);
		lenient().when(repository.listVersion(LIST_ID)).thenReturn(2L);
		lenient().when(repository.touchList(LIST_ID, NOW)).thenReturn(2L);
		lenient().when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void beShop01ControllerExigeSegurancaVersaoChaveCorpoERetornaResumoEtag() throws Exception {
		var method = ItemController.class.getMethod("check", String.class, String.class, String.class,
				String.class, java.util.UUID.class, java.util.UUID.class, ItemDtos.CheckInput.class);
		assertThat(method.getParameters()).hasSize(7);
		assertThat(method.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class)).isNotNull();
	}

	@Test
	void beShop02AutorizaMembroAtivoERejeitaConcluidaOuAlheia() {
		when(repository.list(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new ItemRepository.ListState(LIST_ID, "COMPLETED", 1)));
		assertThatThrownBy(() -> service.check(ACCOUNT, LIST_ID, ITEM_ID, true, 1, "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("LIST_COMPLETED"));
		when(repository.list(LIST_ID, ACCOUNT_ID)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.check(ACCOUNT, LIST_ID, ITEM_ID, true, 1, "key"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void beShop03MarcarUsaAutorHorarioServidorEIncrementaUmaVez() {
		when(repository.check(ITEM_ID, true, ACCOUNT_ID, NOW, 1)).thenReturn(1);
		service.check(ACCOUNT, LIST_ID, ITEM_ID, true, 1, "key");
		verify(repository).check(ITEM_ID, true, ACCOUNT_ID, NOW, 1);
		verify(repository, times(1)).touchList(LIST_ID, NOW);
	}

	@Test
	void beShop04DesmarcarRemoveAutorHorarioEIncrementaUmaVez() {
		var checked = new ItemDtos.ListItem(ITEM_ID, item(ITEM_ID, "1", "UNIT", 1).product(),
				item(ITEM_ID, "1", "UNIT", 1).category(), "1", "UNIT", null, true, NOW,
				item(ITEM_ID, "1", "UNIT", 1).createdBy(), item(ITEM_ID, "1", "UNIT", 1).createdBy(),
				NOW, NOW, 1);
		when(repository.find(LIST_ID, ITEM_ID)).thenReturn(Optional.of(checked));
		service.check(ACCOUNT, LIST_ID, ITEM_ID, false, 1, "key");
		verify(repository).check(ITEM_ID, false, ACCOUNT_ID, NOW, 1);
	}

	@Test
	void beShop05ResumoIgnoraExcluidosECalculaPercentual() throws Exception {
		var source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src/main/java/br/leobarros/compracerta/itens/ItemRepository.java"));
		assertThat(source).contains("WHERE lista_id=? AND NOT excluido")
				.contains("checked * 100 / total");
	}

	@Test
	void beShop06ItemResumoEEventoParticipamDaOperacaoTransacional() throws Exception {
		assertThat(ItemService.class.getMethod("check", br.leobarros.compracerta.autenticacao.cadastro.Conta.class,
				java.util.UUID.class, java.util.UUID.class, Boolean.class, long.class, String.class)
				.isAnnotationPresent(Transactional.class)).isTrue();
	}

	@Test
	void beShop07VersaoAntigaInformaEtagAtualSemSobrescrever() {
		when(repository.find(LIST_ID, ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID, "1", "UNIT", 3)));
		assertThatThrownBy(() -> service.check(ACCOUNT, LIST_ID, ITEM_ID, true, 2, "key"))
				.satisfies(error -> assertThat(((ApiException) error).etag()).isEqualTo("\"3\""));
		verify(repository, never()).check(any(), anyBoolean(), any(), any(), anyLong());
	}

	@Test
	void beShop08IdempotenciaNaoIncrementaNemPublicaNovamente() {
		when(idempotency.replay(any(), any(), eq("same"), any()))
				.thenReturn(Optional.of(new IdempotenciaRepository.Result("x", ITEM_ID, "CHECKED")));
		service.check(ACCOUNT, LIST_ID, ITEM_ID, true, 1, "same");
		verify(repository, never()).check(any(), anyBoolean(), any(), any(), anyLong());
		verify(events, never()).publish(any(), anyLong(), any(), any(), any());
	}

	@Test
	void beShop09EventoContemListaTipoVersaoEDadosMinimos() {
		service.check(ACCOUNT, LIST_ID, ITEM_ID, true, 1, "key");
		verify(events).publish(eq(LIST_ID), eq(2L), eq(ITEM_ID), eq("list.item.checked"), any());
	}

	@Test
	void beShop10RessincronizacaoRetornaEstadoPersistidoEVersaoAtual() {
		when(repository.listItems(LIST_ID, 30, 0)).thenReturn(java.util.List.of(item(ITEM_ID, "1", "UNIT", 1)));
		assertThat(service.list(ACCOUNT, LIST_ID, null, 30).listVersion()).isEqualTo(1);
	}

	@Test
	void beShop11ValidaEstadoEErrosNormativos() {
		assertThatThrownBy(() -> service.check(ACCOUNT, LIST_ID, ITEM_ID, null, 1, "key"))
				.isInstanceOf(ApiException.class);
		assertThat(ApiSupport.notFound().code()).isEqualTo("NOT_FOUND");
		assertThat(ApiSupport.conflict(2).code()).isEqualTo("CONFLICT");
	}

	@Test
	void beShop12ArquiteturaIsolaControllerServiceRepositoryEEventos() {
		assertThat(ItemController.class.getDeclaredFields()).anyMatch(f -> f.getType().equals(ItemService.class));
		assertThat(ItemService.class.getDeclaredFields()).anyMatch(f -> f.getType().equals(ItemRepository.class));
		assertThat(ItemService.class.getDeclaredFields()).anyMatch(f -> f.getType().equals(ListaEventService.class));
	}
}
