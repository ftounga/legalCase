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
  computeDatePriseEffet,
  computeSecteurActivite,
} from './non-concurrence-section-prefill-rules';

describe('NonConcurrenceSectionPrefillRules', () => {
  /** SF-246-13 : clause complète avec les 2 nouveaux champs */
  const fullAi: NonConcurrencePrefillInput['aiData'] = {
    salaireBrutMensuel: 3000,
    clauseNonConcurrenceDetectee: true,
    nonConcurrenceDureeMois: 24,
    nonConcurrenceZoneGeographique: 'France métropolitaine',
    nonConcurrenceContrepartieMontantEur: 900,
    nonConcurrenceDatePriseEffet: '2026-03-31',
    nonConcurrenceSecteurActivite: 'INFORMATIQUE',
  };

  /** SF-246-02 seule (sans les 2 nouveaux champs) — doit retourner 8. */
  const fullAi8: NonConcurrencePrefillInput['aiData'] = {
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

  it('cas partiel — clause SF-246-02 sans date/secteur retourne 8', () => {
    expect(computePrefillCount({ aiData: fullAi8, workspaceCountry: 'FRANCE' })).toBe(8);
  });

  it('cas nominal — clause complète (SF-246-13) retourne 10', () => {
    expect(computePrefillCount({ aiData: fullAi, workspaceCountry: 'FRANCE' })).toBe(10);
  });

  it('cas BELGIQUE — toujours 0 même avec clause complète', () => {
    expect(computePrefillCount({ aiData: fullAi, workspaceCountry: 'BELGIQUE' })).toBe(0);
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
    // SF-246-13 : idem nouveaux champs
    expect(computeDatePriseEffet(be)).toBeNull();
    expect(computeSecteurActivite(be)).toBeNull();
  });

  // --- SF-246-13 : computeDatePriseEffet ------------------------------------

  it('computeDatePriseEffet — ISO valide OK', () => {
    expect(computeDatePriseEffet({ aiData: { nonConcurrenceDatePriseEffet: '2026-03-31' }, workspaceCountry: 'FRANCE' })).toBe('2026-03-31');
  });

  it('computeDatePriseEffet — format non-ISO → null (défense en profondeur)', () => {
    expect(computeDatePriseEffet({ aiData: { nonConcurrenceDatePriseEffet: '31/03/2026' }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeDatePriseEffet({ aiData: { nonConcurrenceDatePriseEffet: '2026/03/31' }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeDatePriseEffet({ aiData: { nonConcurrenceDatePriseEffet: 'mars 2026' }, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeDatePriseEffet — null/vide → null', () => {
    expect(computeDatePriseEffet({ aiData: { nonConcurrenceDatePriseEffet: null }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeDatePriseEffet({ aiData: { nonConcurrenceDatePriseEffet: '' }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeDatePriseEffet({ aiData: {}, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeDatePriseEffet — hors FRANCE → null', () => {
    expect(computeDatePriseEffet({ aiData: { nonConcurrenceDatePriseEffet: '2026-03-31' }, workspaceCountry: 'BELGIQUE' })).toBeNull();
  });

  // --- SF-246-13 : computeSecteurActivite ------------------------------------

  it('computeSecteurActivite — codes valides OK (insensible à la casse)', () => {
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'INFORMATIQUE' }, workspaceCountry: 'FRANCE' })).toBe('INFORMATIQUE');
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'informatique' }, workspaceCountry: 'FRANCE' })).toBe('INFORMATIQUE');
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'COMMERCE' }, workspaceCountry: 'FRANCE' })).toBe('COMMERCE');
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'INDUSTRIE' }, workspaceCountry: 'FRANCE' })).toBe('INDUSTRIE');
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'SERVICES' }, workspaceCountry: 'FRANCE' })).toBe('SERVICES');
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'AUTRE' }, workspaceCountry: 'FRANCE' })).toBe('AUTRE');
  });

  it('computeSecteurActivite — code hors whitelist (ex. BTP) → null', () => {
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'BTP' }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'banque' }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'INCONNU' }, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeSecteurActivite — null/vide → null', () => {
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: null }, workspaceCountry: 'FRANCE' })).toBeNull();
    expect(computeSecteurActivite({ aiData: {}, workspaceCountry: 'FRANCE' })).toBeNull();
  });

  it('computeSecteurActivite — hors FRANCE → null', () => {
    expect(computeSecteurActivite({ aiData: { nonConcurrenceSecteurActivite: 'INFORMATIQUE' }, workspaceCountry: 'BELGIQUE' })).toBeNull();
  });

  it('expose barrel avec les nouveaux champs SF-246-13', () => {
    expect(NonConcurrenceSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(NonConcurrenceSectionPrefillRules.computeDureeMois).toBe(computeDureeMois);
    expect(NonConcurrenceSectionPrefillRules.computeContrepartieMontantEur).toBe(computeContrepartieMontantEur);
    expect(NonConcurrenceSectionPrefillRules.computeDatePriseEffet).toBe(computeDatePriseEffet);
    expect(NonConcurrenceSectionPrefillRules.computeSecteurActivite).toBe(computeSecteurActivite);
  });
});
