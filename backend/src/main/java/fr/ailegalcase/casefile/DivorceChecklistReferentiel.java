package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Référentiel statique des étapes et pièces pour divorce par consentement mutuel FR + BE.
 */
public final class DivorceChecklistReferentiel {

    private DivorceChecklistReferentiel() {}

    // ========== ÉTAPES ==========

    private static final List<DivorceEtape> ALL_ETAPES = List.of(
            // FRANCE
            new DivorceEtape("FR_CHOIX_AVOCATS", "Choix des avocats", "FRANCE", 1,
                    "Chaque époux doit avoir son propre avocat (obligatoire depuis 2017).", "—", true),
            new DivorceEtape("FR_REDACTION_CONVENTION", "Rédaction de la convention de divorce", "FRANCE", 2,
                    "Les avocats rédigent conjointement la convention réglant toutes les conséquences du divorce.", "2-4 semaines", true),
            new DivorceEtape("FR_ENVOI_LRAR", "Envoi de la convention par LRAR", "FRANCE", 3,
                    "Chaque avocat envoie le projet de convention à son client par lettre recommandée.", "—", true),
            new DivorceEtape("FR_DELAI_REFLEXION", "Délai de réflexion de 15 jours", "FRANCE", 4,
                    "Délai incompressible de 15 jours entre la réception du courrier et la signature.", "15 jours", true),
            new DivorceEtape("FR_SIGNATURE_CONVENTION", "Signature de la convention", "FRANCE", 5,
                    "Les deux époux et leurs avocats respectifs signent la convention.", "—", true),
            new DivorceEtape("FR_DEPOT_NOTAIRE", "Dépôt chez le notaire", "FRANCE", 6,
                    "L'un des avocats dépose la convention signée chez un notaire dans un délai de 7 jours.", "7 jours après signature", true),
            new DivorceEtape("FR_ENREGISTREMENT", "Enregistrement et transcription", "FRANCE", 7,
                    "Le notaire enregistre la convention et la transmet à l'état civil pour transcription.", "15 jours", true),

            // BELGIQUE
            new DivorceEtape("BE_CHOIX_AVOCAT", "Choix de l'avocat ou notaire", "BELGIQUE", 1,
                    "Les époux peuvent recourir à un seul avocat commun ou à un notaire médiateur.", "—", true),
            new DivorceEtape("BE_REDACTION_CONVENTION", "Rédaction de la convention préalable", "BELGIQUE", 2,
                    "Convention réglant toutes les conséquences : enfants, biens, pension, logement.", "2-6 semaines", true),
            new DivorceEtape("BE_REQUETE_CONJOINTE", "Dépôt de la requête conjointe", "BELGIQUE", 3,
                    "Dépôt de la requête conjointe au greffe du tribunal de la famille.", "—", true),
            new DivorceEtape("BE_COMPARUTION", "Comparution devant le juge", "BELGIQUE", 4,
                    "Les époux comparaissent devant le juge de la famille qui vérifie le consentement.", "1-3 mois après dépôt", true),
            new DivorceEtape("BE_JUGEMENT", "Prononcé du jugement", "BELGIQUE", 5,
                    "Le juge prononce le divorce et homologue la convention.", "À l'audience ou sous 1 mois", true),
            new DivorceEtape("BE_TRANSCRIPTION", "Transcription à l'état civil", "BELGIQUE", 6,
                    "Le greffe transmet le jugement à l'officier de l'état civil pour transcription.", "1 mois", true)
    );

    // ========== PIÈCES ==========

    private static final List<DivorcePiece> ALL_PIECES = List.of(
            // FRANCE
            new DivorcePiece("FR_ACTE_MARIAGE", "Copie intégrale de l'acte de mariage", "FRANCE", "Moins de 3 mois", true),
            new DivorcePiece("FR_ACTE_NAISSANCE_EPOUX", "Actes de naissance des époux", "FRANCE", "Moins de 3 mois", true),
            new DivorcePiece("FR_ACTE_NAISSANCE_ENFANTS", "Actes de naissance des enfants", "FRANCE", "Moins de 3 mois", true),
            new DivorcePiece("FR_LIVRET_FAMILLE", "Copie du livret de famille", "FRANCE", "—", true),
            new DivorcePiece("FR_JUSTIF_DOMICILE", "Justificatifs de domicile des deux époux", "FRANCE", "Moins de 3 mois", true),
            new DivorcePiece("FR_CONTRAT_MARIAGE", "Contrat de mariage (si existant)", "FRANCE", "—", false),
            new DivorcePiece("FR_ETAT_PATRIMOINE", "État liquidatif du patrimoine", "FRANCE", "Notaire si biens immobiliers", true),
            new DivorcePiece("FR_JUSTIF_REVENUS", "Justificatifs de revenus (avis d'imposition, bulletins de salaire)", "FRANCE", "—", true),
            new DivorcePiece("FR_PIECE_IDENTITE", "Pièces d'identité des deux époux", "FRANCE", "—", true),

            // BELGIQUE
            new DivorcePiece("BE_ACTE_MARIAGE", "Copie de l'acte de mariage", "BELGIQUE", "—", true),
            new DivorcePiece("BE_ACTE_NAISSANCE_EPOUX", "Actes de naissance des époux", "BELGIQUE", "—", true),
            new DivorcePiece("BE_ACTE_NAISSANCE_ENFANTS", "Actes de naissance des enfants", "BELGIQUE", "—", true),
            new DivorcePiece("BE_COMPOSITION_MENAGE", "Certificat de composition de ménage", "BELGIQUE", "Commune de résidence", true),
            new DivorcePiece("BE_CONTRAT_MARIAGE", "Contrat de mariage (si existant)", "BELGIQUE", "—", false),
            new DivorcePiece("BE_CONVENTION_PREALABLE", "Convention préalable signée", "BELGIQUE", "Réglant toutes les conséquences", true),
            new DivorcePiece("BE_JUSTIF_REVENUS", "Justificatifs de revenus", "BELGIQUE", "—", true),
            new DivorcePiece("BE_PIECE_IDENTITE", "Pièces d'identité des deux époux", "BELGIQUE", "—", true)
    );

    public static List<DivorceEtape> getEtapes(String country) {
        return ALL_ETAPES.stream().filter(e -> e.country().equals(country)).toList();
    }

    public static List<DivorcePiece> getPieces(String country) {
        return ALL_PIECES.stream().filter(p -> p.country().equals(country)).toList();
    }

    public static List<DivorceEtape> getAllEtapes() { return ALL_ETAPES; }
    public static List<DivorcePiece> getAllPieces() { return ALL_PIECES; }

    public static boolean isCountryValid(String country) {
        return "FRANCE".equals(country) || "BELGIQUE".equals(country);
    }

    public static String getBaseJuridique(String country) {
        return "FRANCE".equals(country)
                ? "Articles 229-1 à 229-4 du Code civil (loi du 18 novembre 2016)"
                : "Articles 1287 à 1304/1 du Code judiciaire belge";
    }
}
