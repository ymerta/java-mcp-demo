package com.tutorial.mcpserver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB Document olarak tanimlanan User modeli.
 *
 * @Document: Bu sinifin MongoDB'de bir collection'a (tablo benzeri) karsilik geldigini belirtir.
 *            collection = "users" → MongoDB'deki collection adi.
 *
 * JPA'dan farki:
 *   - @Entity yerine @Document kullanilir
 *   - @Table yerine collection parametresi
 *   - @Column yerine @Field (opsiyonel, alan adi ayni ise gerekli degil)
 *   - @GeneratedValue yok → MongoDB kendi ObjectId'sini otomatik uretir
 *   - ID tipi Long yerine String (MongoDB ObjectId string formatindadir)
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;    // MongoDB ObjectId - otomatik uretilir (ornek: "507f1f77bcf86cd799439011")

    private String name;

    @Indexed(unique = true)   // Bu alanda unique index olusturur (email tekrar edemez)
    private String email;

    private String department;

    private String password;  // BCrypt hashed password

    // Default constructor
    public User() {}

    public User(String name, String email, String department) {
        this.name = name;
        this.email = email;
        this.department = department;
    }

    public User(String name, String email, String department, String password) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', name='%s', email='%s', department='%s'}",
                id, name, email, department);  // Password excluded for security
    }
}
