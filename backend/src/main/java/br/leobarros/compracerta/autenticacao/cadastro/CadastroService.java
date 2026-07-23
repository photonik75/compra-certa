package br.leobarros.compracerta.autenticacao.cadastro;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;

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
	private static final String EMAIL_CADASTRADO = "E-mail já foi cadastrado";

	private final ContaRepository contaRepository;
	private final PasswordEncoder passwordEncoder;

	public CadastroService(ContaRepository contaRepository, PasswordEncoder passwordEncoder) {
		this.contaRepository = contaRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public void cadastrar(DadosCadastro dadosCadastro) {
		validar(dadosCadastro);
		var emailNormalizado = dadosCadastro.email().toLowerCase(Locale.ROOT);
		if (contaRepository.existePorEmail(emailNormalizado)) {
			throw new IllegalArgumentException(EMAIL_CADASTRADO);
		}
		var senhaHash = passwordEncoder.encode(dadosCadastro.senha());
		contaRepository.salvar(new Conta(dadosCadastro.nome(), emailNormalizado, senhaHash));
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
