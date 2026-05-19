/**
 * F-236 SF-236-02 — Tests `CalendrierGardePrefillRules`.
 * SF-246-10 : ajout tests pré-fill âges + dates calendrier.
 */
import {
  CalendrierGardePrefillRules,
  computeAgesEnfants,
  computeDateDebutCalendrier,
  computeDateFinCalendrier,
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

  // ---- SF-246-10 : âges enfants + dates calendrier ----

  describe('SF-246-10 — computeAgesEnfants', () => {
    it('retourne [] si aiData absent', () => {
      expect(computeAgesEnfants({})).toEqual([]);
    });

    it('filtre âges hors plage [0, 25]', () => {
      expect(
        computeAgesEnfants({ aiData: { agesEnfantsDetectes: [5, -1, 26, 7.5, 12] } } as any),
      ).toEqual([5, 12]);
    });

    it('retourne les 3 âges valides d\'un dossier nominal', () => {
      expect(
        computeAgesEnfants({ aiData: { agesEnfantsDetectes: [12, 9, 4] } } as any),
      ).toEqual([12, 9, 4]);
    });

    it('retourne [] si liste vide', () => {
      expect(
        computeAgesEnfants({ aiData: { agesEnfantsDetectes: [] } } as any),
      ).toEqual([]);
    });
  });

  describe('SF-246-10 — computeDateDebutCalendrier', () => {
    it('retourne null si absent', () => {
      expect(computeDateDebutCalendrier({})).toBeNull();
    });

    it('retourne la date ISO si présente', () => {
      expect(
        computeDateDebutCalendrier({ aiData: { dateDebutCalendrierDetectee: '2026-09-01' } } as any),
      ).toBe('2026-09-01');
    });

    it('retourne null si chaîne vide', () => {
      expect(
        computeDateDebutCalendrier({ aiData: { dateDebutCalendrierDetectee: '' } } as any),
      ).toBeNull();
    });
  });

  describe('SF-246-10 — computeDateFinCalendrier', () => {
    it('retourne null si absent', () => {
      expect(computeDateFinCalendrier({})).toBeNull();
    });

    it('retourne la date ISO si présente', () => {
      expect(
        computeDateFinCalendrier({ aiData: { dateFinCalendrierDetectee: '2027-08-31' } } as any),
      ).toBe('2027-08-31');
    });
  });

  describe('SF-246-10 — computePrefillCount enrichi', () => {
    it('cas 0 — aucun champ', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('cas partiel — ages seulement (+ mode)', () => {
      expect(
        computePrefillCount({
          aiModeGardeDetaille: 'ALTERNEE_FR',
          workspaceCountry: 'FRANCE',
          aiData: { agesEnfantsDetectes: [8] },
        } as any),
      ).toBe(2);
    });

    it('cas nominal — mode + ages + 2 dates = 4', () => {
      expect(
        computePrefillCount({
          aiModeGardeDetaille: 'DVH_CLASSIQUE_FR',
          workspaceCountry: 'FRANCE',
          aiData: {
            agesEnfantsDetectes: [12, 9],
            dateDebutCalendrierDetectee: '2026-09-01',
            dateFinCalendrierDetectee: '2027-08-31',
          },
        } as any),
      ).toBe(4);
    });

    it('expose computeAgesEnfants dans le module', () => {
      expect(CalendrierGardePrefillRules.computeAgesEnfants).toBe(computeAgesEnfants);
    });
  });
});
