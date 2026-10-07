package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.dto.LoginRequest;
import backend.example.EcoBackend.dto.SignupRequest;
import backend.example.EcoBackend.entity.Role;
import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.entity.UserStatus;
import backend.example.EcoBackend.repository.UserDataRepository;
import backend.example.EcoBackend.service.UserDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserDataController {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/saveData")
    public ResponseEntity<?> saveData(@RequestBody SignupRequest req) {

        if (!req.getPassword().equals(req.getConfirm())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));
        }

        UserData userData = new UserData(req.getFullname(), req.getEmail(), passwordEncoder.encode(req.getPassword()), Role.USER);


        UserData saved = userDataService.saveUserData(userData);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Account created successfully",
                "id", saved.getId(),
                "fullName", saved.getFullName(),
                "email", saved.getEmail()
        ));
    }

    @Autowired
    private UserDataRepository userDataRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return userDataRepository.findByEmail(req.getEmail())
                .filter(user -> passwordEncoder.matches(req.getPassword(), user.getPassword()))
                .map(user -> {
                    if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.DEACTIVATED) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "This account has been " + user.getStatus().name().toLowerCase()));
                    }
                    return ResponseEntity.ok(Map.of(
                            "message", "Login successful",
                            "fullName", user.getFullName(),
                            "email", user.getEmail(),
                            "role", user.getRole()
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password")));
    }
















}