package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-217-14 : moteur décisionnel BE pour la protection du majeur incapable —
 * statut unique d'administration issu de la loi du 17/03/2013 (en vigueur le
 * 01/09/2014 — à vérifier), qui a refondu le droit belge de la protection
 * des majeurs (remplaçant administration provisoire, minorité prolongée,
 * conseil judiciaire et interdiction).
 *
 * <p>Arbre décisionnel construit à partir des sources belges :</p>
 * <ul>
 *   <li>détection du <b>mandat extra-judiciaire</b> (CC art. 490 nouveau —
 *       à vérifier) : si le majeur a signé un mandat alors qu'il était encore
 *       capable, après l'entrée en vigueur de la loi, la voie privée prime ;</li>
 *   <li>détection d'une <b>urgence vitale</b> permettant une mesure
 *       provisoire ;</li>
 *   <li>recommandation de la <b>mesure d'administration</b> adéquate selon
 *       la gravité de l'incapacité ({@code ADMINISTRATION_BIENS} /
 *       {@code ADMINISTRATION_PERSONNE} /
 *       {@code ADMINISTRATION_BIENS_ET_PERSONNE}) ;</li>
 *   <li>liste des <b>actes protégés</b> avec leur nécessité
 *       (autorisation JP préalable, administrateur seul, autonomie
 *       préservée).</li>
 * </ul>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. Outil bâti depuis les sources belges,
 * non réutilisable côté FR (les régimes FR — tutelle / curatelle / sauvegarde
 * de justice — sont structurellement distincts du statut unique belge). Référence
 * mémoire : {@code feedback_belgique_never_forget}.</p>
 *
 * <p><b>Validation juridique requise</b> : la loi du 17/03/2013, les articles
 * du CJ (594-595 — compétence Justice de paix), CC art. 490 nouveau (mandat
 * extra-judiciaire), le principe de capacité résiduelle et la liste des actes
 * soumis à autorisation préalable sont à <b>valider par un avocat belge avant
 * mise en production</b>. Articles tagués (à vérifier) cohérent audit F-191.</p>
 */
public final class ProtectionMajeurBeCalculator {

    /** Verdict d'analyse de la situation de protection du majeur. */
    public enum ProtectionMajeurBeVerdict {
        MANDAT_EXTRA_JUDICIAIRE_VALABLE,
        MESURE_PROTECTION_RECOMMANDEE,
        URGENCE_MESURE_PROVISOIRE,
        QUALIFICATION_INCOMPLETE
    }

    /** Nature de l'altération à l'origine de l'incapacité. */
    public enum NatureAlterationBe {
        MEDICALE_DURABLE,
        MEDICALE_TEMPORAIRE,
        PRODIGALITE,
        AUTRE
    }

    /** Gravité de l'incapacité (CC art. 490 nouveau — à vérifier). */
    public enum GraviteIncapaciteBe {
        INCAPACITE_GERER_BIENS,
        INCAPACITE_GERER_PERSONNE,
        LES_DEUX,
        INCAPACITE_PARTIELLE
    }

    /** Niveau d'urgence procédurale. */
    public enum NiveauUrgenceProtectionBe {
        URGENCE_VITALE,
        URGENCE_PATRIMONIALE,
        AUCUNE_URGENCE
    }

    /** Mode de saisine envisagé pour la mesure de protection. */
    public enum ModeSaisineProtectionBe {
        REQUETE_JP,
        INDETERMINE
    }

    /** Mesure de protection recommandée. */
    public enum MesureProtectionBe {
        MANDAT_EXTRA_JUDICIAIRE,
        ADMINISTRATION_BIENS,
        ADMINISTRATION_PERSONNE,
        ADMINISTRATION_BIENS_ET_PERSONNE,
        MESURE_PROVISOIRE_URGENCE,
        AUCUNE_RECOMMANDATION
    }

