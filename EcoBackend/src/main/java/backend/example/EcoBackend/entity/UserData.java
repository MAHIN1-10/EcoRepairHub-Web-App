package backend.example.EcoBackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserData {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "name", nullable = false)
        private String fullName;

        @Column(name = "email", nullable = false, unique = true)
        private String email;

        @Column(name = "password", nullable = false)
        private String password;

        @Enumerated(EnumType.STRING)
        @Column(name = "role", nullable = false)
        private Role role;

        @Column(name = "phone")
        private String phone;

        @Enumerated(EnumType.STRING)
        @Column(name = "status")
        private UserStatus status = UserStatus.ACTIVE;

        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt = LocalDateTime.now();

        public UserData() {}

        public UserData(String fullName, String email, String password, Role role) {
                this.fullName = fullName;
                this.email = email;
                this.password = password;
                this.role = role;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}