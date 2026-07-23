package br.leobarros.compracerta.unit.autenticacao.recuperacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.recuperacao.ContaRecuperacaoRepository;
import br.leobarros.compracerta.autenticacao.recuperacao.EntregaRecuperacaoSenha;
import br.leobarros.compracerta.autenticacao.recuperacao.SolicitacaoRecuperacaoService;
import br.leobarros.compracerta.autenticacao.recuperacao.TokenRecuperacaoRepository;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolicitacaoRecuperacaoServiceTest {

	private static final String EMAIL = "maria@example.com";

	@Mock
	private ContaRecuperacaoRepository contaRepository;
	@Mock
	private EntregaRecuperacaoSenha entrega;

	private TokenRecuperacaoRepository tokenRepository;
	private SolicitacaoRecuperacaoService service;
	private Conta conta;

	@BeforeEach
	void configurar() {
		tokenRepository = new TokenRecuperacaoRepository();
		service = new SolicitacaoRecuperacaoService(
				contaRepository,
				tokenRepository,
				entrega,
				new GeradorIdentificadorService(),
				new MutableClock(Instant.parse("2026-07-23T12:00:00Z")));
		conta = new Conta("Maria", EMAIL, "hash");
	}

	@ParameterizedTest
	@MethodSource("emailsInvalidos")
	void beRec01RejeitaEmailInvalido(String email) {
		assertThrows(IllegalArgumentException.class, () -> service.solicitar(email));
	}

	@Test
	void beRec02NormalizaEmailAntesDeBuscarConta() {
		service.solicitar("Maria@EXAMPLE.COM");
		verify(contaRepository).buscarPorEmail(EMAIL);
	}

	@Test
	void beRec04CriaTokenImprevisivelDeUsoUnicoValidoPorTrintaMinutos() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(conta));
		service.solicitar(EMAIL);
		service.solicitar(EMAIL);
		var links = ArgumentCaptor.forClass(String.class);
		verify(entrega, org.mockito.Mockito.times(2)).enviar(eq(EMAIL), links.capture());
		assertNotEquals(links.getAllValues().get(0), links.getAllValues().get(1));
		var token = links.getAllValues().get(1).substring(links.getAllValues().get(1).indexOf("#token=") + 7);
		assertFalse(tokenRepository.contemValor(token));
		assertEquals(
				Instant.parse("2026-07-23T12:30:00Z"),
				tokenRepository.expiracaoDoToken(token));
	}

	@Test
	void beRec05EmailInexistenteNaoCriaTokenNemEnviaMensagem() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());
		service.solicitar(EMAIL);
		verify(entrega, never()).enviar(any(), any());
		assertFalse(tokenRepository.quantidade() > 0);
	}

	@Test
	void beRec06NovoPedidoInvalidaTokensAnteriores() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(conta));
		service.solicitar(EMAIL);
		service.solicitar(EMAIL);
		verify(entrega, org.mockito.Mockito.times(2)).enviar(eq(EMAIL), any());
	}

	@Test
	void beRec08LinkUsaFragmentoENaoQueryString() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(conta));
		service.solicitar(EMAIL);
		var link = ArgumentCaptor.forClass(String.class);
		verify(entrega).enviar(eq(EMAIL), link.capture());
		org.junit.jupiter.api.Assertions.assertTrue(link.getValue().contains("#token="));
		assertFalse(link.getValue().contains("?token="));
	}

	private static Stream<String> emailsInvalidos() {
		return Stream.of(null, "", "invalido", "a".repeat(243) + "@example.com");
	}
}
