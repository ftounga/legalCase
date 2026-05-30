package fr.ailegalcase.casefile;

/**
 * SF-218-05 : cas d'ouverture d'un pourvoi en cassation devant la chambre
 * sociale de la Cour de cassation (art. 604 CPC — le pourvoi tend à censurer la
 * non-conformité de l'arrêt aux règles de droit). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Chaque cas est affecté d'une force probatoire (FORTE / MOYENNE / FAIBLE)
 * qui pondère le risque de non-admission (filtre NPC, art. 1014 CPC) :
 * <ul>
 *   <li>VIOLATION_LOI — violation de la loi : moyen de pur droit, contrôle
 *       plein de la Cour, force FORTE ;</li>
 *   <li>DEFAUT_BASE_LEGALE — défaut de base légale : motifs insuffisants pour
 *       permettre le contrôle de la qualification, force FORTE ;</li>
 *   <li>MANQUE_DE_BASE — manque de base légale (motifs incomplets), force
 *       MOYENNE ;</li>
 *   <li>CONTRADICTION_MOTIFS — contradiction de motifs (équivaut à un défaut de
 *       motifs), force MOYENNE ;</li>
 *   <li>DENATURATION — dénaturation d'un écrit clair et précis : contrôle
 *       restreint, force MOYENNE ;</li>
 *   <li>PERTE_FONDEMENT_JURIDIQUE — perte de fondement juridique (revirement de
 *       jurisprudence / abrogation), force MOYENNE ;</li>
 *   <li>VICE_FORME — vice de forme de l'arrêt : moyen fragile, souvent
 *       neutralisé, force FAIBLE.</li>
 * </ul>
 */
public enum PourvoiCassationSocCasOuverture {
    VIOLATION_LOI(PourvoiCassationSocForce.FORTE),
    DEFAUT_BASE_LEGALE(PourvoiCassationSocForce.FORTE),
    MANQUE_DE_BASE(PourvoiCassationSocForce.MOYENNE),
    CONTRADICTION_MOTIFS(PourvoiCassationSocForce.MOYENNE),
    DENATURATION(PourvoiCassationSocForce.MOYENNE),
    PERTE_FONDEMENT_JURIDIQUE(PourvoiCassationSocForce.MOYENNE),
    VICE_FORME(PourvoiCassationSocForce.FAIBLE);

    private final PourvoiCassationSocForce force;

    PourvoiCassationSocCasOuverture(PourvoiCassationSocForce force) {
        this.force = force;
    }

    public PourvoiCassationSocForce force() {
        return force;
    }

    public String libelle() {
        return switch (this) {
            case VIOLATION_LOI -> "Violation de la loi";
            case DEFAUT_BASE_LEGALE -> "Défaut de base légale";
            case MANQUE_DE_BASE -> "Manque de base légale";
            case CONTRADICTION_MOTIFS -> "Contradiction de motifs";
            case DENATURATION -> "Dénaturation d'un écrit clair et précis";
            case PERTE_FONDEMENT_JURIDIQUE -> "Perte de fondement juridique";
            case VICE_FORME -> "Vice de forme de l'arrêt";
        };
    }

    public String baseJuridique() {
        return switch (this) {
            case VIOLATION_LOI -> "art. 604 CPC (non-conformité aux règles de droit)";
            case DEFAUT_BASE_LEGALE -> "art. 604 CPC ; contrôle de la motivation suffisante";
            case MANQUE_DE_BASE -> "art. 604 CPC ; insuffisance de motifs de fait";
            case CONTRADICTION_MOTIFS -> "art. 455 CPC (motivation) ; art. 604 CPC";
            case DENATURATION -> "principe de l'interdiction de dénaturer les écrits ; art. 604 CPC";
            case PERTE_FONDEMENT_JURIDIQUE -> "art. 604 CPC (perte de fondement juridique de l'arrêt)";
            case VICE_FORME -> "art. 455 et 458 CPC (mentions et forme de l'arrêt)";
        };
    }
}
