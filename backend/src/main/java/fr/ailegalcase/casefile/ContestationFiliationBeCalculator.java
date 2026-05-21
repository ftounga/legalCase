package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SF-217-18 : moteur décisionnel BE pour l'analyse de recevabilité d'une action
 * en contestation de filiation paternelle (paternité présumée du mari ou
 * paternité reconnue volontairement).
 *
 * <p>Arbre décisionnel construit à partir des sources belges (CC art. 318
 * nouveau — à vérifier, loi du 01/07/2006 refonte de la filiation), avec :</p>
 * <ul>
 *   <li>vérification de la <b>qualité à agir</b> reconnue par le code (limitée
 *       à l'enfant, ses père et mère, le père présumé ou reconnaissant, le
 *       tiers prétendant être le père biologique, et le Ministère public —
 *       à vérifier) ;</li>
 *   <li>calcul du <b>délai d'action</b> de 1 an à compter de la connaissance
 *       du fait remettant en cause la filiation (V1 délai commun aux qualités
 *       principales — à vérifier — règles distinctes possibles par qualité) ;</li>
 *   <li>vérification de la <b>possession d'état conforme</b> : 5 ans depuis
 *       la naissance ou la reconnaissance bloquent la contestation (CC art. 318
 *       § 2 — à vérifier) ;</li>
 *   <li>recommandation de l'expertise ADN si pertinent.</li>
 * </ul>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. Outil bâti depuis les sources belges,
 * non réutilisable côté FR (le Calculator FR F-FA-18 traite la contestation de
 * paternité française — articles, délais et règle de possession d'état
 * distincts). Référence mémoire : {@code feedback_belgique_never_forget}.</p>
 *
 * <p><b>Validation juridique requise</b> : les articles cités, le délai de 1 an,
 * la règle de possession d'état conforme de 5 ans, la qualité à agir restrictive
 * et la voie procédurale (saisine du Tribunal de la famille, CJ art. 572bis —
 * à vérifier) sont à <b>valider par un avocat belge avant mise en production</b>.
 * Articles tagués (à vérifier) cohérents avec l'audit F-191.</p>
 */
public final class ContestationFiliationBeCalculator {

    /** Verdict d'analyse de recevabilité de l'action en contestation. */
    public enum ContestationFiliationBeVerdict {
        ACTION_RECEVABLE,
        ACTION_RECEVABLE_DELAI_CRITIQUE,
        IRRECEVABLE_DELAI_DEPASSE,
        IRRECEVABLE_QUALITE_A_AGIR,
        IRRECEVABLE_POSSESSION_ETAT,
        QUALIFICATION_INCOMPLETE
    }

    /** Nature de la filiation contestée. */
    public enum NatureActionFiliationBe {
        PATERNITE_PRESUMEE_MARI,
        PATERNITE_RECONNUE_VOLONTAIRE,
        /** Hors scope V1 — rejeté en amont (400). */
        MATERNITE
    }

    /** Qualité du demandeur à l'action. */
    public enum QualiteDemandeurFiliationBe {
        ENFANT_MAJEUR,
        ENFANT_MINEUR_REPRESENTE,
        MERE,
        PERE_PRESUME_OU_RECONNAISSANT,
        TIERS_PRETENDANT_PERE_BIOLOGIQUE,
        MINISTERE_PUBLIC
    }

    /** Voie procédurale recommandée. */
    public enum VoieProceduraleFiliationBe {
        REQUETE_TRIBUNAL_FAMILLE,
        AUCUNE_ACTION_RECEVABLE
    }

    /** Statut du délai d'action à la date du calcul. */
    public enum DelaiStatutBe {
        OK,
        CRITIQUE,
        DEPASSE
    }

    /** Code structuré d'un motif d'irrecevabilité. */
    public enum MotifIrrecevabiliteFiliationBeCode {
        DELAI_FORCLUSION,
        QUALITE_NON_RECONNUE,
        POSSESSION_ETAT_CONFORME_5_ANS,
        MATERNITE_HORS_SCOPE_V1
    }

