package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.dto.PickupRequestDto;
import backend.example.EcoBackend.entity.RepairRequest;
import backend.example.EcoBackend.entity.RequestItem;
import backend.example.EcoBackend.entity.RequestStatus;
import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.repository.RepairRequestRepository;
import backend.example.EcoBackend.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RepairRequestController {

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private UserDataRepository userRepository;

    @PostMapping("/requests")
    @Transactional
    public ResponseEntity<?> create(@RequestBody PickupRequestDto dto) {

        if (blank(dto.getUserEmail())) return bad("Please log in first");

        Optional<UserData> user = userRepository.findByEmail(dto.getUserEmail());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found. Please log in again."));
        }

        if (dto.getItems() == null || dto.getItems().isEmpty()) return bad("Add at least one item");
        if (blank(dto.getAddress()) || blank(dto.getTime())) return bad("Address and time slot are required");

        LocalDate date;
        try {
            date = LocalDate.parse(dto.getDate());
        } catch (Exception e) {
            return bad("Invalid pickup date");
        }
        if (date.isBefore(LocalDate.now())) return bad("Pickup date cannot be in the past");

        RepairRequest request = new RepairRequest();
        request.setUser(user.get());
        request.setAddress(dto.getAddress().trim());
        request.setPickupDate(date);
        request.setTimeSlot(dto.getTime());
        request.setNotes(dto.getNotes());

        for (PickupRequestDto.ItemDto i : dto.getItems()) {
            if (blank(i.getCategory()) || i.getQuantity() < 1) {
                return bad("Each item needs a category and a quantity of at least 1");
            }
            RequestItem item = new RequestItem();
            item.setCategory(i.getCategory());
            item.setQuantity(i.getQuantity());
            item.setDescription(i.getDescription());
            request.addItem(item);
        }

        RepairRequest saved = requestRepository.save(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Request submitted",
                "id", saved.getId(),
                "requestCode", code(saved.getId())
        ));
    }

    @GetMapping("/requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(@RequestParam String email) {
        List<Map<String, Object>> out = requestRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("requestCode", code(r.getId()));
                    m.put("address", r.getAddress());
                    m.put("date", r.getPickupDate().toString());
                    m.put("time", r.getTimeSlot());
                    m.put("notes", r.getNotes() == null ? "" : r.getNotes());
                    m.put("status", r.getStatus().name());
                    m.put("createdAt", r.getCreatedAt().toString());
                    m.put("items", r.getItems().stream().map(i -> Map.of(
                            "category", i.getCategory(),
                            "quantity", i.getQuantity(),
                            "description", i.getDescription() == null ? "" : i.getDescription()
                    )).toList());
                    return m;
                }).toList();
        return ResponseEntity.ok(out);
    }

    private String code(Long id) {
        return String.format("REQ-%03d", id);
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }


    @PutMapping("/requests/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestParam String email) {
        Optional<RepairRequest> opt = requestRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Request not found"));
        }

        RepairRequest request = opt.get();

        if (!request.getUser().getEmail().equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not your request"));
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            return bad("Only pending requests can be cancelled");
        }

        request.setStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);

        return ResponseEntity.ok(Map.of("message", "Request cancelled"));
    }














}
