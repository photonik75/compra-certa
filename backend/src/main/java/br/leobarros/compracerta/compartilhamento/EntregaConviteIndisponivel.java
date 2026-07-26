package br.leobarros.compracerta.compartilhamento;

import org.springframework.stereotype.Service;

@Service
class EntregaConviteIndisponivel implements EntregaConvite {
	@Override
	public void send(String email, String token) {
		throw new IllegalStateException("Serviço de entrega de convites indisponível.");
	}
}
