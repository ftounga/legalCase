import {
  computeRisquesBadge,
  getRisquesACreuserCountFor,
  getRisquesBadgeFor,
  risquesValidesFor,
} from './risque-badge.helper';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { RisqueStatutValue } from '../../core/models/risque-status.model';

const TOOL_ID = 'F-DT-11-harcelement-licenciement-nul';

function risque(
  statut: RisqueStatutValue,
  libelle: string,
  toolIdsCibles: string[] = [TOOL_ID],
): RisqueAlignment {
  return {
    risqueLibelle: libelle,
    statut,
    toolIdsCibles,
    raisonEcarte: statut === 'ECARTE' ? 'Levé à l\'amiable' : null,
  };
}

describe('risque-badge.helper — F-195 SF-195-02', () => {
  describe('computeRisquesBadge', () => {
    it('liste vide → kind=none, counts à 0', () => {
      expect(computeRisquesBadge([])).toEqual({
        kind: 'none',
        counts: { aCreuser: 0, valides: 0, ecartes: 0 },
      });
    });

    it('uniquement A_CREUSER → kind=to_explore', () => {
      expect(computeRisquesBadge([
        risque('A_CREUSER', 'Risque flou A'),
        risque('A_CREUSER', 'Risque flou B'),
      ])).toEqual({
        kind: 'to_explore',
        counts: { aCreuser: 2, valides: 0, ecartes: 0 },
      });
    });

    it('uniquement VALIDE non critique → kind=validated', () => {
      expect(computeRisquesBadge([
        risque('VALIDE', 'Discrimination salariale'),
      ])).toEqual({
        kind: 'validated',
        counts: { aCreuser: 0, valides: 1, ecartes: 0 },
      });
    });

    it('VALIDE critique (harcèlement) → kind=validated_critical', () => {
      expect(computeRisquesBadge([
        risque('VALIDE', 'Harcèlement moral subi'),
      ]).kind).toBe('validated_critical');
    });

    it('VALIDE critique (violence) → kind=validated_critical', () => {
      expect(computeRisquesBadge([
        risque('VALIDE', 'Violence intra-familiale'),
      ]).kind).toBe('validated_critical');
    });

    it('VALIDE critique (expulsion / OQTF) → kind=validated_critical', () => {
      expect(computeRisquesBadge([
        risque('VALIDE', 'OQTF imminente'),
      ]).kind).toBe('validated_critical');
    });

    it('VALIDE + ECARTE non critique → kind=mixed', () => {
      expect(computeRisquesBadge([
        risque('VALIDE', 'Discrimination'),
        risque('ECARTE', 'Risque levé'),
      ])).toEqual({
        kind: 'mixed',
        counts: { aCreuser: 0, valides: 1, ecartes: 1 },
      });
    });

    it('VALIDE critique + ECARTE → kind=validated_critical (priorité critique)', () => {
      expect(computeRisquesBadge([
        risque('VALIDE', 'Harcèlement moral subi'),
        risque('ECARTE', 'Autre risque levé'),
      ]).kind).toBe('validated_critical');
    });

    it('uniquement ECARTE → kind=discarded', () => {
      expect(computeRisquesBadge([
        risque('ECARTE', 'Clause non-concurrence abusive'),
      ])).toEqual({
        kind: 'discarded',
        counts: { aCreuser: 0, valides: 0, ecartes: 1 },
      });
    });
  });

  describe('getRisquesBadgeFor — filtre par toolId', () => {
    it('filtre les risques d\'autres outils', () => {
      const list = [
        risque('VALIDE', 'Harcèlement moral subi', [TOOL_ID]),
        risque('A_CREUSER', 'Autre risque', ['F-DT-24-non-concurrence']),
      ];
      expect(getRisquesBadgeFor({ risquesAlignment: list }, TOOL_ID).kind)
        .toBe('validated_critical');
    });

    it('input null/undefined → kind=none', () => {
      expect(getRisquesBadgeFor({}, TOOL_ID)).toEqual({
        kind: 'none',
        counts: { aCreuser: 0, valides: 0, ecartes: 0 },
      });
      expect(getRisquesBadgeFor({ risquesAlignment: null }, TOOL_ID)).toEqual({
        kind: 'none',
        counts: { aCreuser: 0, valides: 0, ecartes: 0 },
      });
    });

    it('risque avec toolIdsCibles vide → ignoré', () => {
      const list = [risque('VALIDE', 'Risque transversal', [])];
      expect(getRisquesBadgeFor({ risquesAlignment: list }, TOOL_ID).kind).toBe('none');
    });
  });

  describe('risquesValidesFor — extraction libellés VALIDE', () => {
    it('retourne les libellés des risques VALIDE pour un outil', () => {
      const list = [
        risque('VALIDE', 'Harcèlement moral', [TOOL_ID]),
        risque('VALIDE', 'Discrimination', [TOOL_ID]),
        risque('A_CREUSER', 'Risque flou', [TOOL_ID]),
        risque('ECARTE', 'Risque levé', [TOOL_ID]),
        risque('VALIDE', 'Autre risque', ['F-DT-24-non-concurrence']),
      ];
      expect(risquesValidesFor(list, TOOL_ID)).toEqual([
        'Harcèlement moral',
        'Discrimination',
      ]);
    });

    it('null → []', () => {
      expect(risquesValidesFor(null, TOOL_ID)).toEqual([]);
      expect(risquesValidesFor(undefined, TOOL_ID)).toEqual([]);
    });

    it('liste vide → []', () => {
      expect(risquesValidesFor([], TOOL_ID)).toEqual([]);
    });
  });

  describe('getRisquesACreuserCountFor — F-253 SF-253-02', () => {
    it('alignment null → 0', () => {
      expect(getRisquesACreuserCountFor(null, TOOL_ID)).toBe(0);
    });

    it('alignment undefined → 0', () => {
      expect(getRisquesACreuserCountFor(undefined, TOOL_ID)).toBe(0);
    });

    it('liste vide → 0', () => {
      expect(getRisquesACreuserCountFor([], TOOL_ID)).toBe(0);
    });

    it('compte uniquement les A_CREUSER (ignore V/É)', () => {
      const list: RisqueAlignment[] = [
        risque('A_CREUSER', 'R1'),
        risque('A_CREUSER', 'R2'),
        risque('VALIDE', 'R3'),
        risque('ECARTE', 'R4'),
      ];
      expect(getRisquesACreuserCountFor(list, TOOL_ID)).toBe(2);
    });

    it('ne compte que les risques mappés au toolId', () => {
      const otherTool = 'F-DT-99-autre';
      const list: RisqueAlignment[] = [
        risque('A_CREUSER', 'R1', [TOOL_ID]),
        risque('A_CREUSER', 'R2', [otherTool]),
      ];
      expect(getRisquesACreuserCountFor(list, TOOL_ID)).toBe(1);
    });

    it('toolIdsCibles incluant plusieurs outils → comptabilisé pour chaque outil mappé', () => {
      const otherTool = 'F-DT-99-autre';
      const list: RisqueAlignment[] = [
        risque('A_CREUSER', 'R1', [TOOL_ID, otherTool]),
      ];
      expect(getRisquesACreuserCountFor(list, TOOL_ID)).toBe(1);
      expect(getRisquesACreuserCountFor(list, otherTool)).toBe(1);
    });
  });
});
