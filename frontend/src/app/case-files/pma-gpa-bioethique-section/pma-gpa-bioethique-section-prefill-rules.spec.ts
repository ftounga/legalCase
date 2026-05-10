/** F-236 SF-236-02 — Tests `PmaGpaBioethiquePrefillRules`. */
import {
  PmaGpaBioethiquePrefillRules,
  computePrefillCount,
  parseDispositifFromIa,
} from './pma-gpa-bioethique-section-prefill-rules';

describe('PmaGpaBioethiquePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('parseDispositifFromIa', () => {
    expect(parseDispositifFromIa('PMA_RECONNAISSANCE_ANTICIPEE')).toBe('PMA_RECONNAISSANCE_ANTICIPEE');
    expect(parseDispositifFromIa('GPA_TRANSCRIPTION_ETAT_CIVIL')).toBe('GPA_TRANSCRIPTION_ETAT_CIVIL');
    expect(parseDispositifFromIa('INCONNU')).toBeNull();
  });

  it('cas N — 1/1', () => {
    expect(
      computePrefillCount({ aiData: { dispositifBioethiqueDetecte: 'GPA_TRANSCRIPTION_ETAT_CIVIL' } } as any),
    ).toBe(1);
  });

  it('surface', () => {
    expect(PmaGpaBioethiquePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
