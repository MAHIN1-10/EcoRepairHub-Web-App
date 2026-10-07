
package backend.example.EcoBackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "request_items")
public class RequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private RepairRequest request;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int quantity;

    private String description;

    public Long getId() { return id; }
    public RepairRequest getRequest() { return request; }
    public void setRequest(RepairRequest request) { this.request = request; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}