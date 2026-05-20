package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-217-12 : moteur décisionnel BE pour le choix de l'option successorale de
 * l'héritier — acceptation pure et simple, acceptation sous bénéfice d'inventaire,
 * ou renonciation.
 *
 * <p>Arbre décisionnel construit à partir des sources belges (CC art. 774+ nouveau —
 * à vérifier), avec :</p>
 * <ul>
 *   <li>calcul du <b>délai d'option</b> de 4 mois (à vérifier) à compter de
 *       l'ouverture de la succession, raccourci sur mise en demeure d'un créancier
 *       (V1 : 2 mois post mise en demeure — à vérifier) ;</li>
 *   <li>détection des <b>actes d'héritier valant acceptation tacite</b> (CC art. 779
 *       ancien / équivalent réformé — à vérifier) : encaissement de créance,
 *       paiement de dette, vente d'un bien de la succession, prise de possession
 *       des biens. Les actes conservatoires neutres et la demande d'inventaire ne
 *       valent pas acceptation (à vérifier) ;</li>
 *   <li>recommandation d'option la plus protectrice selon l'état du patrimoine ;</li>
 *   <li>flag des risques (dévolution forcée passé le délai, mise en demeure ayant
 *       réduit le délai, patrimoine insolvable avec acceptation envisagée).</li>
 * </ul>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. Outil bâti depuis les sources belges,
 * non réutilisable côté FR (les options FR existent — CC art. 768+ — mais les délais
 * et formes diffèrent — à vérifier). Référence mémoire :
 * {@code feedback_belgique_never_forget}.</p>
 *
 * <p><b>Validation juridique requise</b> : les articles cités, le délai de 4 mois,
 * le délai raccourci de 2 mois post mise en demeure, le délai de 17 ans pour
 * l'inventaire bénéficiaire, et la compétence du greffe TF (vs notaire — réforme
 * 2018-2019 redistribuant ces compétences) sont à <b>valider par un avocat belge
 * avant mise en production</b>. Articles tagués (à vérifier) cohérent audit F-191.</p>
 */
public final class SuccessionBeAcceptationRenonciationCalculator {

    /** Verdict d'analyse de l'option successorale. */
    public enum SuccessionBeAcceptationRenonciationVerdict {
        OPTION_LIBRE_DELAI_OK,
        OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE,
        OPTION_RECOMMANDEE_RENONCIATION,
        ACCEPTATION_TACITE_PROBABLE,
        DELAI_CRITIQUE,
        DELAI_DEPASSE,
        QUALIFICATION_INCOMPLETE
    }

    /** Qualité d'héritier appelé à la succession. */
    public enum QualiteHeritierBe {
        CONJOINT_SURVIVANT,
        COHABITANT_LEGAL_SURVIVANT,
        ENFANT,
        DESCENDANT_REPRESENTATION,
        PARENT,
        FRERE_SOEUR,
        LEGATAIRE_UNIVERSEL,
        LEGATAIRE_PARTICULIER
    }

    /** État du patrimoine successoral apparent. */
    public enum EtatPatrimoineSuccessoralBe {
        SOLVABLE,
        DOUTEUX,
        INSOLVABLE,
        INCONNU
    }

    /**
     * Actes accomplis par l'héritier susceptibles ou non de valoir acceptation
     * tacite (CC art. 779 ancien / équivalent réformé — à vérifier).
     */
    public enum ActeHeritierBe {
        AUCUN,
        /** Acceptation tacite probable — acte de disposition. */
        ENCAISSEMENT_CREANCE_DEFUNT,
        /** Acceptation tacite probable — acte d'administration. */
        PAIEMENT_DETTE_DEFUNT,
        /** Acceptation tacite probable — acte de disposition. */
        VENTE_BIEN_SUCCESSION,
        /** Acceptation tacite probable — appréhension de la masse. */
        PRISE_POSSESSION_BIENS,
        /** Acte conservatoire NEUTRE — ne vaut pas acceptation tacite (à vérifier). */
        ACTE_CONSERVATOIRE_NEUTRE,
        /** Demande d'inventaire — ne vaut pas acceptation tacite (à vérifier). */
        DEMANDE_INVENTAIRE
    }

