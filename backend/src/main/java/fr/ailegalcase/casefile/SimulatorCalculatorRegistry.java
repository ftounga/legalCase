package fr.ailegalcase.casefile;

import fr.ailegalcase.referential.LegalReferentialService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * SF-163-03 : registry des calculators stateless exposés via le dispatcher
 * {@code POST /api/v1/simulators/{toolId}/calculate}.
 *
 * <p>Chaque entrée associe un {@code toolId} (présent dans le frontend
 * {@code TOOL_REGISTRY} + dans la table {@code decision_tool_visibility_rules})
 * à un {@link SimulatorCalculatorDescriptor} qui invoque le calculator pur
 * <b>sans persistance</b>.</p>
 *
 * <p><b>Retour du handler</b> : pour V1, chaque handler retourne le
 * {@code Result} record produit par le calculator (pas le {@code Response}
 * record du endpoint case-file scoped). Ces deux types portent les mêmes
 * champs sémantiques (verdict, scores, fourchettes, formule, baseJuridique,
 * messages, etc.) ; le {@code Response} case-file ajoute juste
 * {@code caseFileId} (null en mode simulateur) et parfois ré-expose les
 * champs du request (que le frontend a déjà puisque il les a envoyés). Le
 * composant frontend en mode simulateur (SF-163-02a) lit les mêmes champs ;
 * la compatibilité sémantique est donc préservée.</p>
 *
 * <h2>Exclusions V1 (404 si appelés)</h2>
 *
 * <ul>
 *   <li><b>Calculators PDF / OCR</b> : {@code F-DT-04-fiche-prudhomale},
 *       {@code F-DT-06-requete-tribunal-travail} — leur output est un PDF, pas
 *       un calcul.</li>
 *   <li><b>Wrappers info-only</b> ({@code PREFILL_COUNT_ALWAYS_ZERO=true}) :
 *       {@code F-FA-01-prestation-compensatoire},
 *       {@code F-FA-02-pension-alimentaire},
 *       {@code F-FA-04-liquidation-communaute},
 *       {@code F-DT-07-anciennete-conges-prime} (lit la convention collective
 *       du dossier),
 *       {@code F-IM-01-checklist-pieces},
 *       {@code F-FA-06-calendrier-garde}, {@code F-FA-07-checklist-divorce},
 *       {@code F-DT-03-prescription-litige},
 *       {@code F-DT-03-mediation-familiale-pre-saisine}.</li>
 *   <li><b>Outils flux/procédure</b> : checklists et timeline pures
 *       ({@code immigration-checklist}, {@code divorce-checklist}, etc.).</li>
 *   <li><b>Calculators avec dépendances cross-analysis</b> : aucun cas
 *       identifié dans l'audit 2026-05-11.</li>
 *   <li><b>Calculators avec signatures complexes</b> non couverts V1 (backlog
 *       SF-163-03b) : {@code F-DT-09-comparateur-indemnites} (lit barème
 *       Macron + CCT), {@code F-DT-20-rappel-salaire} (lit prime
 *       convention), {@code F-DT-25-indemnite-preavis} (lit convention),
 *       {@code F-DT-32-documents-fin-contrat} (snapshot état civil),
 *       {@code F-FA-12-mesures-provisoires}, {@code F-FA-16-communaute-universelle},
 *       {@code F-FA-17-partage-judiciaire},
 *       {@code F-FA-18-*} (reconnaissance/contestation paternité, recherche
 *       paternité, possession état, adoption), {@code F-FA-20-pacs-dissolution},
 *       {@code F-FA-22-indivision}, {@code F-FA-23-ordonnance-requete},
 *       {@code F-FA-24-*} (dévolution, testament, donation, partage
 *       successoral, indivision successorale), {@code F-FA-25-majeurs-proteges},
 *       {@code F-FA-27-pma-gpa}. Signatures à harmoniser dans une SF
 *       ultérieure.</li>
 * </ul>
 */
@Component
public class SimulatorCalculatorRegistry {

    private static final Logger log = LoggerFactory.getLogger(SimulatorCalculatorRegistry.class);

    private final LegalReferentialService referentialService;

    private final Map<String, SimulatorCalculatorDescriptor<?>> descriptors = new HashMap<>();

    public SimulatorCalculatorRegistry(LegalReferentialService referentialService) {
        this.referentialService = referentialService;
    }

