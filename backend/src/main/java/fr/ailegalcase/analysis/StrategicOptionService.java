package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * F-176 SF-176-01 — service applicatif des "Pistes stratégiques".
 *
 * Pattern miroir de {@link ProcedureCheckService} : extraction fail-open depuis le JSON IA,
 * persistance par analyse, mise à jour de statut avec isolation workspace, propagation
 * inter-analyses des pistes RETAINED + DISCARDED.
 */
@Service
public class StrategicOptionService {

    private static final Logger log = LoggerFactory.getLogger(StrategicOptionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StrategicOptionRepository strategicOptionRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public StrategicOptionService(StrategicOptionRepository strategicOptionRepository,
                                  CaseAnalysisRepository caseAnalysisRepository,
                                  CaseFileRepository caseFileRepository,
                                  WorkspaceMemberRepository workspaceMemberRepository,
                                  CurrentUserResolver currentUserResolver) {
        this.strategicOptionRepository = strategicOptionRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /** Représentation parsée d'une piste stratégique du JSON IA. */
    record ParsedOption(String texte, String baseJuridique, String horizonTemporel,
                        List<String> conditions, String source) {}

    static List<ParsedOption> parsePistesStrategiques(JsonNode node) {
        List<ParsedOption> result = new ArrayList<>();
        if (node == null || !node.isArray()) return result;
        for (JsonNode item : node) {
            if (!item.isObject()) continue;
            JsonNode texteNode = item.get("texte");
            if (texteNode == null || !texteNode.isTextual()) continue;
            String texte = texteNode.asText().trim();
            if (texte.isEmpty()) continue;

            String baseJuridique = optionalText(item, "base_juridique");
            String horizonTemporel = optionalText(item, "horizon_temporel");
            String source = optionalText(item, "source");

            List<String> conditions = new ArrayList<>();
            JsonNode condNode = item.get("conditions");
            if (condNode != null && condNode.isArray()) {
                for (JsonNode c : condNode) {
                    if (c.isTextual()) {
                        String s = c.asText().trim();
                        if (!s.isEmpty()) conditions.add(s);
                    }
                }
            }

            result.add(new ParsedOption(texte, baseJuridique, horizonTemporel, conditions, source));
        }
        return result;
    }

    private static String optionalText(JsonNode obj, String field) {
        JsonNode n = obj.get(field);
        if (n == null || !n.isTextual()) return null;
        String s = n.asText().trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Extrait `pistes_strategiques` du JSON brut et persiste les pistes (statut TO_STUDY).
     * Fail-open : si extraction échoue, aucune piste créée, log debug.
     * Remplace les pistes existantes pour cette analyse (alignement F-96 createChecks).
     */
    @Transactional
    public void persistFromAnalysis(CaseAnalysis analysis, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return;

        String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson);
        List<ParsedOption> options;
        try {
            JsonNode root = MAPPER.readTree(stripped);
            options = parsePistesStrategiques(root.get("pistes_strategiques"));
        } catch (Exception e) {
            log.debug("pistes_strategiques extraction failed for analysis {} — skipping", analysis.getId());
            return;
        }

        if (options.isEmpty()) return;

        strategicOptionRepository.deleteByCaseAnalysisId(analysis.getId());

        Workspace workspace = analysis.getCaseFile().getWorkspace();
        for (int i = 0; i < options.size(); i++) {
            ParsedOption opt = options.get(i);
            StrategicOption entity = new StrategicOption();
            entity.setCaseAnalysis(analysis);
            entity.setWorkspace(workspace);
            entity.setOrdre(i);
            entity.setTexte(opt.texte());
            entity.setBaseJuridique(opt.baseJuridique());
            entity.setHorizonTemporel(opt.horizonTemporel());
            entity.setConditionsJson(serializeConditions(opt.conditions()));
            entity.setSource(opt.source());
            entity.setStatut(StrategicOptionStatus.TO_STUDY);
            strategicOptionRepository.save(entity);
        }
    }

    private static String serializeConditions(List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(conditions);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Snapshot des pistes RETAINED + DISCARDED de l'analyse précédente — utilisé pour
     * injecter dans le prompt enrichi (RETAINED = approfondir, DISCARDED = ne pas re-proposer).
     * Fail-open : retourne {@link EnrichmentSnapshot#empty()} si quoi que ce soit échoue.
     */
    @Transactional(readOnly = true)
    public EnrichmentSnapshot collectForEnrichment(UUID previousAnalysisId) {
        if (previousAnalysisId == null) return EnrichmentSnapshot.empty();
        try {
            List<String> retained = strategicOptionRepository
                    .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(previousAnalysisId, StrategicOptionStatus.RETAINED)
                    .stream().map(StrategicOption::getTexte).toList();
            List<String> discarded = strategicOptionRepository
                    .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(previousAnalysisId, StrategicOptionStatus.DISCARDED)
                    .stream().map(StrategicOption::getTexte).toList();
            return new EnrichmentSnapshot(retained, discarded);
        } catch (Exception e) {
            log.warn("collectForEnrichment failed for previousAnalysisId {} — skipping", previousAnalysisId, e);
            return EnrichmentSnapshot.empty();
        }
    }

    public record EnrichmentSnapshot(List<String> retainedTexts, List<String> discardedTexts) {
        public static EnrichmentSnapshot empty() {
            return new EnrichmentSnapshot(List.of(), List.of());
        }
    }

    /**
     * Clone les pistes RETAINED + DISCARDED de l'analyse précédente sur la nouvelle analyse,
     * statuts conservés, ordre re-calculé à la suite des pistes nouvellement extraites par
     * {@link #persistFromAnalysis}. Pattern miroir F-96 createChecksWithVerifiedPropagation.
     * Fail-open.
     */
    @Transactional
    public void propagateRetainedAndDiscarded(UUID previousAnalysisId, CaseAnalysis newAnalysis) {
        if (previousAnalysisId == null) return;
        try {
            List<StrategicOption> currentOptions = strategicOptionRepository
                    .findByCaseAnalysisIdOrderByOrdreAsc(newAnalysis.getId());
            int offset = currentOptions.size();
            Workspace workspace = newAnalysis.getCaseFile().getWorkspace();

            List<StrategicOption> retained = strategicOptionRepository
                    .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(previousAnalysisId, StrategicOptionStatus.RETAINED);
            List<StrategicOption> discarded = strategicOptionRepository
                    .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(previousAnalysisId, StrategicOptionStatus.DISCARDED);

            for (StrategicOption src : retained) {
                StrategicOption copy = cloneFor(src, newAnalysis, workspace, offset++);
                strategicOptionRepository.save(copy);
            }
            for (StrategicOption src : discarded) {
                StrategicOption copy = cloneFor(src, newAnalysis, workspace, offset++);
                strategicOptionRepository.save(copy);
            }
        } catch (Exception e) {
            log.warn("propagateRetainedAndDiscarded failed for newAnalysis {} from {} — skipping",
                    newAnalysis.getId(), previousAnalysisId, e);
        }
    }

    private static StrategicOption cloneFor(StrategicOption src, CaseAnalysis newAnalysis, Workspace workspace, int ordre) {
        StrategicOption copy = new StrategicOption();
        copy.setCaseAnalysis(newAnalysis);
        copy.setWorkspace(workspace);
        copy.setOrdre(ordre);
        copy.setTexte(src.getTexte());
        copy.setBaseJuridique(src.getBaseJuridique());
        copy.setHorizonTemporel(src.getHorizonTemporel());
        copy.setConditionsJson(src.getConditionsJson());
        copy.setSource(src.getSource());
        copy.setStatut(src.getStatut());
        copy.setRaisonDiscard(src.getRaisonDiscard());
        return copy;
    }

    /**
     * Liste les pistes pour une analyse donnée, isolation workspace stricte.
     * 404 si dossier ou analyse inexistant ou hors workspace.
     */
    @Transactional(readOnly = true)
    public List<StrategicOptionResponse> list(UUID caseFileId, UUID analysisId,
                                              OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = resolveWorkspace(user);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace);

        CaseAnalysis analysis = caseAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));

        if (!analysis.getCaseFile().getId().equals(caseFile.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found");
        }

        return strategicOptionRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysisId)
                .stream().map(StrategicOptionResponse::from).toList();
    }

    /**
     * Met à jour le statut d'une piste avec isolation workspace.
     * raisonDiscard est optionnelle (null accepté).
     */
    @Transactional
    public StrategicOptionResponse updateStatus(UUID optionId, String newStatut, String raisonDiscard,
                                                OidcUser oidcUser, Principal principal) {
        StrategicOptionStatus status;
        try {
            status = StrategicOptionStatus.valueOf(newStatut);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide : " + newStatut);
        }

        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = resolveWorkspace(user);

        StrategicOption option = strategicOptionRepository.findByIdAndWorkspaceId(optionId, workspace.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Strategic option not found"));

        option.setStatut(status);
        // raisonDiscard est appliquée seulement si statut = DISCARDED ; sinon nettoyée pour cohérence
        option.setRaisonDiscard(status == StrategicOptionStatus.DISCARDED ? raisonDiscard : null);
        return StrategicOptionResponse.from(strategicOptionRepository.save(option));
    }

    private Workspace resolveWorkspace(User user) {
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    private CaseFile resolveCaseFile(UUID caseFileId, Workspace workspace) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }
}