    /** Volonté exprimée par le client. */
    public enum VolonteClientSuccessionBe {
        ACCEPTER,
        ACCEPTER_BENEFICE_INVENTAIRE,
        RENONCER,
        INDECIS
    }

    /** Option recommandée par l'outil. */
    public enum OptionRecommandeeSuccessionBe {
        ACCEPTER,
        ACCEPTER_BENEFICE_INVENTAIRE,
        RENONCER
    }

    /** Statut du délai d'option à la date du calcul. */
    public enum DelaiStatutBe {
        OK,
        CRITIQUE,
        DEPASSE
    }

    /** Code structuré d'un risque identifié. */
    public enum RisqueSuccessionBeCode {
        ACTE_HERITIER_VALANT_ACCEPTATION_TACITE,
        DELAI_TRES_COURT,
        PATRIMOINE_INSOLVABLE_ACCEPTATION_DANGEREUSE,
        MISE_EN_DEMEURE_DELAI_REDUIT,
        DEVOLUTION_FORCEE_RISQUE
    }

    /** Sévérité d'un risque. */
    public enum SeveriteRisqueBe {
        LOW,
        MEDIUM,
        HIGH
    }

    /** Risque identifié — exposé dans la réponse API. */
    public record RisqueSuccessionBe(
            RisqueSuccessionBeCode code,
            String libelle,
            String fondement,
            SeveriteRisqueBe severite
    ) {}

    // ---------------------------------------------------------------
    // Constantes documentées (à vérifier — validation avocat belge requise)
    // ---------------------------------------------------------------

    /** Délai d'option par défaut, en mois, à compter de l'ouverture de la succession
     *  (CC art. 774+ nouveau — à vérifier). */
    private static final int DELAI_OPTION_MOIS = 4;

    /** Délai raccourci post mise en demeure d'un créancier, en mois (V1 — à vérifier ;
     *  ouverture juridique signalée dans la mini-spec : le délai exact n'est pas tranché
     *  par le modèle, paramètre conservateur à valider). */
    private static final int DELAI_MED_CREANCIER_MOIS = 2;

    /** Seuil de jours restants en deçà duquel le délai est jugé critique. */
    private static final int SEUIL_DELAI_CRITIQUE_JOURS = 30;

    /** Longueur maximale du commentaire libre. */
    private static final int COMMENTAIRE_MAX = 1000;

    /**
     * Actes considérés comme valant <b>acceptation tacite</b> (CC art. 779 ancien /
     * équivalent réformé — à vérifier). Liste figée dans le Calculator (source de
     * vérité). Une évolution doctrinale = nouvelle version de l'outil.
     */
    private static final Set<ActeHeritierBe> ACTES_VALANT_ACCEPTATION_TACITE = EnumSet.of(
            ActeHeritierBe.ENCAISSEMENT_CREANCE_DEFUNT,
            ActeHeritierBe.PAIEMENT_DETTE_DEFUNT,
            ActeHeritierBe.VENTE_BIEN_SUCCESSION,
            ActeHeritierBe.PRISE_POSSESSION_BIENS
    );

    private SuccessionBeAcceptationRenonciationCalculator() {}

