package com.tutorial.mcpclient.runner;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Client demo runner.
 *
 * Bu sinif uygulama basladiginda otomatik olarak calisir ve
 * MCP Server ile iletisimi gosterir.
 *
 * Gosterilen MCP islemleri:
 * 1. Tool Discovery      - Server'daki mevcut 16 tool'u listeleme
 * 2. Resource Discovery   - Server'daki 3 resource'u listeleme
 * 3-5. User Tool'lari    - listAllUsers, createUser, findUsersByDepartment
 * 6. User Resource        - users://list okuma
 * 7-9. Message Tool'lari  - listMessages, findMessagesByType(PUSH), getMessageStats
 * 10. Message Resource    - messages://list okuma
 * 11-13. Segment Tool'lari - listSegments, findActiveSegments, getSegmentStats
 * 14. Segment Resource    - segments://list okuma
 */
@Component
@ConditionalOnProperty(name = "demo.auto-run", havingValue = "true", matchIfMissing = false)
public class McpDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(McpDemoRunner.class);

    private final List<McpSyncClient> mcpClients;

    public McpDemoRunner(List<McpSyncClient> mcpClients) {
        this.mcpClients = mcpClients;
    }

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("   MCP CLIENT DEMO BASLIYOR");
        log.info("========================================");

        for (McpSyncClient client : mcpClients) {

            // ============================================================
            // ADIM 1: Tool Discovery
            // MCP Server'daki tum tool'lari kesfediyoruz.
            // Bu, MCP'nin en temel ozelliklerinden biri:
            // Client, Server'a "hangi tool'larin var?" diye sorar.
            // ============================================================
            log.info("");
            log.info(">> ADIM 1: Tool Discovery - Mevcut tool'lari listeliyoruz...");
            McpSchema.ListToolsResult toolsResult = client.listTools();
            for (McpSchema.Tool tool : toolsResult.tools()) {
                log.info("   Tool bulundu: {} - {}", tool.name(), tool.description());
            }

            // ============================================================
            // ADIM 2: Resource Discovery
            // MCP Server'daki resource'lari kesfediyoruz.
            // Resource'lar read-only veri kaynaklaridir.
            // Tool'lardan farki: Resource pasif bilgi sunar,
            // Tool'lar ise aksiyon alir.
            // ============================================================
            log.info("");
            log.info(">> ADIM 2: Resource Discovery - Mevcut resource'lari listeliyoruz...");
            McpSchema.ListResourcesResult resourcesResult = client.listResources();
            for (McpSchema.Resource resource : resourcesResult.resources()) {
                log.info("   Resource bulundu: {} - {}", resource.uri(), resource.description());
            }

            // ============================================================
            // ADIM 3: Tool Calling - listAllUsers
            // Ilk tool cagrimiz! Server'daki "listAllUsers" tool'unu
            // cagiriyoruz. Parametre gerektirmiyor.
            // ============================================================
            log.info("");
            log.info(">> ADIM 3: Tool Calling - 'listAllUsers' cagiriliyor...");
            McpSchema.CallToolResult result1 = client.callTool(
                    new McpSchema.CallToolRequest("listAllUsers", Map.of())
            );
            logToolResult(result1);

            // ============================================================
            // ADIM 4: Tool Calling - createUser
            // MongoDB'ye yeni bir kullanici ekliyoruz.
            // NOT: getUserById demo'su kaldirildi cunku MongoDB ObjectId'leri
            // dinamik olusturulur (ornek: "507f1f77bcf86cd799439011")
            // ve onceden bilinemez. Gercek uygulamada listAllUsers sonucundan
            // ID alinip getUserById cagirilabilir.
            // ============================================================
            log.info("");
            log.info(">> ADIM 4: Tool Calling - 'createUser' ile yeni kullanici olusturuluyor...");
            McpSchema.CallToolResult result3 = client.callTool(
                    new McpSchema.CallToolRequest("createUser", Map.of(
                            "name", "Deniz Test",
                            "email", "deniz@example.com",
                            "department", "QA"
                    ))
            );
            logToolResult(result3);

            // ============================================================
            // ADIM 5: Tool Calling - findUsersByDepartment
            // Departmana gore arama yapiyoruz.
            // ============================================================
            log.info("");
            log.info(">> ADIM 5: Tool Calling - 'findUsersByDepartment' Engineering ile cagiriliyor...");
            McpSchema.CallToolResult result4 = client.callTool(
                    new McpSchema.CallToolRequest("findUsersByDepartment",
                            Map.of("department", "Engineering"))
            );
            logToolResult(result4);

            // ============================================================
            // ADIM 6: Resource Reading - users://list
            // Tum kullanicilar JSON olarak doner (yeni eklenen dahil).
            // ============================================================
            log.info("");
            log.info(">> ADIM 6: Resource Reading - 'users://list' okunuyor...");
            readResource(client, "users://list");

            // ============================================================
            // ADIM 7: Tool Calling - listMessages
            // Tum mesajlari/kampanyalari listeliyoruz.
            // ============================================================
            log.info("");
            log.info(">> ADIM 7: Tool Calling - 'listMessages' cagiriliyor...");
            logToolResult(client.callTool(
                    new McpSchema.CallToolRequest("listMessages", Map.of())
            ));

            // ============================================================
            // ADIM 8: Tool Calling - findMessagesByType
            // Sadece PUSH tipindeki mesajlari filtreliyoruz.
            // ============================================================
            log.info("");
            log.info(">> ADIM 8: Tool Calling - 'findMessagesByType' PUSH ile cagiriliyor...");
            logToolResult(client.callTool(
                    new McpSchema.CallToolRequest("findMessagesByType",
                            Map.of("msgType", "PUSH"))
            ));

            // ============================================================
            // ADIM 9: Tool Calling - getMessageStats
            // Mesaj istatistiklerini aliyoruz: tipe ve statusye gore dagilim.
            // ============================================================
            log.info("");
            log.info(">> ADIM 9: Tool Calling - 'getMessageStats' cagiriliyor...");
            logToolResult(client.callTool(
                    new McpSchema.CallToolRequest("getMessageStats", Map.of())
            ));

            // ============================================================
            // ADIM 10: Resource Reading - messages://list
            // Tum mesajlar JSON olarak doner.
            // ============================================================
            log.info("");
            log.info(">> ADIM 10: Resource Reading - 'messages://list' okunuyor...");
            readResource(client, "messages://list");

            // ============================================================
            // ADIM 11: Tool Calling - listSegments
            // Tum segmentleri listeliyoruz.
            // ============================================================
            log.info("");
            log.info(">> ADIM 11: Tool Calling - 'listSegments' cagiriliyor...");
            logToolResult(client.callTool(
                    new McpSchema.CallToolRequest("listSegments", Map.of())
            ));

            // ============================================================
            // ADIM 12: Tool Calling - findActiveSegments
            // Sadece aktif segmentleri getiriyoruz (DELETED olanlari haric).
            // ============================================================
            log.info("");
            log.info(">> ADIM 12: Tool Calling - 'findActiveSegments' cagiriliyor...");
            logToolResult(client.callTool(
                    new McpSchema.CallToolRequest("findActiveSegments", Map.of())
            ));

            // ============================================================
            // ADIM 13: Tool Calling - getSegmentStats
            // Segment istatistiklerini aliyoruz.
            // ============================================================
            log.info("");
            log.info(">> ADIM 13: Tool Calling - 'getSegmentStats' cagiriliyor...");
            logToolResult(client.callTool(
                    new McpSchema.CallToolRequest("getSegmentStats", Map.of())
            ));

            // ============================================================
            // ADIM 14: Resource Reading - segments://list
            // Tum segmentler JSON olarak doner.
            // ============================================================
            log.info("");
            log.info(">> ADIM 14: Resource Reading - 'segments://list' okunuyor...");
            readResource(client, "segments://list");
        }

        log.info("");
        log.info("========================================");
        log.info("   MCP CLIENT DEMO TAMAMLANDI");
        log.info("========================================");
    }

    private void logToolResult(McpSchema.CallToolResult result) {
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent) {
                log.info("   Sonuc: {}", textContent.text());
            }
        }
    }

    private void readResource(McpSyncClient client, String uri) {
        McpSchema.ReadResourceResult resourceResult = client.readResource(
                new McpSchema.ReadResourceRequest(uri)
        );
        for (McpSchema.ResourceContents content : resourceResult.contents()) {
            if (content instanceof McpSchema.TextResourceContents textContent) {
                // JSON cok uzun olabilir, ilk 500 karakteri goster
                String text = textContent.text();
                if (text.length() > 500) {
                    log.info("   Resource icerigi (ilk 500 karakter): {}...", text.substring(0, 500));
                } else {
                    log.info("   Resource icerigi: {}", text);
                }
            }
        }
    }
}
