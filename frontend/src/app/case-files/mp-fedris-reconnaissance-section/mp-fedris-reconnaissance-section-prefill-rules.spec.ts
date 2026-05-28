import {
  MpFedrisReconnaissanceSectionPrefillRules as R,
} from './mp-fedris-reconnaissance-section-prefill-rules';

/**
 * SF-219-28b — tests unitaires du helper pur.
 *
 * <p>V1 : aucun champ pre-remplissable depuis {@code TravailExtractedData}
 * (cf. doc du helper — typeMaladie, codeMaladieListe, libelleMaladie,
 * dates, exposition, causalite non extractibles du dossier salarie
 * generique du pipeline Travail BE — relevent du rapport medical
 * specialiste, du CV professionnel, du carnet de surveillance sante
 * AR 28/05/2003, et de la nomenclature AR 28/03/1969). On verifie que
 * {@code computePrefillCount} retourne <b>toujours 0</b> quel que soit
 * l'input — y compris en presence de champs IA Travail BE riches.</p>
 *
 * <p>Garantit la parite runtime/static : si on tente de pre-remplir un
 * champ depuis le runtime, ce spec doit egalement etre mis a jour pour
 * eviter la divergence (F-236 SF-236-02).</p>
 */
describe('MpFedrisReconnaissanceSectionPrefillRules', () => {

  describe('V1 — invariant : count toujours 0', () => {
    it('input vide => 0', () => {
      expect(R.computePrefillCount({})).toBe(0);
    });

    it('aiData null => 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: null })).toBe(0);
    });

    it('aiData undefined => 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: undefined })).toBe(0);
    });

    it('aiData vide => 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: {} })).toBe(0);
    });

    it('BELGIQUE avec champs Travail BE riches => 0 (V1)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'BELGIQUE',
        aiData: {
          dateNaissanceSalarie: '1965-04-12',
          dateRuptureContrat: '2026-09-30',
          motifRupture: 'autre',
          anneesCarriereSalarie: 30,
          salaireBrutMensuel: 2900,
          salaireBrutAnnuel: 34800,
        },
      })).toBe(0);
    });

    it('FRANCE => 0 (gate symetrique)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: {
          dateNaissanceSalarie: '1965-04-12',
          dateRuptureContrat: '2026-09-30',
          salaireBrutAnnuel: 34800,
        },
      })).toBe(0);
    });

    it('workspaceCountry absent => 0', () => {
      expect(R.computePrefillCount({
        aiData: { dateRuptureContrat: '2026-09-30' },
      })).toBe(0);
    });
  });

  describe('Contrat F-236 — comportement deterministe', () => {
    it('toujours un nombre fini, non-NaN, non-negatif', () => {
      const result = R.computePrefillCount({});
      expect(typeof result).toBe('number');
      expect(Number.isFinite(result)).toBe(true);
      expect(Number.isNaN(result)).toBe(false);
      expect(result).toBeGreaterThanOrEqual(0);
    });
  });
});
