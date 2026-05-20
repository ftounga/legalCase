/**
 * SF-IM-12-02 : types frontend pour l'outil décisionnel "Asile avancé"
 * (F-IM-12). FR uniquement (CESEDA Livre V).
 *
 * Contrat figé dans SF-IM-12-01 (backend, mergé PR #644).
 *
 * 5 dispositifs distincts :
 * - DUBLIN_III             : Règlement UE 604/2013 (transfert vers EM responsable)
 * - PROCEDURE_ACCELEREE    : CESEDA L.531-24 (pays sûrs / fraude / refus empreintes)
 * - REEXAMEN               : CESEDA L.531-32 (éléments nouveaux postérieurs)
 * - APATRIDIE              : CESEDA L.512-1 (Convention NY 1954)
 * - PROTECTION_SUBSIDIAIRE : CESEDA L.512-1+ (traitements graves)
 */

/** Code des dispositifs d'asile supportés par le calculateur backend. */
export type DispositifAsileCode =
  | 'DUBLIN_III'
  | 'PROCEDURE_ACCELEREE'
  | 'REEXAMEN'
  | 'APATRIDIE'
  | 'PROTECTION_SUBSIDIAIRE';

/**
 * Verdict de recevabilité (8 valeurs backend).
 * Mapping bandeau (3 niveaux) :
 * - navy/info     : RECEVABLE_TRANSFERT, FRANCE_COMPETENTE, RECEVABLE_REEXAMEN,
 *                   RECEVABLE_APATRIDIE, RECEVABLE_PROTECTION_SUBSIDIAIRE
 * - or/warning    : ACCELEREE_APPLICABLE, ACCELEREE_NON_APPLICABLE
 * - rouge/critical: IRRECEVABLE
 */
export type VerdictAsileAvance =
  | 'RECEVABLE_TRANSFERT'
  | 'FRANCE_COMPETENTE'
  | 'ACCELEREE_APPLICABLE'
  | 'ACCELEREE_NON_APPLICABLE'
  | 'RECEVABLE_REEXAMEN'
  | 'RECEVABLE_APATRIDIE'
  | 'RECEVABLE_PROTECTION_SUBSIDIAIRE'
  | 'IRRECEVABLE';

/** Libellés humains pour mat-radio dispositif. */
export const DISPOSITIF_ASILE_LABELS:
  ReadonlyArray<{ code: DispositifAsileCode; label: string; sub: string }> = [
    {
      code: 'DUBLIN_III',
      label: 'Procédure Dublin III',
      sub: 'Règl. UE 604/2013 — transfert vers l\'État membre responsable',
    },
    {
      code: 'PROCEDURE_ACCELEREE',
      label: 'Procédure accélérée',
      sub: 'CESEDA L.531-24 — pays sûrs / fraude / refus empreintes',
    },
    {
      code: 'REEXAMEN',
      label: 'Réexamen',
      sub: 'CESEDA L.531-32 — éléments nouveaux postérieurs au rejet',
    },
    {
      code: 'APATRIDIE',
      label: 'Statut d\'apatride',
      sub: 'CESEDA L.512-1 — Convention de New York 1954',
    },
    {
      code: 'PROTECTION_SUBSIDIAIRE',
      label: 'Protection subsidiaire',
      sub: 'CESEDA L.512-1+ — traitements graves redoutés (4 ans renouvelable)',
    },
  ];

export interface AsileAvanceRequest {
  dispositifAsile: DispositifAsileCode;
  /** REEXAMEN : date du rejet précédent (ISO YYYY-MM-DD). */
  dateDecisionAnterieure?: string | null;
  /** REEXAMEN : éléments nouveaux postérieurs au rejet. */
  elementsNouveaux?: boolean | null;
  /** PROCEDURE_ACCELEREE : pays d'origine sur la liste OFPRA. */
  paysOrigineDansListeSurs?: boolean | null;
  /** DUBLIN_III : empreintes EURODAC déjà prises dans un autre EM. */
  empreintesEurodacAutresEm?: boolean | null;
  /** DUBLIN_III : demandeur en fuite (délai 6 → 18 mois). */
  demandeurEnFuite?: boolean | null;
  /** APATRIDIE / PROTECTION_SUBSIDIAIRE : motifs d'exclusion (bloquant). */
  motifsExclusion?: boolean | null;
  /** PROTECTION_SUBSIDIAIRE : crainte fondée de traitements graves établie. */
  traitementsGravesEtablis?: boolean | null;
  /** PROCEDURE_ACCELEREE : fraude documentaire avérée (motif alternatif). */
  fraudeDocumentaireAvere?: boolean | null;
  /** PROCEDURE_ACCELEREE : refus de prise d'empreintes (motif alternatif). */
  refusPriseEmpreintes?: boolean | null;
  /** APATRIDIE : présence régulière en France (défaut true). */
  presenceReguliere?: boolean | null;
}

