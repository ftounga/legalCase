package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-11 : calculateur statique de recevabilité d'un retrait d'autorité
 * parentale (art. 378-381 Cciv + loi n°2022-140 du 7 février 2022 LMVSS +
 * art. 343-1 al. 2 Cciv pour l'admissibilité adoption + Cass. 1ère civ.,
 * 26/10/2011 sur les conditions du retrait civil).
 *
 * <p>Arbre décisionnel :</p>
 * <ol>
 *   <li>Si l'enfant est majeur (ageEnfant &gt;= 18) →
 *       {@link VerdictRetraitApEnum#IRRECEVABLE_ENFANT_MAJEUR}
 *       (l'autorité parentale s'éteint à 18 ans, art. 371-1 Cciv).</li>
 *   <li>Si motif {@code VIOLENCES_LMVSS_2022} OU
 *       {@code violencesConjugalesDetectees == true} → suspension automatique
 *       de l'AP (loi 2022-140) puis saisine accélérée pour retrait.</li>
 *   <li>Si motif {@code CONDAMNATION_PENALE} ET
 *       {@code condamnationPenaleDetectee == true} → retrait de plein droit
 *       prononcé accessoirement par la juridiction pénale (art. 378 al. 1).</li>
 *   <li>Si motif {@code DANGER_CARACTERISE_VIOLENCES} OU
 *       {@code COMPORTEMENT_GRAVEMENT_COMPROMETTANT} → retrait civil JAF /
 *       tribunal judiciaire (art. 378-1 al. 1).</li>
 *   <li>Si motif {@code DESINTERET_GRAVE} → retrait civil JAF, mais
 *       irrecevable si la durée du désintérêt (≥ 2 ans, art. 378-1 al. 2)
 *       n'est pas étayée (pas de décisions judiciaires précédentes ni de
 *       danger caractérisé corroborant l'abandon).</li>
 * </ol>
 *
 * <p>Conséquences juridiques calculées :</p>
 * <ul>
 *   <li>Retrait total → enfant peut être déclaré adoptable (art. 343-1 al. 2
 *       Cciv) → {@code admissibiliteAdoption=true}.</li>
 *   <li>Retrait → délégation à un tiers ou ouverture d'une tutelle.</li>
 *   <li>Mention de la décision en marge de l'acte d'état civil.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, l'équivalent est la
 * déchéance de l'autorité parentale (art. 33 et s. Loi du 8/4/1965 relative
 * à la protection de la jeunesse) — outil distinct hors périmètre F-216.</p>
 */
public final class RetraitAutoriteParentaleCalculator {

    static final String BASE_JURIDIQUE =
            "art. 378-381 Cciv (retrait total et partiel)"
                    + " + art. 378-1 Cciv (retrait civil JAF)"
                    + " + loi n°2022-140 du 7 février 2022 (LMVSS — violences conjugales)"
                    + " + art. 343-1 al. 2 Cciv (admissibilité adoption)"
                    + " + art. 371-1 Cciv (durée AP)"
                    + " + Cass. 1ère civ., 26/10/2011 (conditions du retrait civil)";

    /** Majorité civile — au-delà, l'AP s'est éteinte, le retrait est sans objet. */
    static final int AGE_MAJORITE = 18;

    private RetraitAutoriteParentaleCalculator() {}

