package backend.example.EcoBackend.controller;



import backend.example.EcoBackend.dto.FeedbackRequest;
import backend.example.EcoBackend.entity.Feedback;
import backend.example.EcoBackend.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @PostMapping("/feedback")
    public ResponseEntity<?> saveFeedback(@RequestBody FeedbackRequest req) {

        if (isBlank(req.getName()) || isBlank(req.getEmail())
                || isBlank(req.getSubject()) || isBlank(req.getMessage())) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required"));
        }

        Feedback saved = feedbackRepository.save(new Feedback(
                req.getName().trim(),
                req.getEmail().trim(),
                req.getSubject().trim(),
                req.getMessage().trim()
        ));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Thank you! Your message was sent.", "id", saved.getId()));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
