package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.ContributionConfirmationResponse;
import co.udea.crowdfunding.exception.ContributionProcessingException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * HU-12: envía por correo el comprobante de un aporte confirmado.
 * La configuración SMTP viene de variables de entorno (ver application.properties).
 */
@Service
public class ContributionEmailService {

    static final String NOT_CONFIGURED_MESSAGE =
            "El servicio de correo no está configurado, no fue posible confirmar el aporte.";
    static final String SEND_FAILED_MESSAGE =
            "No fue posible enviar el correo de confirmación, el aporte no fue realizado.";

    private static final Locale LOCALE_CO = Locale.of("es", "CO");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE_CO).withZone(ZoneId.of("America/Bogota"));

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String smtpHost;
    private final String from;

    public ContributionEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    @Value("${spring.mail.host:}") String smtpHost,
                                    @Value("${app.mail.from:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.smtpHost = smtpHost;
        this.from = from;
    }

    public void sendConfirmation(String to, ContributionConfirmationResponse receipt) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || isBlank(smtpHost) || isBlank(from)) {
            throw new ContributionProcessingException(NOT_CONFIGURED_MESSAGE);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Confirmación de tu aporte a " + receipt.campaignName());
        message.setText(buildBody(receipt));

        try {
            sender.send(message);
        } catch (MailException ex) {
            throw new ContributionProcessingException(SEND_FAILED_MESSAGE, ex);
        }
    }

    String buildBody(ContributionConfirmationResponse receipt) {
        String reward = receipt.rewardName() == null ? "Sin recompensa" : receipt.rewardName();
        return String.join("\n",
                "¡Gracias por apoyar este proyecto! Este es el comprobante de tu aporte:",
                "",
                "ID de la transacción: " + receipt.transactionId(),
                "Campaña: " + receipt.campaignName(),
                "Monto aportado: " + money(receipt.amount()),
                "Recompensa: " + reward,
                "Saldo restante: " + money(receipt.remainingBalance()),
                "Fecha: " + DATE_FORMAT.format(receipt.transactionDate()),
                "");
    }

    private String money(java.math.BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(LOCALE_CO));
        return "$" + format.format(value) + " COP";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
