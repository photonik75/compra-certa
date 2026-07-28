package br.leobarros.compracerta.configuracao;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

	@GetMapping({
			"/cadastro",
			"/entrar",
			"/recuperar-senha",
			"/redefinir-senha",
			"/listas",
			"/listas/nova",
			"/listas/{listId}",
			"/listas/{listId}/editar",
			"/listas/{listId}/itens/novo",
			"/listas/{listId}/itens/{itemId}/editar",
			"/listas/{listId}/compartilhar",
			"/categorias",
			"/produtos",
			"/convites/aceitar"
	})
	String encaminharParaAngular() {
		return "forward:/index.html";
	}
}
