package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-08 : payload de création / mise à jour de l'analyse de la protection
 * d'un délégué syndical / candidat non élu BE en cas de licenciement.
 *
 * <p>Outil <b>BE-only</b> — Loi du 19 mars 1991 (protection des représentants
 * CE/CPPT) + CCT n° 5 du 24 mai 1971 (statut des délégations syndicales).
 * La France a un dispositif distinct (statut protégé délégué / CSE /
 * conseiller prud'homme — L. 2411-1 et s. C. trav.).</p>
 *
 * <h2>Logique métier</h2>
 * <p>L'outil détermine si le licenciement tombe dans la <b>période de
 * protection</b> calculée à partir de {@code dateElectionOuMandat} et du
 * statut. Si oui, il calcule l'<b>indemnité forfaitaire</b> (2 ans de
 * rémunération brute, ou 4 ans si circonstances aggravantes / récidive) et
 * le <b>délai de 30 jours</b> pour demander la réintégration.</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code statutProtege} — pilote la durée de la fenêtre de
 *       protection.</li>
 *   <li>{@code ancienneteAnnees} — ancienneté totale ; doit être ≥ 0.</li>
 *   <li>{@code remunerationAnnuelleBrute} — base du calcul de l'indemnité ;
 *       doit être &gt; 0.</li>
 *   <li>{@code dateLicenciement} — date de notification du licenciement.</li>
 *   <li>{@code dateElectionOuMandat} — date d'élection (ou début de mandat)
 *       qui ancre la fenêtre de protection.</li>
 * </ul>
 *
 * <h2>Conditionnels / cohérence</h2>
 * <ul>
 *   <li>{@code demandeReintegrationDansTrente} — défaut {@code false}. Si
 *       le délégué a réellement demandé la réintégration dans les 30j.</li>
 *   <li>{@code employeurRefuseReintegration} — défaut {@code false}.
 *       <b>Requis</b> à {@code true} uniquement si
 *       {@code demandeReintegrationDansTrente=true} (sinon 400 cohérence
 *       côté Validator).</li>
 *   <li>{@code circonstancesAggravantes} — défaut {@code false}. Active le
 *       barème de 4 ans (récidive ou licenciement collectif de
 *       représentants).</li>
 * </ul>
 */
public record LicenciementBeProtectionDelegueeRequest(

        @NotNull(message = "statutProtege est requis")
        LicenciementBeStatutProtegeEnum statutProtege,

        @NotNull(message = "ancienneteAnnees est requise")
        Integer ancienneteAnnees,

        @NotNull(message = "remunerationAnnuelleBrute est requise")
        BigDecimal remunerationAnnuelleBrute,

        @NotNull(message = "dateLicenciement est requise")
        LocalDate dateLicenciement,

        @NotNull(message = "dateElectionOuMandat est requise")
        LocalDate dateElectionOuMandat,

        boolean demandeReintegrationDansTrente,

        boolean employeurRefuseReintegration,

        boolean circonstancesAggravantes
) {
}
