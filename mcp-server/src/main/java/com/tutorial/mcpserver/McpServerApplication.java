package com.tutorial.mcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Server uygulamasi.
 *
 * Bu uygulama basladiginda:
 * 1. MongoDB'ye baglanir (localhost:27017/mcptutorialdb)
 * 2. DataSeeder ile dummy kullanicilar yuklenir (collection bos ise)
 * 3. @Tool annotation'li methodlar MCP tool olarak kaydedilir
 * 4. SSE transport endpoint'leri aktif olur
 *
 * MCP Client bu sunucuya http://localhost:8080 uzerinden baglanabilir.
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
