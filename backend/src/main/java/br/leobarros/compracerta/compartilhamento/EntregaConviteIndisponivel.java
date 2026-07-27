package br.leobarros.compracerta.compartilhamento;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(
		name = "compra-certa.email.smtp.enabled",
		havingValue = "false",
		matchIfMissing = true)
class EntregaConviteIndisponivel implements EntregaConvite {
	@Override
	public void send(String email, String token) {
		throw new IllegalStateException("Serviço de entrega de convites indisponível.");
	}
}
