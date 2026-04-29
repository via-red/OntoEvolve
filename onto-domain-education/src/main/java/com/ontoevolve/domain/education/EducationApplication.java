package com.ontoevolve.domain.education;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 教育领域应用入口。
 * <p>
 * 扫描 onto-evolve-starter 和本模块的 Bean。
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ontoevolve.starter",
        "com.ontoevolve.domain.education"
})
public class EducationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EducationApplication.class, args);
    }
}
