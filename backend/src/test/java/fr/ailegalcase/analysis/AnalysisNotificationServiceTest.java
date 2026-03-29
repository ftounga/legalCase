package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisNotificationServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private EmailService emailService;
    @InjectMocks private AnalysisNotificationService service;

    private final UUID caseFileId = UUID.randomUUID();

    // U-01 : DONE + CASE_ANALYSIS → sendAnalysisDone appelé
    @Test
    void onEvent_doneAndCaseAnalysis_sendsEmail() {
        when(caseFileRepository.findTitleById(caseFileId)).thenReturn(Optional.of("Dossier Dupont"));
        when(caseFileRepository.findCreatorEmailById(caseFileId)).thenReturn(Optional.of("avocat@example.com"));

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE, JobType.CASE_ANALYSIS));

        verify(emailService).sendAnalysisDone("avocat@example.com", caseFileId, "Dossier Dupont", JobType.CASE_ANALYSIS);
    }

    // U-02 : DONE + ENRICHED_ANALYSIS → sendAnalysisDone appelé
    @Test
    void onEvent_doneAndEnrichedAnalysis_sendsEmail() {
        when(caseFileRepository.findTitleById(caseFileId)).thenReturn(Optional.of("Dossier Martin"));
        when(caseFileRepository.findCreatorEmailById(caseFileId)).thenReturn(Optional.of("avocat@example.com"));

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE, JobType.ENRICHED_ANALYSIS));

        verify(emailService).sendAnalysisDone(eq("avocat@example.com"), eq(caseFileId), eq("Dossier Martin"), eq(JobType.ENRICHED_ANALYSIS));
    }

    // U-03 : FAILED → sendAnalysisDone non appelé
    @Test
    void onEvent_failed_doesNotSendEmail() {
        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.FAILED, JobType.CASE_ANALYSIS));
        verifyNoInteractions(emailService);
    }

    // U-04 : DONE + DOCUMENT_ANALYSIS → sendAnalysisDone non appelé
    @Test
    void onEvent_doneDocumentAnalysis_doesNotSendEmail() {
        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE, JobType.DOCUMENT_ANALYSIS));
        verifyNoInteractions(emailService);
    }

    // U-05 : CaseFile introuvable → aucun envoi, pas d'exception propagée
    @Test
    void onEvent_caseFileNotFound_doesNotSendAndDoesNotThrow() {
        when(caseFileRepository.findTitleById(caseFileId)).thenReturn(Optional.empty());

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE, JobType.CASE_ANALYSIS));

        verifyNoInteractions(emailService);
    }

    // U-06 : email creator introuvable → aucun envoi
    @Test
    void onEvent_creatorEmailNotFound_doesNotSend() {
        when(caseFileRepository.findTitleById(caseFileId)).thenReturn(Optional.of("Dossier X"));
        when(caseFileRepository.findCreatorEmailById(caseFileId)).thenReturn(Optional.empty());

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE, JobType.CASE_ANALYSIS));

        verifyNoInteractions(emailService);
    }

    // U-07 : exception mail avalée — ne bloque pas
    @Test
    void onEvent_emailServiceThrows_doesNotPropagate() {
        when(caseFileRepository.findTitleById(caseFileId)).thenReturn(Optional.of("Dossier X"));
        when(caseFileRepository.findCreatorEmailById(caseFileId)).thenReturn(Optional.of("avocat@example.com"));
        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendAnalysisDone(any(), any(), any(), any());

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE, JobType.CASE_ANALYSIS));
        // pas d'exception propagée
    }
}
