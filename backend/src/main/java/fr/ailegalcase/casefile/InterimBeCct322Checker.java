package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-219-14 : vérificateur de validité d'une mission intérimaire BE
 * (Loi du 24/07/1987 + CCT n° 322 du 14/06/2010 du CNT) — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 24/07/1987</b> relative au travail temporaire, au
 *       travail intérimaire et à la mise de travailleurs à la
 *       disposition d'utilisateurs (M.B. 20/08/1987) — texte
 *       fondateur, modifié successivement.</li>
 *   <li><b>CCT n° 322 du 14/06/2010</b> conclue au CNT — fixe la
 *       responsabilité solidaire ETI / utilisateur et la parité
 *       salariale stricte (art. 10 Loi 24/07/1987 + art. 3 CCT
 *       322).</li>
 *   <li><b>Loi du 26/06/2013</b> + <b>CCT n° 108 du 16/07/2013</b>
 *       — introduction du motif d'insertion en vue d'embauche
 *       durable (art. 1bis Loi 24/07/1987).</li>
 *   <li><b>AR du 11/10/1976</b> — liste limitative des cas de
 *       travail exceptionnel (inventaires, déménagements, foires,
 *       événements, ...).</li>
 *   <li><b>Loi-programme du 27/12/2012</b> — sanctions ONSS
 *       renforcées en cas de fraude au régime intérimaire.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT</b> — recours
 *       pour remplacer un travailleur en grève / lock-out
 *       (art. 19) : interdiction absolue, présomption irréfragable
 *       de fraude (sortie immédiate, prime sur tout autre motif).</li>
 *   <li><b>INELIGIBLE_MOTIF_NON_AUTORISE</b> — motif invoqué non
 *       listé par la loi → requalification CDI utilisateur
 *       (art. 20). Sortie immédiate.</li>
 *   <li><b>A_ANALYSER</b> — motif artistique ou flux exceptionnel :
 *       analyse cas par cas requise (avocat BE), durée variable
 *       selon AR sectoriels.</li>
 *   <li><b>INELIGIBLE_DUREE_MAX_DEPASSEE</b> — la durée totale de
 *       la mission excède le plafond légal applicable.</li>
 *   <li><b>INELIGIBLE_PARITE_SALARIALE_VIOLEE</b> — salaire
 *       intérimaire sous celui du permanent de référence (art. 10).</li>
 *   <li><b>FRAGILE_CONTRAT_OU_DIMONA_MANQUANT</b> — conditions de
 *       fond OK mais contrat écrit absent ou Dimona ETI non
 *       déclarée — requalification automatique pour défaut de
 *       formalisme.</li>
 *   <li>Sinon : <b>ELIGIBLE_MISSION_REGULIERE</b>.</li>
 * </ol>
 *
 * <h2>Sanction (commune à tous les verdicts négatifs)</h2>
 * <p>Requalification du contrat en <b>CDI ordinaire entre l'utilisateur
 * et le travailleur</b> avec rétroactivité (art. 20 Loi 24/07/1987 +
 * CCT n° 322) + <b>responsabilité solidaire</b> ETI / utilisateur pour
 * paiement intégral des cotisations ONSS éludées, salaires arriérés,
 * indemnités de rupture éventuelles et sanctions administratives ONSS.</p>
 */
public final class InterimBeCct322Checker {

