package br.leobarros.compracerta.e2e;

import org.junit.jupiter.api.Test;

class LoginSessaoBackendE2ETest extends AutenticacaoBackendE2ESupport {

	@Test
	void beE2e07LoginUsaContaPersistidaESemEnumeracaoDeEmail() throws Exception {
		super.beE2e07LoginUsaContaPersistidaESemEnumeracaoDeEmail();
	}

	@Test
	void beE2e08BloqueioDeLoginPersisteEntreRequisicoes() throws Exception {
		super.beE2e08BloqueioDeLoginPersisteEntreRequisicoes();
	}

	@Test
	void beE2e09SessaoPersistidaPodeSerConsultadaEmOutraRequisicao() throws Exception {
		super.beE2e09SessaoPersistidaPodeSerConsultadaEmOutraRequisicao();
	}

	@Test
	void beE2e10LogoutRevogaSomenteSessaoAtual() throws Exception {
		super.beE2e10LogoutRevogaSomenteSessaoAtual();
	}
}
