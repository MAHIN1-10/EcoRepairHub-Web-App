package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.entity.UserStatus;
import backend.example.EcoBackend.repository.RepairRequestRepository;
import backend.example.EcoBackend.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminUserController {

    @Autowired
    private UserDataRepository userRepository;

    @Autowired
    private RepairRequestRepository requestRepository;

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream().map(u -> {
            UserStatus status = u.getStatus() == null ? UserStatus.ACTIVE : u.getStatus();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("status", status.name().toLowerCase());
            m.put("pickups", requestRepository.countByUserEmail(u.getEmail()));
            m.put("points", 0); // replace when you store points
            m.put("createdAt", u.getCreatedAt() == null ? "" : u.getCreatedAt().toString());
            return m;
        }).toList();
    }

    @PutMapping("/users/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        UserStatus status;
        try {
            status = UserStatus.valueOf(String.valueOf(body.get("status")).toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status"));
        }

        Optional<UserData> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        UserData user = opt.get();
        user.setStatus(status);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Status updated",
                "status", status.name().toLowerCase()
        ));
    }
}