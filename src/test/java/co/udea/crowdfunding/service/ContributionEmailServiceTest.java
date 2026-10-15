package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.ContributionConfirmationResponse;
import co.udea.crowdfunding.exception.ContributionProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionEmailServiceTest {

    private static final UUID TRANSACTION_ID = UUID.randomUUID();

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendsTheReceiptWithAllTheTransactionData() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        ContributionEmailService service = new ContributionEmailService(mailSenderProvider, "smtp.test", "no-reply@test.com");

        service.sendConfirmation("beto@test.com", receipt("Camiseta oficial"));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        SimpleMailMessage message = sent.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@test.com");
        assertThat(message.getTo()).containsExactly("beto@test.com");
        assertThat(message.getSubject()).contains("Huerta comunitaria");
        assertThat(message.getText())
                .contains(TRANSACTION_ID.toString())
                .contains("Huerta comunitaria")
                .contains("Camiseta oficial")
                .contains("60.000,00")
                .contains("40.000,00")
                .contains("15/10/2026 07:00");
    }

    @Test
    void bodyShowsNoRewardWhenNoneWasChosen() {
        ContributionEmailService service = new ContributionEmailService(mailSenderProvider, "smtp.test", "no-reply@test.com");

        assertThat(service.buildBody(receipt(null))).contains("Recompensa: Sin recompensa");
    }

    @Test
    void withoutSmtpHost_theConfirmationFails() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        ContributionEmailService service = new ContributionEmailService(mailSenderProvider, "", "no-reply@test.com");

        assertThatThrownBy(() -> service.sendConfirmation("beto@test.com", receipt(null)))
                .isInstanceOf(ContributionProcessingException.class)
                .hasMessage(ContributionEmailService.NOT_CONFIGURED_MESSAGE);
        verifyNoInteractions(mailSender);
    }

    @Test
    void withoutSenderAddress_theConfirmationFails() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        ContributionEmailService service = new ContributionEmailService(mailSenderProvider, "smtp.test", " ");

        assertThatThrownBy(() -> service.sendConfirmation("beto@test.com", receipt(null)))
                .isInstanceOf(ContributionProcessingException.class)
                .hasMessage(ContributionEmailService.NOT_CONFIGURED_MESSAGE);
    }

    @Test
    void withoutMailSenderBean_theConfirmationFails() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        ContributionEmailService service = new ContributionEmailService(mailSenderProvider, "smtp.test", "no-reply@test.com");

        assertThatThrownBy(() -> service.sendConfirmation("beto@test.com", receipt(null)))
                .isInstanceOf(ContributionProcessingException.class)
                .hasMessage(ContributionEmailService.NOT_CONFIGURED_MESSAGE);
    }

    @Test
    void whenTheSmtpProviderRejectsTheMessage_theConfirmationFails() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("rechazado")).when(mailSender).send(any(SimpleMailMessage.class));
        ContributionEmailService service = new ContributionEmailService(mailSenderProvider, "smtp.test", "no-reply@test.com");

        assertThatThrownBy(() -> service.sendConfirmation("beto@test.com", receipt(null)))
                .isInstanceOf(ContributionProcessingException.class)
                .hasMessage(ContributionEmailService.SEND_FAILED_MESSAGE)
                .hasCauseInstanceOf(MailSendException.class);
    }

    private ContributionConfirmationResponse receipt(String rewardName) {
        return new ContributionConfirmationResponse(TRANSACTION_ID, UUID.randomUUID(), "Huerta comunitaria",
                new BigDecimal("60000"), rewardName == null ? null : UUID.randomUUID(), rewardName,
                new BigDecimal("40000"), Instant.parse("2026-10-15T12:00:00Z"), "Aporte realizado exitosamente.");
    }
}
