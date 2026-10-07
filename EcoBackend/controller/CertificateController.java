package backend.example.EcoBackend.controller;

import backend.example.EcoBackend.repository.CertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CertificateController {

    @Autowired
    private CertificateRepository certificateRepository;

    @GetMapping("/certificates")
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(@RequestParam String email) {
        List<Map<String, Object>> out = certificateRepository.findByUserEmailOrderByIssuedDateDesc(email)
                .stream().map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("certCode", String.format("CERT-%d-%03d", c.getIssuedDate().getYear(), c.getId()));
                    m.put("weightKg", c.getWeightKg());
                    m.put("co2OffsetKg", c.getCo2OffsetKg());
                    m.put("centerName", c.getCenterName());
                    m.put("issuedDate", c.getIssuedDate().toString());
                    m.put("itemsSummary", c.getRequest().getItems().stream()
                            .map(i -> i.getQuantity() + "x " + i.getCategory())
                            .reduce((a, b) -> a + ", " + b).orElse(""));
                    return m;
                }).toList();
        return ResponseEntity.ok(out);
    }
}
