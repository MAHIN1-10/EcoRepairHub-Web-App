package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.entity.RepairRequest;
import backend.example.EcoBackend.entity.RequestStatus;
import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.repository.RepairRequestRepository;
import backend.example.EcoBackend.repository.UserDataRepository;
import backend.example.EcoBackend.service.NotificationService;
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

    @Autowired
    private NotificationService notificationService;

    // Pending (unassigned) requests + whatever this collector already has
    @GetMapping("/requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(@RequestParam String email) {
        List<RepairRequest> all = requestRepository.findAll();

        List<Map<String, Object>> out = all.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING
                        || (r.getCollector() != null && r.getCollector().getEmail().equalsIgnoreCase(email)))
                .sorted(Comparator.comparing(RepairRequest::getCreatedAt).reversed())
                .map(r -> toMap(r, email))
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
        notificationService.notify(request.getUser(), "pickup", "Pickup Scheduled",
                "Your request " + userCode(request) + " was accepted by " + collector.get().getFullName()
                        + " for " + request.getPickupDate() + " (" + request.getTimeSlot()
                        + "). Please have your items ready.");

        return ResponseEntity.ok(toMap(request, email));
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
        notificationService.notify(request.getUser(), "pickup", "Items Picked Up",
                "Your items for request " + userCode(request) + " have been picked up.");

        return ResponseEntity.ok(toMap(request, email));
    }

    // Collector asks to verify the pickup: generate a 4-digit OTP and send it to the customer
    @PutMapping("/requests/{id}/request-otp")
    @Transactional
    public ResponseEntity<?> requestOtp(@PathVariable Long id, @RequestParam String email) {
        Optional<RepairRequest> opt = requestRepository.findById(id);
        if (opt.isEmpty()) return notFound();

        RepairRequest request = opt.get();
        if (!isAssignedTo(request, email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not your assignment"));
        }
        if (request.getStatus() != RequestStatus.PICKED_UP) {
            return bad("Start the pickup before verifying");
        }

        String otp = String.format("%04d", new java.security.SecureRandom().nextInt(10000));
        request.setPickupOtp(otp);
        requestRepository.save(request);
        notificationService.notify(request.getUser(), "pickup", "Pickup OTP: " + otp,
                "Collector " + request.getCollector().getFullName() + " is verifying your pickup " + userCode(request)
                        + ". Your 4-digit OTP is " + otp + ". Only share it with the collector when they are with you.");

        return ResponseEntity.ok(Map.of("message", "OTP sent to the customer"));
    }

    // Collector has verified the customer's OTP and now holds the items
    @PutMapping("/requests/{id}/collect")
    @Transactional
    public ResponseEntity<?> collect(@PathVariable Long id, @RequestParam String email,
                                     @RequestParam(required = false) String otp) {
        Optional<RepairRequest> opt = requestRepository.findById(id);
        if (opt.isEmpty()) return notFound();

        RepairRequest request = opt.get();
        if (!isAssignedTo(request, email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not your assignment"));
        }
        if (request.getStatus() != RequestStatus.PICKED_UP) {
            return bad("Pickup must be in transit before items can be collected");
        }
        if (request.getPickupOtp() == null) {
            return bad("No OTP has been sent to the customer yet");
        }
        if (otp == null || !request.getPickupOtp().equals(otp.trim())) {
            return bad("Incorrect OTP");
        }

        request.setPickupOtp(null);
        request.setStatus(RequestStatus.COLLECTED);
        requestRepository.save(request);
        notificationService.notify(request.getUser(), "pickup", "Items Collected",
                "Your items for request " + userCode(request) + " have been collected and are on their way to the recycling center.");

        return ResponseEntity.ok(toMap(request, email));
    }

    // Items delivered to the recycling center
    @PutMapping("/requests/{id}/complete")
    @Transactional
    public ResponseEntity<?> complete(@PathVariable Long id, @RequestParam String email) {
        Optional<RepairRequest> opt = requestRepository.findById(id);
        if (opt.isEmpty()) return notFound();

        RepairRequest request = opt.get();
        if (!isAssignedTo(request, email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not your assignment"));
        }
        if (request.getStatus() != RequestStatus.COLLECTED) {
            return bad("Items must be collected before they can be delivered");
        }

        request.setStatus(RequestStatus.COMPLETED);
        requestRepository.save(request);
        notificationService.notify(request.getUser(), "pickup", "Pickup Completed",
                "Your request " + userCode(request) + " is complete. Thank you for recycling!");

        return ResponseEntity.ok(toMap(request, email));
    }

    private boolean isAssignedTo(RepairRequest r, String email) {
        return r.getCollector() != null && r.getCollector().getEmail().equalsIgnoreCase(email);
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
        notificationService.notify(request.getUser(), "pickup", "Collector Unassigned",
                "The collector for request " + userCode(request) + " is no longer available. Your request is pending again.");

        return ResponseEntity.ok(toMap(request, email));
    }

    private String userCode(RepairRequest r) {
        return String.format("REQ-%03d", r.getId());
    }

    private Map<String, Object> toMap(RepairRequest r, String email) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("requestCode", String.format("ASN-%03d", r.getId()));
        m.put("status", r.getStatus().name());
        m.put("mine", isAssignedTo(r, email));
        m.put("customerName", r.getUser().getFullName());
        m.put("customerEmail", r.getUser().getEmail());
        m.put("customerPhone", r.getUser().getPhone() == null ? "" : r.getUser().getPhone());
        m.put("address", r.getAddress());
        m.put("date", r.getPickupDate().toString());
        m.put("time", r.getTimeSlot());
        m.put("itemCount", r.getItems().stream().mapToInt(i -> i.getQuantity()).sum());
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