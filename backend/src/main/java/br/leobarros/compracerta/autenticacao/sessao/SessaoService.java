package br.leobarros.compracerta.autenticacao.sessao;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;
import org.springframework.stereotype.Service;

@Service
public class SessaoService {

	private static final Duration DURACAO_SESSAO_CADASTRO = Duration.ofHours(24);
	private static final String STATUS_CONTA_ATIVA = "ACTIVE";

	public SessaoCriada criarParaCadastro(Conta conta) {
		var agora = Instant.now();
		var usuario = new SessionResponse.UserSummary(
				UUID.randomUUID(), conta.getNome(), conta.getEmail(), STATUS_CONTA_ATIVA, agora);
		var resposta = new SessionResponse(
				usuario,
				UUID.randomUUID().toString(),
				agora.plus(DURACAO_SESSAO_CADASTRO));
		return new SessaoCriada(UUID.randomUUID().toString(), resposta);
	}
}
