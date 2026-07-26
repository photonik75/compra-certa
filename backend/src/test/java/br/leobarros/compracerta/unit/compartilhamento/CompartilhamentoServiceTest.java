package br.leobarros.compracerta.compartilhamento;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Invitation;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Membership;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.UserContact;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
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
class CompartilhamentoServiceTest {
	private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID LIST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID INVITATION_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
	private static final Conta OWNER = new Conta(OWNER_ID, "Ana", "ana@example.com", "hash", true);
	private static final Conta USER = new Conta(USER_ID, "Bia", "bia@example.com", "hash", true);
	private static final CompartilhamentoRepository.ListState LIST = new CompartilhamentoRepository.ListState(
			LIST_ID, "Compras", "ACTIVE", OWNER_ID, "Ana", "ana@example.com", 1);
	private static final Membership MEMBER =
			new Membership(new UserContact(USER_ID, "Bia", "bia@example.com"), "EDITOR", NOW, 1);
	@Mock CompartilhamentoRepository repository;
	@Mock IdempotenciaRepository idempotency;
	@Mock GeradorIdentificadorService tokens;
	@Mock EntregaConvite delivery;
	@Mock ListaEventService events;
	private CompartilhamentoService service;

	@BeforeEach
	void setUp() {
		service = new CompartilhamentoService(
				Clock.fixed(NOW, ZoneOffset.UTC), repository, idempotency, tokens, delivery, events);
		lenient().when(repository.list(LIST_ID, OWNER_ID)).thenReturn(Optional.of(LIST));
		lenient().when(idempotency.replay(any(), any(), any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void beShare01AcessoOrdenadoNoStoreESemToken() throws Exception {
		when(repository.members(LIST_ID)).thenReturn(List.of(MEMBER));
		when(repository.invitations(LIST_ID)).thenReturn(List.of());
		assertThat(service.access(OWNER, LIST_ID).members()).containsExactly(MEMBER);
		var method = CompartilhamentoController.class.getMethod("access", String.class, UUID.class);
		assertThat(method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class)).isNotNull();
		assertThat(CompartilhamentoDtos.ListAccess.class.getRecordComponents())
				.noneMatch(component -> component.getName().toLowerCase().contains("token"));
	}

	@Test
	void beShare02ConviteExigeProprietarioAtivaNormalizaEmailEPersisteAntesDeEnviar() {
		when(repository.account("bia@example.com"))
				.thenReturn(Optional.of(new CompartilhamentoRepository.Account(USER_ID, "Bia", "bia@example.com")));
		when(repository.member(LIST_ID, USER_ID)).thenReturn(Optional.empty()).thenReturn(Optional.of(MEMBER));
		service.invite(OWNER, LIST_ID, " BIA@EXAMPLE.COM ", "key");
		var order = inOrder(repository, delivery);
		order.verify(repository).addMember(LIST_ID, USER_ID, NOW);
		verify(repository).account("bia@example.com");
	}

	@Test
	void beShare03ContaExistenteCriaVinculoEInexistenteConviteSeteDias() {
		when(repository.account("bia@example.com"))
				.thenReturn(Optional.of(new CompartilhamentoRepository.Account(USER_ID, "Bia", "bia@example.com")));
		when(repository.member(LIST_ID, USER_ID)).thenReturn(Optional.empty()).thenReturn(Optional.of(MEMBER));
		assertThat(service.invite(OWNER, LIST_ID, "bia@example.com", "member").outcome())
				.isEqualTo("MEMBER_ADDED");
		when(repository.account("nova@example.com")).thenReturn(Optional.empty());
		when(repository.pending(LIST_ID, "nova@example.com")).thenReturn(false);
		when(tokens.gerarToken()).thenReturn("token-seguro");
		var invitation = invitation("nova@example.com", "PENDING", 1);
		when(repository.invitation(eq(LIST_ID), any())).thenReturn(Optional.of(invitationData(invitation)));
		service.invite(OWNER, LIST_ID, "nova@example.com", "invite");
		verify(repository).createInvitation(any(), eq(LIST_ID), eq("nova@example.com"), any(),
				eq(NOW.plus(java.time.Duration.ofDays(7))), eq(NOW));
	}

	@Test
	void beShare04RejeitaProprietarioMembroEPendenteComCodigosEspecificos() {
		assertThatThrownBy(() -> service.invite(OWNER, LIST_ID, "ana@example.com", "owner"))
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("CANNOT_INVITE_OWNER"));
		when(repository.account("bia@example.com"))
				.thenReturn(Optional.of(new CompartilhamentoRepository.Account(USER_ID, "Bia", "bia@example.com")));
		when(repository.member(LIST_ID, USER_ID)).thenReturn(Optional.of(MEMBER));
		assertThatThrownBy(() -> service.invite(OWNER, LIST_ID, "bia@example.com", "member"))
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("ALREADY_MEMBER"));
	}

	@Test
	void beShare05FalhaEntregaMantemPendenteEReenvioTrocaTokenEValidade() {
		when(repository.account("nova@example.com")).thenReturn(Optional.empty());
		when(tokens.gerarToken()).thenReturn("primeiro");
		doThrow(new IllegalStateException("falha")).when(delivery).send(any(), any());
		when(repository.invitation(eq(LIST_ID), any()))
				.thenReturn(Optional.of(invitationData(invitation("nova@example.com", "PENDING", 1))));
		service.invite(OWNER, LIST_ID, "nova@example.com", "key");
		verify(repository).deliveryFailed(any());
	}

