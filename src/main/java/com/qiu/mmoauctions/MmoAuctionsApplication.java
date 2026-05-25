package com.qiu.mmoauctions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableScheduling
@ComponentScan(basePackages = {"com.qiu.controllers"})
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableTransactionManagement
@ComponentScan(basePackages = {"com.qiu"})
@EnableJpaRepositories(basePackages = "com.qiu.repositories")
@EntityScan(basePackages = "com.qiu.entities")
public class MmoAuctionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MmoAuctionsApplication.class, args);
	}

}
