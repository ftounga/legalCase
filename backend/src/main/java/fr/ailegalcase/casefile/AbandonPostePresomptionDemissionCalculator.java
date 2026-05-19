package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-206-01 : moteur d'analyse de la contestation d'une présomption de démission
 * par abandon de poste (FRANCE, loi 21/12/2022 — art. L.1237-1-1 CT,
 * D.1237-2-1 s. CT décret 17/04/2023).
 *
 * <p>Analyse, du point de vue de l'avocat du salarié, la <b>solidité de la
 * contestation</b> en détectant les irrégularités côté employeur (délai &lt; 15
 * jours calendaires, mode de notification autre que LRAR / remise en main propre
 * contre décharge, mentions manquantes dans la mise en demeure) et la présence
 * d'un motif légitime d'absence (médical, droit de retrait, droit de grève,
 * modification du contrat refusée, défaut de paiement du salaire) ou la reprise
 * du poste / justification dans le délai imparti.</p>
 *
 * <p><b>Verdict</b> : piloté par le cumul et la gravité des motifs détectés —
 * un motif légitime ou une reprise dans le délai suffit à caractériser une
 * <code>CONTESTATION_SOLIDE</code> (la présomption est inopérante / renversée).
 * En l'absence de motif majeur, plusieurs irrégularités de forme cumulées
 * donnent <code>CONTESTATION_SOLIDE</code> ; une irrégularité isolée
 * <code>CONTESTATION_INCERTAINE</code> ; aucune irrégularité
 * <code>CONTESTATION_DIFFICILE</code>.</p>
 *
 * <p><b>Score 0-100</b> (solidité) : somme pondérée des motifs (motif majeur
 * 50, irrégularité de forme 20), plafonné à 100. Indicateur secondaire.</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la présomption de démission par abandon
 * de poste est un mécanisme franco-français (loi 21/12/2022).</p>
 */
public final class AbandonPostePresomptionDemissionCalculator {

    /** Solidité de la contestation salariale de la présomption de démission. */
    public enum Verdict {
        CONTESTATION_SOLIDE,
        CONTESTATION_INCERTAINE,
        CONTESTATION_DIFFICILE
    }

    /** Mode de notification de la mise en demeure de reprendre le poste. */
    public enum ModeNotification {
        LRAR,
        REMISE_MAIN_PROPRE,
        AUTRE
    }

    /** Motif d'absence éventuellement invoqué par le salarié. */
    public enum MotifAbsence {
        AUCUN,
        MEDICAL,
        DROIT_RETRAIT,
        DROIT_GREVE,
        MODIFICATION_CONTRAT_REFUSEE,
        DEFAUT_PAIEMENT_SALAIRE,
        AUTRE
    }

    /** Code structuré d'un motif de contestation détecté. */
    public enum CodeContestation {
        DT42_DELAI_INSUFFISANT,
        DT42_MED_MENTION_DELAI_MANQUANTE,
        DT42_MED_MENTION_CONSEQUENCES_MANQUANTE,
        DT42_MODE_NOTIFICATION_IRREGULIER,
        DT42_MOTIF_LEGITIME,
        DT42_REPRISE_DANS_DELAI
    }

