package br.leobarros.compracerta.autenticacao.recuperacao;

public interface EntregaRecuperacaoSenha {

	void enviar(String email, String link);
}
