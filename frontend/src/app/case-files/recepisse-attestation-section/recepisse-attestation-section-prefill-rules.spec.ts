import { RecepisseAttestationPrefillRules } from './recepisse-attestation-section-prefill-rules';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-16 — Tests du helper de pré-fill récépissé vs attestation (F-IM-32, FR).
 */
describe('RecepisseAttestationPrefillRules', () => {
  const fr = (aiData: any): PrefillCountInput => ({ aiData, workspaceCountry: 'FRANCE' });

  describe('computeDateExpiration', () => {
    it('retourne la date ISO depuis dateExpirationTitre', () => {
      const input = fr({ dateExpirationTitre: '2026-03-15' });
      expect(RecepisseAttestationPrefillRules.computeDateExpiration(input)).toBe('2026-03-15');
    });

    it('rejette un format non ISO', () => {
      const input = fr({ dateExpirationTitre: '15/03/2026' });
      expect(RecepisseAttestationPrefillRules.computeDateExpiration(input)).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = {
        aiData: { dateExpirationTitre: '2026-03-15' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(RecepisseAttestationPrefillRules.computeDateExpiration(input)).toBeNull();
    });
  });

  describe('computeTypeDocument', () => {
    it('retourne le type valide RECEPISSE', () => {
      const input = fr({ recepisseOuAttestationType: 'RECEPISSE' });
      expect(RecepisseAttestationPrefillRules.computeTypeDocument(input)).toBe('RECEPISSE');
    });

    it('retourne le type valide ATTESTATION_PROLONGATION', () => {
      const input = fr({ recepisseOuAttestationType: 'ATTESTATION_PROLONGATION' });
      expect(RecepisseAttestationPrefillRules.computeTypeDocument(input))
        .toBe('ATTESTATION_PROLONGATION');
    });

    it('retourne null pour une valeur inconnue', () => {
      const input = fr({ recepisseOuAttestationType: 'PASSEPORT' });
      expect(RecepisseAttestationPrefillRules.computeTypeDocument(input)).toBeNull();
    });

    it('retourne null si absent', () => {
      const input = fr({});
      expect(RecepisseAttestationPrefillRules.computeTypeDocument(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('compte 2 quand type + date présents (nominal)', () => {
      const input = fr({
        recepisseOuAttestationType: 'RECEPISSE',
        dateExpirationTitre: '2026-03-15',
      });
      expect(RecepisseAttestationPrefillRules.computePrefillCount(input)).toBe(2);
    });

    it('compte 1 quand seul le type est présent (partiel)', () => {
      const input = fr({ recepisseOuAttestationType: 'ATTESTATION_PROLONGATION' });
      expect(RecepisseAttestationPrefillRules.computePrefillCount(input)).toBe(1);
    });

    it('compte 0 quand aiData absent', () => {
      const input = fr(null);
      expect(RecepisseAttestationPrefillRules.computePrefillCount(input)).toBe(0);
    });

    it('compte 0 hors France', () => {
      const input: PrefillCountInput = {
        aiData: {
          recepisseOuAttestationType: 'RECEPISSE',
          dateExpirationTitre: '2026-03-15',
        },
        workspaceCountry: 'BELGIQUE',
      };
      expect(RecepisseAttestationPrefillRules.computePrefillCount(input)).toBe(0);
    });
  });
});
