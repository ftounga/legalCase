package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-216-27 : résultat du calcul Partage successoral notarié FR
 * (art. 816 et s. Cciv + art. 870 Cciv + art. 1592 CGI + art. 641 CGI +
 * art. 840 Cciv).
 *
 * <ul>
 *   <li>{@code notaireObligatoire} — true si la présence d'un immeuble
 *       dans la succession impose le recours au notaire (art. 1592 CGI).
 *       Sinon, recours simplement recommandé pour sécuriser l'acte.</li>
 *   <li>{@code calendrierEtapes} — 5 étapes : désignation notaire, bilan
 *       patrimonial, attestation après décès (immeubles), projet de
 *       partage, signature de l'acte de partage.</li>
 *   <li>{@code delaiDeclarationFiscale} — échéance fiscale calculée
 *       depuis la date d'ouverture (+ 6 mois — art. 641 CGI).</li>
 *   <li>{@code alerteDelai} — true si la date du jour dépasse
 *       {@code delaiDeclarationFiscale}.</li>
 *   <li>{@code orientationJudiciaire} — true si la procédure amiable
 *       n'est pas tenable (désaccord persistant) → renvoi vers
 *       F-FA-17-partage-judiciaire (art. 840 Cciv).</li>
 *   <li>{@code baseLegale} — articles applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record PartageNotarialResult(
        boolean notaireObligatoire,
        List<String> calendrierEtapes,
        LocalDate delaiDeclarationFiscale,
        boolean alerteDelai,
        boolean orientationJudiciaire,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
