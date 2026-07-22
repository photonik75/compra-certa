package br.leobarros.compracerta;

import org.springframework.boot.SpringApplication;

public class TestCompraCertaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(CompraCertaBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
