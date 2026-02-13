package com.tutorial.mcpserver.service;

import com.tutorial.mcpserver.model.Segment;
import com.tutorial.mcpserver.repository.SegmentRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Segment (Kullanici Segmenti) MCP Tool'lari.
 *
 * Segmentler, belirli kriterlere gore gruplanmis kullanici kitlelerini temsil eder.
 * Ornegin: "Son 7 gunde giris yapan Android kullanicilari" veya "Push izinli kullanicilar"
 *
 * Bu tool'lar MCP Client (veya LLM) tarafindan cagirilarak
 * segment verilerine erisim saglar.
 */
@Service
public class SegmentService {

    private final SegmentRepository segmentRepository;

    public SegmentService(SegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
    }

    @Tool(description = "Lists all segments with summary info: code, name, type, status, and user count.")
    public String listSegments() {
        List<Segment> segments = segmentRepository.findAll();
        if (segments.isEmpty()) {
            return "No segments found in the database.";
        }
        StringBuilder sb = new StringBuilder("Segments in database:\n");
        for (Segment seg : segments) {
            sb.append(String.format("  [%s] %-5s | %-25s | %-10s | %-18s | users: %d%n",
                    seg.getId(), seg.getCode(), seg.getName(),
                    seg.getEntityStatus(), seg.getSegmentStatus(), seg.getUserCount()));
        }
        return sb.toString();
    }

    @Tool(description = "Gets detailed information about a segment by its unique code (5-character code like 'svysw').")
    public String getSegmentByCode(
            @ToolParam(description = "The unique 5-character segment code") String code) {
        return segmentRepository.findByCode(code)
                .map(seg -> String.format(
                        "Segment Details:\n" +
                        "  ID: %s\n" +
                        "  Code: %s\n" +
                        "  Name: %s\n" +
                        "  Type: %s\n" +
                        "  Segment Status: %s\n" +
                        "  Entity Status: %s\n" +
                        "  Created By: %s\n" +
                        "  User Count: %d\n" +
                        "  Device Counts: Android=%d, iOS=%d, Total=%d\n" +
                        "  Created: %s\n" +
                        "  Updated: %s",
                        seg.getId(), seg.getCode(), seg.getName(),
                        seg.getSegmentType(), seg.getSegmentStatus(), seg.getEntityStatus(),
                        seg.getCreatedBy(), seg.getUserCount(),
                        seg.getDeviceCounts() != null ? seg.getDeviceCounts().getAndroid() : 0,
                        seg.getDeviceCounts() != null ? seg.getDeviceCounts().getIos() : 0,
                        seg.getDeviceCounts() != null ? seg.getDeviceCounts().getTotal() : 0,
                        seg.getDate(), seg.getUpdateDate()))
                .orElse("Segment with code '" + code + "' not found.");
    }

    @Tool(description = "Finds segments by their calculation status. Valid statuses: READY, WAITING_CALCULATE, WAITING_DELETED")
    public String findSegmentsByStatus(
            @ToolParam(description = "Segment status: READY, WAITING_CALCULATE, or WAITING_DELETED") String segmentStatus) {
        List<Segment> segments = segmentRepository.findBySegmentStatus(segmentStatus.toUpperCase());
        if (segments.isEmpty()) {
            return "No segments found with status: " + segmentStatus;
        }
        StringBuilder sb = new StringBuilder("Segments with status " + segmentStatus + ":\n");
        for (Segment seg : segments) {
            sb.append(String.format("  [%s] %-25s | users: %d%n",
                    seg.getCode(), seg.getName(), seg.getUserCount()));
        }
        return sb.toString();
    }

    @Tool(description = "Lists only active segments (entityStatus = ACTIVE). Filters out deleted segments.")
    public String findActiveSegments() {
        List<Segment> segments = segmentRepository.findByEntityStatus("ACTIVE");
        if (segments.isEmpty()) {
            return "No active segments found.";
        }
        StringBuilder sb = new StringBuilder("Active segments:\n");
        for (Segment seg : segments) {
            sb.append(String.format("  [%s] %-25s | %-18s | users: %d%n",
                    seg.getCode(), seg.getName(), seg.getSegmentStatus(), seg.getUserCount()));
        }
        return sb.toString();
    }

    @Tool(description = "Returns statistics about segments: counts by type, status, and total user reach.")
    public String getSegmentStats() {
        long total = segmentRepository.count();
        if (total == 0) {
            return "No segments in the database.";
        }

        long activeCount = segmentRepository.countByEntityStatus("ACTIVE");
        long deletedCount = segmentRepository.countByEntityStatus("DELETED");

        // Toplam kullanici erisimi hesapla
        List<Segment> allSegments = segmentRepository.findAll();
        int totalUsers = allSegments.stream().mapToInt(Segment::getUserCount).sum();

        StringBuilder sb = new StringBuilder("Segment Statistics:\n");
        sb.append(String.format("  Total segments: %d%n", total));
        sb.append(String.format("  Active: %d | Deleted: %d%n%n", activeCount, deletedCount));

        sb.append("  By Status:\n");
        for (String status : List.of("READY", "WAITING_CALCULATE", "WAITING_DELETED")) {
            long count = segmentRepository.findBySegmentStatus(status).size();
            if (count > 0) {
                sb.append(String.format("    %-20s : %d%n", status, count));
            }
        }

        sb.append(String.format("%n  Total user reach: %,d%n", totalUsers));

        return sb.toString();
    }
}
