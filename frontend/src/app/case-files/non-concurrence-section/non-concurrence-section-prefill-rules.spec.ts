import {
  NonConcurrenceSectionPrefillRules,
  NonConcurrencePrefillInput,
  computePrefillCount,
  computeSalaireMensuelBrutEur,
  computeClausePresenteContrat,
  computeDureeMois,
  computeLimiteDureeDefinie,
  computeTerritoireDescription,
  computeLimiteTerritoireDefini,
  computeContrepartieMontantEur,
  computeContrepartieFinancierePresente,
} from './non-concurrence-section-prefill-rules';

describe('NonConcurrenceSectionPrefillRules', () => {
  const fullAi: NonConcurrencePrefillInput['aiData'] = {
    salaireBrutMensuel: 3000,
    clauseNonConcurrenceDetectee: true,
    nonConcurrenceDureeMois: 24,
    nonConcurrenceZoneGeographique: 'France métropolitaine',
    nonConcurrenceContrepartieMontantEur: 900,
  };

  // --- computePrefillCount : cas 0 / partiel / nominal -----------------------

  it('cas 0 — non-FRANCE retourne 0', () => {
    expect(computePrefillCount({ aiData: fullAi, workspaceCountry: 'BELGIQUE' })).toBe(0);
  });

  it('cas 0 — input vide (FRANCE) retourne 0', () => {
    expect(computePrefillCount({ workspaceCountry: 'FRANCE' })).toBe(0);
    expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
  });

  it('cas partiel — durée + salaire seuls retourne 3 (salaire + durée + limiteDuree dérivé)', () => {
    expect(computePrefillCount({
      aiData: { salaireBrutMensuel: 2500, nonConcurrenceDureeMois: 12 },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('cas nominal — clause complète retourne 8', () => {
    expect(computePrefillCount({ aiData: fullAi, workspaceCountry: 'FRANCE' })).toBe(8);
  });

  // --- Champs valeur ---------------------------------------------------------

  it('computeSalaireMensuelBrutEur — > 0 OK, ≤ 0 et BE → null', () => {
    expect(computeSalaireMensuelBrutEur({ aiData: { salaireBrutMensuel: 3000 }, workspaceCountry: 'FRANCE' })).toBe(3000);
    expect(computeSalaireMensuelBrutEur({ aiData: { salaireBrutMensuel: 0 }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeSalaireMensuelBrutEur({ aiData: { salaireBrutMensuel: 3000 }, workspaceCountry: 'BELGIQUE' })).toBeNull();
  });

  it('computeDureeMois — plage [0, 600], hors plage → null', () => {
    expect(computeDureeMois({ aiData: { nonConcurrenceDureeMois: 24 }, workspaceCountry: 'FRANCE' })).toBe(24);
    expect(computeDureeMois({ aiData: { nonConcurrenceDureeMois: -3 }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeDureeMois({ aiData: { nonConcurrenceDureeMois: 720 }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeDureeMois({ aiData: { nonConcurrenceDureeMois: 0 }, workspaceCountry: 'FRANCE' })).toBe(0);
  });

  it('computeTerritoireDescription — texte non vide OK, vide/blanc → null', () => {
    expect(computeTerritoireDescription({ aiData: { nonConcurrenceZoneGeographique: ' Lyon ' }, workspaceCountry: 'FRANCE' })).toBe('Lyon');
    expect(computeTerritoireDescription({ aiData: { nonConcurrenceZoneGeographique: '   ' }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeTerritoireDescription({ aiData: {}, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeContrepartieMontantEur — > 0 OK, ≤ 0 → null', () => {
    expect(computeContrepartieMontantEur({ aiData: { nonConcurrenceContrepartieMontantEur: 900 }, workspaceCountry: 'FRANCE' })).toBe(900);
    expect(computeContrepartieMontantEur({ aiData: { nonConcurrenceContrepartieMontantEur: 0 }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeContrepartieMontantEur({ aiData: { nonConcurrenceContrepartieMontantEur: -10 }, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeClausePresenteContrat — booléen direct, non booléen → null', () => {
    expect(computeClausePresenteContrat({ aiData: { clauseNonConcurrenceDetectee: true }, workspaceCountry: 'FRANCE' })).toBe(true);
    expect(computeClausePresenteContrat({ aiData: { clauseNonConcurrenceDetectee: false }, workspaceCountry: 'FRANCE' })).toBe(false);
    expect(computeClausePresenteContrat({ aiData: {}, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  // --- Booléens dérivés ------------------------------------------------------

  it('computeLimiteDureeDefinie — true si durée détectée, null sinon', () => {
    expect(computeLimiteDureeDefinie({ aiData: { nonConcurrenceDureeMois: 24 }, workspaceCountry: 'FRANCE' })).toBe(true);
    expect(computeLimiteDureeDefinie({ aiData: { nonConcurrenceDureeMois: 720 }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeLimiteDureeDefinie({ aiData: {}, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeLimiteTerritoireDefini — true si zone non vide, null sinon', () => {
    expect(computeLimiteTerritoireDefini({ aiData: { nonConcurrenceZoneGeographique: 'Paris' }, workspaceCountry: 'FRANCE' })).toBe(true);
    expect(computeLimiteTerritoireDefini({ aiData: { nonConcurrenceZoneGeographique: '' }, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeContrepartieFinancierePresente — true si montant détecté, null sinon', () => {
    expect(computeContrepartieFinancierePresente({ aiData: { nonConcurrenceContrepartieMontantEur: 900 }, workspaceCountry: 'FRANCE' })).toBe(true);
    expect(computeContrepartieFinancierePresente({ aiData: { nonConcurrenceContrepartieMontantEur: 0 }, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('tous les compute* retournent null hors FRANCE', () => {
    const be: NonConcurrencePrefillInput = { aiData: fullAi, workspaceCountry: 'BELGIQUE' };
    expect(computeSalaireMensuelBrutEur(be)).toBeNull();
    expect(computeClausePresenteContrat(be)).toBeNull();
    expect(computeDureeMois(be)).toBeNull();
    expect(computeLimiteDureeDefinie(be)).toBeNull();
    expect(computeTerritoireDescription(be)).toBeNull();
    expect(computeLimiteTerritoireDefini(be)).toBeNull();
    expect(computeContrepartieMontantEur(be)).toBeNull();
    expect(computeContrepartieFinancierePresente(be)).toBeNull();
  });

  it('expose barrel', () => {
    expect(NonConcurrenceSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(NonConcurrenceSectionPrefillRules.computeDureeMois).toBe(computeDureeMois);
    expect(NonConcurrenceSectionPrefillRules.computeContrepartieMontantEur).toBe(computeContrepartieMontantEur);
  });
});
