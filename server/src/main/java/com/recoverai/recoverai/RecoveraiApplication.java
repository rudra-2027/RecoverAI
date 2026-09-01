package com.recoverai.recoverai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RecoveraiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecoveraiApplication.class, args);
	}

}
	