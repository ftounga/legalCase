package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SF-213-10 : échelon CCT n° 109 du 12 février 2014 — score d'indemnisation
 * du licenciement manifestement déraisonnable (art. 9).
 *
 * <p>Outil <b>BE-only</b>. Le sérialisateur JSON émet les valeurs telles
 * qu'attendues par le frontend ({@code "3_SEMAINES"}, {@code "8_SEMAINES"},
 * etc.) — les noms Java ({@link #TROIS_SEMAINES} etc.) sont préfixés par
 * leur lettre pour rester des identifiants valides.</p>
 *
 * <h2>Échelle</h2>
 * <ul>
 *   <li>{@link #NON_DERAISONNABLE} — motif valable + procédures respectées :
 *       pas d'indemnité CCT 109 due.</li>
 *   <li>{@link #TROIS_SEMAINES} — minimum légal (sans motif valable mais pas
 *       manifestement déraisonnable).</li>
 *   <li>{@link #HUIT_SEMAINES} — licenciement manifestement déraisonnable de
 *       gravité ordinaire.</li>
 *   <li>{@link #DOUZE_SEMAINES} — licenciement manifestement déraisonnable
 *       de gravité haute (discrimination ou représailles suspectées).</li>
 *   <li>{@link #DIX_SEPT_SEMAINES} — maximum légal (faits graves :
 *       discrimination + représailles cumulés).</li>
 * </ul>
 */
public enum LicenciementBeCct109EchelonEnum {

    @JsonProperty("NON_DERAISONNABLE")
    NON_DERAISONNABLE,

    @JsonProperty("3_SEMAINES")
    TROIS_SEMAINES,

    @JsonProperty("8_SEMAINES")
    HUIT_SEMAINES,

    @JsonProperty("12_SEMAINES")
    DOUZE_SEMAINES,

    @JsonProperty("17_SEMAINES")
    DIX_SEPT_SEMAINES
}
