package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * SF-219-11 : payload de création / mise à jour de l'analyse
 * <i>congé-éducation payé régionalisé</i> (Loi 22/01/1985 régionalisée
 * 2014 — WBR / FLA / BXL).
 *
 * <h2>Logique métier — régimes régionaux applicables</h2>
 *
 * <h3>Région du lieu de travail (gate régional)</h3>
 * <ul>
 *   <li><b>WALLONIE</b> — Décret WBR 19/02/2014 + AGW 19/12/2014.
 *       Plafond annuel 120 h (formation générale) ; 180 h (formation
 *       professionnelle qualifiante).</li>
 *   <li><b>FLANDRE</b> — Décret 12/12/2014 (Vlaams Opleidingsverlof
 *       VOV, en vigueur 01/09/2019). Plafond annuel 125 h
 *       (général / professionnel) ; 250 h (études supérieures en
 *       alternance).</li>
 *   <li><b>BRUXELLES-CAPITALE</b> — Ordonnance 02/07/2015 + AGBR
 *       14/04/2016. Plafond annuel 80 h (formation générale) ;
 *       100 h (formation professionnelle qualifiante).</li>
 * </ul>
 *
 * <h3>Conditions cumulatives (toutes régions)</h3>
 * <ul>
 *   <li>Travailleur du secteur privé sous contrat de travail
 *       (CDD/CDI/intérim assimilés selon régimes régionaux).</li>
 *   <li>Formation inscrite à la <b>liste agréée régionale</b> tenue
 *       par le ministère régional de l'emploi / formation.</li>
 *   <li>Occupation ≥ <b>mi-temps</b> en règle générale (FLA prévoit
 *       une dérogation pour publics fragilisés peu qualifiés).</li>
 * </ul>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code region} — région du lieu de travail (WALLONIE / FLANDRE /
 *       BRUXELLES).</li>
 *   <li>{@code typeFormation} — typologie de formation suivie.</li>
 *   <li>{@code heuresFormationAnnuelles} — nombre d'heures de cours
 *       suivies par an (cours en présentiel + assimilés).</li>
 *   <li>{@code tauxOccupation} — taux d'occupation contractuel
 *       (1.0 = temps plein, 0.5 = mi-temps).</li>
 *   <li>{@code publicFragilise} — true si travailleur peu qualifié
 *       au sens FLA (déclenche dérogation occupation insuffisante en
 *       Flandre uniquement).</li>
 *   <li>{@code formationAgreeeRegion} — true si formation présente
 *       dans la liste agréée régionale.</li>
 * </ul>
 */
public record CongeEducationPayeRegionRequest(

        @NotNull(message = "region est requise")
        CongeEducationPayeRegionRegion region,

        @NotNull(message = "typeFormation est requis")
        CongeEducationPayeRegionTypeFormation typeFormation,

        @NotNull(message = "heuresFormationAnnuelles est requis")
        @Min(value = 0, message = "heuresFormationAnnuelles doit être positif ou nul")
        Integer heuresFormationAnnuelles,

        @NotNull(message = "tauxOccupation est requis")
        @DecimalMin(value = "0.0", message = "tauxOccupation doit être positif ou nul")
        @DecimalMax(value = "1.0", message = "tauxOccupation doit être au plus 1.0")
        BigDecimal tauxOccupation,

        @NotNull(message = "publicFragilise est requis")
        Boolean publicFragilise,

        @NotNull(message = "formationAgreeeRegion est requis")
        Boolean formationAgreeeRegion
) {
}
