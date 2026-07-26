package br.leobarros.compracerta.listas;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.comum.Sha256;
import br.leobarros.compracerta.autenticacao.comum.idempotencia.ChaveIdempotenciaReutilizadaException;
import br.leobarros.compracerta.listas.ListaDtos.CreateListRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.leobarros.compracerta.listas.ListaTestFixtures.AGORA;
import static br.leobarros.compracerta.listas.ListaTestFixtures.CONTA;
import static br.leobarros.compracerta.listas.ListaTestFixtures.CONTA_ID;
import static br.leobarros.compracerta.listas.ListaTestFixtures.LISTA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaServiceTest {

	@Mock
	private ListaRepository repository;
	private ListaService service;

	@BeforeEach
	void setUp() {
		service = new ListaService(Clock.fixed(AGORA, ZoneOffset.UTC), repository);
	}

	@Test
	void beLis02ServiceDelegaVisibilidadeDaContaAutenticadaAoRepository() {
		when(repository.listar(CONTA_ID, "ACTIVE", null, null, null, 30))
				.thenReturn(ListaTestFixtures.colecao(List.of(), false));
		service.listar(CONTA, null, null, null, null);
		verify(repository).listar(CONTA_ID, "ACTIVE", null, null, null, 30);
	}

	@Test
	void beLis04CursorRejeitaFiltrosDiferentesEPreservaResumoGlobal() {
		var card = ListaTestFixtures.cartao(LISTA_ID, AGORA);
		when(repository.listar(CONTA_ID, "ACTIVE", "feira", null, null, 1))
				.thenReturn(ListaTestFixtures.colecao(List.of(card), true));
		var primeira = service.listar(CONTA, "ACTIVE", "feira", null, 1);
		assertThat(primeira.page().nextCursor()).isNotBlank();
		assertThatThrownBy(() -> service.listar(
				CONTA, "COMPLETED", "feira", primeira.page().nextCursor(), 1))
				.isInstanceOf(ListaExceptions.Validacao.class);
	}

	@Test
	void beLis06CriaListaNormalizadaAtivaVaziaDoUsuario() {
		when(repository.buscarIdempotencia(CONTA_ID, "create-1")).thenReturn(Optional.empty());
		when(repository.iniciarIdempotencia(eq(CONTA_ID), eq("create-1"), any())).thenReturn(true);
		when(repository.buscarAcessivel(any(), eq(CONTA_ID)))
				.thenAnswer(call -> Optional.of(ListaTestFixtures.detalhe(
						"Compras do mês", "Casa", "ACTIVE", "OWNER", 1)));
		service.criar(CONTA, new CreateListRequest("  Compras   do mês ", "Casa"), "create-1");
		var nome = ArgumentCaptor.forClass(String.class);
		verify(repository).criar(any(), eq(CONTA_ID), nome.capture(), eq("Casa"), eq(AGORA));
		assertThat(nome.getValue()).isEqualTo("Compras do mês");
		assertThatThrownBy(() -> service.criar(
				CONTA, new CreateListRequest("x".repeat(61), null), "create-2"))
				.isInstanceOf(ListaExceptions.Validacao.class);
	}

	@Test
	void beLis07UnicidadeConsideraSomenteListasPropriasNaoExcluidas() {
		when(repository.buscarIdempotencia(CONTA_ID, "create-1")).thenReturn(Optional.empty());
		when(repository.iniciarIdempotencia(eq(CONTA_ID), eq("create-1"), any())).thenReturn(true);
		when(repository.nomeEmUso(CONTA_ID, "Viagem", null)).thenReturn(true);
		assertThatThrownBy(() -> service.criar(
				CONTA, new CreateListRequest("Viagem", null), "create-1"))
				.isInstanceOf(ListaExceptions.NomeEmUso.class);
		verify(repository, never()).criar(any(), any(), any(), any(), any());
	}

	@Test
	void beLis08RepeteResultadoERejeitaChaveComCargaDiferente() {
		var fingerprint = Sha256.hex("Mercado\nnull");
		when(repository.buscarIdempotencia(CONTA_ID, "same"))
				.thenReturn(Optional.of(new ListaRepository.Idempotencia(fingerprint, LISTA_ID)));
		when(repository.buscarAcessivel(LISTA_ID, CONTA_ID))
				.thenReturn(Optional.of(ListaTestFixtures.detalhe(
						"Mercado", null, "ACTIVE", "OWNER", 1)));
		assertThat(service.criar(CONTA, new CreateListRequest("Mercado", null), "same").id())
				.isEqualTo(LISTA_ID);
		assertThatThrownBy(() -> service.criar(
				CONTA, new CreateListRequest("Feira", null), "same"))
				.isInstanceOf(ChaveIdempotenciaReutilizadaException.class);
	}

	@Test
	void beLis11EditaSomenteProprietarioDeListaAtivaEAceitaDescricaoNula() {
		when(repository.buscarAcessivel(LISTA_ID, CONTA_ID))
				.thenReturn(Optional.of(ListaTestFixtures.detalhe(
						"Mercado", "Casa", "ACTIVE", "OWNER", 1)))
				.thenReturn(Optional.of(ListaTestFixtures.detalhe(
						"Mercado", null, "ACTIVE", "OWNER", 2)));
		when(repository.atualizar(LISTA_ID, "Mercado", null, AGORA, 1)).thenReturn(1);
		service.atualizar(CONTA, LISTA_ID, 1L, false, null, true, null);
		verify(repository).nomeEmUso(CONTA_ID, "Mercado", LISTA_ID);
		verify(repository).atualizar(LISTA_ID, "Mercado", null, AGORA, 1);
	}

	@Test
	void beLis12AtualizaSomenteCamposEnviadosERejeitaAusenciaDeMudanca() {
		when(repository.buscarAcessivel(LISTA_ID, CONTA_ID))
				.thenReturn(Optional.of(ListaTestFixtures.detalhe(
						"Mercado", "Casa", "ACTIVE", "OWNER", 1)));
		assertThatThrownBy(() -> service.atualizar(
				CONTA, LISTA_ID, 1L, false, null, false, null))
				.isInstanceOf(ListaExceptions.Validacao.class);
		assertThatThrownBy(() -> service.atualizar(
				CONTA, LISTA_ID, 1L, true, "Mercado", false, null))
				.isInstanceOf(ListaExceptions.Validacao.class);
		verify(repository, never()).atualizar(any(), any(), any(), any(), eq(1L));
	}

	@Test
	void beLis13VersaoAntigaNaoPersisteEInformaVersaoAtual() {
		when(repository.buscarAcessivel(LISTA_ID, CONTA_ID))
				.thenReturn(Optional.of(ListaTestFixtures.detalhe(
						"Mercado", null, "ACTIVE", "OWNER", 3)));
		assertThatThrownBy(() -> service.atualizar(
				CONTA, LISTA_ID, 2L, true, "Feira", false, null))
				.isInstanceOf(ListaExceptions.Conflito.class)
				.satisfies(error -> assertThat(((ListaExceptions.Conflito) error).versao()).isEqualTo(3));
		verify(repository, never()).atualizar(any(), any(), any(), any(), any(Long.class));
	}
}
