package br.leobarros.compracerta.listas;

import java.util.List;

import br.leobarros.compracerta.autenticacao.erro.ApiFieldError;

final class ListaExceptions {

	private ListaExceptions() {
	}

	static class Validacao extends RuntimeException {
		private final List<ApiFieldError> erros;

		Validacao(String campo, String mensagem) {
			this.erros = List.of(new ApiFieldError(campo, mensagem));
		}

		List<ApiFieldError> erros() {
			return erros;
		}
	}

	static class NaoEncontrada extends RuntimeException {
	}

	static class NomeEmUso extends RuntimeException {
	}

	static class Proibida extends RuntimeException {
	}

	static class Concluida extends RuntimeException {
	}

	static class Conflito extends RuntimeException {
		private final long versao;

		Conflito(long versao) {
			this.versao = versao;
		}

		long versao() {
			return versao;
		}
	}
}
