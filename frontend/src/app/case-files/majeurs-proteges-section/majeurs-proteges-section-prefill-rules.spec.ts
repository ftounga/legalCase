import { MajeursProtegesPrefillRules as Rules } from './majeurs-proteges-section-prefill-rules';

describe('MajeursProtegesPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: undefined })).toBe(0);
  });

  it('returns 0 when aiData is empty object', () => {
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when boolean fields are false (sémantique runtime : false ne surcharge pas)', () => {
    const input = {
      aiData: {
        altertationFacultesMentales: false,
        altertationFacultesPhysiques: false,
        certificatMedicalCirconstancieDetected: false,
        consentementPersonneAProtegerDetected: false,
        incapaciteGestionQuotidienneDetected: false,
        altertationGraveDetected: false,
        mandatPrealableSigneDetected: false,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 when enum fields contain invalid values', () => {
    const input = {
      aiData: {
        regimeProtectionDemande: 'INVALID',
        demandeurFamilialDetected: 'NOT_AN_ENUM',
        formeMandatProtectionDetected: 'BAD',
        actesEnvisagesDetected: ['INVALID_ACTE'],
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 when date is malformed', () => {
    expect(Rules.computePrefillCount({ aiData: { dateCertificatMedicalDetected: '01/01/2026' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateCertificatMedicalDetected: '' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateCertificatMedicalDetected: 123 } })).toBe(0);
  });

  // ── Cas M (partiel) ───────────────────────────────────────────────────
  it('returns 1 when only regimeProtectionDemande is set', () => {
    const input = { aiData: { regimeProtectionDemande: 'TUTELLE' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeRegimeProtectionDemande(input)).toBe('TUTELLE');
  });

  it('returns 1 when only altertationFacultesMentales=true', () => {
    const input = { aiData: { altertationFacultesMentales: true } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeAltertationFacultesMentales(input)).toBe(true);
  });

  it('returns 1 when only ISO date dateCertificatMedicalDetected set', () => {
    const input = { aiData: { dateCertificatMedicalDetected: '2026-04-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateCertificatMedical(input)).toBe('2026-04-15');
  });

  it('returns 1 when only actesEnvisages array with valid acte', () => {
    const input = { aiData: { actesEnvisagesDetected: ['GESTION_PATRIMOINE'] } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeActesEnvisages(input)).toEqual(['GESTION_PATRIMOINE']);
  });

  it('filters out invalid actes in actesEnvisages array', () => {
    const input = { aiData: { actesEnvisagesDetected: ['GESTION_PATRIMOINE', 'INVALID', 'DECISIONS_SANTE'] } };
    expect(Rules.computeActesEnvisages(input)).toEqual(['GESTION_PATRIMOINE', 'DECISIONS_SANTE']);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 4 when regime + dem + actes + date are set', () => {
    const input = {
      aiData: {
        regimeProtectionDemande: 'HABILITATION_FAMILIALE',
        demandeurFamilialDetected: 'CONJOINT',
        actesEnvisagesDetected: ['GESTION_PATRIMOINE'],
        dateCertificatMedicalDetected: '2026-04-15',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('normalizes enum case (lowercase input → uppercase output)', () => {
    const input = { aiData: { regimeProtectionDemande: 'tutelle' } };
    expect(Rules.computeRegimeProtectionDemande(input)).toBe('TUTELLE');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  // ── Cas N (nominal — 12 champs) ───────────────────────────────────────
  it('returns N=12 when all 12 fields alimentent', () => {
    const input = {
      aiData: {
        regimeProtectionDemande: 'TUTELLE',
        altertationFacultesMentales: true,
        altertationFacultesPhysiques: true,
        certificatMedicalCirconstancieDetected: true,
        dateCertificatMedicalDetected: '2026-04-15',
        consentementPersonneAProtegerDetected: true,
        demandeurFamilialDetected: 'CONJOINT',
        actesEnvisagesDetected: ['GESTION_PATRIMOINE', 'DECISIONS_SANTE'],
        incapaciteGestionQuotidienneDetected: true,
        altertationGraveDetected: true,
        mandatPrealableSigneDetected: true,
        formeMandatProtectionDetected: 'NOTARIE',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(12);
    expect(Rules.computeRegimeProtectionDemande(input)).toBe('TUTELLE');
    expect(Rules.computeAltertationFacultesMentales(input)).toBe(true);
    expect(Rules.computeAltertationFacultesPhysiques(input)).toBe(true);
    expect(Rules.computeCertificatMedicalCirconstancie(input)).toBe(true);
    expect(Rules.computeDateCertificatMedical(input)).toBe('2026-04-15');
    expect(Rules.computeConsentementPersonneAProteger(input)).toBe(true);
    expect(Rules.computeDemandeurFamilial(input)).toBe('CONJOINT');
    expect(Rules.computeActesEnvisages(input)).toEqual(['GESTION_PATRIMOINE', 'DECISIONS_SANTE']);
    expect(Rules.computeIncapaciteGestionQuotidienne(input)).toBe(true);
    expect(Rules.computeAltertationGrave(input)).toBe(true);
    expect(Rules.computeMandatPrealableSigne(input)).toBe(true);
    expect(Rules.computeFormeMandatProtection(input)).toBe('NOTARIE');
  });
});
