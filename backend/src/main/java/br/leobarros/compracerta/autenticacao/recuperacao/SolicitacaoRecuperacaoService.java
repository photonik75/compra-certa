package br.leobarros.compracerta.autenticacao.recuperacao;

import java.time.Clock;
import java.time.Duration;

import br.leobarros.compracerta.autenticacao.comum.Email;
import br.leobarros.compracerta.autenticacao.sessao.GeradorIdentificadorService;
import org.springframework.stereotype.Service;

@Service
public class SolicitacaoRecuperacaoService {

	private static final Duration VALIDADE_TOKEN = Duration.ofMinutes(30);
	private static final String URL_REDEFINICAO = "https://compra-certa.app/redefinir-senha#token=";

	private final ContaRecuperacaoRepository contaRepository;
	private final TokenRecuperacaoRepository tokenRepository;
	private final EntregaRecuperacaoSenha entrega;
	private final GeradorIdentificadorService gerador;
	private final Clock clock;

	public SolicitacaoRecuperacaoService(
			ContaRecuperacaoRepository contaRepository,
			TokenRecuperacaoRepository tokenRepository,
			EntregaRecuperacaoSenha entrega,
			GeradorIdentificadorService gerador,
			Clock clock) {
		this.contaRepository = contaRepository;
		this.tokenRepository = tokenRepository;
		this.entrega = entrega;
		this.gerador = gerador;
		this.clock = clock;
	}

	public void solicitar(String emailInformado) {
		var email = Email.validarENormalizar(emailInformado);
		contaRepository.buscarPorEmail(email).ifPresent(conta -> {
			tokenRepository.invalidarDaConta(conta.getId());
			var token = gerador.gerarToken();
			tokenRepository.salvar(new TokenRecuperacao(
					HashSeguro.gerar(token),
					conta,
					clock.instant().plus(VALIDADE_TOKEN)));
			try {
				entrega.enviar(email, URL_REDEFINICAO + token);
			} catch (RuntimeException exception) {
				tokenRepository.invalidarDaConta(conta.getId());
				throw new RecuperacaoSenhaException(
						"Não foi possível enviar as instruções. Tente novamente mais tarde.");
			}
		});
	}
}
