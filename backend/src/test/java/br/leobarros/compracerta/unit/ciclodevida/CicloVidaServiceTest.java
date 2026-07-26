package br.leobarros.compracerta.ciclodevida;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import br.leobarros.compracerta.listas.ListaDtos.ListDetail;
import br.leobarros.compracerta.listas.ListaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CicloVidaServiceTest {
	private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID LIST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
	private static final Conta ACCOUNT = new Conta(ACCOUNT_ID, "Ana", "ana@example.com", "hash", true);
	@Mock CicloVidaRepository repository;
	@Mock IdempotenciaRepository idempotency;
	@Mock ListaService lists;
	@Mock ListaEventService events;
	private CicloVidaService service;

	@BeforeEach
	void setUp() {
		service = new CicloVidaService(Clock.fixed(NOW, ZoneOffset.UTC), repository, idempotency, lists, events);
		lenient().when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void beLife01ControllerExigeSessaoCsrfProprietarioVersaoChaveEStatus() throws Exception {
		var method = CicloVidaController.class.getMethod("change", String.class, String.class, String.class,
				String.class, UUID.class, CicloVidaController.ChangeStatus.class);
		assertThat(method.getParameters()).hasSize(6);
		assertThat(method.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class)).isNotNull();
	}

	@Test
	void beLife02ConcluirDefineDataServidorEPreservaRelacoesEItens() {
		when(repository.find(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new CicloVidaRepository.State(LIST_ID, ACCOUNT_ID, "ACTIVE", 1)));
		when(repository.change(LIST_ID, "COMPLETED", NOW, 1)).thenReturn(1);
		var detail = mock(ListDetail.class);
		when(detail.version()).thenReturn(2L);
		when(lists.buscar(ACCOUNT, LIST_ID)).thenReturn(detail);
		service.change(ACCOUNT, LIST_ID, "COMPLETED", 1, "key");
		verify(repository).change(LIST_ID, "COMPLETED", NOW, 1);
	}

	@Test
	void beLife03ReabrirRemoveDataEPreservaConteudoERelacoes() {
		when(repository.find(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new CicloVidaRepository.State(LIST_ID, ACCOUNT_ID, "COMPLETED", 2)));
		when(repository.change(LIST_ID, "ACTIVE", NOW, 2)).thenReturn(1);
		var detail = mock(ListDetail.class);
		when(detail.version()).thenReturn(3L);
		when(lists.buscar(ACCOUNT, LIST_ID)).thenReturn(detail);
		service.change(ACCOUNT, LIST_ID, "ACTIVE", 2, "key");
		verify(repository).change(LIST_ID, "ACTIVE", NOW, 2);
	}

	@Test
	void beLife04TransicaoIgualOuInvalidaNaoPublicaEvento() {
		when(repository.find(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new CicloVidaRepository.State(LIST_ID, ACCOUNT_ID, "ACTIVE", 1)));
		assertThatThrownBy(() -> service.change(ACCOUNT, LIST_ID, "ACTIVE", 1, "key"))
				.isInstanceOf(ApiException.class)
				.satisfies(error -> assertThat(((ApiException) error).code())
						.isEqualTo("INVALID_LIST_TRANSITION"));
		assertThatThrownBy(() -> service.change(ACCOUNT, LIST_ID, "UNKNOWN", 1, "key"))
				.isInstanceOf(ApiException.class);
		verify(events, never()).publish(any(), anyLong(), any(), any(), any());
	}

	@Test
	void beLife05ListaConcluidaRecusaMutacoesInclusiveConvite() throws Exception {
		var source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src/main/java/br/leobarros/compracerta/compartilhamento/CompartilhamentoService.java"));
		assertThat(source).contains("if (!\"ACTIVE\".equals(list.status())) completed()")
				.contains("if (accepting && \"COMPLETED\".equals(data.listStatus())) completed()");
	}

	@Test
	void beLife06ExcluirRevogaMembrosConvitesEAcesso() throws Exception {
		var source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src/main/java/br/leobarros/compracerta/ciclodevida/CicloVidaRepository.java"));
		assertThat(source).contains("excluida=TRUE").contains("DELETE FROM participantes_lista")
				.contains("UPDATE convites_lista SET estado='CANCELLED'");
	}

	@Test
	void beLife07ExclusaoPreservaHistoricoMasConsultasNaoRevelam() throws Exception {
		var source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src/main/java/br/leobarros/compracerta/ciclodevida/CicloVidaRepository.java"));
		assertThat(source).doesNotContain("DELETE FROM listas").contains("NOT excluida");
	}

	@Test
	void beLife08EstadoRelacoesEEventosSaoTransacionais() {
		for (var name : java.util.List.of("change", "delete")) {
			assertThat(java.util.Arrays.stream(CicloVidaService.class.getDeclaredMethods())
					.filter(method -> method.getName().equals(name)).findFirst().orElseThrow()
					.isAnnotationPresent(Transactional.class)).isTrue();
		}
	}

	@Test
	void beLife09IdempotenciaRepeteSemVersaoEventoENovaExclusaoNaoEncontra() {
		when(idempotency.replay(any(), any(), eq("same"), any()))
				.thenReturn(Optional.of(new IdempotenciaRepository.Result("x", LIST_ID, "DELETED")));
		service.delete(ACCOUNT, LIST_ID, 1, "same");
		verify(repository, never()).delete(any(), any(), anyLong());
		verify(events, never()).publish(any(), anyLong(), any(), any(), any());
	}

	@Test
	void beLife10ConcorrenciaParticipanteEAlheioSaoDistinguidos() {
		when(repository.find(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new CicloVidaRepository.State(LIST_ID, ACCOUNT_ID, "ACTIVE", 3)));
		assertThatThrownBy(() -> service.change(ACCOUNT, LIST_ID, "COMPLETED", 2, "key"))
				.satisfies(error -> assertThat(((ApiException) error).etag()).isEqualTo("\"3\""));
		var other = UUID.randomUUID();
		when(repository.find(LIST_ID, ACCOUNT_ID))
				.thenReturn(Optional.of(new CicloVidaRepository.State(LIST_ID, other, "ACTIVE", 3)));
		assertThatThrownBy(() -> service.delete(ACCOUNT, LIST_ID, 3, "other"))
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("FORBIDDEN"));
	}

	@Test
	void beLife11EventoIncluiVersaoEstadoOuRedirecionamento() throws Exception {
		var source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src/main/java/br/leobarros/compracerta/ciclodevida/CicloVidaService.java"));
		assertThat(source).contains("\"list.status.changed\"").contains("\"list.deleted\"")
				.contains("version + 1");
	}

	@Test
	void beLife12ArquiteturaIsolaControllerServiceRepositoryEPublicador() {
		assertThat(CicloVidaController.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(CicloVidaService.class));
		assertThat(CicloVidaService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(CicloVidaRepository.class));
		assertThat(CicloVidaService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(ListaEventService.class));
	}
}