    /**
     * Calcule la recevabilité et la voie procédurale du retrait d'AP.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static RetraitAutoriteParentaleResult compute(RetraitAutoriteParentaleRequest req,
                                                         String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-RETRAIT-AP applicable uniquement en France (art. 378 Cciv).");
        }

        List<String> etapes = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();
        List<String> consequences = new ArrayList<>();

        int age = req.ageEnfant() == null ? 0 : req.ageEnfant();
        boolean condamnationPenale = Boolean.TRUE.equals(req.condamnationPenaleDetectee());
        boolean danger = Boolean.TRUE.equals(req.dangerCaracterise());
        boolean violencesConjugales = Boolean.TRUE.equals(req.violencesConjugalesDetectees());
        boolean decisionsPrec = Boolean.TRUE.equals(req.decisionsJudiciairesPrecedentes());
        TypeRetraitApEnum typeRetrait = req.typeRetrait();
        MotifRetraitApEnum motif = req.motifRetrait();

        // ── 1. Cas d'irrecevabilité : enfant majeur ──────────────────────────
        if (age >= AGE_MAJORITE) {
            messages.add("L'autorité parentale s'éteint à la majorité (art. 371-1 Cciv). "
                    + "Une demande de retrait est sans objet pour un enfant majeur.");
            return new RetraitAutoriteParentaleResult(
                    VerdictRetraitApEnum.IRRECEVABLE_ENFANT_MAJEUR,
                    VoieProceduraleRetraitApEnum.SANS_OBJET,
                    false,
                    consequences,
                    etapes,
                    0,
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // ── 2. Voie LMVSS 2022 — violences conjugales en présence de l'enfant ─
        if (motif == MotifRetraitApEnum.VIOLENCES_LMVSS_2022 || violencesConjugales) {
            VerdictRetraitApEnum verdict =
                    VerdictRetraitApEnum.SUSPENSION_ACCELEREE_LMVSS_2022;
            VoieProceduraleRetraitApEnum voie =
                    VoieProceduraleRetraitApEnum.LMVSS_2022_SUSPENSION_AUTOMATIQUE;
            int dureeJours = 90;
            alertes.add("Loi n°2022-140 du 7 février 2022 (LMVSS) : suspension de plein droit "
                    + "de l'exercice de l'autorité parentale dès la mise en examen pour crime "
                    + "ou violence sur le conjoint en présence de l'enfant. Saisine accélérée "
                    + "du juge pour statuer sur le retrait définitif.");
            etapes.add("Vérifier la qualification pénale en cours (mise en examen / "
                    + "condamnation pour violences conjugales graves art. 222-7 et s. CP, "
                    + "féminicide en présence de l'enfant).");
            etapes.add("Demander au juge pénal le maintien de la suspension de l'AP (loi 2022) "
                    + "et la saisine accélérée du JAF pour statuer sur le retrait total.");
            etapes.add("Saisir le JAF / tribunal judiciaire d'une requête aux fins de retrait "
                    + "total ou partiel (art. 378-1 Cciv) — procédure accélérée en parallèle.");
            etapes.add("Solliciter l'audition de l'enfant doué de discernement (art. 388-1 Cciv) "
                    + "et la mise en place immédiate d'une délégation tiers ou tutelle.");
            etapes.add("Inscrire la décision en marge de l'acte d'état civil de l'enfant.");
            messages.add("La loi LMVSS 2022 a institutionnalisé une voie accélérée : la "
                    + "suspension intervient sans débat contradictoire dès la mise en examen, "
                    + "puis le juge statue dans un délai resserré sur le retrait définitif.");
            consequences.add("Suspension immédiate de l'exercice de l'AP et des droits de "
                    + "visite + hébergement (loi 2022-140).");
            consequences.add("Si retrait total prononcé : l'enfant peut être déclaré adoptable "
                    + "(art. 343-1 al. 2 Cciv) — voir outil F-FA-ADOPTION-INTRA pour la suite.");
            consequences.add("Délégation à un tiers (art. 376-1 al. 2 Cciv) ou ouverture d'une "
                    + "tutelle (art. 390 et s. Cciv) selon la situation familiale.");

            boolean admissibilite = typeRetrait == TypeRetraitApEnum.TOTAL;
            if (decisionsPrec) {
                messages.add("Décisions judiciaires antérieures concernant l'enfant signalées : "
                        + "joindre l'historique procédural complet pour éclairer la juridiction.");
            }
            return new RetraitAutoriteParentaleResult(verdict, voie, admissibilite,
                    consequences, etapes, dureeJours, BASE_JURIDIQUE, messages, alertes);
        }

        // ── 3. Retrait accessoire pénal — condamnation crime / délit sur enfant ─
        if (motif == MotifRetraitApEnum.CONDAMNATION_PENALE) {
            if (!condamnationPenale) {
                alertes.add("Motif CONDAMNATION_PENALE choisi mais aucune condamnation "
                        + "pénale n'est documentée dans les pièces. Le retrait accessoire "
                        + "exige une décision pénale définitive ou un jugement de condamnation "
                        + "(art. 378 al. 1 Cciv).");
                return new RetraitAutoriteParentaleResult(
                        VerdictRetraitApEnum.IRRECEVABLE_MOTIF_NON_CARACTERISE,
                        VoieProceduraleRetraitApEnum.SANS_OBJET,
                        false,
                        consequences,
                        etapes,
                        0,
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
            VerdictRetraitApEnum verdict = VerdictRetraitApEnum.RETRAIT_PLEIN_DROIT;
            VoieProceduraleRetraitApEnum voie =
                    VoieProceduraleRetraitApEnum.JURIDICTION_PENALE_ACCESSOIRE;
            int dureeJours = 60;
            etapes.add("Vérifier que la condamnation pénale porte sur un crime ou délit commis "
                    + "sur la personne de l'enfant, par l'enfant ou avec sa complicité "
                    + "(art. 378 al. 1 Cciv).");
            etapes.add("Demander à la juridiction pénale de prononcer le retrait total ou "
                    + "partiel de l'AP à titre de peine complémentaire — la demande peut être "
                    + "formée par le ministère public ou la partie civile.");
            etapes.add("Joindre la pièce de condamnation pénale définitive (ou jugement avec "
                    + "exécution provisoire si l'affaire est en appel).");
            etapes.add("Solliciter à titre conservatoire la mise en place d'une délégation "
                    + "tiers ou tutelle pour assurer la prise en charge immédiate de l'enfant.");
            etapes.add("Inscrire la décision pénale prononçant le retrait en marge de l'acte "
                    + "d'état civil de l'enfant.");
            messages.add("Le retrait accessoire au pénal est de plein droit : la juridiction "
                    + "pénale est compétente (art. 378 al. 1 Cciv) — pas de saisine séparée "
                    + "du JAF nécessaire pour le prononcé du retrait lui-même.");
            consequences.add("Retrait total : enfant pouvant être déclaré adoptable "
                    + "(art. 343-1 al. 2 Cciv) — admissibilité à l'adoption intra-familiale.");
            consequences.add("Délégation à un tiers (art. 376-1 al. 2) ou ouverture d'une "
                    + "tutelle (art. 390 et s. Cciv) selon les liens familiaux disponibles.");
            consequences.add("Mention en marge de l'acte d'état civil + perte de tous les droits "
                    + "et devoirs de l'AP (art. 379 Cciv).");

            boolean admissibilite = typeRetrait == TypeRetraitApEnum.TOTAL;
            if (decisionsPrec) {
                messages.add("Historique procédural à joindre pour la juridiction pénale.");
            }
            return new RetraitAutoriteParentaleResult(verdict, voie, admissibilite,
                    consequences, etapes, dureeJours, BASE_JURIDIQUE, messages, alertes);
        }

        // ── 4. Désintérêt grave — bascule irrecevabilité si non documenté ─────
        if (motif == MotifRetraitApEnum.DESINTERET_GRAVE) {
            // L'art. 378-1 al. 2 exige un désintérêt manifeste de plus de 2 ans.
            // À défaut de décisions judiciaires antérieures ou d'indice corroborant,
            // la voie est irrecevable.
            if (!decisionsPrec && !danger) {
                alertes.add("Motif DESINTERET_GRAVE invoqué mais le désintérêt de plus de "
                        + "2 ans (art. 378-1 al. 2 Cciv) n'est pas factuellement étayé. "
                        + "Joindre attestations, courriers restés sans réponse, défaut de "
                        + "pension alimentaire, absence de visites, constats d'huissier.");
                etapes.add("Documenter le désintérêt manifeste sur 2 ans minimum : preuves "
                        + "factuelles concrètes (attestations, défaut de contribution à "
                        + "l'entretien, absence totale de relations).");
                etapes.add("Si le désintérêt n'atteint pas 2 ans, envisager d'abord la voie "
                        + "DÉLÉGATION judiciaire (art. 376-1 al. 2 Cciv — désintérêt > 1 an) "
                        + "via l'outil F-FA-XX-delegation-ap, moins exigeante.");
                messages.add("Le retrait pour désintérêt grave est subsidiaire : la "
                        + "jurisprudence (Cass. 1ère civ., 26/10/2011) exige une "
                        + "documentation concrète de l'abandon total et durable.");
                return new RetraitAutoriteParentaleResult(
                        VerdictRetraitApEnum.IRRECEVABLE_MOTIF_NON_CARACTERISE,
                        VoieProceduraleRetraitApEnum.SANS_OBJET,
                        false,
                        consequences,
                        etapes,
                        0,
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
        }

        // ── 5. Retrait civil JAF — danger / désintérêt étayé / comportement ───
        VerdictRetraitApEnum verdict = VerdictRetraitApEnum.RETRAIT_CIVIL_JAF;
        VoieProceduraleRetraitApEnum voie =
                VoieProceduraleRetraitApEnum.JAF_TRIBUNAL_JUDICIAIRE;
        int dureeJours = 240;

        etapes.add("Saisir le JAF / tribunal judiciaire par requête fondée sur l'art. 378-1 "
                + "Cciv (retrait total ou partiel selon les attributs visés).");
        etapes.add("Documenter le motif retenu par des pièces probantes : mauvais traitements "
                + "(certificats médicaux, signalements, rapports ASE), comportement "
                + "gravement compromettant (consommation alcool/stupéfiants, casier "
                + "judiciaire, témoignages), désintérêt manifeste > 2 ans.");
        etapes.add("La demande peut être formée par : l'autre parent, un membre de la famille, "
                + "le ministère public, ou le tuteur (si AP déjà déléguée).");
        etapes.add("Audience contradictoire — convocation du parent visé, audition de l'enfant "
                + "doué de discernement (art. 388-1 Cciv).");
        etapes.add("Solliciter en parallèle une mesure d'urgence (assistance éducative, "
                + "délégation provisoire) si l'enfant est immédiatement en danger.");
        etapes.add("Inscrire la décision en marge de l'acte d'état civil + organiser la prise "
                + "en charge post-retrait (délégation tiers art. 376-1, tutelle art. 390+).");

        messages.add("Le retrait civil JAF couvre les hypothèses art. 378-1 Cciv : mauvais "
                + "traitements, abus d'autorité, négligence, comportement compromettant, "
                + "désintérêt manifeste > 2 ans. La procédure est contradictoire et exige une "
                + "documentation factuelle solide (Cass. 1ère civ., 26/10/2011).");

        if (danger) {
            alertes.add("Danger immédiat caractérisé : saisir IMPÉRATIVEMENT en parallèle le "
                    + "Procureur de la République pour déclencher une assistance éducative "
                    + "(art. 375 et s. Cciv) et solliciter des mesures conservatoires pendant "
                    + "l'instance de retrait.");
            voie = VoieProceduraleRetraitApEnum.PROCUREUR_REPUBLIQUE_ASSISTANCE_EDUCATIVE;
        }

        if (typeRetrait == TypeRetraitApEnum.TOTAL) {
            consequences.add("Retrait TOTAL : perte de tous les droits et devoirs liés à l'AP "
                    + "(art. 379 Cciv). L'enfant pourra être déclaré adoptable au sens de "
                    + "l'art. 343-1 al. 2 Cciv (admissibilité adoption intra-familiale).");
        } else if (typeRetrait == TypeRetraitApEnum.PARTIEL_EXERCICE) {
            consequences.add("Retrait PARTIEL EXERCICE : la titularité de l'AP est conservée "
                    + "mais l'exercice (décisions au quotidien, hébergement, scolarité) est "
                    + "transféré à l'autre parent ou à un tiers délégataire.");
        } else {
            consequences.add("Retrait PARTIEL ATTRIBUTS : limitation à certains attributs "
                    + "(consentement adoption, autorisations administratives, etc.) — "
                    + "art. 379-1 Cciv. L'AP demeure pour le reste.");
        }
        consequences.add("Délégation à un tiers art. 376-1 al. 2 Cciv (voir outil "
                + "F-FA-XX-delegation-ap) ou ouverture d'une tutelle art. 390 et s. Cciv "
                + "selon la situation familiale.");
        consequences.add("Mention de la décision en marge de l'acte d'état civil de l'enfant "
                + "(art. 380 Cciv).");

        if (decisionsPrec) {
            messages.add("Décisions judiciaires antérieures concernant l'enfant signalées : "
                    + "joindre l'historique procédural complet (jugements JAF, décisions JE, "
                    + "mesures d'assistance éducative passées) — éléments majeurs pour le juge.");
        }

        boolean admissibilite = typeRetrait == TypeRetraitApEnum.TOTAL;
        return new RetraitAutoriteParentaleResult(verdict, voie, admissibilite,
                consequences, etapes, dureeJours, BASE_JURIDIQUE, messages, alertes);
    }
}
