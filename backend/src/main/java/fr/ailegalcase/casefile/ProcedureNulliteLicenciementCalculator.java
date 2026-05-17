package fr.ailegalcase.casefile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-DT-36-01 : moteur de détection des vices de procédure de licenciement (FR).
 *
 * <p>Analyse les 10 critères de vice de forme côté employeur (absence / délai de
 * convocation, absence d'entretien préalable, notification prématurée, absence /
 * non-motivation de la lettre, motivation insuffisante, faute grave sans procédure,
 * procédure CSE d'un licenciement collectif, procédure conventionnelle), rend un
 * verdict en 3 niveaux <b>piloté par la gravité</b> des vices détectés, et calcule
 * un score 0-100 servant d'indicateur secondaire d'ampleur du cumul.</p>
 *
 * <p><b>Logique du verdict</b> : juridiquement, un seul vice substantiel avéré suffit
 * à caractériser l'irrégularité de la procédure. Le verdict est donc dérivé de la
 * gravité (≥ 1 vice AVERE → nullité avérée), non d'un seuil de score additif. Le
 * score n'est qu'un indicateur d'ampleur affiché à titre informatif.</p>
 *
 * <p><b>Gravité des vices</b> : 8 vices reposent sur des faits objectifs (dates,
 * absence d'acte) → gravité AVERE. 2 vices reposent sur une appréciation à confirmer
 * par pièce — la suffisance de la motivation (appréciation du juge) et le non-respect
 * de la procédure conventionnelle (dépend de la clause exacte de la CCN) → gravité
 * PROBABLE. Un dossier ne comportant que ces vices rend le verdict NULLITE_PROBABLE.</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement. La nullité de procédure belge repose sur des
 * mécaniques distinctes (Loi 1978 + CCT 109) et fera l'objet d'une feature jumelle.</p>
 *
 * <p><b>Validation juridique requise</b> : les fondements légaux, les délais (5 jours
 * ouvrables L.1232-2, 2 jours ouvrables L.1232-6) et le barème de score sont centralisés
 * ici (source unique) et doivent être relus par un avocat avant mise en production.</p>
 */
public final class ProcedureNulliteLicenciementCalculator {

    /** Verdict d'analyse des vices de procédure, piloté par la gravité des vices. */
    public enum Verdict {
        NULLITE_AVEREE,        // au moins un vice de gravité AVERE
        NULLITE_PROBABLE,      // aucun vice AVERE mais au moins un vice PROBABLE
        PROCEDURE_REGULIERE    // aucun vice détecté
    }

    /** Gravité d'un vice détecté. */
    public enum Gravite {
        AVERE,     // vice caractérisé
        PROBABLE   // présomption, à confirmer par pièce
    }

    /** Code structuré d'un vice de procédure. */
    public enum CodeVice {
        ABSENCE_CONVOCATION,
        DELAI_CONVOCATION_INSUFFISANT,
        ABSENCE_ENTRETIEN,
        NOTIFICATION_PREMATUREE,
        ABSENCE_LETTRE,
        LETTRE_NON_MOTIVEE,
        MOTIVATION_INSUFFISANTE,
        ABSENCE_CONVOCATION_MOTIF_GRAVE,
        PROCEDURE_CSE_NON_RESPECTEE,
        CONVENTION_COLLECTIVE_NON_RESPECTEE
    }

    /** Vice détecté — structure exposée dans la réponse API. */
    public record ViceDetecte(
            CodeVice code,
            String libelle,
            String fondement,
            Gravite gravite,
            String explication
    ) {}

    /** Délai minimal entre présentation de la convocation et entretien (L.1232-2). */
    private static final int DELAI_CONVOCATION_JOURS_OUVRABLES = 5;
    /** Délai minimal entre entretien et notification du licenciement (L.1232-6). */
    private static final int DELAI_NOTIFICATION_JOURS_OUVRABLES = 2;

    /** Pondération des vices pour le score indicatif : vice grave = 30, vice modéré = 20. */
    private static final int POIDS_VICE_GRAVE = 30;
    private static final int POIDS_VICE_MODERE = 20;

    private static final int SCORE_MAX = 100;

    private ProcedureNulliteLicenciementCalculator() {}

    /**
     * Analyse les vices de procédure de licenciement.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("FRANCE" uniquement supporté pour cette SF)
     * @return résultat structuré (vices détectés, score, verdict, bases juridiques)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static ProcedureNulliteResult compute(ProcedureNulliteLicenciementInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — pendant BE en cours de scoping");
        }
        validateCommentaire(input.motivationCommentaire(), "motivationCommentaire");
        validateCommentaire(input.conventionCollectiveCommentaire(), "conventionCollectiveCommentaire");

        List<ViceDetecte> vices = detecterVices(input);
        int score = scorePour(vices);
        Verdict verdict = verdictPour(vices);

        List<String> bases = basesJuridiques(vices);
        List<String> messages = construireMessages(input, vices, verdict);

        return new ProcedureNulliteResult(
                List.copyOf(vices),
                score,
                verdict,
                bases,
                messages,
                countryNormalized
        );
    }

    private static void validateCommentaire(String commentaire, String champ) {
        if (commentaire != null && commentaire.length() > 1000) {
            throw new IllegalArgumentException("Le champ " + champ + " ne peut dépasser 1000 caractères");
        }
    }

    /**
     * Détecte les 10 vices de procédure selon les règles figées de la mini-spec.
     * Ordre stable (LinkedHashSet de codes) pour des snapshots JSON déterministes.
     */
    private static List<ViceDetecte> detecterVices(ProcedureNulliteLicenciementInput in) {
        Set<CodeVice> codes = new LinkedHashSet<>();
        List<ViceDetecte> vices = new ArrayList<>();

        boolean absenceConvocation = Boolean.FALSE.equals(in.convocationEnvoyee());
        boolean absenceEntretien = Boolean.FALSE.equals(in.entretienTenu());

        // 1 — ABSENCE_CONVOCATION (L.1232-2) : convocationEnvoyee = false
        if (absenceConvocation) {
            codes.add(CodeVice.ABSENCE_CONVOCATION);
            vices.add(new ViceDetecte(
                    CodeVice.ABSENCE_CONVOCATION,
                    "Absence de convocation à l'entretien préalable",
                    "Art. L.1232-2 C. trav.",
                    Gravite.AVERE,
                    "L'employeur doit convoquer le salarié à un entretien préalable par "
                            + "lettre recommandée ou remise en main propre contre décharge. "
                            + "L'absence de convocation vicie la procédure."));
        }

        // 2 — DELAI_CONVOCATION_INSUFFISANT (L.1232-2) :
        //     délai (présentation convocation → entretien) < 5 jours ouvrables
        if (!absenceConvocation
                && in.dateConvocationPresentee() != null
                && in.dateEntretienPrealable() != null) {
            int joursOuvrables = joursOuvrablesEntre(
                    in.dateConvocationPresentee(), in.dateEntretienPrealable());
            if (joursOuvrables < DELAI_CONVOCATION_JOURS_OUVRABLES) {
                codes.add(CodeVice.DELAI_CONVOCATION_INSUFFISANT);
                vices.add(new ViceDetecte(
                        CodeVice.DELAI_CONVOCATION_INSUFFISANT,
                        "Délai de convocation insuffisant (moins de 5 jours ouvrables)",
                        "Art. L.1232-2 C. trav.",
                        Gravite.AVERE,
                        "L'entretien préalable ne peut avoir lieu moins de 5 jours ouvrables "
                                + "après la présentation de la lettre de convocation. Délai "
                                + "constaté : " + joursOuvrables + " jour(s) ouvrable(s)."));
            }
        }

        // 3 — ABSENCE_ENTRETIEN (L.1232-2) : entretienTenu = false
        if (absenceEntretien) {
            codes.add(CodeVice.ABSENCE_ENTRETIEN);
            vices.add(new ViceDetecte(
                    CodeVice.ABSENCE_ENTRETIEN,
                    "Entretien préalable non tenu",
                    "Art. L.1232-2 C. trav.",
                    Gravite.AVERE,
                    "L'entretien préalable est une formalité substantielle ; son absence "
                            + "constitue une irrégularité de procédure."));
        }

        // 4 — NOTIFICATION_PREMATUREE (L.1232-6) :
        //     délai (entretien → notification) < 2 jours ouvrables
        if (!absenceEntretien
                && in.dateEntretienPrealable() != null
                && in.dateNotificationLicenciement() != null) {
            int joursOuvrables = joursOuvrablesEntre(
                    in.dateEntretienPrealable(), in.dateNotificationLicenciement());
            if (joursOuvrables < DELAI_NOTIFICATION_JOURS_OUVRABLES) {
                codes.add(CodeVice.NOTIFICATION_PREMATUREE);
                vices.add(new ViceDetecte(
                        CodeVice.NOTIFICATION_PREMATUREE,
                        "Notification du licenciement prématurée (moins de 2 jours ouvrables)",
                        "Art. L.1232-6 C. trav.",
                        Gravite.AVERE,
                        "La lettre de licenciement ne peut être expédiée moins de 2 jours "
                                + "ouvrables après l'entretien préalable. Délai constaté : "
                                + joursOuvrables + " jour(s) ouvrable(s)."));
            }
        }

        // 5 — ABSENCE_LETTRE (L.1232-6) : lettreLicenciementEcrite = false
        boolean absenceLettre = Boolean.FALSE.equals(in.lettreLicenciementEcrite());
        if (absenceLettre) {
            codes.add(CodeVice.ABSENCE_LETTRE);
            vices.add(new ViceDetecte(
                    CodeVice.ABSENCE_LETTRE,
                    "Absence de lettre de licenciement écrite",
                    "Art. L.1232-6 C. trav.",
                    Gravite.AVERE,
                    "Le licenciement doit être notifié par lettre recommandée avec accusé "
                            + "de réception. L'absence d'écrit prive le licenciement de cause "
                            + "réelle et sérieuse."));
        }

        // 6 — LETTRE_NON_MOTIVEE (L.1232-6) : lettreMotivee = false
        boolean lettreNonMotivee = Boolean.FALSE.equals(in.lettreMotivee());
        if (lettreNonMotivee) {
            codes.add(CodeVice.LETTRE_NON_MOTIVEE);
            vices.add(new ViceDetecte(
                    CodeVice.LETTRE_NON_MOTIVEE,
                    "Lettre de licenciement sans énoncé des motifs",
                    "Art. L.1232-6 C. trav.",
                    Gravite.AVERE,
                    "La lettre de licenciement doit énoncer le ou les motifs invoqués. "
                            + "L'absence de motif équivaut à une absence de cause réelle et "
                            + "sérieuse."));
        }

        // 7 — MOTIVATION_INSUFFISANTE (L.1232-6, L.1235-2) :
        //     lettreMotivee = true ET motivationSuffisante = false
        //     Gravité PROBABLE : la suffisance de la motivation relève de
        //     l'appréciation du juge du fond — à confirmer.
        if (Boolean.TRUE.equals(in.lettreMotivee())
                && Boolean.FALSE.equals(in.motivationSuffisante())) {
            codes.add(CodeVice.MOTIVATION_INSUFFISANTE);
            vices.add(new ViceDetecte(
                    CodeVice.MOTIVATION_INSUFFISANTE,
                    "Motivation de la lettre insuffisante",
                    "Art. L.1232-6 et L.1235-2 C. trav.",
                    Gravite.PROBABLE,
                    "Une lettre dont les motifs sont imprécis ou non matériellement "
                            + "vérifiables peut rendre le licenciement sans cause réelle et "
                            + "sérieuse — appréciation soumise au juge du fond."));
        }

        // 8 — ABSENCE_CONVOCATION_MOTIF_GRAVE (L.1234-9, L.1332-2) :
        //     motif grave ET (absence convocation OU absence entretien)
        if (Boolean.TRUE.equals(in.licenciementPourMotifGrave())
                && (absenceConvocation || absenceEntretien)) {
            codes.add(CodeVice.ABSENCE_CONVOCATION_MOTIF_GRAVE);
            vices.add(new ViceDetecte(
                    CodeVice.ABSENCE_CONVOCATION_MOTIF_GRAVE,
                    "Faute grave/lourde sans procédure de convocation respectée",
                    "Art. L.1234-9 et L.1332-2 C. trav.",
                    Gravite.AVERE,
                    "Même en cas de faute grave ou lourde, l'employeur doit respecter la "
                            + "procédure de convocation et d'entretien préalable. Son absence "
                            + "rend la procédure irrégulière."));
        }

        // 9 — PROCEDURE_CSE_NON_RESPECTEE (L.1233-8, L.1233-28 et s.) :
        //     licenciement collectif ET procédure CSE non respectée
        if (Boolean.TRUE.equals(in.licenciementCollectif())
                && Boolean.FALSE.equals(in.procedureCseRespectee())) {
            codes.add(CodeVice.PROCEDURE_CSE_NON_RESPECTEE);
            vices.add(new ViceDetecte(
                    CodeVice.PROCEDURE_CSE_NON_RESPECTEE,
                    "Procédure d'information/consultation du CSE non respectée",
                    "Art. L.1233-8, L.1233-28 et s. C. trav.",
                    Gravite.AVERE,
                    "En cas de licenciement collectif, l'employeur doit informer et "
                            + "consulter le CSE. Le non-respect de cette procédure vicie le "
                            + "licenciement."));
        }

        // 10 — CONVENTION_COLLECTIVE_NON_RESPECTEE (clause de la CCN) :
        //      CCN applicable ET procédure conventionnelle non respectée
        //      Gravité PROBABLE : l'existence et la portée de la procédure
        //      conventionnelle dépendent de la clause exacte de la CCN — à vérifier.
        if (Boolean.TRUE.equals(in.conventionCollectiveApplicable())
                && Boolean.FALSE.equals(in.conventionCollectiveRespectee())) {
            codes.add(CodeVice.CONVENTION_COLLECTIVE_NON_RESPECTEE);
            vices.add(new ViceDetecte(
                    CodeVice.CONVENTION_COLLECTIVE_NON_RESPECTEE,
                    "Procédure conventionnelle non respectée",
                    "Clause de la convention collective applicable",
                    Gravite.PROBABLE,
                    "La convention collective applicable peut imposer une procédure "
                            + "préalable (entretien disciplinaire, saisine d'une commission "
                            + "paritaire…). Son non-respect prive le licenciement de cause "
                            + "réelle et sérieuse."));
        }

        return vices;
    }

    /**
     * Score indicatif = somme pondérée des vices. Vice grave (gravité AVERE) = 30,
     * vice modéré (gravité PROBABLE) = 20. Plafonné à 100. Sert uniquement à jauger
     * l'ampleur du cumul de vices — il ne pilote pas le verdict.
     */
    private static int scorePour(List<ViceDetecte> vices) {
        int score = 0;
        for (ViceDetecte v : vices) {
            score += (v.gravite() == Gravite.AVERE) ? POIDS_VICE_GRAVE : POIDS_VICE_MODERE;
        }
        return Math.min(SCORE_MAX, score);
    }

    /**
     * Verdict piloté par la gravité des vices, non par un seuil de score additif.
     * Juridiquement, un seul vice substantiel avéré suffit à caractériser
     * l'irrégularité : un unique vice AVERE rend donc le verdict NULLITE_AVEREE.
     */
    private static Verdict verdictPour(List<ViceDetecte> vices) {
        if (vices.stream().anyMatch(v -> v.gravite() == Gravite.AVERE)) {
            return Verdict.NULLITE_AVEREE;
        }
        if (!vices.isEmpty()) {
            return Verdict.NULLITE_PROBABLE;
        }
        return Verdict.PROCEDURE_REGULIERE;
    }

    private static List<String> basesJuridiques(List<ViceDetecte> vices) {
        Set<String> bases = new LinkedHashSet<>();
        for (ViceDetecte v : vices) {
            bases.add(v.fondement());
        }
        return new ArrayList<>(bases);
    }

    private static List<String> construireMessages(ProcedureNulliteLicenciementInput in,
                                                   List<ViceDetecte> vices,
                                                   Verdict verdict) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case NULLITE_AVEREE -> msgs.add(
                    "Nullité avérée : au moins un vice grave de procédure est caractérisé. "
                            + "Le licenciement encourt la nullité ou l'absence de cause réelle "
                            + "et sérieuse.");
            case NULLITE_PROBABLE -> msgs.add(
                    "Nullité probable : un ou plusieurs vices sont identifiés — confirmer les "
                            + "pièces du dossier avant d'arbitrer la stratégie contentieuse.");
            case PROCEDURE_REGULIERE -> msgs.add(
                    "Aucun vice de procédure détecté à partir des éléments saisis — vérifier "
                            + "néanmoins le fond du licenciement (cause réelle et sérieuse).");
        }
        // Pièces à vérifier au dossier.
        if (!Boolean.FALSE.equals(in.convocationEnvoyee())) {
            msgs.add("Vérifier la disponibilité de la lettre de convocation au dossier (Pièce).");
        }
        if (Boolean.TRUE.equals(in.lettreLicenciementEcrite())) {
            msgs.add("Vérifier la disponibilité de la lettre de licenciement au dossier (Pièce).");
        }
        if (Boolean.TRUE.equals(in.licenciementCollectif()) && in.procedureCseRespectee() == null) {
            msgs.add("Licenciement collectif : statut de la procédure CSE non renseigné — "
                    + "compléter pour fiabiliser l'analyse.");
        }
        if (Boolean.TRUE.equals(in.conventionCollectiveApplicable())) {
            msgs.add("Identifier précisément la convention collective applicable et ses "
                    + "clauses procédurales disciplinaires.");
        }
        return msgs;
    }

    /**
     * Compte les jours ouvrables (lundi-vendredi) strictement entre deux dates,
     * c'est-à-dire le nombre de jours ouvrables séparant {@code debut} de {@code fin}.
     * Le jour de départ n'est pas compté, le jour d'arrivée l'est s'il est ouvrable.
     * Renvoie 0 si {@code fin} n'est pas postérieure à {@code debut}.
     */
    static int joursOuvrablesEntre(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || !fin.isAfter(debut)) {
            return 0;
        }
        int count = 0;
        LocalDate cursor = debut.plusDays(1);
        while (!cursor.isAfter(fin)) {
            DayOfWeek d = cursor.getDayOfWeek();
            if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }
}
