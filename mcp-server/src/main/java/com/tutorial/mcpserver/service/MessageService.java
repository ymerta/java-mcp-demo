package com.tutorial.mcpserver.service;

import com.tutorial.mcpserver.model.Message;
import com.tutorial.mcpserver.repository.MessageRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Message (Kampanya/Bildirim) MCP Tool'lari.
 *
 * Bu tool'lar MCP Client (veya LLM) tarafindan cagirilarak
 * mesaj/kampanya verilerine erisim saglar.
 *
 * Gercek bir Netmera benzeri sistemde bu tool'lar:
 * - Kampanya listesi goruntuleme
 * - Kampanya durumu sorgulama
 * - Kampanya istatistikleri cekme
 * gibi islemler icin kullanilabilir.
 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Tool(description = "Lists all messages/campaigns in the database with summary info: id, type, title, status, and target audience count.")
    public String listMessages() {
        List<Message> messages = messageRepository.findAll();
        if (messages.isEmpty()) {
            return "No messages found in the database.";
        }
        StringBuilder sb = new StringBuilder("Messages in database:\n");
        for (Message msg : messages) {
            sb.append(String.format("  [%s] %-10s | %-25s | %-10s | audience: %d%n",
                    msg.getId(), msg.getMsgType(), msg.getTitle(), msg.getSendStatus(), msg.getTargetAudience()));
        }
        return sb.toString();
    }

    @Tool(description = "Gets detailed information about a specific message/campaign by its ID.")
    public String getMessageById(
            @ToolParam(description = "The MongoDB ObjectId of the message") String messageId) {
        return messageRepository.findById(messageId)
                .map(msg -> String.format(
                        "Message Details:\n" +
                        "  ID: %s\n" +
                        "  Type: %s\n" +
                        "  Method: %s\n" +
                        "  Title: %s\n" +
                        "  Content: %s\n" +
                        "  Status: %s\n" +
                        "  Style: %s\n" +
                        "  Platforms: %s\n" +
                        "  Creator: %s\n" +
                        "  Schedule: %s\n" +
                        "  Target Audience: %d\n" +
                        "  Target User: %d\n" +
                        "  Created: %s",
                        msg.getId(), msg.getMsgType(), msg.getMsgMethod(),
                        msg.getTitle(), msg.getMessage(), msg.getSendStatus(),
                        msg.getCampaignStyle(),
                        msg.getPlatforms() != null ? String.join(", ", msg.getPlatforms()) : "N/A",
                        msg.getCreator(),
                        msg.getSchedule() != null ? msg.getSchedule().getType() : "N/A",
                        msg.getTargetAudience(), msg.getTargetUser(),
                        msg.getCreateDate()))
                .orElse("Message with ID " + messageId + " not found.");
    }

    @Tool(description = "Finds messages by their type. Valid types: PUSH, EMAIL, SMS, WEB_PUSH, IN_APP")
    public String findMessagesByType(
            @ToolParam(description = "Message type to filter by: PUSH, EMAIL, SMS, WEB_PUSH, or IN_APP") String msgType) {
        List<Message> messages = messageRepository.findByMsgType(msgType.toUpperCase());
        if (messages.isEmpty()) {
            return "No messages found with type: " + msgType;
        }
        StringBuilder sb = new StringBuilder(msgType + " messages:\n");
        for (Message msg : messages) {
            sb.append(String.format("  [%s] %-25s | %-10s | audience: %d%n",
                    msg.getId(), msg.getTitle(), msg.getSendStatus(), msg.getTargetAudience()));
        }
        return sb.toString();
    }

    @Tool(description = "Finds messages by their send status. Valid statuses: DRAFT, FINISHED, STOPPED, DELETED")
    public String findMessagesByStatus(
            @ToolParam(description = "Send status to filter by: DRAFT, FINISHED, STOPPED, or DELETED") String sendStatus) {
        List<Message> messages = messageRepository.findBySendStatus(sendStatus.toUpperCase());
        if (messages.isEmpty()) {
            return "No messages found with status: " + sendStatus;
        }
        StringBuilder sb = new StringBuilder("Messages with status " + sendStatus + ":\n");
        for (Message msg : messages) {
            sb.append(String.format("  [%s] %-10s | %-25s | audience: %d%n",
                    msg.getId(), msg.getMsgType(), msg.getTitle(), msg.getTargetAudience()));
        }
        return sb.toString();
    }

    @Tool(description = "Creates a new message/campaign. Requires type, title, message content, and platforms.")
    public String createMessage(
            @ToolParam(description = "Message type: PUSH, EMAIL, SMS, WEB_PUSH, or IN_APP") String msgType,
            @ToolParam(description = "Title of the message/campaign") String title,
            @ToolParam(description = "Message content/body") String messageContent,
            @ToolParam(description = "Comma-separated platforms: ANDROID,IOS,CHROME,FIREFOX") String platforms) {
        List<String> platformList = List.of(platforms.toUpperCase().split(","));
        Message msg = new Message(
                msgType.toUpperCase(), "CAMPAIGN", title, messageContent,
                "DRAFT", platformList, "MCP-Client",
                "TEXT", 0, 0,
                new Message.Schedule("NOW", null)
        );
        Message saved = messageRepository.save(msg);
        return String.format("Message created: [%s] %s | %s | status: DRAFT",
                saved.getId(), saved.getMsgType(), saved.getTitle());
    }

    @Tool(description = "Returns statistics about messages: count by type and count by status.")
    public String getMessageStats() {
        long total = messageRepository.count();
        if (total == 0) {
            return "No messages in the database.";
        }

        StringBuilder sb = new StringBuilder("Message Statistics:\n");
        sb.append(String.format("  Total messages: %d%n%n", total));

        sb.append("  By Type:\n");
        for (String type : List.of("PUSH", "EMAIL", "SMS", "WEB_PUSH", "IN_APP")) {
            long count = messageRepository.countByMsgType(type);
            if (count > 0) {
                sb.append(String.format("    %-10s : %d%n", type, count));
            }
        }

        sb.append("\n  By Status:\n");
        for (String status : List.of("DRAFT", "FINISHED", "STOPPED", "DELETED")) {
            long count = messageRepository.countBySendStatus(status);
            if (count > 0) {
                sb.append(String.format("    %-10s : %d%n", status, count));
            }
        }

        return sb.toString();
    }
}
