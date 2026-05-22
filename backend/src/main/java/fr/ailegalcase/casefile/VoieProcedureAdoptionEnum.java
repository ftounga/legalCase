package fr.ailegalcase.casefile;

/**
 * SF-216-17 : voie procédurale d'une adoption internationale (FRANCE).
 *
 * <ul>
 *   <li>{@link #OAA_AGREE} — Organisme Autorisé pour l'Adoption (loi
 *       n°2001-111 du 6/2/2001). Voie privilégiée vers les États signataires
 *       de la Convention La Haye 1993.</li>
 *   <li>{@link #VOIE_AUTONOME} — démarche individuelle de l'adoptant sans
 *       intermédiation OAA. Possible hors convention, mais expose à un
 *       risque de refus de transcription en France (art. 370-3 al. 2
 *       Cciv).</li>
 *   <li>{@link #ORGANISME_ETAT} — opérateur public (AFA — Agence Française
 *       de l'Adoption, fusionnée avec le GIP enfance en danger en 2019,
 *       voie résiduelle pour certains pays).</li>
 * </ul>
 */
public enum VoieProcedureAdoptionEnum {
    OAA_AGREE,
    VOIE_AUTONOME,
    ORGANISME_ETAT
}
