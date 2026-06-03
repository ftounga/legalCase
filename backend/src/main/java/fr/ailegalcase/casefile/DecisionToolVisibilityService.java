package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SF-IA-04-01 : résout la liste d'outils décisionnels à afficher pour un dossier
 * donné, en trois couches (alwaysOn / contextual / catalog).
 *
 * Pur read-only : lit la configuration {@link DecisionToolVisibilityRule} et les
 * codes de situation détectés par l'IA dans la dernière analyse DONE. Ne modifie
 * aucun état.
 */
@Service
public class DecisionToolVisibilityService {

    private static final Logger log = LoggerFactory.getLogger(DecisionToolVisibilityService.class);

    private final DecisionToolVisibilityRuleRepository ruleRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;
    // SF-238-03 : activations manuelles injectées dans contextual au runtime.
    private final ManualToolActivationRepository manualToolActivationRepository;

    public DecisionToolVisibilityService(DecisionToolVisibilityRuleRepository ruleRepository,
                                         CaseFileRepository caseFileRepository,
                                         CaseAnalysisRepository caseAnalysisRepository,
                                         WorkspaceMemberRepository workspaceMemberRepository,
                                         CurrentUserResolver currentUserResolver,
                                         ObjectMapper objectMapper,
                                         ManualToolActivationRepository manualToolActivationRepository) {
        this.ruleRepository = ruleRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
        this.manualToolActivationRepository = manualToolActivationRepository;
    }

