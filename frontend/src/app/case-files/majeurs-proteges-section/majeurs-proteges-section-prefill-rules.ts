import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-03 — Helper partagé pour le pré-remplissage IA de
 * `MajeursProtegesSectionComponent` (F-FA-25-majeurs-proteges).
 *
 * Référence canonique du contrat (cf. `docs/features/F-236/contract-prefill-rules.md`).
 *
 * <p>12 champs pré-remplissables depuis `aiData` (`FamilleExtractedData`) :
 * <ol>
 *   <li>`regimeProtectionDemande` (string enum 6 valeurs) — depuis
 *   `ai.regimeProtectionMajeursDetected` (SF-246-27 — source réelle).
 *   Anciennement `ai.regimeProtectionDemande` (aspirationnel, conservé pour
 *   rétro-compat runtime mais source réelle désormais prioritaire).</li>
 *   <li>`altertationFacultesMentales` (boolean) — depuis
 *   `ai.altertationFacultesMentales`. Pré-fill seulement si `true`
 *   (sémantique runtime : `false` ne surcharge pas la valeur par défaut).</li>
 *   <li>`altertationFacultesPhysiques` (boolean) — depuis
 *   `ai.altertationFacultesPhysiques`. Pré-fill seulement si `true`.</li>
 *   <li>`certificatMedicalCirconstancie` (boolean) — depuis
 *   `ai.certificatMedicalCirconstancieDetected`. Pré-fill seulement si `true`.</li>
 *   <li>`dateCertificatMedical` (string ISO YYYY-MM-DD) — depuis
 *   `ai.dateCertificatMedicalMajeursDetected` (SF-246-27 — source réelle).
 *   Anciennement `ai.dateCertificatMedicalDetected` (aspirationnel).</li>
 *   <li>`consentementPersonneAProteger` (boolean) — depuis
 *   `ai.consentementPersonneAProtegerDetected`. Pré-fill seulement si `true`.</li>
 *   <li>`demandeurFamilial` (string enum 6 valeurs) — depuis
 *   `ai.demandeurFamilialDetected` (validé contre `VALID_DEMANDEURS`).</li>
 *   <li>`actesEnvisages` (string[] enum) — depuis
 *   `ai.actesEnvisagesDetected` (chaque acte validé contre `VALID_ACTES`,
 *   array non vide après filtrage).</li>
 *   <li>`incapaciteGestionQuotidienne` (boolean) — depuis
 *   `ai.incapaciteGestionQuotidienneDetected`. Pré-fill seulement si `true`.</li>
 *   <li>`altertationGrave` (boolean) — depuis
 *   `ai.altertationGraveDetected`. Pré-fill seulement si `true`.</li>
 *   <li>`mandatPrealableSigne` (boolean) — depuis
 *   `ai.mandatPrealableSigneDetected`. Pré-fill seulement si `true`.</li>
 *   <li>`formeMandatProtection` (string enum 2 valeurs) — depuis
 *   `ai.formeMandatProtectionDetected` (validé contre `VALID_FORMES_MANDAT`).</li>
 * </ol></p>
 *
 * <p>F-FA-25 est FRANCE only — pas de gating pays au niveau du helper (déjà
 * filtré côté composant via `isFrance()` qui contrôle `load()` et donc le
 * mount effectif du formulaire).</p>
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const VALID_REGIMES: ReadonlySet<string> = new Set<string>([
  'SAUVEGARDE_JUSTICE',
  'HABILITATION_FAMILIALE',
  'CURATELLE_SIMPLE',
  'CURATELLE_RENFORCEE',
  'TUTELLE',
  'MANDAT_PROTECTION_FUTURE',
]);

export const VALID_DEMANDEURS: ReadonlySet<string> = new Set<string>([
  'CONJOINT',
  'ENFANT_MAJEUR',
  'PARENT',
  'FRERE_SOEUR',
  'TIERS_PROCHE',
  'MINISTERE_PUBLIC',
]);

export const VALID_ACTES: ReadonlySet<string> = new Set<string>([
  'GESTION_PATRIMOINE',
  'DECISIONS_LOGEMENT',
  'DECISIONS_SANTE',
  'DECISIONS_FAMILIALES',
  'ACTES_ETAT_CIVIL',
  'AUTRE',
]);

export const VALID_FORMES_MANDAT: ReadonlySet<string> = new Set<string>([
  'NOTARIE',
  'SOUS_SEING_PRIVE',
]);

