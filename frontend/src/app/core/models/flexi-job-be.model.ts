/**
 * SF-219-12b : modèle frontend de l'outil F-219 — Flexi-job (Belgique).
 *
 * <p><b>BE uniquement</b> — Loi-programme du 26/12/2013 (art. 13 à 28)
 * + Loi 25/04/2014 + Loi-programme 16/11/2015 (extension boulangerie /
 * coiffure) + Loi-programme 30/10/2018 (extension commerce de détail /
 * agriculture / soins de santé + suppression plafond pensionnés) +
 * Loi-programme 28/12/2023 + AR 02/06/2024 (extension secteurs publics
 * limités, sport, culture, événementiel, garages, transport autocar).</p>
 *
 * <p>La France n'a pas d'équivalent direct au régime flexi-job
 * (CDD d'usage L. 1242-2 § 3 et CDI intérimaire L. 1251-58-1 partagent
 * une logique d'occupation atypique mais le régime de cotisations,
 * de formalisme Dimona et de plafonds annuels est structurellement
 * incomparable). Aucun mapping mécanique possible.</p>
 *
 * <p>Contrat figé dans SF-219-12 backend
 * ({@code FlexiJobBeRequest} + {@code FlexiJobBeResponse}).</p>
 */

/**
 * Statut du travailleur candidat flexi-job (Loi-programme 26/12/2013
 * art. 13 — conditions personnelles).
 *
 * <ul>
 *   <li>{@code PENSIONNE} — pension de retraite / survie ; cumul flexi
 *       autorisé sans restriction trimestrielle ni plafond annuel
 *       (extension 2018).</li>
 *   <li>{@code SALARIE_4_5_TEMPS_T_MOINS_3} — salarié occupé chez un
 *       autre employeur à concurrence d'au moins 4/5 ETP au 3e trimestre
 *       civil précédant l'occupation flexi (référence T-3 ONSS DmfA —
 *       filtre anti-substitution).</li>
 *   <li>{@code AUTRE} — ni l'un ni l'autre — pas de statut flexi
 *       possible (catégorie résiduelle).</li>
 * </ul>
 */
export type FlexiJobBeStatutTravailleur =
  | 'PENSIONNE'
  | 'SALARIE_4_5_TEMPS_T_MOINS_3'
  | 'AUTRE';

/**
 * Secteur d'activité de l'employeur — chronologie des ouvertures
 * sectorielles (Lois-programmes 2013 / 2015 / 2018 / 2023).
 *
 * <ul>
 *   <li>{@code HORECA_302} — secteur historique 2013 (CP 302).</li>
 *   <li>{@code BOULANGERIE_COIFFURE} — extension 2015 (CP 119 / CP 314).</li>
 *   <li>{@code COMMERCE_DETAIL} — extension 2018 (CP 201/202/311/312).</li>
 *   <li>{@code AGRICULTURE} — extension 2018 (CP 132/144/145).</li>
 *   <li>{@code SOINS_DE_SANTE} — extension 2018 (périmètre limité).</li>
 *   <li>{@code SECTEUR_PUBLIC} — extension 2023 (périmètre limité).</li>
 *   <li>{@code SPORT_CULTURE_EVENEMENTIEL} — extension 2023 (sport CP 223,
 *       culture CP 304, événementiel, garages, transport autocar).</li>
 *   <li>{@code AUTRE_NON_ELIGIBLE} — secteur non listé par les
 *       lois-programmes.</li>
 *   <li>{@code ZONE_GRISE_EXTENSION_RECENTE} — extension récente sans AR
 *       d'application connu — analyse juridique requise.</li>
 * </ul>
 */
export type FlexiJobBeSecteurEmployeur =
  | 'HORECA_302'
  | 'BOULANGERIE_COIFFURE'
  | 'COMMERCE_DETAIL'
  | 'AGRICULTURE'
  | 'SOINS_DE_SANTE'
  | 'SECTEUR_PUBLIC'
  | 'SPORT_CULTURE_EVENEMENTIEL'
  | 'AUTRE_NON_ELIGIBLE'
  | 'ZONE_GRISE_EXTENSION_RECENTE';

