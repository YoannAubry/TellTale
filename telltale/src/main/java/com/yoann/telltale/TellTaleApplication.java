package com.yoann.telltale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class TellTaleApplication {

	@Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

	public static void main(String[] args) {
		SpringApplication.run(TellTaleApplication.class, args);
	}

}
