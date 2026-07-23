package br.leobarros.compracerta.autenticacao.sessao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import org.springframework.stereotype.Service;

@Service
public class SessaoService {

	private static final Duration DURACAO_INATIVIDADE = Duration.ofHours(12);
	private static final Duration DURACAO_MAXIMA = Duration.ofHours(24);
	private static final Duration DURACAO_SESSAO_PERSISTENTE = Duration.ofDays(30);
	private static final String STATUS_CONTA_ATIVA = "ACTIVE";

	private final Clock clock;
	private final GeradorIdentificadorService geradorIdentificadorService;
	private final SessaoRepository sessaoRepository;

	public SessaoService(
			Clock clock,
			GeradorIdentificadorService geradorIdentificadorService,
			SessaoRepository sessaoRepository) {
		this.clock = clock;
		this.geradorIdentificadorService = geradorIdentificadorService;
		this.sessaoRepository = sessaoRepository;
	}

	public SessaoCriada criarParaCadastro(Conta conta) {
		return criar(conta, DURACAO_INATIVIDADE, DURACAO_MAXIMA);
	}

	public SessaoCriada criarParaLogin(Conta conta, boolean manterConectado) {
		var inatividade = manterConectado ? DURACAO_SESSAO_PERSISTENTE : DURACAO_INATIVIDADE;
		var duracaoMaxima = manterConectado ? DURACAO_SESSAO_PERSISTENTE : DURACAO_MAXIMA;
		return criar(conta, inatividade, duracaoMaxima);
	}

	public SessionResponse consultar(String token) {
		var sessao = buscarValida(token);
		var agora = clock.instant();
		sessao.atualizarExpiracaoPorInatividade(
				menor(agora.plus(sessao.duracaoInatividade()), sessao.expiraEmDefinitivo()));
		return rotacionarCsrf(token);
	}

	public SessionResponse rotacionarCsrf(String token) {
		var sessao = buscarValida(token);
		var csrfToken = geradorIdentificadorService.gerarToken();
		sessao.atualizarCsrfTokenHash(hash(csrfToken));
		return resposta(sessao, csrfToken);
	}

	public void validarCsrf(String token, String csrfToken) {
		var sessao = buscarValida(token);
		if (csrfToken == null || !MessageDigest.isEqual(
				sessao.csrfTokenHash().getBytes(StandardCharsets.UTF_8),
				hash(csrfToken).getBytes(StandardCharsets.UTF_8))) {
			throw new CsrfInvalidoException();
		}
	}

	public void encerrar(String token, String csrfToken) {
		validarCsrf(token, csrfToken);
		revogar(token);
	}

	public void revogar(String token) {
		var sessao = buscarValida(token);
		sessao.revogar();
	}

	public void revogarDaConta(Conta conta) {
		sessaoRepository.revogarDaConta(conta.getId());
	}

	public SessaoEstado obterEstado(String token) {
		var sessao = buscar(token);
		return new SessaoEstado(
				sessao.tokenHash(),
				sessao.csrfTokenHash(),
				sessao.expiraPorInatividade(),
				sessao.expiraEmDefinitivo());
	}

	private SessaoCriada criar(Conta conta, Duration inatividade, Duration duracaoMaxima) {
		var agora = clock.instant();
		var token = geradorIdentificadorService.gerarToken();
		var csrfToken = geradorIdentificadorService.gerarToken();
		var expiraEmDefinitivo = agora.plus(duracaoMaxima);
		var expiraPorInatividade = menor(agora.plus(inatividade), expiraEmDefinitivo);
		var sessao = new SessaoRegistro(
				hash(token),
				hash(csrfToken),
				conta,
				agora,
				inatividade,
				expiraPorInatividade,
				expiraEmDefinitivo);
		sessaoRepository.salvar(sessao);
		return new SessaoCriada(token, resposta(sessao, csrfToken));
	}

	private SessionResponse resposta(SessaoRegistro sessao, String csrfToken) {
		var usuario = new SessionResponse.UserSummary(
				sessao.conta().getId(),
				sessao.conta().getNome(),
				sessao.conta().getEmail(),
				STATUS_CONTA_ATIVA,
				sessao.criadaEm());
		return new SessionResponse(usuario, csrfToken, sessao.expiraPorInatividade());
	}

	private SessaoRegistro buscarValida(String token) {
		var sessao = buscar(token);
		var agora = clock.instant();
		if (sessao.revogada()
				|| !sessao.conta().isAtiva()
				|| agora.isAfter(sessao.expiraPorInatividade())
				|| agora.isAfter(sessao.expiraEmDefinitivo())) {
			throw new SessaoExpiradaException();
		}
		return sessao;
	}

	private SessaoRegistro buscar(String token) {
		if (token == null || token.isBlank()) throw new SessaoInvalidaException();
		return sessaoRepository.buscarPorTokenHash(hash(token)).orElseThrow(SessaoInvalidaException::new);
	}

	private Instant menor(Instant primeira, Instant segunda) {
		return primeira.isBefore(segunda) ? primeira : segunda;
	}

	private String hash(String valor) {
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Não foi possível proteger os dados da sessão.", exception);
		}
	}

}