    static final String BASE_JURIDIQUE =
            "Loi du 24/07/1987 relative au travail temporaire, au travail"
                    + " intérimaire et à la mise de travailleurs à la"
                    + " disposition d'utilisateurs (M.B. 20/08/1987),"
                    + " art. 1er § 1 (motifs limitatifs), art. 1bis"
                    + " (insertion durable, motif artistique), art. 8"
                    + " (contrat écrit ETI/intérimaire), art. 9 (contrat"
                    + " de mise à disposition ETI/utilisateur), art. 10"
                    + " (parité salariale stricte), art. 19 (interdiction"
                    + " absolue de remplacement grève/lock-out), art. 20"
                    + " (requalification CDI utilisateur + responsabilité"
                    + " solidaire) ; CCT n° 322 du 14/06/2010 conclue au"
                    + " CNT (responsabilité solidaire ETI/utilisateur,"
                    + " parité salariale) ; CCT n° 108 du 16/07/2013"
                    + " (motif insertion en vue d'embauche durable, max"
                    + " 3 tentatives / 9 mois cumulés) ; AR du 11/10/1976"
                    + " (liste limitative cas de travail exceptionnel) ;"
                    + " Loi-programme du 27/12/2012 (sanctions ONSS"
                    + " renforcées). Cumul Dimona ETI + contrat écrit"
                    + " obligatoires à peine de requalification immédiate.";

    static final String AVERT_AVOCAT_BE =
            "Les durées plafond exactes (notamment le plafond de la mission"
                    + " pour surcroît temporaire, l'application des AR"
                    + " sectoriels au travail exceptionnel, la durée"
                    + " maximale du motif artistique) doivent être"
                    + " confirmées par un avocat spécialisé en droit"
                    + " social BE pour la date d'occupation considérée et"
                    + " la commission paritaire de l'utilisateur. En cas"
                    + " d'irrégularité (motif non autorisé, dépassement"
                    + " durée, parité salariale violée, contrat écrit"
                    + " absent ou Dimona manquante), le contrat est"
                    + " requalifié en CDI ordinaire avec l'utilisateur"
                    + " (art. 20 Loi 24/07/1987), l'utilisateur et l'ETI"
                    + " sont solidairement responsables des cotisations"
                    + " ONSS éludées + salaires arriérés + indemnités de"
                    + " rupture + sanctions administratives ONSS (CCT"
                    + " n° 322 du 14/06/2010). Cas grève/lock-out :"
                    + " présomption irréfragable de fraude (art. 19),"
                    + " aucune justification a posteriori possible.";

    static final String SANCTION_REQUALIFICATION =
            "Requalification du contrat en CDI ordinaire entre l'utilisateur"
                    + " et le travailleur avec rétroactivité (art. 20"
                    + " Loi 24/07/1987) + responsabilité solidaire"
                    + " ETI/utilisateur pour cotisations ONSS éludées,"
                    + " salaires arriérés et indemnités de rupture"
                    + " éventuelles (CCT n° 322 du 14/06/2010).";

    private InterimBeCct322Checker() {
    }

