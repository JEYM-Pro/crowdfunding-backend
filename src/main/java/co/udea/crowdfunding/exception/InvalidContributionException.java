package co.udea.crowdfunding.exception;

/**
 * Aporte que incumple una regla de negocio: monto inválido, saldo insuficiente, recompensa agotada,
 * monto menor al mínimo de la recompensa o recompensa ajena a la campaña (HTTP 400).
 */
public class InvalidContributionException extends RuntimeException {

    public InvalidContributionException(String message) {
        super(message);
    }
}
