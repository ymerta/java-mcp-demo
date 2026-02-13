package com.tutorial.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Segment MCP Tool'lari REST Controller.
 *
 * MCP Server'daki 5 segment tool'unu HTTP endpoint olarak sunar.
 *
 * Tool'lar:
 *   GET  /api/tools/segments                    → listSegments
 *   GET  /api/tools/segments/code/{code}        → getSegmentByCode
 *   GET  /api/tools/segments/status/{status}    → findSegmentsByStatus
 *   GET  /api/tools/segments/active             → findActiveSegments
 *   GET  /api/tools/segments/stats              → getSegmentStats
 *
 * Resource:
 *   GET  /api/resources/segments                → segments://list
 */
@RestController
public class SegmentToolController {

    private final McpSyncClient mcpClient;

    public SegmentToolController(List<McpSyncClient> mcpClients) {
        this.mcpClient = mcpClients.get(0);
    }

    @GetMapping("/api/tools/segments")
    public Map<String, Object> listSegments() {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("listSegments", Map.of())
        );
        return buildResponse("listSegments", result);
    }

    @GetMapping("/api/tools/segments/code/{code}")
    public Map<String, Object> getSegmentByCode(@PathVariable String code) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("getSegmentByCode",
                        Map.of("code", code))
        );
        return buildResponse("getSegmentByCode", result);
    }

    @GetMapping("/api/tools/segments/status/{status}")
    public Map<String, Object> findSegmentsByStatus(@PathVariable String status) {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("findSegmentsByStatus",
                        Map.of("segmentStatus", status))
        );
        return buildResponse("findSegmentsByStatus", result);
    }

    @GetMapping("/api/tools/segments/active")
    public Map<String, Object> findActiveSegments() {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("findActiveSegments", Map.of())
        );
        return buildResponse("findActiveSegments", result);
    }

    @GetMapping("/api/tools/segments/stats")
    public Map<String, Object> getSegmentStats() {
        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("getSegmentStats", Map.of())
        );
        return buildResponse("getSegmentStats", result);
    }

    @GetMapping("/api/resources/segments")
    public Map<String, Object> segmentsResource() {
        McpSchema.ReadResourceResult result = mcpClient.readResource(
                new McpSchema.ReadResourceRequest("segments://list")
        );
        return UserToolController.buildResourceResponse("segments://list", result);
    }

    private Map<String, Object> buildResponse(String toolName, McpSchema.CallToolResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tool", toolName);
        response.put("result", UserToolController.extractText(result));
        return response;
    }
}
