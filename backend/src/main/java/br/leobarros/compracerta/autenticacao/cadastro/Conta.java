package br.leobarros.compracerta.autenticacao.cadastro;

import java.util.UUID;

public class Conta {

	private final UUID id;
	private final String nome;
	private final String email;
	private String senhaHash;
	private boolean ativa = true;

	public Conta(String nome, String email, String senhaHash) {
		this.id = UUID.randomUUID();
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
	}

	public UUID getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void alterarSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public boolean isAtiva() {
		return ativa;
	}

	public void desativar() {
		ativa = false;
	}
}