    /** Motif de contestation détecté — structure exposée dans la réponse API. */
    public record MotifContestation(
            CodeContestation code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    /** Délai minimal légal accordé au salarié pour reprendre ou justifier (D.1237-2-1). */
    private static final int DELAI_MINIMAL_JOURS = 15;
    /** Poids d'un motif renversant la présomption (motif légitime, reprise dans le délai). */
    private static final int POIDS_MOTIF_MAJEUR = 50;
    /** Poids d'une irrégularité de forme (délai, mentions, mode de notification). */
    private static final int POIDS_IRREGULARITE_FORME = 20;
    private static final int SCORE_MAX = 100;

    private AbandonPostePresomptionDemissionCalculator() {}

    /**
     * Analyse la solidité de la contestation de la présomption de démission.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("FRANCE" uniquement supporté pour cette SF)
     * @return résultat structuré (motifs détectés, score, verdict, dateExpirationDelai)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static AbandonPostePresomptionDemissionResult compute(
            AbandonPostePresomptionDemissionInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — la présomption de démission "
                            + "par abandon de poste est un mécanisme franco-français "
                            + "(loi 21/12/2022).");
        }
        if (input.dateMiseEnDemeurePresentee() == null) {
            throw new IllegalArgumentException("dateMiseEnDemeurePresentee requise");
        }
        if (input.delaiAccordeJours() == null) {
            throw new IllegalArgumentException("delaiAccordeJours requis");
        }
        if (input.delaiAccordeJours() < 0) {
            throw new IllegalArgumentException("delaiAccordeJours doit être positif ou nul");
        }
        if (input.motifAbsenceCommentaire() != null
                && input.motifAbsenceCommentaire().length() > 1000) {
            throw new IllegalArgumentException(
                    "motifAbsenceCommentaire ne peut dépasser 1000 caractères");
        }

        List<MotifContestation> motifs = detecterMotifs(input);
        int score = scorePour(motifs);
        Verdict verdict = verdictPour(motifs);
        LocalDate dateExpirationDelai = input.dateMiseEnDemeurePresentee()
                .plusDays(input.delaiAccordeJours());
        List<String> bases = basesJuridiques(motifs);
        List<String> messages = construireMessages(input, motifs, verdict);

        return new AbandonPostePresomptionDemissionResult(
                List.copyOf(motifs),
                score,
                verdict,
                dateExpirationDelai,
                bases,
                messages,
                countryNormalized
        );
    }

    /**
     * Détecte les motifs de contestation selon les règles figées de la mini-spec.
     * Ordre stable (LinkedHashSet de codes) pour des snapshots JSON déterministes.
     */
    private static List<MotifContestation> detecterMotifs(AbandonPostePresomptionDemissionInput in) {
        Set<CodeContestation> codes = new LinkedHashSet<>();
        List<MotifContestation> motifs = new ArrayList<>();

        // 1 — DT42_DELAI_INSUFFISANT (D.1237-2-1) : délai < 15 jours calendaires
        if (in.delaiAccordeJours() != null && in.delaiAccordeJours() < DELAI_MINIMAL_JOURS) {
            codes.add(CodeContestation.DT42_DELAI_INSUFFISANT);
            motifs.add(new MotifContestation(
                    CodeContestation.DT42_DELAI_INSUFFISANT,
                    "Délai accordé inférieur à 15 jours calendaires",
                    "Art. D.1237-2-1 C. trav. (décret 17/04/2023)",
                    POIDS_IRREGULARITE_FORME,
                    "Le décret du 17 avril 2023 (D.1237-2-1) impose à l'employeur "
                            + "d'accorder au salarié un délai d'au moins 15 jours calendaires "
                            + "à compter de la présentation de la mise en demeure. Délai "
                            + "constaté : " + in.delaiAccordeJours() + " jour(s)."));
        }

        // 2 — DT42_MED_MENTION_DELAI_MANQUANTE (D.1237-2-1) : MED sans mention du délai
        if (Boolean.FALSE.equals(in.miseEnDemeureMentionneDelai())) {
            codes.add(CodeContestation.DT42_MED_MENTION_DELAI_MANQUANTE);
            motifs.add(new MotifContestation(
                    CodeContestation.DT42_MED_MENTION_DELAI_MANQUANTE,
                    "Mise en demeure ne mentionnant pas le délai imparti",
                    "Art. D.1237-2-1 C. trav.",
                    POIDS_IRREGULARITE_FORME,
                    "La mise en demeure doit indiquer expressément le délai imparti au "
                            + "salarié pour reprendre son poste ou justifier son absence. "
                            + "L'omission de cette mention prive la procédure d'effet."));
        }

        // 3 — DT42_MED_MENTION_CONSEQUENCES_MANQUANTE : MED sans mention des conséquences
        if (Boolean.FALSE.equals(in.miseEnDemeureMentionneConsequences())) {
            codes.add(CodeContestation.DT42_MED_MENTION_CONSEQUENCES_MANQUANTE);
            motifs.add(new MotifContestation(
                    CodeContestation.DT42_MED_MENTION_CONSEQUENCES_MANQUANTE,
                    "Mise en demeure ne mentionnant pas les conséquences (présomption de démission)",
                    "Art. L.1237-1-1 et D.1237-2-1 C. trav.",
                    POIDS_IRREGULARITE_FORME,
                    "La mise en demeure doit informer le salarié des conséquences "
                            + "(présomption de démission) à défaut de reprise ou de "
                            + "justification dans le délai. L'omission de cette mention "
                            + "prive la procédure d'effet."));
        }

        // 4 — DT42_MODE_NOTIFICATION_IRREGULIER (D.1237-2-1) :
        //     mode ≠ LRAR et ≠ REMISE_MAIN_PROPRE (donc AUTRE ou inconnu)
        if (in.modeNotificationMiseEnDemeure() == ModeNotification.AUTRE) {
            codes.add(CodeContestation.DT42_MODE_NOTIFICATION_IRREGULIER);
            motifs.add(new MotifContestation(
                    CodeContestation.DT42_MODE_NOTIFICATION_IRREGULIER,
                    "Mise en demeure notifiée par un mode irrégulier",
                    "Art. D.1237-2-1 C. trav.",
                    POIDS_IRREGULARITE_FORME,
                    "Le décret impose une notification par lettre recommandée avec "
                            + "accusé de réception ou par remise en main propre contre "
                            + "décharge. Tout autre mode (courriel simple, SMS, oral…) "
                            + "vicie la procédure."));
        }

        // 5 — DT42_MOTIF_LEGITIME : motif d'absence ≠ AUCUN (renverse la présomption)
        if (in.motifAbsenceInvoque() != null
                && in.motifAbsenceInvoque() != MotifAbsence.AUCUN) {
            codes.add(CodeContestation.DT42_MOTIF_LEGITIME);
            motifs.add(new MotifContestation(
                    CodeContestation.DT42_MOTIF_LEGITIME,
                    "Motif légitime d'absence invoqué : " + libelleMotif(in.motifAbsenceInvoque()),
                    fondementMotif(in.motifAbsenceInvoque()),
                    POIDS_MOTIF_MAJEUR,
                    explicationMotif(in.motifAbsenceInvoque())));
        }

        // 6 — DT42_REPRISE_DANS_DELAI : reprise ou justification dans le délai
        //     (présomption inopérante).
        if (Boolean.TRUE.equals(in.repriseOuJustificationDansDelai())) {
            codes.add(CodeContestation.DT42_REPRISE_DANS_DELAI);
            motifs.add(new MotifContestation(
                    CodeContestation.DT42_REPRISE_DANS_DELAI,
                    "Reprise du poste ou justification de l'absence dans le délai imparti",
                    "Art. L.1237-1-1 C. trav.",
                    POIDS_MOTIF_MAJEUR,
                    "Si le salarié a repris son poste ou justifié son absence dans le "
                            + "délai accordé par l'employeur, la présomption de démission "
                            + "ne peut juridiquement opérer."));
        }

        return motifs;
    }

    private static String libelleMotif(MotifAbsence m) {
        return switch (m) {
            case MEDICAL -> "motif médical";
            case DROIT_RETRAIT -> "exercice du droit de retrait";
            case DROIT_GREVE -> "exercice du droit de grève";
            case MODIFICATION_CONTRAT_REFUSEE -> "refus d'une modification du contrat de travail";
            case DEFAUT_PAIEMENT_SALAIRE -> "défaut de paiement du salaire";
            case AUTRE -> "autre motif légitime invoqué";
            case AUCUN -> "aucun";
        };
    }

    private static String fondementMotif(MotifAbsence m) {
        return switch (m) {
            case MEDICAL -> "Arrêt de travail / certificat médical — L.1226-1+ C. trav.";
            case DROIT_RETRAIT -> "Art. L.4131-1 et L.4131-3 C. trav.";
            case DROIT_GREVE -> "Préambule Constitution 1946 al. 7 ; art. L.2511-1 C. trav.";
            case MODIFICATION_CONTRAT_REFUSEE -> "Cass. soc. 10/07/1996 — refus légitime de modification";
            case DEFAUT_PAIEMENT_SALAIRE -> "Cass. soc. 02/02/2022 n°20-21.290 — défaut de paiement du salaire";
            case AUTRE -> "Motif d'absence à qualifier juridiquement";
            case AUCUN -> "";
        };
    }

    private static String explicationMotif(MotifAbsence m) {
        return switch (m) {
            case MEDICAL -> "L'absence justifiée par un arrêt de travail ou un certificat "
                    + "médical fait obstacle à la présomption de démission : la jurisprudence "
                    + "constante (Cass. soc.) écarte la qualification d'abandon de poste en "
                    + "présence d'un motif médical avéré.";
            case DROIT_RETRAIT -> "Le salarié qui exerce son droit de retrait face à un "
                    + "danger grave et imminent ne peut être considéré comme ayant abandonné "
                    + "son poste — l'absence est juridiquement légitime.";
            case DROIT_GREVE -> "La participation à un mouvement de grève licite ne "
                    + "constitue pas un abandon de poste : la suspension du contrat de "
                    + "travail au titre du droit de grève fait tomber la présomption.";
            case MODIFICATION_CONTRAT_REFUSEE -> "Le refus du salarié d'accepter une "
                    + "modification de son contrat de travail (rémunération, durée, "
                    + "qualification, lieu) ne peut s'analyser en abandon de poste — "
                    + "l'employeur ne peut imposer cette modification.";
            case DEFAUT_PAIEMENT_SALAIRE -> "Le salarié peut légitimement refuser de "
                    + "poursuivre l'exécution du contrat face au non-paiement durable du "
                    + "salaire (Cass. soc. 02/02/2022) : l'absence n'est pas fautive.";
            case AUTRE -> "Un autre motif légitime est invoqué — à qualifier juridiquement "
                    + "au regard des pièces du dossier et de la jurisprudence applicable.";
            case AUCUN -> "";
        };
    }

    /**
     * Score indicatif = somme pondérée des motifs. Motif majeur (motif légitime,
     * reprise dans le délai) = 50, irrégularité de forme = 20. Plafonné à 100.
     */
    private static int scorePour(List<MotifContestation> motifs) {
        int score = 0;
        for (MotifContestation m : motifs) {
            score += m.poids();
        }
        return Math.min(SCORE_MAX, score);
    }

    /**
     * Verdict piloté par le cumul et la gravité des motifs détectés.
     *
     * <ul>
     *   <li>Motif majeur (motif légitime, reprise dans le délai) → SOLIDE.</li>
     *   <li>≥ 2 irrégularités de forme cumulées → SOLIDE.</li>
     *   <li>1 irrégularité isolée → INCERTAINE.</li>
     *   <li>Aucun motif → DIFFICILE.</li>
     * </ul>
     */
    private static Verdict verdictPour(List<MotifContestation> motifs) {
        boolean hasMajeur = motifs.stream().anyMatch(m -> m.poids() >= POIDS_MOTIF_MAJEUR);
        if (hasMajeur) {
            return Verdict.CONTESTATION_SOLIDE;
        }
        if (motifs.size() >= 2) {
            return Verdict.CONTESTATION_SOLIDE;
        }
        if (motifs.size() == 1) {
            return Verdict.CONTESTATION_INCERTAINE;
        }
        return Verdict.CONTESTATION_DIFFICILE;
    }

    private static List<String> basesJuridiques(List<MotifContestation> motifs) {
        Set<String> bases = new LinkedHashSet<>();
        bases.add("Art. L.1237-1-1 C. trav. (loi 21/12/2022)");
        bases.add("Art. D.1237-2-1 s. C. trav. (décret 17/04/2023)");
        for (MotifContestation m : motifs) {
            if (m.fondement() != null && !m.fondement().isBlank()) {
                bases.add(m.fondement());
            }
        }
        return new ArrayList<>(bases);
    }

    private static List<String> construireMessages(AbandonPostePresomptionDemissionInput in,
                                                   List<MotifContestation> motifs,
                                                   Verdict verdict) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case CONTESTATION_SOLIDE -> msgs.add(
                    "Contestation solide : au moins un motif majeur ou un cumul "
                            + "d'irrégularités caractérise une présomption fragile. "
                            + "La saisine du CPH en référé peut être envisagée.");
            case CONTESTATION_INCERTAINE -> msgs.add(
                    "Contestation incertaine : un seul motif est identifié — "
                            + "vérifier les pièces du dossier et compléter avant "
                            + "d'arrêter la stratégie contentieuse.");
            case CONTESTATION_DIFFICILE -> msgs.add(
                    "Contestation difficile : aucune irrégularité détectée à partir "
                            + "des éléments saisis — la présomption de démission "
                            + "paraît opérer. Examiner d'éventuelles autres voies "
                            + "(prise d'acte, demande d'indemnités de rupture).");
        }
        // Pièces à vérifier au dossier.
        msgs.add("Vérifier au dossier la lettre de mise en demeure (Pièce) et l'AR "
                + "de la LRAR ou la décharge de remise en main propre.");
        if (in.motifAbsenceInvoque() == MotifAbsence.MEDICAL) {
            msgs.add("Joindre l'arrêt de travail / certificat médical correspondant à "
                    + "la période d'absence pour appuyer le motif légitime.");
        }
        if (in.motifAbsenceInvoque() == MotifAbsence.DEFAUT_PAIEMENT_SALAIRE) {
            msgs.add("Documenter le défaut de paiement du salaire par bulletins de "
                    + "paie non honorés ou mises en demeure préalables de l'employeur.");
        }
        return msgs;
    }
}