/**
 * Verdict hiérarchisé 7 états de l'outil flexi-job BE — ordre de
 * priorité travailleur → secteur → cumul → formalisme → rémunération.
 *
 * <ul>
 *   <li>{@code ELIGIBLE} — toutes conditions cumulatives réunies
 *       (vert).</li>
 *   <li>{@code FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} — conditions de fond
 *       OK mais formalisme défaillant (ambre — risque requalification
 *       ONSS au régime normal + récupération cotisations).</li>
 *   <li>{@code FRAGILE_PLAFOND_DEPASSE} — flexi-salaire sous minimum
 *       légal OU plafond annuel exonéré dépassé (ambre — partie
 *       excédentaire soumise à cotisations normales + taxation).</li>
 *   <li>{@code INELIGIBLE_TRAVAILLEUR_HORS_CONDITION} — ni pensionné,
 *       ni 4/5 ETP T-3 — pas de statut flexi possible (rouge).</li>
 *   <li>{@code INELIGIBLE_SECTEUR_NON_ELIGIBLE} — secteur de l'employeur
 *       non listé parmi les secteurs flexi éligibles (rouge).</li>
 *   <li>{@code INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR} — le travailleur
 *       est déjà occupé chez le même employeur (rouge — interdiction
 *       cumul prestations principales + flexi, art. 13 § 2).</li>
 *   <li>{@code A_ANALYSER} — secteur en zone grise (extension récente
 *       en cours d'AR d'application) — analyse juridique requise
 *       (gris info).</li>
 * </ul>
 */
export type FlexiJobBeVerdict =
  | 'ELIGIBLE'
  | 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT'
  | 'FRAGILE_PLAFOND_DEPASSE'
  | 'INELIGIBLE_TRAVAILLEUR_HORS_CONDITION'
  | 'INELIGIBLE_SECTEUR_NON_ELIGIBLE'
  | 'INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR'
  | 'A_ANALYSER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/flexi-job-be}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface FlexiJobBeRequest {
  /** Date de début de la première prestation flexi pour cet employeur. */
  datePremiereOccupationFlexi: string;
  /** Catégorie du candidat flexi (pensionné / 4/5 T-3 / autre). */
  statutTravailleur: FlexiJobBeStatutTravailleur;
  /** Secteur d'activité de l'employeur. */
  secteurEmployeur: FlexiJobBeSecteurEmployeur;
  /** Travailleur déjà occupé chez le MÊME employeur (cumul interdit). */
  dejaOccupeChezMemeEmployeur: boolean;
  /** Contrat-cadre flexi écrit signé (art. 16 Loi-prog. 26/12/2013). */
  contratCadreSigne: boolean;
  /** Déclaration Dimona spécifique FLX faite (DmfA ONSS). */
  dimonaFlxDeclaree: boolean;
  /** Taux horaire brut convenu en € (≥ minimum légal indexé). */
  flexiSalaireHoraireBrut: number;
  /** Minimum légal applicable à la date d'occupation, en € (paramétrable). */
  flexiSalaireMinimumApplicable: number;
  /** Revenu flexi cumulé sur l'année civile, en €. */
  revenuAnnuelFlexiCumule: number;
  /** Plafond annuel exonéré applicable, en € (paramétrable, inapplicable aux pensionnés). */
  plafondAnnuelExonere: number;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + verdict +
 * ventilation d'éligibilité par dimension + montant excédentaire au plafond.
 */
export interface FlexiJobBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  datePremiereOccupationFlexi: string;
  statutTravailleur: FlexiJobBeStatutTravailleur;
  secteurEmployeur: FlexiJobBeSecteurEmployeur;
  dejaOccupeChezMemeEmployeur: boolean;
  contratCadreSigne: boolean;
  dimonaFlxDeclaree: boolean;
  flexiSalaireHoraireBrut: number;
  flexiSalaireMinimumApplicable: number;
  revenuAnnuelFlexiCumule: number;
  plafondAnnuelExonere: number;

  // -- Verdict --
  verdict: FlexiJobBeVerdict;
  /** Condition travailleur respectée (pensionné OU 4/5 ETP T-3). */
  travailleurEligible: boolean;
  /** Secteur listé parmi les secteurs flexi éligibles. */
  secteurEligible: boolean;
  /** Cumul même employeur respecté (= pas déjà occupé chez lui). */
  cumulRespecte: boolean;
  /** Formalisme cumulatif respecté (contrat-cadre + Dimona FLX). */
  formalismeRespecte: boolean;
  /** Rémunération conforme (salaire ≥ minimum + revenu ≤ plafond). */
  remunerationConforme: boolean;
  /** Montant excédentaire au plafond annuel exonéré, en € (0 si inapplicable). */
  montantExcedentaireAuPlafond: number;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}
