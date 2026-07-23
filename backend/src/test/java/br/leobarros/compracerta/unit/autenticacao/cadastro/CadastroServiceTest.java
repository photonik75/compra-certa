package br.leobarros.compracerta.unit.autenticacao.cadastro;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import br.leobarros.compracerta.autenticacao.cadastro.CadastroService;
import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.cadastro.ContaRepository;
import br.leobarros.compracerta.autenticacao.cadastro.DadosCadastro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CadastroServiceTest {

	private static final String NOME = "Maria Silva";
	private static final String NOME_COM_CAPITALIZACAO_VARIADA = "mArIa da SiLvA";
	private static final String EMAIL = "maria@example.com";
	private static final String EMAIL_COM_MAIUSCULAS = "Maria@EXAMPLE.COM";
	private static final String SENHA = "senha segura";
	private static final String SENHA_COM_ESPACOS = "  senha  segura  ";
	private static final String HASH_DA_SENHA = "hash-da-senha";
	private static final String MENSAGEM_DE_ERRO_OBRIGATORIA = "A validação deve apresentar uma mensagem polida";

	@Mock
	private ContaRepository contaRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private CadastroService cadastroService;

	@Test
	void cadastraNomeEmailEHashDaSenhaQuandoDadosSaoValidos() {
		when(passwordEncoder.encode(SENHA)).thenReturn(HASH_DA_SENHA);
		cadastroService.cadastrar(dadosValidos());
		var contaSalva = capturarContaSalva();
		assertAll(
				() -> assertEquals(NOME, contaSalva.getNome()),
				() -> assertEquals(EMAIL, contaSalva.getEmail()),
				() -> assertEquals(HASH_DA_SENHA, contaSalva.getSenhaHash()));
	}

	@Test
	void preservaCapitalizacaoDoNomeCadastrado() {
		when(passwordEncoder.encode(SENHA)).thenReturn(HASH_DA_SENHA);
		cadastroService.cadastrar(new DadosCadastro(NOME_COM_CAPITALIZACAO_VARIADA, EMAIL, SENHA, SENHA));
		assertEquals(NOME_COM_CAPITALIZACAO_VARIADA, capturarContaSalva().getNome());
	}

	@Test
	void normalizaEmailAntesDeConsultarDuplicidadeEPersistir() {
		when(passwordEncoder.encode(SENHA)).thenReturn(HASH_DA_SENHA);
		cadastroService.cadastrar(new DadosCadastro(NOME, EMAIL_COM_MAIUSCULAS, SENHA, SENHA));
		var contaCaptor = ArgumentCaptor.forClass(Conta.class);
		var ordem = inOrder(contaRepository);
		ordem.verify(contaRepository).existePorEmail(EMAIL);
		ordem.verify(contaRepository).salvar(contaCaptor.capture());
		assertEquals(EMAIL, contaCaptor.getValue().getEmail());
	}

	@Test
	void preservaTodosOsEspacosDaSenhaAoGerarHash() {
		when(passwordEncoder.encode(SENHA_COM_ESPACOS)).thenReturn(HASH_DA_SENHA);
		cadastroService.cadastrar(new DadosCadastro(NOME, EMAIL, SENHA_COM_ESPACOS, SENHA_COM_ESPACOS));
		verify(passwordEncoder).encode(SENHA_COM_ESPACOS);
	}

	@ParameterizedTest
	@MethodSource("nomesInvalidos")
	void rejeitaNomeAusenteVazioSomenteComEspacosOuForaDoTamanhoPermitido(String nome) {
		var excecao = assertThrows(IllegalArgumentException.class,
				() -> cadastroService.cadastrar(new DadosCadastro(nome, EMAIL, SENHA, SENHA)));
		assertFalse(excecao.getMessage() == null || excecao.getMessage().isBlank(), MENSAGEM_DE_ERRO_OBRIGATORIA);
	}

	@ParameterizedTest
	@MethodSource("emailsInvalidos")
	void rejeitaEmailAusenteInvalidoOuComMaisDe254Caracteres(String email) {
		var excecao = assertThrows(IllegalArgumentException.class,
				() -> cadastroService.cadastrar(new DadosCadastro(NOME, email, SENHA, SENHA)));
		assertFalse(excecao.getMessage() == null || excecao.getMessage().isBlank(), MENSAGEM_DE_ERRO_OBRIGATORIA);
	}

	@ParameterizedTest
	@MethodSource("senhasInvalidas")
	void rejeitaSenhaAusenteOuForaDoTamanhoPermitido(String senha) {
		var excecao = assertThrows(IllegalArgumentException.class,
				() -> cadastroService.cadastrar(new DadosCadastro(NOME, EMAIL, senha, senha)));
		assertFalse(excecao.getMessage() == null || excecao.getMessage().isBlank(), MENSAGEM_DE_ERRO_OBRIGATORIA);
	}

	@ParameterizedTest
	@MethodSource("confirmacoesInvalidas")
	void rejeitaConfirmacaoAusenteOuDiferenteDaSenha(String confirmacao) {
		var excecao = assertThrows(IllegalArgumentException.class,
				() -> cadastroService.cadastrar(new DadosCadastro(NOME, EMAIL, SENHA, confirmacao)));
		assertFalse(excecao.getMessage() == null || excecao.getMessage().isBlank(), MENSAGEM_DE_ERRO_OBRIGATORIA);
	}

	@Test
	void naoGeraHashNemPersisteQuandoEntradaEhInvalida() {
		assertThrows(IllegalArgumentException.class,
				() -> cadastroService.cadastrar(new DadosCadastro(" ", EMAIL, SENHA, SENHA)));
		assertAll(
				() -> verifyNoInteractions(passwordEncoder),
				() -> verifyNoInteractions(contaRepository));
	}

	private DadosCadastro dadosValidos() {
		return new DadosCadastro(NOME, EMAIL, SENHA, SENHA);
	}

	private Conta capturarContaSalva() {
		var contaCaptor = ArgumentCaptor.forClass(Conta.class);
		verify(contaRepository).salvar(contaCaptor.capture());
		return contaCaptor.getValue();
	}

	private static Stream<Arguments> nomesInvalidos() {
		return Stream.of(null, "", "   ", "A", "A".repeat(101)).map(Arguments::of);
	}

	private static Stream<Arguments> emailsInvalidos() {
		return Stream.of(null, "", "   ", "email-invalido", emailComTamanho(255)).map(Arguments::of);
	}

	private static Stream<Arguments> senhasInvalidas() {
		return Stream.of(null, "", "A".repeat(7), "A".repeat(129)).map(Arguments::of);
	}

	private static Stream<Arguments> confirmacoesInvalidas() {
		return Stream.of(null, "", "senha segurA", " senha segura", "senha segura ").map(Arguments::of);
	}

	private static String emailComTamanho(int tamanho) {
		var dominioFixo = "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(58) + ".com";
		return "a".repeat(tamanho - dominioFixo.length()) + dominioFixo;
	}
}
