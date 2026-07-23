package br.leobarros.compracerta.autenticacao.cadastro;

import br.leobarros.compracerta.autenticacao.login.LoginController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class CadastroSecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(autorizacao -> autorizacao
						.requestMatchers(HttpMethod.POST, CadastroController.ENDPOINT_CADASTRO).permitAll()
						.requestMatchers(HttpMethod.POST, LoginController.ENDPOINT_LOGIN).permitAll()
						.anyRequest().authenticated())
				.csrf(csrf -> csrf.ignoringRequestMatchers(
						CadastroController.ENDPOINT_CADASTRO,
						LoginController.ENDPOINT_LOGIN));
		return http.build();
	}
}
