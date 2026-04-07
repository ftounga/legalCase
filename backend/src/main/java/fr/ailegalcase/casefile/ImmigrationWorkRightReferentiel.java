package fr.ailegalcase.casefile;

import java.util.List;

import static fr.ailegalcase.casefile.WorkRightResult.*;

/**
 * Référentiel statique des droits au travail par titre de séjour, France + Belgique.
 */
public final class ImmigrationWorkRightReferentiel {

    private ImmigrationWorkRightReferentiel() {}

    private static final List<WorkRightResult> ALL = List.of(

            // ========== FRANCE ==========

            new WorkRightResult("VLS_TS_ETUDIANT", "VLS-TS Étudiant", "FRANCE", CONDITIONNEL,
                    "Autorisé à travailler 964 heures par an (60 % de la durée annuelle légale). Pas d'autorisation de travail préalable requise.",
                    List.of(
                            "Vérifier la validité du VLS-TS étudiant",
                            "S'assurer du respect du plafond de 964 heures/an",
                            "Conserver une copie du titre de séjour",
                            "Sanctions : amende de 15 000 € par salarié étranger en situation irrégulière (art. L. 8256-2 Code du travail)"
                    ),
                    "Articles L. 422-1 et R. 5221-26 du CESEDA ; art. L. 8251-1 du Code du travail"),

            new WorkRightResult("VLS_TS_SALARIE", "VLS-TS Salarié", "FRANCE", OUI,
                    "Droit au travail inclus dans le titre. Limité à l'employeur et au métier mentionnés sur l'autorisation de travail.",
                    List.of(
                            "Vérification préalable auprès de la préfecture (art. L. 5221-8 Code du travail)",
                            "Paiement de la taxe OFII employeur",
                            "Conserver une copie du titre et de l'autorisation de travail",
                            "Sanctions : 15 000 € d'amende + 5 ans emprisonnement en cas de récidive"
                    ),
                    "Articles L. 421-1, L. 5221-2 et L. 5221-8 du Code du travail"),

            new WorkRightResult("CST_SALARIE", "Carte de séjour temporaire Salarié", "FRANCE", OUI,
                    "Droit au travail pour l'emploi et l'employeur autorisés. Changement d'employeur soumis à nouvelle autorisation.",
                    List.of(
                            "Vérification préalable auprès de la préfecture",
                            "Déclaration d'embauche (DPAE)",
                            "Conserver une copie du titre de séjour"
                    ),
                    "Articles L. 421-1 et L. 421-3 du CESEDA"),

            new WorkRightResult("CARTE_PLURIANNUELLE", "Carte pluriannuelle", "FRANCE", OUI,
                    "Droit au travail inclus. Dispense de demander une autorisation de travail pour tout employeur.",
                    List.of(
                            "Vérification de la validité du titre",
                            "Déclaration d'embauche (DPAE)",
                            "Conserver une copie du titre"
                    ),
                    "Article L. 421-9 du CESEDA"),

            new WorkRightResult("CARTE_RESIDENT", "Carte de résident (10 ans)", "FRANCE", OUI,
                    "Droit au travail plein et entier. Aucune restriction de métier, secteur ou employeur.",
                    List.of(
                            "Vérification de la validité du titre",
                            "Déclaration d'embauche (DPAE)"
                    ),
                    "Article L. 414-10 du CESEDA"),

            new WorkRightResult("APS", "Autorisation provisoire de séjour", "FRANCE", CONDITIONNEL,
                    "Droit au travail si l'APS a été délivrée pour recherche d'emploi (post-master). Pas de restriction de métier.",
                    List.of(
                            "Vérifier que l'APS mentionne le droit au travail",
                            "Conserver une copie du titre",
                            "Déclaration d'embauche (DPAE)"
                    ),
                    "Article L. 422-14 du CESEDA"),

            new WorkRightResult("CST_VPF", "Carte vie privée et familiale", "FRANCE", OUI,
                    "Droit au travail plein. Aucune restriction.",
                    List.of(
                            "Vérification de la validité du titre",
                            "Déclaration d'embauche (DPAE)"
                    ),
                    "Article L. 423-1 du CESEDA"),

            new WorkRightResult("RECEPISSE_ASILE", "Récépissé de demande d'asile", "FRANCE", CONDITIONNEL,
                    "Pas de droit au travail pendant les 6 premiers mois. Après 6 mois sans décision de l'OFPRA, le demandeur peut demander une autorisation provisoire de travail (APT).",
                    List.of(
                            "Vérifier la date de dépôt de la demande d'asile (> 6 mois)",
                            "Demander l'APT auprès de la DREETS si nécessaire",
                            "Sanctions : 15 000 € d'amende par salarié sans autorisation"
                    ),
                    "Article L. 554-1 du CESEDA"),

            // ========== BELGIQUE ==========

            new WorkRightResult("CARTE_A_TRAVAIL", "Carte A — Travail", "BELGIQUE", OUI,
                    "Droit au travail inclus via le permis unique. Limité à l'employeur et à la fonction mentionnés.",
                    List.of(
                            "Déclaration Dimona (déclaration immédiate d'emploi) obligatoire",
                            "Vérifier que le permis unique couvre l'emploi proposé",
                            "Conserver une copie du permis unique et de la carte A",
                            "Sanctions : amende de 2 400 à 48 000 € par travailleur (Code pénal social, art. 175)"
                    ),
                    "Loi du 15 décembre 1980 ; Accord de coopération du 2 février 2018 relatif au permis unique"),

            new WorkRightResult("CARTE_A_ETUDES", "Carte A — Études", "BELGIQUE", CONDITIONNEL,
                    "Autorisé à travailler 20 heures/semaine pendant les périodes de cours. Pas de limite pendant les vacances scolaires.",
                    List.of(
                            "Déclaration Dimona obligatoire",
                            "Vérifier le calendrier académique (20h/semaine en période de cours)",
                            "Pas de permis de travail requis si inscription en enseignement de plein exercice",
                            "Sanctions : amende de 2 400 à 48 000 € par travailleur"
                    ),
                    "Arrêté royal du 2 septembre 2018, art. 17"),

            new WorkRightResult("CARTE_A_FAMILLE", "Carte A — Regroupement familial", "BELGIQUE", OUI,
                    "Droit au travail illimité dès l'obtention de la carte A regroupement familial. Pas de permis de travail requis.",
                    List.of(
                            "Déclaration Dimona obligatoire",
                            "Conserver une copie de la carte A",
                            "Sanctions : amende de 2 400 à 48 000 €"
                    ),
                    "Loi du 15 décembre 1980, art. 10"),

            new WorkRightResult("CARTE_B", "Carte B — Séjour illimité", "BELGIQUE", OUI,
                    "Droit au travail plein et entier. Aucune restriction.",
                    List.of(
                            "Déclaration Dimona obligatoire",
                            "Conserver une copie de la carte B"
                    ),
                    "Loi du 15 décembre 1980, art. 14"),

            new WorkRightResult("CARTE_C", "Carte C — Établissement", "BELGIQUE", OUI,
                    "Droit au travail plein et entier. Aucune restriction.",
                    List.of(
                            "Déclaration Dimona obligatoire"
                    ),
                    "Loi du 15 décembre 1980, art. 15"),

            new WorkRightResult("PERMIS_UNIQUE", "Permis unique", "BELGIQUE", OUI,
                    "Combine autorisation de travail et de séjour. Droit au travail pour l'employeur et la fonction autorisés.",
                    List.of(
                            "Déclaration Dimona obligatoire",
                            "Le changement d'employeur nécessite une nouvelle demande de permis unique",
                            "Conserver une copie du permis unique",
                            "Sanctions : amende de 2 400 à 48 000 €"
                    ),
                    "Accord de coopération du 2 février 2018 ; Arrêté royal du 2 septembre 2018"),

            new WorkRightResult("ANNEXE_15", "Annexe 15", "BELGIQUE", CONDITIONNEL,
                    "Droit au travail uniquement si l'annexe 15 est délivrée dans le cadre d'une procédure de séjour qui ouvre le droit au travail (ex: renouvellement carte A travail). Sinon, pas de droit au travail.",
                    List.of(
                            "Vérifier le motif de délivrance de l'annexe 15",
                            "Si droit au travail : déclaration Dimona obligatoire",
                            "Conserver une copie de l'annexe 15"
                    ),
                    "Arrêté royal du 8 octobre 1981, art. 7"),

            new WorkRightResult("ATTESTATION_IMMATRICULATION", "Attestation d'immatriculation (carte orange)", "BELGIQUE", CONDITIONNEL,
                    "Droit au travail après 4 mois suivant l'introduction de la demande de protection internationale. Pas de restriction de secteur.",
                    List.of(
                            "Vérifier la date d'introduction de la demande (> 4 mois)",
                            "Déclaration Dimona obligatoire",
                            "Le droit au travail cesse si la demande est rejetée définitivement",
                            "Sanctions : amende de 2 400 à 48 000 €"
                    ),
                    "Loi du 15 décembre 1980, art. 18 ; Arrêté royal du 2 septembre 2018, art. 18")
    );

    public static WorkRightResult getByTitreType(String titreType, String country) {
        return ALL.stream()
                .filter(w -> w.titreType().equals(titreType) && w.country().equals(country))
                .findFirst()
                .orElse(null);
    }

    public static WorkRightResult getByTitreType(String titreType) {
        return ALL.stream()
                .filter(w -> w.titreType().equals(titreType))
                .findFirst()
                .orElse(null);
    }

    public static List<WorkRightResult> getAll() {
        return ALL;
    }

    public static List<WorkRightResult> getByCountry(String country) {
        return ALL.stream().filter(w -> w.country().equals(country)).toList();
    }

    public static boolean isTitreTypeValid(String titreType) {
        return ALL.stream().anyMatch(w -> w.titreType().equals(titreType));
    }
}
