package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-217-02 : moteur de suivi de la procédure de liquidation-partage post-divorce
 * belge devant notaire commis (Code judiciaire — art. 1207 et suivants).
 *
 * <p>Positionne le dossier sur la séquence des 8 étapes de la procédure
 * (désignation du notaire-liquidateur, ouverture des opérations, inventaire,
 * projet de liquidation, notification du projet, contredits, procès-verbal de
 * dires, homologation), calcule le délai procédural critique (délai de contredits
 * d'un mois — CJ art. 1218) et rend un verdict d'avancement en 5 niveaux.</p>
 *
 * <p><b>Logique de séquence</b> : toutes les étapes antérieures à la dernière
 * étape « franchie » sont {@code FAITE} ; l'étape immédiatement suivante est
 * {@code EN_COURS} ; le reste est {@code A_VENIR}. Une étape franchie alors qu'une
 * étape antérieure ne l'est pas produit un message d'anomalie de séquence.</p>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. La procédure belge repose sur le
 * notaire-liquidateur <i>commis</i> par le Tribunal de la famille, distincte de la
 * liquidation-partage française (F-FA-17).</p>
 *
 * <p><b>Validation juridique requise</b> : les articles du Code judiciaire belge
 * (art. 1207 à 1224, art. 1218 pour les contredits), la séquence des étapes et le
 * délai d'un mois pour les contredits reflètent l'état du droit connu du modèle
 * et la réforme de la procédure de liquidation-partage (loi du 13/08/2011, en
 * vigueur 01/04/2012). Ils sont centralisés ici (source unique de vérité) et
 * <b>doivent être validés par un avocat belge avant mise en production</b>.</p>
 */
public final class LiquidationPartageBeCalculator {

    /** Verdict d'avancement de la procédure de liquidation-partage. */
    public enum LiquidationPartageBeVerdict {
        PROCEDURE_NON_ENGAGEE,       // notaire pas encore désigné
        EN_COURS,                    // procédure engagée, pas de délai critique imminent
        DELAI_CONTREDITS_CRITIQUE,   // délai de contredits ≤ 10 j ou dépassé sans contredits actés
        EN_ATTENTE_HOMOLOGATION,     // procès-verbal de dires établi, homologation non intervenue
        CLOTUREE                     // homologation prononcée
    }

    /** Statut d'une étape de la procédure. */
    public enum EtapeStatutBe {
        FAITE,
        EN_COURS,
        A_VENIR
    }

    /** Statut d'un délai procédural. */
    public enum DelaiStatutBe {
        OK,           // échéance > 10 jours
        CRITIQUE,     // échéance ≤ 10 jours et non dépassée
        DEPASSE,      // échéance dépassée
        NON_DEMARRE   // point de départ non encore connu
    }

    /** Code structuré d'une étape de la procédure. */
    public enum CodeEtape {
        DESIGNATION_NOTAIRE,
        OUVERTURE_OPERATIONS,
        INVENTAIRE,
        PROJET_LIQUIDATION,
        NOTIFICATION_PROJET,
        CONTREDITS,
        PROCES_VERBAL_DIRES,
        HOMOLOGATION_TF
    }

    /** Code structuré d'un délai procédural. */
    public enum CodeDelai {
        DELAI_CONTREDITS
    }

    /** Étape de la procédure — structure exposée dans la réponse API. */
    public record EtapeProcedure(
            CodeEtape code,
            String libelle,
            EtapeStatutBe statut,
            int ordre,
            String fondement,
            String explication
    ) {}

    /** Délai procédural — structure exposée dans la réponse API. */
    public record DelaiProcedure(
            CodeDelai code,
            String libelle,
            String fondement,
            LocalDate dateDepart,
            LocalDate dateEcheance,
            Integer joursRestants,
            DelaiStatutBe statut
    ) {}

    /** Durée du délai de contredits (CJ art. 1218) — un mois date à date. */
    private static final int DELAI_CONTREDITS_MOIS = 1;
    /** Seuil de jours restants en deçà duquel un délai est jugé critique. */
    private static final int SEUIL_DELAI_CRITIQUE_JOURS = 10;
    /** Longueur maximale du commentaire libre. */
    private static final int COMMENTAIRE_MAX = 1000;

    private LiquidationPartageBeCalculator() {}

    /**
     * Suit la procédure de liquidation-partage belge à partir des éléments saisis.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté pour cette SF)
     * @param today   date de référence pour le calcul des délais (injectable pour les tests)
     * @return résultat structuré (verdict, étapes, délais, bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static LiquidationPartageBeResult compute(LiquidationPartageBeInput input,
                                                     String country, LocalDate today) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — liquidation-partage du notaire commis");
        }
        if (today == null) {
            throw new IllegalArgumentException("Date de référence requise");
        }
        validateCommentaire(input.commentaire());
        validateDatesPresentes(input);
        validateCoherenceDates(input);

        List<EtapeProcedure> etapes = construireEtapes(input, today);
        DelaiProcedure delaiContredits = calculerDelaiContredits(input, today);
        List<DelaiProcedure> delais = List.of(delaiContredits);
        LiquidationPartageBeVerdict verdict = verdictPour(input, delaiContredits);
        List<String> bases = basesJuridiques();
        List<String> messages = construireMessages(input, verdict, delaiContredits);
        String prochaineEtape = prochaineEtape(input, etapes, verdict, delaiContredits);

        return new LiquidationPartageBeResult(
                verdict,
                etapes,
                delais,
                prochaineEtape,
                bases,
                messages,
                countryNormalized
        );
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private static void validateCommentaire(String commentaire) {
        if (commentaire != null && commentaire.length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    /** Une étape franchie doit s'accompagner de sa date si elle en exige une. */
    private static void validateDatesPresentes(LiquidationPartageBeInput in) {
        if (Boolean.TRUE.equals(in.notaireDesigne()) && in.dateDesignationNotaire() == null) {
            throw new IllegalArgumentException(
                    "La date de désignation du notaire est requise lorsque le notaire est désigné");
        }
        if (Boolean.TRUE.equals(in.operationsOuvertes()) && in.dateOuvertureOperations() == null) {
            throw new IllegalArgumentException(
                    "La date d'ouverture des opérations est requise lorsque les opérations sont ouvertes");
        }
        if (Boolean.TRUE.equals(in.projetLiquidationEtabli()) && in.dateNotificationProjet() == null) {
            // La notification du projet est le point de départ du délai de contredits :
            // un projet établi sans notification reste tolérable (étape 5 non franchie),
            // mais un projet établi ne peut être suivi de contredits sans notification.
            if (Boolean.TRUE.equals(in.contreditsDeposes())) {
                throw new IllegalArgumentException(
                        "La date de notification du projet est requise dès lors que des "
                                + "contredits ont été déposés");
            }
        }
        if (Boolean.TRUE.equals(in.homologationDemandee()) && in.dateHomologation() == null) {
            throw new IllegalArgumentException(
                    "La date d'homologation est requise lorsque l'homologation a été prononcée");
        }
    }

    /** Les dates doivent respecter la chronologie de la procédure. */
    private static void validateCoherenceDates(LiquidationPartageBeInput in) {
        LocalDate designation = in.dateDesignationNotaire();
        LocalDate ouverture = in.dateOuvertureOperations();
        LocalDate notification = in.dateNotificationProjet();
        LocalDate homologation = in.dateHomologation();

        if (designation != null && ouverture != null && ouverture.isBefore(designation)) {
            throw new IllegalArgumentException(
                    "L'ouverture des opérations ne peut précéder la désignation du notaire");
        }
        if (designation != null && notification != null && notification.isBefore(designation)) {
            throw new IllegalArgumentException(
                    "La notification du projet ne peut précéder la désignation du notaire");
        }
        if (ouverture != null && notification != null && notification.isBefore(ouverture)) {
            throw new IllegalArgumentException(
                    "La notification du projet ne peut précéder l'ouverture des opérations");
        }
        if (notification != null && homologation != null && homologation.isBefore(notification)) {
            throw new IllegalArgumentException(
                    "L'homologation ne peut précéder la notification du projet");
        }
        if (designation != null && homologation != null && homologation.isBefore(designation)) {
            throw new IllegalArgumentException(
                    "L'homologation ne peut précéder la désignation du notaire");
        }
    }

    // ------------------------------------------------------------------
    // Séquence des étapes
    // ------------------------------------------------------------------

    /**
     * Construit la checklist des 8 étapes en les positionnant FAITE / EN_COURS /
     * A_VENIR. Toutes les étapes jusqu'au dernier rang « franchi » sont FAITE ;
     * l'étape de rang immédiatement supérieur est EN_COURS ; le reste est A_VENIR.
     */
    private static List<EtapeProcedure> construireEtapes(LiquidationPartageBeInput in,
                                                         LocalDate today) {
        boolean[] franchie = new boolean[9]; // index 1..8
        franchie[1] = Boolean.TRUE.equals(in.notaireDesigne());
        franchie[2] = Boolean.TRUE.equals(in.operationsOuvertes());
        franchie[3] = Boolean.TRUE.equals(in.inventaireEtabli());
        franchie[4] = Boolean.TRUE.equals(in.projetLiquidationEtabli());
        franchie[5] = in.dateNotificationProjet() != null;
        franchie[6] = Boolean.TRUE.equals(in.contreditsDeposes())
                || delaiContreditsEcoule(in, today);
        franchie[7] = Boolean.TRUE.equals(in.procesVerbalDiresEtabli());
        franchie[8] = Boolean.TRUE.equals(in.homologationDemandee())
                && in.dateHomologation() != null;

        // Rang de la dernière étape franchie selon l'ordre nominal (séquence linéaire).
        int dernierRangFaite = 0;
        for (int rang = 1; rang <= 8; rang++) {
            if (franchie[rang]) {
                dernierRangFaite = rang;
            }
        }

        List<EtapeProcedure> etapes = new ArrayList<>();
        for (int rang = 1; rang <= 8; rang++) {
            EtapeStatutBe statut;
            if (rang <= dernierRangFaite) {
                statut = EtapeStatutBe.FAITE;
            } else if (rang == dernierRangFaite + 1) {
                statut = EtapeStatutBe.EN_COURS;
            } else {
                statut = EtapeStatutBe.A_VENIR;
            }
            etapes.add(etapePour(rang, statut));
        }
        return List.copyOf(etapes);
    }

    private static EtapeProcedure etapePour(int rang, EtapeStatutBe statut) {
        return switch (rang) {
            case 1 -> new EtapeProcedure(CodeEtape.DESIGNATION_NOTAIRE,
                    "Désignation du notaire-liquidateur par le Tribunal de la famille",
                    statut, 1, "CJ art. 1207-1209",
                    "Le Tribunal de la famille désigne un notaire-liquidateur (et le cas "
                            + "échéant un notaire représentant la partie défaillante) chargé de "
                            + "conduire les opérations.");
            case 2 -> new EtapeProcedure(CodeEtape.OUVERTURE_OPERATIONS,
                    "Ouverture des opérations / inventaire des biens",
                    statut, 2, "CJ art. 1210-1213",
                    "Le notaire-liquidateur ouvre les opérations de liquidation-partage et "
                            + "convoque les parties.");
            case 3 -> new EtapeProcedure(CodeEtape.INVENTAIRE,
                    "Établissement de l'inventaire du patrimoine",
                    statut, 3, "CJ art. 1213",
                    "Le notaire dresse l'inventaire des biens, droits et dettes composant "
                            + "la masse à partager.");
            case 4 -> new EtapeProcedure(CodeEtape.PROJET_LIQUIDATION,
                    "Établissement de l'état liquidatif / projet de partage par le notaire",
                    statut, 4, "CJ art. 1214",
                    "Le notaire établit l'état liquidatif et le projet de partage chiffrant "
                            + "les droits respectifs des parties.");
            case 5 -> new EtapeProcedure(CodeEtape.NOTIFICATION_PROJET,
                    "Notification du projet aux parties — point de départ du délai de contredits",
                    statut, 5, "CJ art. 1218",
                    "Le notaire notifie le projet de liquidation-partage aux parties ; cette "
                            + "notification fait courir le délai d'un mois pour déposer les contredits.");
            case 6 -> new EtapeProcedure(CodeEtape.CONTREDITS,
                    "Dépôt des contredits dans le mois de la notification",
                    statut, 6, "CJ art. 1218",
                    "Chaque partie peut, dans le mois de la notification, faire valoir ses "
                            + "contredits sur le projet de liquidation-partage.");
            case 7 -> new EtapeProcedure(CodeEtape.PROCES_VERBAL_DIRES,
                    "Établissement du procès-verbal de dires et difficultés par le notaire",
                    statut, 7, "CJ art. 1218",
                    "Le notaire dresse le procès-verbal des dires et difficultés et le "
                            + "transmet, le cas échéant, au Tribunal de la famille.");
            case 8 -> new EtapeProcedure(CodeEtape.HOMOLOGATION_TF,
                    "Homologation de l'état liquidatif par le Tribunal de la famille",
                    statut, 8, "CJ art. 1223-1224",
                    "Le Tribunal de la famille homologue l'état liquidatif, qui acquiert "
                            + "ainsi force exécutoire et clôture la procédure.");
            default -> throw new IllegalStateException("Rang d'étape inconnu : " + rang);
        };
    }

    // ------------------------------------------------------------------
    // Délai de contredits (CJ art. 1218)
    // ------------------------------------------------------------------

    /**
     * Calcule le délai de contredits d'un mois à compter de la notification du
     * projet. L'échéance est calculée date à date ({@code plusMonths}), ce qui gère
     * naturellement les mois de longueurs différentes. {@code joursRestants} est
     * exprimé en jours calendaires entre la date du jour et l'échéance.
     */
    private static DelaiProcedure calculerDelaiContredits(LiquidationPartageBeInput in,
                                                          LocalDate today) {
        LocalDate depart = in.dateNotificationProjet();
        String libelle = "Délai de contredits sur le projet de liquidation-partage";
        String fondement = "CJ art. 1218";

        if (depart == null) {
            return new DelaiProcedure(CodeDelai.DELAI_CONTREDITS, libelle, fondement,
                    null, null, null, DelaiStatutBe.NON_DEMARRE);
        }

        LocalDate echeance = depart.plusMonths(DELAI_CONTREDITS_MOIS);
        long joursRestants = ChronoUnit.DAYS.between(today, echeance);

        DelaiStatutBe statut;
        if (Boolean.TRUE.equals(in.contreditsDeposes())) {
            // Contredits actés : le délai n'est plus un sujet — statut OK quelle que
            // soit l'échéance (mini-spec : DEPASSE n'existe que si contreditsDeposes = false).
            statut = DelaiStatutBe.OK;
        } else if (joursRestants < 0) {
            statut = DelaiStatutBe.DEPASSE;
        } else if (joursRestants <= SEUIL_DELAI_CRITIQUE_JOURS) {
            statut = DelaiStatutBe.CRITIQUE;
        } else {
            statut = DelaiStatutBe.OK;
        }

        return new DelaiProcedure(CodeDelai.DELAI_CONTREDITS, libelle, fondement,
                depart, echeance, (int) joursRestants, statut);
    }

    /** Indique si le délai de contredits d'un mois est écoulé à la date donnée. */
    private static boolean delaiContreditsEcoule(LiquidationPartageBeInput in, LocalDate today) {
        LocalDate depart = in.dateNotificationProjet();
        if (depart == null) {
            return false;
        }
        LocalDate echeance = depart.plusMonths(DELAI_CONTREDITS_MOIS);
        return today.isAfter(echeance);
    }

    // ------------------------------------------------------------------
    // Verdict
    // ------------------------------------------------------------------

    /**
     * Verdict piloté par la règle figée de la mini-spec :
     * notaire non désigné → PROCEDURE_NON_ENGAGEE ;
     * sinon homologation prononcée → CLOTUREE ;
     * sinon délai de contredits CRITIQUE ou DEPASSE → DELAI_CONTREDITS_CRITIQUE ;
     * sinon procès-verbal de dires établi → EN_ATTENTE_HOMOLOGATION ;
     * sinon → EN_COURS.
     */
    private static LiquidationPartageBeVerdict verdictPour(LiquidationPartageBeInput in,
                                                           DelaiProcedure delaiContredits) {
        if (!Boolean.TRUE.equals(in.notaireDesigne())) {
            return LiquidationPartageBeVerdict.PROCEDURE_NON_ENGAGEE;
        }
        if (in.dateHomologation() != null) {
            return LiquidationPartageBeVerdict.CLOTUREE;
        }
        if (delaiContredits.statut() == DelaiStatutBe.CRITIQUE
                || delaiContredits.statut() == DelaiStatutBe.DEPASSE) {
            return LiquidationPartageBeVerdict.DELAI_CONTREDITS_CRITIQUE;
        }
        if (Boolean.TRUE.equals(in.procesVerbalDiresEtabli())) {
            return LiquidationPartageBeVerdict.EN_ATTENTE_HOMOLOGATION;
        }
        return LiquidationPartageBeVerdict.EN_COURS;
    }

    // ------------------------------------------------------------------
    // Bases juridiques et messages
    // ------------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "CJ art. 1207-1209 (désignation du notaire-liquidateur)",
                "CJ art. 1210-1213 (ouverture des opérations, inventaire)",
                "CJ art. 1214 (état liquidatif / projet de partage)",
                "CJ art. 1218 (notification du projet, contredits dans le mois, procès-verbal de dires)",
                "CJ art. 1223-1224 (homologation par le Tribunal de la famille)");
    }

    /**
     * Construit les messages : verdict, délai de contredits, anomalies de séquence
     * et pièces à vérifier. Ordre stable pour des snapshots JSON déterministes.
     */
    private static List<String> construireMessages(LiquidationPartageBeInput in,
                                                   LiquidationPartageBeVerdict verdict,
                                                   DelaiProcedure delai) {
        List<String> msgs = new ArrayList<>();

        switch (verdict) {
            case PROCEDURE_NON_ENGAGEE -> msgs.add(
                    "La procédure de liquidation-partage n'est pas engagée : le notaire-liquidateur "
                            + "n'a pas encore été désigné par le Tribunal de la famille.");
            case EN_COURS -> msgs.add(
                    "La procédure de liquidation-partage est en cours devant le notaire commis.");
            case DELAI_CONTREDITS_CRITIQUE -> msgs.add(
                    "Vigilance : le délai de contredits sur le projet de liquidation-partage est "
                            + "critique ou dépassé.");
            case EN_ATTENTE_HOMOLOGATION -> msgs.add(
                    "Le procès-verbal de dires et difficultés est établi : la procédure est en "
                            + "attente de l'homologation par le Tribunal de la famille.");
            case CLOTUREE -> msgs.add(
                    "La procédure de liquidation-partage est clôturée : l'état liquidatif a été "
                            + "homologué par le Tribunal de la famille.");
        }

        if (delai.statut() == DelaiStatutBe.CRITIQUE) {
            msgs.add("Le projet de liquidation a été notifié le " + format(delai.dateDepart())
                    + " : le délai d'un mois pour déposer les contredits expire le "
                    + format(delai.dateEcheance()) + ".");
        } else if (delai.statut() == DelaiStatutBe.DEPASSE && !Boolean.TRUE.equals(in.contreditsDeposes())) {
            msgs.add("Le délai d'un mois pour déposer les contredits, ouvert par la notification "
                    + "du " + format(delai.dateDepart()) + ", est expiré depuis le "
                    + format(delai.dateEcheance()) + " sans contredits actés.");
        }

        ajouterAnomaliesSequence(in, msgs);

        if (Boolean.TRUE.equals(in.projetLiquidationEtabli()) && in.dateNotificationProjet() == null) {
            msgs.add("Le projet de liquidation est établi mais n'a pas encore été notifié : "
                    + "renseigner la date de notification fera courir le délai de contredits.");
        }
        return msgs;
    }

    /**
     * Détecte les anomalies de séquence : une étape franchie alors qu'une étape
     * antérieure ne l'est pas (la procédure n'est pas linéaire dans le dossier).
     */
    private static void ajouterAnomaliesSequence(LiquidationPartageBeInput in, List<String> msgs) {
        boolean[] franchie = new boolean[9];
        franchie[1] = Boolean.TRUE.equals(in.notaireDesigne());
        franchie[2] = Boolean.TRUE.equals(in.operationsOuvertes());
        franchie[3] = Boolean.TRUE.equals(in.inventaireEtabli());
        franchie[4] = Boolean.TRUE.equals(in.projetLiquidationEtabli());
        franchie[5] = in.dateNotificationProjet() != null;
        franchie[6] = Boolean.TRUE.equals(in.contreditsDeposes());
        franchie[7] = Boolean.TRUE.equals(in.procesVerbalDiresEtabli());
        franchie[8] = Boolean.TRUE.equals(in.homologationDemandee()) && in.dateHomologation() != null;

        String[] libelles = {
                "", "désignation du notaire", "ouverture des opérations",
                "établissement de l'inventaire", "établissement du projet de liquidation",
                "notification du projet", "dépôt des contredits",
                "établissement du procès-verbal de dires", "homologation"
        };

        Set<String> anomalies = new LinkedHashSet<>();
        for (int rang = 2; rang <= 8; rang++) {
            if (franchie[rang]) {
                for (int anterieur = 1; anterieur < rang; anterieur++) {
                    if (!franchie[anterieur]) {
                        anomalies.add("Anomalie de séquence : l'étape « " + libelles[rang]
                                + " » est renseignée alors que l'étape antérieure « "
                                + libelles[anterieur] + " » ne l'est pas — vérifier l'avancement réel "
                                + "du dossier.");
                    }
                }
            }
        }
        msgs.addAll(anomalies);
    }

    /**
     * Texte d'orientation sur la prochaine action attendue, dérivé du verdict et de
     * la première étape EN_COURS.
     */
    private static String prochaineEtape(LiquidationPartageBeInput in,
                                          List<EtapeProcedure> etapes,
                                          LiquidationPartageBeVerdict verdict,
                                          DelaiProcedure delai) {
        if (verdict == LiquidationPartageBeVerdict.CLOTUREE) {
            return "La procédure est clôturée : aucune action complémentaire requise.";
        }
        if (verdict == LiquidationPartageBeVerdict.DELAI_CONTREDITS_CRITIQUE) {
            if (delai.statut() == DelaiStatutBe.DEPASSE) {
                return "Acter l'absence de contredits ou régulariser la situation : le délai "
                        + "de contredits est expiré depuis le " + format(delai.dateEcheance()) + ".";
            }
            return "Déposer les contredits au notaire avant le " + format(delai.dateEcheance())
                    + " ou acter l'absence de contredits.";
        }
        return etapes.stream()
                .filter(e -> e.statut() == EtapeStatutBe.EN_COURS)
                .findFirst()
                .map(e -> "Étape suivante attendue : " + e.libelle() + " (" + e.fondement() + ").")
                .orElse("Toutes les étapes renseignées sont franchies — vérifier l'homologation.");
    }

    private static String format(LocalDate d) {
        if (d == null) {
            return "—";
        }
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
