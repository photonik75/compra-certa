package br.leobarros.compracerta.autenticacao.configuracao;

import br.leobarros.compracerta.autenticacao.cadastro.CadastroController;
import br.leobarros.compracerta.autenticacao.login.LoginController;
import br.leobarros.compracerta.autenticacao.sessao.SessaoController;
import br.leobarros.compracerta.autenticacao.recuperacao.RecuperacaoSenhaController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AutenticacaoSecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(autorizacao -> autorizacao
						.requestMatchers(HttpMethod.POST, CadastroController.ENDPOINT_CADASTRO).permitAll()
						.requestMatchers(HttpMethod.POST, LoginController.ENDPOINT_LOGIN).permitAll()
						.requestMatchers(
								HttpMethod.POST,
								RecuperacaoSenhaController.ENDPOINT_SOLICITACAO,
								RecuperacaoSenhaController.ENDPOINT_REDEFINICAO).permitAll()
						.requestMatchers(
								SessaoController.ENDPOINT_CONSULTA,
								SessaoController.ENDPOINT_LOGOUT).permitAll()
						.anyRequest().authenticated())
				.csrf(csrf -> csrf.ignoringRequestMatchers(
						CadastroController.ENDPOINT_CADASTRO,
						LoginController.ENDPOINT_LOGIN,
						RecuperacaoSenhaController.ENDPOINT_SOLICITACAO,
						RecuperacaoSenhaController.ENDPOINT_REDEFINICAO,
						SessaoController.ENDPOINT_CONSULTA,
						SessaoController.ENDPOINT_LOGOUT));
		return http.build();
	}
}
