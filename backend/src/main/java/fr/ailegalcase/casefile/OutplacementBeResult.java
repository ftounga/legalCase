package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-207-08 : résultat brut produit par {@link OutplacementBeCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en
 * JSON dans la colonne {@code outplacement_be_analyses.result_data}.</p>
 *
 * <p>5 verdicts conditionnels selon l'âge, l'ancienneté, le motif et l'état
 * de l'offre (cf. {@link Verdict}). Sanction employeur 1 800 € (AR 30/05/2018)
 * ou sanction salarié 4-52 semaines (AR 25/11/1991 art. 154) selon le cas.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code verdict} — un des 5 {@link Verdict}.</li>
 *   <li>{@code ageALaDateLicenciement} — âge en années pleines à la date du
 *       licenciement.</li>
 *   <li>{@code obligationEmployeurApplicable} — true si l'employeur est tenu
 *       (âge ≥ 45 + ancienneté ≥ 1 + motif licenciement hors faute grave).</li>
 *   <li>{@code raisonNonObligation} — non null UNIQUEMENT pour le verdict
 *       {@link Verdict#OUTPLACEMENT_NON_DU}.</li>
 *   <li>{@code sanctionEmployeurEuros} — 1 800 € si verdict d'infraction
 *       employeur, 0 € sinon.</li>
 *   <li>{@code sanctionSalarieRange} — fourchette 4-52 semaines UNIQUEMENT
 *       pour le verdict {@link Verdict#OFFRE_REFUSEE_SANCTION_SALARIE}.</li>
 *   <li>{@code dateLimiteOffre} — {@code dateLicenciement + 15 jours}
 *       calendaires (fuseau Europe/Brussels).</li>
 *   <li>{@code delaiOffreRespecte} — {@code dateOffre ≤ dateLimiteOffre} si
 *       l'offre est reçue, null sinon.</li>
 *   <li>{@code etapeSuivante} — orientation procédurale.</li>
 *   <li>{@code baseJuridique} — citation complète des bases juridiques BE.</li>
 *   <li>{@code formuleCalcul} — explication textuelle synthétique.</li>
 *   <li>Champs <i>input</i> normalisés persistés pour pré-fill GET.</li>
 * </ul>
 */
public record OutplacementBeResult(
        // Inputs normalisés (persistés pour survie au reload)
        LocalDate dateLicenciement,
        LocalDate dateNaissanceSalarie,
        BigDecimal ancienneteAnnees,
        OutplacementBeMotifLicenciement motifLicenciement,
        boolean contratTempsPlein,
        boolean offreOutplacementRecue,
        LocalDate dateOffreOutplacement,
        Boolean offreConformeCCT82,
        Boolean salarieAcceptantOffre,
        // Outputs calculés
        Verdict verdict,
        int ageALaDateLicenciement,
        boolean obligationEmployeurApplicable,
        OutplacementBeRaisonNonObligation raisonNonObligation,
        BigDecimal sanctionEmployeurEuros,
        SanctionSalarieRange sanctionSalarieRange,
        LocalDate dateLimiteOffre,
        Boolean delaiOffreRespecte,
        EtapeSuivante etapeSuivante,
        String baseJuridique,
        String formuleCalcul
) {

    /**
     * 5 verdicts conditionnels de l'outil — priorité de gauche à droite (le
     * premier critère matching détermine le verdict) :
     * <ol>
     *   <li>{@link #OUTPLACEMENT_NON_DU} — au moins une des 4 conditions
     *       d'exclusion remplie (âge &lt; 45, ancienneté &lt; 1, faute grave,
     *       démission).</li>
     *   <li>{@link #OFFRE_NON_FAITE_SANCTION_EMPLOYEUR} — obligation
     *       applicable mais aucune offre formelle reçue.</li>
     *   <li>{@link #OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR} — offre reçue
     *       mais non conforme CCT 82 (contenu invalide ou hors délai 15 j).</li>
     *   <li>{@link #OFFRE_REFUSEE_SANCTION_SALARIE} — offre conforme mais
     *       refusée par le salarié.</li>
     *   <li>{@link #OFFRE_CONFORME_RESPECTEE} — offre conforme acceptée ou
     *       pas encore décidée.</li>
     * </ol>
     */
    public enum Verdict {
        OUTPLACEMENT_NON_DU,
        OFFRE_CONFORME_RESPECTEE,
        OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR,
        OFFRE_NON_FAITE_SANCTION_EMPLOYEUR,
        OFFRE_REFUSEE_SANCTION_SALARIE
    }

    /**
     * Orientation procédurale recommandée à l'avocat selon le verdict.
     *
     * <p>{@link #PLAINTE_INSPECTION_LOIS_SOCIALES} : sanction employeur à
     * actionner — plainte au Contrôle des lois sociales (recouvrement de
     * l'amende administrative 1 800 €, AR 30/05/2018).</p>
     * <p>{@link #PRESENTATION_C4_ONEM} : refus offre conforme — la sanction
     * salarié sera visible sur le C4 (orienter vers SF-207-02).</p>
     * <p>{@link #AUCUNE} : verdict conforme ou non dû, aucune action requise.</p>
     */
    public enum EtapeSuivante {
        PLAINTE_INSPECTION_LOIS_SOCIALES,
        PRESENTATION_C4_ONEM,
        AUCUNE
    }

    /**
     * Fourchette d'exclusion ONEM pour le salarié refusant une offre
     * d'outplacement conforme — 4 à 52 semaines (AR du 25 novembre 1991
     * art. 154).
     */
    public record SanctionSalarieRange(int minSemaines, int maxSemaines) {}
}
