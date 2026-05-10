/** F-236 SF-236-02 — Tests `PartageSuccessoralPrefillRules`. */
import {
  PartageSuccessoralPrefillRules,
  computePrefillCount,
  parseModeFromIa,
} from './partage-successoral-section-prefill-rules';

describe('PartageSuccessoralPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('parseMode aliases', () => {
    expect(parseModeFromIa('amiable')).toBe('PARTAGE_AMIABLE');
    expect(parseModeFromIa('JUDICIAIRE')).toBe('PARTAGE_JUDICIAIRE');
    expect(parseModeFromIa('xx')).toBeNull();
  });

  it('cas N — 3/3', () => {
    expect(
      computePrefillCount({
        aiData: {
          modePartageDemandeDetecte: 'AMIABLE',
          nombreCoheritiersDetecte: 3,
          dateOuvertureSuccessionDetectee: '2024-12-01',
        },
      } as any),
    ).toBe(3);
  });

  it('surface', () => {
    expect(PartageSuccessoralPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
