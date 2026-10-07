package backend.example.EcoBackend.dto;

import java.util.List;

public class PickupRequestDto {
    private String userEmail;
    private String address;
    private String date;
    private String time;
    private String notes;
    private List<ItemDto> items;

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<ItemDto> getItems() { return items; }
    public void setItems(List<ItemDto> items) { this.items = items; }

    public static class ItemDto {
        private String category;
        private int quantity;
        private String description;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
