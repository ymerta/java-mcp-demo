package com.tutorial.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Discovery REST Controller.
 *
 * MCP Server'daki tum tool'lari ve resource'lari kesfetmek icin
 * kullanilir. Sunum icin guzel bir baslangic noktasidir:
 * "Bakın, server'da sunlar var" diyerek gosterilir.
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final McpSyncClient mcpClient;

    public DiscoveryController(List<McpSyncClient> mcpClients) {
        this.mcpClient = mcpClients.get(0);
    }

    /**
     * MCP Server'daki tum tool'lari listeler.
     * Her tool icin isim, aciklama ve parametre bilgisi doner.
     */
    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        McpSchema.ListToolsResult result = mcpClient.listTools();

        List<Map<String, Object>> tools = result.tools().stream()
                .map(tool -> {
                    Map<String, Object> toolInfo = new LinkedHashMap<>();
                    toolInfo.put("name", tool.name());
                    toolInfo.put("description", tool.description());
                    return toolInfo;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalTools", tools.size());
        response.put("tools", tools);
        return response;
    }

    /**
     * MCP Server'daki tum resource'lari listeler.
     * Resource'lar read-only veri kaynaklaridir.
     */
    @GetMapping("/resources")
    public Map<String, Object> listResources() {
        McpSchema.ListResourcesResult result = mcpClient.listResources();

        List<Map<String, String>> resources = result.resources().stream()
                .map(res -> {
                    Map<String, String> resInfo = new LinkedHashMap<>();
                    resInfo.put("uri", res.uri());
                    resInfo.put("name", res.name());
                    resInfo.put("description", res.description());
                    return resInfo;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalResources", resources.size());
        response.put("resources", resources);
        return response;
    }
}
