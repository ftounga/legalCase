import {
  InastriStatutTravailleurIndependantSectionPrefillRules as R,
} from './inastri-statut-travailleur-independant-section-prefill-rules';

/**
 * SF-219-27b — tests unitaires du helper pur.
 *
 * <p>V1 : aucun champ pré-remplissable depuis {@code TravailExtractedData}
 * (cf. doc du helper — volonté des parties, modalités d'exécution,
 * critères de subordination, secteur, statut administratif INASTI /
 * DIMONA, présence d'autres clients non extractibles du dossier
 * salarié individuel — relèvent de l'analyse contractuelle in
 * concreto par l'avocat). On vérifie que {@code computePrefillCount}
 * retourne <b>toujours 0</b> quel que soit l'input — y compris en
 * présence de champs IA Travail BE riches.</p>
 *
 * <p>Garantit la parité runtime/static : si on tente de pré-remplir un
 * champ depuis le runtime, ce spec doit également être mis à jour pour
 * éviter la divergence (F-236 SF-236-02).</p>
 */
describe('InastriStatutTravailleurIndependantSectionPrefillRules', () => {

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

    it('aiData {} => 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: {} })).toBe(0);
    });

    it('BELGIQUE avec champs Travail BE riches => 0 (V1)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'BELGIQUE',
        aiData: {
          dateNaissanceSalarie: '1985-06-12',
          dateRuptureContrat: '2026-09-30',
          motifRupture: 'autre',
          anneesCarriereSalarie: 8,
          salaireBrutMensuel: 2400,
          salaireBrutAnnuel: 28800,
        },
      })).toBe(0);
    });

    it('FRANCE => 0 (gate symétrique)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: {
          dateNaissanceSalarie: '1985-06-12',
          dateRuptureContrat: '2026-09-30',
          salaireBrutAnnuel: 28800,
        },
      })).toBe(0);
    });

    it('workspaceCountry absent => 0', () => {
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