    @PostConstruct
    void init() {
        // ════════════════════════════════════════════════════════════════════
        // Travail FR — validités
        // ════════════════════════════════════════════════════════════════════

        register("F-DT-08-licenciement-validity", LicenciementRequest.class, (req, ctxCountry) -> {
            String c = pickCountry(req.country(), ctxCountry);
            if (c == null || !LicenciementAnalyzer.isCountryValid(c)) {
                throw new IllegalArgumentException("Pays non supporté : " + c);
            }
            List<LicenciementCritere> criteres = referentialService.getLicenciementCriteres(c);
            if (criteres.isEmpty()) {
                throw new IllegalArgumentException("Référentiel LICENCIEMENT_CRITERES indisponible pour " + c);
            }
            return LicenciementAnalyzer.analyze(c,
                    req.reponses() != null ? req.reponses() : Map.of(),
                    criteres);
        });

        register("F-DT-10-rupture-conv-validity", RuptureConvRequest.class, (req, ctxCountry) -> {
            String c = pickCountry(req.country(), ctxCountry);
            if (c == null || !RuptureConvAnalyzer.isCountryValid(c)) {
                throw new IllegalArgumentException("Pays non supporté : " + c);
            }
            String normalized = c.toUpperCase();
            List<RuptureConvCritere> criteres = referentialService.getRuptureConvCriteres(normalized);
            if (criteres.isEmpty()) {
                throw new IllegalArgumentException("Référentiel RUPTURE_CONV_CRITERES indisponible pour " + normalized);
            }
            return RuptureConvAnalyzer.analyze(normalized,
                    req.reponses() != null ? req.reponses() : Map.of(),
                    criteres);
        });

        register("F-DT-11-harcelement-licenciement-nul", HarcelementNulliteRequest.class,
                (req, country) -> HarcelementNulliteCalculator.compute(
                        req.salaireMensuelReference(),
                        req.motifNullite(),
                        country));

        register("F-DT-12-discrimination-dommages-interets", DiscriminationRequest.class,
                (req, country) -> DiscriminationCalculator.compute(
                        req.salaireMensuelReference(),
                        req.motifDiscrimination(),
                        req.contexteActe(),
                        country));

        register("F-DT-13-licenciement-economique", LicenciementEconomiqueRequest.class,
                (req, country) -> LicenciementEconomiqueCalculator.compute(
                        req.motifEconomiqueInvoque(),
                        nullSafeList(req.preuvesMotif()),
                        req.criteresOrdreAppliques(),
                        req.salarieAge() != null ? req.salarieAge() : 0,
                        req.salarieAncienneteMois() != null ? req.salarieAncienneteMois() : 0,
                        req.salarieChargesFamille() != null ? req.salarieChargesFamille() : 0,
                        req.salarieQualitesProf(),
                        req.tentativesReclassement(),
                        Boolean.TRUE.equals(req.prioriteReembauchePropose()),
                        Boolean.TRUE.equals(req.congeReclassementPropose()),
                        req.dateNotification(),
                        country));

        register("F-DT-15-inaptitude", InaptitudeRequest.class,
                (req, country) -> InaptitudeCalculator.compute(
                        req.salaireMensuelReference(),
                        req.ancienneteAnnees(),
                        req.origineInaptitude(),
                        req.reclassementRespecte(),
                        req.avisMedecinTravailDate(),
                        country));

        register("F-DT-16-licenciement-nul-detection", LicenciementNulDetectionRequest.class,
                (req, country) -> LicenciementNulDetectionCalculator.compute(req.toInput(), country));

        register("F-DT-17-indemnite-precarite-cdd", IndemnitePrecariteCddRequest.class,
                (req, country) -> IndemnitePrecariteCddCalculator.compute(
                        req.totalSalairesBruts(),
                        // taux légal 10% par défaut (les conventions plus avantageuses
                        // sont gérées en mode case-file via LegalReferentialService —
                        // hors-périmètre V1 simulateur).
                        10,
                        req.casExclusion()));

        register("F-DT-18-fin-mission-interim", IndemniteFinMissionInterimRequest.class,
                (req, country) -> IndemniteFinMissionInterimCalculator.compute(
                        req.totalRemunerationsBrutesEur(),
                        req.dureeMissionJours(),
                        req.motifExclusion(),
                        req.dateFinMission()));

        register("F-DT-19-heures-sup", HeuresSupRequest.class,
                (req, country) -> HeuresSupCalculator.compute(req, country));

        register("F-DT-21-travail-dissimule", TravailDissimuleRequest.class,
                (req, country) -> IndemniteTravailDissimuleCalculator.compute(req.salaireMensuelReference()));

        register("F-DT-22-requalification-cdd-cdi", RequalificationCddCdiRequest.class,
                (req, country) -> RequalificationCddCdiCalculator.compute(req));

        register("F-DT-23-requalification-interim-cdi", RequalificationInterimCdiRequest.class,
                (req, country) -> RequalificationInterimCdiCalculator.compute(req));

        register("F-DT-24-non-concurrence", NonConcurrenceRequest.class,
                (req, country) -> NonConcurrenceCalculator.compute(req.toInput(), country));

        register("F-DT-26-conges-payes-indemnite", IndemniteCongesPayesRequest.class,
                (req, country) -> IndemniteCongesPayesCalculator.compute(
                        req.totalRemunerationPeriodeEur(),
                        req.joursAcquisAnnee(),
                        req.joursPris(),
                        req.salaireMensuelBrutEur(),
                        req.dateRupture(),
                        req.methodeForcee()));

        register("F-DT-30-protection-rp", ProtectionRpRequest.class,
                (req, country) -> ProtectionRpCalculator.compute(
                        req.statutProtege(),
                        req.dateExpirationMandat(),
                        req.datePresumeeRupture(),
                        req.procedureSuivie(),
                        req.motifLicenciement(),
                        req.salaireMensuelBrutEur(),
                        country));

        register("F-DT-31-transaction", TransactionRequest.class,
                (req, country) -> TransactionCalculator.compute(req.toInput(), country));

        register("F-DT-33-at-mp", AtMpRequest.class,
                (req, country) -> AtMpCalculator.compute(
                        req.dispositif(),
                        req.dateAccident(),
                        req.lieuTravail(),
                        req.declarationEmployeurDansLes48h(),
                        req.certificatMedicalInitial(),
                        req.numeroTableau(),
                        req.delaiPriseEnChargeRespecte(),
                        req.dateExposition(),
                        req.tauxFixeParCpam(),
                        req.tauxRevendique(),
                        req.expertiseMedicaleProduite(),
                        req.datePremierAvisCpam(),
                        LocalDate.now()));

        register("F-DT-34-refere-prudhomal", ReferePrudhomalRequest.class,
                (req, country) -> ReferePrudhomalCalculator.compute(
                        req.typeRefere(),
                        req.natureCreance(),
                        req.montantProvisionDemandeeEur(),
                        Boolean.TRUE.equals(req.absenceContestationSerieuse()),
                        req.preuvesUrgenceProduites(),
                        Boolean.TRUE.equals(req.dommageImmediatCarac()),
                        Boolean.TRUE.equals(req.tresorerieEmployeurDouteuse()),
                        req.dateMiseEnDemeure(),
                        req.ancienneteContratMois()));

        register("F-DT-35-contestation-are-fr", ContestationAreRequest.class,
                (req, country) -> ContestationAreCalculator.compute(
                        req.typeDecisionContestee(),
                        req.motifContestation(),
                        req.dateNotificationDecision(),
                        req.dateRecoursHierarchiquePropose(),
                        req.preuvesProduites() != null ? req.preuvesProduites() : Collections.emptyList(),
                        req.montantContesteEur(),
                        req.demandeurDejaSaisiTribunal(),
                        req.delaiContestationRespecte()));

        // ════════════════════════════════════════════════════════════════════
        // Travail BE
        // ════════════════════════════════════════════════════════════════════

        register("F-DT-27-motif-grave-be", MotifGraveBeRequest.class,
                (req, country) -> MotifGraveBeCalculator.compute(
                        req.dateConnaissanceFait(),
                        req.dateNotificationRupture(),
                        req.dateNotificationMotifs(),
                        req.anciennetteAnnees(),
                        req.salaireMensuelReference()));

        register("F-DT-28-avantages-conventionnels-be", AvantagesConventionnelsBeRequest.class,
                (req, country) -> AvantagesConventionnelsBeCalculator.compute(
                        req.salaireMensuelBrutEur(),
                        req.joursTravaillesAnneePrecedente(),
                        req.anciennetteMois(),
                        req.commissionParitaire(),
                        req.annee(),
                        Boolean.TRUE.equals(req.doublePeculeVacancesPercu()),
                        Boolean.TRUE.equals(req.primeFinAnneePrevueCcCp()),
                        Boolean.TRUE.equals(req.ecoChequesPrevuCcCp()),
                        Boolean.TRUE.equals(req.ecoChequesUtilisationDansAn()),
                        Boolean.TRUE.equals(req.chequesRepasPrevu()),
                        req.joursPrestesEffectifs()));

        register("F-DT-29-credit-temps-be", CreditTempsBeRequest.class,
                (req, country) -> CreditTempsBeCalculator.compute(
                        req.regime(),
                        req.motif(),
                        req.ancienneteEntrepriseMois(),
                        req.tailleEntrepriseEtp(),
                        req.dureeReductionType(),
                        req.ageDemandeurAnnees(),
                        req.dateDemande()));

        // ════════════════════════════════════════════════════════════════════
        // Immigration FR
        // ════════════════════════════════════════════════════════════════════

        register("F-IM-08-oqtf-avec-delai-fr", OqtfAvecDelaiRequest.class,
                (req, country) -> OqtfAvecDelaiCalculator.compute(
                        req.dateNotificationOqtf(),
                        req.motifOqtf(),
                        Boolean.TRUE.equals(req.recoursForme()),
                        req.dateRecours()));

        register("F-IM-08-oqtf-sans-delai-fr", OqtfSansDelaiRequest.class,
                (req, country) -> OqtfSansDelaiCalculator.compute(
                        req.dateHeureNotificationOqtf(),
                        req.motifSansDelai(),
                        Boolean.TRUE.equals(req.placementCra()),
                        Boolean.TRUE.equals(req.recoursForme()),
                        req.dateHeureRecours()));

        register("F-IM-08-referes-admin-fr", ReferesAdminRequest.class,
                (req, country) -> ReferesAdminCalculator.compute(
                        req.typeRefere(),
                        req.decisionContestee(),
                        req.dateNotificationDecision(),
                        Boolean.TRUE.equals(req.urgenceCaracterisee()),
                        Boolean.TRUE.equals(req.atteinteLiberteFondamentale()),
                        Boolean.TRUE.equals(req.doutesSerieuxLegalite()),
                        req.preuvesUrgence(),
                        Boolean.TRUE.equals(req.demandeurDejaPrived())));

        register("F-IM-09-aes-metiers-tension", AesMetiersTensionRequest.class,
                (req, country) -> AesMetiersTensionCalculator.compute(
                        req.dateEntreeFrance(),
                        req.moisActiviteSalarieeDernieres24Mois(),
                        Boolean.TRUE.equals(req.metierEstEnTension()),
                        req.codeMetier(),
                        Boolean.TRUE.equals(req.menaceOrdrePublic()),
                        Boolean.TRUE.equals(req.contratOuPromesseValide()),
                        req.dateDepotDemande()));

        register("F-IM-09-aes-famille", AesFamilleRequest.class,
                (req, country) -> AesFamilleCalculator.compute(
                        req.dateEntreeFrance(),
                        req.dureePresenceMois(),
                        Boolean.TRUE.equals(req.conjointFrancaisOuRegulier()),
                        req.enfantsScolarisesFrance(),
                        req.dureeScolaritePlusAncienEnfantAnnees(),
                        req.preuvesInsertion(),
                        Boolean.TRUE.equals(req.menaceOrdrePublic()),
                        req.dateDepotDemande()));

        register("F-IM-09-aes-etudiant", AesEtudiantRequest.class,
                (req, country) -> AesEtudiantCalculator.compute(
                        req.dateEntreeFrance(),
                        req.dureePresenceMois(),
                        req.anneesScolariteEnFranceConsecutives(),
                        req.niveauEtudesActuel(),
                        req.resultatsAcademiques(),
                        Boolean.TRUE.equals(req.inscriptionEtablissementReconnu()),
                        Boolean.TRUE.equals(req.moyensSubsistance()),
                        Boolean.TRUE.equals(req.menaceOrdrePublic()),
                        Boolean.TRUE.equals(req.parcoursCoherent()),
                        req.dateDepotDemande()));

        register("F-IM-09-aes-humanitaire", AesHumanitaireRequest.class,
                (req, country) -> AesHumanitaireCalculator.compute(
                        req.dateEntreeFrance(),
                        req.motifHumanitaireDominant(),
                        req.preuvesMedicales(),
                        req.preuvesViolencesOuTraite(),
                        Boolean.TRUE.equals(req.demandeAsileDeposeeEtRejetee()),
                        Boolean.TRUE.equals(req.commissionTitreSejourSaisie()),
                        Boolean.TRUE.equals(req.menaceOrdrePublic()),
                        req.dateDepotDemande()));

        register("F-IM-11-changement-statut", ChangementStatutRequest.class,
                (req, country) -> ChangementStatutCalculator.compute(
                        req.titreActuel(),
                        req.titreEnvisage(),
                        req.dureeRestanteSurTitreActuelMois(),
                        Boolean.TRUE.equals(req.documentJustificatifFourni()),
                        req.remunerationContratEur(),
                        Boolean.TRUE.equals(req.casierJudiciaireVierge())));

        register("F-IM-12-asile-avance", AsileAvanceRequest.class,
                (req, country) -> AsileAvanceCalculator.compute(
                        req.dispositifAsile(),
                        req.dateDecisionAnterieure(),
                        req.elementsNouveaux(),
                        Boolean.TRUE.equals(req.paysOrigineDansListeSurs()),
                        Boolean.TRUE.equals(req.empreintesEurodacAutresEm()),
                        Boolean.TRUE.equals(req.demandeurEnFuite()),
                        req.motifsExclusion(),
                        Boolean.TRUE.equals(req.traitementsGravesEtablis()),
                        Boolean.TRUE.equals(req.fraudeDocumentaireAvere()),
                        Boolean.TRUE.equals(req.refusPriseEmpreintes()),
                        Boolean.TRUE.equals(req.presenceReguliere())));

        register("F-IM-13-naturalisation", NaturalisationRequest.class,
                (req, country) -> NaturalisationCalculator.compute(
                        req.voieNaturalisation(),
                        req.dureeResidenceReguliereAnnees(),
                        req.dureeMariageAnnees(),
                        req.cohabitationContinue(),
                        req.ageDemandeur(),
                        req.ascendantDirectFrancais(),
                        req.parentAcquiertNationalite(),
                        req.vitAvecParentAcquereur(),
                        req.ancienFrancais(),
                        !Boolean.FALSE.equals(req.casierJudiciaireVierge()),
                        req.assimilationLangueB1(),
                        req.ressourcesStables(),
                        Boolean.TRUE.equals(req.oppositionGouvernementaleActive()),
                        req.etudesSuperieuresFrance()));

        register("F-IM-17-regime-algerien", RegimeAlgerienRequest.class,
                (req, country) -> RegimeAlgerienCalculator.compute(
                        req.voieDemande(),
                        req.documentEtatCivilOriginal(),
                        req.presenceReguliereFranceMois(),
                        Boolean.TRUE.equals(req.casierJudiciaireVierge()),
                        req.visaLongSejourValide(),
                        req.conjointFrancais(),
                        req.parentEnfantFrancais(),
                        req.neEnFrance(),
                        req.arriveeAvant13Ans(),
                        req.contratTravailValide(),
                        req.ressourcesSuffisantes(),
                        req.logementDecent(),
                        req.nombrePersonnesFoyer()));

        register("F-IM-19-mineurs", MineursImmigrationRequest.class,
                (req, country) -> MineursImmigrationCalculator.compute(
                        req.dispositifVise(),
                        req.dateNaissance(),
                        req.dateEntreeFrance(),
                        Boolean.TRUE.equals(req.parentRegulier()),
                        Boolean.TRUE.equals(req.isolementAvere()),
                        Boolean.TRUE.equals(req.motifOrdrePublic()),
                        req.nationalite()));

        register("F-IM-20-mesures-eloignement", MesuresEloignementRequest.class,
                (req, country) -> MesuresEloignementCalculator.compute(
                        req.dispositif(),
                        req.motifMenace(),
                        req.procedureCommissionRespectee(),
                        req.urgenceAbsolueJustifiee(),
                        req.dureeCircularitePrecaire(),
                        req.dureePresenceIrreguliereMois(),
                        req.comportementAggravant(),
                        req.recoursDelai(),
                        LocalDate.now()));

        register("F-IM-21-jld-retention-fr", JldRetentionRequest.class,
                (req, country) -> JldRetentionCalculator.compute(
                        req.dateNotificationPlacement(),
                        req.motifPlacement(),
                        Boolean.TRUE.equals(req.recoursForme()),
                        req.dateRecours()));

        register("F-IM-22-dublin-recours-fr", DublinRecoursRequest.class,
                (req, country) -> DublinRecoursCalculator.compute(
                        req.dateNotificationDecisionTransfert(),
                        req.etatMembreResponsable(),
                        req.motifTransfert(),
                        Boolean.TRUE.equals(req.recoursForme()),
                        req.dateRecours()));

        register("F-IM-23-crrv-refus-visa-fr", CrrvRefusVisaRequest.class,
                (req, country) -> CrrvRefusVisaCalculator.compute(
                        req.dateNotificationRefus(),
                        req.typeVisa(),
                        req.motifRefus(),
                        Boolean.TRUE.equals(req.recoursForme()),
                        req.dateRecours()));

        register("F-IM-24-victime-violences-l4256-fr", VictimeViolencesL4256Request.class,
                (req, country) -> VictimeViolencesL4256Analyzer.analyze(
                        req.dateOrdonnanceProtection(),
                        req.juridiction(),
                        req.dureeProtectionMois() != null ? req.dureeProtectionMois() : 0,
                        req.dateExpirationProtection(),
                        req.enfantsAcharge() != null ? req.enfantsAcharge() : 0,
                        req.nationalite()));

        // ════════════════════════════════════════════════════════════════════
        // Immigration BE
        // ════════════════════════════════════════════════════════════════════

        register("F-IM-08-annexe13-be", Annexe13BeRequest.class,
                (req, country) -> Annexe13BeCalculator.compute(
                        req.dateNotificationAnnexe13(),
                        req.delaiDepartImposeJours(),
                        req.motifOqt(),
                        Boolean.TRUE.equals(req.transfertImminent()),
                        Boolean.TRUE.equals(req.recoursForme()),
                        req.dateRecours(),
                        req.typeRecours()));

        register("F-IM-14-9bis-humanitaire-be", Belgian9bisRequest.class,
                (req, country) -> Belgian9bisCalculator.compute(
                        req.dateEntreeBelgique(),
                        req.dureePresenceMois(),
                        Boolean.TRUE.equals(req.circonstancesExceptionnelles()),
                        Boolean.TRUE.equals(req.liensFamiliauxBe()),
                        Boolean.TRUE.equals(req.liensProfessionnels()),
                        Boolean.TRUE.equals(req.scolariteEnfantsBe()),
                        Boolean.TRUE.equals(req.menaceOrdrePublic()),
                        req.dateDepotDemande()));

        register("F-IM-14-9ter-medical-be", Belgian9terRequest.class,
                (req, country) -> Belgian9terCalculator.compute(
                        req.dateDebutSymptomes(),
                        Boolean.TRUE.equals(req.maladieGraveCertifiee()),
                        Boolean.TRUE.equals(req.soinsNecessairesDisponiblesBe()),
                        Boolean.TRUE.equals(req.soinsInaccessiblesPaysOrigine()),
                        Boolean.TRUE.equals(req.menaceOrdrePublic()),
                        req.dateDepotDemande()));

        register("F-IM-14-40bis-cohabitant-ue-be", Belgian40bisRequest.class,
                (req, country) -> Belgian40bisCalculator.compute(
                        req.lienFamilial(),
                        req.regroupantCitoyenUe(),
                        req.regroupantActiviteCategorie(),
                        req.ressourcesSuffisantes(),
                        req.assuranceMaladieUe(),
                        req.logementSuffisant(),
                        req.menaceOrdrePublic(),
                        req.dateDepotDemande()));

        register("F-IM-14-40ter-familial-belge-be", Belgian40terRequest.class,
                (req, country) -> Belgian40terCalculator.compute(
                        req.lienFamilial(),
                        Boolean.TRUE.equals(req.regroupantBelge()),
                        req.revenusMensuelsNetsEur(),
                        req.seuil120PctRisEur(),
                        Boolean.TRUE.equals(req.assuranceMaladie()),
                        Boolean.TRUE.equals(req.logementSuffisant()),
                        Boolean.TRUE.equals(req.menaceOrdrePublic()),
                        req.dateDepotDemande()));

        // ════════════════════════════════════════════════════════════════════
        // Famille FR
        // ════════════════════════════════════════════════════════════════════

        register("F-FA-05-partage-immobilier", PartageImmobilierRequest.class,
                (req, ctxCountry) -> PartageImmobilierCalculator.calculate(
                        pickCountry(req.country(), ctxCountry),
                        req.valeurVenale(),
                        req.capitalRestantDu(),
                        req.quotePartAttributaire(),
                        req.isDivorce()));

        register("F-FA-08-divorce-alteration", DivorceAlterationRequest.class,
                (req, country) -> DivorceAlterationCalculator.compute(
                        req.dateCessationVieCommune(),
                        Boolean.TRUE.equals(req.preuvesSeparationDocumentaires()),
                        Boolean.TRUE.equals(req.tentativesReconciliation()),
                        req.dureeMariageAnnees(),
                        req.revenusAnnuelsEpoux1Eur(),
                        req.revenusAnnuelsEpoux2Eur(),
                        Boolean.TRUE.equals(req.patrimoineCommunSignificatif()),
                        req.dateAssignation()));

        register("F-FA-09-divorce-faute", DivorceFauteRequest.class,
                (req, country) -> DivorceFauteCalculator.compute(
                        req.fautesInvoquees(),
                        req.preuvesDocumentaires(),
                        req.tortsAdverseInvoques(),
                        req.dureeMariageAnnees(),
                        req.revenusAnnuelsDemandeurEur(),
                        req.revenusAnnuelsDefendeurEur(),
                        req.dateDepotAssignation()));

        register("F-FA-10-divorce-accepte", DivorceAccepteRequest.class,
                (req, country) -> DivorceAccepteCalculator.compute(
                        Boolean.TRUE.equals(req.acceptationPrincipeSignee()),
                        req.dateAcceptationPV(),
                        req.dureeMariageAnnees(),
                        req.revenusAnnuelsEpoux1Eur(),
                        req.revenusAnnuelsEpoux2Eur(),
                        Boolean.TRUE.equals(req.patrimoineCommun()),
                        req.dateAssignation()));

        register("F-FA-11-desunion-irremediable-be", DivorceDesunionIrremediableBeRequest.class,
                (req, country) -> DivorceDesunionIrremediableBeCalculator.compute(
                        req.dateSeparation(),
                        Boolean.TRUE.equals(req.separationConsentue()),
                        Boolean.TRUE.equals(req.preuvesSeparation()),
                        Boolean.TRUE.equals(req.preuvesDocumentaires()),
                        Boolean.TRUE.equals(req.tentativesReconciliation()),
                        req.dateAssignation()));

        register("F-FA-13-revisions-post-divorce", RevisionsPostDivorceRequest.class,
                (req, country) -> RevisionsPostDivorceCalculator.compute(
                        req.typeRevision(),
                        req.dateDecisionInitiale(),
                        req.changementCirconstance(),
                        req.revenusInitialsDebiteurEur(),
                        req.revenusActuelsDebiteurEur(),
                        req.revenusInitialsCreancierEur(),
                        req.revenusActuelsCreancierEur(),
                        req.nbEnfantsACharge(),
                        req.ageEnfants(),
                        req.modeResidenceActuel(),
                        req.modeResidenceDemande(),
                        req.informationPrealable(),
                        req.distanceDemenagementKm(),
                        LocalDate.now()));

        register("F-FA-14-ordonnance-protection", OrdonnanceProtectionRequest.class,
                (req, country) -> OrdonnanceProtectionCalculator.compute(
                        req.dateRequete(),
                        req.violencesAlleguees(),
                        req.preuvesViolences() != null ? req.preuvesViolences() : Collections.emptyList(),
                        req.dangerImmediat(),
                        req.presenceEnfants(),
                        req.ageEnfants(),
                        req.logementCommun(),
                        req.victimeFinanciairementDependante(),
                        req.demandeurDejaProtege(),
                        req.demandeMesures() != null ? req.demandeMesures() : Collections.emptyList()));

        register("F-FA-15-recompenses", RecompensesRequest.class,
                (req, country) -> RecompensesCalculator.compute(
                        req.regimeMatrimonial(),
                        req.operations()));

        register("F-FA-19-autorite-parentale", AutoriteParentaleRequest.class,
                (req, country) -> AutoriteParentaleCalculator.compute(
                        req.regimeExerciceActuel(),
                        req.regimeExerciceDemande(),
                        req.motifChangement(),
                        Boolean.TRUE.equals(req.dangerCaracterise()),
                        req.preuvesProduites(),
                        req.ageEnfants(),
                        Boolean.TRUE.equals(req.consentementAutreParent()),
                        Boolean.TRUE.equals(req.interferenceVieEnfant()),
                        req.dateRequete()));

        register("F-FA-19-changement-residence", ChangementResidenceRequest.class,
                (req, country) -> ChangementResidenceCalculator.compute(
                        req.dateChangementPrevu(),
                        req.distanceKm(),
                        req.raisonChangement(),
                        Boolean.TRUE.equals(req.consentementAutreParent()),
                        Boolean.TRUE.equals(req.informePrealablement()),
                        req.delaiInformationJours(),
                        req.modeResidenceActuel(),
                        req.ageEnfants(),
                        Boolean.TRUE.equals(req.scolariteImpactee()),
                        Boolean.TRUE.equals(req.modificationDvhDemandee())));

        register("F-FA-19-desaccords-parentaux", DesaccordsParentauxRequest.class,
                (req, country) -> DesaccordsParentauxCalculator.compute(
                        req.domaineDesaccord(),
                        req.intensiteDesaccord(),
                        req.tentativesMediation(),
                        req.ageEnfantsConcernes(),
                        Boolean.TRUE.equals(req.interetSuperieurInvoque()),
                        Boolean.TRUE.equals(req.expertiseDejaRealisee()),
                        Boolean.TRUE.equals(req.urgence()),
                        req.dateRequete()));

        register("F-FA-21-separation-corps", SeparationCorpsRequest.class,
                (req, country) -> SeparationCorpsCalculator.compute(
                        req.modeProcedure(),
                        req.dateJugementSeparationCorps(),
                        req.dateRequeteConversion(),
                        req.dureeSeparationAnnees() != null ? req.dureeSeparationAnnees() : 0,
                        Boolean.TRUE.equals(req.consentementMutuelConversion()),
                        Boolean.TRUE.equals(req.patrimoineCommun()),
                        req.enfantsMineurs() != null ? req.enfantsMineurs() : 0,
                        Boolean.TRUE.equals(req.demandeReconciliationFormulee())));

        register("F-FA-26-changement-etat-civil", ChangementEtatCivilRequest.class,
                (req, country) -> ChangementEtatCivilCalculator.compute(
                        req.typeChangement(),
                        req.motifInvoque(),
                        req.preuvesProduites(),
                        Boolean.TRUE.equals(req.majeurDemandeur()),
                        Boolean.TRUE.equals(req.consentementParental()),
                        Boolean.TRUE.equals(req.datesDocsConcordants()),
                        Boolean.TRUE.equals(req.dejaChangeAuparavant()),
                        req.dateNaissanceDemandeur(),
                        req.departementDeclaration()));

        // F-FA-24 — Rapport succession (signature : 6 params + country).
        register("F-FA-24-rapport-succession", RapportSuccessionRequest.class,
                (req, country) -> RapportSuccessionCalculator.compute(
                        req.donationsRecuesEur(),
                        req.dateDonation(),
                        req.valeurAuJourPartage(),
                        Boolean.TRUE.equals(req.donationDispenseDeRapport()),
                        Boolean.TRUE.equals(req.naturePresumeeNonRapportable()),
                        req.qualiteHeritier(),
                        country));

        register("F-FA-24-reserve-heriditaire", ReserveHereditaireRequest.class,
                (req, country) -> ReserveHereditaireCalculator.compute(
                        req.nombreEnfants(),
                        req.conjointSurvivant(),
                        req.montantSuccession(),
                        req.montantLibsTotal(),
                        req.dateOuvertureSuccession(),
                        req.qualiteDuDemandeur(),
                        country));

        // ════════════════════════════════════════════════════════════════════
        // Famille BE
        // ════════════════════════════════════════════════════════════════════

        register("F-FA-DC-be", DivorceDcBeRequest.class,
                (req, country) -> DivorceDcBeCalculator.compute(
                        req.dateSignatureConvention(),
                        req.dateAudienceHomologation(),
                        Boolean.TRUE.equals(req.conventionLogement()),
                        Boolean.TRUE.equals(req.conventionBiens()),
                        Boolean.TRUE.equals(req.conventionGardeEnfants()),
                        Boolean.TRUE.equals(req.conventionContributions()),
                        Boolean.TRUE.equals(req.enfantsMineursCommuns()),
                        Boolean.TRUE.equals(req.epouxConsentent())));

        register("F-FA-DDI-be", DivorceDdiBeRequest.class,
                (req, country) -> DivorceDdiBeCalculator.compute(
                        req.dateSeparation(),
                        req.natureDemande(),
                        Boolean.TRUE.equals(req.preuvesDesunionDisponibles())));

        register("F-FA-12-tribunal-famille-be", TribunalFamilleBeMesuresProvisoiresRequest.class,
                (req, country) -> TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        Boolean.TRUE.equals(req.violenceFamiliale()),
                        Boolean.TRUE.equals(req.deplacementEnfantImminent()),
                        Boolean.TRUE.equals(req.dilapidationPatrimoine()),
                        Boolean.TRUE.equals(req.besoinResidenceSeparee()),
                        Boolean.TRUE.equals(req.besoinContributionAlimentaire()),
                        Boolean.TRUE.equals(req.besoinAutoriteParentaleExclusive())));

