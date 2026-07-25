package br.leobarros.compracerta.e2e;

import org.junit.jupiter.api.Test;

class RecuperacaoBackendE2ETest extends AutenticacaoBackendE2ESupport {

	@Test
	void beE2e11RecuperacaoPersisteHashInvalidaAnteriorEEntregaUmaVez() throws Exception {
		super.beE2e11RecuperacaoPersisteHashInvalidaAnteriorEEntregaUmaVez();
	}

	@Test
	void beE2e12RecuperacaoDeEmailInexistenteEIndistinguivelESemEfeito() throws Exception {
		super.beE2e12RecuperacaoDeEmailInexistenteEIndistinguivelESemEfeito();
	}

	@Test
	void beE2e13RedefinicaoAlteraSenhaConsomeTokenERevogaSessoesAtomicamente() throws Exception {
		super.beE2e13RedefinicaoAlteraSenhaConsomeTokenERevogaSessoesAtomicamente();
	}

	@Test
	void beE2e14FalhaNaRedefinicaoReverteSenhaTokenESessoes() throws Exception {
		super.beE2e14FalhaNaRedefinicaoReverteSenhaTokenESessoes();
	}

	@Test
	void beE2e15AposRedefinicaoSomenteNovaSenhaCriaSessao() throws Exception {
		super.beE2e15AposRedefinicaoSomenteNovaSenhaCriaSessao();
	}
}
