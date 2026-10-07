package backend.example.EcoBackend.repository;

import backend.example.EcoBackend.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDataRepository extends JpaRepository<UserData, Long> {

    Optional<UserData> findByEmail(String email);
    boolean existsByEmail(String email);




}
