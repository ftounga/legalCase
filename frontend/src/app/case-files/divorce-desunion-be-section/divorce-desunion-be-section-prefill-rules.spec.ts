/**
 * SF-246-12 — Tests du helper DivorceDesunionBePrefillRules.
 *
 * Parité stricte computePrefillCount ↔ computeDateSeparation :
 *   - Cas (a) : aiData absent / vide / date non-ISO → 0
 *   - Cas (a) : workspaceCountry FRANCE → 0
 *   - Cas (c) : date ISO valide + BELGIQUE → 1
 *
 * computeSeparationConsentue toujours null (aspirationnel — aucune source backend BE).
 * Invariant anti-confusion : dateSeparation FR (SF-246-08) ≠ dateSeparationBe BE.
 */
import {
  DivorceDesunionBePrefillRules,
  computePrefillCount,
  computeDateSeparation,
  computeSeparationConsentue,
} from './divorce-desunion-be-section-prefill-rules';

const BE = { workspaceCountry: 'BELGIQUE' as const };
const FR = { workspaceCountry: 'FRANCE' as const };

describe('DivorceDesunionBePrefillRules — SF-246-12', () => {

  // ------------------------------------------------------------------
  // Cas (a) : 0 champ
  // ------------------------------------------------------------------

  it('retourne 0 quand pas de aiData (BE)', () => {
    expect(computePrefillCount({ ...BE })).toBe(0);
    expect(computePrefillCount({ ...BE, aiData: null })).toBe(0);
    expect(computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('retourne 0 quand dateSeparationBe est null ou absent (BE)', () => {
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: null } })).toBe(0);
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: undefined } })).toBe(0);
  });

  it('retourne 0 quand dateSeparationBe est une chaîne non-ISO (BE)', () => {
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: '15/11/2024' } })).toBe(0);
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: '2024-11' } })).toBe(0);
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: '' } })).toBe(0);
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: 'INCONNU' } })).toBe(0);
  });

  // Gating pays
  it('retourne 0 quand workspaceCountry est FRANCE (gating BE-only)', () => {
    expect(computePrefillCount({ ...FR, aiData: { dateSeparationBe: '2024-11-15' } })).toBe(0);
    expect(computeDateSeparation({ ...FR, aiData: { dateSeparationBe: '2024-11-15' } })).toBeNull();
  });

  it('retourne 0 quand workspaceCountry est omis (défaut FRANCE)', () => {
    expect(computePrefillCount({ aiData: { dateSeparationBe: '2024-11-15' } })).toBe(0);
  });

  // ------------------------------------------------------------------
  // Cas (c) : 1 champ — nominal
  // ------------------------------------------------------------------

  it('retourne 1 et fournit la date quand dateSeparationBe est ISO valide + BE', () => {
    const input = { ...BE, aiData: { dateSeparationBe: '2024-11-15' } };
    expect(computePrefillCount(input)).toBe(1);
    expect(computeDateSeparation(input)).toBe('2024-11-15');
  });

  it('retourne 1 pour différentes dates ISO valides (BE)', () => {
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: '2023-01-01' } })).toBe(1);
    expect(computePrefillCount({ ...BE, aiData: { dateSeparationBe: '2025-12-31' } })).toBe(1);
  });

  // ------------------------------------------------------------------
  // Parité stricte computePrefillCount ↔ computeDateSeparation
  // ------------------------------------------------------------------

  it('parité : computePrefillCount === 1 ssi computeDateSeparation !== null', () => {
    const inputs = [
      { ...BE, aiData: { dateSeparationBe: '2024-11-15' } },
      { ...BE, aiData: {} },
      { ...FR, aiData: { dateSeparationBe: '2024-11-15' } },
      { ...BE, aiData: { dateSeparationBe: 'mauvais-format' } },
    ];
    for (const input of inputs) {
      const count = computePrefillCount(input);
      const date = computeDateSeparation(input);
      expect(count === 1).toBe(date !== null);
    }
  });

  // ------------------------------------------------------------------
  // computeSeparationConsentue — toujours null (aspirationnel)
  // ------------------------------------------------------------------

  it('computeSeparationConsentue toujours null (aucune source backend)', () => {
    expect(computeSeparationConsentue({ ...BE, aiData: {} })).toBeNull();
    expect(computeSeparationConsentue({ ...BE })).toBeNull();
    expect(computeSeparationConsentue({ ...FR })).toBeNull();
  });

  // ------------------------------------------------------------------
  // Invariant anti-confusion : dateSeparation FR ≠ dateSeparationBe BE
  // ------------------------------------------------------------------

  it('ne pré-remplit PAS depuis dateSeparation FR (champ distinct SF-246-08)', () => {
    // dateSeparation (FR) présent mais dateSeparationBe (BE) absent → 0
    const input = { ...BE, aiData: { dateSeparation: '2024-11-15' } };
    expect(computePrefillCount(input)).toBe(0);
    expect(computeDateSeparation(input)).toBeNull();
  });

  // Surface object
  it('surface : DivorceDesunionBePrefillRules expose les fonctions', () => {
    expect(DivorceDesunionBePrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(DivorceDesunionBePrefillRules.computeDateSeparation).toBe(computeDateSeparation);
    expect(DivorceDesunionBePrefillRules.computeSeparationConsentue).toBe(computeSeparationConsentue);
  });
});
