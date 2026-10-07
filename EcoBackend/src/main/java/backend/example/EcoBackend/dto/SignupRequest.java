package backend.example.EcoBackend.dto;

public class SignupRequest {
    private String fullname;
    private String email;
    private String password;
    private String confirm;

    private String role;
    private String adminCode;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirm() { return confirm; }
    public void setConfirm(String confirm) { this.confirm = confirm; }












}
