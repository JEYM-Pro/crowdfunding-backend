package co.udea.crowdfunding.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MilestoneEmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(MilestoneEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String smtpHost;
    private final String from;

    public MilestoneEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.mail.from:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.smtpHost = smtpHost;
        this.from = from;
    }

    public void sendMilestoneEmail(
            String email, String campaignName, int percentage) {

        JavaMailSender sender = mailSenderProvider.getIfAvailable();

        if (sender == null || smtpHost.isBlank() || from.isBlank()) {
            logger.warn("Correo de metas no configurado.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Meta alcanzada: " + campaignName);
        message.setText(
                "¡Buenas noticias!\n\n"
                + "La campaña \"" + campaignName + "\" "
                + "ha alcanzado el " + percentage
                + "% de su objetivo de recaudo.\n\n"
                + "Gracias por apoyar este proyecto.");

        try {
            sender.send(message);
        } catch (MailException ex) {
            logger.error("No se pudo enviar la notificación a {}", email, ex);
        }
    }
}
