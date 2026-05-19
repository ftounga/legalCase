import {
  ImmigrationRecoursPrefillRules as Rules,
  computeRecoursType,
  computeDateNotification,
  computeNomRequerant,
  computePrenomRequerant,
  computeNationalite,
  computeDateDecision,
  computeReferenceDecision,
  computePrefillCount,
  VALID_RECOURS_CODES,
} from './immigration-recours-section-prefill-rules';

describe('ImmigrationRecoursPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('retourne 0 quand typeRecoursCode inconnu', () => {
      expect(computePrefillCount({ aiData: { typeRecoursCode: 'BOGUS' } })).toBe(0);
    });

    it('retourne 0 quand date malformée', () => {
      expect(computePrefillCount({ aiData: { dateNotificationDecisionContestee: 'not-a-date' } })).toBe(0);
    });

    it('retourne 1 avec recoursType seul', () => {
      expect(computePrefillCount({ aiData: { typeRecoursCode: 'RECOURS_GRACIEUX_PREFET' } })).toBe(1);
    });

    it('retourne 1 avec dateNotification seule', () => {
      expect(computePrefillCount({ aiData: { dateNotificationDecisionContestee: '2026-01-15' } })).toBe(1);
    });

    it('retourne 2 avec recoursType + dateNotification', () => {
      expect(computePrefillCount({
        aiData: {
          typeRecoursCode: 'RECOURS_CONTENTIEUX_TA',
          dateNotificationDecisionContestee: '2026-03-01',
        },
      })).toBe(2);
    });

    it('retourne 5 avec identités seules (SF-246-16)', () => {
      expect(computePrefillCount({
        aiData: {
          nomRequerant: 'Dupont',
          prenomRequerant: 'Marie',
          nationalite: 'Algérienne',
          dateDecisionContestee: '2024-05-15',
          referenceDecision: 'n°2024-PRF-001',
        },
      })).toBe(5);
    });

    it('cas nominal — 7 champs retourne 7 (SF-246-16)', () => {
      expect(computePrefillCount({
        aiData: {
          typeRecoursCode: 'RECOURS_GRACIEUX_PREFET',
          dateNotificationDecisionContestee: '2026-01-10',
          nomRequerant: 'Dupont',
          prenomRequerant: 'Marie',
          nationalite: 'Algérienne',
          dateDecisionContestee: '2024-05-15',
          referenceDecision: 'n°2024-PRF-001',
        },
      })).toBe(7);
    });

    it('champs vides ou null ne comptent pas', () => {
      expect(computePrefillCount({
        aiData: {
          nomRequerant: '',
          prenomRequerant: null,
          nationalite: '',
          referenceDecision: null,
        },
      })).toBe(0);
    });
  });

  describe('computeRecoursType', () => {
    it('accepte tous les codes valides (case-insensitive)', () => {
      for (const c of VALID_RECOURS_CODES) {
        expect(computeRecoursType({ aiData: { typeRecoursCode: c } })).toBe(c);
        expect(computeRecoursType({ aiData: { typeRecoursCode: c.toLowerCase() } })).toBe(c);
      }
    });

    it('retourne null pour un code inconnu', () => {
      expect(computeRecoursType({ aiData: { typeRecoursCode: 'BOGUS' } })).toBeNull();
    });

    it('retourne null si absent', () => {
      expect(computeRecoursType({})).toBeNull();
      expect(computeRecoursType({ aiData: null })).toBeNull();
    });
  });

  describe('computeDateNotification', () => {
    it('retourne la date si format ISO valide', () => {
      expect(computeDateNotification({ aiData: { dateNotificationDecisionContestee: '2026-01-15' } })).toBe('2026-01-15');
    });

    it('retourne null si date malformée', () => {
      expect(computeDateNotification({ aiData: { dateNotificationDecisionContestee: 'demain' } })).toBeNull();
      expect(computeDateNotification({ aiData: { dateNotificationDecisionContestee: '15/01/2026' } })).toBeNull();
    });

    it('retourne null si absent', () => {
      expect(computeDateNotification({})).toBeNull();
    });
  });

  describe('computeNomRequerant (SF-246-16)', () => {
    it('retourne null si absent ou vide', () => {
      expect(computeNomRequerant({})).toBeNull();
      expect(computeNomRequerant({ aiData: { nomRequerant: '' } })).toBeNull();
      expect(computeNomRequerant({ aiData: { nomRequerant: null } })).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeNomRequerant({ aiData: { nomRequerant: 'Dupont' } })).toBe('Dupont');
    });
  });

  describe('computePrenomRequerant (SF-246-16)', () => {
    it('retourne null si absent', () => {
      expect(computePrenomRequerant({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computePrenomRequerant({ aiData: { prenomRequerant: 'Marie' } })).toBe('Marie');
    });
  });

  describe('computeNationalite (SF-246-16)', () => {
    it('retourne null si absent ou vide', () => {
      expect(computeNationalite({})).toBeNull();
      expect(computeNationalite({ aiData: { nationalite: '' } })).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeNationalite({ aiData: { nationalite: 'Algérienne' } })).toBe('Algérienne');
    });
  });

  describe('computeDateDecision (SF-246-16)', () => {
    it('retourne null si absent', () => {
      expect(computeDateDecision({})).toBeNull();
    });

    it('retourne null si format non ISO', () => {
      expect(computeDateDecision({ aiData: { dateDecisionContestee: 'demain' } })).toBeNull();
      expect(computeDateDecision({ aiData: { dateDecisionContestee: '15-01-2024' } })).toBeNull();
    });

    it('retourne la date si format ISO valide', () => {
      expect(computeDateDecision({ aiData: { dateDecisionContestee: '2024-05-15' } })).toBe('2024-05-15');
    });
  });

  describe('computeReferenceDecision (SF-246-16)', () => {
    it('retourne null si absent ou vide', () => {
      expect(computeReferenceDecision({})).toBeNull();
      expect(computeReferenceDecision({ aiData: { referenceDecision: '' } })).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeReferenceDecision({ aiData: { referenceDecision: 'n°2024-PRF-001' } })).toBe('n°2024-PRF-001');
    });
  });

  it('expose ImmigrationRecoursPrefillRules barrel', () => {
    expect(Rules.computePrefillCount).toBe(computePrefillCount);
    expect(Rules.computeNomRequerant).toBe(computeNomRequerant);
    expect(Rules.computeReferenceDecision).toBe(computeReferenceDecision);
  });
});
