package com.tutorial.mcpserver.repository;

import com.tutorial.mcpserver.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Message collection'ina erisim katmani.
 *
 * Spring Data MongoDB, method isimlerinden otomatik sorgu uretir:
 *   findByMsgType("PUSH")  → db.messages.find({msgType: "PUSH"})
 *   countBySendStatus("FINISHED") → db.messages.count({sendStatus: "FINISHED"})
 *   findByPlatformsContaining("ANDROID") → platforms array'inde "ANDROID" icerenleri bulur
 */
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByMsgType(String msgType);

    List<Message> findBySendStatus(String sendStatus);

    List<Message> findByPlatformsContaining(String platform);

    long countByMsgType(String msgType);

    long countBySendStatus(String sendStatus);
}
