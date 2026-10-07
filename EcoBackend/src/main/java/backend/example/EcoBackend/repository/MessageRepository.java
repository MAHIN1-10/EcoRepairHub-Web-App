package backend.example.EcoBackend.repository;

import backend.example.EcoBackend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m where " +
            "(lower(m.sender.email) = lower(?1) and lower(m.recipient.email) = lower(?2)) or " +
            "(lower(m.sender.email) = lower(?2) and lower(m.recipient.email) = lower(?1)) " +
            "order by m.createdAt asc, m.id asc")
    List<Message> findConversation(String emailA, String emailB);

    @Query("select m from Message m where lower(m.sender.email) = lower(?1) or lower(m.recipient.email) = lower(?1) " +
            "order by m.createdAt desc, m.id desc")
    List<Message> findAllInvolving(String email);
}
