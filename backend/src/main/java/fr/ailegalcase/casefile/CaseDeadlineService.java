package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CaseDeadlineService {

    private static final Logger log = LoggerFactory.getLogger(CaseDeadlineService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CaseDeadlineRepository deadlineRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public CaseDeadlineService(CaseDeadlineRepository deadlineRepository,
                               CaseFileRepository caseFileRepository,
                               WorkspaceMemberRepository workspaceMemberRepository,
                               CurrentUserResolver currentUserResolver) {
        this.deadlineRepository = deadlineRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * F-289 (fix) — sources injectées dans {@code case_deadlines} par F-194 (pièces
     * à demander) / F-193 (checks procéduraux) / F-192 (pistes) pour leur donner une
     * présence datée. Ce ne sont PAS des délais : elles ont leurs propres panneaux et
     * ne sont pas rendues par la section « DÉLAIS DU DOSSIER » (F-69). Mais elles
     * étaient comptées dans le chip (« 30 délais » alors que 6 affichés) → on les EXCLUT
     * de la liste, comme l'échéancier (EcheancierService) le fait déjà.
     */
    private static final java.util.Set<String> NON_DEADLINE_SOURCES =
            java.util.Set.of("PIECE_A_DEMANDER", "PROCEDURE_CHECK_TO_CHECK", "PISTE_RETENUE");

    @Transactional(readOnly = true)
    public List<CaseDeadlineResponse> list(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        return deadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())
                .stream()
                .filter(d -> !NON_DEADLINE_SOURCES.contains(d.getSource()))
                .map(CaseDeadlineResponse::from).toList();
    }

    @Transactional
    public CaseDeadlineResponse create(UUID caseFileId, CaseDeadlineRequest request,
                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        CaseDeadline deadline = new CaseDeadline();
        deadline.setCaseFile(caseFile);
        deadline.setLabel(request.label().trim());
        deadline.setDueDate(request.dueDate());
        return CaseDeadlineResponse.from(deadlineRepository.save(deadline));
    }

    @Transactional
    public CaseDeadlineResponse update(UUID caseFileId, UUID deadlineId, CaseDeadlineRequest request,
                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);
        CaseDeadline deadline = resolveDeadlineForWorkspace(deadlineId, caseFileId);
        deadline.setLabel(request.label().trim());
        deadline.setDueDate(request.dueDate());
        return CaseDeadlineResponse.from(deadlineRepository.save(deadline));
    }

    @Transactional
    public void delete(UUID caseFileId, UUID deadlineId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);
        CaseDeadline deadline = resolveDeadlineForWorkspace(deadlineId, caseFileId);
        deadlineRepository.delete(deadline);
    }

    /**
     * Crée les délais IA détectés à partir du JSON brut de l'analyse.
     * Fail-open : si delais_detectes absent, mal formé ou date invalide, les entrées concernées sont ignorées.
     * La méthode ne propage jamais d'exception.
     */
    @Transactional
    public void createAiDetectedDeadlines(CaseAnalysis analysis, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return;
        try {
            String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson);
            JsonNode root = MAPPER.readTree(stripped);
            JsonNode delaisNode = root.get("delais_detectes");
            if (delaisNode == null || !delaisNode.isArray()) return;

            CaseFile caseFile = analysis.getCaseFile();
            for (JsonNode item : delaisNode) {
                try {
                    JsonNode labelNode = item.get("label");
                    JsonNode dateNode = item.get("date_detectee");
                    if (labelNode == null || dateNode == null) continue;
                    String label = labelNode.asText();
                    LocalDate dueDate = LocalDate.parse(dateNode.asText());

                    CaseDeadline deadline = new CaseDeadline();
                    deadline.setCaseFile(caseFile);
                    deadline.setLabel(label);
                    deadline.setDueDate(dueDate);
                    deadline.setSource("AI");
                    deadline.setAiStatus("PENDING");
                    deadlineRepository.save(deadline);
                } catch (Exception e) {
                    log.debug("Skipping invalid deadline entry in analysis {} — {}", analysis.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Fail-open: AI deadline detection failed for analysis {}: {}", analysis.getId(), e.getMessage());
        }
    }

    /**
     * Valide (ACCEPT ou REJECT) un délai IA en attente.
     * ACCEPT → aiStatus = ACCEPTED
     * REJECT → suppression physique du délai
     *
     * @return la réponse mise à jour (ACCEPT), ou null (REJECT)
     */
    @Transactional
    public CaseDeadlineResponse validate(UUID caseFileId, UUID deadlineId, String action,
                                         OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);
        CaseDeadline deadline = resolveDeadlineForWorkspace(deadlineId, caseFileId);

        if (!"PENDING".equals(deadline.getAiStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce délai a déjà été traité.");
        }

        if ("ACCEPT".equals(action)) {
            deadline.setAiStatus("ACCEPTED");
            return CaseDeadlineResponse.from(deadlineRepository.save(deadline));
        } else {
            deadlineRepository.delete(deadline);
            return null;
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        return caseFile;
    }

    private CaseDeadline resolveDeadlineForWorkspace(UUID deadlineId, UUID caseFileId) {
        return deadlineRepository.findById(deadlineId)
                .filter(d -> d.getCaseFile().getId().equals(caseFileId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deadline not found"));
    }
}
