package br.leobarros.compracerta.autenticacao.sessao;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class GeradorIdentificadorService {

	public UUID gerar() {
		return UUID.randomUUID();
	}

	public String gerarToken() {
		return gerar().toString();
	}
}
