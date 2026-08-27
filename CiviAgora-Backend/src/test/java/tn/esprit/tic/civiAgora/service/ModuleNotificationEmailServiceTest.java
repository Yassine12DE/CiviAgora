package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.tic.civiAgora.dao.entity.Organization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ModuleNotificationEmailServiceTest {

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void grantedNotificationRunsOnlyAfterCommitAndCannotPropagateMailFailure() throws Exception {
        EmailService emailService = mock(EmailService.class);
        ModuleNotificationEmailService service = new ModuleNotificationEmailService(emailService);
        Organization organization = new Organization();
        organization.setId(5);
        organization.setName("Municipality of Tunis");
        organization.setSlug("tunisie");
        organization.setEmail("contact@tunis.com");
        doThrow(new IllegalStateException("SMTP timeout"))
                .when(emailService).sendHtmlMessage(anyString(), anyString(), anyString());
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        service.sendModuleGrantedNotificationAfterCommit(organization, "Voting", "VOTE");

        verify(emailService, never()).sendHtmlMessage(anyString(), anyString(), anyString());
        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit()));
        verify(emailService).sendHtmlMessage(anyString(), anyString(), anyString());
    }
}
