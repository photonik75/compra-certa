package br.leobarros.compracerta.unit.autenticacao.sessao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import br.leobarros.compracerta.autenticacao.sessao.SessaoExpiradaException;
import br.leobarros.compracerta.autenticacao.sessao.SessaoRepository;
import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import br.leobarros.compracerta.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessaoServiceTest {

	private static final Instant AGORA = Instant.parse("2026-07-23T12:00:00Z");
	private static final Duration DOZE_HORAS = Duration.ofHours(12);
	private static final Duration VINTE_E_QUATRO_HORAS = Duration.ofHours(24);
	private static final Duration TRINTA_DIAS = Duration.ofDays(30);

	private MutableClock clock;
	private SessaoService sessaoService;
	private Conta conta;

	@BeforeEach
	void configurar() {
		clock = new MutableClock(AGORA);
		sessaoService = new SessaoService(clock, new GeradorIdentificadorService(), new SessaoRepository());
		conta = new Conta("Maria Silva", "maria@example.com", "hash-seguro");
	}

	@Test
	void beSes01SessaoComumPossuiDozeHorasDeInatividadeEVinteEQuatroHorasDeVidaMaxima() {
		var criada = sessaoService.criarParaLogin(conta, false);
		var estado = sessaoService.obterEstado(criada.token());
		assertEquals(AGORA.plus(DOZE_HORAS), criada.response().expiresAt());
		assertEquals(AGORA.plus(VINTE_E_QUATRO_HORAS), estado.expiraEmDefinitivo());
	}

	@Test
	void beSes02AtividadeRenovaSomenteInatividadeSemUltrapassarPrazoMaximo() {
		var criada = sessaoService.criarParaLogin(conta, false);
		clock.avancar(Duration.ofHours(11));
		assertEquals(AGORA.plus(Duration.ofHours(23)), sessaoService.consultar(criada.token()).expiresAt());
		clock.avancar(Duration.ofHours(11));
		assertEquals(AGORA.plus(Duration.ofHours(24)), sessaoService.consultar(criada.token()).expiresAt());
	}

	@Test
	void beSes03SessaoPersistentePossuiNoMaximoTrintaDias() {
		var criada = sessaoService.criarParaLogin(conta, true);
		assertEquals(AGORA.plus(TRINTA_DIAS), criada.response().expiresAt());
		assertEquals(AGORA.plus(TRINTA_DIAS), sessaoService.obterEstado(criada.token()).expiraEmDefinitivo());
	}

	@Test
	void beSes04RejeitaExpiracaoPorInatividadePrazoMaximoOuTrintaDias() {
		var inativa = sessaoService.criarParaLogin(conta, false);
		clock.avancar(DOZE_HORAS.plusSeconds(1));
		assertThrows(SessaoExpiradaException.class, () -> sessaoService.consultar(inativa.token()));
		var maxima = sessaoService.criarParaLogin(conta, false);
		clock.avancar(VINTE_E_QUATRO_HORAS.plusSeconds(1));
		assertThrows(SessaoExpiradaException.class, () -> sessaoService.consultar(maxima.token()));
		var persistente = sessaoService.criarParaLogin(conta, true);
		clock.avancar(TRINTA_DIAS.plusSeconds(1));
		assertThrows(SessaoExpiradaException.class, () -> sessaoService.consultar(persistente.token()));
	}

	@Test
	void beSes05IdentificadorETokenCsrfSaoImprevisiveisEArmazenadosComoHash() {
		var primeira = sessaoService.criarParaLogin(conta, false);
		var segunda = sessaoService.criarParaLogin(conta, false);
		assertNotEquals(primeira.token(), segunda.token());
		assertNotEquals(primeira.response().csrfToken(), segunda.response().csrfToken());
		var estado = sessaoService.obterEstado(primeira.token());
		assertNotEquals(primeira.token(), estado.tokenHash());
		assertNotEquals(primeira.response().csrfToken(), estado.csrfTokenHash());
	}

	@Test
	void beSes10RotacaoDoCsrfInvalidaTokenAnterior() {
		var criada = sessaoService.criarParaLogin(conta, false);
		var tokenAnterior = criada.response().csrfToken();
		var renovada = sessaoService.rotacionarCsrf(criada.token());
		assertNotEquals(tokenAnterior, renovada.csrfToken());
		assertThrows(SecurityException.class, () -> sessaoService.validarCsrf(criada.token(), tokenAnterior));
		sessaoService.validarCsrf(criada.token(), renovada.csrfToken());
	}
}
