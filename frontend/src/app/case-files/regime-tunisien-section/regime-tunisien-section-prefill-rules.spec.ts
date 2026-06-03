import { RegimeTunisienPrefillRules as Rules } from './regime-tunisien-section-prefill-rules';

describe('RegimeTunisienPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        regimeTunisienCategorie: 'ETUDIANT',
        regimeTunisienDureeSejour: 12,
        regimeTunisienTitreEnCours: true,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns categorie when whitelist (ETUDIANT/COMMERCANT/SALARIE/FAMILIAL/AUTRE)', () => {
    expect(Rules.computeCategorie({ aiData: { regimeTunisienCategorie: 'ETUDIANT' } })).toBe('ETUDIANT');
    expect(Rules.computeCategorie({ aiData: { regimeTunisienCategorie: 'COMMERCANT' } })).toBe('COMMERCANT');
    expect(Rules.computeCategorie({ aiData: { regimeTunisienCategorie: 'SALARIE' } })).toBe('SALARIE');
    expect(Rules.computeCategorie({ aiData: { regimeTunisienCategorie: 'FAMILIAL' } })).toBe('FAMILIAL');
    expect(Rules.computeCategorie({ aiData: { regimeTunisienCategorie: 'AUTRE' } })).toBe('AUTRE');
  });

  it('normalizes categorie case (etudiant → ETUDIANT)', () => {
    expect(Rules.computeCategorie({
      aiData: { regimeTunisienCategorie: 'etudiant' as unknown as 'ETUDIANT' },
    })).toBe('ETUDIANT');
  });

  it('rejects categorie outside whitelist', () => {
    expect(Rules.computeCategorie({
      aiData: { regimeTunisienCategorie: 'AVOCAT' as unknown as 'AUTRE' },
    })).toBeNull();
    expect(Rules.computeCategorie({ aiData: { regimeTunisienCategorie: null } })).toBeNull();
  });

  it('returns duree when finite and >= 0, truncates, rejects negative/non-numeric', () => {
    expect(Rules.computeDureeSejourEnvisageeMois({ aiData: { regimeTunisienDureeSejour: 12 } })).toBe(12);
    expect(Rules.computeDureeSejourEnvisageeMois({ aiData: { regimeTunisienDureeSejour: 12.7 } })).toBe(12);
    expect(Rules.computeDureeSejourEnvisageeMois({ aiData: { regimeTunisienDureeSejour: 0 } })).toBe(0);
    expect(Rules.computeDureeSejourEnvisageeMois({ aiData: { regimeTunisienDureeSejour: -3 } })).toBeNull();
    expect(Rules.computeDureeSejourEnvisageeMois({
      aiData: { regimeTunisienDureeSejour: '12' as unknown as number },
    })).toBeNull();
  });

  it('returns titreEnCours when boolean, rejects non-boolean', () => {
    expect(Rules.computeTitreEnCours({ aiData: { regimeTunisienTitreEnCours: true } })).toBe(true);
    expect(Rules.computeTitreEnCours({ aiData: { regimeTunisienTitreEnCours: false } })).toBe(false);
    expect(Rules.computeTitreEnCours({
      aiData: { regimeTunisienTitreEnCours: 'true' as unknown as boolean },
    })).toBeNull();
    expect(Rules.computeTitreEnCours({ aiData: { regimeTunisienTitreEnCours: null } })).toBeNull();
  });

  it('returns 1 when only categorie is present (partiel)', () => {
    const input = { aiData: { regimeTunisienCategorie: 'SALARIE' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 when all 3 prefill fields are present (complet)', () => {
    const input = {
      aiData: {
        regimeTunisienCategorie: 'ETUDIANT',
        regimeTunisienDureeSejour: 12,
        regimeTunisienTitreEnCours: false,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
