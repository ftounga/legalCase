/** F-236 SF-236-02 / SF-246-27 — Tests `MediationFamilialePrefillRules`. */
import {
  MediationFamilialePrefillRules,
  computePrefillCount,
  computeMotifSaisine,
} from './mediation-familiale-section-prefill-rules';

describe('MediationFamilialePrefillRules', () => {
  it('cas 0 — input vide', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('aspirationnel motifSaisineMediationDetecte dans whitelist → 1', () => {
    expect(
      computePrefillCount({ aiData: { motifSaisineMediationDetecte: 'AUTORITE_PARENTALE' } } as any),
    ).toBe(1);
  });

  it('aspirationnel motifSaisineMediationDetecte hors whitelist (ex DIVORCE_CONTENTIEUX) → 0', () => {
    // SF-246-27 : whitelist stricte — codes non reconnus retournent null
    expect(
      computePrefillCount({ aiData: { motifSaisineMediationDetecte: 'DIVORCE_CONTENTIEUX' } } as any),
    ).toBe(0);
  });

  it('aspirationnel motifSaisineMediationDetecte vide → 0', () => {
    expect(computePrefillCount({ aiData: { motifSaisineMediationDetecte: '' } } as any)).toBe(0);
  });

  // SF-246-27 : source réelle motifSaisineMediationDetected
  it('SF-246-27 : lit motifSaisineMediationDetected (source réelle, prioritaire)', () => {
    expect(
      computeMotifSaisine({ aiData: { motifSaisineMediationDetected: 'DROIT_VISITE' } } as any),
    ).toBe('DROIT_VISITE');
    expect(
      computePrefillCount({ aiData: { motifSaisineMediationDetected: 'DROIT_VISITE' } } as any),
    ).toBe(1);
  });

  it('SF-246-27 : motifSaisineMediationDetected hors whitelist → null', () => {
    expect(
      computeMotifSaisine({ aiData: { motifSaisineMediationDetected: 'SUCCESSION' } } as any),
    ).toBeNull();
    expect(
      computePrefillCount({ aiData: { motifSaisineMediationDetected: 'SUCCESSION' } } as any),
    ).toBe(0);
  });

  it('SF-246-27 : tous les codes de la whitelist sont acceptés', () => {
    for (const code of ['AUTORITE_PARENTALE', 'CONTRIBUTION_ENTRETIEN', 'DROIT_VISITE', 'RESIDENCE', 'AUTRE']) {
      expect(computeMotifSaisine({ aiData: { motifSaisineMediationDetected: code } } as any)).toBe(code);
    }
  });

  it('SF-246-27 : normalise en majuscules', () => {
    expect(computeMotifSaisine({ aiData: { motifSaisineMediationDetected: 'residence' } } as any)).toBe('RESIDENCE');
  });

  it('surface', () => {
    expect(MediationFamilialePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
