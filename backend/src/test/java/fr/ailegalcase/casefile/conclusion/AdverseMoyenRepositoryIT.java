package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-261 / SF-261-05 — IT du repository des moyens adverses persistés : ordre stable,
 * delete + insert idempotent (replace-set), isolation par {@code case_file_id}.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
class AdverseMoyenRepositoryIT {

    @Autowired private AdverseMoyenRepository adverseMoyenRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private UserRepository userRepository;

    private CaseFile caseFile;
    private CaseFile otherCaseFile;

    @BeforeEach
    void setUp() {
        adverseMoyenRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("adverse-moyen-it@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        caseFile = newCaseFile(user, "Dossier moyens A", "moyen-a");
        otherCaseFile = newCaseFile(user, "Dossier moyens B", "moyen-b");
    }

    @Test
    void findByCaseFileIdOrderByOrdreAsc_returnsStableOrder() {
        adverseMoyenRepository.save(entity(caseFile, "Moyen 2", 1));
        adverseMoyenRepository.save(entity(caseFile, "Moyen 1", 0));
        adverseMoyenRepository.save(entity(caseFile, "Moyen 3", 2));

        List<AdverseMoyenEntity> result =
                adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFile.getId());

        assertThat(result).extracting(AdverseMoyenEntity::getThese)
                .containsExactly("Moyen 1", "Moyen 2", "Moyen 3");
        assertThat(adverseMoyenRepository.existsByCaseFileId(caseFile.getId())).isTrue();
    }

    @Test
    void deleteByCaseFileId_thenReinsert_isIdempotentReplace() {
        adverseMoyenRepository.save(entity(caseFile, "Ancien moyen", 0));
        assertThat(adverseMoyenRepository.existsByCaseFileId(caseFile.getId())).isTrue();

        // replace-set : on supprime puis réinsère un nouveau set
        adverseMoyenRepository.deleteByCaseFileId(caseFile.getId());
        adverseMoyenRepository.save(entity(caseFile, "Nouveau moyen", 0));

        List<AdverseMoyenEntity> result =
                adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFile.getId());
        assertThat(result).extracting(AdverseMoyenEntity::getThese).containsExactly("Nouveau moyen");
    }

    @Test
    void isolationByCaseFileId_moyensDoNotLeakAcrossCaseFiles() {
        adverseMoyenRepository.save(entity(caseFile, "Moyen dossier A", 0));
        adverseMoyenRepository.save(entity(otherCaseFile, "Moyen dossier B", 0));

        assertThat(adverseMoyenRepository.findByCaseFileIdOrderByOrdreAsc(caseFile.getId()))
                .extracting(AdverseMoyenEntity::getThese).containsExactly("Moyen dossier A");

        // supprimer le set du dossier A ne touche pas celui du dossier B
        adverseMoyenRepository.deleteByCaseFileId(caseFile.getId());
        assertThat(adverseMoyenRepository.existsByCaseFileId(caseFile.getId())).isFalse();
        assertThat(adverseMoyenRepository.existsByCaseFileId(otherCaseFile.getId())).isTrue();
    }

    private CaseFile newCaseFile(User owner, String title, String slugSuffix) {
        Workspace workspace = new Workspace();
        workspace.setName("WS-" + slugSuffix);
        workspace.setSlug("adverse-moyen-" + slugSuffix + "-" + System.nanoTime());
        workspace.setOwner(owner);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        CaseFile cf = new CaseFile();
        cf.setTitle(title);
        cf.setLegalDomain("DROIT_DU_TRAVAIL");
        cf.setStatus("OPEN");
        cf.setWorkspace(workspace);
        cf.setCreatedBy(owner);
        return caseFileRepository.save(cf);
    }

    private AdverseMoyenEntity entity(CaseFile cf, String these, int ordre) {
        AdverseMoyenEntity entity = new AdverseMoyenEntity();
        entity.setCaseFileId(cf.getId());
        entity.setThese(these);
        entity.setFondements("[]");
        entity.setPiecesInvoquees("[]");
        entity.setOrdre(ordre);
        return entity;
    }
}