    /** Motif d'irrecevabilité — exposé dans la réponse API. */
    public record MotifIrrecevabiliteFiliationBe(
            MotifIrrecevabiliteFiliationBeCode code,
            String libelle,
            String fondement
    ) {}

    // ---------------------------------------------------------------
    // Constantes documentées (à vérifier — validation avocat belge requise)
    // ---------------------------------------------------------------

    /** Délai d'action par défaut, en années, à compter de la connaissance du fait
     *  remettant en cause la filiation (CC art. 318 nouveau — à vérifier).
     *  V1 délai commun aux qualités principales — à raffiner ultérieurement. */
    private static final int DELAI_ACTION_ANNEES = 1;

    /** Durée de possession d'état conforme bloquant la contestation, en années
     *  (CC art. 318 § 2 — à vérifier). */
    private static final int POSSESSION_ETAT_BLOQUANTE_ANNEES = 5;

    /** Seuil de jours restants en deçà duquel le délai est jugé critique. */
    private static final int SEUIL_DELAI_CRITIQUE_JOURS = 30;

    /** Longueur maximale du commentaire libre. */
    private static final int COMMENTAIRE_MAX = 1000;

    /** Borne supérieure de durée de possession d'état (années entières). */
    private static final int DUREE_POSSESSION_ETAT_MAX = 100;

    /**
     * Qualités reconnues par nature de l'action (CC art. 318 / 330 — à vérifier).
     * Liste figée dans le Calculator (source de vérité). Une évolution doctrinale
     * = nouvelle version de l'outil.
     */
    private static final Map<NatureActionFiliationBe, Set<QualiteDemandeurFiliationBe>>
            QUALITES_RECONNUES = new EnumMap<>(NatureActionFiliationBe.class);

    static {
        Set<QualiteDemandeurFiliationBe> qualitesLarges = EnumSet.of(
                QualiteDemandeurFiliationBe.ENFANT_MAJEUR,
                QualiteDemandeurFiliationBe.ENFANT_MINEUR_REPRESENTE,
                QualiteDemandeurFiliationBe.MERE,
                QualiteDemandeurFiliationBe.PERE_PRESUME_OU_RECONNAISSANT,
                QualiteDemandeurFiliationBe.TIERS_PRETENDANT_PERE_BIOLOGIQUE,
                QualiteDemandeurFiliationBe.MINISTERE_PUBLIC
        );
        QUALITES_RECONNUES.put(NatureActionFiliationBe.PATERNITE_PRESUMEE_MARI, qualitesLarges);
        QUALITES_RECONNUES.put(NatureActionFiliationBe.PATERNITE_RECONNUE_VOLONTAIRE, qualitesLarges);
        // MATERNITE : hors scope V1 — pas de qualité reconnue (rejeté en amont).
        QUALITES_RECONNUES.put(NatureActionFiliationBe.MATERNITE, EnumSet.noneOf(QualiteDemandeurFiliationBe.class));
    }

    private ContestationFiliationBeCalculator() {}

