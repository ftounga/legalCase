import { TravailExtractedData } from '../../core/models/case-analysis.model';
import {
  computeAdhesionSalarie,
  computeCspPropose,
  computeDateRemise,
  computeDocumentInformationRemis,
  computeEffectifEntreprise,
  computePrefillCount,
  computeSalaireMensuelBrutEuros,
  CspCrpFrSectionPrefillRules,
} from './csp-crp-fr-section-prefill-rules';

describe('CspCrpFrSectionPrefillRules', () => {
  const FULL: TravailExtractedData = {
    cspEffectifEntreprise: 250,
    cspProposeDetail: true,
    cspDocumentRemis: true,
    cspDateRemise: '2026-04-01',
    cspAdhesion: true,
    cspSalaireMensuelBrut: 3000.0,
  };

  it('retourne 6 champs sur fixture FR complète', () => {
    expect(computePrefillCount({ aiData: FULL, workspaceCountry: 'FRANCE' })).toBe(6);
  });

  it('retourne 0 hors France (BE)', () => {
    expect(computePrefillCount({ aiData: FULL, workspaceCountry: 'BELGIQUE' })).toBe(0);
  });

  it('retourne 0 sans aiData', () => {
    expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
  });

  it('ignore effectif > 100 000', () => {
    const r = computeEffectifEntreprise({
      aiData: { ...FULL, cspEffectifEntreprise: 200_000 },
      workspaceCountry: 'FRANCE',
    });
    expect(r).toBeNull();
  });

  it('ignore effectif négatif', () => {
    expect(computeEffectifEntreprise({
      aiData: { ...FULL, cspEffectifEntreprise: -1 },
      workspaceCountry: 'FRANCE',
    })).toBeNull();
  });

  it('lit cspPropose', () => {
    expect(computeCspPropose({ aiData: FULL, workspaceCountry: 'FRANCE' })).toBe(true);
    expect(computeCspPropose({
      aiData: { ...FULL, cspProposeDetail: false },
      workspaceCountry: 'FRANCE',
    })).toBe(false);
  });

  it('lit documentInformationRemis', () => {
    expect(computeDocumentInformationRemis({
      aiData: FULL, workspaceCountry: 'FRANCE',
    })).toBe(true);
  });

  it('rejette date non ISO', () => {
    expect(computeDateRemise({
      aiData: { ...FULL, cspDateRemise: '01/04/2026' },
      workspaceCountry: 'FRANCE',
    })).toBeNull();
  });

  it('accepte date ISO YYYY-MM-DD', () => {
    expect(computeDateRemise({ aiData: FULL, workspaceCountry: 'FRANCE' }))
        .toBe('2026-04-01');
  });

  it('lit adhesionSalarie tri-état (null si absent)', () => {
    expect(computeAdhesionSalarie({ aiData: FULL, workspaceCountry: 'FRANCE' })).toBe(true);
    expect(computeAdhesionSalarie({
      aiData: { ...FULL, cspAdhesion: undefined },
      workspaceCountry: 'FRANCE',
    })).toBeNull();
  });

  it('rejette salaire ≤ 0', () => {
    expect(computeSalaireMensuelBrutEuros({
      aiData: { ...FULL, cspSalaireMensuelBrut: 0 },
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(computeSalaireMensuelBrutEuros({
      aiData: { ...FULL, cspSalaireMensuelBrut: -10 },
      workspaceCountry: 'FRANCE',
    })).toBeNull();
  });

  it('object module exporte les 7 fonctions', () => {
    expect(CspCrpFrSectionPrefillRules.computeEffectifEntreprise).toBeDefined();
    expect(CspCrpFrSectionPrefillRules.computeCspPropose).toBeDefined();
    expect(CspCrpFrSectionPrefillRules.computeDocumentInformationRemis).toBeDefined();
    expect(CspCrpFrSectionPrefillRules.computeDateRemise).toBeDefined();
    expect(CspCrpFrSectionPrefillRules.computeAdhesionSalarie).toBeDefined();
    expect(CspCrpFrSectionPrefillRules.computeSalaireMensuelBrutEuros).toBeDefined();
    expect(CspCrpFrSectionPrefillRules.computePrefillCount).toBeDefined();
  });
});
