package backend.example.EcoBackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private RepairRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserData user;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    @Column(name = "co2_offset_kg", nullable = false)
    private double co2OffsetKg;

    @Column(name = "center_name", nullable = false)
    private String centerName;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate = LocalDate.now();

    public Long getId() { return id; }
    public RepairRequest getRequest() { return request; }
    public void setRequest(RepairRequest request) { this.request = request; }
    public UserData getUser() { return user; }
    public void setUser(UserData user) { this.user = user; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public double getCo2OffsetKg() { return co2OffsetKg; }
    public void setCo2OffsetKg(double co2OffsetKg) { this.co2OffsetKg = co2OffsetKg; }
    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; }
}
