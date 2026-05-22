package fr.ailegalcase.casefile;

/**
 * SF-216-07 : voie de recouvrement recommandée par l'outil ARIPA.
 *
 * <ul>
 *   <li>{@link #TITRE_REQUIS} — Aucun titre exécutoire (jugement / convention
 *       CM notariée / acte notarié). Impossible de recourir à l'ARIPA tant que
 *       le titre n'est pas obtenu (art. L. 581-2 CSS).</li>
 *   <li>{@link #SDR_ARIPA} — SDR (Service de Demande de Recouvrement) direct
 *       via la CAF/MSA — voie principale pour les créanciers d'aliments avec
 *       enfants mineurs à charge (art. L. 581-1 CSS).</li>
 *   <li>{@link #SAISIE_SUR_SALAIRE} — Procédure simplifiée de paiement direct
 *       quand le débiteur est salarié (art. L. 582-1 CSS).</li>
 *   <li>{@link #SATD} — Saisie administrative à tiers détenteur, applicable
 *       notamment quand le débiteur est employeur public ou perçoit des
 *       revenus mobiliers (art. L. 262 LPF).</li>
 *   <li>{@link #CONVENTION_INTERNATIONALE} — Débiteur résidant hors de France :
 *       Convention La Haye 23/11/2007 (États tiers) ou Règlement UE n°4/2009
 *       (États membres UE).</li>
 * </ul>
 */
public enum VoieRecouvrementAripaEnum {
    TITRE_REQUIS,
    SDR_ARIPA,
    SAISIE_SUR_SALAIRE,
    SATD,
    CONVENTION_INTERNATIONALE
}
