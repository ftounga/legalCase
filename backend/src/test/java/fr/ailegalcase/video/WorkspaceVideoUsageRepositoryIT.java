package fr.ailegalcase.video;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-231-03 — IT du repository {@link WorkspaceVideoUsageRepository}. Vérifie :
 * <ul>
 *   <li>{@code sumMinutesByWorkspaceAndMonth} additionne correctement plusieurs lignes ;
 *   <li>isolation workspace : un INSERT pour un autre workspace n'affecte pas le total ;
 *   <li>isolation mois : un INSERT pour un autre yearMonth n'affecte pas le total ;
 *   <li>workspace sans données → 0 (et pas null grâce au COALESCE).
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@Transactional
class WorkspaceVideoUsageRepositoryIT {

    @Autowired private WorkspaceVideoUsageRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private DocumentRepository documentRepository;

    private Workspace workspace;
    private Workspace otherWorkspace;
    private Document doc1;
    private Document doc2;
    private Document docOther;

    private final String month = YearMonth.now().toString();
    private final String pastMonth = YearMonth.now().minusMonths(2).toString();

    @BeforeEach
    void setUp() {
        // Cleanup pour isolation
        repository.deleteAll();

        User user = new User();
        user.setEmail("vqs-it-" + System.nanoTime() + "@example.com");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        workspace = new Workspace();
        workspace.setName(user.getEmail());
        workspace.setSlug("vqs-it-slug-" + System.nanoTime());
        workspace.setOwner(user);
        workspace.setPlanCode("SOLO");
        workspace.setStatus("ACTIVE");
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace = workspaceRepository.save(workspace);

        otherWorkspace = new Workspace();
        otherWorkspace.setName("other-" + user.getEmail());
        otherWorkspace.setSlug("vqs-it-other-slug-" + System.nanoTime());
        otherWorkspace.setOwner(user);
        otherWorkspace.setPlanCode("PRO");
        otherWorkspace.setStatus("ACTIVE");
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        otherWorkspace = workspaceRepository.save(otherWorkspace);

        // CaseFile + Documents pour FK
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier vidéo IT");
        caseFile.setStatus("OPEN");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile = caseFileRepository.save(caseFile);

        doc1 = newVideoDoc(caseFile, user, "v1.mp4");
        doc2 = newVideoDoc(caseFile, user, "v2.mp4");

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(user);
        otherCaseFile.setTitle("Dossier autre WS");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        otherCaseFile = caseFileRepository.save(otherCaseFile);
        docOther = newVideoDoc(otherCaseFile, user, "v-other.mp4");
    }

    private Document newVideoDoc(CaseFile cf, User uploader, String name) {
        Document d = new Document();
        d.setCaseFile(cf);
        d.setUploadedBy(uploader);
        d.setOriginalFilename(name);
        d.setContentType("video/mp4");
        d.setFileSize(1024L);
        d.setStorageKey("test/" + name + "-" + UUID.randomUUID());
        return documentRepository.save(d);
    }

    @Test
    void I_VWUR_sum_emptyWorkspace_returnsZero() {
        int total = repository.sumMinutesByWorkspaceAndMonth(workspace.getId(), month);
        assertThat(total).isZero();
    }

    @Test
    void I_VWUR_sum_singleRow_returnsValue() {
        repository.save(usage(workspace.getId(), doc1.getId(), month, 3));

        int total = repository.sumMinutesByWorkspaceAndMonth(workspace.getId(), month);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void I_VWUR_sum_multipleRows_aggregatesCorrectly() {
        repository.save(usage(workspace.getId(), doc1.getId(), month, 2));
        repository.save(usage(workspace.getId(), doc2.getId(), month, 5));

        int total = repository.sumMinutesByWorkspaceAndMonth(workspace.getId(), month);
        assertThat(total).isEqualTo(7);
    }

    @Test
    void I_VWUR_sum_otherWorkspace_isolated_returnsZero() {
        // workspace courant : 4 min. otherWorkspace : 10 min.
        repository.save(usage(workspace.getId(), doc1.getId(), month, 4));
        repository.save(usage(otherWorkspace.getId(), docOther.getId(), month, 10));

        assertThat(repository.sumMinutesByWorkspaceAndMonth(workspace.getId(), month)).isEqualTo(4);
        assertThat(repository.sumMinutesByWorkspaceAndMonth(otherWorkspace.getId(), month)).isEqualTo(10);
    }

    @Test
    void I_VWUR_sum_otherMonth_isolated() {
        // 3 min en mois courant, 99 min en mois passé → on filtre uniquement le mois courant
        repository.save(usage(workspace.getId(), doc1.getId(), month, 3));
        repository.save(usage(workspace.getId(), doc2.getId(), pastMonth, 99));

        assertThat(repository.sumMinutesByWorkspaceAndMonth(workspace.getId(), month)).isEqualTo(3);
        assertThat(repository.sumMinutesByWorkspaceAndMonth(workspace.getId(), pastMonth)).isEqualTo(99);
    }

    private WorkspaceVideoUsage usage(UUID wid, UUID docId, String yearMonth, int minutes) {
        WorkspaceVideoUsage u = new WorkspaceVideoUsage();
        u.setWorkspaceId(wid);
        u.setDocumentId(docId);
        u.setYearMonth(yearMonth);
        u.setMinutesConsumed(minutes);
        return u;
    }
}
