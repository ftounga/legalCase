package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-261 / SF-261-05 — tests du service de persistance des moyens adverses :
 * extraction + persistance au 1ᵉʳ appel, lecture du set persisté (0 extraction) au 2ᵉ,
 * aucun document adverse → vide sans extraction, fail-open.
 */
class AdverseMoyenPersistenceServiceTest {

    private final AdverseMoyenRepository adverseMoyenRepository = mock(AdverseMoyenRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentExtractionRepository documentExtractionRepository =
            mock(DocumentExtractionRepository.class);
    private final AdverseMoyensExtractor adverseMoyensExtractor = mock(AdverseMoyensExtractor.class);

    private final AdverseMoyenPersistenceService service = new AdverseMoyenPersistenceService(
            adverseMoyenRepository, documentRepository, documentExtractionRepository,
            adverseMoyensExtractor, new ObjectMapper());

    @Test
    void loadOrExtract_firstTime_extractsThenPersistsOrderedSet() {
        UUID caseFileId = UUID.randomUUID();
        when(adverseMoyenRepository.existsByCaseFileId(caseFileId)).thenReturn(false);

        Document adverse = adverseDocument(true);
        Document normal = adverseDocument(false);
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId))
                .thenReturn(List.of(adverse, normal));
        when(documentExtractionRepository.findByDocumentId(adverse.getId()))
                .thenReturn(Optional.of(extraction("Conclusions adverses : faute grave invoquée.")));
        when(adverseMoyensExtractor.extract(
                eq(List.of("Conclusions adverses : faute grave invoquée.")),
                eq("DROIT_DU_TRAVAIL"), eq("FRANCE"), eq(caseFileId)))
                .thenReturn(List.of(
                        new AdverseMoyen("Le licenciement repose sur une faute grave.",
                                List.of("art. L. 1234-1 C. trav."), List.of("Lettre de licenciement")),
                        new AdverseMoyen("Aucune indemnité n'est due.",
                                List.of("art. L. 1234-9 C. trav."), List.of())));
        // save() renvoie l'entité avec un id (simule le générateur JPA).
        when(adverseMoyenRepository.save(any(AdverseMoyenEntity.class))).thenAnswer(inv -> {
            AdverseMoyenEntity e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
            return e;
        });

        List<AdverseMoyenWithId> result = service.loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE");

        // extraction appelée avec le texte du seul document adverse
        verify(adverseMoyensExtractor).extract(
                eq(List.of("Conclusions adverses : faute grave invoquée.")),
                eq("DROIT_DU_TRAVAIL"), eq("FRANCE"), eq(caseFileId));
        // replace-set : delete préalable + 2 insertions ordonnées
        verify(adverseMoyenRepository).deleteByCaseFileId(caseFileId);
        org.mockito.ArgumentCaptor<AdverseMoyenEntity> captor =
                org.mockito.ArgumentCaptor.forClass(AdverseMoyenEntity.class);
        verify(adverseMoyenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<AdverseMoyenEntity> saved = captor.getAllValues();
        assertThat(saved).extracting(AdverseMoyenEntity::getOrdre).containsExactly(0, 1);
        assertThat(saved.get(0).getThese()).isEqualTo("Le licenciement repose sur une faute grave.");
        assertThat(saved.get(0).getFondements()).contains("art. L. 1234-1 C. trav.");
        assertThat(saved.get(0).getCaseFileId()).isEqualTo(caseFileId);
        // résultat retourné avec id + contenu inchangé
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isNotNull();
        assertThat(result.get(0).moyen().these()).isEqualTo("Le licenciement repose sur une faute grave.");
        assertThat(result.get(0).moyen().fondements()).containsExactly("art. L. 1234-1 C. trav.");
    }

