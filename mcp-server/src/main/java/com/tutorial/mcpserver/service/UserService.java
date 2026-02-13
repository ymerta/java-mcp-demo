package com.tutorial.mcpserver.service;

import com.tutorial.mcpserver.model.User;
import com.tutorial.mcpserver.repository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Tool'larinin tanimlandigi servis sinifi.
 *
 * @Tool annotation'i ile isaretlenen her method, MCP protokolu uzerinden
 * client'larin kesfedip cagirabileceği bir "tool" olarak sunulur.
 *
 * MCP Client bu tool'lari su sekilde gorecek:
 *   - listAllUsers     -> Tum kullanicilari listeler
 *   - getUserById      -> ID ile kullanici getirir
 *   - createUser       -> Yeni kullanici olusturur
 *   - deleteUser       -> Kullanici siler
 *   - findUsersByDepartment -> Departmana gore arar
 *
 * NOT: MongoDB gecisiyle birlikte userId tipi Long'dan String'e degisti.
 * Cunku MongoDB ObjectId string formatindadir (ornek: "507f1f77bcf86cd799439011").
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Tool(description = "Lists all users in the database. Returns a formatted string of all users with their id, name, email, and department.")
    public String listAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return "No users found in the database.";
        }
        StringBuilder sb = new StringBuilder("Users in database:\n");
        for (User user : users) {
            sb.append(String.format("  [ID: %s] %s (%s) - %s%n",
                    user.getId(), user.getName(), user.getEmail(), user.getDepartment()));
        }
        return sb.toString();
    }

    @Tool(description = "Gets a specific user by their ID. Returns user details or a not-found message.")
    public String getUserById(
            @ToolParam(description = "The unique ID of the user to retrieve (MongoDB ObjectId string)") String userId) {
        return userRepository.findById(userId)
                .map(user -> String.format("User found: [ID: %s] %s (%s) - %s",
                        user.getId(), user.getName(), user.getEmail(), user.getDepartment()))
                .orElse("User with ID " + userId + " not found.");
    }

    @Tool(description = "Creates a new user in the database. Requires name, email, and department. Returns the created user's information.")
    public String createUser(
            @ToolParam(description = "Full name of the user") String name,
            @ToolParam(description = "Email address of the user (must be unique)") String email,
            @ToolParam(description = "Department the user belongs to") String department) {
        if (userRepository.existsByEmail(email)) {
            return "Error: A user with email '" + email + "' already exists.";
        }
        User user = new User(name, email, department);
        User saved = userRepository.save(user);
        return String.format("User created successfully: [ID: %s] %s (%s) - %s",
                saved.getId(), saved.getName(), saved.getEmail(), saved.getDepartment());
    }

    @Tool(description = "Deletes a user from the database by their ID. Returns confirmation or error message.")
    public String deleteUser(
            @ToolParam(description = "The unique ID of the user to delete (MongoDB ObjectId string)") String userId) {
        if (!userRepository.existsById(userId)) {
            return "Error: User with ID " + userId + " not found.";
        }
        userRepository.deleteById(userId);
        return "User with ID " + userId + " has been deleted successfully.";
    }

    @Tool(description = "Finds all users belonging to a specific department. Returns matching users or a message if none found.")
    public String findUsersByDepartment(
            @ToolParam(description = "The department name to search for") String department) {
        List<User> users = userRepository.findByDepartment(department);
        if (users.isEmpty()) {
            return "No users found in department: " + department;
        }
        StringBuilder sb = new StringBuilder("Users in " + department + " department:\n");
        for (User user : users) {
            sb.append(String.format("  [ID: %s] %s (%s)%n",
                    user.getId(), user.getName(), user.getEmail()));
        }
        return sb.toString();
    }
}
