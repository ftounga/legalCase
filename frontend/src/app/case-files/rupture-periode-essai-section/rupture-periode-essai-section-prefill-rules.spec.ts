import {
  RupturePeriodeEssaiSectionPrefillRules,
} from './rupture-periode-essai-section-prefill-rules';

/**
 * SF-DT-38-02 : tests unitaires du helper de pré-fill IA F-DT-38.
 *
 * Vérifie les mappings depuis `TravailExtractedData` pour les 9 champs
 * pré-remplissables FRANCE (typeContrat, dateDebutContrat, dateRupture,
 * motifInvoque, discriminationInvoquee, grossesseAuMomentRupture,
 * arretAccidentTravailEnCours, conventionCollectiveApplicable,
 * salaireMensuelBrut).
 */
describe('RupturePeriodeEssaiSectionPrefillRules', () => {

  function frInput(overrides: Partial<any> = {}) {
    return { aiData: { ...overrides }, workspaceCountry: 'FRANCE' };
  }

  function beInput(overrides: Partial<any> = {}) {
    return { aiData: { ...overrides }, workspaceCountry: 'BELGIQUE' };
  }

  // === typeContrat ===

  it('computeTypeContrat — CDI/CDD/INTERIM whitelist', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeTypeContrat(frInput({ typeContrat: 'CDI' }))).toBe('CDI');
    expect(RupturePeriodeEssaiSectionPrefillRules.computeTypeContrat(frInput({ typeContrat: 'CDD' }))).toBe('CDD');
    expect(RupturePeriodeEssaiSectionPrefillRules.computeTypeContrat(frInput({ typeContrat: 'INTERIM' }))).toBe('INTERIM');
    expect(RupturePeriodeEssaiSectionPrefillRules.computeTypeContrat(frInput({ typeContrat: 'AUTRE' }))).toBeNull();
  });

  it('computeTypeContrat — null pour BE', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeTypeContrat(beInput({ typeContrat: 'CDI' }))).toBeNull();
  });

  it('computeTypeContrat — case-insensitive', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeTypeContrat(frInput({ typeContrat: 'cdi' }))).toBe('CDI');
  });

  // === dateDebutContrat ===

  it('computeDateDebutContrat — date ISO valide', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDateDebutContrat(frInput({ dateEntree: '2025-01-15' }))).toBe('2025-01-15');
  });

  it('computeDateDebutContrat — format invalide → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDateDebutContrat(frInput({ dateEntree: '15/01/2025' }))).toBeNull();
  });

  it('computeDateDebutContrat — absent → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDateDebutContrat(frInput())).toBeNull();
  });

  it('computeDateDebutContrat — BE → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDateDebutContrat(beInput({ dateEntree: '2025-01-15' }))).toBeNull();
  });

  // === dateRupture ===

  it('computeDateRupture — dateLicenciement ISO', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDateRupture(frInput({ dateLicenciement: '2025-04-10' }))).toBe('2025-04-10');
  });

  it('computeDateRupture — BE → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDateRupture(beInput({ dateLicenciement: '2025-04-10' }))).toBeNull();
  });

  // === motifInvoque ===

  it('computeMotifInvoque — texte non vide', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeMotifInvoque(frInput({ motifLicenciement: 'Insuffisance résultats' }))).toBe('Insuffisance résultats');
  });

  it('computeMotifInvoque — texte vide → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeMotifInvoque(frInput({ motifLicenciement: '   ' }))).toBeNull();
  });

  // === discriminationInvoquee ===

  it('computeDiscriminationInvoquee — DISCRIMINATION → AUTRE', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDiscriminationInvoquee(frInput({ motifNullitePressenti: 'DISCRIMINATION' }))).toBe('AUTRE');
  });

  it('computeDiscriminationInvoquee — SYNDICAL → SYNDICAL', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDiscriminationInvoquee(frInput({ motifNullitePressenti: 'SYNDICAL' }))).toBe('SYNDICAL');
  });

  it('computeDiscriminationInvoquee — HARCELEMENT_SEXUEL → SEXE', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDiscriminationInvoquee(frInput({ motifNullitePressenti: 'HARCELEMENT_SEXUEL' }))).toBe('SEXE');
  });

  it('computeDiscriminationInvoquee — ACCIDENT_MP → SANTE', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDiscriminationInvoquee(frInput({ motifNullitePressenti: 'ACCIDENT_MP' }))).toBe('SANTE');
  });

  it('computeDiscriminationInvoquee — MATERNITE_PATERNITE → null (traité par grossesse)', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeDiscriminationInvoquee(frInput({ motifNullitePressenti: 'MATERNITE_PATERNITE' }))).toBeNull();
  });

  // === grossesseAuMomentRupture ===

  it('computeGrossesseAuMomentRupture — MATERNITE_PATERNITE → true', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeGrossesseAuMomentRupture(frInput({ motifNullitePressenti: 'MATERNITE_PATERNITE' }))).toBe(true);
  });

  it('computeGrossesseAuMomentRupture — autre motif → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeGrossesseAuMomentRupture(frInput({ motifNullitePressenti: 'SYNDICAL' }))).toBeNull();
  });

  // === arretAccidentTravailEnCours ===

  it('computeArretAccidentTravail — atMpDetecte=true → true', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeArretAccidentTravail(frInput({ atMpDetecte: true }))).toBe(true);
  });

  it('computeArretAccidentTravail — atMpDetecte=false → false', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeArretAccidentTravail(frInput({ atMpDetecte: false }))).toBe(false);
  });

  // === conventionCollectiveApplicable ===

  it('computeConventionApplicable — CCN identifiée → true', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeConventionApplicable(frInput({ conventionCollective: 'IDCC_1486' }))).toBe(true);
  });

  it('computeConventionApplicable — CCN null → false', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeConventionApplicable(frInput({ conventionCollective: null }))).toBe(false);
  });

  it('computeConventionApplicable — absent → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeConventionApplicable(frInput())).toBeNull();
  });

  // === salaireMensuelBrut ===

  it('computeSalaireMensuelBrut — valeur > 0', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeSalaireMensuelBrut(frInput({ salaireBrutMensuel: 4500 }))).toBe(4500);
  });

  it('computeSalaireMensuelBrut — 0 ou négatif → null', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computeSalaireMensuelBrut(frInput({ salaireBrutMensuel: 0 }))).toBeNull();
    expect(RupturePeriodeEssaiSectionPrefillRules.computeSalaireMensuelBrut(frInput({ salaireBrutMensuel: -100 }))).toBeNull();
  });

  // === computePrefillCount ===

  it('computePrefillCount — 0 si BE', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computePrefillCount(beInput({
      typeContrat: 'CDI',
      dateEntree: '2025-01-01',
      dateLicenciement: '2025-04-10',
      motifLicenciement: 'x',
      salaireBrutMensuel: 3000,
    }))).toBe(0);
  });

  it('computePrefillCount — 0 si aucune donnée mappable', () => {
    expect(RupturePeriodeEssaiSectionPrefillRules.computePrefillCount(frInput())).toBe(0);
  });

  it('computePrefillCount — compte exact des champs présents', () => {
    const count = RupturePeriodeEssaiSectionPrefillRules.computePrefillCount(frInput({
      typeContrat: 'CDI',
      dateEntree: '2025-01-01',
      dateLicenciement: '2025-04-10',
      motifLicenciement: 'Insuffisance',
      motifNullitePressenti: 'MATERNITE_PATERNITE', // → grossesse=true, pas de discrimination
      atMpDetecte: false, // → false (compté)
      conventionCollective: 'IDCC_1486', // → true (compté)
      salaireBrutMensuel: 4500,
    }));
    // typeContrat + dateEntree + dateLicenciement + motifLicenciement
    // + grossesse + atMpDetecte (false compté) + conventionApplicable + salaire = 8
    expect(count).toBe(8);
  });

  it('computePrefillCount — discriminationInvoquee compte 1 (sans grossesse)', () => {
    const count = RupturePeriodeEssaiSectionPrefillRules.computePrefillCount(frInput({
      motifNullitePressenti: 'SYNDICAL',
    }));
    // discriminationInvoquee = SYNDICAL → 1
    expect(count).toBe(1);
  });
});
