package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.entity.RepairRequest;
import backend.example.EcoBackend.entity.RequestStatus;
import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.repository.RepairRequestRepository;
import backend.example.EcoBackend.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collector")
@CrossOrigin(origins = "*")
public class CollectorRequestController {

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private UserDataRepository userRepository;

    // Pending (unassigned) requests + whatever this collector already has
    @GetMapping("/requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(@RequestParam String email) {
        List<RepairRequest> all = requestRepository.findAll();

        List<Map<String, Object>> out = all.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING
                        || (r.getCollector() != null && r.getCollector().getEmail().equalsIgnoreCase(email)))
                .sorted(Comparator.comparing(RepairRequest::getCreatedAt).reversed())
                .map(this::toMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(out);
    }

    @PutMapping("/requests/{id}/accept")
    @Transactional
    public ResponseEntity<?> accept(@PathVariable Long id, @RequestParam String email) {
        Optional<RepairRequest> opt = requestRepository.findById(id);
        if (opt.isEmpty()) return notFound();

        RepairRequest request = opt.get();
        if (request.getStatus() != RequestStatus.PENDING) {
            return bad("This request is no longer available");
        }

        Optional<UserData> collector = userRepository.findByEmail(email);
        if (collector.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Collector not found"));

        request.setCollector(collector.get());
        request.setStatus(RequestStatus.ASSIGNED);
        requestRepository.save(request);

        return ResponseEntity.ok(toMap(request));
    }

    @PutMapping("/requests/{id}/start")
    @Transactional
    public ResponseEntity<?> start(@PathVariable Long id, @RequestParam String email) {
        Optional<RepairRequest> opt = requestRepository.findById(id);
        if (opt.isEmpty()) return notFound();

        RepairRequest request = opt.get();
        if (request.getCollector() == null || !request.getCollector().getEmail().equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not your assignment"));
        }
        if (request.getStatus() != RequestStatus.ASSIGNED) {
            return bad("Request must be accepted before starting pickup");
        }

        request.setStatus(RequestStatus.PICKED_UP);
        requestRepository.save(request);

        return ResponseEntity.ok(toMap(request));
    }

    @PutMapping("/requests/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestParam String email) {
        Optional<RepairRequest> opt = requestRepository.findById(id);
        if (opt.isEmpty()) return notFound();

        RepairRequest request = opt.get();
        if (request.getCollector() == null || !request.getCollector().getEmail().equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not your assignment"));
        }

        request.setCollector(null);
        request.setStatus(RequestStatus.PENDING);
        requestRepository.save(request);

        return ResponseEntity.ok(toMap(request));
    }

    private Map<String, Object> toMap(RepairRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("requestCode", String.format("ASN-%03d", r.getId()));
        m.put("status", r.getStatus().name());
        m.put("customerName", r.getUser().getFullName());
        m.put("customerPhone", r.getUser().getPhone() == null ? "" : r.getUser().getPhone());
        m.put("address", r.getAddress());
        m.put("date", r.getPickupDate().toString());
        m.put("time", r.getTimeSlot());
        m.put("itemsSummary", r.getItems().stream()
                .map(i -> i.getQuantity() + "x " + i.getCategory())
                .collect(Collectors.joining(", ")));
        return m;
    }

    private ResponseEntity<?> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Request not found"));
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }
}