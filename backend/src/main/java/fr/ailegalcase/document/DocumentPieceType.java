package fr.ailegalcase.document;

import java.util.EnumSet;
import java.util.Set;

/**
 * SF-145-01 + SF-145-09 : types contraints de pièces identifiables dans un
 * document composite. Enum unifié couvrant les 3 domaines métier V1.
 * Liste fermée — toute valeur non mappée remonte en AUTRE (fail-open parseur).
 *
 * <p>La méthode {@link #applicableFor(String)} retourne le sous-ensemble
 * pertinent pour un {@code workspace.legalDomain} donné. Utilisé par
 * {@code DocumentPieceDetectionService} pour n'exposer que les types
 * cohérents au domaine dans le prompt Sonnet (éviter BULLETIN_PAIE pour
 * un workspace immigration).
 */
public enum DocumentPieceType {
    // ── Types communs aux 3 domaines ────────────────────────────────────
    PHOTO,
    LETTRE,
    EMAIL,
    SMS,
    ATTESTATION,
    PIECE_IDENTITE,
    CERTIFICAT_MEDICAL,
    AUTRE,

    // ── Droit du travail ────────────────────────────────────────────────
    CONTRAT,
    BULLETIN_PAIE,

    // ── Immigration (SF-145-09) ─────────────────────────────────────────
    TITRE_DE_SEJOUR,
    PASSEPORT,
    VISA,
    ACTE_NAISSANCE,
    AVIS_IMPOSITION,
    QUITTANCE_LOYER,
    PROMESSE_EMBAUCHE,
    RECEPISSE_PREFECTURE,
    DECISION_OQTF,
    RECOURS_CONTENTIEUX,
    ATTESTATION_HEBERGEMENT,

    // ── Famille (SF-145-09) ─────────────────────────────────────────────
    ACTE_MARIAGE,
    ACTE_NAISSANCE_ENFANT,
    JUGEMENT_DIVORCE,
    LIVRET_FAMILLE,
    JUSTIFICATIF_REVENUS,

    // ── Commun 3 domaines (SF-145-10) ───────────────────────────────────
    /** Contrat de bail de location (distinct de QUITTANCE_LOYER qui prouve le paiement). */
    BAIL_LOCATION;

    private static final Set<DocumentPieceType> COMMON = EnumSet.of(
            PHOTO, LETTRE, EMAIL, SMS, ATTESTATION, PIECE_IDENTITE, CERTIFICAT_MEDICAL, AUTRE,
            // SF-145-10 : BAIL_LOCATION commun aux 3 domaines (justif domicile travail,
            // communauté de vie immigration L.423-1, logement familial famille).
            BAIL_LOCATION);

    private static final Set<DocumentPieceType> DROIT_DU_TRAVAIL_TYPES = EnumSet.of(
            CONTRAT, BULLETIN_PAIE, JUSTIFICATIF_REVENUS);

    private static final Set<DocumentPieceType> IMMIGRATION_TYPES = EnumSet.of(
            TITRE_DE_SEJOUR, PASSEPORT, VISA, ACTE_NAISSANCE, AVIS_IMPOSITION,
            QUITTANCE_LOYER, PROMESSE_EMBAUCHE, RECEPISSE_PREFECTURE, DECISION_OQTF,
            RECOURS_CONTENTIEUX, ATTESTATION_HEBERGEMENT,
            // SF-145-10 : ACTE_MARIAGE pertinent en immigration pour L.423-1
            // (conjoint de Français) et L.434-7 (regroupement familial) — auparavant
            // seul ACTE_NAISSANCE était proposé à Sonnet, d'où la confusion IA.
            ACTE_MARIAGE);

    private static final Set<DocumentPieceType> FAMILLE_TYPES = EnumSet.of(
            ACTE_MARIAGE, ACTE_NAISSANCE_ENFANT, JUGEMENT_DIVORCE, LIVRET_FAMILLE,
            JUSTIFICATIF_REVENUS, ACTE_NAISSANCE);

    public static DocumentPieceType fromStringOrDefault(String raw) {
        if (raw == null) return AUTRE;
        try {
            return DocumentPieceType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTRE;
        }
    }

    /**
     * SF-145-09 : retourne les types pertinents pour un legalDomain donné
     * (commons + types spécifiques au domaine). Si {@code legalDomain} est null
     * ou non reconnu, retourne tous les types (pas de filtrage).
     */
    public static Set<DocumentPieceType> applicableFor(String legalDomain) {
        EnumSet<DocumentPieceType> out = EnumSet.copyOf(COMMON);
        if (legalDomain == null) {
            out.addAll(EnumSet.allOf(DocumentPieceType.class));
            return out;
        }
        switch (legalDomain) {
            case "DROIT_DU_TRAVAIL" -> out.addAll(DROIT_DU_TRAVAIL_TYPES);
            case "DROIT_IMMIGRATION" -> out.addAll(IMMIGRATION_TYPES);
            case "DROIT_FAMILLE" -> out.addAll(FAMILLE_TYPES);
            default -> out.addAll(EnumSet.allOf(DocumentPieceType.class));
        }
        return out;
    }
}
