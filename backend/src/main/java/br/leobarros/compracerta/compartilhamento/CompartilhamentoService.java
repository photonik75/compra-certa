package br.leobarros.compracerta.compartilhamento;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.comum.Sha256;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.AcceptResult;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Invitation;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.ListAccess;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Membership;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.Preview;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.ShareResult;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoDtos.UserContact;
import br.leobarros.compracerta.compartilhamento.CompartilhamentoRepository.InvitationData;
import br.leobarros.compracerta.comum.ApiException;
import br.leobarros.compracerta.comum.ApiSupport;
import br.leobarros.compracerta.comum.IdempotenciaRepository;
import br.leobarros.compracerta.eventos.ListaEventService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompartilhamentoService {
	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Duration VALIDITY = Duration.ofDays(7);
	private final Clock clock;
	private final CompartilhamentoRepository repository;
	private final IdempotenciaRepository idempotency;
	private final GeradorIdentificadorService tokens;
	private final EntregaConvite delivery;
	private final ListaEventService events;
	CompartilhamentoService(
			Clock clock, CompartilhamentoRepository repository, IdempotenciaRepository idempotency,
			GeradorIdentificadorService tokens, EntregaConvite delivery, ListaEventService events) {
		this.clock = clock;
		this.repository = repository;
		this.idempotency = idempotency;
		this.tokens = tokens;
		this.delivery = delivery;
		this.events = events;
	}
	ListAccess access(Conta account, UUID listId) {
		var list = repository.list(listId, account.getId()).orElseThrow(ApiSupport::notFound);
		var owner = new Membership(
				new UserContact(list.ownerId(), list.ownerName(), list.ownerEmail()),
				"OWNER", null, list.version());
		return new ListAccess(listId, owner, repository.members(listId), repository.invitations(listId));
	}
	@Transactional
	ShareResult invite(Conta account, UUID listId, String rawEmail, String key) {
		var list = ownerActive(account, listId);
		var email = validateEmail(rawEmail);
		var replay = idempotency.replay(account.getId(), "INVITE_" + listId, key, email);
		if (replay.isPresent()) {
			if ("MEMBER_ADDED".equals(replay.orElseThrow().result())) {
				return new ShareResult(
						"MEMBER_ADDED",
						repository.member(listId, replay.orElseThrow().resourceId()).orElseThrow(),
						null);
			}
			return new ShareResult(
					"INVITATION_CREATED", null,
					repository.invitation(listId, replay.orElseThrow().resourceId())
							.orElseThrow().invitation());
		}
		if (list.ownerEmail().equals(email)) {
			throw new ApiException(
					HttpStatus.CONFLICT, "CANNOT_INVITE_OWNER",
					"Você já é o proprietário desta lista.");
		}
		var existingAccount = repository.account(email);
		if (existingAccount.isPresent()) {
			if (repository.member(listId, existingAccount.orElseThrow().id()).isPresent()) {
				throw new ApiException(
						HttpStatus.CONFLICT, "ALREADY_MEMBER",
						"Esta pessoa já participa da lista.");
			}
			idempotency.begin(account.getId(), "INVITE_" + listId, key, email);
			repository.addMember(listId, existingAccount.orElseThrow().id(), clock.instant());
			var version = repository.touch(listId, clock.instant());
			idempotency.finish(
					account.getId(), "INVITE_" + listId, key,
					existingAccount.orElseThrow().id(), "MEMBER_ADDED");
			var membership = repository.member(listId, existingAccount.orElseThrow().id()).orElseThrow();
			events.publish(listId, version, membership.user().id(), "list.access.changed", membership);
			return new ShareResult("MEMBER_ADDED", membership, null);
		}
		if (repository.pending(listId, email)) {
			throw new ApiException(
					HttpStatus.CONFLICT, "INVITATION_ALREADY_PENDING",
					"Já existe um convite pendente para este e-mail.");
		}
		var invitationId = UUID.randomUUID();
		var token = tokens.gerarToken();
		var now = clock.instant();
		idempotency.begin(account.getId(), "INVITE_" + listId, key, email);
		repository.createInvitation(
				invitationId, listId, email, Sha256.hex(token), now.plus(VALIDITY), now);
		try {
			delivery.send(email, token);
		} catch (RuntimeException exception) {
			repository.deliveryFailed(invitationId);
		}
		idempotency.finish(
				account.getId(), "INVITE_" + listId, key, invitationId, "INVITATION_CREATED");
		var invitation = repository.invitation(listId, invitationId).orElseThrow().invitation();
		return new ShareResult("INVITATION_CREATED", null, invitation);
	}
	@Transactional
	Invitation resend(
			Conta account, UUID listId, UUID invitationId, long version, String key) {
		ownerActive(account, listId);
		var content = invitationId + "|" + version;
		var replay = idempotency.replay(account.getId(), "INVITE_RESEND_" + invitationId, key, content);
		if (replay.isPresent()) {
			return repository.invitation(listId, invitationId).orElseThrow().invitation();
		}
		var data = repository.invitation(listId, invitationId).orElseThrow(ApiSupport::notFound);
		if (data.invitation().version() != version) throw ApiSupport.conflict(data.invitation().version());
		var token = tokens.gerarToken();
		var now = clock.instant();
		idempotency.begin(account.getId(), "INVITE_RESEND_" + invitationId, key, content);
		repository.resend(invitationId, Sha256.hex(token), now.plus(VALIDITY), now, version);
		try {
			delivery.send(data.invitation().email(), token);
		} catch (RuntimeException exception) {
			repository.deliveryFailed(invitationId);
		}
		idempotency.finish(
				account.getId(), "INVITE_RESEND_" + invitationId, key, invitationId, "RESENT");
		return repository.invitation(listId, invitationId).orElseThrow().invitation();
	}
	@Transactional
	void cancel(Conta account, UUID listId, UUID invitationId, long version, String key) {
		ownerActive(account, listId);
		var content = invitationId + "|" + version;
		if (idempotency.replay(
				account.getId(), "INVITE_CANCEL_" + invitationId, key, content).isPresent()) return;
		var invitation = repository.invitation(listId, invitationId)
				.orElseThrow(ApiSupport::notFound).invitation();
		if (invitation.version() != version) throw ApiSupport.conflict(invitation.version());
		idempotency.begin(account.getId(), "INVITE_CANCEL_" + invitationId, key, content);
		repository.cancel(invitationId, version);
		idempotency.finish(
				account.getId(), "INVITE_CANCEL_" + invitationId, key, invitationId, "CANCELLED");
	}
	Preview preview(String token) {
		var data = tokenData(token);
		validateInvitation(data, false);
		return new Preview(
				data.listName(), data.ownerName(), data.invitation().email(),
				data.invitation().status(), data.invitation().expiresAt(), true);
	}
	@Transactional
	AcceptResult accept(Conta account, String token, String key) {
		var data = tokenData(token);
		var content = Sha256.hex(token);
		var replay = idempotency.replay(account.getId(), "INVITE_ACCEPT", key, content);
		if (replay.isPresent()) {
			var membership = repository.member(
					replay.orElseThrow().resourceId(), account.getId()).orElseThrow();
			var list = repository.list(replay.orElseThrow().resourceId(), account.getId()).orElseThrow();
			return new AcceptResult(list.id(), list.name(), membership);
		}
		validateInvitation(data, true);
		if (!account.getEmail().equalsIgnoreCase(data.invitation().email())) {
			throw new ApiException(
					HttpStatus.FORBIDDEN, "INVITATION_EMAIL_MISMATCH",
					"Este convite foi enviado para outro e-mail. Entre com a conta correta para continuar.");
		}
		idempotency.begin(account.getId(), "INVITE_ACCEPT", key, content);
		repository.accept(data.invitation().id(), data.listId(), account.getId(), clock.instant());
		var version = repository.touch(data.listId(), clock.instant());
		idempotency.finish(
				account.getId(), "INVITE_ACCEPT", key, data.listId(), "ACCEPTED");
		var membership = repository.member(data.listId(), account.getId()).orElseThrow();
		events.publish(data.listId(), version, account.getId(), "list.access.changed", membership);
		return new AcceptResult(data.listId(), data.listName(), membership);
	}
	@Transactional
	void remove(
			Conta account, UUID listId, UUID userId, long version, String key, boolean self) {
		var list = repository.list(listId, account.getId()).orElseThrow(ApiSupport::notFound);
		if (!"ACTIVE".equals(list.status())) completed();
		if (self) {
			if (list.ownerId().equals(account.getId())) {
				throw new ApiException(
						HttpStatus.CONFLICT, "OWNER_CANNOT_LEAVE",
						"O proprietário não pode sair da própria lista.");
			}
			userId = account.getId();
		} else if (!list.ownerId().equals(account.getId())) {
			throw new ApiException(
					HttpStatus.FORBIDDEN, "FORBIDDEN",
					"Somente o proprietário pode remover participantes.");
		}
		if (list.ownerId().equals(userId)) {
			throw new ApiException(
					HttpStatus.CONFLICT, "CANNOT_REMOVE_OWNER",
					"O proprietário não pode ser removido.");
		}
		var content = listId + "|" + userId + "|" + version;
		var scope = "MEMBER_REMOVE_" + listId + "_" + userId;
		if (idempotency.replay(account.getId(), scope, key, content).isPresent()) return;
		var member = repository.member(listId, userId).orElseThrow(ApiSupport::notFound);
		if (member.version() != version) throw ApiSupport.conflict(member.version());
		idempotency.begin(account.getId(), scope, key, content);
		repository.removeMember(listId, userId, version);
		var listVersion = repository.touch(listId, clock.instant());
		idempotency.finish(account.getId(), scope, key, userId, "REMOVED");
		events.publish(
				listId, listVersion, userId, "list.access.changed", java.util.Map.of("removedUserId", userId));
	}
	private CompartilhamentoRepository.ListState ownerActive(Conta account, UUID listId) {
		var list = repository.list(listId, account.getId()).orElseThrow(ApiSupport::notFound);
		if (!list.ownerId().equals(account.getId())) {
			throw new ApiException(
					HttpStatus.FORBIDDEN, "FORBIDDEN",
					"Somente o proprietário pode administrar participantes.");
		}
		if (!"ACTIVE".equals(list.status())) completed();
		return list;
	}
	private InvitationData tokenData(String token) {
		if (token == null || token.isBlank()) throw ApiSupport.notFound();
		return repository.invitationByToken(Sha256.hex(token)).orElseThrow(ApiSupport::notFound);
	}
	private void validateInvitation(InvitationData data, boolean accepting) {
		if ("EXPIRED".equals(data.invitation().status())) {
			throw new ApiException(
					HttpStatus.GONE, "INVITATION_EXPIRED",
					"Este convite expirou. Solicite um novo convite ao proprietário.");
		}
		if (!"PENDING".equals(data.invitation().status())) throw ApiSupport.notFound();
		if (accepting && "COMPLETED".equals(data.listStatus())) completed();
	}
	private String validateEmail(String raw) {
		var email = raw == null ? "" : raw.trim().toLowerCase();
		if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
			throw ApiSupport.validation("email", "Por favor, informe um e-mail válido.");
		}
		return email;
	}
	private void completed() {
		throw new ApiException(
				HttpStatus.CONFLICT, "LIST_COMPLETED",
				"Esta lista está concluída e não pode ser alterada.");
	}
}
