/**
 * F-177 SF-177-03 — Tests factorisés validant le pattern d'instrumentation B
 * (TOOL_LABEL static + TOOL_ICON static + @Input forceExpanded) sur les 4 outils
 * ALWAYS_ON Travail FR pilotes.
 *
 * Ces tests garantissent qu'au cours des SF futures (étendant le pattern aux ~26
 * composants restants), aucun pilote ne perd ses metadata.
 */
import { Type } from '@angular/core';

import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';
import { IndemniteComparatifSectionComponent } from '../indemnite-comparatif-section/indemnite-comparatif-section.component';
import { LicenciementSectionComponent } from '../licenciement-section/licenciement-section.component';
import { PrudhomeFicheSectionComponent } from '../prudhome-fiche-section/prudhome-fiche-section.component';
import { getToolMetadata } from './decision-tool.contract';

interface PilotEntry {
  toolId: string;
  component: Type<unknown>;
  expectedLabel: string;
  expectedIcon: string;
}

const PILOTES: PilotEntry[] = [
  {
    toolId: 'F-DT-04-fiche-prudhomale',
    component: PrudhomeFicheSectionComponent,
    expectedLabel: "FICHE PRUD'HOMALE",
    expectedIcon: 'gavel',
  },
  {
    toolId: 'F-DT-07-anciennete-conges-prime',
    component: AncienneteSectionComponent,
    expectedLabel: 'ANCIENNETÉ ET CONGÉS',
    expectedIcon: 'calendar_month',
  },
  {
    toolId: 'F-DT-08-licenciement-validity',
    component: LicenciementSectionComponent,
    expectedLabel: 'VALIDITÉ DU LICENCIEMENT',
    expectedIcon: 'gavel',
  },
  {
    toolId: 'F-DT-09-comparateur-indemnites',
    component: IndemniteComparatifSectionComponent,
    expectedLabel: 'COMPARATEUR INDEMNITÉS',
    expectedIcon: 'euro_symbol',
  },
];

describe('F-177 SF-177-03 — Pattern B instrumentation Travail FR pilote', () => {
  describe.each(PILOTES)('$toolId', ({ component, expectedLabel, expectedIcon }) => {
    it('expose TOOL_LABEL static', () => {
      const meta = getToolMetadata(component);
      expect(meta).not.toBeNull();
      expect(meta!.label).toBe(expectedLabel);
    });

    it('expose TOOL_ICON static', () => {
      const meta = getToolMetadata(component);
      expect(meta!.icon).toBe(expectedIcon);
    });
  });

  it('les 4 pilotes Travail FR sont tous instrumentés (cohérence du périmètre)', () => {
    PILOTES.forEach(({ component, toolId }) => {
      const meta = getToolMetadata(component);
      expect(meta).not.toBeNull();
      expect(meta!.label).toBeTruthy();
      expect(meta!.icon).toBeTruthy();
    });
    expect(PILOTES).toHaveLength(4);
  });
});
