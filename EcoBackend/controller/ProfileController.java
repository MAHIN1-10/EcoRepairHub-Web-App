package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserDataRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(u -> ResponseEntity.ok(Map.of(
                        "fullName", u.getFullName(),
                        "email", u.getEmail(),
                        "role", u.getRole()
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found")));
    }
}
