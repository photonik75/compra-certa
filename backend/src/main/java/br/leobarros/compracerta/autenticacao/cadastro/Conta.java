package br.leobarros.compracerta.autenticacao.cadastro;

public class Conta {

	private final String nome;
	private final String email;
	private final String senhaHash;

	public Conta(String nome, String email, String senhaHash) {
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
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
}
