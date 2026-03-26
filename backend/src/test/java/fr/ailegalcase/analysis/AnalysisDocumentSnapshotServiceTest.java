package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisDocumentSnapshotServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private AnalysisDocumentRepository analysisDocumentRepository;

    @InjectMocks private AnalysisDocumentSnapshotService service;

    // S-01 : 3 documents → 3 snapshots avec les bons champs
    @Test
    void snapshot_threeDocuments_savesThreeSnapshotsWithCorrectFields() {
        UUID analysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        Document d1 = document("Contrat de travail.pdf");
        Document d2 = document("Avenant n°1.pdf");
        Document d3 = document("Lettre de licenciement.pdf");

        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile))
                .thenReturn(List.of(d1, d2, d3));
        when(analysisDocumentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.snapshot(analysisId, caseFile);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnalysisDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisDocumentRepository).saveAll(captor.capture());

        List<AnalysisDocument> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved).allMatch(s -> s.getAnalysisId().equals(analysisId));
        assertThat(saved.stream().map(AnalysisDocument::getDocumentName).toList())
                .containsExactly("Contrat de travail.pdf", "Avenant n°1.pdf", "Lettre de licenciement.pdf");
        assertThat(saved.stream().map(AnalysisDocument::getDocumentId).toList())
                .containsExactly(d1.getId(), d2.getId(), d3.getId());
    }

    // S-02 : dossier sans document → saveAll avec liste vide, pas d'erreur
    @Test
    void snapshot_noDocuments_savesEmptyList() {
        UUID analysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());
        when(analysisDocumentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.snapshot(analysisId, caseFile);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnalysisDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisDocumentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    // S-03 : le document_name est le originalFilename (snapshot du nom au moment de l'analyse)
    @Test
    void snapshot_documentNameIsOriginalFilename() {
        UUID analysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        Document doc = document("Pièce justificative n°7.pdf");

        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of(doc));
        when(analysisDocumentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.snapshot(analysisId, caseFile);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnalysisDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisDocumentRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getDocumentName()).isEqualTo("Pièce justificative n°7.pdf");
    }

    // S-04 : chaque snapshot porte le bon analysisId
    @Test
    void snapshot_allSnapshotsHaveCorrectAnalysisId() {
        UUID analysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile))
                .thenReturn(List.of(document("A.pdf"), document("B.pdf")));
        when(analysisDocumentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.snapshot(analysisId, caseFile);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnalysisDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisDocumentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allMatch(s -> analysisId.equals(s.getAnalysisId()));
    }

    // Helper
    private Document document(String filename) {
        Document d = new Document();
        d.setId(UUID.randomUUID());
        d.setOriginalFilename(filename);
        return d;
    }
}