    @Test
    void loadOrExtract_secondTime_readsPersistedWithoutExtraction() {
        UUID caseFileId = UUID.randomUUID();
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenRepository.existsByCaseFileId(caseFileId)).thenReturn(true);
        when(adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFileId))
                .thenReturn(List.of(persistedEntity(moyenId, caseFileId,
                        "Le licenciement repose sur une faute grave.",
                        "[\"art. L. 1234-1 C. trav.\"]", "[\"Lettre de licenciement\"]", 0)));

        List<AdverseMoyenWithId> result = service.loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE");

        // aucune ré-extraction, aucune écriture
        verify(adverseMoyensExtractor, never()).extract(any(), any(), any(), any());
        verify(adverseMoyenRepository, never()).save(any());
        verify(adverseMoyenRepository, never()).deleteByCaseFileId(any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(moyenId);
        assertThat(result.get(0).moyen().these()).isEqualTo("Le licenciement repose sur une faute grave.");
        assertThat(result.get(0).moyen().fondements()).containsExactly("art. L. 1234-1 C. trav.");
        assertThat(result.get(0).moyen().piecesInvoquees()).containsExactly("Lettre de licenciement");
    }

    @Test
    void loadOrExtract_noAdverseDocument_returnsEmptyWithoutExtraction() {
        UUID caseFileId = UUID.randomUUID();
        when(adverseMoyenRepository.existsByCaseFileId(caseFileId)).thenReturn(false);
        // seul un document NON adverse → aucun texte
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId))
                .thenReturn(List.of(adverseDocument(false)));

        List<AdverseMoyenWithId> result = service.loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE");

        assertThat(result).isEmpty();
        verify(adverseMoyensExtractor, never()).extract(any(), any(), any(), any());
        verify(adverseMoyenRepository, never()).save(any());
    }

    @Test
    void loadOrExtract_extractorThrows_failsOpenWithEmptyList() {
        UUID caseFileId = UUID.randomUUID();
        when(adverseMoyenRepository.existsByCaseFileId(caseFileId)).thenReturn(false);
        Document adverse = adverseDocument(true);
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId))
                .thenReturn(List.of(adverse));
        when(documentExtractionRepository.findByDocumentId(adverse.getId()))
                .thenReturn(Optional.of(extraction("Conclusions adverses.")));
        when(adverseMoyensExtractor.extract(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Anthropic 529 overloaded"));

        // fail-open : pas d'exception propagée, liste vide
        List<AdverseMoyenWithId> result = service.loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE");

        assertThat(result).isEmpty();
        verify(adverseMoyenRepository, never()).save(any());
    }

    @Test
    void findPersisted_readsOnly_noExtraction() {
        UUID caseFileId = UUID.randomUUID();
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFileId))
                .thenReturn(List.of(persistedEntity(moyenId, caseFileId,
                        "Thèse adverse.", "[]", "[]", 0)));

        List<AdverseMoyenWithId> result = service.findPersisted(caseFileId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(moyenId);
        assertThat(result.get(0).moyen().fondements()).isEmpty();
        verify(adverseMoyensExtractor, never()).extract(any(), any(), any(), any());
        verify(adverseMoyenRepository, never()).existsByCaseFileId(any());
    }

    // ── SF-261-06 — ré-extraction si un acte adverse plus récent que le set figé ──

    @Test
    void loadOrExtract_newerAdverseDocument_reExtractsReplaceSet() {
        UUID caseFileId = UUID.randomUUID();
        java.time.Instant extractedAt = java.time.Instant.parse("2026-06-01T10:00:00Z");
        when(adverseMoyenRepository.existsByCaseFileId(caseFileId)).thenReturn(true);
        when(adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFileId))
                .thenReturn(List.of(persistedEntityAt(UUID.randomUUID(), caseFileId, extractedAt)));
        // Un nouvel acte adverse a été ajouté APRÈS l'extraction figée.
        Document newerAdverse = adverseDocumentAt(true, java.time.Instant.parse("2026-06-05T10:00:00Z"));
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId))
                .thenReturn(List.of(newerAdverse));
        when(documentExtractionRepository.findByDocumentId(newerAdverse.getId()))
                .thenReturn(Optional.of(extraction("Nouvelles conclusions adverses : prescription invoquée.")));
        when(adverseMoyensExtractor.extract(any(), any(), any(), any()))
                .thenReturn(List.of(new AdverseMoyen("La demande est prescrite.",
                        List.of("art. L. 1471-1 C. trav."), List.of())));
        when(adverseMoyenRepository.save(any(AdverseMoyenEntity.class))).thenAnswer(inv -> {
            AdverseMoyenEntity e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
            return e;
        });

        List<AdverseMoyenWithId> result = service.loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE");

        // Ré-extraction (replace-set) → la réplique réfutera le nouveau moyen.
        verify(adverseMoyensExtractor).extract(any(), any(), any(), any());
        verify(adverseMoyenRepository).deleteByCaseFileId(caseFileId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).moyen().these()).isEqualTo("La demande est prescrite.");
    }

    @Test
    void loadOrExtract_persistedSet_noNewerAdverseDoc_keepsSetWithoutExtraction() {
        UUID caseFileId = UUID.randomUUID();
        java.time.Instant extractedAt = java.time.Instant.parse("2026-06-05T10:00:00Z");
        when(adverseMoyenRepository.existsByCaseFileId(caseFileId)).thenReturn(true);
        when(adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFileId))
                .thenReturn(List.of(persistedEntityAt(UUID.randomUUID(), caseFileId, extractedAt)));
        // Acte adverse ANTÉRIEUR à l'extraction → rien de neuf.
        Document olderAdverse = adverseDocumentAt(true, java.time.Instant.parse("2026-06-01T10:00:00Z"));
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId))
                .thenReturn(List.of(olderAdverse));

        service.loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE");

        verify(adverseMoyensExtractor, never()).extract(any(), any(), any(), any());
        verify(adverseMoyenRepository, never()).deleteByCaseFileId(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Document adverseDocument(boolean adverse) {
        Document doc = new Document();
        ReflectionTestUtils.setField(doc, "id", UUID.randomUUID());
        doc.setAdversePleadings(adverse);
        return doc;
    }

    private Document adverseDocumentAt(boolean adverse, java.time.Instant createdAt) {
        Document doc = adverseDocument(adverse);
        ReflectionTestUtils.setField(doc, "createdAt", createdAt);
        return doc;
    }

    private AdverseMoyenEntity persistedEntityAt(UUID id, UUID caseFileId, java.time.Instant createdAt) {
        AdverseMoyenEntity e = persistedEntity(id, caseFileId, "Moyen figé", "[]", "[]", 0);
        ReflectionTestUtils.setField(e, "createdAt", createdAt);
        return e;
    }

    private DocumentExtraction extraction(String text) {
        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setExtractedText(text);
        return extraction;
    }

    private AdverseMoyenEntity persistedEntity(UUID id, UUID caseFileId, String these,
                                               String fondementsJson, String piecesJson, int ordre) {
        AdverseMoyenEntity entity = new AdverseMoyenEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setCaseFileId(caseFileId);
        entity.setThese(these);
        entity.setFondements(fondementsJson);
        entity.setPiecesInvoquees(piecesJson);
        entity.setOrdre(ordre);
        return entity;
    }
}
