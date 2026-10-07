package backend.example.EcoBackend;

public class HashGenerator {



    public static void main(String[] args) {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            System.out.println(encoder.encode("admin123"));
            System.out.println(encoder.encode("collector123"));
        }












}
