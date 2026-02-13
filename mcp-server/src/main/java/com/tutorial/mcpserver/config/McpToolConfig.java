package com.tutorial.mcpserver.config;

import com.tutorial.mcpserver.service.MessageService;
import com.tutorial.mcpserver.service.SegmentService;
import com.tutorial.mcpserver.service.UserService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Tool kayit konfigurasyonu.
 *
 * MethodToolCallbackProvider, @Tool annotation'i ile isaretlenmis
 * tum methodlari tarar ve MCP protokolune tool olarak kaydeder.
 *
 * Toplam 16 tool kaydedilir:
 *   - UserService    → 5 tool (listAllUsers, getUserById, createUser, deleteUser, findUsersByDepartment)
 *   - MessageService → 6 tool (listMessages, getMessageById, findMessagesByType, findMessagesByStatus, createMessage, getMessageStats)
 *   - SegmentService → 5 tool (listSegments, getSegmentByCode, findSegmentsByStatus, findActiveSegments, getSegmentStats)
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider allTools(UserService userService,
                                         MessageService messageService,
                                         SegmentService segmentService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(userService, messageService, segmentService)
                .build();
    }
}
