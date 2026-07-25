package br.leobarros.compracerta.e2e;

import org.junit.jupiter.api.Test;

class CadastroBackendE2ETest extends AutenticacaoBackendE2ESupport {

	@Test
	void beE2e02CadastroPersisteContaNormalizadaEAtiva() throws Exception {
		super.beE2e02CadastroPersisteContaNormalizadaEAtiva();
	}

	@Test
	void beE2e03CadastroPersisteSomenteHashForteDaSenha() throws Exception {
		super.beE2e03CadastroPersisteSomenteHashForteDaSenha();
	}

	@Test
	void beE2e04BancoGaranteUnicidadeConcorrenteDoEmailNormalizado() throws Exception {
		super.beE2e04BancoGaranteUnicidadeConcorrenteDoEmailNormalizado();
	}

	@Test
	void beE2e05FalhaAoCriarSessaoReverteCadastro() throws Exception {
		super.beE2e05FalhaAoCriarSessaoReverteCadastro();
	}

	@Test
	void beE2e06IdempotenciaDeCadastroSobreviveANovaRequisicao() throws Exception {
		super.beE2e06IdempotenciaDeCadastroSobreviveANovaRequisicao();
	}
}
