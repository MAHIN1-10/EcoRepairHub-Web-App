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

    @org.springframework.beans.factory.annotation.Value("${app.admin-signup-code}")
    private String adminSignupCode;

    @Autowired
    private backend.example.EcoBackend.service.NotificationService notificationService;

    @PostMapping("/saveData")
    public ResponseEntity<?> saveData(@RequestBody SignupRequest req) {

        if (!req.getPassword().equals(req.getConfirm())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));
        }

        Role role;
        try {
            role = req.getRole() == null || req.getRole().isBlank() ? Role.USER : Role.valueOf(req.getRole().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid account type"));
        }
        if (role == Role.ADMIN && !adminSignupCode.equals(req.getAdminCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Invalid admin access code"));
        }
        if (userDataRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "An account with this email already exists"));
        }

        UserData userData = new UserData(req.getFullname(), req.getEmail(), passwordEncoder.encode(req.getPassword()), role);


        UserData saved = userDataService.saveUserData(userData);
        notificationService.notify(saved, "info", "Welcome to EcoRecycle!",
                "Your account is ready. Schedule your first e-waste pickup to start earning EcoPoints.");

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