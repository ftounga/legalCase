import {
  DiscriminationSectionPrefillRules,
  computeDiscriminationMotif,
  computeDiscriminationContexte,
  computePrefillCount,
} from './discrimination-section-prefill-rules';

describe('DiscriminationSectionPrefillRules', () => {
  it('cas 0 — input vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('cas M — salaire <= 0 retourne 0 pour ce champ', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 0 } })).toBe(0);
  });

  it('cas N — salaire valide retourne au moins 1', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 2500 } })).toBeGreaterThanOrEqual(1);
  });

  // SF-246-21 — computeDiscriminationMotif
  describe('computeDiscriminationMotif (SF-246-21)', () => {
    it('code absent → null', () => {
      expect(computeDiscriminationMotif({ aiData: {} })).toBeNull();
    });

    it('code inconnu → null', () => {
      expect(computeDiscriminationMotif({ aiData: { discriminationMotif: 'INCONNUE' } })).toBeNull();
    });

    it('ORIGINE → ORIGINE_ETHNIQUE', () => {
      expect(computeDiscriminationMotif({ aiData: { discriminationMotif: 'ORIGINE' } })).toBe('ORIGINE_ETHNIQUE');
    });

    it('ACTIVITES_SYNDICALES → OPINIONS_POLITIQUES', () => {
      expect(computeDiscriminationMotif({ aiData: { discriminationMotif: 'ACTIVITES_SYNDICALES' } })).toBe('OPINIONS_POLITIQUES');
    });

    it('AGE → AGE', () => {
      expect(computeDiscriminationMotif({ aiData: { discriminationMotif: 'AGE' } })).toBe('AGE');
    });

    it('tolère la casse mixte', () => {
      expect(computeDiscriminationMotif({ aiData: { discriminationMotif: 'handicap' } })).toBe('ETAT_SANTE_HANDICAP');
    });
  });

  // SF-246-21 — computeDiscriminationContexte
  describe('computeDiscriminationContexte (SF-246-21)', () => {
    it('code absent → null', () => {
      expect(computeDiscriminationContexte({ aiData: {} })).toBeNull();
    });

    it('code inconnu → null', () => {
      expect(computeDiscriminationContexte({ aiData: { discriminationContexte: 'RUPTURE' } })).toBeNull();
    });

    it('LICENCIEMENT → LICENCIEMENT', () => {
      expect(computeDiscriminationContexte({ aiData: { discriminationContexte: 'LICENCIEMENT' } })).toBe('LICENCIEMENT');
    });

    it('REFUS_EMBAUCHE → EMBAUCHE_REFUSEE', () => {
      expect(computeDiscriminationContexte({ aiData: { discriminationContexte: 'REFUS_EMBAUCHE' } })).toBe('EMBAUCHE_REFUSEE');
    });

    it('REMUNERATION_INFERIEURE → DIFFERENCE_SALARIALE', () => {
      expect(computeDiscriminationContexte({ aiData: { discriminationContexte: 'REMUNERATION_INFERIEURE' } })).toBe('DIFFERENCE_SALARIALE');
    });
  });

  // count max = 3
  it('tous les champs remplis → count = 3', () => {
    expect(computePrefillCount({
      aiData: {
        salaireBrutMensuel: 3000,
        discriminationMotif: 'SEXE',
        discriminationContexte: 'LICENCIEMENT',
      },
    })).toBe(3);
  });

  it('expose DiscriminationSectionPrefillRules barrel', () => {
    expect(DiscriminationSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
