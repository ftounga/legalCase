package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-219-13 : vérificateur de l'éligibilité au régime <i>étudiant
 * jobiste BE</i> — fonction <b>pure</b> (sans dépendance HTTP /
 * persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 3 juillet 1978</b> sur les contrats de travail,
 *       Titre VII "Du contrat d'occupation d'étudiants", art. 120 à
 *       130ter — formalisme + statut.</li>
 *   <li><b>Loi-programme du 24 décembre 2002</b> — instauration des
 *       cotisations de solidarité réduites (modifie l'art. 17bis de
 *       l'AR du 28/11/1969 pris en exécution de la loi du 27/06/1969
 *       sur la sécurité sociale).</li>
 *   <li><b>AR du 14 juillet 1995</b> — modalités d'exécution des
 *       cotisations réduites étudiants (5,42 % patronale + 2,71 %
 *       personnelle, soit 8,13 % total).</li>
 *   <li><b>AR du 5 novembre 2002</b> — déclaration immédiate à l'ONSS
 *       (Dimona spécifique type STU pour étudiants).</li>
 *   <li><b>Loi du 18 décembre 2016</b> — passage du quota de 50 jours
 *       à 475 heures (entrée en vigueur 01/01/2017).</li>
 *   <li><b>Loi-programme du 28 décembre 2022 + AR du 06/03/2023</b> —
 *       relèvement transitoire à 600 heures/an pour 2023-2024.</li>
 *   <li><b>Loi-programme du 22 décembre 2023</b> (M.B. 29/12/2023) —
 *       pérennisation du quota à 600 heures/an dès 2024.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>INELIGIBLE_STATUT_NON_ETUDIANT</b> — ni étudiant à titre
 *       principal, ni interruption courte ≤ 12 mois (sortie immédiate).</li>
 *   <li><b>A_ANALYSER</b> — vide pédagogique &gt; 12 mois avec
 *       réinscription confirmée — zone grise jurisprudentielle.</li>
 *   <li><b>INELIGIBLE_QUOTA_DEPASSE</b> — heures déjà prestées +
 *       heures du contrat font basculer le compteur au-delà du quota
 *       (sortie immédiate après filtre statut).</li>
 *   <li><b>FRAGILE_CONTRAT_OU_DIMONA_MANQUANT</b> — statut + quota OK
 *       mais contrat écrit absent ou Dimona STU non déclarée.</li>
 *   <li><b>FRAGILE_COTISATIONS_NON_REDUITES</b> — statut + formalisme
 *       OK mais cotisations déclarées hors barème AR 14/07/1995.</li>
 *   <li>Sinon : <b>ELIGIBLE</b> — occupation étudiant régulière.</li>
 * </ol>
 *
 * <h2>Calcul du redressement sur excédent</h2>
 * <p>En cas de dépassement du quota, les heures hors quota basculent
 * automatiquement au régime ordinaire (cotisations ~50 % au lieu de
 * 8,13 %). Le redressement estimé = {@code heuresHorsQuota ×
 * remunerationBruteHoraire × (0,50 − 0,0813)} ≈ 41,87 % de la
 * rémunération brute correspondante (différentiel patronal +
 * personnel — calcul indicatif, taux exact dépend de la CP).</p>
 */
public final class EtudiantJobisteBeChecker {

    /** Différentiel approximatif entre cotisations ordinaires (~50 %)
     *  et cotisations de solidarité étudiant (8,13 %) — utilisé pour
     *  estimer le redressement sur les heures hors quota. */
    static final BigDecimal DIFFERENTIEL_COTISATIONS =
            new BigDecimal("0.4187");

    static final String BASE_JURIDIQUE =
            "Loi du 03/07/1978 sur les contrats de travail, Titre VII"
                    + " (art. 120 à 130ter — contrat d'occupation"
                    + " d'étudiants) ; Loi-programme du 24/12/2002"
                    + " (cotisations de solidarité réduites) ; AR du"
                    + " 14/07/1995 (modalités — 5,42 % patronale +"
                    + " 2,71 % personnelle) ; AR du 05/11/2002 (Dimona"
                    + " spécifique type STU) ; Loi du 18/12/2016"
                    + " (passage 50 jours → 475 heures/an) ;"
                    + " Loi-programme du 28/12/2022 + AR du 06/03/2023"
                    + " (relèvement transitoire 600h/an 2023-2024) ;"
                    + " Loi-programme du 22/12/2023 (M.B. 29/12/2023)"
                    + " pérennisant le quota à 600 heures/an.";

    static final String AVERT_AVOCAT_BE =
            "Le quota annuel (600 heures depuis 2023, 475 heures"
                    + " antérieurement) se compte sur l'année civile et"
                    + " agrège l'ensemble des occupations étudiantes"
                    + " (tous employeurs confondus, compteur Student@work"
                    + " — student.be). Le dépassement bascule les heures"
                    + " excédentaires au régime de cotisations ordinaire"
                    + " (~37 % patronales + ~13 % personnelles selon"
                    + " commission paritaire, soit ~50 % au lieu de"
                    + " 8,13 %). En cas d'irrégularité (contrat écrit"
                    + " absent, Dimona STU manquante, cotisations hors"
                    + " barème), l'ONSS requalifie l'occupation au régime"
                    + " normal sur la totalité de la période avec"
                    + " récupération des cotisations patronales et"
                    + " personnelles + intérêts moratoires + sanctions"
                    + " administratives. Confirmation par avocat BE"
                    + " spécialisé en droit social recommandée — en"
                    + " particulier pour les cas frontaliers (étudiants"
                    + " étrangers hors UE, élèves enseignement à horaire"
                    + " réduit ou en alternance, apprentis classes"
                    + " moyennes).";

    private EtudiantJobisteBeChecker() {
    }

    public static EtudiantJobisteBeResult analyze(EtudiantJobisteBeRequest request) {
        validateInputs(request);

        int heuresDeja = request.heuresDejaPresteesDansAnnee();
        int heuresContrat = request.heuresContratEnCours();
        int quota = request.quotaAnnuelHeures();
        int heuresRestantesAvant = Math.max(0, quota - heuresDeja);
        int heuresHorsQuota = Math.max(0,
                (heuresDeja + heuresContrat) - quota);

        BigDecimal redressement = BigDecimal.valueOf(heuresHorsQuota)
                .multiply(request.remunerationBruteHoraire())
                .multiply(DIFFERENTIEL_COTISATIONS)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Hiérarchie 1 : statut non étudiant (sortie immédiate) ───────────
        if (request.statutEtudiant()
                == EtudiantJobisteBeStatutEtudiant.NON_ETUDIANT) {
            return ineligible(request,
                    EtudiantJobisteBeVerdict.INELIGIBLE_STATUT_NON_ETUDIANT,
                    "STATUT_NON_ETUDIANT",
                    "Le candidat n'a pas le statut d'étudiant à titre"
                            + " principal et n'est pas dans une période"
                            + " d'interruption courte (≤ 12 mois entre"
                            + " cycles) ouvrant droit au régime de"
                            + " l'occupation d'étudiants (Loi 03/07/1978,"
                            + " art. 120). L'occupation envisagée ne peut"
                            + " pas bénéficier du contrat d'occupation"
                            + " étudiant ni des cotisations de solidarité"
                            + " réduites.",
                    false, true, true, true,
                    heuresRestantesAvant, heuresHorsQuota, redressement);
        }

        // ── Hiérarchie 2 : vide pédagogique > 12 mois → zone grise ─────────
        if (request.statutEtudiant()
                == EtudiantJobisteBeStatutEtudiant.VIDE_PEDAGOGIQUE_PROLONGE) {
            return ineligible(request,
                    EtudiantJobisteBeVerdict.A_ANALYSER,
                    "VIDE_PEDAGOGIQUE_ZONE_GRISE",
                    "Le candidat est en interruption d'études supérieure"
                            + " à 12 mois consécutifs mais dispose d'une"
                            + " preuve d'inscription ou de réinscription"
                            + " pour l'année académique suivante. La"
                            + " jurisprudence sociale BE est partagée"
                            + " sur le maintien du statut étudiant dans"
                            + " ce cas. Une analyse juridique pointue"
                            + " est requise (avocat BE spécialisé droit"
                            + " social) avant de conclure sur"
                            + " l'applicabilité du régime jobiste — au"
                            + " risque de redressement ONSS rétroactif.",
                    true, true, true, true,
                    heuresRestantesAvant, heuresHorsQuota, redressement);
        }

        boolean statutEligible = true;

        // ── Hiérarchie 3 : quota 600h dépassé ──────────────────────────────
        boolean quotaRespecte = heuresHorsQuota == 0;
        if (!quotaRespecte) {
            String synthese = "Le contrat envisagé (" + heuresContrat
                    + " h) ajouté aux heures déjà prestées dans l'année"
                    + " civile (" + heuresDeja + " h, compteur"
                    + " Student@work) dépasse le quota annuel applicable ("
                    + quota + " h depuis la Loi-programme du 22/12/2023)."
                    + " " + heuresHorsQuota + " h se retrouvent hors"
                    + " quota et basculent automatiquement au régime de"
                    + " cotisations ordinaire (~37 % patronales + ~13 %"
                    + " personnelles selon CP, au lieu de 8,13 % en"
                    + " solidarité étudiante). Redressement indicatif"
                    + " estimé : " + redressement + " € (différentiel"
                    + " ~41,87 % × " + heuresHorsQuota + " h × "
                    + request.remunerationBruteHoraire() + " €/h).";
            return new EtudiantJobisteBeResult(
                    request.dateDebutOccupation(),
                    request.statutEtudiant(),
                    heuresDeja, heuresContrat, quota,
                    request.contratEcritSigne(),
                    request.dimonaStuDeclaree(),
                    request.cotisationsReduitesAppliquees(),
                    request.remunerationBruteHoraire(),
                    EtudiantJobisteBeVerdict.INELIGIBLE_QUOTA_DEPASSE,
                    statutEligible, false,
                    request.contratEcritSigne() && request.dimonaStuDeclaree(),
                    request.cotisationsReduitesAppliquees(),
                    heuresRestantesAvant, heuresHorsQuota, redressement,
                    "QUOTA_DEPASSE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 4 : formalisme défaillant ───────────────────────────
        boolean formalismeRespecte = request.contratEcritSigne()
                && request.dimonaStuDeclaree();
        if (!formalismeRespecte) {
            String quoi = !request.contratEcritSigne()
                    && !request.dimonaStuDeclaree()
                    ? "contrat d'occupation étudiant écrit absent ET"
                            + " déclaration Dimona STU non effectuée"
                    : !request.contratEcritSigne()
                            ? "contrat d'occupation étudiant écrit absent"
                                    + " (art. 123 Loi 03/07/1978)"
                            : "déclaration Dimona spécifique STU (AR"
                                    + " 05/11/2002) non effectuée avant"
                                    + " le début des prestations";
            String synthese = "Statut étudiant et quota OK mais formalisme"
                    + " défaillant : " + quoi + ". Risque de"
                    + " requalification ONSS au régime ordinaire avec"
                    + " récupération de l'intégralité des cotisations"
                    + " patronales et personnelles (~37 % + ~13 % selon"
                    + " CP), intérêts moratoires et sanctions"
                    + " administratives. À régulariser sans délai avant"
                    + " contrôle ONSS / inspection sociale.";
            return new EtudiantJobisteBeResult(
                    request.dateDebutOccupation(),
                    request.statutEtudiant(),
                    heuresDeja, heuresContrat, quota,
                    request.contratEcritSigne(),
                    request.dimonaStuDeclaree(),
                    request.cotisationsReduitesAppliquees(),
                    request.remunerationBruteHoraire(),
                    EtudiantJobisteBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT,
                    statutEligible, true, false,
                    request.cotisationsReduitesAppliquees(),
                    heuresRestantesAvant, heuresHorsQuota, redressement,
                    "CONTRAT_OU_DIMONA_MANQUANT",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 5 : cotisations hors barème ─────────────────────────
        if (!request.cotisationsReduitesAppliquees()) {
            String synthese = "Statut, quota et formalisme OK mais"
                    + " l'employeur n'applique pas le barème de"
                    + " solidarité réduit prévu par l'AR du 14/07/1995"
                    + " (5,42 % patronale + 2,71 % personnelle = 8,13 %"
                    + " total). La DmfA ONSS doit indiquer le code"
                    + " travailleur étudiant correspondant. Risque de"
                    + " redressement ONSS pour calcul erroné de"
                    + " cotisations (différentiel ~41,87 % de la"
                    + " rémunération brute) — à régulariser par"
                    + " déclaration modificative.";
            return new EtudiantJobisteBeResult(
                    request.dateDebutOccupation(),
                    request.statutEtudiant(),
                    heuresDeja, heuresContrat, quota,
                    request.contratEcritSigne(),
                    request.dimonaStuDeclaree(),
                    request.cotisationsReduitesAppliquees(),
                    request.remunerationBruteHoraire(),
                    EtudiantJobisteBeVerdict.FRAGILE_COTISATIONS_NON_REDUITES,
                    statutEligible, true, true, false,
                    heuresRestantesAvant, heuresHorsQuota, redressement,
                    "COTISATIONS_NON_REDUITES",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Cas qualifiant : occupation étudiant pleinement régulière ──────
        int heuresRestantesApres = heuresRestantesAvant - heuresContrat;
        String synthese = "Occupation étudiant pleinement régulière au"
                + " sens de la Loi du 03/07/1978 (Titre VII) et de"
                + " l'AR du 14/07/1995 : statut " + libelleStatut(
                        request.statutEtudiant()) + ", quota annuel"
                + " respecté (" + heuresDeja + " h déjà prestées + "
                + heuresContrat + " h ce contrat ≤ " + quota + " h —"
                + " reste " + heuresRestantesApres + " h disponibles"
                + " après ce contrat), contrat d'occupation étudiant"
                + " écrit signé, Dimona STU déclarée, cotisations de"
                + " solidarité réduites appliquées (5,42 % + 2,71 %"
                + " = 8,13 % au lieu de ~50 % en régime ordinaire).";

        return new EtudiantJobisteBeResult(
                request.dateDebutOccupation(),
                request.statutEtudiant(),
                heuresDeja, heuresContrat, quota,
                request.contratEcritSigne(),
                request.dimonaStuDeclaree(),
                request.cotisationsReduitesAppliquees(),
                request.remunerationBruteHoraire(),
                EtudiantJobisteBeVerdict.ELIGIBLE,
                true, true, true, true,
                heuresRestantesAvant, heuresHorsQuota, redressement,
                "ETUDIANT_JOBISTE_REGULIER",
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    // ── Construction d'un résultat inéligible commun (statut) ──────────────

    private static EtudiantJobisteBeResult ineligible(
            EtudiantJobisteBeRequest request,
            EtudiantJobisteBeVerdict verdict,
            String raison,
            String synthese,
            boolean statutEligible,
            boolean quotaRespecte,
            boolean formalismeRespecte,
            boolean cotisationsConformes,
            int heuresRestantesAvant,
            int heuresHorsQuota,
            BigDecimal redressement) {
        return new EtudiantJobisteBeResult(
                request.dateDebutOccupation(),
                request.statutEtudiant(),
                request.heuresDejaPresteesDansAnnee(),
                request.heuresContratEnCours(),
                request.quotaAnnuelHeures(),
                request.contratEcritSigne(),
                request.dimonaStuDeclaree(),
                request.cotisationsReduitesAppliquees(),
                request.remunerationBruteHoraire(),
                verdict,
                statutEligible,
                quotaRespecte,
                formalismeRespecte,
                cotisationsConformes,
                heuresRestantesAvant,
                heuresHorsQuota,
                redressement,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static String libelleStatut(EtudiantJobisteBeStatutEtudiant s) {
        return switch (s) {
            case ETUDIANT_PRINCIPAL -> "étudiant à titre principal (inscription régulière)";
            case INTERRUPTION_COURTE_MOINS_12_MOIS -> "interruption courte entre cycles (≤ 12 mois)";
            case VIDE_PEDAGOGIQUE_PROLONGE -> "vide pédagogique avec réinscription";
            case NON_ETUDIANT -> "non étudiant";
        };
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(EtudiantJobisteBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateDebutOccupation() == null) {
            throw new IllegalArgumentException(
                    "dateDebutOccupation est requise");
        }
        if (request.statutEtudiant() == null) {
            throw new IllegalArgumentException(
                    "statutEtudiant est requis");
        }
        if (request.heuresDejaPresteesDansAnnee() == null) {
            throw new IllegalArgumentException(
                    "heuresDejaPresteesDansAnnee est requis");
        }
        if (request.heuresDejaPresteesDansAnnee() < 0) {
            throw new IllegalArgumentException(
                    "heuresDejaPresteesDansAnnee doit être positif ou nul");
        }
        if (request.heuresContratEnCours() == null) {
            throw new IllegalArgumentException(
                    "heuresContratEnCours est requis");
        }
        if (request.heuresContratEnCours() <= 0) {
            throw new IllegalArgumentException(
                    "heuresContratEnCours doit être strictement positif");
        }
        if (request.quotaAnnuelHeures() == null) {
            throw new IllegalArgumentException(
                    "quotaAnnuelHeures est requis");
        }
        if (request.quotaAnnuelHeures() <= 0) {
            throw new IllegalArgumentException(
                    "quotaAnnuelHeures doit être strictement positif");
        }
        if (request.contratEcritSigne() == null) {
            throw new IllegalArgumentException(
                    "contratEcritSigne est requis");
        }
        if (request.dimonaStuDeclaree() == null) {
            throw new IllegalArgumentException(
                    "dimonaStuDeclaree est requis");
        }
        if (request.cotisationsReduitesAppliquees() == null) {
            throw new IllegalArgumentException(
                    "cotisationsReduitesAppliquees est requis");
        }
        if (request.remunerationBruteHoraire() == null) {
            throw new IllegalArgumentException(
                    "remunerationBruteHoraire est requise");
        }
        if (request.remunerationBruteHoraire()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "remunerationBruteHoraire doit être positif ou nul");
        }
    }
}
