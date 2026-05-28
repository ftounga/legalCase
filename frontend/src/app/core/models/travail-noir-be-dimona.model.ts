/**
 * SF-219-26b : modèle frontend de l'outil F-219 — Travail noir BE
 * DIMONA, requalification et sanctions (Loi-programme du 24/12/2002
 * art. 167-184 + AR du 05/11/2002 + Code pénal social art. 181
 * niveau 4 + Loi 22/04/2003 art. 28 amende ONSS forfaitaire 3 ×).
 *
 * <p><b>BE uniquement</b> — le régime français du travail dissimulé
 * (Code du travail art. L. 8221-1 et s. + L. 8224-1 amende 45 000 €
 * pp / 225 000 € pm + URSSAF redressement majoration 25 % art.
 * R. 243-18) constitue un dispositif juridiquement distinct dans ses
 * quanta (DPAE et non DIMONA, parquet et non auditorat, pas de barème
 * uniforme 4 niveaux du Code pénal social) et sa procédure. Aucune
 * transposition mécanique. Le gate {@code workspaceCountry} est porté
 * côté composant ; le backend renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-26 backend
 * ({@code TravailNoirBeDimonaRequest} +
 * {@code TravailNoirBeDimonaResponse}).</p>
 */

/**
 * Statut de la déclaration DIMONA au moment du contrôle inspection
 * sociale (SPF Emploi / ONSS / Auditorat).
 *
 * <ul>
 *   <li>{@code DECLAREE_AVANT_DEBUT} — DIMONA effectuée avant le début
 *       effectif de l'occupation (cas nominal, art. 4 AR 05/11/2002).</li>
 *   <li>{@code DECLAREE_TARDIVE} — DIMONA effectuée après le début mais
 *       avant le contrôle (régularisation partielle).</li>
 *   <li>{@code ABSENTE} — aucune DIMONA au moment du contrôle
 *       (infraction art. 181 C. pén. soc. niveau 4).</li>
 *   <li>{@code INDEPENDANT_FAUSSEMENT_QUALIFIE} — relation faussement
 *       qualifiée d'indépendante à requalifier en salariat (Loi-programme
 *       I 27/12/2006 art. 328 et s.).</li>
 * </ul>
 */
export type TravailNoirBeDimonaStatutDimona =
  | 'DECLAREE_AVANT_DEBUT'
  | 'DECLAREE_TARDIVE'
  | 'ABSENTE'
  | 'INDEPENDANT_FAUSSEMENT_QUALIFIE';

/**
 * Présence d'éléments de subordination caractérisant le salariat (Loi
 * 27/06/1969 + art. 328 § 1 C. pén. soc. — présomption simple
 * renversable Cass. BE 06/12/2010 S.10.0067.F).
 */
export type TravailNoirBeDimonaElementsSubordination =
  | 'OUI'
  | 'NON'
  | 'INDETERMINE';

/**
 * Verdict de l'outil — 6 cas couvrant les branches de qualification
 * du défaut DIMONA et de la requalification de la relation de travail.
 *
 * <ul>
 *   <li>{@code DIMONA_CONFORME} — déclaration effectuée avant début
 *       occupation, aucune sanction.</li>
 *   <li>{@code DIMONA_TARDIVE_REGULARISABLE} — sanction administrative
 *       ONSS atténuée, pas de niveau 4 pénal.</li>
 *   <li>{@code ABSENCE_DIMONA_SANCTION_NIVEAU_4} — infraction art. 181
 *       C. pén. soc. niveau 4 + cotisations rétroactives + amende
 *       ONSS forfaitaire 3 ×.</li>
 *   <li>{@code REQUALIFICATION_SALARIE_PRESUMEE} — présomption de
 *       salariat art. 328 § 1 C. pén. soc. déclenchée par absence
 *       DIMONA + subordination caractérisée.</li>
 *   <li>{@code INDEPENDANT_REQUALIFIE_NON_DECLARE} — faux indépendant
 *       requalifié, cotisations rétroactives depuis l'origine.</li>
 *   <li>{@code A_QUALIFIER} — inputs insuffisants ou ambigus.</li>
 * </ul>
 */
