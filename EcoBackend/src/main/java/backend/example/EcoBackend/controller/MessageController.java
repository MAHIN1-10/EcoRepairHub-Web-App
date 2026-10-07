package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.entity.Message;
import backend.example.EcoBackend.entity.RepairRequest;
import backend.example.EcoBackend.entity.RequestStatus;
import backend.example.EcoBackend.entity.Role;
import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.repository.MessageRepository;
import backend.example.EcoBackend.repository.RepairRequestRepository;
import backend.example.EcoBackend.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Chat between a customer and the collector assigned to one of their requests.
 */
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private UserDataRepository userRepository;

    // A collector may open a chat with the customer of any request that is still open for pickup
    private Optional<UserData> pendingCustomer(String collectorEmail, String customerEmail) {
        Optional<UserData> collector = userRepository.findByEmail(collectorEmail);
        if (collector.isEmpty() || collector.get().getRole() != Role.COLLECTOR) return Optional.empty();
        return requestRepository.findAll().stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING && r.getUser().getEmail().equalsIgnoreCase(customerEmail))
                .map(RepairRequest::getUser)
                .findFirst();
    }

    private boolean canMessage(String from, String to) {
        if (partnersOf(from).containsKey(to.toLowerCase())) return true;
        if (pendingCustomer(from, to).isPresent()) return true;
        // replying to someone who already wrote to you
        return messageRepository.findConversation(from, to).stream()
                .anyMatch(m -> m.getSender().getEmail().equalsIgnoreCase(to));
    }

    // People this user is allowed to chat with (customers <-> their assigned collectors)
    private Map<String, UserData> partnersOf(String email) {
        Map<String, UserData> partners = new LinkedHashMap<>();
        for (RepairRequest r : requestRepository.findAll()) {
            if (r.getCollector() == null) continue;
            String customer = r.getUser().getEmail();
            String collector = r.getCollector().getEmail();
            if (customer.equalsIgnoreCase(email)) {
                partners.put(collector.toLowerCase(), r.getCollector());
            } else if (collector.equalsIgnoreCase(email)) {
                partners.put(customer.toLowerCase(), r.getUser());
            }
        }
        return partners;
    }

    private static Map<String, Object> messageMap(Message m, String me) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", m.getId());
        out.put("mine", m.getSender().getEmail().equalsIgnoreCase(me));
        out.put("content", m.getContent());
        out.put("createdAt", m.getCreatedAt().toString());
        return out;
    }

    @GetMapping("/conversations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> conversations(@RequestParam String email,
                                           @RequestParam(value = "with", required = false) String with) {
        Map<String, UserData> partners = partnersOf(email);

        // Deep link from an assignment's Chat button: include that person even before any message exists
        if (with != null && !with.isBlank() && !partners.containsKey(with.toLowerCase())) {
            pendingCustomer(email, with).ifPresent(u -> partners.put(u.getEmail().toLowerCase(), u));
        }

        // Also keep anyone who already exchanged messages with this user
        List<Message> all = messageRepository.findAllInvolving(email);
        for (Message m : all) {
            UserData other = m.getSender().getEmail().equalsIgnoreCase(email) ? m.getRecipient() : m.getSender();
            partners.putIfAbsent(other.getEmail().toLowerCase(), other);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (UserData p : partners.values()) {
            Message last = null;
            long unread = 0;
            for (Message m : all) { // newest first
                boolean fromPartner = m.getSender().getEmail().equalsIgnoreCase(p.getEmail());
                boolean toPartner = m.getRecipient().getEmail().equalsIgnoreCase(p.getEmail());
                if (!fromPartner && !toPartner) continue;
                if (last == null) last = m;
                if (fromPartner && !m.isRead()) unread++;
            }
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("email", p.getEmail());
            c.put("name", p.getFullName());
            c.put("role", p.getRole().name());
            c.put("lastMessage", last == null ? "" : last.getContent());
            c.put("lastTime", last == null ? null : last.getCreatedAt().toString());
            c.put("unread", unread);
            out.add(c);
        }

        out.sort((a, b) -> {
            String ta = (String) a.get("lastTime"), tb = (String) b.get("lastTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return ResponseEntity.ok(out);
    }

    @GetMapping("/thread")
    @Transactional
    public ResponseEntity<?> thread(@RequestParam String email, @RequestParam("with") String with) {
        List<Message> msgs = messageRepository.findConversation(email, with);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Message m : msgs) {
            if (!m.isRead() && m.getRecipient().getEmail().equalsIgnoreCase(email)) {
                m.setRead(true); // opening the thread marks incoming messages as read
            }
            out.add(messageMap(m, email));
        }
        return ResponseEntity.ok(out);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> send(@RequestBody Map<String, String> body) {
        String from = body.get("from"), to = body.get("to"), content = body.get("content");
        if (from == null || to == null || content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Message cannot be empty"));
        }
        content = content.trim();
        if (content.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Message is too long (max 1000 characters)"));
        }

        Optional<UserData> sender = userRepository.findByEmail(from);
        Optional<UserData> recipient = userRepository.findByEmail(to);
        if (sender.isEmpty() || recipient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        if (!canMessage(from, to)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only message the collector or customer on your request"));
        }

        Message saved = messageRepository.save(new Message(sender.get(), recipient.get(), content));
        return ResponseEntity.status(HttpStatus.CREATED).body(messageMap(saved, from));
    }
}
