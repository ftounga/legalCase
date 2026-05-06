package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-197 SF-197-01 — UT du service {@link TypeLitigeOverrideService}.
 *
 * <p>Pattern miroir {@link RisqueStatusServiceTest} (F-195). Différence : single-value
 * override directement sur {@code case_analyses} (pas de table dédiée).</p>
 *
 * <ul>
 *   <li>upsert nominal Travail (set + raison)</li>
 *   <li>upsert nominal Immigration</li>
 *   <li>upsert remplace ancienne valeur (idempotence — pas de doublon)</li>
 *   <li>validation type vide → 400</li>
 *   <li>validation type inconnu → 400</li>
 *   <li>validation type Travail sur dossier Immigration → 400</li>
 *   <li>aucune analyse DONE → 404</li>
 *   <li>autre workspace → 404</li>
 *   <li>cloneOverrideFromPrevious clone les 3 colonnes</li>
 *   <li>cloneOverrideFromPrevious sans override → no-op</li>
 *   <li>readOverrideForLatestDone retourne null si pas d'override</li>
 * </ul>
 */
class TypeLitigeOverrideServiceTest {

    private CaseAnalysisRepository caseAnalysisRepository;
    private CaseFileRepository caseFileRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;
    private TypeLitigeOverrideService service;

    private User user;
    private Workspace workspace;
    private CaseFile caseFile;
    private UUID caseFileId;
    private CaseAnalysis latestAnalysis;
    private UUID latestAnalysisId;

    @BeforeEach
    void setUp() {
        caseAnalysisRepository = mock(CaseAnalysisRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);

        service = new TypeLitigeOverrideService(
                caseAnalysisRepository, caseFileRepository,
                workspaceMemberRepository, currentUserResolver);

        user = new User();
        workspace = new Workspace();
        setField(workspace, "id", UUID.randomUUID());
        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileId = UUID.randomUUID();
        setField(caseFile, "id", caseFileId);

        latestAnalysis = new CaseAnalysis();
        latestAnalysisId = UUID.randomUUID();
        setField(latestAnalysis, "id", latestAnalysisId);
        latestAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
        latestAnalysis.setCaseFile(caseFile);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(latestAnalysis));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void upsertOverride_travail_nominal_setsTypeLitige() {
        CaseAnalysis saved = service.upsertOverride(caseFileId,
                "LICENCIEMENT_ECONOMIQUE", "Documents probants",
                null, mock(Principal.class));

        assertThat(saved.getTypeLitigeAvocatOverride()).isEqualTo("LICENCIEMENT_ECONOMIQUE");
        assertThat(saved.getTypeProcedureAvocatOverride()).isNull();
        assertThat(saved.getTypeOverrideRaison()).isEqualTo("Documents probants");
        verify(caseAnalysisRepository, times(1)).save(latestAnalysis);
    }

    @Test
    void upsertOverride_immigration_nominal_setsTypeProcedure() {
        caseFile.setLegalDomain("DROIT_IMMIGRATION");

        CaseAnalysis saved = service.upsertOverride(caseFileId,
                "OQTF_AVEC_DELAI", null,
                null, mock(Principal.class));

        assertThat(saved.getTypeProcedureAvocatOverride()).isEqualTo("OQTF_AVEC_DELAI");
        assertThat(saved.getTypeLitigeAvocatOverride()).isNull();
        assertThat(saved.getTypeOverrideRaison()).isNull();
    }

    @Test
    void upsertOverride_replacesPreviousValue_idempotent() {
        // 1er PUT
        service.upsertOverride(caseFileId, "LICENCIEMENT_ECONOMIQUE", "raison1",
                null, mock(Principal.class));
        // 2ᵉ PUT remplace
        CaseAnalysis saved = service.upsertOverride(caseFileId, "HARCELEMENT_MORAL", "raison2",
                null, mock(Principal.class));

        // Vérifie remplacement, pas accumulation
        assertThat(saved.getTypeLitigeAvocatOverride()).isEqualTo("HARCELEMENT_MORAL");
        assertThat(saved.getTypeOverrideRaison()).isEqualTo("raison2");
        verify(caseAnalysisRepository, times(2)).save(latestAnalysis);
    }

