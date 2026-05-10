/**
 * F-236 SF-236-02 — Tests `CalendrierGardePrefillRules`.
 */
import {
  CalendrierGardePrefillRules,
  computeGardeCode,
  computeModeDetailleNote,
  computePrefillCount,
  resolveModeIa,
} from './calendrier-garde-section-prefill-rules';

describe('CalendrierGardePrefillRules', () => {
  describe('cas 0 — input vide ou mode inconnu', () => {
    it('retourne 0 quand input vide', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('retourne 0 quand aiModeGardeDetaille absent et synthesis absent', () => {
      expect(computePrefillCount({ workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('retourne 0 pour un mode inconnu', () => {
      expect(
        computePrefillCount({ aiModeGardeDetaille: 'MODE_X', workspaceCountry: 'FRANCE' }),
      ).toBe(0);
    });
  });

  describe('cas M — gate workspaceCountry', () => {
    it('retourne 0 quand mode FR mais workspace BE', () => {
      expect(
        computePrefillCount({
          aiModeGardeDetaille: 'ALTERNEE_FR',
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });

    it('retourne 0 quand mode BE mais workspace FR', () => {
      expect(
        computePrefillCount({
          aiModeGardeDetaille: 'ALTERNEE_BE',
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(0);
    });

    it('produit une note informative quand mode autre pays', () => {
      const note = computeModeDetailleNote({
        aiModeGardeDetaille: 'ALTERNEE_FR',
        workspaceCountry: 'BELGIQUE',
      });
      expect(note).toContain('ALTERNEE_FR');
      expect(note).toContain('autre pays');
    });
  });

  describe('cas N — mode compatible avec workspace', () => {
    it('retourne 1 pour mode FR sur workspace FRANCE', () => {
      expect(
        computePrefillCount({
          aiModeGardeDetaille: 'ALTERNEE_FR',
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
    });

    it('retourne 1 pour mode BE sur workspace BELGIQUE', () => {
      expect(
        computePrefillCount({
          aiModeGardeDetaille: 'SECONDAIRE_BE',
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(1);
    });

    it('lit le mode via synthesis.pensionAlimentaireEstimate.modeGardeDetaille', () => {
      expect(
        computePrefillCount({
          synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'DVH_CLASSIQUE_FR' } },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
    });

    it('priorise le @Input direct sur synthesis', () => {
      expect(
        resolveModeIa({
          aiModeGardeDetaille: 'ALTERNEE_FR',
          synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'ALTERNEE_BE' } },
        }),
      ).toBe('ALTERNEE_FR');
    });

    it('normalise la casse', () => {
      expect(
        computeGardeCode({ aiModeGardeDetaille: 'alternee_fr', workspaceCountry: 'FRANCE' }),
      ).toBe('ALTERNEE_FR');
    });
  });

  describe('surface', () => {
    it('expose constantes et fonctions', () => {
      expect(CalendrierGardePrefillRules.computePrefillCount).toBe(computePrefillCount);
      expect(CalendrierGardePrefillRules.MODES_FR.has('ALTERNEE_FR')).toBe(true);
      expect(CalendrierGardePrefillRules.MODES_BE.has('ALTERNEE_BE')).toBe(true);
    });
  });
});