    /**
     * Applique l'arbre décisionnel BE des options successorales sur les éléments saisis.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @param today   date de référence pour le calcul des délais (injectable pour les tests)
     * @return résultat structuré (verdict, option recommandée, délai, risques, actions)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static SuccessionBeAcceptationRenonciationResult compute(
            SuccessionBeAcceptationRenonciationInput input, String country, LocalDate today) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — option successorale héritier");
        }
        if (today == null) {
            throw new IllegalArgumentException("Date de référence requise");
        }
        validateInputs(input, today);

        // Cas QUALIFICATION_INCOMPLETE : mise en demeure cochée sans date.
        if (input.miseEnDemeureCreancier() && input.dateMiseEnDemeureCreancier() == null) {
            return resultIncomplet(input, countryNormalized);
        }

        LocalDate dateLimite = calculerDateLimite(input);
        long joursRestantsLong = ChronoUnit.DAYS.between(today, dateLimite);
        int joursRestants = (int) joursRestantsLong;
        DelaiStatutBe delaiStatut = statutDelai(joursRestants);

        List<RisqueSuccessionBe> risques = new ArrayList<>();
        boolean acteAcceptationTacite = detecteActeAcceptationTacite(input);
        if (acteAcceptationTacite) {
            risques.add(new RisqueSuccessionBe(
                    RisqueSuccessionBeCode.ACTE_HERITIER_VALANT_ACCEPTATION_TACITE,
                    "Un ou plusieurs actes accomplis par l'héritier peuvent être qualifiés d'actes "
                            + "d'héritier valant acceptation tacite (encaissement, paiement de dette, "
                            + "vente, prise de possession).",
                    "CC art. 779 ancien / équivalent réformé (à vérifier) — acceptation tacite par "
                            + "actes d'administration ou de disposition à titre d'héritier",
                    SeveriteRisqueBe.HIGH));
        }
        if (delaiStatut == DelaiStatutBe.CRITIQUE) {
            risques.add(new RisqueSuccessionBe(
                    RisqueSuccessionBeCode.DELAI_TRES_COURT,
                    "Le délai d'option restant est inférieur à " + SEUIL_DELAI_CRITIQUE_JOURS
                            + " jours — l'option doit être formalisée rapidement.",
                    "CC art. 774+ nouveau (à vérifier)",
                    SeveriteRisqueBe.HIGH));
        }
        if (input.etatPatrimoineSuccessoral() == EtatPatrimoineSuccessoralBe.INSOLVABLE
                && input.volonteClient() == VolonteClientSuccessionBe.ACCEPTER) {
            risques.add(new RisqueSuccessionBe(
                    RisqueSuccessionBeCode.PATRIMOINE_INSOLVABLE_ACCEPTATION_DANGEREUSE,
                    "Patrimoine successoral insolvable signalé et volonté d'acceptation pure : "
                            + "l'héritier serait tenu des dettes au-delà de l'actif (à vérifier).",
                    "CC art. 774+ nouveau / effets de l'acceptation pure (à vérifier)",
                    SeveriteRisqueBe.HIGH));
        }
        if (input.miseEnDemeureCreancier()) {
            risques.add(new RisqueSuccessionBe(
                    RisqueSuccessionBeCode.MISE_EN_DEMEURE_DELAI_REDUIT,
                    "Mise en demeure d'un créancier d'opter : le délai d'option est potentiellement "
                            + "raccourci par rapport au délai légal de 4 mois.",
                    "CC art. 774+ nouveau (à vérifier) — mise en demeure d'opter",
                    SeveriteRisqueBe.MEDIUM));
        }
        if (delaiStatut == DelaiStatutBe.DEPASSE) {
            risques.add(new RisqueSuccessionBe(
                    RisqueSuccessionBeCode.DEVOLUTION_FORCEE_RISQUE,
                    "Délai d'option dépassé sans option formalisée — risque d'acceptation tacite "
                            + "à défaut d'option, ou alimentation du créancier sur la succession.",
                    "CC art. 774+ nouveau (à vérifier)",
                    SeveriteRisqueBe.HIGH));
        }

        SuccessionBeAcceptationRenonciationVerdict verdict =
                verdictPour(input, acteAcceptationTacite, delaiStatut);
        OptionRecommandeeSuccessionBe option = optionRecommandee(input, acteAcceptationTacite);

        List<String> actions = construireActionsConcretes(option, input);
        List<String> bases = basesJuridiques();
        List<String> messages = construireMessages(input, verdict, option, dateLimite,
                joursRestants, delaiStatut, acteAcceptationTacite);

        return new SuccessionBeAcceptationRenonciationResult(
                verdict, option, dateLimite, joursRestants, delaiStatut,
                actions, List.copyOf(risques), bases, messages, countryNormalized);
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(SuccessionBeAcceptationRenonciationInput in, LocalDate today) {
        if (in.dateDeces() == null) {
            throw new IllegalArgumentException("La date du décès est requise");
        }
        if (in.dateDeces().isAfter(today)) {
            throw new IllegalArgumentException("La date du décès ne peut pas être dans le futur");
        }
        if (in.qualiteHeritier() == null) {
            throw new IllegalArgumentException("La qualité de l'héritier est requise");
        }
        if (in.etatPatrimoineSuccessoral() == null) {
            throw new IllegalArgumentException("L'état du patrimoine successoral est requis");
        }
        if (in.actesAccomplis() == null) {
            throw new IllegalArgumentException("La liste des actes accomplis est requise (vide si aucun)");
        }
        if (in.volonteClient() == null) {
            throw new IllegalArgumentException("La volonté du client est requise");
        }
        if (in.miseEnDemeureCreancier() && in.dateMiseEnDemeureCreancier() != null
                && in.dateMiseEnDemeureCreancier().isBefore(in.dateDeces())) {
            throw new IllegalArgumentException(
                    "La date de mise en demeure ne peut être antérieure à la date du décès");
        }
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    // ---------------------------------------------------------------
    // Calcul du délai
    // ---------------------------------------------------------------

    /**
     * Calcule la date limite d'option. Par défaut {@code dateDeces + 4 mois} ; si une
     * mise en demeure d'un créancier est intervenue, on conserve la date la plus
     * proche entre le délai légal et {@code dateMiseEnDemeure + 2 mois} (V1, à vérifier).
     */
    private static LocalDate calculerDateLimite(SuccessionBeAcceptationRenonciationInput in) {
        LocalDate limiteLegale = in.dateDeces().plusMonths(DELAI_OPTION_MOIS);
        if (in.miseEnDemeureCreancier() && in.dateMiseEnDemeureCreancier() != null) {
            LocalDate limiteMed = in.dateMiseEnDemeureCreancier().plusMonths(DELAI_MED_CREANCIER_MOIS);
            return limiteMed.isBefore(limiteLegale) ? limiteMed : limiteLegale;
        }
        return limiteLegale;
    }

