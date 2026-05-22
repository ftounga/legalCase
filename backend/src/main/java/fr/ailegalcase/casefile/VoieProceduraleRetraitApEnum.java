package fr.ailegalcase.casefile;

/**
 * SF-216-11 : voie procédurale effectivement applicable pour mener à bien
 * le retrait d'autorité parentale envisagé (art. 378-381 Cciv).
 *
 * <ul>
 *   <li>{@link #JURIDICTION_PENALE_ACCESSOIRE} — retrait accessoire prononcé
 *       par la juridiction de jugement pénale en cas de condamnation pour
 *       crime ou délit sur l'enfant (art. 378 al. 1 Cciv).</li>
 *   <li>{@link #JAF_TRIBUNAL_JUDICIAIRE} — voie civile : requête au tribunal
 *       judiciaire (JAF), demande aux fins de retrait total ou partiel
 *       (art. 378-1 Cciv).</li>
 *   <li>{@link #PROCUREUR_REPUBLIQUE_ASSISTANCE_EDUCATIVE} — saisine du
 *       Procureur de la République pour le déclenchement d'une mesure
 *       d'assistance éducative + procédure parallèle de retrait (cas
 *       d'urgence + danger immédiat).</li>
 *   <li>{@link #LMVSS_2022_SUSPENSION_AUTOMATIQUE} — suspension automatique
 *       de l'AP prévue par la loi 2022-140 + saisine accélérée du JAF /
 *       juridiction pénale pour le retrait.</li>
 *   <li>{@link #SANS_OBJET} — pas de voie procédurale (cas d'irrecevabilité,
 *       ex. enfant majeur).</li>
 * </ul>
 */
public enum VoieProceduraleRetraitApEnum {
    JURIDICTION_PENALE_ACCESSOIRE,
    JAF_TRIBUNAL_JUDICIAIRE,
    PROCUREUR_REPUBLIQUE_ASSISTANCE_EDUCATIVE,
    LMVSS_2022_SUSPENSION_AUTOMATIQUE,
    SANS_OBJET
}