	@Test
	void beShare06CancelarExigePapelVersaoInvalidaTokenEPublicaMudanca() {
		var invitation = invitation("nova@example.com", "PENDING", 1);
		when(repository.invitation(LIST_ID, INVITATION_ID))
				.thenReturn(Optional.of(invitationData(invitation)));
		service.cancel(OWNER, LIST_ID, INVITATION_ID, 1, "key");
		verify(repository).cancel(INVITATION_ID, 1);
	}

	@Test
	void beShare07PreviewValidoNaoVazaDadosEInvalidoExpiradoTemCodigo() {
		when(repository.invitationByToken(any()))
				.thenReturn(Optional.of(invitationData(invitation("nova@example.com", "PENDING", 1))));
		assertThat(service.preview("token").listName()).isEqualTo("Compras");
		when(repository.invitationByToken(any()))
				.thenReturn(Optional.of(invitationData(invitation("nova@example.com", "EXPIRED", 1))));
		assertThatThrownBy(() -> service.preview("expired"))
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("INVITATION_EXPIRED"));
	}

	@Test
	void beShare08AceitarExigeEmailListaAtivaCriaVinculoAntesDeConsumirToken() throws Exception {
		var source = source("CompartilhamentoService.java");
		assertThat(source).contains("repository.accept").contains("idempotency.finish");
		assertThat(source.indexOf("repository.accept")).isLessThan(source.indexOf(
				"idempotency.finish", source.indexOf("repository.accept")));
	}

	@Test
	void beShare09RemoverParticipantePreservaAutoriaEProtegeProprietarioAlheio() {
		when(repository.member(LIST_ID, USER_ID)).thenReturn(Optional.of(MEMBER));
		service.remove(OWNER, LIST_ID, USER_ID, 1, "key", false);
		verify(repository).removeMember(LIST_ID, USER_ID, 1);
		assertThatThrownBy(() -> service.remove(OWNER, LIST_ID, OWNER_ID, 1, "owner", false))
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("CANNOT_REMOVE_OWNER"));
	}

	@Test
	void beShare10ParticipanteSaiEProprietarioNaoPodeSair() {
		when(repository.list(LIST_ID, USER_ID)).thenReturn(Optional.of(LIST));
		when(repository.member(LIST_ID, USER_ID)).thenReturn(Optional.of(MEMBER));
		service.remove(USER, LIST_ID, USER_ID, 1, "leave", true);
		verify(repository).removeMember(LIST_ID, USER_ID, 1);
		assertThatThrownBy(() -> service.remove(OWNER, LIST_ID, OWNER_ID, 1, "owner", true))
				.satisfies(error -> assertThat(((ApiException) error).code()).isEqualTo("OWNER_CANNOT_LEAVE"));
	}

	@Test
	void beShare11ServiceRevalidaMatrizDePermissoesEmTodaMutacao() throws Exception {
		var source = source("CompartilhamentoService.java");
		assertThat(source).contains("ownerActive(account, listId)").contains("repository.list(listId, account.getId())")
				.contains("LIST_COMPLETED").contains("FORBIDDEN");
	}

	@Test
	void beShare12TokenImprevisivelHasheadoSemLogsRespostasEventosEUsaFragmento() throws Exception {
		var source = source("CompartilhamentoService.java");
		assertThat(source).contains("tokens.gerarToken()").contains("Sha256.hex(token)")
				.doesNotContain("System.out").doesNotContain("logger");
		assertThat(CompartilhamentoDtos.Invitation.class.getRecordComponents())
				.noneMatch(component -> component.getName().toLowerCase().contains("token"));
	}

	@Test
	void beShare13MutacoesSaoAtomicasEIdempotentes() {
		for (var name : List.of("invite", "resend", "cancel", "accept", "remove")) {
			assertThat(java.util.Arrays.stream(CompartilhamentoService.class.getDeclaredMethods())
					.filter(method -> method.getName().equals(name)).findFirst().orElseThrow()
					.isAnnotationPresent(Transactional.class)).isTrue();
		}
	}

	@Test
	void beShare14EventosMinimosERevogacaoImediata() throws Exception {
		var source = source("CompartilhamentoService.java");
		assertThat(source).contains("\"list.access.changed\"").contains("\"removedUserId\"")
				.contains("repository.removeMember");
	}

	@Test
	void beShare15ArquiteturaIsolaControllerServiceRepositoryEmailEEventos() {
		assertThat(CompartilhamentoController.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(CompartilhamentoService.class));
		assertThat(CompartilhamentoService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(CompartilhamentoRepository.class));
		assertThat(CompartilhamentoService.class.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(EntregaConvite.class));
	}

	private Invitation invitation(String email, String status, long version) {
		return new Invitation(INVITATION_ID, email, status, "SENT", NOW.plusSeconds(604800), NOW, NOW, version);
	}

	private CompartilhamentoRepository.InvitationData invitationData(Invitation invitation) {
		return new CompartilhamentoRepository.InvitationData(
				invitation, LIST_ID, "Compras", "ACTIVE", OWNER_ID, "Ana");
	}

	private String source(String file) throws Exception {
		return java.nio.file.Files.readString(java.nio.file.Path.of(
				"src/main/java/br/leobarros/compracerta/compartilhamento/" + file));
	}
}
