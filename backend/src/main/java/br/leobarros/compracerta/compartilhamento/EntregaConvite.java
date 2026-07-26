package br.leobarros.compracerta.compartilhamento;

public interface EntregaConvite {
	void send(String email, String token);
}
