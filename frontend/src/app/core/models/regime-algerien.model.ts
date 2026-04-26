/**
 * SF-IM-17-02 : types frontend pour l'outil décisionnel "Régime algérien"
 * (F-IM-17). FR uniquement (Accord franco-algérien du 27/12/1968 modifié
 * par les avenants 1985 / 1994 / 2001).
 *
 * Contrat figé dans SF-IM-17-01 (backend, mergé PR #653).
 *
 * 5 voies de l'accord :
 * - CRA_1_AN                       : art. 5  — 1ère demande, équivalent VPF étudiant / visiteur
 * - CRA_10_ANS_LIEN_FRANCE         : art. 6 al. 1, 2, 3 — conjoint FR / parent enfant FR / 10 ans présence
 * - CRA_10_ANS_RESIDENT_ANCIEN     : art. 7bis — né en France OU arrivé < 13 ans
 * - CHANGEMENT_VERS_TRAVAILLEUR    : art. 7 — passage CRA 1 an → CRA Travailleur
 * - REGROUPEMENT_FAMILIAL_ACCORD_1968 : art. 4 — conjoint et enfants mineurs
 */

/** Code des voies supportées par le calculateur backend. */
export type VoieRegimeAlgerienCode =
  | 'CRA_1_AN'
  | 'CRA_10_ANS_LIEN_FRANCE'
  | 'CRA_10_ANS_RESIDENT_ANCIEN'
  | 'CHANGEMENT_VERS_TRAVAILLEUR'
  | 'REGROUPEMENT_FAMILIAL_ACCORD_1968';

/** Verdict de recevabilité (scoring backend). */
export type VerdictRecevabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio voie. */
export const VOIE_REGIME_ALGERIEN_LABELS:
  ReadonlyArray<{ code: VoieRegimeAlgerienCode; label: string; sub: string }> = [
    {
      code: 'CRA_1_AN',
      label: 'Certificat de résidence algérien — 1 an',
      sub: 'Accord 27/12/1968 art. 5 — 1ère demande (étudiant / visiteur)',
    },
    {
      code: 'CRA_10_ANS_LIEN_FRANCE',
      label: 'Certificat de résidence algérien — 10 ans (lien France)',
      sub: 'Accord art. 6 al. 1, 2, 3 — conjoint FR / parent enfant FR / 10 ans',
    },
    {
      code: 'CRA_10_ANS_RESIDENT_ANCIEN',
      label: 'Certificat de résidence algérien — 10 ans (résident ancien)',
      sub: 'Accord art. 7bis — né en France OU arrivé avant 13 ans',
    },
    {
      code: 'CHANGEMENT_VERS_TRAVAILLEUR',
      label: 'Changement vers CRA Travailleur',
      sub: 'Accord art. 7 — passage CRA 1 an → CRA Travailleur',
    },
    {
      code: 'REGROUPEMENT_FAMILIAL_ACCORD_1968',
      label: 'Regroupement familial (accord 1968)',
      sub: 'Accord art. 4 — conjoint et enfants mineurs (conditions réduites)',
    },
  ];

export interface RegimeAlgerienRequest {
  voieDemande: VoieRegimeAlgerienCode;
  /** Gate métier — backend rejette si false explicitement. */
  nationaliteAlgerienne: boolean;
  documentEtatCivilOriginal?: boolean | null;
  /** ≥ 0. */
  presenceReguliereFranceMois?: number | null;
  casierJudiciaireVierge?: boolean | null;
  visaLongSejourValide?: boolean | null;
  conjointFrancais?: boolean | null;
  parentEnfantFrancais?: boolean | null;
  neEnFrance?: boolean | null;
  arriveeAvant13Ans?: boolean | null;
  contratTravailValide?: boolean | null;
  ressourcesSuffisantes?: boolean | null;
  logementDecent?: boolean | null;
  /** ≥ 0 (regroupement familial). */
  nombrePersonnesFoyer?: number | null;
}

export interface RegimeAlgerienResponse {
  caseFileId: string;
  country: 'FRANCE';
  voieDemande: string;
  voieRecommandee: string;
  verdictRecevabilite: VerdictRecevabilite;
  titreApplicable: string;
  /** 1 ou 10 ans selon voie. */
  dureeTitreAnnees: number;
  criteresNonRemplis: string[];
  documentsRequis: string[];
  /** 3 ou 6 mois selon voie. */
  delaiInstructionMois: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
}

