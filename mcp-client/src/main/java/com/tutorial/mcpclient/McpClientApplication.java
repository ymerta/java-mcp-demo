package com.tutorial.mcpclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Client uygulamasi.
 *
 * Bu uygulama basladiginda:
 * 1. application.yml'deki SSE baglanti bilgisi ile MCP Server'a baglanir
 * 2. Server'daki tool ve resource listesini ceker
 * 3. CommandLineRunner (McpDemoRunner) ile demo islemleri gerceklestirir
 *
 * ONEMLI: Bu uygulamayi baslatmadan once MCP Server'in calisiyor olmasi gerekir!
 */
@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }
}
