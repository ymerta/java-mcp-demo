package com.tutorial.mcpserver.repository;

import com.tutorial.mcpserver.model.Segment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Segment collection'ina erisim katmani.
 *
 * Spring Data MongoDB, method isimlerinden otomatik sorgu uretir:
 *   findByEntityStatus("ACTIVE") → db.segments.find({entityStatus: "ACTIVE"})
 *   findByCode("svysw") → db.segments.find({code: "svysw"})
 */
public interface SegmentRepository extends MongoRepository<Segment, String> {

    List<Segment> findBySegmentType(String segmentType);

    List<Segment> findByEntityStatus(String entityStatus);

    List<Segment> findBySegmentStatus(String segmentStatus);

    Optional<Segment> findByCode(String code);

    long countByEntityStatus(String entityStatus);
}
