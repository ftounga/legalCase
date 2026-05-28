import {
  EgaliteFemmesHommesBeSectionPrefillRules as R,
} from './egalite-femmes-hommes-be-section-prefill-rules';

/**
 * SF-219-22b — tests unitaires du helper pur.
 *
 * <p>V1 : aucun champ pré-remplissable depuis {@code TravailExtractedData}
 * (cf. doc du helper — effectif moyen ETP entreprise, statut du rapport
 * biennal, ventilations art. 4 AR 17/08/2013, plan d'action, médiateur,
 * plainte IEFH non extractibles du dossier salarié individuel —
 * déclaratifs RH employeur ou avocat). On vérifie donc que
 * {@code computePrefillCount} retourne <b>toujours 0</b> quel que soit
 * l'input — y compris en présence de champs IA Travail BE riches.</p>
 *
 * <p>Garantit la parité runtime/static : si on tente de pré-remplir
 * un champ depuis le runtime, ce spec doit également être mis à jour
 * pour éviter la divergence (F-236 SF-236-02).</p>
 */
describe('EgaliteFemmesHommesBeSectionPrefillRules', () => {

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
          dateNaissanceSalarie: '1980-04-15',
          dateRuptureContrat: '2026-09-30',
          motifRupture: 'autre',
          anneesCarriereSalarie: 12,
          salaireBrutMensuel: 2800,
          salaireBrutAnnuel: 33600,
        },
      })).toBe(0);
    });

    it('FRANCE → 0 (gate symétrique)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: {
          dateNaissanceSalarie: '1980-04-15',
          dateRuptureContrat: '2026-09-30',
          salaireBrutAnnuel: 33600,
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
