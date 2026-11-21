package co.udea.crowdfunding.service;

import java.util.UUID;

/**
 * HU-S07: qué verificación se está contando. La contraseña de la cuenta y el PIN de cada campaña
 * llevan contadores independientes, así un bloqueo del PIN de una campaña no afecta a las demás.
 */
public record VerificationScope(String key, UUID campaignId) {

    private static final String ACCOUNT_KEY = "ACCOUNT";
    private static final String CAMPAIGN_PREFIX = "CAMPAIGN:";

    public static VerificationScope account() {
        return new VerificationScope(ACCOUNT_KEY, null);
    }

    public static VerificationScope campaign(UUID campaignId) {
        return new VerificationScope(CAMPAIGN_PREFIX + campaignId, campaignId);
    }
}
