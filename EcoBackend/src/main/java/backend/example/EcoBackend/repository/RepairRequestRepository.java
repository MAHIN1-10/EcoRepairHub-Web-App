package backend.example.EcoBackend.repository;

import backend.example.EcoBackend.entity.RepairRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findByUserEmailOrderByCreatedAtDesc(String email);
    long countByUserEmail(String email);
}