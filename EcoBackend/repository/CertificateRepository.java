
package backend.example.EcoBackend.repository;

import backend.example.EcoBackend.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByUserEmailOrderByIssuedDateDesc(String email);
}