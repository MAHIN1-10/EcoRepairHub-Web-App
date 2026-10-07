package backend.example.EcoBackend.config;

import backend.example.EcoBackend.entity.Role;
import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.repository.UserDataRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final String ADMIN_EMAIL = "admin@ecorepair.com";
    private static final String ADMIN_PASSWORD = "admin123";

    // Creates the default admin on startup if it doesn't exist yet.
    @Bean
    public CommandLineRunner seedAdmin(UserDataRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.findByEmail(ADMIN_EMAIL).isEmpty()) {
                users.save(new UserData("Admin", ADMIN_EMAIL, encoder.encode(ADMIN_PASSWORD), Role.ADMIN));
            }
        };
    }
}