/**
 * SF-206-02 hotfix master-red : la Response ré-expose le snapshot d'inputs
 * Request (ré-édition du formulaire après rechargement). Le composant
 * `asile-avance-section` hydrate déjà ces champs depuis `r.*` (cf. ngOnInit
 * load()). Aligne le contrat sur le pattern F-DT-36 / F-DT-42.
 */
export interface AsileAvanceResponse extends AsileAvanceRequest {
  caseFileId: string;
  country: 'FRANCE';
  dispositifLibelle: string;
  verdictRecevabilite: VerdictAsileAvance;
  delaiInstructionMois: number;
  recoursPossible: string;
  documentsRequis: string[];
  risqueRefus: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
}

/** Libellé humain depuis un code dispositif. */
export function dispositifLabel(
  code: DispositifAsileCode | string | null | undefined,
): string {
  if (!code) return '';
  const opt = DISPOSITIF_ASILE_LABELS.find((o) => o.code === code);
  return opt?.label ?? String(code);
}

/**
 * Mappe une chaîne IA libre (procedureChecks / questions /
 * `aiData.typeProcedureDetectee`) vers l'enum frontend.
 * Normalisation insensible à la casse, accents et tirets/underscores.
 * Retourne `null` si non reconnu (no-op gracieux pour validation F-IA-03).
 *
 * Mapping pragmatique :
 * - DUBLIN_III, ASILE_DUBLIN, ASILE_DUBLIN_III → DUBLIN_III
 * - PROCEDURE_ACCELEREE, ASILE_ACCELEREE, ASILE_PROCEDURE_ACCELEREE → PROCEDURE_ACCELEREE
 * - REEXAMEN, ASILE_REEXAMEN → REEXAMEN
 * - APATRIDIE, ASILE_APATRIDIE → APATRIDIE
 * - PROTECTION_SUBSIDIAIRE, ASILE_PROTECTION_SUBSIDIAIRE → PROTECTION_SUBSIDIAIRE
 */
export function mapDispositifFromIa(
  value: string | null | undefined,
): DispositifAsileCode | null {
  if (!value) return null;
  const normalized = value.toString().trim().toUpperCase()
    .replace(/[\s\-]+/g, '_')
    .replace(/[ÉÈÊË]/g, 'E')
    .replace(/[ÀÂÄ]/g, 'A')
    .replace(/[ÎÏ]/g, 'I')
    .replace(/[ÔÖ]/g, 'O')
    .replace(/[ÙÛÜ]/g, 'U');

  if (normalized === 'DUBLIN_III' || normalized === 'DUBLIN'
      || normalized === 'ASILE_DUBLIN' || normalized === 'ASILE_DUBLIN_III'
      || normalized === 'PROCEDURE_DUBLIN' || normalized === 'TRANSFERT_DUBLIN') {
    return 'DUBLIN_III';
  }
  if (normalized === 'PROCEDURE_ACCELEREE' || normalized === 'ACCELEREE'
      || normalized === 'ASILE_ACCELEREE' || normalized === 'ASILE_PROCEDURE_ACCELEREE') {
    return 'PROCEDURE_ACCELEREE';
  }
  if (normalized === 'REEXAMEN' || normalized === 'ASILE_REEXAMEN'
      || normalized === 'REEXAMEN_ASILE') {
    return 'REEXAMEN';
  }
  if (normalized === 'APATRIDIE' || normalized === 'ASILE_APATRIDIE'
      || normalized === 'STATUT_APATRIDE' || normalized === 'APATRIDE') {
    return 'APATRIDIE';
  }
  if (normalized === 'PROTECTION_SUBSIDIAIRE'
      || normalized === 'ASILE_PROTECTION_SUBSIDIAIRE'
      || normalized === 'PROTECTION_SUB' || normalized === 'PS_ASILE') {
    return 'PROTECTION_SUBSIDIAIRE';
  }
  return null;
}
