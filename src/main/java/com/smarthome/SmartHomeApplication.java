package com.smarthome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.smarthome.entity.User;
import com.smarthome.repository.UserRepository;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.smarthome.repository")
@EntityScan(basePackages = "com.smarthome.entity")
@EnableAsync
public class SmartHomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartHomeApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  🏠 SmartHome Backend đang chạy!");
        System.out.println("  👉 http://localhost:8080");
        System.out.println("==============================================");
    }

    @Bean
    public CommandLineRunner createAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            User admin = userRepository.findByEmail("admin@smarthome.vn").orElse(null);
            if (admin == null) {
                admin = new User();
                admin.setEmail("admin@smarthome.vn");
                admin.setFullName("Quản trị viên");
                admin.setRole(User.Role.ADMIN);
                admin.setStatus(User.Status.ACTIVE);
            }
            admin.setPassword(passwordEncoder.encode("123456"));
            userRepository.save(admin);
            System.out.println("✅ Tự động đặt lại mật khẩu Admin: admin@smarthome.vn / 123456");
        };
    }
}
