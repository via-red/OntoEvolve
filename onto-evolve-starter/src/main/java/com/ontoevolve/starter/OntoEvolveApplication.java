package com.ontoevolve.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OntoEvolve 启动入口。
 * <p>
 * 用于快速启动和验证。建议领域项目在自己的 Application 类上
 * 使用 @SpringBootApplication 并扫描此包。
 */
@SpringBootApplication
public class OntoEvolveApplication {
    public static void main(String[] args) {
        SpringApplication.run(OntoEvolveApplication.class, args);
    }
}
