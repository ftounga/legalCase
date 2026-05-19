import { PrescriptionBeLitigeTravailPrefillRules as Rules } from './prescription-be-litige-travail-section-prefill-rules';

describe('PrescriptionBeLitigeTravailPrefillRules', () => {
  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computeDateRupture({})).toBeNull();
    expect(Rules.computeTypeCreance({})).toBeNull();
  });

  it('returns 0 when dateRuptureContrat is malformed and motifRupture is unmapped', () => {
    const input = { aiData: { dateRuptureContrat: '15/03/2025', motifRupture: 'fin de CDD' } };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeTypeCreance(input)).toBeNull();
  });

  // ── Cas 1 : un seul champ pré-remplissable ────────────────────────────
  it('returns 1 when only dateRuptureContrat is set (ISO valid)', () => {
    const input = { aiData: { dateRuptureContrat: '2025-09-30' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateRupture(input)).toBe('2025-09-30');
    expect(Rules.computeTypeCreance(input)).toBeNull();
  });

  it('returns 1 when only motifRupture is mapped (date absente)', () => {
    const input = { aiData: { motifRupture: 'licenciement avec préavis' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeTypeCreance(input)).toBe('EX_CONTRAT_GENERAL');
  });

  // ── Cas 2 : nominal — les 2 champs pré-remplis ────────────────────────
  it('returns 2 nominal — dateRuptureContrat ISO + motifRupture mapped', () => {
    const input = {
      aiData: {
        dateRuptureContrat: '2025-06-15',
        motifRupture: 'Licenciement pour motif grave (faute grave invoquée)',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeDateRupture(input)).toBe('2025-06-15');
    expect(Rules.computeTypeCreance(input)).toBe('EX_CONTRAT_GENERAL');
  });

  // ── Cas mapping motifRupture complet ──────────────────────────────────
  describe('motifRupture → typeCreance mapping (couverture exhaustive)', () => {
    it('LICENCIEMENT → EX_CONTRAT_GENERAL', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'licenciement' } }))
        .toBe('EX_CONTRAT_GENERAL');
    });

    it('DEMISSION → EX_CONTRAT_GENERAL', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'Démission du salarié' } }))
        .toBe('EX_CONTRAT_GENERAL');
    });

    it('FAUTE GRAVE → EX_CONTRAT_GENERAL', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'Licenciement pour faute grave' } }))
        .toBe('EX_CONTRAT_GENERAL');
    });

    it('RCC → EX_CONTRAT_GENERAL', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'RCC chômage avec complément d\'entreprise' } }))
        .toBe('EX_CONTRAT_GENERAL');
    });

    it('RUPTURE_AMIABLE → EX_CONTRAT_GENERAL', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'Rupture amiable' } }))
        .toBe('EX_CONTRAT_GENERAL');
    });

    it('insensitive aux accents / casse', () => {
      // "démission" en minuscules accentué doit matcher la même règle que DEMISSION.
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'démission' } }))
        .toBe('EX_CONTRAT_GENERAL');
    });
  });

  // ── Cas mapping motif inconnu ─────────────────────────────────────────
  describe('motifRupture inconnu → null', () => {
    it('Fin de CDD → null (pas dans le mapping V1)', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'fin de CDD' } })).toBeNull();
    });

    it('Force majeure médicale → null', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 'Force majeure médicale' } })).toBeNull();
    });

    it('Chaîne vide / whitespace → null', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: '' } })).toBeNull();
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: '   ' } })).toBeNull();
    });

    it('motifRupture non string → null', () => {
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: null as unknown as string } })).toBeNull();
      expect(Rules.computeTypeCreance({ aiData: { motifRupture: 42 as unknown as string } })).toBeNull();
    });
  });

  // ── isoDateOrNull / date validation ───────────────────────────────────
  it('dateRuptureContrat invalide non ISO YYYY-MM-DD → null', () => {
    expect(Rules.computeDateRupture({ aiData: { dateRuptureContrat: '2025/09/30' } })).toBeNull();
    expect(Rules.computeDateRupture({ aiData: { dateRuptureContrat: '30-09-2025' } })).toBeNull();
    expect(Rules.computeDateRupture({ aiData: { dateRuptureContrat: '2025-13-45' } })).toBeNull();
    expect(Rules.computeDateRupture({ aiData: { dateRuptureContrat: '' } })).toBeNull();
    expect(Rules.computeDateRupture({ aiData: { dateRuptureContrat: null as unknown as string } })).toBeNull();
  });
});
