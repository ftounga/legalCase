import {
  HarcelementBeProcedureFormelleSectionPrefillRules as R,
} from './harcelement-be-procedure-formelle-section-prefill-rules';

/**
 * SF-213-07b — tests unitaires du helper pur.
 *
 * <p>V1 : aucun champ pré-remplissable depuis {@code TravailExtractedData}
 * (cf. doc du helper). On vérifie donc que {@code computePrefillCount}
 * retourne <b>toujours 0</b> quel que soit l'input — y compris en présence
 * de champs IA Travail BE riches.</p>
 *
 * <p>Garantit la parité runtime/static
 * (`prefill-count-integrity.spec.ts`) : si on tente de pré-remplir un
 * champ depuis le runtime, ce spec doit également être mis à jour pour
 * éviter la divergence.</p>
 */
describe('HarcelementBeProcedureFormelleSectionPrefillRules', () => {

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

    it('BELGIQUE avec flag harcelementBeDetecte=true → 0 (pas extractible V1)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'BELGIQUE',
        aiData: {
          harcelementBeDetecte: true,
          dateLicenciement: '2026-03-15',
          salaireBrutMensuel: 3500,
          motifLicenciementDetecte: 'Motif grave',
        },
      })).toBe(0);
    });

    it('FRANCE → 0 (gate symétrique)', () => {
      expect(R.computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: {
          dateLicenciement: '2026-03-15',
          salaireBrutMensuel: 3500,
        },
      })).toBe(0);
    });

    it('workspaceCountry absent → 0', () => {
      expect(R.computePrefillCount({ aiData: { salaireBrutMensuel: 3500 } })).toBe(0);
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
