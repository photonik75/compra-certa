package br.leobarros.compracerta.autenticacao.cadastro;

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
						.anyRequest().authenticated())
				.csrf(csrf -> csrf.ignoringRequestMatchers(CadastroController.ENDPOINT_CADASTRO));
		return http.build();
	}
}
