package br.leobarros.compracerta.autenticacao.configuracao;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutenticacaoConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
