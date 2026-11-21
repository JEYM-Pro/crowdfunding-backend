
package co.udea.crowdfunding.dto;

import co.udea.crowdfunding.entity.CampaignStatus;

public class UpdateCampaignStatusRequest {
    private CampaignStatus status;

    /** HU-S05: solo para cerrar; PIN de la campaña. */
    private String pin;

    /** HU-S05: solo para cerrar; contraseña de la cuenta del creador. */
    private String password;

    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    /** El PIN y la contraseña no deben aparecer en registros ni en trazas. */
    @Override
    public String toString() {
        return "UpdateCampaignStatusRequest[status=" + status + ", pin=****, password=****]";
    }
}
