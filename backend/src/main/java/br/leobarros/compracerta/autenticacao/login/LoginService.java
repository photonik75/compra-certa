package br.leobarros.compracerta.autenticacao.login;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import br.leobarros.compracerta.autenticacao.comum.Email;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

	private static final int TAMANHO_MINIMO_SENHA = 8;
	private static final int LIMITE_FALHAS = 5;
	private static final Duration JANELA_FALHAS = Duration.ofMinutes(15);
	private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);
	private static final String SENHA_INVALIDA = "A senha deve ter pelo menos 8 caracteres";
	private static final String MANTER_CONECTADO_INVALIDO = "Informe se deseja manter o acesso conectado";
	private static final String HASH_FICTICIO =
			"$2a$10$7EqJtq98hPqEX7fNZaFWoO5vY6Tn7E6dJH5FbJ8QqYq4Z5l5z5J5a";

	private final LoginContaRepository contaRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final ConcurrentHashMap<String, HistoricoFalhas> historicos = new ConcurrentHashMap<>();

	public LoginService(LoginContaRepository contaRepository, PasswordEncoder passwordEncoder, Clock clock) {
		this.contaRepository = contaRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	public Conta autenticar(DadosLogin dadosLogin) {
		validar(dadosLogin);
		var email = Email.validarENormalizar(dadosLogin.email());
		var agora = clock.instant();
		var historico = historicos.computeIfAbsent(email, chave -> new HistoricoFalhas());
		synchronized (historico) {
			verificarBloqueio(historico, agora);
			var conta = contaRepository.buscarPorEmail(email);
			var hash = conta.map(Conta::getSenhaHash).orElse(HASH_FICTICIO);
			var senhaCorreta = passwordEncoder.matches(dadosLogin.senha(), hash);
			if (conta.isEmpty() || !senhaCorreta) {
				registrarFalha(historico, agora);
				throw new CredenciaisInvalidasException();
			}
			historicos.remove(email, historico);
			return conta.orElseThrow();
		}
	}

	private void validar(DadosLogin dadosLogin) {
		Email.validarENormalizar(dadosLogin.email());
		var senha = dadosLogin.senha();
		if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
			throw new IllegalArgumentException(SENHA_INVALIDA);
		}
		if (!(dadosLogin.manterConectado() instanceof Boolean)) {
			throw new IllegalArgumentException(MANTER_CONECTADO_INVALIDO);
		}
	}

	private void verificarBloqueio(HistoricoFalhas historico, Instant agora) {
		if (historico.bloqueadoAte == null) {
			return;
		}
		if (!agora.isBefore(historico.bloqueadoAte)) {
			historico.limpar();
			return;
		}
		var segundos = Duration.between(agora, historico.bloqueadoAte).toSeconds();
		throw new LoginBloqueadoException(Math.max(1, segundos));
	}

	private void registrarFalha(HistoricoFalhas historico, Instant agora) {
		var inicioJanela = agora.minus(JANELA_FALHAS);
		historico.tentativas.removeIf(tentativa -> !tentativa.isAfter(inicioJanela));
		historico.tentativas.addLast(agora);
		if (historico.tentativas.size() >= LIMITE_FALHAS) {
			historico.bloqueadoAte = agora.plus(DURACAO_BLOQUEIO);
		}
	}

	private static class HistoricoFalhas {

		private final ArrayDeque<Instant> tentativas = new ArrayDeque<>();
		private Instant bloqueadoAte;

		void limpar() {
			tentativas.clear();
			bloqueadoAte = null;
		}
	}
}
