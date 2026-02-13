package com.tutorial.mcpserver.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.mcpserver.repository.MessageRepository;
import com.tutorial.mcpserver.repository.SegmentRepository;
import com.tutorial.mcpserver.repository.UserRepository;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP Resource tanimlari.
 *
 * 3 resource tanimlanir:
 *   - users://list    → Tum kullanicilar JSON
 *   - messages://list → Tum mesajlar/kampanyalar JSON
 *   - segments://list → Tum segmentler JSON
 */
@Configuration
public class UserResourceProvider {

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> mcpResources(
            UserRepository userRepository,
            MessageRepository messageRepository,
            SegmentRepository segmentRepository,
            ObjectMapper objectMapper) {

        // Resource 1: users://list
        var usersSpec = createResourceSpec(
                "users://list", "All Users",
                "Returns a JSON list of all users in the database",
                () -> userRepository.findAll(), objectMapper);

        // Resource 2: messages://list
        var messagesSpec = createResourceSpec(
                "messages://list", "All Messages",
                "Returns a JSON list of all messages/campaigns in the database",
                () -> messageRepository.findAll(), objectMapper);

        // Resource 3: segments://list
        var segmentsSpec = createResourceSpec(
                "segments://list", "All Segments",
                "Returns a JSON list of all segments in the database",
                () -> segmentRepository.findAll(), objectMapper);

        return List.of(usersSpec, messagesSpec, segmentsSpec);
    }

    /**
     * Yardimci method: Tekrarlanan resource tanimi kodunu azaltir.
     */
    private McpServerFeatures.SyncResourceSpecification createResourceSpec(
            String uri, String name, String description,
            DataSupplier dataSupplier, ObjectMapper objectMapper) {

        var resource = new McpSchema.Resource(uri, name, description, "application/json", null);

        return new McpServerFeatures.SyncResourceSpecification(
                resource,
                (exchange, request) -> {
                    try {
                        String json = objectMapper.writeValueAsString(dataSupplier.get());
                        return new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(
                                        request.uri(), "application/json", json)));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to read resource: " + uri, e);
                    }
                }
        );
    }

    @FunctionalInterface
    private interface DataSupplier {
        Object get();
    }
}
