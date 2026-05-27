package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-213-07 : payload de création / mise à jour de l'analyse de procédure
 * interne BE de plainte pour harcèlement moral / sexuel.
 *
 * <p>Outil <b>BE-only</b> — Loi du 4 août 1996 art. 32bis–32sexies + AR du
 * 10 avril 2014 relatif à la prévention des risques psychosociaux au
 * travail. La France a un dispositif distinct (référent harcèlement,
 * médecin du travail) avec d'autres procédures.</p>
 *
 * <p>Outil <b>complémentaire</b> de {@code F-DT-11} (qui couvre la nullité
 * du licenciement représailles BE) — {@code SF-213-07} intervient
 * <i>avant</i> toute rupture pour guider la procédure interne (CPAP,
 * demande informelle/formelle, délais 90j enquête, protection 12 mois).</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code typeHarcelement} : {@link HarcelementBeTypeEnum} — MORAL,
 *       SEXUEL, LES_DEUX.</li>
 *   <li>{@code etapeProcedure} : {@link HarcelementBeEtapeEnum} — 5 étapes
 *       du cycle.</li>
 *   <li>{@code entrepriseTaille} :
 *       {@link HarcelementBeTailleEntrepriseEnum} — pilote l'avertissement
 *       CPAP absent.</li>
 * </ul>
 *
 * <h2>Conditionnels / optionnels</h2>
 * <ul>
 *   <li>{@code dateDepotPlainte} : <b>requis si</b> {@code etapeProcedure
 *       != AVANT_DEPOT} (sinon 400). Permet de calculer le délai fatal
 *       90 j (DEMANDE_FORMELLE_EN_COURS) et la fenêtre de protection
 *       12 mois (art. 32sexies).</li>
 *   <li>{@code entreprisePossedeCPAP} : flag — déclenche un avertissement
 *       spécifique selon la taille d'entreprise.</li>
 *   <li>{@code mesureDefavorableApres} : flag — défaut {@code false}.
 *       Si {@code true} avec date de dépôt, déclenche
 *       {@code representaillesPossibles=true}.</li>
 *   <li>{@code delaiDepuisDepotJours} : optionnel — sert au calcul du
 *       dépassement éventuel de la fenêtre de protection 12 mois (au-delà
 *       de 365 j, l'avertissement « présomption affaiblie » est posé).</li>
 * </ul>
 */
public record HarcelementBeProcedureFormelleRequest(

        @NotNull(message = "typeHarcelement est requis")
        HarcelementBeTypeEnum typeHarcelement,

        @NotNull(message = "etapeProcedure est requis")
        HarcelementBeEtapeEnum etapeProcedure,

        /**
         * Requis si {@code etapeProcedure != AVANT_DEPOT}. Validation
         * conditionnelle côté {@link HarcelementBeProcedureFormelleProcedureChecker}.
         */
        LocalDate dateDepotPlainte,

        boolean entreprisePossedeCPAP,

        @NotNull(message = "entrepriseTaille est requis")
        HarcelementBeTailleEntrepriseEnum entrepriseTaille,

        /**
         * Défaut {@code false} — mesure défavorable observée après dépôt
         * (rétrogradation, sanction, modification substantielle des
         * conditions de travail, etc.). Si {@code true} avec date de
         * dépôt fournie, déclenche {@code representaillesPossibles=true}
         * (art. 32sexies Loi 04/08/1996).
         */
        boolean mesureDefavorableApres,

        /**
         * Optionnel — nombre de jours depuis le dépôt de plainte. Sert à
         * détecter le dépassement de la fenêtre de protection 12 mois
         * (au-delà de 365 j, présomption de représailles affaiblie).
         */
        Integer delaiDepuisDepotJours
) {
}
