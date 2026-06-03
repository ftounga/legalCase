package fr.ailegalcase.casefile;

/**
 * SF-218-43 : base de calcul de la durée du congé pour évènement familial retenue
 * (art. L.3142-1 à L.3142-5 CT, F-DT-76). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>LEGALE : la durée légale minimale (L.3142-4) est retenue (aucune durée
 *       conventionnelle plus favorable, ou durée conventionnelle inférieure ou
 *       égale à la durée légale).</li>
 *   <li>CONVENTIONNELLE : la durée conventionnelle (CCN), plus favorable que la
 *       durée légale, est retenue (L.3142-5).</li>
 * </ul>
 */
public enum CongesEvenementsFamiliauxBase {
    LEGALE,
    CONVENTIONNELLE
}
