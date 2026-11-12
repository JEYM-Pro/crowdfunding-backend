package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionDTO {
    private UUID id;
    private Instant date;
    private String type; // recharge, contribution, etc.
    private String userName;
    private BigDecimal amount;
    private String status;

    public TransactionDTO() {
    }

    public TransactionDTO(UUID id, Instant date, String type, String userName, BigDecimal amount, String status) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.userName = userName;
        this.amount = amount;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}