/** Libellé humain depuis un code voie. */
export function voieLabel(code: VoieRegimeAlgerienCode | string | null | undefined): string {
  if (!code) return '';
  const opt = VOIE_REGIME_ALGERIEN_LABELS.find((o) => o.code === code);
  return opt?.label ?? String(code);
}

/**
 * Mappe une chaîne IA libre (procedureChecks / questions / typeProcedureDetectee)
 * vers l'enum frontend. Normalisation insensible à la casse, accents et
 * tirets/underscores. Retourne `null` si non reconnu (no-op gracieux pour
 * la validation F-IA-03).
 */
export function mapVoieFromIa(value: string | null | undefined): VoieRegimeAlgerienCode | null {
  if (!value) return null;
  const normalized = value.toString().trim().toUpperCase()
    .replace(/[\s\-]+/g, '_')
    .replace(/[ÉÈÊË]/g, 'E')
    .replace(/[ÀÂÄ]/g, 'A')
    .replace(/[ÎÏ]/g, 'I')
    .replace(/[ÔÖ]/g, 'O')
    .replace(/[ÙÛÜ]/g, 'U');

  if (normalized === 'CRA_1_AN' || normalized === 'CRA_1AN'
      || normalized === 'CERTIFICAT_RESIDENCE_1_AN' || normalized === 'CRA1') {
    return 'CRA_1_AN';
  }
  if (normalized === 'CRA_10_ANS_LIEN_FRANCE' || normalized === 'CRA_10_ANS'
      || normalized === 'CERTIFICAT_RESIDENCE_10_ANS_LIEN_FRANCE'
      || normalized === 'CRA_10ANS_LIEN_FRANCE'
      || normalized === 'CRA_LIEN_FRANCE') {
    return 'CRA_10_ANS_LIEN_FRANCE';
  }
  if (normalized === 'CRA_10_ANS_RESIDENT_ANCIEN'
      || normalized === 'CERTIFICAT_RESIDENCE_10_ANS_RESIDENT_ANCIEN'
      || normalized === 'CRA_RESIDENT_ANCIEN'
      || normalized === 'CRA_NE_EN_FRANCE') {
    return 'CRA_10_ANS_RESIDENT_ANCIEN';
  }
  if (normalized === 'CHANGEMENT_VERS_TRAVAILLEUR'
      || normalized === 'CHANGEMENT_TRAVAILLEUR'
      || normalized === 'CRA_TRAVAILLEUR') {
    return 'CHANGEMENT_VERS_TRAVAILLEUR';
  }
  if (normalized === 'REGROUPEMENT_FAMILIAL_ACCORD_1968'
      || normalized === 'REGROUPEMENT_FAMILIAL'
      || normalized === 'REGROUPEMENT_FAMILIAL_ALGERIEN') {
    return 'REGROUPEMENT_FAMILIAL_ACCORD_1968';
  }
  return null;
}

/**
 * Heuristique défensive : tente de détecter si la nationalité algérienne
 * est plausible depuis ImmigrationExtractedData. Le prompt IA actuel
 * n'expose PAS de champ `nationalite` dédié — la fonction reste donc un
 * no-op gracieux et retournera `null` dans la majorité des cas. Pattern
 * présent pour bénéficier sans changement structurel d'un futur
 * enrichissement extracteur (cf. SF-IM-17-02 mini-spec, Hors périmètre).
 */
export function detectNationaliteAlgerienneFromIa(
  ai: { typeTitreSejour?: string | null; typeProcedureDetectee?: string | null } | null | undefined,
): boolean | null {
  if (!ai) return null;
  const haystack = [ai.typeTitreSejour, ai.typeProcedureDetectee]
    .filter((v): v is string => !!v)
    .map((v) => v.toUpperCase()
      .replace(/[ÉÈÊË]/g, 'E')
      .replace(/[ÀÂÄ]/g, 'A'))
    .join(' ');
  if (!haystack) return null;
  if (haystack.includes('ALGERIEN') || haystack.includes('ALGERIE')
      || haystack.includes('CRA_') || /\bCRA\b/.test(haystack)
      || haystack.includes('ACCORD_FRANCO_ALGERIEN')
      || haystack.includes('ACCORD_1968')) {
    return true;
  }
  return null;
}
