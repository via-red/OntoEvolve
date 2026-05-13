package com.ontoevolve.domain.education;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 教育领域应用入口。
 * <p>
 * 扫描 onto-evolve-starter 和本模块的 Bean。
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ontoevolve.starter",
        "com.ontoevolve.infra",
        "com.ontoevolve.graphstore",
        "com.ontoevolve.domain.education"
})
public class EducationApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducationApplication.class, args);
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> allowEncodedSlash() {
        return factory -> factory.addConnectorCustomizers(
                connector -> connector.setProperty("ALLOW_ENCODED_SLASH", "true")
        );
    }
}
