import { RegimeMayottePrefillRules as Rules } from './regime-mayotte-section-prefill-rules';

describe('RegimeMayottePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        mayotteTitreDelivreAMayotte: true,
        mayotteTypeTitre: 'VPF',
        mayotteProjetDeplacementMetropole: true,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns typeTitre when whitelist (VPF/SALARIE/ETUDIANT/RESIDENT/AUTRE)', () => {
    expect(Rules.computeTypeTitre({ aiData: { mayotteTypeTitre: 'VPF' } })).toBe('VPF');
    expect(Rules.computeTypeTitre({ aiData: { mayotteTypeTitre: 'SALARIE' } })).toBe('SALARIE');
    expect(Rules.computeTypeTitre({ aiData: { mayotteTypeTitre: 'ETUDIANT' } })).toBe('ETUDIANT');
    expect(Rules.computeTypeTitre({ aiData: { mayotteTypeTitre: 'RESIDENT' } })).toBe('RESIDENT');
    expect(Rules.computeTypeTitre({ aiData: { mayotteTypeTitre: 'AUTRE' } })).toBe('AUTRE');
  });

  it('normalizes typeTitre case (vpf → VPF)', () => {
    expect(Rules.computeTypeTitre({
      aiData: { mayotteTypeTitre: 'vpf' as unknown as 'VPF' },
    })).toBe('VPF');
  });

  it('rejects typeTitre outside whitelist', () => {
    expect(Rules.computeTypeTitre({
      aiData: { mayotteTypeTitre: 'PASSEPORT' as unknown as 'AUTRE' },
    })).toBeNull();
    expect(Rules.computeTypeTitre({ aiData: { mayotteTypeTitre: null } })).toBeNull();
  });

  it('returns titreDelivreAMayotte when boolean, rejects non-boolean', () => {
    expect(Rules.computeTitreDelivreAMayotte({ aiData: { mayotteTitreDelivreAMayotte: true } })).toBe(true);
    expect(Rules.computeTitreDelivreAMayotte({ aiData: { mayotteTitreDelivreAMayotte: false } })).toBe(false);
    expect(Rules.computeTitreDelivreAMayotte({
      aiData: { mayotteTitreDelivreAMayotte: 'true' as unknown as boolean },
    })).toBeNull();
    expect(Rules.computeTitreDelivreAMayotte({ aiData: { mayotteTitreDelivreAMayotte: null } })).toBeNull();
  });

  it('returns projetDeplacementMetropole when boolean, rejects non-boolean', () => {
    expect(Rules.computeProjetDeplacementMetropole({ aiData: { mayotteProjetDeplacementMetropole: true } })).toBe(true);
    expect(Rules.computeProjetDeplacementMetropole({ aiData: { mayotteProjetDeplacementMetropole: false } })).toBe(false);
    expect(Rules.computeProjetDeplacementMetropole({
      aiData: { mayotteProjetDeplacementMetropole: 'true' as unknown as boolean },
    })).toBeNull();
  });

  it('returns 1 when only typeTitre is present (partiel)', () => {
    const input = { aiData: { mayotteTypeTitre: 'SALARIE' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 when all 3 prefill fields are present (complet)', () => {
    const input = {
      aiData: {
        mayotteTitreDelivreAMayotte: true,
        mayotteTypeTitre: 'VPF',
        mayotteProjetDeplacementMetropole: false,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