    /** Juridiction compétente pour la saisine de la mesure de protection. */
    public enum JuridictionProtectionBe {
        JUSTICE_PAIX,
        AUTRE_JURIDICTION
    }

    /** Code structuré d'un acte protégé par la mesure de protection. */
    public enum ActeProtegeBeCode {
        DISPOSITION_BIENS_IMMOBILIERS,
        DISPOSITION_BIENS_MOBILIERS_VALEURS,
        OPERATION_BANCAIRE,
        ACTES_PATRIMONIAUX_COURANTS,
        ACTES_PERSONNELS_LIEU_VIE,
        ACTES_PERSONNELS_SOINS,
        MARIAGE_PACS_TESTAMENT,
        ACTES_QUOTIDIENS
    }

    /** Nécessité d'autorisation / autonomie sur un acte protégé. */
    public enum NecessiteActeBe {
        AUTORISATION_JP_PREALABLE,
        ADMINISTRATEUR_SEUL,
        AUTONOMIE_PRESERVEE,
        INTERDICTION_ABSOLUE
    }

    /** Acte protégé exposé dans la réponse API. */
    public record ActeProtegeBe(
            ActeProtegeBeCode code,
            String libelle,
            NecessiteActeBe necessite,
            String fondement
    ) {}

    // ---------------------------------------------------------------
    // Constantes documentées (à vérifier — validation avocat belge requise)
    // ---------------------------------------------------------------

    /** Date d'entrée en vigueur de la loi du 17/03/2013 (à vérifier). */
    private static final LocalDate LOI_2013_ENTREE_VIGUEUR = LocalDate.of(2014, 9, 1);

    /** Longueur maximale du commentaire libre. */
    private static final int COMMENTAIRE_MAX = 1000;

    private ProtectionMajeurBeCalculator() {}

