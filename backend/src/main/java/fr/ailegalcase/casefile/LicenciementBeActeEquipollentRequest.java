package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-09 : payload de création / mise à jour de l'analyse de l'acte
 * équipollent à rupture BE — Loi 03/07/1978 art. 20 + Cass. BE 23/12/1957.
 *
 * <p>Outil <b>BE-only</b>. Le concept belge est distinct du régime FR
 * (prise d'acte L. 1237-19 C. trav. FR + résiliation judiciaire) — outil
 * non exposé côté France.</p>
 *
 * <h2>Logique métier</h2>
 * <p>L'outil détermine si une modification unilatérale du contrat par
 * l'employeur peut être qualifiée d'acte équipollent à rupture (rupture
 * imputable à l'employeur). Le verdict combine la nature de la
 * modification, son ampleur, son caractère essentiel et la réactivité
 * du salarié (risque d'acceptation tacite si silence > 30 j).</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code typeModification} — nature de l'élément modifié.</li>
 *   <li>{@code ampleurModification} — degré de la modification (pilote le
 *       verdict).</li>
 *   <li>{@code dateModification} — date à laquelle l'employeur a notifié
 *       ou imposé la modification (ancre le calcul du délai d'acceptation
 *       tacite).</li>
 * </ul>
 *
 * <h2>Champs optionnels</h2>
 * <ul>
 *   <li>{@code elementEssentielDuContrat} — défaut {@code false}. Doit être
 *       {@code true} (combiné à {@code SUBSTANTIELLE}) pour atteindre le
 *       verdict {@code ACTE_EQUIPOLLENT_PROBABLE}.</li>
 *   <li>{@code salarieAProteste} — défaut {@code false}. Si le salarié a
 *       protesté formellement la modification.</li>
 *   <li>{@code delaiDepuisModificationJours} — calculé à partir de
 *       {@code dateModification} si non fourni (today − dateModification).
 *       Pilote le déclenchement de l'avertissement
 *       {@code RISQUE_ACCEPTATION_TACITE}.</li>
 *   <li>{@code remunerationHebdomadaireBrute} — pour calcul de l'ICP
 *       indicatif (si {@code ACTE_EQUIPOLLENT_PROBABLE}).</li>
 *   <li>{@code dureePreavisCalculeeSemaines} — durée de préavis déjà
 *       calculée par ailleurs (outil statut unique SF-213-03 ou formule
 *       Claeys SF-213-04) — pour boucler le calcul ICP indicatif.</li>
 * </ul>
 */
public record LicenciementBeActeEquipollentRequest(

        @NotNull(message = "typeModification est requis")
        TypeModificationUnilateraleEnum typeModification,

        @NotNull(message = "ampleurModification est requis")
        AmpleurModificationEnum ampleurModification,

        @NotNull(message = "dateModification est requise")
        LocalDate dateModification,

        boolean elementEssentielDuContrat,

        boolean salarieAProteste,

        Integer delaiDepuisModificationJours,

        BigDecimal remunerationHebdomadaireBrute,

        Integer dureePreavisCalculeeSemaines
) {
}
