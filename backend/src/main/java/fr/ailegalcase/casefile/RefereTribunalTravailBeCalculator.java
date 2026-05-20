package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-207-05 : calculateur d'éligibilité au référé devant le président du
 * tribunal du travail belge.
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>Code Judiciaire art. 584</b> — référé : « le président du tribunal
 *       […] statue au provisoire dans les cas dont il reconnaît l'urgence ».
 *       Compétence du président du tribunal du travail pour les contestations
 *       relevant de la matière sociale (CJ art. 578 et s.).</li>
 *   <li><b>Code Judiciaire art. 627</b> — compétence territoriale : pour les
 *       contestations relatives à un contrat de travail, le juge du domicile
 *       du défendeur ou du lieu où l'obligation est née/exécutée
 *       (CJ art. 627, 9°).</li>
 *   <li><b>Loi du 3 juillet 1978 relative aux contrats de travail</b> —
 *       référence générale du fondement substantiel.</li>
 * </ul>
 *
 * <p>Logique : évaluation de 5 conditions cumulatives
 * (cf. {@link RefereTribunalTravailBeCondition}) puis verdict basé sur le
 * {@code scoreConditions} (nombre de booléens true) :
 * <ol>
 *   <li>Score = 5 → {@link RefereTribunalTravailBeResult.Verdict#REFERE_ELIGIBLE}
 *       + squelette généré + {@code etapeSuivante = DEPOSER_REQUETE}.</li>
 *   <li>Score 3-4 → {@link RefereTribunalTravailBeResult.Verdict#REFERE_INCERTAIN}
 *       + squelette généré + {@code etapeSuivante = RENFORCER_DOSSIER}.</li>
 *   <li>Score ≤ 2 → {@link RefereTribunalTravailBeResult.Verdict#REFERE_NON_ELIGIBLE}
 *       + squelette null + {@code etapeSuivante = ALTERNATIVE_PROCEDURE_FOND}.</li>
 * </ol>
 *
 * <p>Outil <b>BE-only</b> par construction — aucune logique FR (cf. mémoire
 * {@code feedback_belgique_never_forget}). Le référé prud'homal FR
 * (R.1454-1 CT) est un régime juridiquement distinct géré par
 * {@link ReferePrudhomalCalculator}.</p>
 */
public final class RefereTribunalTravailBeCalculator {

    /** Seuil de score minimal pour {@code REFERE_ELIGIBLE} (toutes conditions). */
    public static final int SEUIL_ELIGIBLE = 5;

    /** Seuil de score minimal pour {@code REFERE_INCERTAIN}. */
    public static final int SEUIL_INCERTAIN = 3;

    /** Longueur minimale d'une description circonstanciée d'urgence (motif AUTRE). */
    public static final int DESCRIPTION_LONGUEUR_MIN = 30;

    /** Longueur minimale d'une mesure provisoire précise. */
    public static final int MESURE_LONGUEUR_MIN = 10;

    public static final String BASE_JURIDIQUE_COMPLETE =
            "Code Judiciaire art. 584 (référé) ; CJ art. 627, 9° "
                    + "(compétence territoriale contrat de travail) ; "
                    + "Loi du 3 juillet 1978 relative aux contrats de travail";

    private RefereTribunalTravailBeCalculator() {
    }

    /**
     * Calcule l'éligibilité au référé tribunal du travail BE pour les inputs
     * donnés. Fonction pure : aucun effet de bord, déterministe à inputs
     * égaux.
     *
     * @param request payload (les contrôles Bean Validation ont déjà été
     *                passés en amont par le service ; le calculateur valide
     *                en plus la cohérence inter-champs minimale et la
     *                non-nullité défensive).
     * @return résultat structuré (verdict + conditions non remplies + score
     *         + squelette de requête + base juridique + étape suivante).
     * @throws IllegalArgumentException si {@code request} est null ou si un
     *         champ requis (motifUrgence, motifUrgenceDescription,
     *         dateFaitGenerateur, mesureProvisoireDemandee) est manquant
     *         (défense en profondeur — Bean Validation amont).
     */
    public static RefereTribunalTravailBeResult compute(RefereTribunalTravailBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête référé tribunal du travail BE requise");
        }
        if (request.motifUrgence() == null) {
            throw new IllegalArgumentException("motifUrgence est requis");
        }
        if (request.motifUrgenceDescription() == null || request.motifUrgenceDescription().isBlank()) {
            throw new IllegalArgumentException("motifUrgenceDescription est requise");
        }
        if (request.dateFaitGenerateur() == null) {
            throw new IllegalArgumentException("dateFaitGenerateur est requise");
        }
        if (request.mesureProvisoireDemandee() == null || request.mesureProvisoireDemandee().isBlank()) {
            throw new IllegalArgumentException("mesureProvisoireDemandee est requise");
        }

        // Normalisations défensives — Bean Validation amont a déjà fait
        // l'essentiel mais le calculateur reste robuste si appelé directement.
        boolean preuveUrgenceJointe = Boolean.TRUE.equals(request.preuveUrgenceJointe());
        boolean perilEnDemeure = Boolean.TRUE.equals(request.perilEnDemeure());
        boolean competenceTerritorialeIdentifiee =
                Boolean.TRUE.equals(request.competenceTerritorialeIdentifiee());

        String descTrim = request.motifUrgenceDescription().trim();
        String mesureTrim = request.mesureProvisoireDemandee().trim();

        // ---- Évaluation des 5 conditions cumulatives ---------------------
        boolean urgenceQualifiable = request.motifUrgence() != RefereTribunalTravailBeMotifUrgence.AUTRE
                || descTrim.length() > DESCRIPTION_LONGUEUR_MIN;
        boolean mesurePrecise = mesureTrim.length() > MESURE_LONGUEUR_MIN;

        List<RefereTribunalTravailBeCondition> conditionsNonRemplies = new ArrayList<>();
        int score = 0;

        if (urgenceQualifiable) {
            score++;
        } else {
            conditionsNonRemplies.add(RefereTribunalTravailBeCondition.URGENCE_QUALIFIABLE);
        }
        if (preuveUrgenceJointe) {
            score++;
        } else {
            conditionsNonRemplies.add(RefereTribunalTravailBeCondition.PEUVE_URGENCE_JOINTE);
        }
        if (perilEnDemeure) {
            score++;
        } else {
            conditionsNonRemplies.add(RefereTribunalTravailBeCondition.PERIL_EN_DEMEURE);
        }
        if (mesurePrecise) {
            score++;
        } else {
            conditionsNonRemplies.add(RefereTribunalTravailBeCondition.MESURE_PROVISOIRE_PRECISE);
        }
        if (competenceTerritorialeIdentifiee) {
            score++;
        } else {
            conditionsNonRemplies.add(RefereTribunalTravailBeCondition.COMPETENCE_IDENTIFIEE);
        }

        // ---- Verdict + étape suivante ------------------------------------
        RefereTribunalTravailBeResult.Verdict verdict;
        RefereTribunalTravailBeResult.EtapeSuivante etape;
        if (score >= SEUIL_ELIGIBLE) {
            verdict = RefereTribunalTravailBeResult.Verdict.REFERE_ELIGIBLE;
            etape = RefereTribunalTravailBeResult.EtapeSuivante.DEPOSER_REQUETE;
        } else if (score >= SEUIL_INCERTAIN) {
            verdict = RefereTribunalTravailBeResult.Verdict.REFERE_INCERTAIN;
            etape = RefereTribunalTravailBeResult.EtapeSuivante.RENFORCER_DOSSIER;
        } else {
            verdict = RefereTribunalTravailBeResult.Verdict.REFERE_NON_ELIGIBLE;
            etape = RefereTribunalTravailBeResult.EtapeSuivante.ALTERNATIVE_PROCEDURE_FOND;
        }

        // ---- Squelette de requête ----------------------------------------
        // Généré pour ELIGIBLE et INCERTAIN uniquement (NON_ELIGIBLE → null,
        // procédure de fond requise, le squelette de référé serait trompeur).
        String squelette = (verdict == RefereTribunalTravailBeResult.Verdict.REFERE_NON_ELIGIBLE)
                ? null
                : construireSquelette(request.motifUrgence(), descTrim, mesureTrim,
                        request.dateFaitGenerateur());

        return new RefereTribunalTravailBeResult(
                request.motifUrgence(),
                descTrim,
                request.dateFaitGenerateur(),
                request.dateDemarcheAmiable(),
                preuveUrgenceJointe,
                mesureTrim,
                perilEnDemeure,
                competenceTerritorialeIdentifiee,
                verdict,
                List.copyOf(conditionsNonRemplies),
                score,
                squelette,
                BASE_JURIDIQUE_COMPLETE,
                etape);
    }

    // =========================================================================
    // Helpers — génération du squelette de requête
    // =========================================================================

    /**
     * Construit le squelette texte d'une requête en référé devant le président
     * du tribunal du travail BE. Sans en-tête formel destinataire/signature
     * (l'avocat les ajoute dans son traitement de texte) ; sections :
     * <ul>
     *   <li>en-tête type juridiction (à compléter par l'avocat) ;</li>
     *   <li>« EN FAIT » avec placeholder du motif d'urgence et de la date du
     *       fait générateur ;</li>
     *   <li>« EN DROIT » citant CJ art. 584 + Loi du 3 juillet 1978 ;</li>
     *   <li>dispositif demandant la mesure provisoire saisie.</li>
     * </ul>
     */
    private static String construireSquelette(RefereTribunalTravailBeMotifUrgence motif,
                                              String description,
                                              String mesureProvisoire,
                                              java.time.LocalDate dateFaitGenerateur) {
        String motifLibelle = libelleMotif(motif);
        StringBuilder sb = new StringBuilder();
        sb.append("REQUÊTE EN RÉFÉRÉ — Tribunal du travail de [À COMPLÉTER]\n\n");

        sb.append("EN FAIT\n");
        sb.append("───────\n\n");
        sb.append("Le requérant expose qu'à la date du ")
                .append(dateFaitGenerateur)
                .append(" est survenu le fait générateur suivant, constitutif d'une urgence ")
                .append("au sens de l'art. 584 du Code judiciaire :\n\n");
        sb.append("  ").append(motifLibelle).append(" — ").append(description).append("\n\n");
        sb.append("Ce fait cause au requérant un préjudice imminent et difficilement ")
                .append("réparable, qui justifie le recours à la procédure en référé devant ")
                .append("le président du tribunal du travail.\n\n");

        sb.append("EN DROIT\n");
        sb.append("────────\n\n");
        sb.append("L'article 584 du Code judiciaire dispose que le président du tribunal ")
                .append("statue au provisoire dans les cas dont il reconnaît l'urgence, en ")
                .append("toutes matières, sauf celles que la loi soustrait au pouvoir judiciaire.\n\n");
        sb.append("La compétence du président du tribunal du travail trouve son fondement ")
                .append("dans les articles 578 et suivants du Code judiciaire, qui attribuent ")
                .append("au tribunal du travail la connaissance des contestations relatives ")
                .append("au contrat de travail (Loi du 3 juillet 1978 relative aux contrats ")
                .append("de travail).\n\n");
        sb.append("L'urgence est caractérisée dès lors qu'une décision rapide est nécessaire ")
                .append("pour prévenir un préjudice irréversible ou pour mettre fin à une ")
                .append("situation manifestement illicite.\n\n");

        sb.append("PAR CES MOTIFS\n");
        sb.append("──────────────\n\n");
        sb.append("Le requérant prie qu'il plaise à Monsieur/Madame le Président du tribunal ")
                .append("du travail, statuant en référé :\n\n");
        sb.append("  - Ordonner, à titre provisoire, la mesure suivante : ")
                .append(mesureProvisoire).append(" ;\n");
        sb.append("  - Dire que l'ordonnance sera exécutoire par provision nonobstant tout ")
                .append("recours et sans caution ;\n");
        sb.append("  - Condamner la partie défenderesse aux dépens de l'instance, en ce ")
                .append("compris l'indemnité de procédure.\n");
        return sb.toString();
    }

    /** Libellé humain du motif d'urgence pour intégration dans le squelette. */
    private static String libelleMotif(RefereTribunalTravailBeMotifUrgence motif) {
        return switch (motif) {
            case HARCELEMENT -> "Harcèlement persistant au travail";
            case SALAIRE_IMPAYE -> "Non-paiement de salaire ou d'indemnité due";
            case MODIFICATION_UNILATERALE -> "Modification unilatérale d'un élément essentiel du contrat";
            case AUTRE -> "Motif d'urgence circonstancié";
        };
    }
}
