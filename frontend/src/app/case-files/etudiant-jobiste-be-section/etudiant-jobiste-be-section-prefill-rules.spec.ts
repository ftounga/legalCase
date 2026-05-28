import {
  EtudiantJobisteBeSectionPrefillRules as R,
} from './etudiant-jobiste-be-section-prefill-rules';

/**
 * SF-219-13b — tests unitaires du helper pur.
 *
 * <p>V1 : aucun champ pré-remplissable depuis {@code TravailExtractedData}
 * (cf. doc du helper — statut étudiant, compteur Student@work,
 * volume horaire contrat étudiant, paramètres légaux, formalisme
 * contrat / Dimona STU, barème de cotisations non extractibles du
 * dossier salarié principal). On vérifie donc que
 * {@code computePrefillCount} retourne <b>toujours 0</b> quel que soit
 * l'input — y compris en présence de champs IA Travail BE riches.</p>
 *
 * <p>Garantit la parité runtime/static : si on tente de pré-remplir un
 * champ depuis le runtime, ce spec doit également être mis à jour pour
 * éviter la divergence (F-236 SF-236-02).</p>
 */
describe('EtudiantJobisteBeSectionPrefillRules', () => {

  describe('V1 — invariant : count toujours 0', () => {
    it('input vide → 0', () => {
      expect(R.computePrefillCount({})).toBe(0);
    });

    it('aiData null → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: null })).toBe(0);
    });

    it('aiData undefined → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: undefined })).toBe(0);
    });

    it('aiData {} → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: {} })).toBe(0);
    });

    it('BELGIQUE avec champs Travail BE riches → 0 (V1)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'BELGIQUE',
        aiData: {
          dateNaissanceSalarie: '2004-03-15',
          dateRuptureContrat: '2026-09-30',
          motifRupture: 'autre',
          anneesCarriereSalarie: 0,
          salaireBrutMensuel: 1200,
        },
      })).toBe(0);
    });

    it('FRANCE → 0 (gate symétrique)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: {
          dateNaissanceSalarie: '2004-03-15',
          dateRuptureContrat: '2026-09-30',
        },
      })).toBe(0);
    });

    it('workspaceCountry absent → 0', () => {
      expect(R.computePrefillCount({
        aiData: { dateRuptureContrat: '2026-09-30' },
      })).toBe(0);
    });
  });

  describe('Contrat F-236 — comportement déterministe', () => {
    it('toujours un nombre fini, non-NaN, non-négatif', () => {
      const result = R.computePrefillCount({});
      expect(typeof result).toBe('number');
      expect(Number.isFinite(result)).toBe(true);
      expect(Number.isNaN(result)).toBe(false);
      expect(result).toBeGreaterThanOrEqual(0);
    });
  });
});
