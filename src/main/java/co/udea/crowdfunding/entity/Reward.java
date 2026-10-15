package co.udea.crowdfunding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Nivel de recompensa de una campaña (HU-06). Se define al publicar y no se puede modificar después.
 * HU-11: al aportar eligiendo esta recompensa se descuenta una unidad de stock (puede llegar a 0 = agotada).
 */
@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minAmount;

    @Column(nullable = false)
    private int stock;

    protected Reward() {
    }

    public Reward(String name, String description, BigDecimal minAmount, int stock) {
        this.name = name;
        this.description = description;
        this.minAmount = minAmount;
        this.stock = stock;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getMinAmount() { return minAmount; }
    public int getStock() { return stock; }

    /** Descuenta una unidad de stock (HU-11). Rechaza el intento si la recompensa ya está agotada. */
    public void decreaseStock() {
        if (stock <= 0) {
            throw new IllegalStateException("La recompensa seleccionada está agotada.");
        }
        stock--;
    }
}