    @Test
    void upsertOverride_blankType_returns400() {
        assertThatThrownBy(() -> service.upsertOverride(caseFileId, "  ", null,
                null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void upsertOverride_unknownType_returns400() {
        assertThatThrownBy(() -> service.upsertOverride(caseFileId, "INEXISTANT_XYZ", null,
                null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void upsertOverride_travailTypeOnImmigrationCaseFile_returns400() {
        caseFile.setLegalDomain("DROIT_IMMIGRATION");

        assertThatThrownBy(() -> service.upsertOverride(caseFileId, "LICENCIEMENT_ECONOMIQUE", null,
                null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("Type non applicable au domaine");
    }

    @Test
    void upsertOverride_immigrationTypeOnTravailCaseFile_returns400() {
        // caseFile reste DROIT_DU_TRAVAIL
        assertThatThrownBy(() -> service.upsertOverride(caseFileId, "OQTF_AVEC_DELAI", null,
                null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("Type non applicable au domaine");
    }

    @Test
    void upsertOverride_noDoneAnalysis_returns404() {
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertOverride(caseFileId, "LICENCIEMENT_ECONOMIQUE", null,
                null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404")
                .hasMessageContaining("Aucune analyse à overrider");
    }

    @Test
    void upsertOverride_caseFileInOtherWorkspace_returns404() {
        Workspace otherWs = new Workspace();
        setField(otherWs, "id", UUID.randomUUID());
        caseFile.setWorkspace(otherWs);

        assertThatThrownBy(() -> service.upsertOverride(caseFileId, "LICENCIEMENT_ECONOMIQUE", null,
                null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getForCaseFile_returnsCurrentOverride() {
        latestAnalysis.setTypeLitigeAvocatOverride("DISCRIMINATION");
        latestAnalysis.setTypeOverrideRaison("Avocat conviction");

        TypeLitigeOverrideResponse resp = service.getForCaseFile(caseFileId, null, mock(Principal.class));

        assertThat(resp.typeLitigeAvocat()).isEqualTo("DISCRIMINATION");
        assertThat(resp.typeProcedureAvocat()).isNull();
        assertThat(resp.raison()).isEqualTo("Avocat conviction");
    }

    @Test
    void getForCaseFile_noOverride_returnsNullFields() {
        TypeLitigeOverrideResponse resp = service.getForCaseFile(caseFileId, null, mock(Principal.class));

        assertThat(resp.typeLitigeAvocat()).isNull();
        assertThat(resp.typeProcedureAvocat()).isNull();
        assertThat(resp.raison()).isNull();
    }

    @Test
    void getForCaseFile_noAnalysis_returnsNullFields() {
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.empty());

        TypeLitigeOverrideResponse resp = service.getForCaseFile(caseFileId, null, mock(Principal.class));

        assertThat(resp.typeLitigeAvocat()).isNull();
        assertThat(resp.typeProcedureAvocat()).isNull();
        assertThat(resp.raison()).isNull();
    }

    @Test
    void cloneOverrideFromPrevious_clonesAll3Columns() {
        UUID previousId = UUID.randomUUID();
        CaseAnalysis previous = new CaseAnalysis();
        setField(previous, "id", previousId);
        previous.setTypeLitigeAvocatOverride("LICENCIEMENT_ECONOMIQUE");
        previous.setTypeOverrideRaison("Avocat raison");
        when(caseAnalysisRepository.findById(previousId)).thenReturn(Optional.of(previous));

        CaseAnalysis newAnalysis = new CaseAnalysis();
        setField(newAnalysis, "id", UUID.randomUUID());

        service.cloneOverrideFromPrevious(previousId, newAnalysis);

        assertThat(newAnalysis.getTypeLitigeAvocatOverride()).isEqualTo("LICENCIEMENT_ECONOMIQUE");
        assertThat(newAnalysis.getTypeProcedureAvocatOverride()).isNull();
        assertThat(newAnalysis.getTypeOverrideRaison()).isEqualTo("Avocat raison");
        verify(caseAnalysisRepository, times(1)).save(newAnalysis);
    }

    @Test
    void cloneOverrideFromPrevious_immigration_clonesTypeProcedure() {
        UUID previousId = UUID.randomUUID();
        CaseAnalysis previous = new CaseAnalysis();
        setField(previous, "id", previousId);
        previous.setTypeProcedureAvocatOverride("OQTF_SANS_DELAI");
        when(caseAnalysisRepository.findById(previousId)).thenReturn(Optional.of(previous));

        CaseAnalysis newAnalysis = new CaseAnalysis();
        setField(newAnalysis, "id", UUID.randomUUID());

        service.cloneOverrideFromPrevious(previousId, newAnalysis);

        assertThat(newAnalysis.getTypeProcedureAvocatOverride()).isEqualTo("OQTF_SANS_DELAI");
        assertThat(newAnalysis.getTypeLitigeAvocatOverride()).isNull();
    }

    @Test
    void cloneOverrideFromPrevious_noOverrideOnPrevious_isNoOp() {
        UUID previousId = UUID.randomUUID();
        CaseAnalysis previous = new CaseAnalysis();
        setField(previous, "id", previousId);
        // both override fields null
        when(caseAnalysisRepository.findById(previousId)).thenReturn(Optional.of(previous));

        CaseAnalysis newAnalysis = new CaseAnalysis();
        setField(newAnalysis, "id", UUID.randomUUID());

        service.cloneOverrideFromPrevious(previousId, newAnalysis);

        // No save expected, override remains null
        assertThat(newAnalysis.getTypeLitigeAvocatOverride()).isNull();
        assertThat(newAnalysis.getTypeProcedureAvocatOverride()).isNull();
        verify(caseAnalysisRepository, times(0)).save(newAnalysis);
    }

    @Test
    void cloneOverrideFromPrevious_failOpenOnException() {
        UUID previousId = UUID.randomUUID();
        when(caseAnalysisRepository.findById(previousId))
                .thenThrow(new RuntimeException("DB down"));

        CaseAnalysis newAnalysis = new CaseAnalysis();
        setField(newAnalysis, "id", UUID.randomUUID());

        // ne lance pas — fail-open
        service.cloneOverrideFromPrevious(previousId, newAnalysis);

        assertThat(newAnalysis.getTypeLitigeAvocatOverride()).isNull();
    }

    @Test
    void readOverrideForLatestDone_returnsSnapshot() {
        latestAnalysis.setTypeLitigeAvocatOverride("HEURES_SUPPLEMENTAIRES");
        latestAnalysis.setTypeOverrideRaison("Doc preuve");

        TypeLitigeOverrideService.OverrideSnapshot snap = service.readOverrideForLatestDone(caseFileId);

        assertThat(snap).isNotNull();
        assertThat(snap.typeLitige()).isEqualTo("HEURES_SUPPLEMENTAIRES");
        assertThat(snap.raison()).isEqualTo("Doc preuve");
    }

    @Test
    void readOverrideForLatestDone_noOverride_returnsNull() {
        TypeLitigeOverrideService.OverrideSnapshot snap = service.readOverrideForLatestDone(caseFileId);

        assertThat(snap).isNull();
    }

    private static void setField(Object target, String field, Object value) {
        try {
            java.lang.reflect.Field f = findField(target.getClass(), field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
}
