package br.leobarros.compracerta.compartilhamento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.leobarros.compracerta.listas.ListaDtos.ListCard;

public final class CompartilhamentoDtos {
	private CompartilhamentoDtos() {
	}
	public record EmailInput(String email) {
	}
	public record TokenInput(String token) {
	}
	public record UserContact(UUID id, String name, String email) {
	}
	public record Membership(UserContact user, String role, Instant joinedAt, long version) {
	}
	public record Invitation(
			UUID id, String email, String status, String deliveryStatus, Instant expiresAt,
			Instant createdAt, Instant updatedAt, long version) {
	}
	public record ListAccess(
			UUID listId, Membership owner, List<Membership> members, List<Invitation> invitations) {
	}
	public record ShareResult(String outcome, Membership membership, Invitation invitation) {
	}
	public record Preview(
			String listName, String ownerName, String invitedEmail, String status,
			Instant expiresAt, boolean requiresAuthentication) {
	}
	public record AcceptResult(UUID listId, String listName, Membership membership) {
	}
}
