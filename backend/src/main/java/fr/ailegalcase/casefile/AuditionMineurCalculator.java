package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-13 : calculateur statique pour l'audition du mineur par le JAF en
 * droit français (art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC + CIDE art. 12).
 *
 * <p>Le calculateur :</p>
 * <ul>
 *   <li>évalue le droit à l'audition (capacité de discernement +
 *       demande, ou demande formulée par l'enfant lui-même) ;</li>
 *   <li>recommande une modalité (seul / avec avocat / avec tiers) ;</li>
 *   <li>signale les refus du juge non valablement motivés (voie de
 *       recours — Cass. 1ère civ., 18/3/2015, n°14-11.392) ;</li>
 *   <li>alerte si l'enfant a moins de 5 ans (discernement hautement
 *       improbable) ;</li>
 *   <li>alerte en contexte conflictuel sur le risque de manipulation.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, l'audition du mineur
 * est régie par art. 1004/1 et 1004/2 CJ avec un régime distinct (outil
 * F-FA-BE-AUDITION-MINEUR futur, hors périmètre F-216).</p>
 */
public final class AuditionMineurCalculator {

    static final String BASE_JURIDIQUE =
            "art. 388-1 Cciv (droit à l'audition du mineur capable de "
                    + "discernement) + art. 1074-1 à 1074-3 CPC (modalités "
                    + "procédurales) + CIDE art. 12 + Cass. 1ère civ., "
                    + "18/3/2015, n°14-11.392 (motivation impérative du refus)";

    /**
     * Seuil indicatif jurisprudentiel : en dessous, le discernement est
     * généralement considéré comme hautement improbable. La jurisprudence
     * apprécie <i>in concreto</i> et il n'existe pas de seuil légal.
     */
    static final int AGE_DISCERNEMENT_IMPROBABLE = 5;

    /** Âge maximal d'application — au-delà, mineur émancipé ou majeur. */
    static final int AGE_MAJORITE = 18;

    private AuditionMineurCalculator() {}