        register("F-FA-pacte-successoral-be", PacteSuccessoralBe2018Request.class,
                (req, country) -> PacteSuccessoralBe2018Calculator.compute(
                        req.dateSignaturePacte(),
                        Boolean.TRUE.equals(req.acteAuthentique()),
                        Boolean.TRUE.equals(req.accordTousHeritiersReservataires()),
                        Boolean.TRUE.equals(req.equilibreDonationsRapportables()),
                        Boolean.TRUE.equals(req.presenceTousHeritiersReservataires())));

        log.info("SimulatorCalculatorRegistry initialized with {} calculators", descriptors.size());
    }

    /**
     * Helper : enregistre un descripteur.
     */
    private <Req> void register(String toolId, Class<Req> requestType,
                                 BiFunction<Req, String, Object> handler) {
        descriptors.put(toolId, new SimulatorCalculatorDescriptor<>(toolId, requestType, handler));
    }

    /**
     * Helper : priorise le pays du request quand explicite, sinon retombe sur
     * celui résolu depuis le workspace primary de l'utilisateur courant.
     */
    private static String pickCountry(String requestCountry, String resolvedCountry) {
        if (requestCountry != null && !requestCountry.isBlank()) {
            return requestCountry;
        }
        return resolvedCountry;
    }

    private static <T> List<T> nullSafeList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Recherche un descripteur par {@code toolId}. Retourne {@link Optional#empty()}
     * si l'outil n'est pas couvert par le dispatcher V1.
     */
    public Optional<SimulatorCalculatorDescriptor<?>> find(String toolId) {
        return Optional.ofNullable(descriptors.get(toolId));
    }

    /** Retourne le nombre de descripteurs enregistrés (utile pour tests/logs). */
    public int size() {
        return descriptors.size();
    }

    /** Retourne l'ensemble des {@code toolId} supportés. */
    public Set<String> supportedToolIds() {
        return Set.copyOf(descriptors.keySet());
    }
}
