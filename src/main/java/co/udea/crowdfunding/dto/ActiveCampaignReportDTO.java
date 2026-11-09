
package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ActiveCampaignReportDTO {
    private UUID id;
    private String title;
    private BigDecimal goal;
    private BigDecimal currentRaised;
    private Double percentage;
    private Long daysRemaining;
    private Boolean alert;

    public ActiveCampaignReportDTO() {
    }

    public ActiveCampaignReportDTO(UUID id, String title, BigDecimal goal, BigDecimal currentRaised,
                                    Double percentage, Long daysRemaining, Boolean alert) {
        this.id = id;
        this.title = title;
        this.goal = goal;
        this.currentRaised = currentRaised;
        this.percentage = percentage;
        this.daysRemaining = daysRemaining;
        this.alert = alert;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getGoal() { return goal; }
    public void setGoal(BigDecimal goal) { this.goal = goal; }

    public BigDecimal getCurrentRaised() { return currentRaised; }
    public void setCurrentRaised(BigDecimal currentRaised) { this.currentRaised = currentRaised; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public Long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Long daysRemaining) { this.daysRemaining = daysRemaining; }

    public Boolean getAlert() { return alert; }
    public void setAlert(Boolean alert) { this.alert = alert; }
}
