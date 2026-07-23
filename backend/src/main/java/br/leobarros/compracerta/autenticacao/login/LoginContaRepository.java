package br.leobarros.compracerta.autenticacao.login;

import java.util.Optional;

import br.leobarros.compracerta.autenticacao.cadastro.Conta;

public interface LoginContaRepository {

	Optional<Conta> buscarPorEmail(String email);
}