    /**
     * Applique l'arbre décisionnel BE de protection du majeur sur les éléments
     * saisis.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, mesure recommandée, juridiction,
     *         actes protégés, actions concrètes, bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static ProtectionMajeurBeResult compute(
            ProtectionMajeurBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — protection du majeur incapable");
        }
        validateInputs(input);

        // Règle 1 : mandat coché sans date renseignée → QUALIFICATION_INCOMPLETE.
        if (input.mandatExtraJudiciaireSigne()
                && input.mandatExtraJudiciaireDateSignature() == null) {
            return resultIncompletMandatSansDate(countryNormalized);
        }

        // Règle 2 : mandat signé avant l'entrée en vigueur de la loi (2014-09-01)
        // → QUALIFICATION_INCOMPLETE + message d'aide droit transitoire.
        if (input.mandatExtraJudiciaireSigne()
                && input.mandatExtraJudiciaireDateSignature() != null
                && input.mandatExtraJudiciaireDateSignature().isBefore(LOI_2013_ENTREE_VIGUEUR)) {
            return resultMandatAnterieurLoi(input, countryNormalized);
        }

        // Règle 3 : mandat signé valablement (date >= 2014-09-01) → voie privée prime.
        if (input.mandatExtraJudiciaireSigne()) {
            return resultMandatValable(input, countryNormalized);
        }

        // Règle 4 : urgence vitale → mesure provisoire.
        if (input.niveauUrgence() == NiveauUrgenceProtectionBe.URGENCE_VITALE) {
            return resultUrgenceVitale(input, countryNormalized);
        }

        // Règle 5 : mesure d'administration ordinaire calculée selon la gravité.
        return resultMesureRecommandee(input, countryNormalized);
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(ProtectionMajeurBeInput in) {
        if (in.natureAlteration() == null) {
            throw new IllegalArgumentException("La nature de l'altération est requise");
        }
        if (in.graviteIncapacite() == null) {
            throw new IllegalArgumentException("La gravité de l'incapacité est requise");
        }
        if (in.niveauUrgence() == null) {
            throw new IllegalArgumentException("Le niveau d'urgence est requis");
        }
        if (in.modeSaisineEnvisage() == null) {
            throw new IllegalArgumentException("Le mode de saisine envisagé est requis");
        }
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    // ---------------------------------------------------------------
    // Branches de l'arbre décisionnel
    // ---------------------------------------------------------------

    private static ProtectionMajeurBeResult resultIncompletMandatSansDate(String country) {
        List<String> messages = List.of(
                "Mandat extra-judiciaire signalé comme signé sans date de signature renseignée — "
                        + "qualification incomplète : impossible de vérifier que le mandat est postérieur à "
                        + "l'entrée en vigueur de la loi du 17/03/2013 (01/09/2014). Renseigner la date de "
                        + "signature pour obtenir le verdict.");
        return new ProtectionMajeurBeResult(
                ProtectionMajeurBeVerdict.QUALIFICATION_INCOMPLETE,
                MesureProtectionBe.AUCUNE_RECOMMANDATION,
                null,
                "Loi 17/03/2013 (à vérifier) — mandat extra-judiciaire CC art. 490 nouveau",
                List.of(),
                List.of("Renseigner la date de signature du mandat extra-judiciaire."),
                basesJuridiques(),
                messages,
                country);
    }

    private static ProtectionMajeurBeResult resultMandatAnterieurLoi(
            ProtectionMajeurBeInput in, String country) {
        List<String> messages = List.of(
                "Mandat extra-judiciaire antérieur à la loi du 17/03/2013 (entrée en vigueur "
                        + "01/09/2014 — à vérifier) : sa validité doit être confirmée par un avocat belge "
                        + "(droit transitoire). Possibilité que le mandat reste valable, mais qualification "
                        + "à valider en l'état du droit transitoire."
        );
        return new ProtectionMajeurBeResult(
                ProtectionMajeurBeVerdict.QUALIFICATION_INCOMPLETE,
                MesureProtectionBe.AUCUNE_RECOMMANDATION,
                null,
                "Loi 17/03/2013 — droit transitoire mandat antérieur (à vérifier)",
                List.of(),
                List.of(
                        "Vérifier avec un avocat belge la validité du mandat extra-judiciaire signé avant "
                                + "le 01/09/2014.",
                        "Le cas échéant, faire confirmer la validité ou renouveler le mandat selon le "
                                + "régime issu de la loi du 17/03/2013."),
                basesJuridiques(),
                messages,
                country);
    }

    private static ProtectionMajeurBeResult resultMandatValable(
            ProtectionMajeurBeInput in, String country) {
        List<String> actions = new ArrayList<>();
        actions.add("Activer le mandat extra-judiciaire : voie privée — pas de saisine judiciaire nécessaire.");
        actions.add("Vérifier que le mandat couvre bien les actes nécessaires à la gestion actuelle (biens "
                + "et/ou personne) ; à défaut, envisager une saisine complémentaire de la justice de paix.");
        if (in.environnementFamilialProtecteur()) {
            actions.add("L'environnement familial protecteur identifié peut servir d'appui à l'exécution "
                    + "du mandat (mandataire familial désigné dans l'acte).");
        }

        List<String> messages = new ArrayList<>();
        messages.add("Un mandat extra-judiciaire a été signé par le majeur alors qu'il était encore "
                + "capable (CC art. 490 nouveau — à vérifier). La voie privée prime sur l'administration "
                + "judiciaire : pas de saisine de la justice de paix nécessaire pour activer la protection.");
        if (in.niveauUrgence() == NiveauUrgenceProtectionBe.URGENCE_PATRIMONIALE) {
            messages.add("Urgence patrimoniale signalée : vérifier que le mandat couvre les actes "
                    + "patrimoniaux urgents ; à défaut, saisir la justice de paix en complément.");
        }
        if (in.declarationAnticipeeExiste()) {
            messages.add("Une déclaration anticipée du majeur existe également (à vérifier) — elle peut "
                    + "compléter le mandat sur le volet de la personne (lieu de vie, soins).");
        }

        return new ProtectionMajeurBeResult(
                ProtectionMajeurBeVerdict.MANDAT_EXTRA_JUDICIAIRE_VALABLE,
                MesureProtectionBe.MANDAT_EXTRA_JUDICIAIRE,
                null,
                "CC art. 490 nouveau (à vérifier) — mandat extra-judiciaire (loi 17/03/2013)",
                List.of(),
                actions,
                basesJuridiques(),
                messages,
                country);
    }

    private static ProtectionMajeurBeResult resultUrgenceVitale(
            ProtectionMajeurBeInput in, String country) {
        List<String> actions = new ArrayList<>();
        actions.add("Saisir la justice de paix de la résidence du majeur en URGENCE (modèle de requête à "
                + "fournir par F-217 vague ultérieure ou F-223).");
        actions.add("Joindre un certificat médical circonstancié de moins de 15 jours (à vérifier).");
        actions.add("Demander une mesure provisoire (loi 17/03/2013 — à vérifier) en attendant la mesure "
                + "définitive d'administration.");

        List<String> messages = new ArrayList<>();
        messages.add("Urgence vitale signalée : une mesure provisoire de protection est envisageable "
                + "(loi du 17/03/2013 — à vérifier) en attendant la mise en place de la mesure définitive "
                + "d'administration.");
        if (in.environnementFamilialProtecteur()) {
            messages.add("Environnement familial protecteur identifié : proposer un proche comme "
                    + "administrateur provisoire si la justice de paix valide la mesure.");
        }

        return new ProtectionMajeurBeResult(
                ProtectionMajeurBeVerdict.URGENCE_MESURE_PROVISOIRE,
                MesureProtectionBe.MESURE_PROVISOIRE_URGENCE,
                JuridictionProtectionBe.JUSTICE_PAIX,
                "Loi 17/03/2013 (à vérifier) ; CJ art. 594-595 (à vérifier) — Justice de paix saisie en "
                        + "urgence ; mesure provisoire dans l'attente de la mesure définitive",
                actesProtegesPourMesure(MesureProtectionBe.MESURE_PROVISOIRE_URGENCE),
                actions,
                basesJuridiques(),
                messages,
                country);
    }

    private static ProtectionMajeurBeResult resultMesureRecommandee(
            ProtectionMajeurBeInput in, String country) {
        MesureProtectionBe mesure = mesureSelonGravite(in.graviteIncapacite());

        List<String> actions = new ArrayList<>();
        actions.add("Saisir la Justice de paix de la résidence du majeur par requête (modèle à fournir "
                + "par F-217 vague ultérieure ou F-223).");
        actions.add("Joindre un certificat médical circonstancié de moins de 15 jours (à vérifier).");
        if (in.environnementFamilialProtecteur()) {
            actions.add("Proposer un proche identifié comme `environnementFamilialProtecteur = true` "
                    + "comme administrateur.");
        } else {
            actions.add("À défaut d'environnement familial protecteur identifié, la justice de paix "
                    + "désignera un administrateur professionnel (à vérifier — barreau / inscrit liste JP).");
        }
        if (in.declarationAnticipeeExiste()) {
            actions.add("Joindre la déclaration anticipée du majeur (à vérifier — la JP doit en tenir "
                    + "compte pour la désignation de l'administrateur).");
        }

        List<String> messages = new ArrayList<>();
        messages.add(libelleMessageMesure(in.graviteIncapacite(), mesure));
        if (in.niveauUrgence() == NiveauUrgenceProtectionBe.URGENCE_PATRIMONIALE) {
            messages.add("Urgence patrimoniale signalée (sans urgence vitale) : la voie ordinaire de la "
                    + "Justice de paix reste indiquée — solliciter un calendrier court (à vérifier).");
        }
        if (in.graviteIncapacite() == GraviteIncapaciteBe.INCAPACITE_PARTIELLE) {
            messages.add("Incapacité qualifiée de partielle : par défaut l'outil recommande "
                    + "l'administration des biens, à raffiner cas par cas (paramètre documenté — la loi "
                    + "du 17/03/2013 admet une mesure modulée à la capacité résiduelle).");
        }
        if (!in.environnementFamilialProtecteur()) {
            messages.add("Pas d'environnement familial protecteur signalé : la juridiction désignera "
                    + "vraisemblablement un administrateur professionnel (à vérifier).");
        }

        return new ProtectionMajeurBeResult(
                ProtectionMajeurBeVerdict.MESURE_PROTECTION_RECOMMANDEE,
                mesure,
                JuridictionProtectionBe.JUSTICE_PAIX,
                "Loi 17/03/2013 (à vérifier) ; CJ art. 594-595 (à vérifier) — compétence Justice de paix "
                        + "de la résidence du majeur",
                actesProtegesPourMesure(mesure),
                actions,
                basesJuridiques(),
                messages,
                country);
    }

    private static MesureProtectionBe mesureSelonGravite(GraviteIncapaciteBe gravite) {
        return switch (gravite) {
            case INCAPACITE_GERER_BIENS -> MesureProtectionBe.ADMINISTRATION_BIENS;
            case INCAPACITE_GERER_PERSONNE -> MesureProtectionBe.ADMINISTRATION_PERSONNE;
            case LES_DEUX -> MesureProtectionBe.ADMINISTRATION_BIENS_ET_PERSONNE;
            // Incapacité partielle : par défaut → biens (à raffiner cas par cas).
            case INCAPACITE_PARTIELLE -> MesureProtectionBe.ADMINISTRATION_BIENS;
        };
    }

    // ---------------------------------------------------------------
    // Liste des actes protégés selon la mesure
    // ---------------------------------------------------------------

    /**
     * Liste des actes protégés exposés pour une mesure donnée. Centralise les
     * fondements juridiques (loi 17/03/2013 — à vérifier).
     */
    private static List<ActeProtegeBe> actesProtegesPourMesure(MesureProtectionBe mesure) {
        if (mesure == MesureProtectionBe.MANDAT_EXTRA_JUDICIAIRE
                || mesure == MesureProtectionBe.AUCUNE_RECOMMANDATION) {
            return List.of();
        }

        boolean couvreBiens = mesure == MesureProtectionBe.ADMINISTRATION_BIENS
                || mesure == MesureProtectionBe.ADMINISTRATION_BIENS_ET_PERSONNE
                || mesure == MesureProtectionBe.MESURE_PROVISOIRE_URGENCE;
        boolean couvrePersonne = mesure == MesureProtectionBe.ADMINISTRATION_PERSONNE
                || mesure == MesureProtectionBe.ADMINISTRATION_BIENS_ET_PERSONNE
                || mesure == MesureProtectionBe.MESURE_PROVISOIRE_URGENCE;

        List<ActeProtegeBe> actes = new ArrayList<>();
        if (couvreBiens) {
            actes.add(new ActeProtegeBe(
                    ActeProtegeBeCode.DISPOSITION_BIENS_IMMOBILIERS,
                    "Disposition d'un bien immobilier (vente, hypothèque)",
                    NecessiteActeBe.AUTORISATION_JP_PREALABLE,
                    "Loi 17/03/2013 — actes de disposition soumis à autorisation préalable "
                            + "de la justice de paix (à vérifier)"));
            actes.add(new ActeProtegeBe(
                    ActeProtegeBeCode.DISPOSITION_BIENS_MOBILIERS_VALEURS,
                    "Vente de valeurs mobilières significatives",
                    NecessiteActeBe.AUTORISATION_JP_PREALABLE,
                    "Loi 17/03/2013 — actes de disposition de valeurs mobilières (à vérifier)"));
            actes.add(new ActeProtegeBe(
                    ActeProtegeBeCode.OPERATION_BANCAIRE,
                    "Opérations bancaires courantes",
                    NecessiteActeBe.ADMINISTRATEUR_SEUL,
                    "Loi 17/03/2013 — actes d'administration (à vérifier)"));
            actes.add(new ActeProtegeBe(
                    ActeProtegeBeCode.ACTES_PATRIMONIAUX_COURANTS,
                    "Actes patrimoniaux courants",
                    NecessiteActeBe.ADMINISTRATEUR_SEUL,
                    "Loi 17/03/2013 — administration courante des biens (à vérifier)"));
        }
        if (couvrePersonne) {
            actes.add(new ActeProtegeBe(
                    ActeProtegeBeCode.ACTES_PERSONNELS_LIEU_VIE,
                    "Choix du lieu de vie",
                    NecessiteActeBe.AUTORISATION_JP_PREALABLE,
                    "Loi 17/03/2013 — décision personnelle soumise à autorisation JP "
                            + "lorsque la mesure couvre la personne (à vérifier)"));
            actes.add(new ActeProtegeBe(
                    ActeProtegeBeCode.ACTES_PERSONNELS_SOINS,
                    "Soins médicaux courants",
                    NecessiteActeBe.AUTONOMIE_PRESERVEE,
                    "Loi 17/03/2013 — principe de capacité résiduelle / autonomie maximale "
                            + "(à vérifier, sauf décision contraire de la JP)"));
        }
        // Actes hybrides présents pour toute mesure d'administration.
        actes.add(new ActeProtegeBe(
                ActeProtegeBeCode.MARIAGE_PACS_TESTAMENT,
                "Mariage, cohabitation légale, testament",
                NecessiteActeBe.AUTORISATION_JP_PREALABLE,
                "Loi 17/03/2013 — actes très personnels parfois soumis à autorisation JP, "
                        + "testament sous conditions médicales (à vérifier)"));
        actes.add(new ActeProtegeBe(
                ActeProtegeBeCode.ACTES_QUOTIDIENS,
                "Actes de la vie quotidienne (subsistance, soins courants)",
                NecessiteActeBe.AUTONOMIE_PRESERVEE,
                "Loi 17/03/2013 — principe de capacité résiduelle (à vérifier)"));
        return actes;
    }

