package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-DT-38-01 : moteur de qualification d'une rupture pendant la période d'essai (FR).
 *
 * <p>Analyse 12 critères factuels (durée légale L.1221-19, renouvellement L.1221-23,
 * délai de prévenance L.1221-25, motif lié aux compétences, protections L.1132-1 /
 * L.1225-1 / L.1226-9, atteinte liberté fondamentale, dispositions CCN plus
 * favorables…) et rend un verdict en 4 niveaux piloté par la priorité des
 * anomalies (NULLE > ILLEGALE_REQUALIF_LICENCIEMENT > RISQUE_ABUSIVE > REGULIERE).</p>
 *
 * <p><b>Sources</b> : mail Marjolaine RENVERSEZ 19/05/2026 18:38 + jurisprudence
 * Cass. soc. (20/11/2007 — motif lié aux compétences ; 13/09/2017 — fourchette
 * indemnité abus). Précisions Marjolaine :
 * <ul>
 *   <li>Verdict NULLE : option réintégration mise en avant (pas de plancher 6 mois
 *   L.1235-3-1) — dommages et intérêts subsidiaires.</li>
 *   <li>Verdict ILLEGALE → RISQUE_ABUSIVE (atténuation) si lettre de rupture
 *   motivée + motifs avérés par les pièces.</li>
 *   <li>Indemnité abus : fourchette 1 à 6 mois de salaire.</li>
 * </ul></p>
 *
 * <p><b>Pays</b> : FRANCE uniquement. La rupture en phase initiale du contrat BE
 * (post-statut unique 2014) relève d'un régime distinct — feature jumelle F-DT-39
 * au backlog post-livraison FR si signal terrain BE.</p>
 *
 * <p><b>Validation juridique requise</b> : durées légales L.1221-19, échelle de
 * prévenance L.1221-25, fourchette indemnitaire abus et liste des protections
 * sont centralisées ici (source unique) et doivent être relues par un avocat
 * avant mise en production.</p>
 */
public final class RupturePeriodeEssaiCalculator {

    /** Verdict de qualification 4 niveaux. */
    public enum Verdict {
        REGULIERE,                       // tous les critères respectés
        RISQUE_ABUSIVE,                  // motif détourné, non-respect prévenance, légèreté blâmable
        NULLE,                           // discrimination, grossesse, AT/MP, liberté fondamentale
        ILLEGALE_REQUALIF_LICENCIEMENT   // durée essai > légale OU renouvellement irrégulier
    }

    /** Gravité d'une anomalie détectée. */
    public enum Gravite {
        AVERE,
        PROBABLE
    }

    /** Code structuré d'une anomalie. */
    public enum CodeAnomalie {
        PERIODE_ESSAI_ABSENTE,
        DUREE_ESSAI_DEPASSEE,
        RENOUVELLEMENT_IRREGULIER,
        RUPTURE_HORS_PERIODE_ESSAI,
        DELAI_PREVENANCE_INSUFFISANT,
        MOTIF_NON_PROFESSIONNEL,
        MOTIF_ETRANGER_A_ESSAI,
        DISCRIMINATION_AVEREE,
        GROSSESSE_PROTECTION_VIOLEE,
        AT_MP_PROTECTION_VIOLEE,
        ATTEINTE_LIBERTE_FONDAMENTALE,
        CONVENTION_COLLECTIVE_NON_RESPECTEE
    }

    /** Catégorie socio-professionnelle déterminant la durée légale L.1221-19. */
    public enum CategorieSocioProfessionnelle {
        OUVRIER_EMPLOYE,
        AGENT_MAITRISE_TECHNICIEN,
        CADRE
    }

    /** Type de contrat. */
    public enum TypeContrat {
        CDI,
        CDD,
        INTERIM
    }

    /** Auteur de la rupture. */
    public enum AuteurRupture {
        EMPLOYEUR,
        SALARIE
    }

