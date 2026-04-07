package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Référentiel statique des titres de séjour France + Belgique.
 * Chaque titre contient : code, libellé, pays, motif, conditions, pièces, délai moyen.
 */
public final class ImmigrationTitleReferentiel {

    private ImmigrationTitleReferentiel() {}

    private static final List<TitleRecommendation> ALL_TITLES = List.of(

            // ========== FRANCE ==========

            new TitleRecommendation(
                    "VLS_TS_ETUDIANT", "Visa long séjour valant titre de séjour — Étudiant", "FRANCE", "ETUDES",
                    "Inscription dans un établissement d'enseignement supérieur français. Ressources minimales : 615 €/mois.",
                    List.of("Passeport valide", "Justificatif d'inscription", "Justificatif de ressources (615 €/mois)", "Justificatif d'hébergement", "Photo d'identité", "Formulaire CERFA"),
                    90
            ),
            new TitleRecommendation(
                    "VLS_TS_SALARIE", "Visa long séjour valant titre de séjour — Salarié", "FRANCE", "TRAVAIL",
                    "Contrat de travail d'au moins 12 mois. Autorisation de travail délivrée par la DREETS.",
                    List.of("Passeport valide", "Contrat de travail", "Autorisation de travail DREETS", "Justificatif de qualification", "Justificatif d'hébergement", "Photo d'identité", "Formulaire CERFA"),
                    120
            ),
            new TitleRecommendation(
                    "CST_SALARIE", "Carte de séjour temporaire — Salarié", "FRANCE", "TRAVAIL",
                    "Pour les salariés déjà en France avec un VLS-TS arrivant à expiration. Renouvellement sur justificatif d'emploi.",
                    List.of("Passeport valide", "Titre de séjour précédent", "Contrat de travail en cours", "3 derniers bulletins de salaire", "Justificatif de domicile", "Photo d'identité"),
                    90
            ),
            new TitleRecommendation(
                    "CARTE_PLURIANNUELLE", "Carte de séjour pluriannuelle", "FRANCE", "TRAVAIL",
                    "Délivrée après un premier titre d'un an. Durée de 2 à 4 ans selon le motif. Dispense de renouvellement annuel.",
                    List.of("Passeport valide", "Titre de séjour précédent", "Justificatif de continuité d'emploi ou d'études", "Justificatif de domicile", "Photo d'identité"),
                    60
            ),
            new TitleRecommendation(
                    "CARTE_RESIDENT", "Carte de résident (10 ans)", "FRANCE", "AUTRE",
                    "Après 5 ans de séjour régulier en France. Conditions d'intégration républicaine. Ressources stables.",
                    List.of("Passeport valide", "Titres de séjour des 5 dernières années", "Justificatif d'intégration républicaine", "Justificatif de ressources stables", "Attestation de maîtrise du français (B1)", "Casier judiciaire", "Photo d'identité"),
                    180
            ),
            new TitleRecommendation(
                    "APS", "Autorisation provisoire de séjour", "FRANCE", "AUTRE",
                    "Titre temporaire (3 à 12 mois) pour recherche d'emploi après un diplôme de master en France, ou pour raisons médicales.",
                    List.of("Passeport valide", "Diplôme de master obtenu en France", "Titre de séjour étudiant précédent", "Justificatif de domicile", "Photo d'identité"),
                    30
            ),
            new TitleRecommendation(
                    "CST_VPF", "Carte de séjour — Vie privée et familiale", "FRANCE", "FAMILLE",
                    "Pour les conjoints de Français, parents d'enfant français, ou liens personnels et familiaux en France.",
                    List.of("Passeport valide", "Acte de mariage ou acte de naissance de l'enfant", "Justificatif de vie commune ou de liens familiaux", "Justificatif de domicile", "Photo d'identité"),
                    120
            ),
            new TitleRecommendation(
                    "RECEPISSE_ASILE", "Récépissé de demande d'asile", "FRANCE", "ASILE",
                    "Délivré dès l'enregistrement de la demande d'asile à l'OFPRA. Renouvelable pendant l'instruction.",
                    List.of("Passeport ou document de voyage (si disponible)", "Formulaire OFPRA complété", "Récit de vie", "Justificatif de domiciliation", "Photo d'identité"),
                    21
            ),

            // ========== BELGIQUE ==========

            new TitleRecommendation(
                    "CARTE_A_TRAVAIL", "Carte A — Séjour limité (travail)", "BELGIQUE", "TRAVAIL",
                    "Séjour limité lié à un permis unique de travail. Durée alignée sur le contrat de travail.",
                    List.of("Passeport valide", "Contrat de travail", "Demande de permis unique via l'employeur", "Diplômes ou qualifications", "Casier judiciaire du pays d'origine", "Certificat médical", "Photo d'identité"),
                    120
            ),
            new TitleRecommendation(
                    "CARTE_A_ETUDES", "Carte A — Séjour limité (études)", "BELGIQUE", "ETUDES",
                    "Pour les étudiants inscrits dans un établissement d'enseignement supérieur belge reconnu. Ressources : 679 €/mois.",
                    List.of("Passeport valide", "Lettre d'admission", "Preuve de paiement des frais d'inscription", "Justificatif de ressources (679 €/mois)", "Assurance maladie", "Certificat médical", "Photo d'identité"),
                    90
            ),
            new TitleRecommendation(
                    "CARTE_A_FAMILLE", "Carte A — Séjour limité (regroupement familial)", "BELGIQUE", "FAMILLE",
                    "Pour les membres de famille d'un ressortissant belge ou d'un étranger en séjour régulier.",
                    List.of("Passeport valide", "Preuve du lien familial (acte de mariage/naissance légalisé)", "Titre de séjour du regroupant", "Justificatif de ressources stables du regroupant", "Justificatif de logement", "Assurance maladie", "Photo d'identité"),
                    150
            ),
            new TitleRecommendation(
                    "CARTE_B", "Carte B — Séjour illimité", "BELGIQUE", "AUTRE",
                    "Après 5 ans de séjour régulier en Belgique avec carte A. Donne droit au séjour sans limitation de durée.",
                    List.of("Passeport valide", "Cartes A des 5 dernières années", "Justificatif de résidence ininterrompue", "Justificatif de ressources", "Preuve d'intégration sociale", "Photo d'identité"),
                    120
            ),
            new TitleRecommendation(
                    "CARTE_C", "Carte C — Établissement", "BELGIQUE", "AUTRE",
                    "Séjour permanent. Délivrée après la carte B ou dans certains cas spécifiques (réfugié reconnu).",
                    List.of("Passeport valide", "Carte B ou preuve de statut de réfugié", "Justificatif de résidence", "Photo d'identité"),
                    90
            ),
            new TitleRecommendation(
                    "PERMIS_UNIQUE", "Permis unique (travail + séjour)", "BELGIQUE", "TRAVAIL",
                    "Combine autorisation de travail et de séjour en un seul document. Demande introduite par l'employeur auprès de la Région.",
                    List.of("Passeport valide", "Contrat de travail", "Formulaire de demande de permis unique", "Diplômes ou qualifications", "Casier judiciaire du pays d'origine", "Certificat médical", "Photo d'identité"),
                    120
            ),
            new TitleRecommendation(
                    "ANNEXE_15", "Annexe 15 — Attestation de déclaration d'arrivée", "BELGIQUE", "AUTRE",
                    "Document provisoire délivré lors de l'enregistrement à la commune. Valable 3 mois, renouvelable.",
                    List.of("Passeport valide", "Justificatif de domicile en Belgique", "Photo d'identité"),
                    7
            ),
            new TitleRecommendation(
                    "ATTESTATION_IMMATRICULATION", "Attestation d'immatriculation (carte orange)", "BELGIQUE", "ASILE",
                    "Délivrée aux demandeurs de protection internationale pendant l'instruction par le CGRA.",
                    List.of("Passeport ou document de voyage (si disponible)", "Questionnaire CGRA complété", "Récit de vie détaillé", "Preuves à l'appui de la demande", "Photo d'identité"),
                    14
            )
    );

    /**
     * Retourne tous les titres pour un pays donné.
     */
    public static List<TitleRecommendation> getTitles(String country) {
        return ALL_TITLES.stream()
                .filter(t -> t.country().equals(country))
                .toList();
    }

    /**
     * Retourne tous les titres pour un pays et un motif donnés.
     */
    public static List<TitleRecommendation> getTitles(String country, String motif) {
        return ALL_TITLES.stream()
                .filter(t -> t.country().equals(country) && t.motif().equals(motif))
                .toList();
    }

    /**
     * Retourne un titre par son code.
     */
    public static TitleRecommendation getByCode(String code) {
        return ALL_TITLES.stream()
                .filter(t -> t.code().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retourne tous les titres du référentiel.
     */
    public static List<TitleRecommendation> getAll() {
        return ALL_TITLES;
    }

    public static boolean isCountryValid(String country) {
        return "FRANCE".equals(country) || "BELGIQUE".equals(country);
    }

    public static boolean isMotifValid(String motif) {
        return List.of("TRAVAIL", "ETUDES", "FAMILLE", "ASILE", "AUTRE").contains(motif);
    }
}
