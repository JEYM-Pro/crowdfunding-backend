
package co.udea.crowdfunding.dto;

import co.udea.crowdfunding.entity.CampaignStatus;

public class UpdateCampaignStatusRequest {
    private CampaignStatus status;

    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
}
