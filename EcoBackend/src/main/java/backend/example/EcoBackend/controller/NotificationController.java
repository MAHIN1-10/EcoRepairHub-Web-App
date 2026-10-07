package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.entity.Notification;
import backend.example.EcoBackend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(@RequestParam String email) {
        List<Notification> all = notificationRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email);
        long unread = all.stream().filter(n -> !n.isRead()).count();

        List<Map<String, Object>> items = all.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("read", n.isRead());
            m.put("createdAt", n.getCreatedAt().toString());
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of("unread", unread, "items", items));
    }

    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<?> markRead(@PathVariable Long id, @RequestParam String email) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Notification not found"));
        }
        Notification n = opt.get();
        if (!n.getUser().getEmail().equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not your notification"));
        }
        n.setRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<?> markAllRead(@RequestParam String email) {
        int updated = notificationRepository.markAllRead(email);
        return ResponseEntity.ok(Map.of("message", "All marked as read", "updated", updated));
    }
}
