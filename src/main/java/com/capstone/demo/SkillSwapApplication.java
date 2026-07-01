package com.capstone.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import config.JwtProperties;

@SpringBootApplication(exclude = {
	    org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration.class,
	    org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration.class})
	@EnableConfigurationProperties({JwtProperties.class})
	@EnableScheduling
	@ComponentScan(basePackages = {"com.capstone.demo", "controller", "service", "security", "repository", "config", "dto", "model"})
	@EntityScan(basePackages = {"model"})
	@EnableJpaRepositories(basePackages = {"repository"})
	@EnableAsync
public class SkillSwapApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillSwapApplication.class, args);
    }
}