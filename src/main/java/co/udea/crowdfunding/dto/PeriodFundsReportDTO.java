
package co.udea.crowdfunding.dto;

import java.math.BigDecimal;

public class PeriodFundsReportDTO {
    private BigDecimal totalFunds;
    private Integer contributionCount;
    private BigDecimal averageContribution;
    private String message;

    public PeriodFundsReportDTO() {
    }

    public PeriodFundsReportDTO(BigDecimal totalFunds, Integer contributionCount,
                                BigDecimal averageContribution, String message) {
        this.totalFunds = totalFunds;
        this.contributionCount = contributionCount;
        this.averageContribution = averageContribution;
        this.message = message;
    }

    public BigDecimal getTotalFunds() { return totalFunds; }
    public void setTotalFunds(BigDecimal totalFunds) { this.totalFunds = totalFunds; }

    public Integer getContributionCount() { return contributionCount; }
    public void setContributionCount(Integer contributionCount) { this.contributionCount = contributionCount; }

    public BigDecimal getAverageContribution() { return averageContribution; }
    public void setAverageContribution(BigDecimal averageContribution) { this.averageContribution = averageContribution; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
