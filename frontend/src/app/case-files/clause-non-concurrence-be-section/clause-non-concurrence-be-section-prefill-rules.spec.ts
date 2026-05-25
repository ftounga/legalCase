import { ClauseNonConcurrenceBeSectionPrefillRules as R } from './clause-non-concurrence-be-section-prefill-rules';

/**
 * SF-213-01b — tests unitaires du helper pur (5 cas minimum imposés par mini-spec).
 * Garantit la parité runtime/static (`prefill-count-integrity.spec.ts`).
 */
describe('ClauseNonConcurrenceBeSectionPrefillRules', () => {

  describe('Gate workspaceCountry', () => {
    it('FRANCE → tous les compute* retournent null', () => {
      const input = {
        workspaceCountry: 'FRANCE',
        aiData: {
          salaireBrutAnnuel: 80000,
          clauseNonConcurrenceDureeMois: 6,
          clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT',
        },
      };
      expect(R.computeRemunerationAnnuelleBrute(input)).toBeNull();
      expect(R.computeDureeMois(input)).toBeNull();
      expect(R.computeZoneGeographique(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('workspaceCountry absent → tous null', () => {
      expect(R.computePrefillCount({ aiData: { salaireBrutAnnuel: 80000 } })).toBe(0);
    });
  });

  describe('aiData absent', () => {
    it('aiData null → tous null (0)', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: null };
      expect(R.computeRemunerationAnnuelleBrute(input)).toBeNull();
      expect(R.computeDureeMois(input)).toBeNull();
      expect(R.computeZoneGeographique(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData undefined → tous null', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: undefined };
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData {} → tous null', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: {} };
      expect(R.computePrefillCount(input)).toBe(0);
    });
  });

  describe('computeRemunerationAnnuelleBrute (positive number)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('valeur > 0 → retourne la valeur', () => {
      expect(R.computeRemunerationAnnuelleBrute({ ...base, aiData: { salaireBrutAnnuel: 80000 } })).toBe(80000);
    });

    it('valeur = 0 → null', () => {
      expect(R.computeRemunerationAnnuelleBrute({ ...base, aiData: { salaireBrutAnnuel: 0 } })).toBeNull();
    });

    it('valeur négative → null', () => {
      expect(R.computeRemunerationAnnuelleBrute({ ...base, aiData: { salaireBrutAnnuel: -1000 } })).toBeNull();
    });

    it('NaN / non-number → null', () => {
      expect(R.computeRemunerationAnnuelleBrute({ ...base, aiData: { salaireBrutAnnuel: NaN } })).toBeNull();
      expect(R.computeRemunerationAnnuelleBrute({ ...base, aiData: { salaireBrutAnnuel: '80000' as any } })).toBeNull();
    });
  });

  describe('computeDureeMois (entier 1-12)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('1 → 1 (borne basse)', () => {
      expect(R.computeDureeMois({ ...base, aiData: { clauseNonConcurrenceDureeMois: 1 } })).toBe(1);
    });

    it('12 → 12 (borne haute)', () => {
      expect(R.computeDureeMois({ ...base, aiData: { clauseNonConcurrenceDureeMois: 12 } })).toBe(12);
    });

    it('6 → 6 (cas nominal)', () => {
      expect(R.computeDureeMois({ ...base, aiData: { clauseNonConcurrenceDureeMois: 6 } })).toBe(6);
    });

    it('0 → null (hors borne)', () => {
      expect(R.computeDureeMois({ ...base, aiData: { clauseNonConcurrenceDureeMois: 0 } })).toBeNull();
    });

    it('13 → null (hors borne haute)', () => {
      expect(R.computeDureeMois({ ...base, aiData: { clauseNonConcurrenceDureeMois: 13 } })).toBeNull();
    });

    it('décimal → null', () => {
      expect(R.computeDureeMois({ ...base, aiData: { clauseNonConcurrenceDureeMois: 6.5 } })).toBeNull();
    });

    it('non-number → null', () => {
      expect(R.computeDureeMois({ ...base, aiData: { clauseNonConcurrenceDureeMois: '6' as any } })).toBeNull();
    });
  });

  describe('computeZoneGeographique (whitelist 3 valeurs)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('BELGIQUE_UNIQUEMENT → mapping exact', () => {
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT' } }))
        .toBe('BELGIQUE_UNIQUEMENT');
    });

    it('BELGIQUE_ET_ETRANGER → mapping exact', () => {
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: 'BELGIQUE_ET_ETRANGER' } }))
        .toBe('BELGIQUE_ET_ETRANGER');
    });

    it('NON_SPECIFIEE → mapping exact', () => {
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: 'NON_SPECIFIEE' } }))
        .toBe('NON_SPECIFIEE');
    });

    it('trim appliqué', () => {
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: '  BELGIQUE_UNIQUEMENT  ' } }))
        .toBe('BELGIQUE_UNIQUEMENT');
    });

    it('valeur hors whitelist → null', () => {
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: 'EUROPE' as any } })).toBeNull();
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: 'belgique_uniquement' as any } })).toBeNull();
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: '' } })).toBeNull();
    });

    it('non-string → null', () => {
      expect(R.computeZoneGeographique({ ...base, aiData: { clauseNonConcurrenceZone: 42 as any } })).toBeNull();
    });
  });

  describe('computePrefillCount (intégration)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('0 champ → 0', () => {
      expect(R.computePrefillCount({ ...base, aiData: {} })).toBe(0);
    });

    it('1 champ → 1', () => {
      expect(R.computePrefillCount({ ...base, aiData: { salaireBrutAnnuel: 80000 } })).toBe(1);
    });

    it('2 champs → 2', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: { salaireBrutAnnuel: 80000, clauseNonConcurrenceDureeMois: 6 },
      })).toBe(2);
    });

    it('3 champs → 3 (max théorique)', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: {
          salaireBrutAnnuel: 80000,
          clauseNonConcurrenceDureeMois: 6,
          clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT',
        },
      })).toBe(3);
    });

    it('activiteInternationaleProuvee n\'est PAS comptée (toujours 0 / false par défaut)', () => {
      // Même avec une valeur en aiData, le compteur ne dépasse jamais 3.
      const input = {
        ...base,
        aiData: {
          salaireBrutAnnuel: 80000,
          clauseNonConcurrenceDureeMois: 6,
          clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT',
          domaineActiviteInternational: true,
        },
      };
      expect(R.computePrefillCount(input)).toBe(3);
    });
  });
});
