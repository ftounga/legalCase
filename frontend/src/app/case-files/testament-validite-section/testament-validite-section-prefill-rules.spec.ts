/** F-236 SF-236-02 — Tests `TestamentValiditePrefillRules`. */
import {
  TestamentValiditePrefillRules,
  computePrefillCount,
  parseFormeFromIa,
} from './testament-validite-section-prefill-rules';

describe('TestamentValiditePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('parseFormeFromIa — alias court et long', () => {
    expect(parseFormeFromIa('OLOGRAPHE')).toBe('TESTAMENT_OLOGRAPHE');
    expect(parseFormeFromIa('TESTAMENT_AUTHENTIQUE')).toBe('TESTAMENT_AUTHENTIQUE');
    expect(parseFormeFromIa('BIZARRE')).toBeNull();
  });

  it('cas N — 4/4', () => {
    expect(
      computePrefillCount({
        aiData: {
          formeTestamentDetectee: 'OLOGRAPHE',
          dateRedactionTestamentDetectee: '2022-01-15',
          saineDEspritTestateurDetected: true,
          legsExcedeQuotiteDisponibleDetected: false,
        },
      } as any),
    ).toBe(4);
  });

  it('surface', () => {
    expect(TestamentValiditePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
