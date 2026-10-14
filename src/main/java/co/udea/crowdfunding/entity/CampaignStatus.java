package co.udea.crowdfunding.entity;

/**
 * Ciclo de vida de una campaña. HU-06 solo crea campañas en ACTIVE;
 * HU-08 (pausar/cerrar) y HU-13 (finalizar por fecha) usarán el resto de estados.
 */
public enum CampaignStatus {
    ACTIVE,
    PAUSED,
    CLOSED,
    FINISHED
}