    /**
     * Évalue les conditions de l'audition du mineur et émet le verdict +
     * les alertes correspondantes.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE} ou âge
     *                                  hors plage [0, 17].
     */
    public static AuditionMineurResult compute(
            AuditionMineurRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-AUDITION-MINEUR applicable uniquement en "
                            + "France (art. 388-1 Cciv).");
        }
        if (req == null || req.ageEnfant() == null) {
            throw new IllegalArgumentException("ageEnfant est requis.");
        }
        int age = req.ageEnfant();
        if (age < 0 || age >= AGE_MAJORITE) {
            throw new IllegalArgumentException(
                    "ageEnfant doit être compris entre 0 et 17 (art. 388-1 "
                            + "Cciv vise les mineurs).");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        CapaciteDiscernementEnum disc = req.capaciteDiscernement() != null
                ? req.capaciteDiscernement()
                : CapaciteDiscernementEnum.INCONNUE;
        boolean demandeFormalisee = Boolean.TRUE.equals(req.demandeFormalisee());
        boolean demandeParEnfantLuiMeme = Boolean.TRUE.equals(req.demandeParEnfantLuiMeme());
        boolean refusMotive = Boolean.TRUE.equals(req.refusMotive());
        String motivationRefus = req.motivationRefus();
        ProcedureAuditionEnum procedure = req.procedureEnCours();

        // 1. Alerte âge — discernement hautement improbable < 5 ans
        boolean discernementImprobableParAge = age < AGE_DISCERNEMENT_IMPROBABLE;
        if (discernementImprobableParAge) {
            alertes.add("Âge de l'enfant (" + age + " ans) : le discernement "
                    + "est hautement improbable avant 5-6 ans selon la "
                    + "jurisprudence dominante. Le juge peut refuser "
                    + "l'audition pour défaut de discernement caractérisé "
                    + "(motivation obligatoire — Cass. 1ère civ., "
                    + "18/3/2015). Apprécier in concreto avec l'aide d'un "
                    + "psychologue si la situation le justifie.");
        }

        // 2. Cas spécial — demande par l'enfant lui-même (art. 388-1 al. 1)
        if (demandeParEnfantLuiMeme) {
            messages.add("L'enfant a demandé lui-même son audition : "
                    + "l'audition est de droit dès lors qu'il est capable "
                    + "de discernement (art. 388-1 al. 1 Cciv). Le juge "
                    + "ne peut refuser que par décision spécialement "
                    + "motivée fondée sur l'absence de discernement.");

            if (disc == CapaciteDiscernementEnum.CERTAINE
                    || disc == CapaciteDiscernementEnum.PROBABLE) {
                ModaliteAuditionEnum modalite = recommendModalite(age, procedure, demandeFormalisee);
                if (refusMotive) {
                    return buildRefusResult(motivationRefus, age, modalite,
                            BASE_JURIDIQUE, messages, alertes);
                }
                addModaliteMessage(messages, modalite);
                addProcedureMessage(messages, procedure);
                return new AuditionMineurResult(
                        true,
                        true,
                        modalite,
                        false,
                        "AUDITION_DE_DROIT",
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
            if (disc == CapaciteDiscernementEnum.DOUTEUSE
                    || discernementImprobableParAge) {
                alertes.add("Demande de l'enfant + discernement douteux : "
                        + "saisir un psychologue ou un expert pour évaluer "
                        + "la maturité avant l'audience. Un refus du juge "
                        + "non spécialement motivé sur ce point serait "
                        + "contestable en appel.");
                return new AuditionMineurResult(
                        false,
                        true,
                        recommendModalite(age, procedure, demandeFormalisee),
                        false,
                        "DISCERNEMENT_DOUTEUX",
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
            // INCONNUE — invite à instruire la capacité avant l'audience
            messages.add("Capacité de discernement non instruite : "
                    + "interroger le client sur les éléments concrets "
                    + "(scolarité, autonomie, expression écrite, etc.).");
            return new AuditionMineurResult(
                    false,
                    true,
                    recommendModalite(age, procedure, demandeFormalisee),
                    false,
                    "DISCERNEMENT_DOUTEUX",
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 3. Refus déjà notifié — contrôle de la motivation
        if (refusMotive) {
            ModaliteAuditionEnum modalite = recommendModalite(age, procedure, demandeFormalisee);
            return buildRefusResult(motivationRefus, age, modalite,
                    BASE_JURIDIQUE, messages, alertes);
        }

        // 4. Cas général — demande formalisée par les parties
        if (demandeFormalisee
                && (disc == CapaciteDiscernementEnum.CERTAINE
                || disc == CapaciteDiscernementEnum.PROBABLE)) {
            ModaliteAuditionEnum modalite = recommendModalite(age, procedure, demandeFormalisee);
            addModaliteMessage(messages, modalite);
            addProcedureMessage(messages, procedure);

            // Alerte manipulation — contexte conflictuel
            if (procedure == ProcedureAuditionEnum.DIVORCE
                    || procedure == ProcedureAuditionEnum.AUTORITE_PARENTALE
                    || procedure == ProcedureAuditionEnum.GARDE) {
                alertes.add("Contexte de procédure conflictuelle (divorce / "
                        + "autorité parentale / garde) : surveiller le "
                        + "risque de manipulation de l'enfant par l'un des "
                        + "parents. Privilégier la modalité « seul » et "
                        + "demander, si justifié, l'assistance d'un avocat "
                        + "pour l'enfant (art. 388-1 al. 2 Cciv).");
            }

            return new AuditionMineurResult(
                    true,
                    true,
                    modalite,
                    false,
                    "AUDITION_RECOMMANDEE",
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 5. Demande formalisée mais discernement douteux ou inconnu
        if (demandeFormalisee) {
            messages.add("Demande d'audition formalisée mais discernement "
                    + (disc == CapaciteDiscernementEnum.DOUTEUSE
                            ? "expressément douteux" : "non instruit")
                    + " : caractériser le discernement par des éléments "
                    + "concrets (scolarité, expression, maturité) avant "
                    + "l'audience. Le juge peut refuser, mais doit motiver "
                    + "spécialement (Cass. 1ère civ., 18/3/2015).");
            return new AuditionMineurResult(
                    false,
                    false,
                    recommendModalite(age, procedure, demandeFormalisee),
                    false,
                    "DISCERNEMENT_DOUTEUX",
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 6. Aucune demande — rappel d'opportunité
        messages.add("Aucune demande d'audition n'a été formalisée. "
                + "L'audition n'est pas automatique : elle suppose une "
                + "demande des parties ou de l'enfant lui-même (art. 388-1 "
                + "al. 1 Cciv). Évaluer l'opportunité avec le client.");
        return new AuditionMineurResult(
                false,
                false,
                recommendModalite(age, procedure, demandeFormalisee),
                false,
                "OK",
                BASE_JURIDIQUE,
                messages,
                alertes);
    }

    /**
     * Construit le résultat lorsqu'un refus du juge a déjà été notifié,
     * en évaluant la motivation (art. 388-1 al. 2 Cciv).
     */
    private static AuditionMineurResult buildRefusResult(
            String motivation,
            int age,
            ModaliteAuditionEnum modalite,
            String baseLegale,
            List<String> messages,
            List<String> alertes) {

        boolean motivationVide = motivation == null || motivation.isBlank();
        if (motivationVide) {
            alertes.add("Refus du juge sans motivation : le refus d'audition "
                    + "doit être SPÉCIALEMENT motivé (art. 388-1 al. 2 Cciv, "
                    + "Cass. 1ère civ., 18/3/2015, n°14-11.392). Voie de "
                    + "recours possible (appel / pourvoi). Préparer un "
                    + "moyen tiré du défaut de motivation.");
            return new AuditionMineurResult(
                    false,
                    true,
                    modalite,
                    true,
                    "REFUS_CONTESTABLE",
                    baseLegale,
                    messages,
                    alertes);
        }
        messages.add("Refus du juge motivé : « " + motivation + " ». "
                + "Apprécier si la motivation caractérise effectivement "
                + "l'absence de discernement (seul motif retenu par la "
                + "jurisprudence — Cass. 1ère civ., 18/3/2015). Sinon, "
                + "voie de recours envisageable.");
        return new AuditionMineurResult(
                false,
                true,
                modalite,
                false,
                "AUDITION_REFUSEE_VALABLEMENT",
                baseLegale,
                messages,
                alertes);
    }

    /**
     * Recommande une modalité d'audition selon l'âge, la procédure et la
     * formalisation de la demande (art. 388-1 al. 2-3 Cciv + art. 1074-2 CPC).
     */
    static ModaliteAuditionEnum recommendModalite(
            int age,
            ProcedureAuditionEnum procedure,
            boolean demandeFormalisee) {

        // Contexte conflictuel + enfant jeune (< 8 ans) → tiers / psy
        boolean conflictuel = procedure == ProcedureAuditionEnum.DIVORCE
                || procedure == ProcedureAuditionEnum.AUTORITE_PARENTALE
                || procedure == ProcedureAuditionEnum.GARDE;
        if (conflictuel && age < 8) {
            return ModaliteAuditionEnum.AVEC_TIERS;
        }
        // Demande formalisée + enfant < 12 ans → audition assistée par
        // avocat recommandée (protection accrue).
        if (demandeFormalisee && age < 12) {
            return ModaliteAuditionEnum.AVEC_AVOCAT;
        }
        // Cas standard — audition par le juge seul.
        return ModaliteAuditionEnum.SEUL;
    }

    private static void addModaliteMessage(List<String> messages, ModaliteAuditionEnum m) {
        switch (m) {
            case SEUL -> messages.add("Modalité recommandée : audition par "
                    + "le juge SEUL, hors présence des parties (art. 388-1 "
                    + "al. 3 Cciv + art. 1074-2 CPC). Mode par défaut — "
                    + "protège la parole de l'enfant.");
            case AVEC_AVOCAT -> messages.add("Modalité recommandée : "
                    + "audition AVEC un avocat désigné pour l'enfant "
                    + "(art. 388-1 al. 2 Cciv). Désignation par le bâtonnier "
                    + "via une demande adressée au juge. Bénéfice : "
                    + "verbalisation accompagnée, accès au dossier dans "
                    + "l'intérêt de l'enfant.");
            case AVEC_TIERS -> messages.add("Modalité recommandée : "
                    + "audition AVEC un tiers de confiance (psychologue, "
                    + "travailleur social — art. 1074-2 CPC). Adaptée aux "
                    + "enfants jeunes ou aux contextes conflictuels.");
        }
    }

    private static void addProcedureMessage(List<String> messages, ProcedureAuditionEnum p) {
        if (p == null) return;
        switch (p) {
            case DIVORCE -> messages.add("Procédure de divorce : l'audition "
                    + "peut éclairer le juge sur la résidence et les "
                    + "modalités d'exercice de l'autorité parentale "
                    + "(art. 373-2-6 Cciv). Ne jamais formuler de question "
                    + "induisant un choix entre les parents.");
            case AUTORITE_PARENTALE -> messages.add("Procédure autorité "
                    + "parentale : audition pertinente pour apprécier "
                    + "l'intérêt supérieur de l'enfant (art. 373-2-6 Cciv) "
                    + "et la mise en œuvre concrète des modalités.");
            case GARDE -> messages.add("Procédure garde / résidence : "
                    + "audition pertinente pour apprécier l'opinion de "
                    + "l'enfant sur ses modalités de vie. Articuler avec "
                    + "l'art. 373-2-11 Cciv (critères de fixation).");
            case SUCCESSION -> messages.add("Procédure successorale "
                    + "impliquant un mineur : audition rare en pratique. "
                    + "Le mineur est représenté par son administrateur "
                    + "légal (parents) — sauf opposition d'intérêts.");
            case AUTRE -> {
                // Pas de message spécifique.
            }
        }
    }
}
