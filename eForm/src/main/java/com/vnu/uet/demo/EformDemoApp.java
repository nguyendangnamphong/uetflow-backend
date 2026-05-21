package com.vnu.uet.demo;

import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication(
    scanBasePackageClasses = { EformDemoApp.class },
    exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, LiquibaseAutoConfiguration.class }
)
public class EformDemoApp {

    public static void main(String[] args) {
        new SpringApplicationBuilder(EformDemoApp.class)
            .properties(Map.of("server.port", "8082", "spring.application.name", "eform-demo"))
            .run(args);
    }

    @Bean
    CorsFilter corsFilter(org.springframework.core.env.Environment env) {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(env.getProperty("JHIPSTER_CORS_ALLOWED_ORIGINS", "https://datawarehouse-subject.web.app"));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(Boolean.parseBoolean(env.getProperty("JHIPSTER_CORS_ALLOW_CREDENTIALS", "true")));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
