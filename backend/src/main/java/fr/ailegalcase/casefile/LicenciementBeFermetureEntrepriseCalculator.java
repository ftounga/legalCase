package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * SF-219-06 : analyseur licenciement BE en cas de fermeture
 * d'entreprise — fonction <b>pure</b> (sans dépendance HTTP /
 * persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 26/06/2002</b> relative aux fermetures d'entreprises
 *       — institue l'indemnité de fermeture et le Fonds Fermeture
 *       Entreprises (FFE) géré par l'ONEM.</li>
 *   <li><b>AR du 23/03/2007</b> portant exécution de la loi du
 *       26/06/2002 — fixe les modalités de calcul de l'indemnité, les
 *       seuils d'effectif et les régimes dérogatoires.</li>
 *   <li><b>CCT n° 9bis</b> conclue au Conseil National du Travail —
 *       information / consultation des travailleurs en cas de fermeture
 *       d'entreprise (volet procédural, hors scope V1 calcul).</li>
 *   <li><b>Loi du 13/02/1998</b> relative aux licenciements collectifs
 *       (procédure dite Renault) — invariante avec la fermeture.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>INELIGIBLE_TYPE_FERMETURE</b> — si le type de fermeture
 *       déclaré n'est pas qualifiant (cessation partielle, transfert
 *       CCT 32bis, fusion sans suppression, fermeture temporaire) :
 *       sortie immédiate.</li>
 *   <li><b>INELIGIBLE_SEUIL_EFFECTIF</b> — entreprise &lt; 20 ETP
 *       (régime général V1, dérogations PME hors scope).</li>
 *   <li><b>INELIGIBLE_ANCIENNETE_INSUFFISANTE</b> — ancienneté &lt;
 *       1 an à la date de fermeture.</li>
 *   <li>Sinon : <b>ELIGIBLE_FFE_REPRISE_CREANCES</b> (insolvabilité +
 *       créances impayées &gt; 0), <b>ELIGIBLE_FFE_COMPLET</b>
 *       (insolvabilité avérée sans créance impayée déclarée), ou
 *       <b>ELIGIBLE_INDEMNITE_SEULE</b> (employeur solvable).</li>
 * </ol>
 *
 * <h2>Indemnité de fermeture (V1, indicatif)</h2>
 * <p>Le barème AR 23/03/2007 distingue un <b>montant forfaitaire par
 * année d'ancienneté</b> (indexé annuellement par le FFE) et un
 * <b>supplément mensuel selon l'âge ≥ 45 ans</b>. V1 retient des
 * montants indicatifs (à confirmer par avocat BE / FFE selon le barème
 * en vigueur l'année de fermeture) — l'utilisateur est explicitement
 * averti que le calcul est non opposable au FFE.</p>
 *
 * <ul>
 *   <li>Montant forfaitaire par année : <b>167,72 €</b> (référence
 *       indicative 2026, plafonné à 25 années).</li>
 *   <li>Supplément mensuel ≥ 45 ans : <b>167,72 €</b> par année au-dessus
 *       de 45 ans (plafonné à 20 années, soit 65 ans).</li>
 * </ul>
 */
public final class LicenciementBeFermetureEntrepriseCalculator {

    static final String BASE_JURIDIQUE =
            "Loi du 26/06/2002 relative aux fermetures d'entreprises (CCT n° 9bis"
                    + " du CNT — information / consultation) ; AR du 23/03/2007"
                    + " portant exécution (Fonds Fermeture Entreprises FFE,"
                    + " indemnité forfaitaire + supplément ≥ 45 ans) ; Loi du"
                    + " 13/02/1998 sur les licenciements collectifs (procédure"
                    + " Renault).";

    static final String AVERT_MONTANTS_INDICATIFS =
            "Les montants d'indemnité de fermeture et les créances reprises"
                    + " par le FFE sont indicatifs (barème AR 23/03/2007"
                    + " susceptible d'indexation annuelle). Confirmation"
                    + " obligatoire auprès du FFE (ONEM) avant toute"
                    + " communication client. Les plafonds légaux applicables"
                    + " aux créances reprises ne sont pas recalculés V1.";

    /** Montant forfaitaire indicatif par année d'ancienneté (€, AR 23/03/2007). */
    static final BigDecimal MONTANT_FORFAITAIRE_PAR_ANNEE = new BigDecimal("167.72");

    /** Supplément indicatif par année au-dessus de 45 ans (€, AR 23/03/2007). */
    static final BigDecimal SUPPLEMENT_PAR_ANNEE_AU_DESSUS_45 = new BigDecimal("167.72");

    /** Seuil d'âge de déclenchement du supplément (Loi 26/06/2002). */
    static final int AGE_SUPPLEMENT = 45;

    /** Plafond d'années d'ancienneté retenu pour l'indemnité forfaitaire. */
    static final int PLAFOND_ANNEES_ANCIENNETE = 25;

    /** Plafond d'années retenu pour le supplément âge (45 → 65). */
    static final int PLAFOND_ANNEES_SUPPLEMENT = 20;

    /** Seuil d'effectif ETP du régime général FFE (AR 23/03/2007). */
    static final int SEUIL_EFFECTIF_REGIME_GENERAL = 20;

    /** Ancienneté minimale pour l'indemnité de fermeture (Loi 26/06/2002 art. 4). */
    static final int ANCIENNETE_MINIMALE_ANNEES = 1;

    private LicenciementBeFermetureEntrepriseCalculator() {
    }

    public static LicenciementBeFermetureEntrepriseResult analyze(
            LicenciementBeFermetureEntrepriseRequest request) {
        validateInputs(request);

        int age = ageAt(request.dateNaissance(), request.dateFermeture());
        int anciennete = anneesAnciennete(request.dateDebutContrat(),
                request.dateFermeture());

        // Le type de fermeture prime sur tout le reste (court-circuite tout).
        if (!estTypeQualifiant(request.typeFermeture())) {
            return ineligible(request,
                    LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_TYPE_FERMETURE,
                    age, anciennete,
                    "TYPE_FERMETURE_NON_QUALIFIANT",
                    "Le type de fermeture déclaré (" + libelleType(request.typeFermeture())
                            + ") ne qualifie pas pour le FFE — pas d'indemnité"
                            + " de fermeture ni de reprise de créances par le Fonds."
                            + " Vérifier l'applicabilité d'autres régimes (CCT 32bis"
                            + " transfert, chômage temporaire, licenciement collectif"
                            + " Renault) selon la situation réelle.");
        }

        if (request.effectifEtp() < SEUIL_EFFECTIF_REGIME_GENERAL) {
            return ineligible(request,
                    LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_SEUIL_EFFECTIF,
                    age, anciennete,
                    "SEUIL_EFFECTIF_NON_ATTEINT",
                    "Effectif " + request.effectifEtp() + " ETP < seuil régime général"
                            + " " + SEUIL_EFFECTIF_REGIME_GENERAL + " ETP (AR 23/03/2007)."
                            + " Vérifier l'éligibilité aux régimes dérogatoires PME"
                            + " (commerce, certains secteurs) — non couverts V1 de"
                            + " l'outil.");
        }

        if (anciennete < ANCIENNETE_MINIMALE_ANNEES) {
            return ineligible(request,
                    LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_ANCIENNETE_INSUFFISANTE,
                    age, anciennete,
                    "ANCIENNETE_INSUFFISANTE",
                    "Ancienneté " + anciennete + " an(s) < " + ANCIENNETE_MINIMALE_ANNEES
                            + " an minimum (Loi 26/06/2002 art. 4) — pas d'indemnité"
                            + " de fermeture ni de reprise FFE possible.");
        }

        // À ce stade : éligible. Calcul de l'indemnité forfaitaire + supplément âge.
        int anneesIndemnisables = Math.min(anciennete, PLAFOND_ANNEES_ANCIENNETE);
        BigDecimal indemniteForfaitaire = MONTANT_FORFAITAIRE_PAR_ANNEE
                .multiply(BigDecimal.valueOf(anneesIndemnisables));

        int anneesSupplement = (age >= AGE_SUPPLEMENT)
                ? Math.min(age - AGE_SUPPLEMENT, PLAFOND_ANNEES_SUPPLEMENT)
                : 0;
        BigDecimal supplementAge = SUPPLEMENT_PAR_ANNEE_AU_DESSUS_45
                .multiply(BigDecimal.valueOf(anneesSupplement));

        BigDecimal indemniteFermeture = indemniteForfaitaire.add(supplementAge)
                .setScale(2, RoundingMode.HALF_UP);

        // Reprise FFE si insolvabilité avérée ou faillite déclarée.
        boolean insolvable =
                request.statutEmployeur() != LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE;
        BigDecimal totalCreancesFfe = BigDecimal.ZERO;
        LicenciementBeFermetureEntrepriseVerdict verdict;
        String raison;

        if (insolvable) {
            totalCreancesFfe = request.salairesImpayes()
                    .add(request.peculeVacancesImpaye())
                    .add(request.indemniteRuptureImpayee())
                    .setScale(2, RoundingMode.HALF_UP);
            if (totalCreancesFfe.signum() > 0) {
                verdict = LicenciementBeFermetureEntrepriseVerdict.ELIGIBLE_FFE_REPRISE_CREANCES;
                raison = "ELIGIBLE_FFE_REPRISE_CREANCES";
            } else {
                verdict = LicenciementBeFermetureEntrepriseVerdict.ELIGIBLE_FFE_COMPLET;
                raison = "ELIGIBLE_FFE_INDEMNITE_FFE";
            }
        } else {
            verdict = LicenciementBeFermetureEntrepriseVerdict.ELIGIBLE_INDEMNITE_SEULE;
            raison = "ELIGIBLE_INDEMNITE_EMPLOYEUR_SOLVABLE";
        }

        String synthese = buildSyntheseEligible(verdict, anciennete, age,
                indemniteFermeture, totalCreancesFfe, anneesSupplement);

        return new LicenciementBeFermetureEntrepriseResult(
                request.dateNaissance(),
                request.dateDebutContrat(),
                request.dateFermeture(),
                request.remunerationMensuelleBrute(),
                request.typeFermeture(),
                request.statutEmployeur(),
                request.effectifEtp(),
                request.salairesImpayes(),
                request.peculeVacancesImpaye(),
                request.indemniteRuptureImpayee(),
                verdict,
                true,
                age,
                anciennete,
                indemniteFermeture,
                MONTANT_FORFAITAIRE_PAR_ANNEE,
                SUPPLEMENT_PAR_ANNEE_AU_DESSUS_45,
                totalCreancesFfe,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_MONTANTS_INDICATIFS);
    }

    // ── Construction du résultat inéligible ────────────────────────────────

    private static LicenciementBeFermetureEntrepriseResult ineligible(
            LicenciementBeFermetureEntrepriseRequest request,
            LicenciementBeFermetureEntrepriseVerdict verdict,
            int age, int anciennete, String raison, String synthese) {
        return new LicenciementBeFermetureEntrepriseResult(
                request.dateNaissance(),
                request.dateDebutContrat(),
                request.dateFermeture(),
                request.remunerationMensuelleBrute(),
                request.typeFermeture(),
                request.statutEmployeur(),
                request.effectifEtp(),
                request.salairesImpayes(),
                request.peculeVacancesImpaye(),
                request.indemniteRuptureImpayee(),
                verdict,
                false,
                age,
                anciennete,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                MONTANT_FORFAITAIRE_PAR_ANNEE,
                SUPPLEMENT_PAR_ANNEE_AU_DESSUS_45,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_MONTANTS_INDICATIFS);
    }

    // ── Synthèse en langage avocat ──────────────────────────────────────────

    private static String buildSyntheseEligible(
            LicenciementBeFermetureEntrepriseVerdict verdict,
            int anciennete, int age,
            BigDecimal indemnite, BigDecimal creances,
            int anneesSupplement) {
        String suffixSupplement = anneesSupplement > 0
                ? " Supplément âge ≥ 45 ans : " + anneesSupplement + " année(s)"
                + " au-dessus de 45 ans intégrée(s) au calcul."
                : "";
        return switch (verdict) {
            case ELIGIBLE_INDEMNITE_SEULE -> "Fermeture qualifiante (ancienneté "
                    + anciennete + " an(s), âge " + age + " ans). Indemnité de"
                    + " fermeture due par l'employeur : ≈ " + format(indemnite)
                    + " €." + suffixSupplement + " Employeur solvable — pas de"
                    + " reprise des créances par le FFE (voie civile en cas de"
                    + " défaut de paiement postérieur).";
            case ELIGIBLE_FFE_REPRISE_CREANCES -> "Fermeture qualifiante +"
                    + " insolvabilité avérée. Indemnité de fermeture ≈ "
                    + format(indemnite) + " € + reprise FFE des créances"
                    + " impayées (salaires + pécule + indemnité de rupture) ≈ "
                    + format(creances) + " € (plafonds légaux à confirmer)."
                    + suffixSupplement;
            case ELIGIBLE_FFE_COMPLET -> "Fermeture qualifiante + insolvabilité"
                    + " avérée, sans créance impayée déclarée. Indemnité de"
                    + " fermeture ≈ " + format(indemnite) + " € à charge du"
                    + " FFE." + suffixSupplement;
            default -> throw new IllegalStateException(
                    "Verdict éligible attendu, reçu : " + verdict);
        };
    }

    // ── Utilitaires métier ─────────────────────────────────────────────────

    private static boolean estTypeQualifiant(
            LicenciementBeFermetureEntrepriseTypeFermeture type) {
        return type == LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE
                || type == LicenciementBeFermetureEntrepriseTypeFermeture.FAILLITE_LIQUIDATION_JUDICIAIRE;
    }

    private static String libelleType(
            LicenciementBeFermetureEntrepriseTypeFermeture type) {
        return switch (type) {
            case CESSATION_DEFINITIVE -> "cessation définitive";
            case FAILLITE_LIQUIDATION_JUDICIAIRE -> "faillite / liquidation judiciaire";
            case CESSATION_PARTIELLE -> "cessation partielle";
            case TRANSFERT_CCT_32BIS -> "transfert d'entreprise CCT 32bis";
            case FUSION_SANS_SUPPRESSION -> "fusion sans suppression d'emplois";
            case FERMETURE_TEMPORAIRE -> "fermeture temporaire";
        };
    }

    private static String format(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static int ageAt(LocalDate naissance, LocalDate reference) {
        return Period.between(naissance, reference).getYears();
    }

    private static int anneesAnciennete(LocalDate debut, LocalDate fin) {
        return Period.between(debut, fin).getYears();
    }

    // ── Validation conditionnelle ───────────────────────────────────────────

    private static void validateInputs(LicenciementBeFermetureEntrepriseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateNaissance() == null) {
            throw new IllegalArgumentException("dateNaissance est requise");
        }
        if (request.dateDebutContrat() == null) {
            throw new IllegalArgumentException("dateDebutContrat est requise");
        }
        if (request.dateFermeture() == null) {
            throw new IllegalArgumentException("dateFermeture est requise");
        }
        if (request.dateFermeture().isBefore(request.dateNaissance())) {
            throw new IllegalArgumentException(
                    "dateFermeture ne peut pas précéder dateNaissance");
        }
        if (request.dateFermeture().isBefore(request.dateDebutContrat())) {
            throw new IllegalArgumentException(
                    "dateFermeture ne peut pas précéder dateDebutContrat");
        }
        if (request.remunerationMensuelleBrute() == null
                || request.remunerationMensuelleBrute().signum() < 0) {
            throw new IllegalArgumentException(
                    "remunerationMensuelleBrute requise et ≥ 0");
        }
        if (request.typeFermeture() == null) {
            throw new IllegalArgumentException("typeFermeture est requise");
        }
        if (request.statutEmployeur() == null) {
            throw new IllegalArgumentException("statutEmployeur est requise");
        }
        if (request.effectifEtp() == null || request.effectifEtp() < 0) {
            throw new IllegalArgumentException("effectifEtp requise et ≥ 0");
        }
        if (request.salairesImpayes() == null
                || request.salairesImpayes().signum() < 0) {
            throw new IllegalArgumentException("salairesImpayes requise et ≥ 0");
        }
        if (request.peculeVacancesImpaye() == null
                || request.peculeVacancesImpaye().signum() < 0) {
            throw new IllegalArgumentException("peculeVacancesImpaye requise et ≥ 0");
        }
        if (request.indemniteRuptureImpayee() == null
                || request.indemniteRuptureImpayee().signum() < 0) {
            throw new IllegalArgumentException(
                    "indemniteRuptureImpayee requise et ≥ 0");
        }
    }
}
