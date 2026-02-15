package com.tutorial.mcpserver.repository;

import com.tutorial.mcpserver.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository.
 * MongoDB'deki "users" collection'ina erisim katmani.
 *
 * MongoRepository<User, String>:
 *   - User  → hangi document tipiyle calisacak
 *   - String → ID'nin tipi (MongoDB ObjectId string olarak saklanir)
 *
 */
public interface UserRepository extends MongoRepository<User, String> {

    List<User> findByDepartment(String department);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
