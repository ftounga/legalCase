/**
 * SF-216-24 — tests Jest helper `donation-entre-epoux-fr-section-prefill-rules`.
 */
import {
  DonationEntreEpouxFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './donation-entre-epoux-fr-section-prefill-rules';

describe('DonationEntreEpouxFrPrefillRules', () => {
  describe('prefillFromAi', () => {
    it('retourne null partout en BELGIQUE', () => {
      const v = prefillFromAi({
        aiData: {
          regimeMatrimonialDetecte: 'COMMUNAUTE_UNIVERSELLE',
          clauseAttributionIntegraleDetected: true,
          enfantsNonCommunsDetected: true,
          revocabiliteDetectee: false,
          bienDonnePrincipalType: 'IMMOBILIER',
        },
        workspaceCountry: 'BELGIQUE',
      });
      expect(v.regimeMatrimonial).toBeNull();
      expect(v.clauseAttributionIntegrale).toBeNull();
      expect(v.enfantsNonCommuns).toBeNull();
      expect(v.revocabiliteDetectee).toBeNull();
      expect(v.bienDonneType).toBeNull();
    });

    it('retourne null partout si aiData absent (FR)', () => {
      const v = prefillFromAi({ workspaceCountry: 'FRANCE' });
      expect(v.regimeMatrimonial).toBeNull();
      expect(v.clauseAttributionIntegrale).toBeNull();
      expect(v.enfantsNonCommuns).toBeNull();
      expect(v.revocabiliteDetectee).toBeNull();
      expect(v.bienDonneType).toBeNull();
    });

    it('retourne null partout si aiData null (FR)', () => {
      const v = prefillFromAi({ aiData: null, workspaceCountry: 'FRANCE' });
      expect(v.regimeMatrimonial).toBeNull();
      expect(v.clauseAttributionIntegrale).toBeNull();
    });

    it('mappe les 5 champs IA FR', () => {
      const v = prefillFromAi({
        aiData: {
          regimeMatrimonialDetecte: 'COMMUNAUTE_UNIVERSELLE',
          clauseAttributionIntegraleDetected: true,
          enfantsNonCommunsDetected: true,
          revocabiliteDetectee: false,
          bienDonnePrincipalType: 'IMMOBILIER',
        },
        workspaceCountry: 'FRANCE',
      });
      expect(v.regimeMatrimonial).toBe('COMMUNAUTE_UNIVERSELLE');
      expect(v.clauseAttributionIntegrale).toBe(true);
      expect(v.enfantsNonCommuns).toBe(true);
      expect(v.revocabiliteDetectee).toBe(false);
      expect(v.bienDonneType).toBe('IMMOBILIER');
    });

    it('rejette les régimes / biens hors whitelist', () => {
      const v = prefillFromAi({
        aiData: {
          regimeMatrimonialDetecte: 'REGIME_BIDON',
          bienDonnePrincipalType: 'CRYPTO',
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.regimeMatrimonial).toBeNull();
      expect(v.bienDonneType).toBeNull();
    });

    it('ignore les non-boolean', () => {
      const v = prefillFromAi({
        aiData: {
          clauseAttributionIntegraleDetected: undefined,
          enfantsNonCommunsDetected: null,
          revocabiliteDetectee: 'non' as any,
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.clauseAttributionIntegrale).toBeNull();
      expect(v.enfantsNonCommuns).toBeNull();
      expect(v.revocabiliteDetectee).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('retourne 0 hors France', () => {
      expect(
        computePrefillCount({
          aiData: {
            regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE',
            clauseAttributionIntegraleDetected: true,
          },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });

    it('retourne 0 si rien détecté (FR)', () => {
      expect(computePrefillCount({ workspaceCountry: 'FRANCE' })).toBe(0);
      expect(
        computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' }),
      ).toBe(0);
    });

    it('compte chaque champ détecté (FR)', () => {
      expect(
        computePrefillCount({
          aiData: { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
      expect(
        computePrefillCount({
          aiData: {
            regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE',
            clauseAttributionIntegraleDetected: false,
            enfantsNonCommunsDetected: true,
            revocabiliteDetectee: false,
            bienDonnePrincipalType: 'NUMERAIRE',
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(5);
    });
  });

  it('expose un objet utilitaire stable', () => {
    expect(DonationEntreEpouxFrPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(DonationEntreEpouxFrPrefillRules.prefillFromAi).toBe(prefillFromAi);
  });
});