export type TravailNoirBeDimonaVerdict =
  | 'DIMONA_CONFORME'
  | 'DIMONA_TARDIVE_REGULARISABLE'
  | 'ABSENCE_DIMONA_SANCTION_NIVEAU_4'
  | 'REQUALIFICATION_SALARIE_PRESUMEE'
  | 'INDEPENDANT_REQUALIFIE_NON_DECLARE'
  | 'A_QUALIFIER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/travail-noir-be-dimona}.
 */
export interface TravailNoirBeDimonaRequest {
  /** Statut DIMONA au moment du contrôle. */
  statutDimona: TravailNoirBeDimonaStatutDimona;
  /** Date de début effectif de l'occupation (ISO yyyy-MM-dd). */
  dateDebutOccupation: string;
  /** Date DIMONA effective si applicable (ISO yyyy-MM-dd, sinon null). */
  dateDimonaEffective: string | null;
  /** Date du contrôle inspection sociale (ISO yyyy-MM-dd). */
  dateControle: string;
  /** Salaire brut mensuel — base cotisations ONSS rétroactives (EUR). */
  salaireBrutMensuel: number;
  /** Nombre de travailleurs concernés (multiplicateur art. 103 § 2). */
  nombreTravailleursConcernes: number;
  /** Employeur personne morale (multiplication × 5 art. 105 § 1). */
  personneMorale: boolean;
  /** Récidive ≤ 1 an depuis condamnation antérieure (art. 110). */
  recidiveDansLAn: boolean;
  /** Éléments de subordination caractérisés (art. 328 § 1 C. pén. soc.). */
  elementsSubordination: TravailNoirBeDimonaElementsSubordination;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs
 * calculés (durée non déclarée, cotisations ONSS rétroactives, amende
 * forfaitaire 3 ×, sanction pénale art. 181, emprisonnement,
 * majorations) + verdict + métadonnées légales.
 */
export interface TravailNoirBeDimonaResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  statutDimona: TravailNoirBeDimonaStatutDimona;
  dateDebutOccupation: string;
  dateDimonaEffective: string | null;
  dateControle: string;
  salaireBrutMensuel: number;
  nombreTravailleursConcernes: number;
  personneMorale: boolean;
  recidiveDansLAn: boolean;
  elementsSubordination: TravailNoirBeDimonaElementsSubordination;

  // -- Outputs calculés --
  /** Verdict 6 états. */
  verdict: TravailNoirBeDimonaVerdict;
  /** Durée non déclarée en jours (entre début occupation et contrôle). */
  dureeNonDeclareeJours: number;
  /** Cotisations ONSS employeur (~25 % brut, EUR). */
  cotisationsOnssEmployeur: number;
  /** Cotisations ONSS travailleur (13,07 % brut, EUR). */
  cotisationsOnssTravailleur: number;
  /** Cotisations ONSS totales (employeur + travailleur, EUR). */
  cotisationsOnssTotal: number;
  /** Amende ONSS forfaitaire 3 × cotisations dues (art. 28 L. 22/04/2003). */
  amendeOnssForfaitaire3x: number;
  /** Sanction pénale art. 181 niveau 4 applicable. */
  sanctionPenaleNiveau4Applicable: boolean;
  /** Amende admin min niveau 4 (300 €, valeur de base). */
  amendePenaleAdminMin: number;
  /** Amende admin max niveau 4 (3000 €, valeur de base). */
  amendePenaleAdminMax: number;
  /** Amende pénale min niveau 4 (600 €, valeur de base). */
  amendePenaleMin: number;
  /** Amende pénale max niveau 4 (6000 €, valeur de base). */
  amendePenaleMax: number;
  /** Emprisonnement applicable (niveau 4). */
  emprisonnementApplicable: boolean;
  /** Emprisonnement min en mois (6 si applicable). */
  emprisonnementMinMois: number;
  /** Emprisonnement max en mois (36 si applicable). */
  emprisonnementMaxMois: number;
  /** Multiplication par nombre de travailleurs appliquée (art. 103 § 2). */
  multiplicationParTravailleur: boolean;
  /** Majoration × 5 personne morale appliquée (art. 105). */
  majorationPersonneMorale: boolean;
  /** Majoration × 2 récidive ≤ 1 an appliquée (art. 110). */
  majorationRecidive: boolean;

  // -- Verdict + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (indexation, transaction, articulation auditorat). */
  avertissement: string;
}
