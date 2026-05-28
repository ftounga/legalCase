package fr.ailegalcase.casefile;

/**
 * SF-219-10 : verdict de l'outil <i>statut du délégué syndical</i>
 * (CCT n° 5 du 24/05/1971 — statut des délégations syndicales du
 * personnel des entreprises, conclue au CNT).
 *
 * <p>L'outil restitue l'éligibilité d'un travailleur au statut de
 * délégué syndical, les conditions de désignation, les missions
 * exerçables (négociation CCT d'entreprise, traitement des plaintes
 * individuelles et collectives, information du personnel, information /
 * consultation conjointe avec le CE / CPPT s'ils n'existent pas), et
 * la durée du mandat. Le volet « protection contre le licenciement »
 * (Loi 19/03/1991) est couvert par un outil distinct (SF-213-08).</p>
 *
 * <ul>
 *   <li>{@link #STATUT_RECONNU} — entreprise dans le champ (seuil
 *       sectoriel + syndicalisation requise), désignation régulière
 *       par une OS représentative, notification à l'employeur faite —
 *       statut pleinement reconnu, missions exerçables.</li>
 *   <li>{@link #STATUT_FRAGILE_NOTIFICATION_MANQUANTE} — désignation
 *       régulière par OS mais notification écrite à l'employeur
 *       manquante — statut formellement défaillant, à régulariser.</li>
 *   <li>{@link #INELIGIBLE_ENTREPRISE_HORS_CHAMP} — entreprise sous le
 *       seuil sectoriel ou sans présence syndicale suffisante — la CCT
 *       n° 5 ne s'applique pas (régime sectoriel ou CCT d'entreprise
 *       propre éventuelle).</li>
 *   <li>{@link #INELIGIBLE_PAS_DESIGNE_PAR_OS} — pas de désignation
 *       par une OS représentative (auto-proclamation, élection
 *       interne) — pas de statut CCT 5.</li>
 *   <li>{@link #A_ANALYSER} — désignation contestée ou ambiguïté sur
 *       l'applicabilité sectorielle — analyse juridique requise
 *       (avocat BE).</li>
 * </ul>
 */
public enum DelegueSyndicalCct5Verdict {
    /** Statut DS pleinement reconnu — missions CCT 5 exerçables. */
    STATUT_RECONNU,

    /** Désignation OK mais notification employeur manquante — statut fragile. */
    STATUT_FRAGILE_NOTIFICATION_MANQUANTE,

    /** Entreprise hors champ (sous seuil sectoriel ou sans présence OS). */
    INELIGIBLE_ENTREPRISE_HORS_CHAMP,

    /** Pas de désignation par une OS représentative — pas de statut CCT 5. */
    INELIGIBLE_PAS_DESIGNE_PAR_OS,

    /** Désignation contestée / ambiguïté sectorielle — avocat BE requis. */
    A_ANALYSER
}