    private static DelaiStatutBe statutDelai(int joursRestants) {
        if (joursRestants < 0) {
            return DelaiStatutBe.DEPASSE;
        }
        if (joursRestants <= SEUIL_DELAI_CRITIQUE_JOURS) {
            return DelaiStatutBe.CRITIQUE;
        }
        return DelaiStatutBe.OK;
    }

    // ---------------------------------------------------------------
    // Détection acceptation tacite
    // ---------------------------------------------------------------

    /**
     * Détecte si au moins un acte accompli figure dans la liste figée des actes
     * valant acceptation tacite. L'acte conservatoire neutre et la demande
     * d'inventaire ne valent pas acceptation tacite (à vérifier).
     */
    private static boolean detecteActeAcceptationTacite(SuccessionBeAcceptationRenonciationInput in) {
        if (in.actesAccomplis() == null) {
            return false;
        }
        for (ActeHeritierBe acte : in.actesAccomplis()) {
            if (ACTES_VALANT_ACCEPTATION_TACITE.contains(acte)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Verdict et option recommandée
    // ---------------------------------------------------------------

    /**
     * Verdict piloté par la règle figée de la mini-spec (ordre exact) :
     * délai dépassé → DELAI_DEPASSE ;
     * sinon acte d'héritier accompli → ACCEPTATION_TACITE_PROBABLE ;
     * sinon insolvable → OPTION_RECOMMANDEE_RENONCIATION ;
     * sinon douteux/inconnu → OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE ;
     * sinon délai critique → DELAI_CRITIQUE ;
     * sinon OPTION_LIBRE_DELAI_OK.
     */
    private static SuccessionBeAcceptationRenonciationVerdict verdictPour(
            SuccessionBeAcceptationRenonciationInput in,
            boolean acteAcceptationTacite,
            DelaiStatutBe delaiStatut) {
        if (delaiStatut == DelaiStatutBe.DEPASSE) {
            return SuccessionBeAcceptationRenonciationVerdict.DELAI_DEPASSE;
        }
        if (acteAcceptationTacite) {
            return SuccessionBeAcceptationRenonciationVerdict.ACCEPTATION_TACITE_PROBABLE;
        }
        if (in.etatPatrimoineSuccessoral() == EtatPatrimoineSuccessoralBe.INSOLVABLE) {
            return SuccessionBeAcceptationRenonciationVerdict.OPTION_RECOMMANDEE_RENONCIATION;
        }
        if (in.etatPatrimoineSuccessoral() == EtatPatrimoineSuccessoralBe.DOUTEUX
                || in.etatPatrimoineSuccessoral() == EtatPatrimoineSuccessoralBe.INCONNU) {
            return SuccessionBeAcceptationRenonciationVerdict.OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE;
        }
        if (delaiStatut == DelaiStatutBe.CRITIQUE) {
            return SuccessionBeAcceptationRenonciationVerdict.DELAI_CRITIQUE;
        }
        return SuccessionBeAcceptationRenonciationVerdict.OPTION_LIBRE_DELAI_OK;
    }

    /**
     * Recommande l'option la plus protectrice selon l'état du patrimoine et la
     * volonté du client. Si un acte d'héritier a déjà été accompli et que le client
     * voulait renoncer, on bascule sur l'acceptation sous bénéfice d'inventaire car
     * la renonciation devient inopérante (à vérifier).
     */
    private static OptionRecommandeeSuccessionBe optionRecommandee(
            SuccessionBeAcceptationRenonciationInput in, boolean acteAcceptationTacite) {
        // Acte d'héritier + volonté de renoncer : la renonciation pure est compromise
        // (à vérifier) → bascule sur bénéfice d'inventaire pour limiter l'engagement.
        if (acteAcceptationTacite && in.volonteClient() == VolonteClientSuccessionBe.RENONCER) {
            return OptionRecommandeeSuccessionBe.ACCEPTER_BENEFICE_INVENTAIRE;
        }
        // Volonté explicite de renoncer sans acte d'héritier : on suit le client.
        if (in.volonteClient() == VolonteClientSuccessionBe.RENONCER) {
            return OptionRecommandeeSuccessionBe.RENONCER;
        }
        // Patrimoine insolvable : renonciation conseillée quelle que soit la volonté.
        if (in.etatPatrimoineSuccessoral() == EtatPatrimoineSuccessoralBe.INSOLVABLE) {
            return OptionRecommandeeSuccessionBe.RENONCER;
        }
        // Patrimoine douteux ou inconnu : bénéfice d'inventaire (acte d'héritier compris).
        if (in.etatPatrimoineSuccessoral() == EtatPatrimoineSuccessoralBe.DOUTEUX
                || in.etatPatrimoineSuccessoral() == EtatPatrimoineSuccessoralBe.INCONNU) {
            return OptionRecommandeeSuccessionBe.ACCEPTER_BENEFICE_INVENTAIRE;
        }
        // Acte d'héritier accompli sur patrimoine solvable : acceptation tacite probable
        // déjà consommée → on confirme l'acceptation.
        if (acteAcceptationTacite) {
            return OptionRecommandeeSuccessionBe.ACCEPTER;
        }
        // Patrimoine solvable + volonté ACCEPTER/INDECIS/ACCEPTER_BENEFICE_INVENTAIRE :
        // si bénéfice d'inventaire demandé explicitement, on le respecte ; sinon acceptation.
        if (in.volonteClient() == VolonteClientSuccessionBe.ACCEPTER_BENEFICE_INVENTAIRE) {
            return OptionRecommandeeSuccessionBe.ACCEPTER_BENEFICE_INVENTAIRE;
        }
        return OptionRecommandeeSuccessionBe.ACCEPTER;
    }

    // ---------------------------------------------------------------
    // Cas QUALIFICATION_INCOMPLETE
    // ---------------------------------------------------------------

    private static SuccessionBeAcceptationRenonciationResult resultIncomplet(
            SuccessionBeAcceptationRenonciationInput in, String country) {
        List<String> messages = List.of(
                "Mise en demeure d'un créancier cochée sans date renseignée — qualification "
                        + "incomplète : impossible de calculer le délai d'option potentiellement "
                        + "raccourci. Renseigner la date de mise en demeure pour obtenir le verdict.");
        return new SuccessionBeAcceptationRenonciationResult(
                SuccessionBeAcceptationRenonciationVerdict.QUALIFICATION_INCOMPLETE,
                null, null, null, null,
                List.of("Renseigner la date de la mise en demeure du créancier d'opter."),
                List.of(),
                basesJuridiques(),
                messages,
                country);
    }

    // ---------------------------------------------------------------
    // Bases juridiques, actions concrètes, messages
    // ---------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "CC art. 774+ nouveau (à vérifier) — options de l'héritier (acceptation pure, "
                        + "acceptation sous bénéfice d'inventaire, renonciation)",
                "CC art. 779 ancien / équivalent réformé (à vérifier) — acceptation tacite par "
                        + "actes d'héritier",
                "CC art. 795 ancien / équivalent réformé (à vérifier) — délai et formalités de la "
                        + "renonciation",
                "Loi du 31 juillet 2017 modifiant le Code civil en matière de successions et de "
                        + "libéralités (à vérifier — réforme 2018-2019)"
        );
    }

    private static List<String> construireActionsConcretes(OptionRecommandeeSuccessionBe option,
                                                            SuccessionBeAcceptationRenonciationInput in) {
        List<String> actions = new ArrayList<>();
        actions.add("Déposer la déclaration d'option au greffe du Tribunal de la famille du dernier "
                + "domicile du défunt (à vérifier — possibilité notaire selon réforme 2018-2019).");
        switch (option) {
            case ACCEPTER -> actions.add(
                    "Préparer la déclaration d'acceptation pure et simple (à vérifier — forme libre, "
                            + "déclaration au greffe ou acceptation expresse).");
            case ACCEPTER_BENEFICE_INVENTAIRE -> {
                actions.add("Préparer la déclaration d'acceptation sous bénéfice d'inventaire (à "
                        + "vérifier — déclaration au greffe TF) et faire dresser l'inventaire "
                        + "(délai 17 ans maximum — à vérifier).");
                actions.add("Cesser tout acte d'héritier (encaissement, paiement, vente, prise de "
                        + "possession) jusqu'à la formalisation de l'option.");
            }
            case RENONCER -> actions.add(
                    "Préparer la déclaration de renonciation au greffe TF (CC art. 795 ancien / "
                            + "équivalent réformé — à vérifier).");
        }
        if (in.miseEnDemeureCreancier()) {
            actions.add("Vérifier le délai exact issu de la mise en demeure du créancier — V1 retient "
                    + "2 mois post mise en demeure (à vérifier avec un avocat belge).");
        }
        return actions;
    }

    private static List<String> construireMessages(
            SuccessionBeAcceptationRenonciationInput in,
            SuccessionBeAcceptationRenonciationVerdict verdict,
            OptionRecommandeeSuccessionBe option,
            LocalDate dateLimite,
            int joursRestants,
            DelaiStatutBe delaiStatut,
            boolean acteAcceptationTacite) {
        List<String> messages = new ArrayList<>();

        switch (verdict) {
            case OPTION_LIBRE_DELAI_OK -> messages.add(
                    "Aucun acte d'héritier susceptible de valoir acceptation tacite, patrimoine "
                            + "successoral clair et délai d'option encore confortable : l'héritier peut "
                            + "librement opter.");
            case OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE -> messages.add(
                    "Patrimoine successoral " + libellePatrimoine(in.etatPatrimoineSuccessoral())
                            + " : l'acceptation sous bénéfice d'inventaire est l'option la plus "
                            + "protectrice — elle limite l'engagement de l'héritier aux forces de la "
                            + "succession (à vérifier).");
            case OPTION_RECOMMANDEE_RENONCIATION -> messages.add(
                    "Patrimoine successoral insolvable signalé : la renonciation est l'option la plus "
                            + "protectrice — l'héritier est réputé n'avoir jamais hérité (à vérifier).");
            case ACCEPTATION_TACITE_PROBABLE -> messages.add(
                    "Au moins un acte accompli par l'héritier peut valoir acceptation tacite : "
                            + "l'option doit être formalisée immédiatement pour cristalliser la position "
                            + "(à vérifier sur la qualification de l'acte).");
            case DELAI_CRITIQUE -> messages.add(
                    "Délai d'option restant inférieur à " + SEUIL_DELAI_CRITIQUE_JOURS
                            + " jours — formaliser l'option sans délai.");
            case DELAI_DEPASSE -> messages.add(
                    "Délai d'option dépassé — risque de dévolution forcée. Vérifier la situation "
                            + "réelle avec un avocat belge sans délai.");
            case QUALIFICATION_INCOMPLETE -> messages.add(
                    "Qualification incomplète : renseigner les éléments manquants pour obtenir le verdict.");
        }

        if (dateLimite != null) {
            messages.add("Délai d'option par défaut : " + DELAI_OPTION_MOIS
                    + " mois à compter de l'ouverture de la succession (à vérifier). Date limite : "
                    + format(dateLimite) + ". " + libelleJoursRestants(joursRestants, delaiStatut) + ".");
        }

        if (option != null) {
            messages.add("Option recommandée : " + libelleOption(option) + ".");
        }

        if (acteAcceptationTacite) {
            messages.add("Au moins un acte d'héritier figure dans la liste : encaissement, paiement de "
                    + "dette du défunt, vente d'un bien de la succession ou prise de possession — "
                    + "vérifier précisément la qualification juridique de l'acte (à vérifier).");
        }

        return messages;
    }

    private static String libellePatrimoine(EtatPatrimoineSuccessoralBe etat) {
        return switch (etat) {
            case SOLVABLE -> "solvable";
            case DOUTEUX -> "douteux";
            case INSOLVABLE -> "insolvable";
            case INCONNU -> "inconnu";
        };
    }

    private static String libelleOption(OptionRecommandeeSuccessionBe option) {
        return switch (option) {
            case ACCEPTER -> "acceptation pure et simple";
            case ACCEPTER_BENEFICE_INVENTAIRE -> "acceptation sous bénéfice d'inventaire";
            case RENONCER -> "renonciation à la succession";
        };
    }

    private static String libelleJoursRestants(int joursRestants, DelaiStatutBe statut) {
        if (statut == DelaiStatutBe.DEPASSE) {
            return "Délai dépassé de " + Math.abs(joursRestants) + " jours";
        }
        return joursRestants + " jours restants — agir dans les délais";
    }

    private static String format(LocalDate d) {
        if (d == null) {
            return "—";
        }
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
