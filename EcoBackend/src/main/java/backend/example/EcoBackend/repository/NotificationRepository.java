package backend.example.EcoBackend.repository;

import backend.example.EcoBackend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("update Notification n set n.read = true where lower(n.user.email) = lower(?1) and n.read = false")
    int markAllRead(String email);
}