    /** Motif de discrimination invoqué (subset L.1132-1). */
    public enum DiscriminationMotif {
        RACE_ORIGINE,
        SEXE,
        GROSSESSE,
        SANTE,
        SYNDICAL,
        AUTRE
    }

    /** Anomalie détectée — structure exposée dans la réponse API. */
    public record Anomalie(
            CodeAnomalie code,
            String libelle,
            String fondement,
            Gravite gravite,
            String explication
    ) {}

    /** Indemnité estimée (verdict RISQUE_ABUSIVE) — fourchette CPH 1 à 6 mois. */
    public record IndemniteEstimee(
            Double montantMinEuros,
            Double montantMaxEuros,
            String baseCalcul,
            String fondement
    ) {}

    // ----------------------------------------------------------------------
    // Constantes juridiques L.1221-19 / L.1221-25
    // ----------------------------------------------------------------------

    /** Durée légale max CDI ouvrier/employé (mois). */
    private static final int CDI_OE_DUREE_MOIS = 2;
    /** Durée légale max CDI agent de maîtrise / technicien (mois). */
    private static final int CDI_AM_DUREE_MOIS = 3;
    /** Durée légale max CDI cadre (mois). */
    private static final int CDI_CADRE_DUREE_MOIS = 4;

    /** Délai prévenance employeur : < 8 jours = 24 h. */
    private static final int PREVENANCE_EMP_SOUS_8J_JOURS = 1;
    /** Délai prévenance employeur : ≥ 8 jours, < 1 mois = 48 h. */
    private static final int PREVENANCE_EMP_SOUS_1M_JOURS = 2;
    /** Délai prévenance employeur : ≥ 1 mois, < 3 mois = 2 semaines. */
    private static final int PREVENANCE_EMP_SOUS_3M_JOURS = 14;
    /** Délai prévenance employeur : ≥ 3 mois = 1 mois. */
    private static final int PREVENANCE_EMP_GE_3M_JOURS = 30;

    /** Délai prévenance salarié : < 8 jours = 24 h. */
    private static final int PREVENANCE_SAL_SOUS_8J_JOURS = 1;
    /** Délai prévenance salarié : ≥ 8 jours = 48 h. */
    private static final int PREVENANCE_SAL_GE_8J_JOURS = 2;

    /** Fourchette indemnité abus (CPH avant barème Macron). */
    private static final double INDEM_ABUS_MIN_MOIS = 1.0;
    private static final double INDEM_ABUS_MAX_MOIS = 6.0;

    /** Pondération anomalies pour le score indicatif. */
    private static final int POIDS_ANOMALIE_AVEREE = 30;
    private static final int POIDS_ANOMALIE_PROBABLE = 20;
    private static final int SCORE_MAX = 100;

    private RupturePeriodeEssaiCalculator() {}