    /**
     * Applique l'arbre décisionnel BE de la recevabilité d'une contestation de
     * filiation sur les éléments saisis.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @param today   date de référence pour le calcul des délais (injectable pour les tests)
     * @return résultat structuré (verdict, délai, motifs d'irrecevabilité, voie procédurale, etc.)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static ContestationFiliationBeResult compute(
            ContestationFiliationBeInput input, String country, LocalDate today) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — contestation de filiation");
        }
        if (today == null) {
            throw new IllegalArgumentException("Date de référence requise");
        }
        validateInputs(input, today);

        // Règle 1 : maternité hors scope V1 → rejet (déjà validé en amont mais
        // sécurité ceinture / bretelles ici).
        if (input.natureActionFiliation() == NatureActionFiliationBe.MATERNITE) {
            throw new IllegalArgumentException(
                    "Contestation de maternité hors scope V1 — outil dédié à venir");
        }

        List<MotifIrrecevabiliteFiliationBe> motifs = new ArrayList<>();

        // Règle 2 : possession d'état conforme ≥ 5 ans bloque l'action
        // (CC art. 318 § 2 — à vérifier).
        boolean possessionBloquante = input.possessionEtatConforme()
                && input.dureePossessionEtatAnnees() >= POSSESSION_ETAT_BLOQUANTE_ANNEES;
        if (possessionBloquante) {
            motifs.add(new MotifIrrecevabiliteFiliationBe(
                    MotifIrrecevabiliteFiliationBeCode.POSSESSION_ETAT_CONFORME_5_ANS,
                    "Possession d'état conforme à la filiation contestée depuis "
                            + input.dureePossessionEtatAnnees()
                            + " ans — la contestation est irrecevable (5 ans depuis la naissance "
                            + "ou la reconnaissance — à vérifier).",
                    "CC art. 318 § 2 nouveau (à vérifier)"
            ));
            return buildIrrecevableResult(input, ContestationFiliationBeVerdict.IRRECEVABLE_POSSESSION_ETAT,
                    motifs, countryNormalized,
                    "Possession d'état conforme de " + input.dureePossessionEtatAnnees()
                            + " ans (≥ 5) : la contestation est irrecevable (CC art. 318 § 2 — à vérifier).");
        }

        // Règle 3 : qualité à agir non reconnue → IRRECEVABLE_QUALITE_A_AGIR.
        Set<QualiteDemandeurFiliationBe> reconnues = QUALITES_RECONNUES.get(input.natureActionFiliation());
        if (reconnues == null || !reconnues.contains(input.qualiteDemandeur())) {
            motifs.add(new MotifIrrecevabiliteFiliationBe(
                    MotifIrrecevabiliteFiliationBeCode.QUALITE_NON_RECONNUE,
                    "La qualité « " + libelleQualite(input.qualiteDemandeur())
                            + " » n'est pas reconnue par CC art. 318 (à vérifier) pour cette nature "
                            + "de contestation.",
                    "CC art. 318 nouveau (à vérifier) — qualités limitatives"
            ));
            return buildIrrecevableResult(input, ContestationFiliationBeVerdict.IRRECEVABLE_QUALITE_A_AGIR,
                    motifs, countryNormalized,
                    "Qualité à agir non reconnue par CC art. 318 (à vérifier) — l'action est irrecevable.");
        }

        // Règle 4 : calcul du délai 1 an à compter de la connaissance.
        LocalDate dateLimite = input.dateConnaissanceFaitContestation().plusYears(DELAI_ACTION_ANNEES);
        long joursRestantsLong = ChronoUnit.DAYS.between(today, dateLimite);
        int joursRestants = (int) joursRestantsLong;
        DelaiStatutBe delaiStatut = statutDelai(joursRestants);

        // Règle 5 : délai dépassé → IRRECEVABLE_DELAI_DEPASSE.
        if (delaiStatut == DelaiStatutBe.DEPASSE) {
            motifs.add(new MotifIrrecevabiliteFiliationBe(
                    MotifIrrecevabiliteFiliationBeCode.DELAI_FORCLUSION,
                    "Délai d'action d'un an dépassé à compter de la connaissance du fait "
                            + "remettant en cause la filiation — date limite "
                            + format(dateLimite) + ", dépassement de "
                            + Math.abs(joursRestants) + " jours.",
                    "CC art. 318 nouveau (à vérifier) — délai d'1 an"
            ));
            return new ContestationFiliationBeResult(
                    ContestationFiliationBeVerdict.IRRECEVABLE_DELAI_DEPASSE,
                    dateLimite, joursRestants, delaiStatut,
                    VoieProceduraleFiliationBe.AUCUNE_ACTION_RECEVABLE,
                    motifs, List.of(
                            "Vérifier l'existence d'une cause de relevé de forclusion (à vérifier).",
                            "Documenter précisément la date de connaissance du fait dans le dossier."
                    ),
                    basesJuridiques(),
                    List.of("Délai d'action dépassé de " + Math.abs(joursRestants) + " jours — l'action est forclose."),
                    countryNormalized
            );
        }

        // Règle 6 : délai critique (≤ 30 j).
        ContestationFiliationBeVerdict verdict =
                delaiStatut == DelaiStatutBe.CRITIQUE
                        ? ContestationFiliationBeVerdict.ACTION_RECEVABLE_DELAI_CRITIQUE
                        : ContestationFiliationBeVerdict.ACTION_RECEVABLE;

        List<String> actions = construireActionsRecevable(input);
        List<String> messages = construireMessagesRecevable(input, verdict, dateLimite,
                joursRestants, delaiStatut);

        return new ContestationFiliationBeResult(
                verdict,
                dateLimite,
                joursRestants,
                delaiStatut,
                VoieProceduraleFiliationBe.REQUETE_TRIBUNAL_FAMILLE,
                motifs, // vide
                actions,
                basesJuridiques(),
                messages,
                countryNormalized
        );
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(ContestationFiliationBeInput in, LocalDate today) {
        if (in.natureActionFiliation() == null) {
            throw new IllegalArgumentException("La nature de l'action en filiation est requise");
        }
        if (in.qualiteDemandeur() == null) {
            throw new IllegalArgumentException("La qualité du demandeur est requise");
        }
        if (in.dateNaissanceEnfant() == null) {
            throw new IllegalArgumentException("La date de naissance de l'enfant est requise");
        }
        if (in.dateNaissanceEnfant().isAfter(today)) {
            throw new IllegalArgumentException(
                    "La date de naissance de l'enfant ne peut pas être dans le futur");
        }
        if (in.dateConnaissanceFaitContestation() == null) {
            throw new IllegalArgumentException(
                    "La date de connaissance du fait remettant en cause la filiation est requise");
        }
        if (in.dateConnaissanceFaitContestation().isAfter(today)) {
            throw new IllegalArgumentException(
                    "La date de connaissance ne peut pas être dans le futur");
        }
        if (in.dateConnaissanceFaitContestation().isBefore(in.dateNaissanceEnfant())) {
            throw new IllegalArgumentException(
                    "La date de connaissance ne peut être antérieure à la date de naissance de l'enfant");
        }
        if (in.dureePossessionEtatAnnees() < 0
                || in.dureePossessionEtatAnnees() > DUREE_POSSESSION_ETAT_MAX) {
            throw new IllegalArgumentException(
                    "La durée de possession d'état doit être comprise entre 0 et "
                            + DUREE_POSSESSION_ETAT_MAX + " ans");
        }
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    // ---------------------------------------------------------------
    // Helpers de résultat
    // ---------------------------------------------------------------

    private static DelaiStatutBe statutDelai(int joursRestants) {
        if (joursRestants < 0) {
            return DelaiStatutBe.DEPASSE;
        }
        if (joursRestants <= SEUIL_DELAI_CRITIQUE_JOURS) {
            return DelaiStatutBe.CRITIQUE;
        }
        return DelaiStatutBe.OK;
    }

    private static ContestationFiliationBeResult buildIrrecevableResult(
            ContestationFiliationBeInput in,
            ContestationFiliationBeVerdict verdict,
            List<MotifIrrecevabiliteFiliationBe> motifs,
            String country,
            String messageEnTete) {
        return new ContestationFiliationBeResult(
                verdict,
                null,
                null,
                null,
                VoieProceduraleFiliationBe.AUCUNE_ACTION_RECEVABLE,
                motifs,
                List.of(
                        "Documenter les motifs d'irrecevabilité dans le dossier.",
                        "Évaluer d'autres voies (action en recherche de paternité, "
                                + "reconnaissance volontaire, le cas échéant — outils dédiés F-223)."
                ),
                basesJuridiques(),
                List.of(messageEnTete),
                country
        );
    }

    private static List<String> basesJuridiques() {
        return List.of(
                "CC art. 318 nouveau (à vérifier) — contestation de la paternité du mari : "
                        + "qualité à agir et délais",
                "CC art. 318 § 2 (à vérifier) — irrecevabilité de la contestation en cas de "
                        + "possession d'état conforme de 5 ans depuis la naissance",
                "Loi du 01/07/2006 (à vérifier) — refonte du droit de la filiation",
                "CJ art. 572bis (à vérifier) — compétence du Tribunal de la famille en matière "
                        + "de filiation"
        );
    }

    private static List<String> construireActionsRecevable(ContestationFiliationBeInput in) {
        List<String> actions = new ArrayList<>();
        actions.add("Saisir le Tribunal de la famille du lieu de résidence de l'enfant par "
                + "requête contradictoire (CJ art. 572bis — à vérifier).");
        if (in.expertiseAdnDisponible()) {
            actions.add("Produire le test ADN disponible : pièce centrale de la contestation.");
        } else if (in.demandeExpertiseAdnEnvisagee()) {
            actions.add("Solliciter une expertise ADN judiciaire dans la requête.");
        } else {
            actions.add("Évaluer l'opportunité d'une expertise ADN (judiciaire ou amiable) — "
                    + "pièce centrale de la contestation.");
        }
        actions.add("Aviser le Ministère public (présence obligatoire en filiation — à vérifier).");
        return actions;
    }

    private static List<String> construireMessagesRecevable(
            ContestationFiliationBeInput in,
            ContestationFiliationBeVerdict verdict,
            LocalDate dateLimite,
            int joursRestants,
            DelaiStatutBe delaiStatut) {
        List<String> messages = new ArrayList<>();
        String qualiteLabel = libelleQualite(in.qualiteDemandeur());

        switch (verdict) {
            case ACTION_RECEVABLE -> messages.add(
                    qualiteLabel + " contestant la "
                            + libelleNature(in.natureActionFiliation())
                            + " : action recevable dans le délai d'un an à compter de la connaissance "
                            + "du fait — date limite " + format(dateLimite) + ", "
                            + joursRestants + " jours restants.");
            case ACTION_RECEVABLE_DELAI_CRITIQUE -> messages.add(
                    "Délai d'action restant inférieur à " + SEUIL_DELAI_CRITIQUE_JOURS
                            + " jours (" + joursRestants + " jours) — date limite "
                            + format(dateLimite) + " — introduire la requête sans délai.");
            default -> {
                // Pas applicable pour les verdicts irrecevables, traités en amont.
            }
        }

        if (in.possessionEtatConforme()) {
            messages.add("Possession d'état conforme de "
                    + in.dureePossessionEtatAnnees()
                    + " ans (< 5) : la contestation n'est pas bloquée par CC art. 318 § 2 (à vérifier), "
                    + "mais cet élément peut être discuté au fond.");
        } else {
            messages.add("Pas de possession d'état conforme : la contestation n'est pas bloquée "
                    + "par CC art. 318 § 2.");
        }

        if (in.expertiseAdnDisponible()) {
            messages.add("Test ADN disponible : pièce centrale de la contestation, à produire.");
        } else if (in.demandeExpertiseAdnEnvisagee()) {
            messages.add("Demande d'expertise ADN envisagée : à formaliser dans la requête.");
        }

        return messages;
    }

    private static String libelleNature(NatureActionFiliationBe nature) {
        return switch (nature) {
            case PATERNITE_PRESUMEE_MARI -> "paternité présumée du mari";
            case PATERNITE_RECONNUE_VOLONTAIRE -> "paternité reconnue volontairement";
            case MATERNITE -> "maternité";
        };
    }

    private static String libelleQualite(QualiteDemandeurFiliationBe qualite) {
        return switch (qualite) {
            case ENFANT_MAJEUR -> "Enfant majeur";
            case ENFANT_MINEUR_REPRESENTE -> "Enfant mineur représenté";
            case MERE -> "Mère";
            case PERE_PRESUME_OU_RECONNAISSANT -> "Père présumé ou reconnaissant";
            case TIERS_PRETENDANT_PERE_BIOLOGIQUE -> "Tiers prétendant être le père biologique";
            case MINISTERE_PUBLIC -> "Ministère public";
        };
    }

    private static String format(LocalDate d) {
        if (d == null) {
            return "—";
        }
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
