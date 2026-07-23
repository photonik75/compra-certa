package br.leobarros.compracerta.autenticacao.sessao;

import java.time.Duration;
import java.time.Clock;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import org.springframework.stereotype.Service;

@Service
public class SessaoService {

	private static final Duration DURACAO_SESSAO_CADASTRO = Duration.ofHours(24);
	private static final Duration DURACAO_SESSAO_PERSISTENTE = Duration.ofDays(30);
	private static final String STATUS_CONTA_ATIVA = "ACTIVE";

	private final Clock clock;
	private final GeradorIdentificadorService geradorIdentificadorService;

	public SessaoService(Clock clock, GeradorIdentificadorService geradorIdentificadorService) {
		this.clock = clock;
		this.geradorIdentificadorService = geradorIdentificadorService;
	}

	public SessaoCriada criarParaCadastro(Conta conta) {
		return criar(conta, DURACAO_SESSAO_CADASTRO);
	}

	public SessaoCriada criarParaLogin(Conta conta, boolean manterConectado) {
		var duracao = manterConectado ? DURACAO_SESSAO_PERSISTENTE : DURACAO_SESSAO_CADASTRO;
		return criar(conta, duracao);
	}

	private SessaoCriada criar(Conta conta, Duration duracao) {
		var agora = clock.instant();
		var usuario = new SessionResponse.UserSummary(
				geradorIdentificadorService.gerar(),
				conta.getNome(),
				conta.getEmail(),
				STATUS_CONTA_ATIVA,
				agora);
		var resposta = new SessionResponse(
				usuario,
				geradorIdentificadorService.gerarToken(),
				agora.plus(duracao));
		return new SessaoCriada(geradorIdentificadorService.gerarToken(), resposta);
	}
}