    @Transactional(readOnly = true)
    public VisibleToolSetResponse resolveVisibleTools(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String legalDomain = caseFile.getLegalDomain();
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        if (legalDomain == null) {
            log.warn("CaseFile {} has null legalDomain — returning empty visibility set", caseFileId);
            return new VisibleToolSetResponse(List.of(), List.of(), List.of());
        }

        List<DecisionToolVisibilityRule> rules = ruleRepository.findForDomainAndCountry(legalDomain, country);
        Map<String, Set<String>> detectedSituations = extractDetectedSituations(caseFileId);

        VisibleToolSetResponse base = buildResponse(rules, detectedSituations);

        // SF-238-03 : injecter les activations manuelles. Chaque tool_id actif :
        //  - retiré de catalog,
        //  - ajouté à contextual s'il n'y est pas déjà.
        // alwaysOn n'est jamais impacté (déjà toujours visible).
        Set<String> manuallyActivated = manualToolActivationRepository
                .findByCaseFileIdAndDeactivatedAtIsNull(caseFileId)
                .stream()
                .map(ManualToolActivation::getToolId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (manuallyActivated.isEmpty()) {
            return base;
        }

        LinkedHashSet<String> contextual = new LinkedHashSet<>(base.contextual());
        Set<String> alwaysOnSet = new HashSet<>(base.alwaysOn());
        for (String toolId : manuallyActivated) {
            if (!alwaysOnSet.contains(toolId)) {
                contextual.add(toolId);
            }
        }
        List<String> catalog = base.catalog().stream()
                .filter(t -> !manuallyActivated.contains(t))
                .collect(java.util.stream.Collectors.toList());

        return new VisibleToolSetResponse(base.alwaysOn(), new ArrayList<>(contextual), catalog);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        UUID userWorkspaceId = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId())
                .orElse(null);
        if (userWorkspaceId == null || !userWorkspaceId.equals(cf.getWorkspace().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return cf;
    }

    /**
     * Résout les codes de situation détectés par l'IA sur la dernière analyse
     * DONE du dossier. Retourne une map `trigger_field -> set de trigger_values`.
     *
     * Tolère l'absence d'analyse (map vide) et les JSON mal formés (skip + log).
     *
     * <p>F-197 SF-197-01 — applique l'override avocat en priorité sur les valeurs
     * IA brutes pour les champs {@code type_litige_detecte} (Travail FR) et
     * {@code type_procedure_detectee} (Immigration). Les autres champs restent
     * inchangés (lecture IA pure).</p>
     */
    private Map<String, Set<String>> extractDetectedSituations(UUID caseFileId) {
        CaseAnalysis latest = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);
        if (latest == null || latest.getAnalysisResult() == null || latest.getAnalysisResult().isBlank()) {
            return Map.of();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(latest.getAnalysisResult());
        } catch (Exception e) {
            log.warn("CaseAnalysis {} for caseFile {} has invalid JSON — skipping detection", latest.getId(), caseFileId);
            return Map.of();
        }

        Map<String, Set<String>> detected = new HashMap<>();
        addIfPresent(detected, "type_rupture", readString(root.path("compensation_data").path("type_rupture")));
        addIfPresent(detected, "type_rupture", readString(root.path("type_rupture")));

        // F-197 SF-197-01 — type_procedure : override avocat prioritaire sur IA.
        String typeProcedureOverride = latest.getTypeProcedureAvocatOverride();
        if (typeProcedureOverride != null && !typeProcedureOverride.isBlank()) {
            addIfPresent(detected, "type_procedure_detectee", typeProcedureOverride);
        } else {
            addIfPresent(detected, "type_procedure_detectee", readString(root.path("type_procedure_detectee")));
        }

        // F-197 SF-197-01 — type_litige : override avocat prioritaire sur IA.
        // L'override est aussi propagé vers les trigger_field réellement utilisés par
        // les règles de visibilité (type_rupture, motif_nullite_pressenti, etc.) pour
        // que F-DT-11/12/13/20 etc. s'activent même si l'IA n'a pas peuplé ces champs
        // ou les a peuplés différemment.
        String typeLitigeOverride = latest.getTypeLitigeAvocatOverride();
        if (typeLitigeOverride != null && !typeLitigeOverride.isBlank()) {
            addIfPresent(detected, "type_litige_detecte", typeLitigeOverride);
            propagateTypeLitigeOverrideTriggers(detected, typeLitigeOverride);
        } else {
            addIfPresent(detected, "type_litige_detecte", readString(root.path("type_litige_detecte")));
        }

        addIfPresent(detected, "type_recours_code", readString(root.path("type_recours_code")));
        addIfPresent(detected, "type_titre_sejour_code", readString(root.path("type_titre_sejour_code")));
        addIfPresent(detected, "regime_matrimonial",
                readString(root.path("liquidation_communaute_data").path("regime_matrimonial")));
        addIfPresent(detected, "mode_garde_detaille",
                readString(root.path("pension_alimentaire_data").path("mode_garde_detaille")));

        // F-165 SF-165-01 : 5 nouveaux trigger_field lus depuis travail_extracted_data
        // pour basculer 14 outils Travail FR ALWAYS_ON → CONTEXTUAL.
        JsonNode travailNode = root.path("travail_extracted_data");
        addIfPresent(detected, "type_contrat", readString(travailNode.path("type_contrat")));
        addIfPresent(detected, "motif_licenciement", readString(travailNode.path("motif_licenciement")));
        addIfPresent(detected, "origine_inaptitude_pressentie",
                readString(travailNode.path("origine_inaptitude_pressentie")));
        addIfPresent(detected, "motif_nullite_pressenti",
                readString(travailNode.path("motif_nullite_pressenti")));
        // heures_sup_mentionnees est un objet JSON ({total_declarees_25pct, ...} ou null) ;
        // on émet le trigger "PRESENT" dès qu'au moins le node est un objet non vide.
        JsonNode heuresSupNode = travailNode.path("heures_sup_mentionnees");
        if (heuresSupNode != null && heuresSupNode.isObject() && !heuresSupNode.isEmpty()) {
            addIfPresent(detected, "heures_sup_mentionnees", "PRESENT");
        }

        // F-166 SF-166-02 : 8 flags décisionnels niveau 3 — booleans dans travail_extracted_data.
        // Émettre la string "true" dans la map quand le flag est à true (consommé par CONTEXTUAL
        // rules de la migration 199 : F-DT-20/21/24/30/31/33/34/35).
        addBooleanFlagIfTrue(detected, travailNode, "rappel_salaire_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "travail_dissimule_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "clause_non_concurrence_detectee");
        addBooleanFlagIfTrue(detected, travailNode, "statut_protege_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "transaction_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "at_mp_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "urgence_procedurale");
        addBooleanFlagIfTrue(detected, travailNode, "contestation_are_envisagee");

        // F-204 : 5 flags Travail BE — booleans dans travail_extracted_data.
        // Migration 215 : F-DT-11/12/15/19/27 BE basculent ALWAYS_ON → CONTEXTUAL.
        addBooleanFlagIfTrue(detected, travailNode, "harcelement_be_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "discrimination_be_detectee");
        addBooleanFlagIfTrue(detected, travailNode, "inaptitude_medicale_be_detectee");
        addBooleanFlagIfTrue(detected, travailNode, "heures_sup_mentionnees_be");
        addBooleanFlagIfTrue(detected, travailNode, "motif_grave_be_envisage");

        // F-205 : 23 flags Travail FR additionnels — booleans dans travail_extracted_data.
        // Aucune migration de visibilité dans cette SF — les flags sont propagés dès maintenant
        // pour que F-206 (P1) et F-212 (P2) puissent les consommer sans modification ultérieure
        // du service. Aucun outil existant n'est impacté tant que decision_tool_visibility_rules
        // n'a pas de règle CONTEXTUAL référençant ces trigger_field.
        addBooleanFlagIfTrue(detected, travailNode, "abandon_poste_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "arret_maladie_long_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "prise_acte_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "resiliation_judiciaire_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "forfait_jours_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "transfert_entreprise_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "faute_inexcusable_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "cs_crp_envisage");
        addBooleanFlagIfTrue(detected, travailNode, "csp_propose");
        addBooleanFlagIfTrue(detected, travailNode, "mutation_refusee");
        addBooleanFlagIfTrue(detected, travailNode, "modification_contrat_refusee");
        // SF-212-19 : nouveau flag F-205 — déclenche F-DT-48 mise à pied disciplinaire.
        addBooleanFlagIfTrue(detected, travailNode, "mise_a_pied_disciplinaire_detectee");
        // SF-212-23 : nouveau flag F-205 — déclenche F-DT-56 égalité salariale femmes/hommes.
        addBooleanFlagIfTrue(detected, travailNode, "egalite_salariale_pressentie");
        // SF-212-21 : nouveau flag F-205 — déclenche F-DT-41 démission validité équivoque.
        addBooleanFlagIfTrue(detected, travailNode, "demission_equivoque_pressentie");
        // SF-212-17 : nouveau flag F-205 — déclenche F-DT-43 rupture anticipée CDD.
        addBooleanFlagIfTrue(detected, travailNode, "rupture_anticipee_cdd_detectee");
        // SF-212-35 : nouveau flag F-205 — déclenche F-DT-46 PDV / RCC conformité.
        addBooleanFlagIfTrue(detected, travailNode, "pdv_rcc_envisage");
        // SF-212-29 : nouveau flag F-205 — déclenche F-DT-77 congé maternité / paternité.
        addBooleanFlagIfTrue(detected, travailNode, "conge_maternite_paternite_detecte");
        // SF-212-27 : nouveau flag F-205 — déclenche F-DT-64 burn-out reconnaissance MP.
        addBooleanFlagIfTrue(detected, travailNode, "burnout_detecte");
        // SF-212-31 : nouveau flag F-205 — déclenche F-DT-65 élections CSE conformité.
        addBooleanFlagIfTrue(detected, travailNode, "election_cse_detectee");
        // SF-212-33 : nouveau flag F-205 — déclenche F-DT-49 temps partiel — requalification.
        addBooleanFlagIfTrue(detected, travailNode, "temps_partiel_requalification_envisagee");
        // SF-212-37 : nouveau flag F-205 — déclenche F-DT-84 conciliation CPH BCA.
        addBooleanFlagIfTrue(detected, travailNode, "conciliation_cph_envisagee");
        // SF-218-01 : nouveau flag F-205 — déclenche F-DT-86 appel CPH cour d'appel.
        addBooleanFlagIfTrue(detected, travailNode, "appel_cph_envisage");
        // SF-218-03 : nouveau flag F-205 — déclenche F-DT-88 exécution du jugement CPH / AGS.
        addBooleanFlagIfTrue(detected, travailNode, "execution_jugement_cph_envisagee");
        // SF-218-05 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-87 pourvoi cassation sociale.
        addBooleanFlagIfTrue(detected, travailNode, "pourvoi_cassation_soc_envisage");
        // SF-218-07 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-89 saisie sur rémunération.
        addBooleanFlagIfTrue(detected, travailNode, "saisie_remuneration_detectee");
        // SF-218-09 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-90 action de groupe en discrimination.
        addBooleanFlagIfTrue(detected, travailNode, "action_groupe_discrimination_envisagee");
        // SF-218-11 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-104 VRP indemnité de clientèle.
        addBooleanFlagIfTrue(detected, travailNode, "vrp_statut_detecte");
        // SF-218-13 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-108 particulier employeur / CESU.
        addBooleanFlagIfTrue(detected, travailNode, "particulier_employeur_detecte");
        // SF-218-15 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-105 statut journaliste professionnel.
        addBooleanFlagIfTrue(detected, travailNode, "statut_journaliste_detecte");
        // SF-218-17 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-106 intermittent du spectacle / ouverture droits ARE.
        addBooleanFlagIfTrue(detected, travailNode, "statut_intermittent_detecte");
        // SF-218-19 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-107 cadre dirigeant / qualification.
        addBooleanFlagIfTrue(detected, travailNode, "statut_cadre_dirigeant_detecte");
        // SF-218-21 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-109 stagiaire / gratification / requalification.
        addBooleanFlagIfTrue(detected, travailNode, "stage_detecte");
        // SF-218-23 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-110 apprentissage / validité de la rupture.
        addBooleanFlagIfTrue(detected, travailNode, "apprentissage_rupture_detectee");
        // SF-218-25 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-37 licenciement CDI de chantier / d'opération.
        addBooleanFlagIfTrue(detected, travailNode, "cdi_chantier_detecte");
        // SF-218-27 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-59 harcèlement / procédure interne de traitement d'un signalement.
        addBooleanFlagIfTrue(detected, travailNode, "harcelement_procedure_interne_detectee");
        // SF-218-29 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-66 NAO / négociation annuelle obligatoire.
        addBooleanFlagIfTrue(detected, travailNode, "nao_detectee");
        // SF-218-35 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-100 règlement intérieur / validité.
        addBooleanFlagIfTrue(detected, travailNode, "reglement_interieur_detecte");
        // SF-218-37 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-51 RTT / monétisation (rachat de jours de RTT).
        addBooleanFlagIfTrue(detected, travailNode, "rtt_monetisation_detectee");
        // SF-218-39 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-52 PPV / exonération (prime de partage de la valeur).
        addBooleanFlagIfTrue(detected, travailNode, "ppv_detectee");
        // SF-218-41 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-53 épargne salariale / conformité (intéressement / participation / partage de la valeur).
        addBooleanFlagIfTrue(detected, travailNode, "epargne_salariale_detectee");
        // SF-218-43 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-76 congés pour évènements familiaux.
        addBooleanFlagIfTrue(detected, travailNode, "conge_evt_familial_detecte");
        // SF-218-45 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-78 congé parental d'éducation.
        addBooleanFlagIfTrue(detected, travailNode, "conge_parental_detecte");
        // SF-218-47 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-79 congé de proche aidant.
        addBooleanFlagIfTrue(detected, travailNode, "conge_proche_aidant_detecte");
        // SF-218-49 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-80 RTT / acquisition selon accord d'aménagement.
        addBooleanFlagIfTrue(detected, travailNode, "rtt_acquisition_detectee");
        // SF-218-51 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-81 temps de trajet / déplacement professionnel.
        addBooleanFlagIfTrue(detected, travailNode, "temps_trajet_detecte");
        // SF-218-53 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-83 droit à la déconnexion / conformité.
        addBooleanFlagIfTrue(detected, travailNode, "droit_deconnexion_detecte");
        // SF-218-33 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-69 délégué syndical / RSS : désignation et protection.
        addBooleanFlagIfTrue(detected, travailNode, "delegation_syndicale_detectee");
        // SF-218-31 : nouveau flag pivot CONTEXTUAL — déclenche F-DT-67 accord d'entreprise / validité.
        addBooleanFlagIfTrue(detected, travailNode, "accord_entreprise_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "faute_grave_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "faute_lourde_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "cdd_requalification_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "interim_requalification_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "forfait_jours_validite_contestee");
        addBooleanFlagIfTrue(detected, travailNode, "prescription_proche_detectee");
        addBooleanFlagIfTrue(detected, travailNode, "rupture_amiable_negociee");
        addBooleanFlagIfTrue(detected, travailNode, "entretien_preavis_obtenu");
        addBooleanFlagIfTrue(detected, travailNode, "cse_consultation_demandee");
        addBooleanFlagIfTrue(detected, travailNode, "irp_election_demandee");
        addBooleanFlagIfTrue(detected, travailNode, "inspection_travail_saisie");
        addBooleanFlagIfTrue(detected, travailNode, "mediation_judiciaire_envisagee");

        // F-201 / F-203 : 9 flags Immigration FR + 5 flags Immigration BE.
        // Migrations 213 + 214 : 14 outils Immigration FR/BE basculent ALWAYS_ON → CONTEXTUAL.
        JsonNode immigrationNode = root.path("immigration_extracted_data");
        // F-201 (9 flags FR)
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_metiers_tension_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_familial_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_humanitaire_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_etudiant_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "changement_statut_envisage_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "procedure_asile_detectee");
        addBooleanFlagIfTrue(detected, immigrationNode, "naturalisation_envisagee_detectee");
        addBooleanFlagIfTrue(detected, immigrationNode, "client_mineur_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "mesure_eloignement_detectee");
        // F-203 (5 flags BE)
        addBooleanFlagIfTrue(detected, immigrationNode, "procedure_9bis_envisagee");
        addBooleanFlagIfTrue(detected, immigrationNode, "procedure_9ter_medicale_detectee");
        addBooleanFlagIfTrue(detected, immigrationNode, "regroupement_40bis_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "regroupement_40ter_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "oqt_annexe13_detectee");
        // SF-215-05 : F-IM-27 Regroupement 10bis BE — flag pivot CONTEXTUAL Immigration BE.
        addBooleanFlagIfTrue(detected, immigrationNode, "regroupement_10bis_detecte");
        // SF-215-07 : F-IM-28 Naturalisation 12bis BE — flag pivot CONTEXTUAL Immigration BE.
        // Partagé avec SF-215-09 (naturalisation conjoint Belge art. 16 CNB).
        addBooleanFlagIfTrue(detected, immigrationNode, "naturalisation_be_envisagee");
        // SF-215-11 : F-IM-30 AESM + tutelle MENA BE — flag pivot CONTEXTUAL Immigration BE.
        // Mineurs uniquement (gate ageActuel < 18 dans le calculator).
        addBooleanFlagIfTrue(detected, immigrationNode, "mineur_non_accompagne_be_detecte");
        // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE — flag pivot CONTEXTUAL Immigration BE.
        addBooleanFlagIfTrue(detected, immigrationNode, "recours_cce_envisage");
        // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE — flag pivot CONTEXTUAL Immigration BE.
        addBooleanFlagIfTrue(detected, immigrationNode, "recours_cce_extreme_urgence");
        // SF-215-17 : F-IM-33 Annexe 13quinquies OQT + interdiction d'entrée art. 74/11 BE — flag pivot CONTEXTUAL Immigration BE.
        addBooleanFlagIfTrue(detected, immigrationNode, "interdiction_entree_be_detectee");
        // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE — flag pivot CONTEXTUAL Immigration BE.
        addBooleanFlagIfTrue(detected, immigrationNode, "protection_temporaire_ukraine_detectee");
        // SF-221-01 : F-IM-53 Prorogation carte A BE — flag pivot CONTEXTUAL Immigration BE.
        addBooleanFlagIfTrue(detected, immigrationNode, "carte_a_prorogation_detecte");
        // SF-221-02 : F-IM-54 Carte B séjour illimité BE — flag pivot CONTEXTUAL Immigration BE.
        addBooleanFlagIfTrue(detected, immigrationNode, "carte_b_sejour_illimite_detecte");
        // SF-214-03 : F-IM-26 Regroupement familial FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "regroupement_familial_envisage");
        // SF-214-05 : F-IM-27 VPF liens personnels L.423-23 FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "vie_privee_familiale_detectee");
        // SF-214-15 : F-IM-32 récépissé vs attestation de prolongation R. 311-4/R. 311-6 FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "recouvrement_titre_en_cours");
        // SF-214-21 : F-IM-35 victime de la traite des êtres humains L. 425-1 FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "victime_traite_detectee");
        // SF-214-23 : F-IM-36 carte de résident 10 ans L. 426-1 FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "carte_resident_envisagee");
        // SF-214-25 : F-IM-37 ANEF procédure / pannes / recours FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "anef_panne_detectee");
        // SF-214-33 : F-IM-41 appel CAA / cassation CE délais FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "recours_envisage_detecte");
        // SF-214-35 : F-IM-42 assignation à résidence L. 731-1 FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "assignation_residence_detectee");
        // SF-214-41 : F-IM-45 retrait de titre pour fraude L. 412-7 FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "retrait_titre_fraude_detecte");
        // SF-214-39 : F-IM-44 séjour UE/EEE/Suisse directive 2004/38 FR — flag pivot CONTEXTUAL Immigration FR.
        addBooleanFlagIfTrue(detected, immigrationNode, "nationalite_ue");
        // SF-220-02 : F-IM-48 portée territoriale du titre à Mayotte FR — flag pivot CONTEXTUAL Immigration FR.
        // Pas de trigger « territoire » natif (extractDetectedSituations) : on utilise un flag
        // pivot booléen `mayotte_detecte` extrait dans immigration_extracted_data.
        addBooleanFlagIfTrue(detected, immigrationNode, "mayotte_detecte");
        // SF-220-03 : F-IM-49 VPF jeune majeur L.423-22 FR — flag pivot CONTEXTUAL Immigration FR.
        // Détecte un jeune majeur ex-MNA scolarisé (transition à la majorité / sortie ASE) :
        // flag pivot booléen `jeune_majeur_ex_mna_detecte` extrait dans immigration_extracted_data.
        addBooleanFlagIfTrue(detected, immigrationNode, "jeune_majeur_ex_mna_detecte");
        // SF-220-04 : F-IM-50 VPF au titre d'un PACS L.423-23 FR — flag pivot CONTEXTUAL Immigration FR.
        // Détecte un contexte de PACS apprécié comme faisceau d'indices de vie privée et familiale :
        // flag pivot booléen `pacs_detecte` extrait dans immigration_extracted_data.
        addBooleanFlagIfTrue(detected, immigrationNode, "pacs_detecte");
        // SF-220-05 : F-IM-51 déchéance de nationalité (Cciv 25 / 25-1) FR — flag pivot CONTEXTUAL Immigration FR.
        // Détecte un contexte de mesure (envisagée ou prononcée) de déchéance de la nationalité française :
        // flag pivot booléen `decheance_nationalite_detectee` extrait dans immigration_extracted_data.
        addBooleanFlagIfTrue(detected, immigrationNode, "decheance_nationalite_detectee");
        // SF-220-06 : F-IM-52 signalement SIS (Règl. UE 2018/1860 / CESEDA L.312-3) FR — flag pivot CONTEXTUAL Immigration FR.
        // Détecte un contexte de signalement aux fins de non-admission dans le SIS (contestation / radiation) :
        // flag pivot booléen `signalement_sis_detecte` extrait dans immigration_extracted_data.
        addBooleanFlagIfTrue(detected, immigrationNode, "signalement_sis_detecte");
        // SF-214-11 : F-IM-30 AES calcul présence prouvée FR — flag pivot DÉRIVÉ.
        // L'outil de calcul de présence est transversal aux 4 voies AES : il se déclenche
        // dès qu'au moins une des 4 voies AES est détectée. Le flag pivot
        // `aes_calcul_presence_declenche` n'est pas extrait par le pipeline IA — il est
        // dérivé ici par OR des 4 flags AES existants (F-201).
        addDerivedAesPresenceDeclenche(detected, immigrationNode);

        // F-235 : nationalite (texte libre normalisé titlecase) — consommée par
        // les règles CONTEXTUAL conditionnées à un régime national bilatéral
        // (F-IM-17 algérien, ouverture future tunisien/marocain/sénégalais).
        // Lue depuis immigration_extracted_data.nationalite ; root.nationalite en fallback
        // pour rétrocompat. Skip silencieux si absente, null ou blank.
        addIfPresent(detected, "nationalite",
                normalizeTitleCase(readString(immigrationNode.path("nationalite"))));
        addIfPresent(detected, "nationalite",
                normalizeTitleCase(readString(root.path("nationalite"))));

        // F-200 + F-202 : flags Famille FR (30) + Famille BE (5) — booleans dans famille_extracted_data.
        //   F-200 (migration 216) : 30 outils Famille FR ALWAYS_ON → CONTEXTUAL.
        //   F-202 (migration 217) : F-FA-11-desunion-irremediable-be ALWAYS_ON → CONTEXTUAL
        //     (trigger divorce_ddi_envisage). Les 4 autres flags BE sont prêts pour les outils
        //     MANQUE Famille BE futurs (F-211/F-217+ — cohabitation légale, pacte successoral,
        //     kafala, DC).
        // Dossiers Famille FR : flags BE tous false (et inversement).
        JsonNode familleNode = root.path("famille_extracted_data");
        // === Flags FR (F-200) — 30 ===
        addBooleanFlagIfTrue(detected, familleNode, "divorce_consentement_mutuel_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "divorce_alteration_lien_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "divorce_faute_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "divorce_accepte_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "revision_post_divorce_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "ordonnance_protection_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "recompenses_envisagees");
        addBooleanFlagIfTrue(detected, familleNode, "regime_communaute_universelle_detecte");
        addBooleanFlagIfTrue(detected, familleNode, "partage_judiciaire_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "adoption_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "reconnaissance_paternelle_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "contestation_paternite_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "recherche_paternite_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "possession_etat_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "changement_residence_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "desaccord_parental_detecte");
        addBooleanFlagIfTrue(detected, familleNode, "pacs_dissolution_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "separation_corps_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "indivision_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "ordonnance_requete_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "succession_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "testament_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "donation_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "reserve_hereditaire_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "partage_successoral_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "indivision_successorale_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "rapport_succession_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "protection_majeur_envisagee");
        addBooleanFlagIfTrue(detected, familleNode, "changement_etat_civil_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "pma_gpa_envisagee");
        // === F-210 — 1 flag procédural Famille FR ===
        addBooleanFlagIfTrue(detected, familleNode, "mediation_familiale_pre_saisine_pertinente");
        // === SF-216-01 — 1 flag CONTEXTUAL Famille FR : prestation compensatoire ===
        // Lire depuis sous-objet `prestation_compensatoire_detection.envisagee` (pattern prompt SF-216-01).
        addBooleanFlagIfTrueNested(detected, familleNode, "prestation_compensatoire_detection", "envisagee",
                "prestation_compensatoire_envisagee");
        // === SF-216-05 — 1 flag CONTEXTUAL Famille FR : liquidation communauté légale ===
        // Lire depuis sous-objet `liquidation_communaute_detection.envisagee` (même pattern).
        addBooleanFlagIfTrueNested(detected, familleNode, "liquidation_communaute_detection", "envisagee",
                "liquidation_communaute_envisagee");
        // === SF-216-07 — 1 flag CONTEXTUAL Famille FR : ARIPA recouvrement pension impayée ===
        // Lire depuis sous-objet `aripa_recouvrement_detection.envisage` (même pattern, art. L. 581 CSS).
        addBooleanFlagIfTrueNested(detected, familleNode, "aripa_recouvrement_detection", "envisage",
                "aripa_recouvrement_envisage");
        // === SF-216-09 — 1 flag CONTEXTUAL Famille FR : délégation autorité parentale ===
        // Lire depuis sous-objet `delegation_ap_detection.envisagee` (même pattern, art. 376-1 Cciv).
        addBooleanFlagIfTrueNested(detected, familleNode, "delegation_ap_detection", "envisagee",
                "delegation_ap_envisagee");
        // === SF-216-11 — 1 flag CONTEXTUAL Famille FR : retrait autorité parentale ===
        // Lire depuis sous-objet `retrait_ap_detection.envisage` (même pattern, art. 378-381 Cciv + loi 2022 LMVSS).
        addBooleanFlagIfTrueNested(detected, familleNode, "retrait_ap_detection", "envisage",
                "retrait_ap_envisage");
        // === SF-216-15 — 1 flag CONTEXTUAL Famille FR : adoption intra-familiale ===
        // Lire depuis sous-objet `adoption_intra_detection.envisagee` (même pattern, art. 345-1 Cciv).
        addBooleanFlagIfTrueNested(detected, familleNode, "adoption_intra_detection", "envisagee",
                "adoption_intra_envisagee");
        // === SF-216-17 — 1 flag CONTEXTUAL Famille FR : adoption internationale ===
        // Lire depuis sous-objet `adoption_internationale_detection.envisagee` (même pattern,
        // art. 370-3 à 370-5 Cciv + Convention La Haye 1993).
        addBooleanFlagIfTrueNested(detected, familleNode, "adoption_internationale_detection", "envisagee",
                "adoption_internationale_envisagee");
        // === SF-216-13 — 1 flag CONTEXTUAL Famille FR : audition du mineur ===
        // Lire depuis sous-objet `audition_mineur_detection.envisagee` (même pattern,
        // art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC + CIDE art. 12).
        addBooleanFlagIfTrueNested(detected, familleNode, "audition_mineur_detection", "envisagee",
                "audition_mineur_envisagee");
        // === SF-216-19 — 1 flag CONTEXTUAL Famille FR : indignité successorale ===
        // Lire depuis sous-objet `indignite_successorale_detection.envisagee` (même pattern,
        // art. 726-729-1 Cciv + Loi n°2022-1617 du 23/12/2022 violences intrafamiliales).
        addBooleanFlagIfTrueNested(detected, familleNode, "indignite_successorale_detection", "envisagee",
                "indignite_successorale_envisagee");
        // === SF-216-21 — 1 flag CONTEXTUAL Famille FR : recel successoral ===
        // Lire depuis sous-objet `recel_succession_detection.envisage` (même pattern,
        // art. 778 Cciv + Cass. 1ère civ., 14/11/2012).
        addBooleanFlagIfTrueNested(detected, familleNode, "recel_succession_detection", "envisage",
                "recel_successoral_envisage");
        // === SF-216-23 — 1 flag CONTEXTUAL Famille FR : donation entre époux ===
        // Lire depuis sous-objet `donation_entre_epoux_detection.envisagee` (même pattern,
        // art. 1091-1100 Cciv + art. 265 al. 2 + art. 1527 al. 2 + art. 912-928).
        addBooleanFlagIfTrueNested(detected, familleNode, "donation_entre_epoux_detection", "envisagee",
                "donation_entre_epoux_envisagee");
        // === SF-216-27 — 1 flag CONTEXTUAL Famille FR : partage successoral notarié ===
        // Lire depuis sous-objet `partage_notarial_detection.envisage` (même pattern,
        // art. 816 et s. Cciv + art. 870 Cciv + art. 1592 CGI + art. 641 CGI + art. 840 Cciv).
        addBooleanFlagIfTrueNested(detected, familleNode, "partage_notarial_detection", "envisage",
                "partage_notarial_envisage");
        // === SF-216-25 — 1 flag CONTEXTUAL Famille FR : présomption de paternité ===
        // Lire depuis sous-objet `presomption_paternite_detection.envisagee` (même pattern,
        // art. 312-316 Cciv + art. 333 al. 1 + Cass. 1ère civ., 19/2/2014).
        addBooleanFlagIfTrueNested(detected, familleNode, "presomption_paternite_detection", "envisagee",
                "presomption_paternite_envisagee");
        // === SF-216-29 — 1 flag CONTEXTUAL Famille FR : donation-partage ===
        // Lire depuis sous-objet `donation_partage_detection.envisagee` (même pattern,
        // art. 1075 à 1075-5 Cciv + art. 1078, 1078-1, 1080 + art. 912-928).
        addBooleanFlagIfTrueNested(detected, familleNode, "donation_partage_detection", "envisagee",
                "donation_partage_envisagee");
        // === SF-216-03 — 1 flag CONTEXTUAL Famille FR : pension alimentaire enfant ===
        // Lire depuis sous-objet `pension_alimentaire_detection.envisagee` (même pattern).
        addBooleanFlagIfTrueNested(detected, familleNode, "pension_alimentaire_detection", "envisagee",
                "pension_alimentaire_envisagee");
        // === SF-222-01 — 1 flag CONTEXTUAL Famille FR : ASF allocation de soutien familial ===
        // Lire depuis sous-objet `asf_caf_detection.detecte` (même pattern, art. L. 523-1 CSS).
        addBooleanFlagIfTrueNested(detected, familleNode, "asf_caf_detection", "detecte",
                "asf_caf_detecte");
        // === SF-222-02 — 1 flag CONTEXTUAL Famille FR : TGD téléphone grave danger éligibilité ===
        // Lire depuis sous-objet `tgd_detection.detecte` (même pattern, art. 41-3-1 CPP).
        addBooleanFlagIfTrueNested(detected, familleNode, "tgd_detection", "detecte",
                "tgd_detecte");
        // === SF-222-03 — 1 flag CONTEXTUAL Famille FR : habilitation familiale ===
        // Lire depuis sous-objet `habilitation_familiale_detection.detecte` (même pattern, art. 494-1 et s. Cciv).
        addBooleanFlagIfTrueNested(detected, familleNode, "habilitation_familiale_detection", "detecte",
                "habilitation_familiale_detectee");
        // === SF-222-04 — 1 flag CONTEXTUAL Famille FR : assistance éducative (mineur en danger) ===
        // Lire depuis sous-objet `assistance_educative_detection.detecte` (même pattern, art. 375 et s. Cciv).
        addBooleanFlagIfTrueNested(detected, familleNode, "assistance_educative_detection", "detecte",
                "assistance_educative_detectee");
        // === Flags BE (F-202) — 5 ===
        addBooleanFlagIfTrue(detected, familleNode, "divorce_dc_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "divorce_ddi_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "cohabitation_legale_be_detectee");
        addBooleanFlagIfTrue(detected, familleNode, "pacte_successoral_envisage");
        addBooleanFlagIfTrue(detected, familleNode, "kafala_recueil_detecte");
        return detected;
    }

    /**
     * F-197 SF-197-01 — propage l'override {@code type_litige_avocat_override} vers les
     * trigger_field réellement utilisés par les règles de visibilité Travail FR
     * (cf. migrations 193 + 199), pour que les outils {@code F-DT-11/12/13/20/21/24/...}
     * s'activent quand l'avocat surcharge la classification IA.
     *
     * <p>Mapping :
     * <ul>
     *   <li>LICENCIEMENT_SANS_CAUSE_REELLE → {@code type_rupture=LICENCIEMENT}</li>
     *   <li>LICENCIEMENT_ECONOMIQUE → {@code type_rupture=LICENCIEMENT_ECONOMIQUE}</li>
     *   <li>PRISE_ACTE_RUPTURE → {@code type_rupture=PRISE_ACTE}</li>
     *   <li>HARCELEMENT_MORAL → {@code motif_nullite_pressenti=HARCELEMENT_MORAL}</li>
     *   <li>DISCRIMINATION → {@code motif_nullite_pressenti=DISCRIMINATION}</li>
     *   <li>HEURES_SUPPLEMENTAIRES → {@code heures_sup_mentionnees=PRESENT}</li>
     *   <li>RAPPEL_SALAIRE → {@code rappel_salaire_detecte=true}</li>
     * </ul>
     */
    private static void propagateTypeLitigeOverrideTriggers(Map<String, Set<String>> detected,
                                                             String typeLitigeOverride) {
        switch (typeLitigeOverride) {
            case "LICENCIEMENT_SANS_CAUSE_REELLE" -> addIfPresent(detected, "type_rupture", "LICENCIEMENT");
            case "LICENCIEMENT_ECONOMIQUE" -> addIfPresent(detected, "type_rupture", "LICENCIEMENT_ECONOMIQUE");
            case "PRISE_ACTE_RUPTURE" -> addIfPresent(detected, "type_rupture", "PRISE_ACTE");
            case "HARCELEMENT_MORAL" -> addIfPresent(detected, "motif_nullite_pressenti", "HARCELEMENT_MORAL");
            case "DISCRIMINATION" -> addIfPresent(detected, "motif_nullite_pressenti", "DISCRIMINATION");
            case "HEURES_SUPPLEMENTAIRES" -> addIfPresent(detected, "heures_sup_mentionnees", "PRESENT");
            case "RAPPEL_SALAIRE" -> addIfPresent(detected, "rappel_salaire_detecte", "true");
            default -> { /* no-op */ }
        }
    }

    /**
     * F-166 SF-166-02 : émet la string "true" dans la map detected[field] quand le node JSON
     * contient le champ avec la valeur boolean true (ou string "true"). Skip silencieux si le
     * champ est absent, null, false, ou non-boolean — comportement aligné sur le helper
     * {@code booleanOrFalse} côté CaseAnalysisResponse (SF-166-01).
     */
    private static void addBooleanFlagIfTrue(Map<String, Set<String>> detected, JsonNode parent, String field) {
        if (parent == null || !parent.has(field)) return;
        JsonNode v = parent.get(field);
        if (v == null || v.isNull()) return;
        boolean isTrue;
        if (v.isBoolean()) {
            isTrue = v.asBoolean();
        } else if (v.isTextual()) {
            isTrue = "true".equalsIgnoreCase(v.asText());
        } else {
            isTrue = false;
        }
        if (isTrue) {
            addIfPresent(detected, field, "true");
        }
    }

    /**
     * SF-216-01 : variante de {@link #addBooleanFlagIfTrue} pour un flag boolean
     * stocké dans un sous-objet JSON imbriqué ({@code parent → nestedObject → nestedField}).
     * La clé insérée dans {@code detected} est {@code triggerKey} (nom du champ dans
     * {@code decision_tool_visibility_rules.trigger_field}).
     * Skip silencieux si le sous-objet ou le champ est absent / null / non-booléen.
     */
    private static void addBooleanFlagIfTrueNested(Map<String, Set<String>> detected,
                                                    JsonNode parent,
                                                    String nestedObject,
                                                    String nestedField,
                                                    String triggerKey) {
        if (parent == null || parent.isMissingNode()) return;
        JsonNode sub = parent.path(nestedObject);
        if (sub == null || sub.isMissingNode() || sub.isNull() || !sub.isObject()) return;
        JsonNode v = sub.path(nestedField);
        if (v == null || v.isMissingNode() || v.isNull()) return;
        boolean isTrue;
        if (v.isBoolean()) {
            isTrue = v.asBoolean();
        } else if (v.isTextual()) {
            isTrue = "true".equalsIgnoreCase(v.asText());
        } else {
            isTrue = false;
        }
        if (isTrue) {
            addIfPresent(detected, triggerKey, "true");
        }
    }

    /**
     * F-235 : normalise une nationalité texte libre en titlecase (1ʳᵉ majuscule,
     * reste minuscule). Préserve les accents. Exemples :
     * <ul>
     *   <li>{@code "algerienne"} → {@code "Algerienne"}</li>
     *   <li>{@code "ALGÉRIENNE"} → {@code "Algérienne"}</li>
     *   <li>{@code "Algérienne"} → {@code "Algérienne"} (idempotent)</li>
     *   <li>{@code "  tunisienne  "} → {@code "Tunisienne"}</li>
     *   <li>{@code null} ou blank → {@code null}</li>
     * </ul>
     * <p>Note : la normalisation des accents (ex. "algerienne" → "Algérienne")
     * n'est pas effectuée — l'IA est instruite via le prompt d'utiliser les
     * adjectifs avec accent natifs ("Algérienne"). Ce helper assure uniquement
     * la cohérence de capitalisation pour le matching CONTEXTUAL.</p>
     */
    static String normalizeTitleCase(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        // 1ʳᵉ lettre en majuscule (locale FR pour gérer correctement les accents),
        // reste en minuscule.
        return Character.toUpperCase(trimmed.charAt(0))
                + trimmed.substring(1).toLowerCase(java.util.Locale.FRENCH);
    }

    private static String readString(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String s = node.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static void addIfPresent(Map<String, Set<String>> map, String field, String value) {
        if (value == null) {
            return;
        }
        map.computeIfAbsent(field, k -> new HashSet<>()).add(value);
    }

    /**
     * SF-214-11 : flag pivot DÉRIVÉ {@code aes_calcul_presence_declenche} pour l'outil
     * F-IM-30 AES calcul présence prouvée (FR). L'outil étant transversal aux 4 voies
     * AES, il se déclenche dès qu'au moins un des 4 flags AES existants (F-201) est true :
     * {@code aes_metiers_tension_eligible_detecte}, {@code aes_familial_eligible_detecte},
     * {@code aes_humanitaire_eligible_detecte}, {@code aes_etudiant_eligible_detecte}.
     * Le flag n'est PAS extrait par le pipeline IA : il est calculé ici par OR.
     */
    private static void addDerivedAesPresenceDeclenche(Map<String, Set<String>> detected, JsonNode parent) {
        boolean any = readBooleanFlag(parent, "aes_metiers_tension_eligible_detecte")
                || readBooleanFlag(parent, "aes_familial_eligible_detecte")
                || readBooleanFlag(parent, "aes_humanitaire_eligible_detecte")
                || readBooleanFlag(parent, "aes_etudiant_eligible_detecte");
        if (any) {
            addIfPresent(detected, "aes_calcul_presence_declenche", "true");
        }
    }

    /** Lit un flag booléen tolérant (boolean natif ou texte "true"). false si absent/null. */
    private static boolean readBooleanFlag(JsonNode parent, String field) {
        if (parent == null || !parent.has(field)) return false;
        JsonNode v = parent.get(field);
        if (v == null || v.isNull()) return false;
        if (v.isBoolean()) return v.asBoolean();
        if (v.isTextual()) return "true".equalsIgnoreCase(v.asText());
        return false;
    }

    private VisibleToolSetResponse buildResponse(List<DecisionToolVisibilityRule> rules,
                                                 Map<String, Set<String>> detected) {
        Comparator<DecisionToolVisibilityRule> byPriorityThenId =
                Comparator.comparingInt(DecisionToolVisibilityRule::getPriority)
                        .thenComparing(DecisionToolVisibilityRule::getToolId);

        Set<String> alwaysOn = new LinkedHashSet<>();
        rules.stream()
                .filter(r -> r.getLayer() == DecisionToolVisibilityRule.Layer.ALWAYS_ON)
                .sorted(byPriorityThenId)
                .forEach(r -> alwaysOn.add(r.getToolId()));

        Set<String> contextual = new LinkedHashSet<>();
        rules.stream()
                .filter(r -> r.getLayer() == DecisionToolVisibilityRule.Layer.CONTEXTUAL)
                .filter(r -> {
                    Set<String> values = detected.get(r.getTriggerField());
                    return values != null && values.contains(r.getTriggerValue());
                })
                .sorted(byPriorityThenId)
                .forEach(r -> contextual.add(r.getToolId()));

        Set<String> allContextualTools = new LinkedHashSet<>();
        rules.stream()
                .filter(r -> r.getLayer() == DecisionToolVisibilityRule.Layer.CONTEXTUAL)
                .map(DecisionToolVisibilityRule::getToolId)
                .sorted()
                .forEach(allContextualTools::add);

        List<String> catalog = new ArrayList<>();
        for (String toolId : allContextualTools) {
            if (!contextual.contains(toolId)) {
                catalog.add(toolId);
            }
        }

        return new VisibleToolSetResponse(
                new ArrayList<>(alwaysOn),
                new ArrayList<>(contextual),
                catalog);
    }
}
