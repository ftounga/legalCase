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
        CONVENTION_COLLECTIVE_NON_RESPECTEE,
        // SF-252-01 — 5 protections nullité additionnelles (2026-05-20)
        SALARIE_PROTEGE_SANS_AUTORISATION,    // L.2411-1 et s.
        LANCEUR_ALERTE_PROTECTION_VIOLEE,     // L.1132-3-3
        TEMOIN_HARCELEMENT_PROTECTION_VIOLEE, // L.1132-3-1, L.1152-2, L.1153-2/3
        DROIT_RETRAIT_PROTECTION_VIOLEE,      // L.4131-3
        GROSSESSE_NOTIFIEE_POST_RUPTURE       // L.1225-5 (notif ≤ 15j)
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
        INTERIM,
        // SF-252c-01 (audit 2026-05-20) — Régime distinct hors L.1221-19.
        // L'apprentissage suit le régime spécial L.6222-18 (45 jours en milieu
        // de travail, rupture libre des deux côtés). Détecté en early return
        // de `compute()` avec message "hors scope F-DT-38".
        APPRENTISSAGE
    }

    /**
     * SF-252c-01 — Type de contrat précédent pour la reprise d'ancienneté
     * (L.1243-11 / Cass. soc. 09/10/2013 n° 12-19.512).
     */
    public enum TypeContratPrecedent {
        STAGE,
        CDD,
        INTERIM,
        AUTRE
    }

    /** Auteur de la rupture. */
    public enum AuteurRupture {
        EMPLOYEUR,
        SALARIE
    }

    /**
     * Motif de discrimination invoqué (L.1132-1 — liste exhaustive 2026).
     *
     * <p>Les 6 premières valeurs sont héritées de SF-DT-38-01 (rétrocompatibilité —
     * dossiers persistés et tests existants). SF-252-01 ajoute les 17 motifs de
     * la liste L.1132-1 manquants pour couvrir l'exhaustivité du Code du travail.</p>
     */
    public enum DiscriminationMotif {
        // SF-DT-38-01 (legacy, conservés pour rétrocompat)
        RACE_ORIGINE,
        SEXE,
        GROSSESSE,
        SANTE,
        SYNDICAL,
        AUTRE,
        // SF-252-01 — L.1132-1 motifs exhaustifs ajoutés (2026-05-20)
        MOEURS,
        ORIENTATION_SEXUELLE,
        IDENTITE_GENRE,
        AGE,
        SITUATION_FAMILLE,
        CARACTERISTIQUES_GENETIQUES,
        VULNERABILITE_ECONOMIQUE,
        OPINIONS_POLITIQUES,
        CONVICTIONS_RELIGIEUSES,
        APPARENCE_PHYSIQUE,
        NOM_DE_FAMILLE,
        LIEU_DE_RESIDENCE,
        DOMICILIATION_BANCAIRE,
        PERTE_AUTONOMIE,
        HANDICAP,
        CAPACITE_LANGUE_FRANCAISE,
        FONCTIONS_JURIDICTIONNELLES
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

    // SF-252b-01 — Barème CDD L.1242-10 (audit 2026-05-20)
    /** CDD ≤ 6 mois : 1 jour d'essai par semaine de contrat. */
    private static final int CDD_COURT_DUREE_JOURS_PAR_SEMAINE = 1;
    /** CDD ≤ 6 mois : plafond absolu 2 semaines = 14 jours. */
    private static final int CDD_COURT_DUREE_MAX_JOURS = 14;
    /** CDD > 6 mois : 1 mois maximum = 30 jours. */
    private static final int CDD_LONG_DUREE_MAX_JOURS = 30;
    /** Approximation 4 semaines/mois pour la conversion CDD courts. */
    private static final double SEMAINES_PAR_MOIS = 4.0;

    // SF-252b-01 — Barème Intérim L.1251-14 (audit 2026-05-20)
    /** Mission ≤ 1 mois : essai max 2 jours. */
    private static final int INTERIM_MISSION_SOUS_1M_JOURS = 2;
    /** Mission > 1 mois et ≤ 2 mois : essai max 3 jours. */
    private static final int INTERIM_MISSION_SOUS_2M_JOURS = 3;
    /** Mission > 2 mois : essai max 5 jours. */
    private static final int INTERIM_MISSION_PLUS_2M_JOURS = 5;

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

        // SF-252c-01 (audit 2026-05-20) — APPRENTISSAGE : régime spécial L.6222-18,
        // hors scope F-DT-38. Early return avec message explicite et verdict REGULIERE
        // neutre — pas de logique CDI/CDD applicable (qui produirait un verdict faux).
        if (input.typeContrat() == TypeContrat.APPRENTISSAGE) {
            return new RupturePeriodeEssaiResult(
                    List.of(), 0, Verdict.REGULIERE,
                    (int) ChronoUnit.DAYS.between(input.dateDebutContrat(), input.dateRupture()),
                    0, 0, true, null, false,
                    List.of("Art. L.6222-18 C. trav."),
                    List.of(
                            "Contrat d'apprentissage — régime spécial L.6222-18 du Code du travail.",
                            "Les 45 premiers jours de présence effective en milieu de travail "
                                    + "permettent la rupture libre des deux côtés (sans motivation, "
                                    + "sans procédure spécifique). Cet outil F-DT-38 (rupture de "
                                    + "période d'essai L.1221-19+) n'est PAS applicable au contrat "
                                    + "d'apprentissage.",
                            "Pour une rupture après les 45 jours, voir le régime de rupture du "
                                    + "contrat d'apprentissage (résiliation conventionnelle écrite "
                                    + "OU rupture par le conseil de prud'hommes pour faute grave, "
                                    + "manquements répétés, ou inaptitude — L.6222-18)."),
                    countryNormalized,
                    0, null
            );
        }

        long ancienneteJours = ChronoUnit.DAYS.between(input.dateDebutContrat(), input.dateRupture());
        int dureeLegaleMois = dureeLegaleMaximaleMois(
                input.categorieSocioProfessionnelle(),
                input.typeContrat(),
                input.dureeCddMois());
        // SF-252b-01 — barème exact en jours (CDD L.1242-10 / INTERIM L.1251-14)
        int dureeLegaleJours = dureeLegaleMaximaleJours(
                input.categorieSocioProfessionnelle(),
                input.typeContrat(),
                input.dureeCddMois());
        int delaiPrevenanceLegalJours = delaiPrevenanceLegalJours(input.auteurRupture(), ancienneteJours);
        boolean delaiPrevenanceRespecte = delaiPrevenanceRespecte(
                input.delaiPrevenanceJoursAppliques(), delaiPrevenanceLegalJours);

        List<Anomalie> anomalies = detecterAnomalies(input, ancienneteJours, dureeLegaleMois,
                dureeLegaleJours, delaiPrevenanceRespecte);
        int score = scorePour(anomalies);
        Verdict verdict = verdictPour(input, anomalies);
        boolean remedeReintegration = (verdict == Verdict.NULLE);

        IndemniteEstimee indemnite = indemniteEstimee(verdict, input.salaireMensuelBrut());
        // SF-252b-01 — indemnité compensatrice de préavis L.1221-25 (Cass. soc. 23/01/2013)
        // Calculée indépendamment du verdict, dès lors que le délai n'est pas respecté.
        Double indemnitePrevenance = computeIndemnitePrevenanceEuros(
                input.delaiPrevenanceJoursAppliques(),
                delaiPrevenanceLegalJours,
                input.salaireMensuelBrut());
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
                countryNormalized,
                dureeLegaleJours,
                indemnitePrevenance
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
                                                    int dureeLegaleJours,
                                                    boolean delaiPrevenanceRespecte) {
        Set<CodeAnomalie> codes = new LinkedHashSet<>();
        List<Anomalie> anomalies = new ArrayList<>();

        // 1 — PERIODE_ESSAI_ABSENTE (hors scope, message uniquement)
        if (in.dureePeriodeEssaiContractuelleMois() <= 0
                && (in.dureePeriodeEssaiContractuelleJours() == null
                        || in.dureePeriodeEssaiContractuelleJours() <= 0)) {
            // Pas d'anomalie ajoutée — message construit dans construireMessages.
            // Cas dégénéré : aucune anomalie, verdict REGULIERE avec message hors scope.
            return List.of();
        }

        // 2 — DUREE_ESSAI_DEPASSEE (L.1221-19 CDI / L.1242-10 CDD / L.1251-14 INTERIM)
        // SF-252b-01 — comparaison en JOURS pour précision CDD/INTERIM.
        int contractuelJours = dureeContractuelleEnJours(in);
        if (contractuelJours > dureeLegaleJours) {
            codes.add(CodeAnomalie.DUREE_ESSAI_DEPASSEE);
            String fondement = switch (in.typeContrat()) {
                case CDD -> "Art. L.1242-10 C. trav.";
                case INTERIM -> "Art. L.1251-14 C. trav.";
                default -> "Art. L.1221-19 C. trav.";
            };
            anomalies.add(new Anomalie(
                    CodeAnomalie.DUREE_ESSAI_DEPASSEE,
                    "Durée de la période d'essai contractuelle supérieure à la durée légale",
                    fondement,
                    Gravite.AVERE,
                    "La durée contractuelle (" + contractuelJours
                            + " jours) excède la durée légale maximale (" + dureeLegaleJours
                            + " jours) pour ce type de contrat. "
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
        // SF-252c-01 (audit 2026-05-20) : fin d'essai = dateDebut + durée effective
        // (réduite par l'ancienneté du contrat précédent L.1243-11 / Cass. soc.
        // 09/10/2013) + jours de suspension du contrat (Cass. soc. 31/01/2018,
        // n° 16-19.836 — arrêt maladie, congés non rémunérés, grève prolongent
        // d'autant la fin d'essai).
        int dureeEffectiveMois = dureeEffectiveEssaiMois(in);
        LocalDate finEssai = in.dateDebutContrat()
                .plusMonths(dureeEffectiveMois)
                .plusDays(in.joursSuspensionContrat() != null
                        ? Math.max(0, in.joursSuspensionContrat()) : 0);
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

        // SF-252-01 — 5 protections nullité additionnelles (audit 2026-05-20)

        // 13 — SALARIE_PROTEGE_SANS_AUTORISATION (L.2411-1 et s.)
        // Élus CSE, délégués syndicaux, conseillers prud'homaux, membres CSSCT, etc. :
        // la rupture nécessite une autorisation préalable de l'inspection du travail,
        // même pendant la période d'essai (Cass. soc., jurisprudence constante).
        if (Boolean.TRUE.equals(in.salarieProtege())
                && !Boolean.TRUE.equals(in.autorisationInspectionTravailObtenue())) {
            codes.add(CodeAnomalie.SALARIE_PROTEGE_SANS_AUTORISATION);
            anomalies.add(new Anomalie(
                    CodeAnomalie.SALARIE_PROTEGE_SANS_AUTORISATION,
                    "Rupture d'un salarié protégé sans autorisation de l'inspection du travail",
                    "Art. L.2411-1 et s. C. trav.",
                    Gravite.AVERE,
                    "Le salarié bénéficie d'un statut protecteur (élu CSE / DS / conseiller "
                            + "prud'homal / membre CSSCT…). Sa rupture, y compris pendant la "
                            + "période d'essai, exige une autorisation préalable de l'inspection "
                            + "du travail. À défaut, la rupture est nulle de plein droit ; le "
                            + "salarié peut demander sa réintégration et le rappel des salaires."));
        }

        // 14 — LANCEUR_ALERTE_PROTECTION_VIOLEE (L.1132-3-3)
        if (Boolean.TRUE.equals(in.lanceurAlerte())) {
            codes.add(CodeAnomalie.LANCEUR_ALERTE_PROTECTION_VIOLEE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.LANCEUR_ALERTE_PROTECTION_VIOLEE,
                    "Rupture en violation de la protection du lanceur d'alerte",
                    "Art. L.1132-3-3 C. trav. (loi Sapin II du 09/12/2016, renforcée Waserman 21/03/2022)",
                    Gravite.AVERE,
                    "Le salarié lanceur d'alerte (signalement d'un crime, délit, menace ou "
                            + "atteinte grave à l'intérêt général dans les conditions L.1132-3-3) "
                            + "bénéficie d'une protection absolue. Toute rupture pour ce motif, "
                            + "y compris en période d'essai, est nulle de plein droit — "
                            + "réintégration possible + rappel des salaires."));
        }

        // 15 — TEMOIN_HARCELEMENT_PROTECTION_VIOLEE (L.1132-3-1, L.1152-2, L.1153-2 et s.)
        if (Boolean.TRUE.equals(in.temoinOuVictimeHarcelement())) {
            codes.add(CodeAnomalie.TEMOIN_HARCELEMENT_PROTECTION_VIOLEE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.TEMOIN_HARCELEMENT_PROTECTION_VIOLEE,
                    "Rupture en représailles d'un témoignage / d'une dénonciation de harcèlement",
                    "Art. L.1132-3-1, L.1152-2, L.1153-2 et L.1153-3 C. trav.",
                    Gravite.AVERE,
                    "Aucun salarié ne peut être sanctionné, licencié ou faire l'objet d'une "
                            + "mesure discriminatoire pour avoir relaté ou témoigné de faits de "
                            + "harcèlement moral ou sexuel, ou de faits de discrimination. "
                            + "Cette protection s'applique pendant la période d'essai — toute "
                            + "rupture pour ce motif est nulle."));
        }

        // 16 — DROIT_RETRAIT_PROTECTION_VIOLEE (L.4131-3)
        if (Boolean.TRUE.equals(in.droitDeRetraitExerce())) {
            codes.add(CodeAnomalie.DROIT_RETRAIT_PROTECTION_VIOLEE);
            anomalies.add(new Anomalie(
                    CodeAnomalie.DROIT_RETRAIT_PROTECTION_VIOLEE,
                    "Rupture sanctionnant l'exercice du droit de retrait",
                    "Art. L.4131-3 C. trav.",
                    Gravite.AVERE,
                    "Aucune sanction, aucune retenue de salaire ne peut être prise à l'encontre "
                            + "d'un salarié ou d'un groupe de salariés qui se sont retirés d'une "
                            + "situation de travail dont ils avaient un motif raisonnable de "
                            + "penser qu'elle présentait un danger grave et imminent pour leur "
                            + "vie ou leur santé. La rupture d'essai pour ce motif est nulle."));
        }

        // 17 — GROSSESSE_NOTIFIEE_POST_RUPTURE (L.1225-5)
        // La salariée dispose de 15 jours après la rupture pour notifier sa grossesse
        // à l'employeur (certificat médical). Si la grossesse est antérieure à la
        // rupture, la nullité rétroagit (Cass. soc. 26/10/2017, n° 16-12.554).
        if (Boolean.TRUE.equals(in.grossesseDeclareePostRupture())
                && in.dateRupture() != null
                && in.dateNotificationGrossesse() != null) {
            long delaiNotifJours = ChronoUnit.DAYS.between(
                    in.dateRupture(), in.dateNotificationGrossesse());
            if (delaiNotifJours >= 0 && delaiNotifJours <= 15) {
                codes.add(CodeAnomalie.GROSSESSE_NOTIFIEE_POST_RUPTURE);
                anomalies.add(new Anomalie(
                        CodeAnomalie.GROSSESSE_NOTIFIEE_POST_RUPTURE,
                        "Grossesse notifiée à l'employeur dans les 15 jours suivant la rupture",
                        "Art. L.1225-5 C. trav.",
                        Gravite.AVERE,
                        "La salariée a notifié sa grossesse à l'employeur le "
                                + in.dateNotificationGrossesse() + " (" + delaiNotifJours
                                + " jour(s) après la rupture du " + in.dateRupture()
                                + "). Dans ce délai légal de 15 jours, et si la grossesse est "
                                + "antérieure à la rupture, celle-ci est nulle rétroactivement — "
                                + "réintégration possible avec rappel des salaires."));
            }
        }

        return anomalies;
    }

    // ----------------------------------------------------------------------
    // Verdict 4 niveaux (priorité décroissante : NULLE > ILLEGALE > ABUSIVE > REGULIERE)
    // ----------------------------------------------------------------------

    private static Verdict verdictPour(RupturePeriodeEssaiInput in, List<Anomalie> anomalies) {
        // Priorité 1 — nullité (protections L.1132-1 / L.1225-1 / L.1226-9 / liberté fondamentale
        // + SF-252-01 : salarié protégé, lanceur d'alerte, témoin harcèlement, droit de retrait,
        // grossesse notifiée post-rupture)
        boolean nullite = anomalies.stream().anyMatch(a ->
                a.code() == CodeAnomalie.DISCRIMINATION_AVEREE
                        || a.code() == CodeAnomalie.GROSSESSE_PROTECTION_VIOLEE
                        || a.code() == CodeAnomalie.AT_MP_PROTECTION_VIOLEE
                        || a.code() == CodeAnomalie.ATTEINTE_LIBERTE_FONDAMENTALE
                        // SF-252-01
                        || a.code() == CodeAnomalie.SALARIE_PROTEGE_SANS_AUTORISATION
                        || a.code() == CodeAnomalie.LANCEUR_ALERTE_PROTECTION_VIOLEE
                        || a.code() == CodeAnomalie.TEMOIN_HARCELEMENT_PROTECTION_VIOLEE
                        || a.code() == CodeAnomalie.DROIT_RETRAIT_PROTECTION_VIOLEE
                        || a.code() == CodeAnomalie.GROSSESSE_NOTIFIEE_POST_RUPTURE);
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
        // SF-252b-01 (audit 2026-05-20) — DELAI_PREVENANCE_INSUFFISANT retiré de la
        // liste. Cass. soc., 23/01/2013, n° 11-23.428 : l'inobservation du délai de
        // prévenance L.1221-25 n'ouvre droit qu'à une indemnité compensatrice de
        // préavis non exécuté — elle ne caractérise pas un abus en soi et ne
        // requalifie pas la rupture. L'indemnité correspondante est exposée
        // séparément via `RupturePeriodeEssaiResult.indemnitePrevenanceEuros`.
        boolean abusif = anomalies.stream().anyMatch(a ->
                a.code() == CodeAnomalie.MOTIF_NON_PROFESSIONNEL
                        || a.code() == CodeAnomalie.MOTIF_ETRANGER_A_ESSAI
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
        // SF-252b-01 (audit 2026-05-20) — pour la vérification réelle de la durée
        // d'essai CDD/INTERIM, utiliser {@link #dureeLegaleMaximaleJours} qui applique
        // les barèmes exacts L.1242-10 et L.1251-14. Cette méthode reste conservée
        // pour la rétrocompat de l'API (Response.dureeLegaleMaximaleMois int).
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
     * SF-252b-01 (audit 2026-05-20) — Durée légale maximale de la période d'essai
     * en <strong>jours</strong>, version exacte des barèmes du Code du travail.
     *
     * <p>CDD (L.1242-10) : 1 jour par semaine de contrat, plafond absolu 2 semaines
     * (CDD ≤ 6 mois) ou 1 mois (CDD &gt; 6 mois). Ex. CDD de 3 mois → essai max 12
     * jours (3×4 semaines × 1 j/sem). Ex. CDD de 5 semaines → essai max 5 jours.</p>
     *
     * <p>Intérim (L.1251-14) : 2 jours (mission ≤ 1 mois) / 3 jours (mission &gt; 1
     * et ≤ 2 mois) / 5 jours (mission &gt; 2 mois). Indépendant du type de contrat.</p>
     *
     * <p>CDI : durée légale par catégorie (L.1221-19) × 30 jours pour homogénéité
     * avec les comparaisons CDD/INTERIM (2 mois OE = 60 jours, etc.).</p>
     */
    static int dureeLegaleMaximaleJours(CategorieSocioProfessionnelle cat,
                                        TypeContrat type,
                                        Integer dureeCddMois) {
        if (type == TypeContrat.CDD) {
            if (dureeCddMois == null || dureeCddMois <= 6) {
                int semaines = (int) Math.ceil(
                        (dureeCddMois != null ? dureeCddMois : 6) * SEMAINES_PAR_MOIS);
                return Math.min(
                        semaines * CDD_COURT_DUREE_JOURS_PAR_SEMAINE,
                        CDD_COURT_DUREE_MAX_JOURS);
            }
            return CDD_LONG_DUREE_MAX_JOURS;
        }
        if (type == TypeContrat.INTERIM) {
            if (dureeCddMois == null) return INTERIM_MISSION_PLUS_2M_JOURS;
            if (dureeCddMois <= 1) return INTERIM_MISSION_SOUS_1M_JOURS;
            if (dureeCddMois <= 2) return INTERIM_MISSION_SOUS_2M_JOURS;
            return INTERIM_MISSION_PLUS_2M_JOURS;
        }
        // CDI : durée par catégorie × 30 jours
        return switch (cat) {
            case OUVRIER_EMPLOYE -> CDI_OE_DUREE_MOIS * 30;
            case AGENT_MAITRISE_TECHNICIEN -> CDI_AM_DUREE_MOIS * 30;
            case CADRE -> CDI_CADRE_DUREE_MOIS * 30;
        };
    }

    /**
     * SF-252b-01 — Durée contractuelle de l'essai en jours, à partir des deux
     * champs d'input. Pour CDD/INTERIM, privilégie le champ `dureePeriodeEssaiContractuelleJours`
     * s'il est renseigné (le contrat exprime souvent l'essai en jours). À défaut,
     * convertit `dureePeriodeEssaiContractuelleMois × 30`.
     */
    static int dureeContractuelleEnJours(RupturePeriodeEssaiInput in) {
        if ((in.typeContrat() == TypeContrat.CDD || in.typeContrat() == TypeContrat.INTERIM)
                && in.dureePeriodeEssaiContractuelleJours() != null) {
            return in.dureePeriodeEssaiContractuelleJours();
        }
        Integer mois = in.dureePeriodeEssaiContractuelleMois();
        return mois != null ? mois * 30 : 0;
    }

    /**
     * Durée effective de l'essai = contractuelle × 2 si renouvellement régulier
     * (accord branche + accord salarié), sinon contractuelle.
     *
     * <p>SF-252c-01 (audit 2026-05-20) — La durée est ensuite réduite par
     * l'ancienneté d'un stage &gt; 2 mois (Cass. soc., 09/10/2013, n° 12-19.512)
     * ou d'un CDD précédent dans la même entreprise / même fonction
     * (L.1243-11), capée à 0 (pas de durée négative). Convention : pour le
     * stage, déduction uniquement si l'ancienneté ≥ 2 mois.</p>
     */
    private static int dureeEffectiveEssaiMois(RupturePeriodeEssaiInput in) {
        int duree = in.dureePeriodeEssaiContractuelleMois();
        if (Boolean.TRUE.equals(in.renouvellementInvoque())
                && Boolean.TRUE.equals(in.accordBrancheRenouvellement())
                && Boolean.TRUE.equals(in.accordEcritSalarieRenouvellement())) {
            duree *= 2;
        }
        // SF-252c-01 — Reprise d'ancienneté du contrat précédent
        Integer ancMois = in.ancienneteContratPrecedentMois();
        TypeContratPrecedent typePrecedent = in.typeContratPrecedent();
        if (ancMois != null && ancMois > 0 && typePrecedent != null) {
            boolean deductible = switch (typePrecedent) {
                case CDD, INTERIM -> true;          // L.1243-11
                case STAGE -> ancMois >= 2;          // Cass. soc. 09/10/2013 (stage > 2 mois)
                case AUTRE -> false;
            };
            if (deductible) {
                duree = Math.max(0, duree - ancMois);
            }
        }
        return duree;
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

    /**
     * SF-252b-01 (audit 2026-05-20) — Indemnité compensatrice de préavis non
     * exécuté L.1221-25 (Cass. soc., 23/01/2013, n° 11-23.428).
     *
     * <p>Distincte des dommages et intérêts pour abus (cf. {@link #indemniteEstimee}).
     * Cumulable avec ces derniers, et applicable indépendamment du verdict global :
     * dès lors que le délai légal de prévenance n'a pas été respecté, l'employeur
     * doit verser le salaire des jours manquants entre la date d'effet réelle de
     * la rupture et la date d'effet qu'elle aurait dû avoir.</p>
     *
     * @return montant en euros, ou null si non applicable (délai respecté, salaire
     *         non renseigné, ou jours appliqués non renseignés)
     */
    private static Double computeIndemnitePrevenanceEuros(Integer joursAppliques,
                                                          int joursRequis,
                                                          Double salaireBrutMensuel) {
        if (salaireBrutMensuel == null || salaireBrutMensuel <= 0) return null;
        if (joursAppliques == null) return null;
        int joursManquants = joursRequis - joursAppliques;
        if (joursManquants <= 0) return null;
        return round(salaireBrutMensuel * joursManquants / 30.0);
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

        // SF-252-01 — Pièces à produire pour les 5 nouvelles protections nullité
        if (Boolean.TRUE.equals(in.salarieProtege())) {
            msgs.add("Salarié protégé : produire le mandat (élu CSE / DS / conseiller "
                    + "prud'homal / etc.) ET, s'il existe, le PV de la demande d'autorisation "
                    + "déposée auprès de l'inspection du travail (ou l'absence formelle de "
                    + "celle-ci).");
        }
        if (Boolean.TRUE.equals(in.lanceurAlerte())) {
            msgs.add("Lanceur d'alerte : produire le signalement initial (canal interne ou "
                    + "externe), les accusés de réception, et toute pièce démontrant le lien "
                    + "entre le signalement et la rupture.");
        }
        if (Boolean.TRUE.equals(in.temoinOuVictimeHarcelement())) {
            msgs.add("Témoin / victime de harcèlement : produire le témoignage formel, "
                    + "le signalement à l'employeur ou au CSE, et toute pièce médicale "
                    + "(arrêt, certificat) caractérisant le harcèlement.");
        }
        if (Boolean.TRUE.equals(in.droitDeRetraitExerce())) {
            msgs.add("Droit de retrait : produire le signalement écrit du danger grave et "
                    + "imminent (au CSE, à l'employeur ou à l'inspection du travail) et toute "
                    + "preuve du danger (rapport, photos, attestations, accident postérieur).");
        }
        if (Boolean.TRUE.equals(in.grossesseDeclareePostRupture())
                && in.dateNotificationGrossesse() != null) {
            msgs.add("Grossesse notifiée post-rupture : produire (a) le certificat médical "
                    + "de grossesse daté ANTÉRIEUREMENT à la rupture, et (b) la LRAR ou "
                    + "tout courrier de notification à l'employeur dans les 15 jours suivant "
                    + "la rupture (art. L.1225-5).");
        }

        return msgs;
    }
}
