package br.leobarros.compracerta.unit.autenticacao.recuperacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.recuperacao.ContaRecuperacaoRepository;
import br.leobarros.compracerta.autenticacao.recuperacao.EntregaRecuperacaoSenha;
import br.leobarros.compracerta.autenticacao.recuperacao.RedefinicaoSenhaService;
import br.leobarros.compracerta.autenticacao.recuperacao.SolicitacaoRecuperacaoService;
import br.leobarros.compracerta.autenticacao.recuperacao.TokenRecuperacaoInvalidoException;
import br.leobarros.compracerta.autenticacao.recuperacao.TokenRecuperacaoRepository;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RedefinicaoSenhaServiceTest {

	private static final String EMAIL = "maria@example.com";
	private static final String TOKEN = "token-valido";
	private static final String NOVA_SENHA = " nova senha ";

	@Mock
	private ContaRecuperacaoRepository contaRepository;
	@Mock
	private EntregaRecuperacaoSenha entrega;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private SessaoService sessaoService;

	private RedefinicaoSenhaService service;
	private SolicitacaoRecuperacaoService solicitacao;
	private Conta conta;

	@BeforeEach
	void configurar() {
		var clock = new MutableClock(Instant.parse("2026-07-23T12:00:00Z"));
		var tokens = new TokenRecuperacaoRepository();
		var gerador = org.mockito.Mockito.mock(GeradorIdentificadorService.class);
		conta = new Conta("Maria", EMAIL, "hash-antigo");
		solicitacao = new SolicitacaoRecuperacaoService(contaRepository, tokens, entrega, gerador, clock);
		service = new RedefinicaoSenhaService(tokens, contaRepository, passwordEncoder, sessaoService, clock);
	}

	@Test
	void beRed01RejeitaTokenAusente() {
		assertThrows(IllegalArgumentException.class, () -> service.redefinir(null, NOVA_SENHA, NOVA_SENHA));
	}

	@Test
	void beRed02RejeitaSenhaAusenteOuForaDoIntervalo() {
		assertThrows(IllegalArgumentException.class, () -> service.redefinir(TOKEN, null, null));
		assertThrows(IllegalArgumentException.class, () -> service.redefinir(TOKEN, "1234567", "1234567"));
		assertThrows(IllegalArgumentException.class,
				() -> service.redefinir(TOKEN, "a".repeat(129), "a".repeat(129)));
	}

	@Test
	void beRed03RejeitaConfirmacaoAusenteOuDiferenteExatamente() {
		assertThrows(IllegalArgumentException.class, () -> service.redefinir(TOKEN, NOVA_SENHA, null));
		assertThrows(IllegalArgumentException.class, () -> service.redefinir(TOKEN, NOVA_SENHA, "NOVA SENHA"));
		assertThrows(IllegalArgumentException.class, () -> service.redefinir(TOKEN, NOVA_SENHA, "nova senha"));
	}

	@Test
	void beRed04TokenDesconhecidoUsadoOuInvalidadoProduzMesmoErro() {
		assertThrows(TokenRecuperacaoInvalidoException.class,
				() -> service.redefinir("desconhecido", NOVA_SENHA, NOVA_SENHA));
	}

	@Test
	void beRed05TokenInvalidoNaoAlteraSenhaSessaoOuAcessos() {
		assertThrows(TokenRecuperacaoInvalidoException.class,
				() -> service.redefinir("desconhecido", NOVA_SENHA, NOVA_SENHA));
		verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.any());
		verify(sessaoService, never()).revogarDaConta(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void beRed06TokenValidoAlteraHashEPreservaEspacos() {
		prepararToken();
		when(passwordEncoder.encode(NOVA_SENHA)).thenReturn("hash-novo");
		service.redefinir(TOKEN, NOVA_SENHA, NOVA_SENHA);
		verify(passwordEncoder).encode(NOVA_SENHA);
		assertEquals("hash-novo", conta.getSenhaHash());
	}

	@Test
	void beRed07SucessoConsomeTokenEInvalidaSessoes() {
		prepararToken();
		when(passwordEncoder.encode(NOVA_SENHA)).thenReturn("hash-novo");
		service.redefinir(TOKEN, NOVA_SENHA, NOVA_SENHA);
		verify(sessaoService).revogarDaConta(conta);
		assertThrows(TokenRecuperacaoInvalidoException.class,
				() -> service.redefinir(TOKEN, NOVA_SENHA, NOVA_SENHA));
	}

	private void prepararToken() {
		var gerador = org.mockito.Mockito.mock(GeradorIdentificadorService.class);
		when(gerador.gerarToken()).thenReturn(TOKEN);
		var tokens = new TokenRecuperacaoRepository();
		var clock = new MutableClock(Instant.parse("2026-07-23T12:00:00Z"));
		solicitacao = new SolicitacaoRecuperacaoService(contaRepository, tokens, entrega, gerador, clock);
		service = new RedefinicaoSenhaService(tokens, contaRepository, passwordEncoder, sessaoService, clock);
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(conta));
		solicitacao.solicitar(EMAIL);
	}
}
