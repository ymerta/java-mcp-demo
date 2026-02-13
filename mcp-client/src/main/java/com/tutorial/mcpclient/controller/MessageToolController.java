package com.tutorial.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Message/Kampanya MCP Tool'lari REST Controller.
 *
 * MCP Server'daki 6 message tool'unu HTTP endpoint olarak sunar.
 *
 * Tool'lar:
 *   GET  /api/tools/messages                    → listMessages
 *   GET  /api/tools/messages/{id}               → getMessageById
 *   GET  /api/tools/messages/type/{type}        → findMessagesByType
 *   GET  /api/tools/messages/status/{status}    → findMessagesByStatus
 *   POST /api/tools/messages                    → createMessage
 *   GET  /api/tools/messages/stats              → getMessageStats
 *
 * Resource:
 *   GET  /api/resources/messages                → messages://list
 */
@RestController
public class MessageToolController {

    private final McpSyncClient mcpClient;

    public MessageToolController(List<McpSyncClient> mcpClients) {
        this.mcpClient = mcpClients.get(0);
    }

    @GetMapping("/api/tools/messages")
    public Map<String, Object> listMessages() {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("listMessages", Map.of())
        );
        return buildResponse("listMessages", result);
    }

    @GetMapping("/api/tools/messages/{id}")
    public Map<String, Object> getMessageById(@PathVariable String id) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("getMessageById",
                        Map.of("messageId", id))
        );
        return buildResponse("getMessageById", result);
    }

    @GetMapping("/api/tools/messages/type/{type}")
    public Map<String, Object> findMessagesByType(@PathVariable String type) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("findMessagesByType",
                        Map.of("msgType", type))
        );
        return buildResponse("findMessagesByType", result);
    }

    @GetMapping("/api/tools/messages/status/{status}")
    public Map<String, Object> findMessagesByStatus(@PathVariable String status) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("findMessagesByStatus",
                        Map.of("sendStatus", status))
        );
        return buildResponse("findMessagesByStatus", result);
    }

    @PostMapping("/api/tools/messages")
    public Map<String, Object> createMessage(@RequestBody Map<String, String> body) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("createMessage", Map.of(
                        "msgType", body.getOrDefault("msgType", "PUSH"),
                        "title", body.getOrDefault("title", ""),
                        "messageContent", body.getOrDefault("message", ""),
                        "platforms", body.getOrDefault("platforms", "ANDROID,IOS")
                ))
        );
        return buildResponse("createMessage", result);
    }

    @GetMapping("/api/tools/messages/stats")
    public Map<String, Object> getMessageStats() {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("getMessageStats", Map.of())
        );
        return buildResponse("getMessageStats", result);
    }

    @GetMapping("/api/resources/messages")
    public Map<String, Object> messagesResource() {
        McpSchema.ReadResourceResult result = mcpClient.readResource(
                new McpSchema.ReadResourceRequest("messages://list")
        );
        return UserToolController.buildResourceResponse("messages://list", result);
    }

    private Map<String, Object> buildResponse(String toolName, McpSchema.CallToolResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tool", toolName);
        response.put("result", UserToolController.extractText(result));
        return response;
    }
}