    /**
     * Analyse la rupture de période d'essai.
     *
     * @param input   données saisies / pré-remplies par l'IA
     * @param country pays du workspace ("FRANCE" uniquement supporté)
     * @return résultat structuré (anomalies, score, verdict, indemnité, bases juridiques)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static RupturePeriodeEssaiResult compute(RupturePeriodeEssaiInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — pendant BE = feature jumelle F-DT-39 backlog");
        }
        if (input.categorieSocioProfessionnelle() == null) {
            throw new IllegalArgumentException("Catégorie socio-professionnelle requise (L.1221-19)");
        }
        if (input.typeContrat() == null) {
            throw new IllegalArgumentException("Type de contrat requis (CDI / CDD / INTERIM)");
        }
        if (input.dateDebutContrat() == null) {
            throw new IllegalArgumentException("Date de début de contrat requise");
        }
        if (input.dateRupture() == null) {
            throw new IllegalArgumentException("Date de rupture requise");
        }
        if (input.dureePeriodeEssaiContractuelleMois() == null) {
            throw new IllegalArgumentException("Durée de période d'essai contractuelle requise");
        }
        if (input.auteurRupture() == null) {
            throw new IllegalArgumentException("Auteur de la rupture requis (EMPLOYEUR / SALARIE)");
        }
        validateCommentaire(input.motifInvoque(), "motifInvoque", 1000);
        validateCommentaire(input.atteinteLiberteFondamentale(), "atteinteLiberteFondamentale", 500);

        long ancienneteJours = ChronoUnit.DAYS.between(input.dateDebutContrat(), input.dateRupture());
        int dureeLegaleMois = dureeLegaleMaximaleMois(
                input.categorieSocioProfessionnelle(),
                input.typeContrat(),
                input.dureeCddMois());
        int delaiPrevenanceLegalJours = delaiPrevenanceLegalJours(input.auteurRupture(), ancienneteJours);
        boolean delaiPrevenanceRespecte = delaiPrevenanceRespecte(
                input.delaiPrevenanceJoursAppliques(), delaiPrevenanceLegalJours);

        List<Anomalie> anomalies = detecterAnomalies(input, ancienneteJours, dureeLegaleMois,
                delaiPrevenanceRespecte);
        int score = scorePour(anomalies);
        Verdict verdict = verdictPour(input, anomalies);
        boolean remedeReintegration = (verdict == Verdict.NULLE);

        IndemniteEstimee indemnite = indemniteEstimee(verdict, input.salaireMensuelBrut());
        List<String> bases = basesJuridiques(anomalies);
        List<String> messages = construireMessages(input, anomalies, verdict, remedeReintegration);

        return new RupturePeriodeEssaiResult(
                List.copyOf(anomalies),
                score,
                verdict,
                (int) ancienneteJours,
                dureeLegaleMois,
                delaiPrevenanceLegalJours,
                delaiPrevenanceRespecte,
                indemnite,
                remedeReintegration,
                bases,
                messages,
                countryNormalized
        );
    }

    // ----------------------------------------------------------------------
    // Validation
    // ----------------------------------------------------------------------

    private static void validateCommentaire(String commentaire, String champ, int maxLen) {
        if (commentaire != null && commentaire.length() > maxLen) {
            throw new IllegalArgumentException(
                    "Le champ " + champ + " ne peut dépasser " + maxLen + " caractères");
        }
    }

    // ----------------------------------------------------------------------
    // Détection des anomalies (12 critères)
    // ----------------------------------------------------------------------

    private static List<Anomalie> detecterAnomalies(RupturePeriodeEssaiInput in,
                                                    long ancienneteJours,
                                                    int dureeLegaleMois,
                                                    boolean delaiPrevenanceRespecte) {
        Set<CodeAnomalie> codes = new LinkedHashSet<>();
        List<Anomalie> anomalies = new ArrayList<>();

        // 1 — PERIODE_ESSAI_ABSENTE (hors scope, message uniquement)
        if (in.dureePeriodeEssaiContractuelleMois() <= 0) {
            // Pas d'anomalie ajoutée — message construit dans construireMessages.
            // Cas dégénéré : aucune anomalie, verdict REGULIERE avec message hors scope.
            return List.of();
        }

        // 2 — DUREE_ESSAI_DEPASSEE (L.1221-19)
        if (in.dureePeriodeEssaiContractuelleMois() > dureeLegaleMois) {
            codes.add(CodeAnomalie.DUREE_ESSAI_DEPASSEE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.DUREE_ESSAI_DEPASSEE,
                    "Durée de la période d'essai contractuelle supérieure à la durée légale",
                    "Art. L.1221-19 C. trav.",
                    Gravite.AVERE,
                    "La durée contractuelle (" + in.dureePeriodeEssaiContractuelleMois()
                            + " mois) excède la durée légale maximale (" + dureeLegaleMois
                            + " mois) pour cette catégorie / type de contrat. "
                            + "La rupture s'analyse comme un licenciement sans cause réelle "
                            + "et sérieuse — barème Macron L.1235-3 applicable, sauf lettre "
                            + "de rupture motivée avec motifs avérés."));
        }

        // 3 — RENOUVELLEMENT_IRREGULIER (L.1221-23)
        if (Boolean.TRUE.equals(in.renouvellementInvoque())) {
            boolean branche = Boolean.TRUE.equals(in.accordBrancheRenouvellement());
            boolean salarie = Boolean.TRUE.equals(in.accordEcritSalarieRenouvellement());
            if (!branche || !salarie) {
                codes.add(CodeAnomalie.RENOUVELLEMENT_IRREGULIER);
                anomalies.add(new Anomalie(
                        CodeAnomalie.RENOUVELLEMENT_IRREGULIER,
                        "Renouvellement de la période d'essai irrégulier",
                        "Art. L.1221-23 C. trav.",
                        Gravite.AVERE,
                        "Le renouvellement de la période d'essai exige cumulativement "
                                + "(a) un accord de branche le prévoyant ET (b) un accord exprès "
                                + "écrit du salarié dans la durée initiale de l'essai. "
                                + "Le renouvellement tacite est interdit (Cass. soc., 23/09/2009)."));
            }
        }

        // 4 — RUPTURE_HORS_PERIODE_ESSAI (L.1221-25)
        int dureeEffectiveMois = dureeEffectiveEssaiMois(in);
        LocalDate finEssai = in.dateDebutContrat().plusMonths(dureeEffectiveMois);
        if (in.dateRupture().isAfter(finEssai)) {
            codes.add(CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI);
            anomalies.add(new Anomalie(
                    CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI,
                    "Date de rupture postérieure à l'expiration de la période d'essai",
                    "Art. L.1221-25 C. trav.",
                    Gravite.AVERE,
                    "La rupture a eu lieu le " + in.dateRupture()
                            + ", soit après l'expiration de la période d'essai effective ("
                            + finEssai + "). Le régime de la rupture d'essai n'est plus applicable "
                            + "— utiliser l'outil F-DT-08 (validité du licenciement)."));
        }

        // 5 — DELAI_PREVENANCE_INSUFFISANT (L.1221-25)
        if (!delaiPrevenanceRespecte) {
            codes.add(CodeAnomalie.DELAI_PREVENANCE_INSUFFISANT);
            anomalies.add(new Anomalie(
                    CodeAnomalie.DELAI_PREVENANCE_INSUFFISANT,
                    "Délai de prévenance non respecté selon l'échelle L.1221-25",
                    "Art. L.1221-25 C. trav.",
                    Gravite.AVERE,
                    "Le délai de prévenance effectivement appliqué ("
                            + (in.delaiPrevenanceJoursAppliques() == null
                                    ? "non renseigné"
                                    : in.delaiPrevenanceJoursAppliques() + " jours")
                            + ") est inférieur au délai légal requis "
                            + "pour cet auteur et cette ancienneté. "
                            + "Le non-respect ouvre droit à indemnisation du préavis non exécuté."));
        }

        // 6 — MOTIF_NON_PROFESSIONNEL (Cass. soc. 20/11/2007)
        if (Boolean.FALSE.equals(in.motifLieAuxCompetencesProfessionnelles())) {
            codes.add(CodeAnomalie.MOTIF_NON_PROFESSIONNEL);
            anomalies.add(new Anomalie(
                    CodeAnomalie.MOTIF_NON_PROFESSIONNEL,
                    "Motif de rupture sans rapport avec les qualités professionnelles",
                    "Cass. soc., 20/11/2007, n° 06-41.212",
                    Gravite.PROBABLE,
                    "La rupture en période d'essai doit être fondée sur l'évaluation des "
                            + "aptitudes professionnelles. Un motif étranger à cette finalité "
                            + "(personnel, comportemental hors travail, etc.) caractérise un abus."));
        }

        // 7 — MOTIF_ETRANGER_A_ESSAI (économique / organisationnel déguisé)
        if (Boolean.TRUE.equals(in.motifEconomiqueOuOrganisationnel())) {
            codes.add(CodeAnomalie.MOTIF_ETRANGER_A_ESSAI);
            anomalies.add(new Anomalie(
                    CodeAnomalie.MOTIF_ETRANGER_A_ESSAI,
                    "Motif économique ou organisationnel déguisé",
                    "Cass. soc., jurisprudence constante",
                    Gravite.AVERE,
                    "Une rupture d'essai motivée par des raisons économiques ou "
                            + "organisationnelles est détournée de sa finalité d'évaluation "
                            + "des aptitudes — la jurisprudence la requalifie en licenciement abusif."));
        }

        // 8 — DISCRIMINATION_AVEREE (L.1132-1)
        if (in.discriminationInvoquee() != null) {
            codes.add(CodeAnomalie.DISCRIMINATION_AVEREE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.DISCRIMINATION_AVEREE,
                    "Discrimination invoquée caractérisant la nullité de la rupture",
                    "Art. L.1132-1 C. trav.",
                    Gravite.AVERE,
                    "Une rupture d'essai fondée sur un motif discriminatoire ("
                            + in.discriminationInvoquee().name().toLowerCase().replace('_', ' ')
                            + ") est nulle de plein droit. Le salarié peut demander sa "
                            + "réintégration et le rappel des salaires entre la rupture et la "
                            + "réintégration."));
        }

        // 9 — GROSSESSE_PROTECTION_VIOLEE (L.1225-1 et s.)
        if (Boolean.TRUE.equals(in.grossesseAuMomentRupture())) {
            codes.add(CodeAnomalie.GROSSESSE_PROTECTION_VIOLEE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.GROSSESSE_PROTECTION_VIOLEE,
                    "Rupture pendant la période de protection liée à la grossesse / maternité",
                    "Art. L.1225-1 et s. C. trav.",
                    Gravite.AVERE,
                    "La protection contre la rupture pendant la grossesse et le congé "
                            + "maternité s'applique également à la période d'essai. La rupture "
                            + "est nulle ; la salariée peut demander sa réintégration."));
        }

        // 10 — AT_MP_PROTECTION_VIOLEE (L.1226-9)
        if (Boolean.TRUE.equals(in.arretAccidentTravailEnCours())) {
            codes.add(CodeAnomalie.AT_MP_PROTECTION_VIOLEE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.AT_MP_PROTECTION_VIOLEE,
                    "Rupture pendant la suspension du contrat pour AT/MP",
                    "Art. L.1226-9 C. trav.",
                    Gravite.AVERE,
                    "Pendant la suspension du contrat pour accident du travail ou maladie "
                            + "professionnelle, l'employeur ne peut rompre que pour faute grave "
                            + "ou impossibilité de maintien — la rupture d'essai pour insuffisance "
                            + "est nulle."));
        }

        // 11 — ATTEINTE_LIBERTE_FONDAMENTALE
        String atteinte = in.atteinteLiberteFondamentale();
        if (atteinte != null && !atteinte.isBlank()) {
            codes.add(CodeAnomalie.ATTEINTE_LIBERTE_FONDAMENTALE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.ATTEINTE_LIBERTE_FONDAMENTALE,
                    "Rupture portant atteinte à une liberté fondamentale",
                    "Cass. soc., jurisprudence sur les libertés fondamentales",
                    Gravite.AVERE,
                    "Une rupture d'essai portant atteinte à une liberté fondamentale "
                            + "(opinion politique, religieuse, vie privée, expression…) est nulle "
                            + "de plein droit — réintégration possible."));
        }

        // 12 — CONVENTION_COLLECTIVE_NON_RESPECTEE
        if (Boolean.TRUE.equals(in.conventionCollectiveApplicable())
                && Boolean.FALSE.equals(in.conventionCollectivePlusFavorableRespectee())) {
            codes.add(CodeAnomalie.CONVENTION_COLLECTIVE_NON_RESPECTEE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.CONVENTION_COLLECTIVE_NON_RESPECTEE,
                    "Dispositions conventionnelles plus favorables non respectées",
                    "Clause de la convention collective applicable",
                    Gravite.PROBABLE,
                    "La convention collective applicable peut prévoir des dispositions "
                            + "plus favorables au salarié (durée d'essai plus courte, préavis "
                            + "rallongé, formalités spécifiques). Leur non-respect ouvre droit "
                            + "à indemnisation — à confirmer par la clause exacte."));
        }

        return anomalies;
    }

    // ----------------------------------------------------------------------
    // Verdict 4 niveaux (priorité décroissante : NULLE > ILLEGALE > ABUSIVE > REGULIERE)
    // ----------------------------------------------------------------------

    private static Verdict verdictPour(RupturePeriodeEssaiInput in, List<Anomalie> anomalies) {
        // Priorité 1 — nullité (protections L.1132-1 / L.1225-1 / L.1226-9 / liberté fondamentale)
        boolean nullite = anomalies.stream().anyMatch(a ->
                a.code() == CodeAnomalie.DISCRIMINATION_AVEREE
                        || a.code() == CodeAnomalie.GROSSESSE_PROTECTION_VIOLEE
                        || a.code() == CodeAnomalie.AT_MP_PROTECTION_VIOLEE
                        || a.code() == CodeAnomalie.ATTEINTE_LIBERTE_FONDAMENTALE);
        if (nullite) {
            return Verdict.NULLE;
        }

        // Priorité 2 — illégalité (durée essai > légale OU renouvellement irrégulier)
        boolean illegalite = anomalies.stream().anyMatch(a ->
                a.code() == CodeAnomalie.DUREE_ESSAI_DEPASSEE
                        || a.code() == CodeAnomalie.RENOUVELLEMENT_IRREGULIER);
        if (illegalite) {
            // Atténuation Marjolaine 19/05 : lettre motivée + motifs avérés → RISQUE_ABUSIVE
            if (Boolean.TRUE.equals(in.lettreRuptureMotivee())
                    && Boolean.TRUE.equals(in.motifsAveresParPieces())) {
                return Verdict.RISQUE_ABUSIVE;
            }
            return Verdict.ILLEGALE_REQUALIF_LICENCIEMENT;
        }

        // Priorité 3 — risque abusif
        boolean abusif = anomalies.stream().anyMatch(a ->
                a.code() == CodeAnomalie.MOTIF_NON_PROFESSIONNEL
                        || a.code() == CodeAnomalie.MOTIF_ETRANGER_A_ESSAI
                        || a.code() == CodeAnomalie.DELAI_PREVENANCE_INSUFFISANT
                        || a.code() == CodeAnomalie.CONVENTION_COLLECTIVE_NON_RESPECTEE
                        || a.code() == CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI);
        if (abusif) {
            return Verdict.RISQUE_ABUSIVE;
        }

        return Verdict.REGULIERE;
    }

    // ----------------------------------------------------------------------
    // Score indicatif (pondération AVERE / PROBABLE)
    // ----------------------------------------------------------------------

    private static int scorePour(List<Anomalie> anomalies) {
        int score = 0;
        for (Anomalie a : anomalies) {
            score += (a.gravite() == Gravite.AVERE) ? POIDS_ANOMALIE_AVEREE : POIDS_ANOMALIE_PROBABLE;
        }
        return Math.min(SCORE_MAX, score);
    }

    // ----------------------------------------------------------------------
    // Durée légale max (L.1221-19 / L.1242-10)
    // ----------------------------------------------------------------------

    static int dureeLegaleMaximaleMois(CategorieSocioProfessionnelle cat,
                                       TypeContrat type,
                                       Integer dureeCddMois) {
        // CDD / INTERIM : 1 jour par semaine, max 2 semaines (CDD ≤ 6 mois) ou 1 mois (CDD > 6 mois).
        // Exprimé en mois pour cohérence : 0.5 mois (2 sem.) → arrondi 1 ; 1 mois → 1.
        if (type == TypeContrat.CDD || type == TypeContrat.INTERIM) {
            if (dureeCddMois == null || dureeCddMois <= 6) {
                return 1; // 2 semaines maxi — modélisé en 1 mois pour comparaison entière
            }
            return 1; // 1 mois maxi
        }
        // CDI : durée par catégorie
        return switch (cat) {
            case OUVRIER_EMPLOYE -> CDI_OE_DUREE_MOIS;
            case AGENT_MAITRISE_TECHNICIEN -> CDI_AM_DUREE_MOIS;
            case CADRE -> CDI_CADRE_DUREE_MOIS;
        };
    }

    /**
     * Durée effective de l'essai = contractuelle × 2 si renouvellement régulier
     * (accord branche + accord salarié), sinon contractuelle.
     */
    private static int dureeEffectiveEssaiMois(RupturePeriodeEssaiInput in) {
        if (Boolean.TRUE.equals(in.renouvellementInvoque())
                && Boolean.TRUE.equals(in.accordBrancheRenouvellement())
                && Boolean.TRUE.equals(in.accordEcritSalarieRenouvellement())) {
            return in.dureePeriodeEssaiContractuelleMois() * 2;
        }
        return in.dureePeriodeEssaiContractuelleMois();
    }

