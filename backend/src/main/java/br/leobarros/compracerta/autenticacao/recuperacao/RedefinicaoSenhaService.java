package br.leobarros.compracerta.autenticacao.recuperacao;

import java.time.Clock;

import br.leobarros.compracerta.autenticacao.sessao.SessaoService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RedefinicaoSenhaService {

	private final TokenRecuperacaoRepository tokenRepository;
	private final ContaRecuperacaoRepository contaRepository;
	private final PasswordEncoder passwordEncoder;
	private final SessaoService sessaoService;
	private final Clock clock;

	public RedefinicaoSenhaService(
			TokenRecuperacaoRepository tokenRepository,
			ContaRecuperacaoRepository contaRepository,
			PasswordEncoder passwordEncoder,
			SessaoService sessaoService,
			Clock clock) {
		this.tokenRepository = tokenRepository;
		this.contaRepository = contaRepository;
		this.passwordEncoder = passwordEncoder;
		this.sessaoService = sessaoService;
		this.clock = clock;
	}

	public void redefinir(String token, String novaSenha, String confirmacao) {
		validar(token, novaSenha, confirmacao);
		var registro = tokenRepository.buscar(HashSeguro.gerar(token))
				.filter(item -> !item.usado() && !item.invalidado() && !clock.instant().isAfter(item.expiraEm()))
				.orElseThrow(TokenRecuperacaoInvalidoException::new);
		var conta = registro.conta();
		var hashAnterior = conta.getSenhaHash();
		try {
			conta.alterarSenhaHash(passwordEncoder.encode(novaSenha));
			contaRepository.salvar(conta);
			registro.usar();
			sessaoService.revogarDaConta(conta);
		} catch (RuntimeException exception) {
			conta.alterarSenhaHash(hashAnterior);
			registro.restaurarUso();
			contaRepository.salvar(conta);
			throw exception;
		}
	}

	private void validar(String token, String novaSenha, String confirmacao) {
		if (token == null || token.isBlank()) throw new IllegalArgumentException("Informe o token de recuperação.");
		if (novaSenha == null || novaSenha.length() < 8 || novaSenha.length() > 128) {
			throw new IllegalArgumentException("A nova senha deve ter entre 8 e 128 caracteres.");
		}
		if (confirmacao == null || !novaSenha.equals(confirmacao)) {
			throw new IllegalArgumentException("A confirmação deve ser idêntica à nova senha.");
		}
	}
}
