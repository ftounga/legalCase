import { VpfLiensPersonnelsPrefillRules as Rules } from './vpf-liens-personnels-section-prefill-rules';

describe('VpfLiensPersonnelsPrefillRules', () => {
  it('getPrefillCount returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('getPrefillCount returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        aesDureePresenceMois: 120,
        clientMineurDetecte: true,
        aesDureeScolaritePlusAncienEnfantAnnees: 5,
        vpfNiveauIntegration: 'FORT',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('getPrefillCount returns 1 (partiel) when only dureeResidence present', () => {
    const input = { aiData: { aesDureePresenceMois: 120 } };
    expect(Rules.computeDureeResidenceFranceMois(input)).toBe(120);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('getPrefillCount returns 4 (nominal) when all 4 IA signals present', () => {
    const input = {
      aiData: {
        aesDureePresenceMois: 120,
        clientMineurDetecte: true,
        aesDureeScolaritePlusAncienEnfantAnnees: 5,
        vpfNiveauIntegration: 'FORT',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('truncates and rejects negative / non-finite duree', () => {
    expect(Rules.computeDureeResidenceFranceMois({ aiData: { aesDureePresenceMois: 120.8 } })).toBe(120);
    expect(Rules.computeDureeResidenceFranceMois({ aiData: { aesDureePresenceMois: -3 } })).toBeNull();
    expect(Rules.computeDureeResidenceFranceMois({
      aiData: { aesDureePresenceMois: '120' as unknown as number },
    })).toBeNull();
    expect(Rules.computeDureeResidenceFranceMois({ aiData: { aesDureePresenceMois: null } })).toBeNull();
  });

  it('computeEntreeEnFranceMineur only true when clientMineurDetecte === true', () => {
    expect(Rules.computeEntreeEnFranceMineur({ aiData: { clientMineurDetecte: true } })).toBe(true);
    expect(Rules.computeEntreeEnFranceMineur({ aiData: { clientMineurDetecte: false } })).toBeNull();
    expect(Rules.computeEntreeEnFranceMineur({ aiData: {} })).toBeNull();
  });

  it('computeEnfantsEnFrance true only when scolarite enfant > 0', () => {
    expect(Rules.computeEnfantsEnFrance({ aiData: { aesDureeScolaritePlusAncienEnfantAnnees: 3 } })).toBe(true);
    expect(Rules.computeEnfantsEnFrance({ aiData: { aesDureeScolaritePlusAncienEnfantAnnees: 0 } })).toBeNull();
    expect(Rules.computeEnfantsEnFrance({ aiData: { aesDureeScolaritePlusAncienEnfantAnnees: -1 } })).toBeNull();
    expect(Rules.computeEnfantsEnFrance({ aiData: {} })).toBeNull();
  });

  it('computeNiveauIntegration whitelists FORT/MOYEN/FAIBLE + normalizes case', () => {
    expect(Rules.computeNiveauIntegration({ aiData: { vpfNiveauIntegration: 'FORT' } })).toBe('FORT');
    expect(Rules.computeNiveauIntegration({ aiData: { vpfNiveauIntegration: 'moyen' } })).toBe('MOYEN');
    expect(Rules.computeNiveauIntegration({ aiData: { vpfNiveauIntegration: ' faible ' } })).toBe('FAIBLE');
  });

  it('computeNiveauIntegration rejects values outside whitelist / non-string', () => {
    expect(Rules.computeNiveauIntegration({ aiData: { vpfNiveauIntegration: 'EXCELLENT' } })).toBeNull();
    expect(Rules.computeNiveauIntegration({ aiData: { vpfNiveauIntegration: null } })).toBeNull();
    expect(Rules.computeNiveauIntegration({ aiData: { vpfNiveauIntegration: 3 as unknown as string } })).toBeNull();
  });

  it('does NOT count viePriveeFamilialeDetectee alone (signal global, pas champ saisissable)', () => {
    expect(Rules.computePrefillCount({ aiData: { viePriveeFamilialeDetectee: true } })).toBe(0);
  });
});
