package co.udea.crowdfunding.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal historic = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String role = "sponsor";

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "font_size", nullable = false, length = 10)
    private String fontSize = "medium";

    @Column(nullable = false, length = 10)
    private String theme = "light";

    @Column(nullable = false, length = 5)
    private String language = "es";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.password = passwordHash;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getHistoric() { return historic; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getFontSize() { return fontSize; }
    public String getTheme() { return theme; }
    public String getLanguage() { return language; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setHistoric(BigDecimal historic) { this.historic = historic; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; }
    public void setTheme(String theme) { this.theme = theme; }
    public void setLanguage(String language) { this.language = language; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void subtractBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente para realizar el aporte.");
        }
        this.balance = this.balance.subtract(amount);
    }
}
