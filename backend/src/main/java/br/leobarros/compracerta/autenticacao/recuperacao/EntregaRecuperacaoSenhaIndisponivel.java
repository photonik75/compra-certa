package br.leobarros.compracerta.autenticacao.recuperacao;

import org.springframework.stereotype.Service;

@Service
public class EntregaRecuperacaoSenhaIndisponivel implements EntregaRecuperacaoSenha {

	private static final String MENSAGEM_INDISPONIVEL =
			"Não foi possível enviar as instruções. Tente novamente mais tarde.";

	@Override
	public void enviar(String email, String link) {
		throw new RecuperacaoSenhaException(MENSAGEM_INDISPONIVEL);
	}
}