    // ---------------------------------------------------------------
    // Bases juridiques + messages d'aide
    // ---------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "Loi du 17/03/2013 (à vérifier) — statut unique de protection des majeurs "
                        + "(administration de la personne et/ou des biens)",
                "CJ art. 594-595 (à vérifier) — compétence Justice de paix de la résidence du majeur",
                "CC art. 490 nouveau (à vérifier) — mandat extra-judiciaire",
                "Loi 17/03/2013 — principe de capacité résiduelle / autonomie maximale (à vérifier)"
        );
    }

    private static String libelleMessageMesure(GraviteIncapaciteBe gravite, MesureProtectionBe mesure) {
        return switch (gravite) {
            case INCAPACITE_GERER_BIENS ->
                    "Incapacité de gestion des biens identifiée → administration des biens recommandée "
                            + "(loi 17/03/2013). Le principe de capacité résiduelle s'applique pour les actes "
                            + "personnels et les actes de la vie quotidienne.";
            case INCAPACITE_GERER_PERSONNE ->
                    "Incapacité de gestion de la personne identifiée (lieu de vie, soins) → administration "
                            + "de la personne recommandée (loi 17/03/2013). Le patrimoine reste géré par le "
                            + "majeur sauf décision contraire de la JP.";
            case LES_DEUX ->
                    "Incapacité étendue aux biens ET à la personne identifiée → administration des biens "
                            + "ET de la personne recommandée (loi 17/03/2013). L'autonomie maximale reste "
                            + "préservée pour les actes de la vie quotidienne.";
            case INCAPACITE_PARTIELLE ->
                    "Incapacité partielle identifiée → administration des biens recommandée par défaut. "
                            + "À raffiner avec la JP qui peut moduler la mesure selon la capacité résiduelle "
                            + "(loi 17/03/2013).";
        };
    }
}
