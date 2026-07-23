package br.leobarros.compracerta.unit.autenticacao.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.login.CredenciaisInvalidasException;
import br.leobarros.compracerta.autenticacao.login.DadosLogin;
import br.leobarros.compracerta.autenticacao.login.LoginBloqueadoException;
import br.leobarros.compracerta.autenticacao.login.LoginContaRepository;
import br.leobarros.compracerta.autenticacao.login.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

	private static final String EMAIL = "maria@example.com";
	private static final String EMAIL_COM_MAIUSCULAS = "Maria@EXAMPLE.COM";
	private static final String SENHA_CORRETA = "senha segura";
	private static final String SENHA_INCORRETA = "senha errada";
	private static final String HASH = "hash-seguro";
	private static final Instant AGORA = Instant.parse("2026-07-23T12:00:00Z");

	@Mock
	private LoginContaRepository contaRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private MutableClock clock;
	private LoginService loginService;
	private Conta conta;

	@BeforeEach
	void configurar() {
		clock = new MutableClock(AGORA);
		loginService = new LoginService(contaRepository, passwordEncoder, clock);
		conta = new Conta("Maria Silva", EMAIL, HASH);
	}

	@Test
	void beLog01AutenticaContaAtivaIgnorandoCaixaDoEmail() {
		prepararContaExistente();
		var autenticada = loginService.autenticar(dadosValidos(EMAIL_COM_MAIUSCULAS));
		assertEquals(conta, autenticada);
		verify(contaRepository).buscarPorEmail(EMAIL);
	}

	@ParameterizedTest
	@MethodSource("emailsInvalidos")
	void beLog02RejeitaEmailAusenteInvalidoOuMaiorQue254Caracteres(String email) {
		var excecao = assertThrows(IllegalArgumentException.class,
				() -> loginService.autenticar(dadosValidos(email)));
		assertFalse(excecao.getMessage() == null || excecao.getMessage().isBlank());
	}

	@ParameterizedTest
	@MethodSource("senhasInvalidas")
	void beLog03RejeitaSenhaAusenteOuMenorQueOitoCaracteres(String senha) {
		var excecao = assertThrows(IllegalArgumentException.class,
				() -> loginService.autenticar(new DadosLogin(EMAIL, senha, false)));
		assertFalse(excecao.getMessage() == null || excecao.getMessage().isBlank());
	}

	@ParameterizedTest
	@MethodSource("opcoesManterConectadoInvalidas")
	void beLog04RejeitaManterConectadoAusenteOuNaoBooleano(Object manterConectado) {
		var excecao = assertThrows(IllegalArgumentException.class,
				() -> loginService.autenticar(new DadosLogin(EMAIL, SENHA_CORRETA, manterConectado)));
		assertFalse(excecao.getMessage() == null || excecao.getMessage().isBlank());
	}

	@Test
	void beLog06VerificaSenhaMesmoQuandoEmailNaoExiste() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());
		assertThrows(CredenciaisInvalidasException.class, () -> loginService.autenticar(dadosValidos(EMAIL)));
		verify(passwordEncoder).matches(eq(SENHA_CORRETA), anyString());
	}

	@Test
	void beLog07ContabilizaCadaFalhaNaJanelaMovel() {
		prepararSenhaIncorreta();
		realizarFalhas(5);
		var bloqueio = assertThrows(LoginBloqueadoException.class, () -> loginService.autenticar(dadosInvalidos()));
		assertEquals(900, bloqueio.retryAfterSeconds());
	}

	@Test
	void beLog08AteQuartaFalhaLoginAindaNaoEstaBloqueado() {
		prepararSenhaIncorreta();
		realizarFalhas(4);
		assertThrows(CredenciaisInvalidasException.class, () -> loginService.autenticar(dadosInvalidos()));
	}

	@Test
	void beLog09DepoisDaQuintaFalhaNovasTentativasFicamBloqueadas() {
		prepararSenhaIncorreta();
		realizarFalhas(5);
		assertThrows(LoginBloqueadoException.class, () -> loginService.autenticar(dadosInvalidos()));
	}

	@Test
	void beLog11DepoisDoBloqueioCredencialValidaVoltaAAutenticar() {
		prepararSenhaVariavel();
		realizarFalhas(5);
		clock.avancar(Duration.ofMinutes(15).plusSeconds(1));
		assertEquals(conta, loginService.autenticar(dadosValidos(EMAIL)));
	}

	@Test
	void beLog12TentativasForaDaJanelaNaoSaoSomadas() {
		prepararSenhaIncorreta();
		realizarFalhas(4);
		clock.avancar(Duration.ofMinutes(15).plusSeconds(1));
		realizarFalhas(4);
		assertThrows(CredenciaisInvalidasException.class, () -> loginService.autenticar(dadosInvalidos()));
	}

	@Test
	void beLog13LoginBemSucedidoLimpaHistoricoDeFalhas() {
		prepararSenhaVariavel();
		realizarFalhas(4);
		assertEquals(conta, loginService.autenticar(dadosValidos(EMAIL)));
		realizarFalhas(5);
		assertThrows(LoginBloqueadoException.class, () -> loginService.autenticar(dadosInvalidos()));
	}

	private void prepararContaExistente() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(conta));
		when(passwordEncoder.matches(SENHA_CORRETA, HASH)).thenReturn(true);
	}

	private void prepararSenhaIncorreta() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(conta));
		when(passwordEncoder.matches(SENHA_INCORRETA, HASH)).thenReturn(false);
	}

	private void prepararSenhaVariavel() {
		when(contaRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(conta));
		when(passwordEncoder.matches(SENHA_INCORRETA, HASH)).thenReturn(false);
		when(passwordEncoder.matches(SENHA_CORRETA, HASH)).thenReturn(true);
	}

	private void realizarFalhas(int quantidade) {
		for (var tentativa = 0; tentativa < quantidade; tentativa++) {
			assertThrows(CredenciaisInvalidasException.class, () -> loginService.autenticar(dadosInvalidos()));
		}
	}

	private DadosLogin dadosValidos(String email) {
		return new DadosLogin(email, SENHA_CORRETA, false);
	}

	private DadosLogin dadosInvalidos() {
		return new DadosLogin(EMAIL, SENHA_INCORRETA, false);
	}

	private static Stream<Arguments> emailsInvalidos() {
		return Stream.of(null, "", "email-invalido", "a".repeat(243) + "@example.com").map(Arguments::of);
	}

	private static Stream<Arguments> senhasInvalidas() {
		return Stream.of(null, "", "1234567").map(Arguments::of);
	}

	private static Stream<Arguments> opcoesManterConectadoInvalidas() {
		return Stream.of(null, "false", 0).map(Arguments::of);
	}

	private static class MutableClock extends Clock {

		private Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		void avancar(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
