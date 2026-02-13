package com.tutorial.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User MCP Tool'lari REST Controller.
 *
 * MCP Server'daki 5 user tool'unu HTTP endpoint olarak sunar.
 * Her endpoint, MCP protokolu uzerinden tool'u cagirir ve
 * sonucu JSON olarak dondurur.
 *
 * Tool'lar:
 *   GET    /api/tools/users                     → listAllUsers
 *   GET    /api/tools/users/{id}                → getUserById
 *   POST   /api/tools/users                     → createUser
 *   DELETE /api/tools/users/{id}                → deleteUser
 *   GET    /api/tools/users/department/{dept}   → findUsersByDepartment
 *
 * Resource:
 *   GET    /api/resources/users                 → users://list
 */
@RestController
public class UserToolController {

    private final McpSyncClient mcpClient;

    public UserToolController(List<McpSyncClient> mcpClients) {
        this.mcpClient = mcpClients.get(0);
    }

    @GetMapping("/api/tools/users")
    public Map<String, Object> listAllUsers() {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("listAllUsers", Map.of())
        );
        return buildResponse("listAllUsers", result);
    }

    @GetMapping("/api/tools/users/{id}")
    public Map<String, Object> getUserById(@PathVariable String id) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("getUserById", Map.of("userId", id))
        );
        return buildResponse("getUserById", result);
    }

    @PostMapping("/api/tools/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> body) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("createUser", Map.of(
                        "name", body.getOrDefault("name", ""),
                        "email", body.getOrDefault("email", ""),
                        "department", body.getOrDefault("department", "")
                ))
        );
        return buildResponse("createUser", result);
    }

    @DeleteMapping("/api/tools/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable String id) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("deleteUser", Map.of("userId", id))
        );
        return buildResponse("deleteUser", result);
    }

    @GetMapping("/api/tools/users/department/{department}")
    public Map<String, Object> findUsersByDepartment(@PathVariable String department) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("findUsersByDepartment",
                        Map.of("department", department))
        );
        return buildResponse("findUsersByDepartment", result);
    }

    @GetMapping("/api/resources/users")
    public Map<String, Object> usersResource() {
        McpSchema.ReadResourceResult result = mcpClient.readResource(
                new McpSchema.ReadResourceRequest("users://list")
        );
        return buildResourceResponse("users://list", result);
    }

    // ── Yardimci Metodlar ──────────────────────────────────────

    private Map<String, Object> buildResponse(String toolName, McpSchema.CallToolResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tool", toolName);
        response.put("result", extractText(result));
        return response;
    }

    static String extractText(McpSchema.CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent) {
                sb.append(textContent.text());
            }
        }
        return sb.toString();
    }

    static Map<String, Object> buildResourceResponse(String uri, McpSchema.ReadResourceResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resource", uri);
        for (McpSchema.ResourceContents content : result.contents()) {
            if (content instanceof McpSchema.TextResourceContents textContent) {
                response.put("mimeType", textContent.mimeType());
                response.put("data", textContent.text());
            }
        }
        return response;
    }
}
