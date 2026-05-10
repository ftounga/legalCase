/** F-236 SF-236-02 — Tests `MajeursProtegesPrefillRules`. */
import {
  MajeursProtegesPrefillRules,
  computePrefillCount,
} from './majeurs-proteges-section-prefill-rules';

describe('MajeursProtegesPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — régime seul', () => {
    expect(
      computePrefillCount({ aiData: { regimeProtectionDemande: 'TUTELLE' } } as any),
    ).toBe(1);
  });

  it('booléens false ne comptent pas', () => {
    expect(
      computePrefillCount({
        aiData: {
          altertationFacultesMentales: false,
          altertationFacultesPhysiques: false,
        },
      } as any),
    ).toBe(0);
  });

  it('cas N — 5/12 (SF-FA-25-02 fields)', () => {
    expect(
      computePrefillCount({
        aiData: {
          regimeProtectionDemande: 'TUTELLE',
          altertationFacultesMentales: true,
          altertationFacultesPhysiques: true,
          certificatMedicalCirconstancieDetected: true,
          dateCertificatMedicalDetected: '2025-02-15',
        },
      } as any),
    ).toBe(5);
  });

  it('cas N maximal — 12/12 champs', () => {
    expect(
      computePrefillCount({
        aiData: {
          regimeProtectionDemande: 'TUTELLE',
          altertationFacultesMentales: true,
          altertationFacultesPhysiques: true,
          certificatMedicalCirconstancieDetected: true,
          dateCertificatMedicalDetected: '2025-02-15',
          consentementPersonneAProtegerDetected: true,
          demandeurFamilialDetected: 'CONJOINT',
          actesEnvisagesDetected: ['GESTION_PATRIMOINE'],
          incapaciteGestionQuotidienneDetected: true,
          altertationGraveDetected: true,
          mandatPrealableSigneDetected: true,
          formeMandatProtectionDetected: 'NOTARIE',
        },
      } as any),
    ).toBe(12);
  });

  it('surface', () => {
    expect(MajeursProtegesPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
