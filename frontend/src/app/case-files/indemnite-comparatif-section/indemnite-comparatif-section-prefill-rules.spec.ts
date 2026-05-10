import {
  IndemniteComparatifSectionPrefillRules,
  computePrefillCount,
  computeTypeRupture,
  computeAlertesValidite,
} from './indemnite-comparatif-section-prefill-rules';

describe('IndemniteComparatifSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('cas M — 2 champs partiels retourne 2', () => {
      expect(
        computePrefillCount({
          aiData: { salaireBrutMensuel: 2500 },
          synthesis: { compensationEstimate: { ancienneteAnnees: 5 } },
        }),
      ).toBe(2);
    });

    it('cas N — 4 champs nominal FR', () => {
      expect(
        computePrefillCount({
          aiData: { salaireBrutMensuel: 2500 },
          synthesis: {
            compensationEstimate: {
              ancienneteAnnees: 5,
              ancienneteMois: 6,
              typeRupture: 'LICENCIEMENT',
            },
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });

    it('rejette typeRupture non supporté pour le pays', () => {
      expect(
        computeTypeRupture({
          synthesis: { compensationEstimate: { typeRupture: 'LICENCIEMENT' } },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBeNull();
      expect(
        computeTypeRupture({
          synthesis: { compensationEstimate: { typeRupture: 'LICENCIEMENT_ORDINAIRE' } },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe('LICENCIEMENT_ORDINAIRE');
    });
  });

  it('expose IndemniteComparatifSectionPrefillRules barrel', () => {
    expect(IndemniteComparatifSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(IndemniteComparatifSectionPrefillRules.computeAlertesValidite).toBe(computeAlertesValidite);
  });

  // F-236 SF-236-04 — couverture alerte F-IA-03 via synthesis.* (anomalie D).
  describe('computeAlertesValidite (F-236 SF-236-04)', () => {
    it('returns null when no synthesis', () => {
      expect(computeAlertesValidite({})).toBeNull();
      expect(computeAlertesValidite({ synthesis: null })).toBeNull();
    });

    it('returns null when synthesis has no validity detections', () => {
      expect(
        computeAlertesValidite({
          synthesis: { compensationEstimate: { ancienneteAnnees: 5 } },
        }),
      ).toBeNull();
    });

    it('returns null when detections have no OUI reponse', () => {
      expect(
        computeAlertesValidite({
          synthesis: {
            ruptureConvValidityDetection: { detections: { CRIT_A: { reponse: 'INCONNU' } } },
            licenciementValidityDetection: { detections: { CRIT_B: { reponse: 'NON' } } },
          },
        }),
      ).toBeNull();
    });

    it('signals ruptureConvAlert when synthesis.ruptureConvValidityDetection has at least one OUI', () => {
      const out = computeAlertesValidite({
        synthesis: {
          ruptureConvValidityDetection: { detections: { CRIT_A: { reponse: 'OUI' } } },
        },
      });
      expect(out).toEqual({ ruptureConvAlert: 'RUPTURE_CONVENTIONNELLE' });
    });

    it('signals licenciementAlert when synthesis.licenciementValidityDetection has at least one OUI', () => {
      const out = computeAlertesValidite({
        synthesis: {
          licenciementValidityDetection: { detections: { CRIT_X: { reponse: 'OUI' } } },
        },
      });
      expect(out).toEqual({ licenciementAlert: 'LICENCIEMENT' });
    });

    it('signals both alerts when both detections have OUI', () => {
      const out = computeAlertesValidite({
        synthesis: {
          ruptureConvValidityDetection: { detections: { A: { reponse: 'OUI' } } },
          licenciementValidityDetection: { detections: { B: { reponse: 'OUI' } } },
        },
      });
      expect(out).toEqual({
        ruptureConvAlert: 'RUPTURE_CONVENTIONNELLE',
        licenciementAlert: 'LICENCIEMENT',
      });
    });
  });
});
