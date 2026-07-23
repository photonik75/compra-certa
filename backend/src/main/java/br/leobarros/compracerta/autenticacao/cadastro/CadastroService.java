package br.leobarros.compracerta.autenticacao.cadastro;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CadastroService {

	private static final int TAMANHO_MINIMO_NOME = 2;
	private static final int TAMANHO_MAXIMO_NOME = 100;
	private static final int TAMANHO_MAXIMO_EMAIL = 254;
	private static final int TAMANHO_MINIMO_SENHA = 8;
	private static final int TAMANHO_MAXIMO_SENHA = 128;
	private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final String NOME_INVALIDO = "Por favor, informe um nome válido";
	private static final String EMAIL_INVALIDO = "Por favor, informe um e-mail válido";
	private static final String SENHA_INVALIDA = "A senha deve ter entre 8 e 128 caracteres";
	private static final String CONFIRMACAO_INVALIDA = "A confirmação deve ser idêntica à senha";

	private final ContaRepository contaRepository;
	private final PasswordEncoder passwordEncoder;
	private final ConcurrentHashMap<String, Object> bloqueiosPorEmail = new ConcurrentHashMap<>();

	public CadastroService(ContaRepository contaRepository, PasswordEncoder passwordEncoder) {
		this.contaRepository = contaRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public Conta cadastrar(DadosCadastro dadosCadastro) {
		validar(dadosCadastro);
		var emailNormalizado = dadosCadastro.email().toLowerCase(Locale.ROOT);
		var bloqueio = bloqueiosPorEmail.computeIfAbsent(emailNormalizado, email -> new Object());
		try {
			synchronized (bloqueio) {
				return cadastrar(dadosCadastro, emailNormalizado);
			}
		} finally {
			bloqueiosPorEmail.remove(emailNormalizado, bloqueio);
		}
	}

	private Conta cadastrar(DadosCadastro dadosCadastro, String emailNormalizado) {
		if (contaRepository.existePorEmail(emailNormalizado)) {
			throw new EmailJaCadastradoException();
		}
		var senhaHash = passwordEncoder.encode(dadosCadastro.senha());
		var conta = new Conta(dadosCadastro.nome(), emailNormalizado, senhaHash);
		try {
			contaRepository.salvar(conta);
			return conta;
		} catch (RuntimeException exception) {
			contaRepository.remover(conta);
			throw exception;
		}
	}

	private void validar(DadosCadastro dadosCadastro) {
		validarNome(dadosCadastro.nome());
		validarEmail(dadosCadastro.email());
		validarSenha(dadosCadastro.senha());
		validarConfirmacao(dadosCadastro.senha(), dadosCadastro.confirmacaoSenha());
	}

	private void validarNome(String nome) {
		if (nome == null || nome.isBlank()
				|| nome.length() < TAMANHO_MINIMO_NOME || nome.length() > TAMANHO_MAXIMO_NOME) {
			throw new IllegalArgumentException(NOME_INVALIDO);
		}
	}

	private void validarEmail(String email) {
		if (email == null || email.length() > TAMANHO_MAXIMO_EMAIL || !FORMATO_EMAIL.matcher(email).matches()) {
			throw new IllegalArgumentException(EMAIL_INVALIDO);
		}
	}

	private void validarSenha(String senha) {
		if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA || senha.length() > TAMANHO_MAXIMO_SENHA) {
			throw new IllegalArgumentException(SENHA_INVALIDA);
		}
	}

	private void validarConfirmacao(String senha, String confirmacaoSenha) {
		if (confirmacaoSenha == null || !confirmacaoSenha.equals(senha)) {
			throw new IllegalArgumentException(CONFIRMACAO_INVALIDA);
		}
	}
}
