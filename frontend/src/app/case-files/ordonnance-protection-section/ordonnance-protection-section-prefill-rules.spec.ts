/**
 * F-236 SF-236-02 — Tests `OrdonnanceProtectionPrefillRules`.
 * SF-246-08 : ajout tests garde BE + `dateRequeteOP` champ réel.
 */
import {
  OrdonnanceProtectionPrefillRules,
  computePrefillCount,
  computeViolencesAllegueesCodes,
} from './ordonnance-protection-section-prefill-rules';

describe('OrdonnanceProtectionPrefillRules', () => {
  it('cas 0 — vide', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('retourne 0 pour workspaceCountry BELGIQUE (garde BE)', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateRequeteOP: '2026-04-15',
          violencesAllegueesDetectees: ['PHYSIQUES'],
          dangerImmediatDetected: true,
        } as any,
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — date + 1 violence valide', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateRequeteOP: '2026-04-15',
          violencesAllegueesDetectees: ['PHYSIQUES', 'INVALIDE'],
        },
      } as any),
    ).toBe(2);
  });

  it('cas M — booléens false ne comptent pas', () => {
    expect(
      computePrefillCount({
        aiData: {
          dangerImmediatDetected: false,
          presenceEnfantsDetected: false,
        },
      } as any),
    ).toBe(0);
  });

  it('cas N — 7/7 champs', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateRequeteOP: '2026-04-15',
          violencesAllegueesDetectees: ['PHYSIQUES'],
          preuvesViolencesDetectees: ['CERTIFICAT_MEDICAL'],
          dangerImmediatDetected: true,
          presenceEnfantsDetected: true,
          logementCommunDetected: true,
          victimeFinanciairementDependanteDetected: true,
        },
        workspaceCountry: 'FRANCE',
      } as any),
    ).toBe(7);
  });

  it('filtre les codes violence non reconnus', () => {
    expect(
      computeViolencesAllegueesCodes({
        aiData: { violencesAllegueesDetectees: ['psychologiques', 'BLABLA', 'PHYSIQUES'] },
      } as any),
    ).toEqual(['PSYCHOLOGIQUES', 'PHYSIQUES']);
  });

  it('surface', () => {
    expect(OrdonnanceProtectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