    public static InterimBeCct322Result analyze(InterimBeCct322Request request) {
        validateInputs(request);

        // Calculs dérivés communs
        int joursExcedentaires = Math.max(
                0,
                request.dureeTotaleMissionJours() - request.dureeMaxLegaleJours());
        BigDecimal ecartParite = request.salaireHoraireReferenceBrut()
                .subtract(request.salaireHoraireIntimaireBrut());
        boolean motifAutorise = request.motifMission()
                != InterimBeCct322MotifMission.AUTRE_NON_AUTORISE;
        boolean dureeRespectee = joursExcedentaires == 0;
        boolean pariteRespectee = ecartParite.compareTo(BigDecimal.ZERO) <= 0;
        boolean formalismeRespecte = request.contratEcritSigne()
                && request.dimonaDeclareeParEti();

        // ── Hiérarchie 1 : interdiction grève / lock-out (art. 19) ─────────────
        if (request.remplacementGreveOuLockout()) {
            String synthese = "Recours à l'intérim INTERDIT en application de"
                    + " l'art. 19 Loi 24/07/1987 : l'utilisateur invoque un"
                    + " remplacement de travailleur en grève ou lock-out."
                    + " Présomption irréfragable de fraude → requalification"
                    + " immédiate. " + SANCTION_REQUALIFICATION;
            return new InterimBeCct322Result(
                    request.dateDebutMission(),
                    request.motifMission(),
                    true,
                    request.dureeTotaleMissionJours(),
                    request.dureeMaxLegaleJours(),
                    request.salaireHoraireIntimaireBrut(),
                    request.salaireHoraireReferenceBrut(),
                    request.contratEcritSigne(),
                    request.dimonaDeclareeParEti(),
                    InterimBeCct322Verdict.INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT,
                    motifAutorise, dureeRespectee, pariteRespectee,
                    formalismeRespecte, joursExcedentaires,
                    ecartParite.max(BigDecimal.ZERO),
                    "MOTIF_INTERDIT_GREVE_LOCKOUT",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 2 : motif non autorisé ──────────────────────────────────
        if (request.motifMission()
                == InterimBeCct322MotifMission.AUTRE_NON_AUTORISE) {
            String synthese = "Motif invoqué non listé par les art. 1er § 1"
                    + " et 1bis de la Loi 24/07/1987 (remplacement, surcroît"
                    + " temporaire, travail exceptionnel AR 11/10/1976,"
                    + " insertion en vue d'embauche durable, motif"
                    + " artistique, flux exceptionnels AR sectoriels) →"
                    + " recours illégal à l'intérim. "
                    + SANCTION_REQUALIFICATION;
            return new InterimBeCct322Result(
                    request.dateDebutMission(),
                    request.motifMission(),
                    false,
                    request.dureeTotaleMissionJours(),
                    request.dureeMaxLegaleJours(),
                    request.salaireHoraireIntimaireBrut(),
                    request.salaireHoraireReferenceBrut(),
                    request.contratEcritSigne(),
                    request.dimonaDeclareeParEti(),
                    InterimBeCct322Verdict.INELIGIBLE_MOTIF_NON_AUTORISE,
                    false, dureeRespectee, pariteRespectee,
                    formalismeRespecte, joursExcedentaires,
                    ecartParite.max(BigDecimal.ZERO),
                    "MOTIF_NON_AUTORISE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 3 : zone grise artistique / flux exceptionnel ───────────
        if (request.motifMission()
                == InterimBeCct322MotifMission.MOTIF_ARTISTIQUE
                || request.motifMission()
                        == InterimBeCct322MotifMission.FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE) {
            String quelMotif = request.motifMission()
                    == InterimBeCct322MotifMission.MOTIF_ARTISTIQUE
                    ? "le motif artistique (art. 1bis § 4) — spectacle vivant,"
                            + " production audiovisuelle, intermittents du"
                            + " spectacle — relève d'un régime spécifique"
                            + " avec durée variable selon la production"
                    : "le motif de flux de travaux exceptionnels / trajet"
                            + " scolaire (cas étroits AR sectoriels)";
            String synthese = "Analyse au cas par cas requise : "
                    + quelMotif + ". La validité de la mission, la durée"
                    + " maximale applicable et le formalisme propre à la"
                    + " situation doivent être vérifiés par un avocat"
                    + " spécialisé en droit social BE avant de conclure.";
            return new InterimBeCct322Result(
                    request.dateDebutMission(),
                    request.motifMission(),
                    false,
                    request.dureeTotaleMissionJours(),
                    request.dureeMaxLegaleJours(),
                    request.salaireHoraireIntimaireBrut(),
                    request.salaireHoraireReferenceBrut(),
                    request.contratEcritSigne(),
                    request.dimonaDeclareeParEti(),
                    InterimBeCct322Verdict.A_ANALYSER,
                    true, dureeRespectee, pariteRespectee,
                    formalismeRespecte, joursExcedentaires,
                    ecartParite.max(BigDecimal.ZERO),
                    "MOTIF_ZONE_GRISE_ANALYSE_CAS_PAR_CAS",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 4 : durée max dépassée ─────────────────────────────────
        if (!dureeRespectee) {
            String synthese = "La durée totale cumulée de la mission ("
                    + request.dureeTotaleMissionJours() + " jours, contrats"
                    + " successifs inclus) dépasse le plafond légal"
                    + " applicable au motif " + libelleMotif(request.motifMission())
                    + " (" + request.dureeMaxLegaleJours() + " jours) de "
                    + joursExcedentaires + " jour(s). "
                    + SANCTION_REQUALIFICATION;
            return new InterimBeCct322Result(
                    request.dateDebutMission(),
                    request.motifMission(),
                    false,
                    request.dureeTotaleMissionJours(),
                    request.dureeMaxLegaleJours(),
                    request.salaireHoraireIntimaireBrut(),
                    request.salaireHoraireReferenceBrut(),
                    request.contratEcritSigne(),
                    request.dimonaDeclareeParEti(),
                    InterimBeCct322Verdict.INELIGIBLE_DUREE_MAX_DEPASSEE,
                    true, false, pariteRespectee,
                    formalismeRespecte, joursExcedentaires,
                    ecartParite.max(BigDecimal.ZERO),
                    "DUREE_MAX_DEPASSEE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 5 : parité salariale violée (art. 10) ──────────────────
        if (!pariteRespectee) {
            String synthese = "Parité salariale violée (art. 10 Loi 24/07/1987"
                    + " + CCT n° 322) : le salaire horaire intérimaire ("
                    + request.salaireHoraireIntimaireBrut() + " € brut) est"
                    + " inférieur de " + ecartParite + " € à celui d'un"
                    + " permanent de même qualification chez l'utilisateur"
                    + " (" + request.salaireHoraireReferenceBrut() + " €"
                    + " brut). L'utilisateur et l'ETI sont solidairement"
                    + " tenus de payer le rappel de salaire intégral +"
                    + " primes + cotisations ONSS sur la différence (CCT"
                    + " n° 322).";
            return new InterimBeCct322Result(
                    request.dateDebutMission(),
                    request.motifMission(),
                    false,
                    request.dureeTotaleMissionJours(),
                    request.dureeMaxLegaleJours(),
                    request.salaireHoraireIntimaireBrut(),
                    request.salaireHoraireReferenceBrut(),
                    request.contratEcritSigne(),
                    request.dimonaDeclareeParEti(),
                    InterimBeCct322Verdict.INELIGIBLE_PARITE_SALARIALE_VIOLEE,
                    true, true, false,
                    formalismeRespecte, joursExcedentaires,
                    ecartParite,
                    "PARITE_SALARIALE_VIOLEE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 6 : formalisme défaillant ──────────────────────────────
        if (!formalismeRespecte) {
            String quoi = !request.contratEcritSigne()
                    && !request.dimonaDeclareeParEti()
                    ? "contrat de travail écrit ETI/intérimaire absent ET"
                            + " déclaration Dimona ETI non effectuée"
                    : !request.contratEcritSigne()
                            ? "contrat de travail écrit ETI/intérimaire"
                                    + " absent (art. 8 § 1 Loi 24/07/1987)"
                            : "déclaration Dimona ETI non effectuée";
            String synthese = "Statut intérim formellement irrégulier : "
                    + quoi + ". Conditions de fond OK (motif autorisé,"
                    + " durée respectée, parité salariale OK) mais"
                    + " requalification automatique pour défaut de"
                    + " formalisme. " + SANCTION_REQUALIFICATION;
            return new InterimBeCct322Result(
                    request.dateDebutMission(),
                    request.motifMission(),
                    false,
                    request.dureeTotaleMissionJours(),
                    request.dureeMaxLegaleJours(),
                    request.salaireHoraireIntimaireBrut(),
                    request.salaireHoraireReferenceBrut(),
                    request.contratEcritSigne(),
                    request.dimonaDeclareeParEti(),
                    InterimBeCct322Verdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT,
                    true, true, true, false, joursExcedentaires,
                    BigDecimal.ZERO,
                    "CONTRAT_OU_DIMONA_MANQUANT",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Cas qualifiant : mission régulière ────────────────────────────────
        String synthese = "Mission intérimaire pleinement régulière au sens"
                + " de la Loi du 24/07/1987 et de la CCT n° 322 : motif "
                + libelleMotif(request.motifMission()) + ", aucune grève"
                + " ni lock-out, durée totale (" + request.dureeTotaleMissionJours()
                + " jours) ≤ plafond légal (" + request.dureeMaxLegaleJours()
                + " jours), parité salariale respectée (intérimaire "
                + request.salaireHoraireIntimaireBrut() + " € ≥ référence "
                + request.salaireHoraireReferenceBrut() + " €), contrat"
                + " écrit ETI/intérimaire signé, Dimona ETI déclarée.";
        return new InterimBeCct322Result(
                request.dateDebutMission(),
                request.motifMission(),
                false,
                request.dureeTotaleMissionJours(),
                request.dureeMaxLegaleJours(),
                request.salaireHoraireIntimaireBrut(),
                request.salaireHoraireReferenceBrut(),
                request.contratEcritSigne(),
                request.dimonaDeclareeParEti(),
                InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE,
                true, true, true, true, 0,
                BigDecimal.ZERO,
                "MISSION_REGULIERE",
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static String libelleMotif(InterimBeCct322MotifMission m) {
        return switch (m) {
            case REMPLACEMENT_TRAVAILLEUR_PERMANENT -> "remplacement d'un"
                    + " travailleur permanent (art. 1er § 1, 1°)";
            case SURCROIT_TEMPORAIRE_DE_TRAVAIL -> "surcroît temporaire de"
                    + " travail (art. 1er § 1, 2°)";
            case EXECUTION_TRAVAIL_EXCEPTIONNEL -> "exécution d'un travail"
                    + " exceptionnel (art. 1er § 1, 3° + AR 11/10/1976)";
            case INSERTION_EMBAUCHE_DURABLE -> "insertion en vue d'embauche"
                    + " durable (art. 1bis + CCT n° 108)";
            case MOTIF_ARTISTIQUE -> "motif artistique (art. 1bis § 4)";
            case FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE -> "flux de"
                    + " travaux exceptionnels / trajet scolaire (AR sectoriels)";
            case AUTRE_NON_AUTORISE -> "autre motif non autorisé";
        };
    }

    private static void validateInputs(InterimBeCct322Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateDebutMission() == null) {
            throw new IllegalArgumentException(
                    "dateDebutMission est requise");
        }
        if (request.motifMission() == null) {
            throw new IllegalArgumentException(
                    "motifMission est requis");
        }
        if (request.remplacementGreveOuLockout() == null) {
            throw new IllegalArgumentException(
                    "remplacementGreveOuLockout est requis");
        }
        if (request.dureeTotaleMissionJours() == null) {
            throw new IllegalArgumentException(
                    "dureeTotaleMissionJours est requise");
        }
        if (request.dureeTotaleMissionJours() < 0) {
            throw new IllegalArgumentException(
                    "dureeTotaleMissionJours doit être positive ou nulle");
        }
        if (request.dureeMaxLegaleJours() == null) {
            throw new IllegalArgumentException(
                    "dureeMaxLegaleJours est requise");
        }
        if (request.dureeMaxLegaleJours() <= 0) {
            throw new IllegalArgumentException(
                    "dureeMaxLegaleJours doit être strictement positive");
        }
        if (request.salaireHoraireIntimaireBrut() == null) {
            throw new IllegalArgumentException(
                    "salaireHoraireIntimaireBrut est requis");
        }
        if (request.salaireHoraireIntimaireBrut()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "salaireHoraireIntimaireBrut doit être positif ou nul");
        }
        if (request.salaireHoraireReferenceBrut() == null) {
            throw new IllegalArgumentException(
                    "salaireHoraireReferenceBrut est requis");
        }
        if (request.salaireHoraireReferenceBrut()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "salaireHoraireReferenceBrut doit être strictement positif");
        }
        if (request.contratEcritSigne() == null) {
            throw new IllegalArgumentException(
                    "contratEcritSigne est requis");
        }
        if (request.dimonaDeclareeParEti() == null) {
            throw new IllegalArgumentException(
                    "dimonaDeclareeParEti est requis");
        }
    }
}
