package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * SF-207-08 : calculateur de conformité de l'<b>outplacement obligatoire
 * pour salariés 45+ ans</b> en droit belge.
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>CCT n°82</b> — outplacement obligatoire pour salariés ≥ 45 ans,
 *       ≥ 1 an d'ancienneté, licenciés (hors faute grave). Offre par écrit
 *       dans les 15 jours du licenciement ; programme de 60 heures sur
 *       12 mois maximum.</li>
 *   <li><b>CCT n°82 bis</b> — variante temps partiel (≥ mi-temps) du même
 *       régime.</li>
 *   <li><b>Loi du 5 septembre 2001 art. 13</b> — base légale, sanction
 *       administrative.</li>
 *   <li><b>AR du 30 mai 2018</b> — sanction administrative employeur
 *       1 800 € (amende forfaitaire) en cas de non-offre ou de non-conformité
 *       de l'offre.</li>
 *   <li><b>AR du 25 novembre 1991 art. 154</b> — sanction salarié 4 à
 *       52 semaines d'exclusion des allocations de chômage en cas de refus
 *       d'une offre conforme.</li>
 * </ul>
 *
 * <p>Logique (5 verdicts conditionnels, priorité de haut en bas) :
 * <ol>
 *   <li>Si âge &lt; 45 → {@link OutplacementBeResult.Verdict#OUTPLACEMENT_NON_DU}
 *       + raison {@link OutplacementBeRaisonNonObligation#AGE_INFERIEUR_45}.</li>
 *   <li>Si ancienneté &lt; 1 an →
 *       {@link OutplacementBeResult.Verdict#OUTPLACEMENT_NON_DU} + raison
 *       {@link OutplacementBeRaisonNonObligation#ANCIENNETE_INFERIEURE_1AN}.</li>
 *   <li>Si motif = FAUTE_GRAVE →
 *       {@link OutplacementBeResult.Verdict#OUTPLACEMENT_NON_DU} + raison
 *       {@link OutplacementBeRaisonNonObligation#FAUTE_GRAVE}.</li>
 *   <li>Si motif = DEMISSION →
 *       {@link OutplacementBeResult.Verdict#OUTPLACEMENT_NON_DU} + raison
 *       {@link OutplacementBeRaisonNonObligation#DEMISSION}.</li>
 *   <li>Sinon obligation applicable :
 *     <ul>
 *       <li>{@code offreRecue=false} →
 *           {@link OutplacementBeResult.Verdict#OFFRE_NON_FAITE_SANCTION_EMPLOYEUR}
 *           + sanction 1 800 € + étape PLAINTE_INSPECTION_LOIS_SOCIALES ;</li>
 *       <li>{@code offreRecue=true} + ({@code offreConforme=false} OU
 *           {@code dateOffre &gt; dateLimiteOffre}) →
 *           {@link OutplacementBeResult.Verdict#OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR}
 *           + sanction 1 800 € + étape PLAINTE_INSPECTION_LOIS_SOCIALES ;</li>
 *       <li>{@code offreRecue=true} + {@code offreConforme=true} + délai OK +
 *           {@code salarieAcceptantOffre=false} →
 *           {@link OutplacementBeResult.Verdict#OFFRE_REFUSEE_SANCTION_SALARIE}
 *           + range 4-52 semaines + étape PRESENTATION_C4_ONEM ;</li>
 *       <li>{@code offreRecue=true} + {@code offreConforme=true} + délai OK +
 *           ({@code salarieAcceptantOffre=true} OU null) →
 *           {@link OutplacementBeResult.Verdict#OFFRE_CONFORME_RESPECTEE} +
 *           sanction 0 € + étape AUCUNE.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>Sanction employeur en {@link BigDecimal} (précision centimes,
 * {@link RoundingMode#HALF_EVEN}, 2 décimales) — conformément au plan de
 * test de la mini-spec et aux conventions du projet.</p>
 *
 * <p>La date limite de l'offre = {@code dateLicenciement + 15 jours}
 * calendaires. Le fuseau {@code Europe/Brussels} est cohérent avec les
 * autres outils BE — toutefois le calcul porte sur des
 * {@link LocalDate} (sans heure), donc le fuseau ne modifie pas le
 * résultat ; il est cité pour traçabilité juridique.</p>
 *
 * <p>Outil <b>BE-only</b> par construction — aucun équivalent FR direct
 * (mémoire {@code feedback_belgique_never_forget}). Les régimes français
 * d'aide au reclassement — Contrat de Sécurisation Professionnelle CSP,
 * congé reclassement — sont des dispositifs distincts.</p>
 */
public final class OutplacementBeCalculator {

    /** Âge plancher déclenchant l'obligation d'outplacement (CCT 82). */
    public static final int AGE_MINIMUM_OBLIGATION = 45;

    /** Ancienneté plancher déclenchant l'obligation d'outplacement (CCT 82). */
    public static final BigDecimal ANCIENNETE_MINIMUM_ANNEES = new BigDecimal("1.00");

    /** Délai pour faire l'offre = 15 jours calendaires (CCT 82). */
    public static final int DELAI_OFFRE_JOURS_CALENDAIRES = 15;

    /** Sanction employeur en cas de non-offre / non-conformité (AR 30/05/2018). */
    public static final BigDecimal SANCTION_EMPLOYEUR_EUROS = new BigDecimal("1800.00");

    /** Fourchette d'exclusion ONEM en cas de refus d'offre conforme (AR 25/11/1991 art. 154). */
    public static final int SANCTION_SALARIE_MIN_SEMAINES = 4;
    public static final int SANCTION_SALARIE_MAX_SEMAINES = 52;

    /** Précision monétaire (centimes). */
    public static final int SCALE_MONETAIRE = 2;

    /** Mode d'arrondi monétaire — HALF_EVEN (banker's rounding). */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static final String BASE_JURIDIQUE_COMPLETE =
            "CCT 82 ; CCT 82 bis ; loi du 5 septembre 2001 art. 13 ; "
                    + "AR 30/05/2018 (sanction administrative employeur) ; "
                    + "AR 25/11/1991 art. 154 (sanction salarié)";

    private OutplacementBeCalculator() {
    }

    /**
     * Calcule la conformité de l'outplacement pour les inputs donnés.
     * Fonction pure : aucun effet de bord, déterministe à inputs égaux.
     *
     * @param request payload (les contrôles Bean Validation et inter-champs
     *                ont déjà été passés en amont par le service ; le
     *                calculateur valide en plus la non-nullité défensive).
     * @return résultat structuré (verdict + sanctions + dates + base juridique
     *         + formule de calcul).
     * @throws IllegalArgumentException si {@code request} est null ou si un
     *         champ requis est manquant (défense en profondeur — Bean
     *         Validation amont).
     */
    public static OutplacementBeResult compute(OutplacementBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête outplacement BE requise");
        }
        if (request.dateLicenciement() == null) {
            throw new IllegalArgumentException("dateLicenciement est requise");
        }
        if (request.dateNaissanceSalarie() == null) {
            throw new IllegalArgumentException("dateNaissanceSalarie est requise");
        }
        if (request.ancienneteAnnees() == null) {
            throw new IllegalArgumentException("ancienneteAnnees est requise");
        }
        if (request.motifLicenciement() == null) {
            throw new IllegalArgumentException("motifLicenciement est requis");
        }
        if (request.contratTempsPlein() == null) {
            throw new IllegalArgumentException("contratTempsPlein est requis");
        }
        if (request.offreOutplacementRecue() == null) {
            throw new IllegalArgumentException("offreOutplacementRecue est requise");
        }

        LocalDate dateLicenciement = request.dateLicenciement();
        LocalDate dateNaissance = request.dateNaissanceSalarie();
        BigDecimal anciennete = request.ancienneteAnnees().setScale(SCALE_MONETAIRE, ROUNDING);
        OutplacementBeMotifLicenciement motif = request.motifLicenciement();
        boolean contratTempsPlein = request.contratTempsPlein();
        boolean offreRecue = request.offreOutplacementRecue();
        LocalDate dateOffre = request.dateOffreOutplacement();
        Boolean offreConforme = request.offreConformeCCT82();
        Boolean salarieAccepte = request.salarieAcceptantOffre();

        // 1) Âge en années pleines à la date du licenciement
        int age = computeAgePleines(dateNaissance, dateLicenciement);

        // 2) Date limite de l'offre = dateLicenciement + 15 jours
        LocalDate dateLimiteOffre = dateLicenciement.plusDays(DELAI_OFFRE_JOURS_CALENDAIRES);

        // 3) Tests d'exclusion de l'obligation (priorité de haut en bas)
        OutplacementBeRaisonNonObligation raisonNonOblig = null;
        if (age < AGE_MINIMUM_OBLIGATION) {
            raisonNonOblig = OutplacementBeRaisonNonObligation.AGE_INFERIEUR_45;
        } else if (anciennete.compareTo(ANCIENNETE_MINIMUM_ANNEES) < 0) {
            raisonNonOblig = OutplacementBeRaisonNonObligation.ANCIENNETE_INFERIEURE_1AN;
        } else if (motif == OutplacementBeMotifLicenciement.FAUTE_GRAVE) {
            raisonNonOblig = OutplacementBeRaisonNonObligation.FAUTE_GRAVE;
        } else if (motif == OutplacementBeMotifLicenciement.DEMISSION) {
            raisonNonOblig = OutplacementBeRaisonNonObligation.DEMISSION;
        }

        if (raisonNonOblig != null) {
            return buildResultNonDu(request, age, anciennete, dateLimiteOffre, raisonNonOblig);
        }

        // 4) Obligation applicable — analyser l'état de l'offre
        if (!offreRecue) {
            return buildResultOffreNonFaite(request, age, anciennete, dateLimiteOffre);
        }

        // Offre reçue — délai respecté ?
        Boolean delaiOffreRespecte = dateOffre != null ? !dateOffre.isAfter(dateLimiteOffre) : null;
        boolean delaiHorsBornes = dateOffre != null && dateOffre.isAfter(dateLimiteOffre);
        boolean offreNonConforme = Boolean.FALSE.equals(offreConforme);

        if (delaiHorsBornes || offreNonConforme) {
            return buildResultOffreNonConforme(request, age, anciennete,
                    dateLimiteOffre, delaiOffreRespecte);
        }

        // Offre conforme — refusée ou acceptée/null
        if (Boolean.FALSE.equals(salarieAccepte)) {
            return buildResultOffreRefusee(request, age, anciennete,
                    dateLimiteOffre, delaiOffreRespecte);
        }

        return buildResultOffreConforme(request, age, anciennete,
                dateLimiteOffre, delaiOffreRespecte);
    }

    // =========================================================================
    // Helpers — construction des résultats
    // =========================================================================

    private static OutplacementBeResult buildResultNonDu(OutplacementBeRequest req,
                                                        int age,
                                                        BigDecimal anciennete,
                                                        LocalDate dateLimiteOffre,
                                                        OutplacementBeRaisonNonObligation raisonNonOblig) {
        String formule = buildFormuleNonDu(age, anciennete, req.motifLicenciement(), raisonNonOblig);
        return new OutplacementBeResult(
                req.dateLicenciement(),
                req.dateNaissanceSalarie(),
                anciennete,
                req.motifLicenciement(),
                Boolean.TRUE.equals(req.contratTempsPlein()),
                Boolean.TRUE.equals(req.offreOutplacementRecue()),
                req.dateOffreOutplacement(),
                req.offreConformeCCT82(),
                req.salarieAcceptantOffre(),
                OutplacementBeResult.Verdict.OUTPLACEMENT_NON_DU,
                age,
                false,
                raisonNonOblig,
                BigDecimal.ZERO.setScale(SCALE_MONETAIRE, ROUNDING),
                null,
                dateLimiteOffre,
                null,
                OutplacementBeResult.EtapeSuivante.AUCUNE,
                BASE_JURIDIQUE_COMPLETE,
                formule
        );
    }

    private static OutplacementBeResult buildResultOffreNonFaite(OutplacementBeRequest req,
                                                                 int age,
                                                                 BigDecimal anciennete,
                                                                 LocalDate dateLimiteOffre) {
        String formule = buildFormuleObligationApplicable(age, anciennete, req.motifLicenciement(),
                req.contratTempsPlein())
                + " Offre non reçue → sanction employeur 1 800 € (AR 30/05/2018).";
        return new OutplacementBeResult(
                req.dateLicenciement(),
                req.dateNaissanceSalarie(),
                anciennete,
                req.motifLicenciement(),
                Boolean.TRUE.equals(req.contratTempsPlein()),
                false,
                null,
                null,
                null,
                OutplacementBeResult.Verdict.OFFRE_NON_FAITE_SANCTION_EMPLOYEUR,
                age,
                true,
                null,
                SANCTION_EMPLOYEUR_EUROS,
                null,
                dateLimiteOffre,
                null,
                OutplacementBeResult.EtapeSuivante.PLAINTE_INSPECTION_LOIS_SOCIALES,
                BASE_JURIDIQUE_COMPLETE,
                formule
        );
    }

    private static OutplacementBeResult buildResultOffreNonConforme(OutplacementBeRequest req,
                                                                    int age,
                                                                    BigDecimal anciennete,
                                                                    LocalDate dateLimiteOffre,
                                                                    Boolean delaiOffreRespecte) {
        String motifNonConformite = (Boolean.FALSE.equals(delaiOffreRespecte))
                ? "Offre tardive (au-delà du délai de 15 jours)"
                : "Offre non conforme aux exigences CCT 82 (60 h/12 mois, écrit, etc.)";
        String formule = buildFormuleObligationApplicable(age, anciennete, req.motifLicenciement(),
                req.contratTempsPlein())
                + " " + motifNonConformite + " → sanction employeur 1 800 € (AR 30/05/2018).";
        return new OutplacementBeResult(
                req.dateLicenciement(),
                req.dateNaissanceSalarie(),
                anciennete,
                req.motifLicenciement(),
                Boolean.TRUE.equals(req.contratTempsPlein()),
                true,
                req.dateOffreOutplacement(),
                req.offreConformeCCT82(),
                req.salarieAcceptantOffre(),
                OutplacementBeResult.Verdict.OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR,
                age,
                true,
                null,
                SANCTION_EMPLOYEUR_EUROS,
                null,
                dateLimiteOffre,
                delaiOffreRespecte,
                OutplacementBeResult.EtapeSuivante.PLAINTE_INSPECTION_LOIS_SOCIALES,
                BASE_JURIDIQUE_COMPLETE,
                formule
        );
    }

    private static OutplacementBeResult buildResultOffreRefusee(OutplacementBeRequest req,
                                                                int age,
                                                                BigDecimal anciennete,
                                                                LocalDate dateLimiteOffre,
                                                                Boolean delaiOffreRespecte) {
        String formule = buildFormuleObligationApplicable(age, anciennete, req.motifLicenciement(),
                req.contratTempsPlein())
                + " Offre conforme refusée par le salarié → sanction salarié "
                + SANCTION_SALARIE_MIN_SEMAINES + " à " + SANCTION_SALARIE_MAX_SEMAINES
                + " semaines d'exclusion ONEM (AR 25/11/1991 art. 154).";
        return new OutplacementBeResult(
                req.dateLicenciement(),
                req.dateNaissanceSalarie(),
                anciennete,
                req.motifLicenciement(),
                Boolean.TRUE.equals(req.contratTempsPlein()),
                true,
                req.dateOffreOutplacement(),
                req.offreConformeCCT82(),
                req.salarieAcceptantOffre(),
                OutplacementBeResult.Verdict.OFFRE_REFUSEE_SANCTION_SALARIE,
                age,
                true,
                null,
                BigDecimal.ZERO.setScale(SCALE_MONETAIRE, ROUNDING),
                new OutplacementBeResult.SanctionSalarieRange(
                        SANCTION_SALARIE_MIN_SEMAINES, SANCTION_SALARIE_MAX_SEMAINES),
                dateLimiteOffre,
                delaiOffreRespecte,
                OutplacementBeResult.EtapeSuivante.PRESENTATION_C4_ONEM,
                BASE_JURIDIQUE_COMPLETE,
                formule
        );
    }

    private static OutplacementBeResult buildResultOffreConforme(OutplacementBeRequest req,
                                                                 int age,
                                                                 BigDecimal anciennete,
                                                                 LocalDate dateLimiteOffre,
                                                                 Boolean delaiOffreRespecte) {
        String formule = buildFormuleObligationApplicable(age, anciennete, req.motifLicenciement(),
                req.contratTempsPlein())
                + " Offre conforme dans le délai → outplacement respecté, aucune sanction.";
        return new OutplacementBeResult(
                req.dateLicenciement(),
                req.dateNaissanceSalarie(),
                anciennete,
                req.motifLicenciement(),
                Boolean.TRUE.equals(req.contratTempsPlein()),
                true,
                req.dateOffreOutplacement(),
                req.offreConformeCCT82(),
                req.salarieAcceptantOffre(),
                OutplacementBeResult.Verdict.OFFRE_CONFORME_RESPECTEE,
                age,
                true,
                null,
                BigDecimal.ZERO.setScale(SCALE_MONETAIRE, ROUNDING),
                null,
                dateLimiteOffre,
                delaiOffreRespecte,
                OutplacementBeResult.EtapeSuivante.AUCUNE,
                BASE_JURIDIQUE_COMPLETE,
                formule
        );
    }

    // =========================================================================
    // Helpers — divers
    // =========================================================================

    /**
     * Calcule l'âge en années pleines à la date de licenciement.
     *
     * <p>Logique : si le mois/jour de naissance est postérieur au mois/jour
     * du licenciement, le salarié n'a pas encore eu son anniversaire à cette
     * date, on retranche 1 à la différence d'années.</p>
     */
    private static int computeAgePleines(LocalDate dateNaissance, LocalDate dateLicenciement) {
        return java.time.Period.between(dateNaissance, dateLicenciement).getYears();
    }

    /**
     * Construit la formule textuelle pour le cas OUTPLACEMENT_NON_DU.
     */
    private static String buildFormuleNonDu(int age, BigDecimal anciennete,
                                            OutplacementBeMotifLicenciement motif,
                                            OutplacementBeRaisonNonObligation raison) {
        StringBuilder sb = new StringBuilder();
        sb.append("Salarié ").append(age).append(" ans, ancienneté ").append(anciennete).append(" ans, ");
        sb.append("motif ").append(motif).append(" → outplacement non dû (raison : ").append(raison).append(").");
        switch (raison) {
            case AGE_INFERIEUR_45 -> sb.append(" Seuil CCT 82 : âge ≥ 45 ans.");
            case ANCIENNETE_INFERIEURE_1AN -> sb.append(" Seuil CCT 82 : ancienneté ≥ 1 an.");
            case FAUTE_GRAVE -> sb.append(" Faute grave (art. 35 Loi 03/07/1978) exclut l'obligation employeur.");
            case DEMISSION -> sb.append(" Démission salarié — l'obligation employeur ne joue pas.");
        }
        return sb.toString();
    }

    /**
     * Construit le préambule textuel commun à tous les verdicts où
     * l'obligation est applicable.
     */
    private static String buildFormuleObligationApplicable(int age, BigDecimal anciennete,
                                                           OutplacementBeMotifLicenciement motif,
                                                           Boolean contratTempsPlein) {
        boolean tempsPlein = Boolean.TRUE.equals(contratTempsPlein);
        String regime = tempsPlein ? "CCT 82" : "CCT 82 bis (temps partiel)";
        return "Salarié " + age + " ans (≥ 45 ✓), ancienneté " + anciennete
                + " ans (≥ 1 ✓), motif " + motif + " (≠ faute grave / démission ✓) → outplacement obligatoire au titre de "
                + regime + ".";
    }
}
