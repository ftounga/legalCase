package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique des pièces requises par type de titre de séjour et par pays.
 */
public final class ImmigrationPieceReferentiel {

    private static final Map<String, Map<String, List<String>>> REFERENTIEL = Map.of(
            "VISA_ETUDIANT", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Justificatif d'inscription dans un établissement d'enseignement",
                            "Justificatif de ressources suffisantes",
                            "Justificatif d'hébergement",
                            "Photo d'identité",
                            "Formulaire de demande de visa complété"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Lettre d'admission d'un établissement d'enseignement belge",
                            "Preuve de paiement des frais d'inscription",
                            "Justificatif de ressources suffisantes (620 EUR/mois)",
                            "Assurance maladie couvrant la Belgique",
                            "Justificatif d'hébergement",
                            "Photo d'identité récente"
                    )
            ),
            "TITRE_SALARIE", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Contrat de travail signé par l'employeur",
                            "Autorisation de travail délivrée par la DREETS",
                            "Justificatif de qualification professionnelle",
                            "Justificatif d'hébergement",
                            "Photo d'identité",
                            "Formulaire de demande CERFA"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Contrat de travail ou offre d'emploi signée",
                            "Permis de travail délivré par la région compétente",
                            "Diplômes ou qualifications professionnelles",
                            "Justificatif d'hébergement",
                            "Casier judiciaire du pays d'origine",
                            "Photo d'identité récente"
                    )
            ),
            "REGROUPEMENT_FAMILIAL", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité du demandeur",
                            "Titre de séjour en cours de validité du regroupant",
                            "Justificatif de ressources suffisantes du regroupant",
                            "Justificatif de logement aux normes (surface habitable)",
                            "Acte de mariage ou acte de naissance (apostille)",
                            "Photo d'identité",
                            "Formulaire de demande de regroupement familial"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité du demandeur",
                            "Titre de séjour du regroupant en Belgique",
                            "Preuve du lien familial (acte de mariage ou naissance légalisé)",
                            "Justificatif de ressources stables et suffisantes",
                            "Justificatif de logement adéquat",
                            "Assurance maladie",
                            "Photo d'identité récente"
                    )
            ),
            "NATURALISATION", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Titre de séjour valide depuis au moins 5 ans",
                            "Justificatif de résidence habituelle en France",
                            "Justificatif d'intégration (niveau de langue B1 minimum)",
                            "Justificatif de ressources stables",
                            "Casier judiciaire vierge",
                            "Formulaire de demande de naturalisation",
                            "Photo d'identité"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Preuve de résidence légale en Belgique depuis 5 ans",
                            "Déclaration de nationalité ou demande de naturalisation",
                            "Preuve d'intégration sociale (participation civique, connaissance de la langue)",
                            "Justificatif de ressources suffisantes",
                            "Casier judiciaire belge et du pays d'origine",
                            "Photo d'identité récente"
                    )
            )
    );

    private ImmigrationPieceReferentiel() {}

    public static List<String> getPieces(String titreType, String country) {
        Map<String, List<String>> byCountry = REFERENTIEL.get(titreType);
        if (byCountry == null) return List.of();
        return byCountry.getOrDefault(country, List.of());
    }

    public static boolean isTitreTypeValid(String titreType) {
        return REFERENTIEL.containsKey(titreType);
    }

    public static boolean isCountryValid(String country) {
        return "FRANCE".equals(country) || "BELGIQUE".equals(country);
    }
}
