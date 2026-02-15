package com.tutorial.mcpserver.config;

import com.tutorial.mcpserver.model.Message;
import com.tutorial.mcpserver.model.Segment;
import com.tutorial.mcpserver.model.User;
import com.tutorial.mcpserver.repository.MessageRepository;
import com.tutorial.mcpserver.repository.SegmentRepository;
import com.tutorial.mcpserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB'ye dummy veri yukleyen seeder.
 *
 * 3 collection'a veri yukler:
 *   - users     → 5 kullanici
 *   - messages  → 10 mesaj/kampanya (Netmera benzeri)
 *   - segments  → 8 segment (Netmera benzeri)
 *
 * Her collection icin: eger zaten veri varsa tekrar yuklemez.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SegmentRepository segmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      MessageRepository messageRepository,
                      SegmentRepository segmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.segmentRepository = segmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedMessages();
        seedSegments();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users: {} kayit mevcut, seed atlaniyor.", userRepository.count());
            return;
        }
        // Tum kullanicilarin sifresi: "password123" (BCrypt hash'lenecek)
        String hashedPassword = passwordEncoder.encode("password123");

        List<User> users = List.of(
                new User("Ahmet Yilmaz", "ahmet@example.com", "Engineering", hashedPassword),
                new User("Elif Kaya", "elif@example.com", "Marketing", hashedPassword),
                new User("Mehmet Demir", "mehmet@example.com", "Engineering", hashedPassword),
                new User("Zeynep Arslan", "zeynep@example.com", "HR", hashedPassword),
                new User("Can Ozturk", "can@example.com", "Marketing", hashedPassword)
        );
        userRepository.saveAll(users);
        log.info("{} adet dummy kullanici yuklendi.", users.size());
    }

    private void seedMessages() {
        if (messageRepository.count() > 0) {
            log.info("Messages: {} kayit mevcut, seed atlaniyor.", messageRepository.count());
            return;
        }
        List<Message> messages = List.of(
                new Message("PUSH", "CAMPAIGN", "Yaz Kampanyasi",
                        "Bu yaz firsatlarini kacirmayin!", "FINISHED",
                        List.of("ANDROID", "IOS"), "Elif Kaya", "TEXT",
                        15000, 14200, new Message.Schedule("NOW", null)),

                new Message("EMAIL", "CAMPAIGN", "Hosgeldin Emaili",
                        "Aramiza hosgeldiniz! Ilk alisverisinize ozel %20 indirim.", "FINISHED",
                        List.of("ANDROID", "IOS"), "Zeynep Arslan", "TEXT",
                        8500, 8100, new Message.Schedule("NOW", null)),

                new Message("PUSH", "CAMPAIGN", "Indirim Bildirimi",
                        "Secili urunlerde %50 indirim!", "STOPPED",
                        List.of("ANDROID"), "Ahmet Yilmaz", "MEDIA",
                        12000, 5000, new Message.Schedule("NOW", null)),

                new Message("EMAIL", "CAMPAIGN", "Haftalik Bulten",
                        "Bu haftanin ozetini inceleyin.", "DRAFT",
                        List.of("ANDROID", "IOS"), "Can Ozturk", "TEXT",
                        20000, 0, new Message.Schedule("SCHEDULED", null)),

                new Message("SMS", "TRANSACTIONAL", "SMS Dogrulama",
                        "Dogrulama kodunuz: 123456", "FINISHED",
                        List.of("ANDROID", "IOS"), "System", "TEXT",
                        5000, 4950, new Message.Schedule("NOW", null)),

                new Message("PUSH", "CAMPAIGN", "Flash Sale",
                        "24 saat icinde biten super firsatlar!", "FINISHED",
                        List.of("ANDROID", "IOS"), "Elif Kaya", "MEDIA",
                        25000, 23500, new Message.Schedule("NOW", null)),

                new Message("WEB_PUSH", "CAMPAIGN", "Web Push Testi",
                        "Web tarayicisi bildirimi testi", "DRAFT",
                        List.of("CHROME", "FIREFOX"), "Mehmet Demir", "TEXT",
                        3000, 0, new Message.Schedule("NOW", null)),

                new Message("IN_APP", "CAMPAIGN", "Anket Daveti",
                        "Deneyiminizi degerlendirin!", "FINISHED",
                        List.of("ANDROID", "IOS"), "Zeynep Arslan", "TEXT",
                        10000, 3200, new Message.Schedule("NOW", null)),

                new Message("PUSH", "CAMPAIGN", "Sepet Hatirlatma",
                        "Sepetinizdeki urunler sizi bekliyor!", "FINISHED",
                        List.of("ANDROID", "IOS"), "Ahmet Yilmaz", "TEXT",
                        7500, 7100, new Message.Schedule("NOW", null)),

                new Message("SMS", "CAMPAIGN", "Promosyon SMS",
                        "Ozel promosyon kodunuz: INDIRIM20", "DELETED",
                        List.of("ANDROID"), "Can Ozturk", "TEXT",
                        4000, 3800, new Message.Schedule("NOW", null))
        );
        messageRepository.saveAll(messages);
        log.info("{} adet dummy mesaj yuklendi.", messages.size());
    }

    private void seedSegments() {
        if (segmentRepository.count() > 0) {
            log.info("Segments: {} kayit mevcut, seed atlaniyor.", segmentRepository.count());
            return;
        }
        List<Segment> segments = List.of(
                new Segment("aktif", "Aktif Kullanicilar", "STANDARD", "READY", "ACTIVE",
                        "Elif Kaya", 12500, new Segment.DeviceCounts(7500, 5000, 12500)),

                new Segment("son7g", "Son 7 Gun Giris", "STANDARD", "READY", "ACTIVE",
                        "Ahmet Yilmaz", 8300, new Segment.DeviceCounts(5000, 3300, 8300)),

                new Segment("pshiz", "Push Izinli", "STANDARD", "READY", "ACTIVE",
                        "Zeynep Arslan", 15000, new Segment.DeviceCounts(9000, 6000, 15000)),

                new Segment("emabo", "Email Aboneleri", "STANDARD", "WAITING_CALCULATE", "ACTIVE",
                        "Can Ozturk", 0, new Segment.DeviceCounts(0, 0, 0)),

                new Segment("andkl", "Android Kullanicilar", "STANDARD", "READY", "ACTIVE",
                        "Mehmet Demir", 9200, new Segment.DeviceCounts(9200, 0, 9200)),

                new Segment("mxpnl", "Mixpanel Import", "CONNECTORS_SEGMENT", "READY", "ACTIVE",
                        "Elif Kaya", 3400, new Segment.DeviceCounts(2000, 1400, 3400)),

                new Segment("esksg", "Eski Segment", "STANDARD", "WAITING_DELETED", "DELETED",
                        "Ahmet Yilmaz", 0, new Segment.DeviceCounts(0, 0, 0)),

                new Segment("vipms", "VIP Musteriler", "STANDARD", "READY", "ACTIVE",
                        "Zeynep Arslan", 1200, new Segment.DeviceCounts(700, 500, 1200))
        );
        segmentRepository.saveAll(segments);
        log.info("{} adet dummy segment yuklendi.", segments.size());
    }
}