    // ----------------------------------------------------------------------
    // Délai de prévenance légal (L.1221-25)
    // ----------------------------------------------------------------------

    static int delaiPrevenanceLegalJours(AuteurRupture auteur, long ancienneteJours) {
        if (auteur == AuteurRupture.EMPLOYEUR) {
            if (ancienneteJours < 8) return PREVENANCE_EMP_SOUS_8J_JOURS;
            if (ancienneteJours < 30) return PREVENANCE_EMP_SOUS_1M_JOURS;
            if (ancienneteJours < 90) return PREVENANCE_EMP_SOUS_3M_JOURS;
            return PREVENANCE_EMP_GE_3M_JOURS;
        }
        // SALARIE
        if (ancienneteJours < 8) return PREVENANCE_SAL_SOUS_8J_JOURS;
        return PREVENANCE_SAL_GE_8J_JOURS;
    }

    /**
     * Respect du délai : si l'avocat n'a pas renseigné le délai effectivement
     * appliqué, le critère est considéré comme non défaillant (présomption de
     * respect — l'avocat préviendra par ailleurs).
     */
    private static boolean delaiPrevenanceRespecte(Integer joursAppliques, int joursRequis) {
        if (joursAppliques == null) return true;
        return joursAppliques >= joursRequis;
    }