export const MajeursProtegesPrefillRules = {
  ISO_DATE_RE,
  VALID_REGIMES,
  VALID_DEMANDEURS,
  VALID_ACTES,
  VALID_FORMES_MANDAT,

  computeRegimeProtectionDemande(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    // SF-246-27 : source réelle `regimeProtectionMajeursDetected` (prioritaire).
    // Fallback sur `regimeProtectionDemande` (aspirationnel, rétro-compat).
    const raw = (ai as any).regimeProtectionMajeursDetected ?? ai.regimeProtectionDemande;
    if (typeof raw !== 'string' || raw.length === 0) return null;
    const upper = raw.toUpperCase();
    return VALID_REGIMES.has(upper) ? upper : null;
  },

  /** Pré-fill seulement quand IA = `true` (sémantique runtime : `false` ne
   *  surcharge pas la valeur par défaut `false` de l'avocat). */
  computeAltertationFacultesMentales(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.altertationFacultesMentales !== 'boolean') return null;
    return ai.altertationFacultesMentales === true ? true : null;
  },

  computeAltertationFacultesPhysiques(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.altertationFacultesPhysiques !== 'boolean') return null;
    return ai.altertationFacultesPhysiques === true ? true : null;
  },

  computeCertificatMedicalCirconstancie(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.certificatMedicalCirconstancieDetected !== 'boolean') return null;
    return ai.certificatMedicalCirconstancieDetected === true ? true : null;
  },

  computeDateCertificatMedical(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    // SF-246-27 : source réelle `dateCertificatMedicalMajeursDetected` (prioritaire).
    // Fallback sur `dateCertificatMedicalDetected` (aspirationnel, rétro-compat).
    const raw = (ai as any).dateCertificatMedicalMajeursDetected ?? ai.dateCertificatMedicalDetected;
    if (typeof raw !== 'string') return null;
    if (!ISO_DATE_RE.test(raw)) return null;
    return raw;
  },

  computeConsentementPersonneAProteger(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.consentementPersonneAProtegerDetected !== 'boolean') return null;
    return ai.consentementPersonneAProtegerDetected === true ? true : null;
  },

  computeDemandeurFamilial(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = ai.demandeurFamilialDetected;
    if (typeof raw !== 'string' || raw.length === 0) return null;
    const upper = raw.toUpperCase();
    return VALID_DEMANDEURS.has(upper) ? upper : null;
  },

  /** Renvoie le tableau filtré (non vide) d'actes valides, ou `null`. */
  computeActesEnvisages(input: PrefillCountInput): string[] | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = ai.actesEnvisagesDetected;
    if (!Array.isArray(raw) || raw.length === 0) return null;
    const filtered = raw
      .map((a: any) => typeof a === 'string' ? a.toUpperCase() : null)
      .filter((a: string | null): a is string => !!a && VALID_ACTES.has(a));
    return filtered.length > 0 ? filtered : null;
  },

  computeIncapaciteGestionQuotidienne(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.incapaciteGestionQuotidienneDetected !== 'boolean') return null;
    return ai.incapaciteGestionQuotidienneDetected === true ? true : null;
  },

  computeAltertationGrave(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.altertationGraveDetected !== 'boolean') return null;
    return ai.altertationGraveDetected === true ? true : null;
  },

  computeMandatPrealableSigne(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.mandatPrealableSigneDetected !== 'boolean') return null;
    return ai.mandatPrealableSigneDetected === true ? true : null;
  },

  computeFormeMandatProtection(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = ai.formeMandatProtectionDetected;
    if (typeof raw !== 'string' || raw.length === 0) return null;
    const upper = raw.toUpperCase();
    return VALID_FORMES_MANDAT.has(upper) ? upper : null;
  },

  /**
   * Maître : compte les 12 champs non-`null` après application des règles.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeRegimeProtectionDemande(input) !== null) n++;
    if (this.computeAltertationFacultesMentales(input) !== null) n++;
    if (this.computeAltertationFacultesPhysiques(input) !== null) n++;
    if (this.computeCertificatMedicalCirconstancie(input) !== null) n++;
    if (this.computeDateCertificatMedical(input) !== null) n++;
    if (this.computeConsentementPersonneAProteger(input) !== null) n++;
    if (this.computeDemandeurFamilial(input) !== null) n++;
    if (this.computeActesEnvisages(input) !== null) n++;
    if (this.computeIncapaciteGestionQuotidienne(input) !== null) n++;
    if (this.computeAltertationGrave(input) !== null) n++;
    if (this.computeMandatPrealableSigne(input) !== null) n++;
    if (this.computeFormeMandatProtection(input) !== null) n++;
    return n;
  },
} as const;
