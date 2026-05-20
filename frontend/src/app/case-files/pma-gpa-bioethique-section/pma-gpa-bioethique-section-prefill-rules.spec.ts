/** F-236 SF-236-02 / SF-246-27 — Tests `PmaGpaBioethiquePrefillRules`. */
import {
  PmaGpaBioethiquePrefillRules,
  computePrefillCount,
  parseDispositifFromIa,
  computeDatePma,
  computeDateReconnaissanceAnterieure,
  computeDateDon,
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

  it('cas N — dispositif seul = 1/4', () => {
    expect(
      computePrefillCount({ aiData: { dispositifBioethiqueDetecte: 'GPA_TRANSCRIPTION_ETAT_CIVIL' } } as any),
    ).toBe(1);
  });

  // SF-246-27 : 3 nouveaux champs dates PMA
  it('SF-246-27 : computeDatePma retourne la date ISO valide', () => {
    expect(computeDatePma({ aiData: { datePmaDetected: '2023-06-01' } } as any)).toBe('2023-06-01');
  });

  it('SF-246-27 : computeDatePma rejette date non-ISO', () => {
    expect(computeDatePma({ aiData: { datePmaDetected: '01/06/2023' } } as any)).toBeNull();
    expect(computeDatePma({ aiData: { datePmaDetected: '' } } as any)).toBeNull();
    expect(computeDatePma({})).toBeNull();
  });

  it('SF-246-27 : computeDateReconnaissanceAnterieure retourne la date ISO valide', () => {
    expect(computeDateReconnaissanceAnterieure({ aiData: { dateReconnaissanceAnterieurePmaDetected: '2023-05-10' } } as any))
      .toBe('2023-05-10');
  });

  it('SF-246-27 : computeDateDon retourne la date ISO valide', () => {
    expect(computeDateDon({ aiData: { dateDonGametesDetected: '2023-07-22' } } as any)).toBe('2023-07-22');
  });

  it('SF-246-27 : cas N nominal — 4/4 champs', () => {
    const input = {
      aiData: {
        dispositifBioethiqueDetecte: 'DON_GAMETES_ACCES_ORIGINES',
        datePmaDetected: '2023-06-01',
        dateReconnaissanceAnterieurePmaDetected: '2023-05-10',
        dateDonGametesDetected: '2023-07-22',
      },
    } as any;
    expect(computePrefillCount(input)).toBe(4);
  });

  it('SF-246-27 : 3 dates seules sans dispositif = 3/4', () => {
    const input = {
      aiData: {
        datePmaDetected: '2023-06-01',
        dateReconnaissanceAnterieurePmaDetected: '2023-05-10',
        dateDonGametesDetected: '2023-07-22',
      },
    } as any;
    expect(computePrefillCount(input)).toBe(3);
  });

  it('surface', () => {
    expect(PmaGpaBioethiquePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