    // ----------------------------------------------------------------------
    // Indemnité estimée (RISQUE_ABUSIVE → fourchette 1-6 mois × salaire)
    // ----------------------------------------------------------------------

    private static IndemniteEstimee indemniteEstimee(Verdict verdict, Double salaireBrutMensuel) {
        if (verdict != Verdict.RISQUE_ABUSIVE) {
            return null;
        }
        if (salaireBrutMensuel == null || salaireBrutMensuel <= 0) {
            return new IndemniteEstimee(
                    null, null,
                    "Salaire mensuel brut non renseigné — fourchette indicative 1 à 6 mois de salaire",
                    "Jurisprudence Cass. soc. / CPH — dommages et intérêts pour abus");
        }
        return new IndemniteEstimee(
                round(INDEM_ABUS_MIN_MOIS * salaireBrutMensuel),
                round(INDEM_ABUS_MAX_MOIS * salaireBrutMensuel),
                "Salaire brut mensuel × 1 à 6 mois (fourchette jurisprudence locale CPH)",
                "Jurisprudence Cass. soc. / CPH — dommages et intérêts pour abus");
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ----------------------------------------------------------------------
    // Bases juridiques + messages
    // ----------------------------------------------------------------------

    private static List<String> basesJuridiques(List<Anomalie> anomalies) {
        Set<String> bases = new LinkedHashSet<>();
        // Articles toujours pertinents (régime de la rupture d'essai)
        bases.add("Art. L.1221-19 C. trav.");
        bases.add("Art. L.1221-25 C. trav.");
        for (Anomalie a : anomalies) {
            bases.add(a.fondement());
        }
        return new ArrayList<>(bases);
    }

    private static List<String> construireMessages(RupturePeriodeEssaiInput in,
                                                   List<Anomalie> anomalies,
                                                   Verdict verdict,
                                                   boolean remedeReintegration) {
        List<String> msgs = new ArrayList<>();

        // Cas dégénéré : pas de période d'essai contractuelle
        if (in.dureePeriodeEssaiContractuelleMois() != null
                && in.dureePeriodeEssaiContractuelleMois() <= 0) {
            msgs.add("Aucune période d'essai contractuelle — outil non applicable. "
                    + "Utiliser F-DT-08 (validité du licenciement) à la place.");
            return msgs;
        }

        switch (verdict) {
            case REGULIERE -> msgs.add(
                    "Rupture conforme aux articles L.1221-19 à L.1221-25 — aucun vice "
                            + "détecté à partir des éléments saisis.");
            case RISQUE_ABUSIVE -> msgs.add(
                    "Risque d'abus caractérisé — une indemnisation de 1 à 6 mois de "
                            + "salaire brut peut être obtenue (fourchette CPH).");
            case NULLE -> {
                msgs.add("Rupture nulle de plein droit — protection violée caractérisée.");
                msgs.add("REMÈDE PRINCIPAL : demander la RÉINTÉGRATION du salarié dans "
                        + "l'entreprise + le RAPPEL des SALAIRES entre la date de rupture "
                        + "et la date effective de réintégration. L'indemnité plancher 6 mois "
                        + "L.1235-3-1 n'est PAS applicable à la rupture d'essai — mais en "
                        + "demandant la réintégration le salarié obtient bien plus.");
                msgs.add("Subsidiairement : dommages et intérêts (pas de plancher fixe — "
                        + "appréciation du juge selon le préjudice).");
            }
            case ILLEGALE_REQUALIF_LICENCIEMENT -> {
                msgs.add("Rupture illégale — s'analyse comme un licenciement sans cause "
                        + "réelle et sérieuse.");
                msgs.add("Barème Macron L.1235-3 applicable — voir outil F-DT-08 "
                        + "(validité du licenciement) pour le calcul de l'indemnité.");
            }
        }

        // Pièces à vérifier au dossier
        if (Boolean.TRUE.equals(in.lettreRuptureMotivee())) {
            msgs.add("Vérifier la lettre de rupture de la période d'essai au dossier (Pièce).");
        }
        if (Boolean.TRUE.equals(in.renouvellementInvoque())) {
            msgs.add("Renouvellement invoqué : produire l'avenant de renouvellement et "
                    + "vérifier la clause CCN autorisant le renouvellement.");
        }
        if (Boolean.TRUE.equals(in.grossesseAuMomentRupture())) {
            msgs.add("Grossesse : produire le certificat médical attestant de la grossesse "
                    + "à la date de rupture.");
        }
        if (Boolean.TRUE.equals(in.arretAccidentTravailEnCours())) {
            msgs.add("AT/MP : produire la déclaration CPAM et le certificat médical initial "
                    + "couvrant la date de rupture.");
        }
        if (Boolean.TRUE.equals(in.conventionCollectiveApplicable())) {
            msgs.add("Identifier précisément la convention collective applicable et ses "
                    + "clauses sur la période d'essai (durée, préavis, formalités).");
        }

        return msgs;
    }
}
