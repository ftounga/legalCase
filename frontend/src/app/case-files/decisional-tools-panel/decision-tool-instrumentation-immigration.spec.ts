/**
 * F-177 SF-177-03b — Tests factorisés validant le pattern d'instrumentation B
 * (TOOL_LABEL static + TOOL_ICON static + @Input forceExpanded) sur les 22
 * composants outils décisionnels Immigration FR + BE.
 *
 * Ces tests garantissent qu'au cours des SF futures (étendant le pattern aux
 * composants Famille restants), aucun outil Immigration ne perd ses metadata.
 */
import { Type } from '@angular/core';

import { ImmigrationChecklistSectionComponent } from '../immigration-checklist-section/immigration-checklist-section.component';
import { ImmigrationRecoursSectionComponent } from '../immigration-recours-section/immigration-recours-section.component';
import { ImmigrationTitleDecisionSectionComponent } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { ImmigrationWorkRightSectionComponent } from '../immigration-work-right-section/immigration-work-right-section.component';
import { OqtfAvecDelaiSectionComponent } from '../oqtf-avec-delai-section/oqtf-avec-delai-section.component';
import { OqtfSansDelaiSectionComponent } from '../oqtf-sans-delai-section/oqtf-sans-delai-section.component';
import { ReferesAdminSectionComponent } from '../referes-admin-section/referes-admin-section.component';
import { AesEtudiantSectionComponent } from '../aes-etudiant-section/aes-etudiant-section.component';
import { AesFamilleSectionComponent } from '../aes-famille-section/aes-famille-section.component';
import { AesHumanitaireSectionComponent } from '../aes-humanitaire-section/aes-humanitaire-section.component';
import { AesMetiersTensionSectionComponent } from '../aes-metiers-tension-section/aes-metiers-tension-section.component';
import { AsileAvanceSectionComponent } from '../asile-avance-section/asile-avance-section.component';
import { MesuresEloignementSectionComponent } from '../mesures-eloignement-section/mesures-eloignement-section.component';
import { NaturalisationSectionComponent } from '../naturalisation-section/naturalisation-section.component';
import { MineursImmigrationSectionComponent } from '../mineurs-immigration-section/mineurs-immigration-section.component';
import { RegimeAlgerienSectionComponent } from '../regime-algerien-section/regime-algerien-section.component';
import { ChangementStatutSectionComponent } from '../changement-statut-section/changement-statut-section.component';
import { Annexe13BeSectionComponent } from '../annexe13-be-section/annexe13-be-section.component';
import { BelgianCohabitantUeBeSectionComponent } from '../belgian-40bis-section/belgian-40bis-section.component';
import { Belgian40terSectionComponent } from '../belgian-40ter-section/belgian-40ter-section.component';
import { Belgian9bisSectionComponent } from '../belgian-9bis-section/belgian-9bis-section.component';
import { Belgian9terSectionComponent } from '../belgian-9ter-section/belgian-9ter-section.component';
import { getToolMetadata } from './decision-tool.contract';

interface ImmigrationEntry {
  toolId: string;
  component: Type<unknown>;
  expectedLabel: string;
  expectedIcon: string;
}

const IMMIGRATION_TOOLS: ImmigrationEntry[] = [
  // Immigration FR
  {
    toolId: 'immigration-checklist',
    component: ImmigrationChecklistSectionComponent,
    expectedLabel: 'CHECKLIST PIÈCES IMMIGRATION',
    expectedIcon: 'assignment_turned_in',
  },
  {
    toolId: 'immigration-recours',
    component: ImmigrationRecoursSectionComponent,
    expectedLabel: 'RECOURS IMMIGRATION',
    expectedIcon: 'gavel',
  },
  {
    toolId: 'immigration-title-decision',
    component: ImmigrationTitleDecisionSectionComponent,
    expectedLabel: 'TITRE DE SÉJOUR RECOMMANDÉ',
    expectedIcon: 'account_tree',
  },
  {
    toolId: 'immigration-work-right',
    component: ImmigrationWorkRightSectionComponent,
    expectedLabel: 'DROIT AU TRAVAIL',
    expectedIcon: 'work',
  },
  {
    toolId: 'oqtf-avec-delai',
    component: OqtfAvecDelaiSectionComponent,
    expectedLabel: 'OQTF AVEC DÉLAI DE DÉPART VOLONTAIRE (FR)',
    expectedIcon: 'gavel',
  },
  {
    toolId: 'oqtf-sans-delai',
    component: OqtfSansDelaiSectionComponent,
    expectedLabel: 'OQTF SANS DÉLAI DE DÉPART VOLONTAIRE (FR) — 48H',
    expectedIcon: 'gavel',
  },
  {
    toolId: 'referes-admin',
    component: ReferesAdminSectionComponent,
    expectedLabel: 'RÉFÉRÉS ADMINISTRATIFS L.521-1 / L.521-2 (FR)',
    expectedIcon: 'gavel',
  },
  {
    toolId: 'aes-etudiant',
    component: AesEtudiantSectionComponent,
    expectedLabel: 'AES VOIE ÉTUDIANTE (FR)',
    expectedIcon: 'school',
  },
  {
    toolId: 'aes-famille',
    component: AesFamilleSectionComponent,
    expectedLabel: 'AES — VOIE FAMILIALE (L.435-1)',
    expectedIcon: 'family_restroom',
  },
  {
    toolId: 'aes-humanitaire',
    component: AesHumanitaireSectionComponent,
    expectedLabel: 'AES — VOIE HUMANITAIRE (L.435-2)',
    expectedIcon: 'volunteer_activism',
  },
  {
    toolId: 'aes-metiers-tension',
    component: AesMetiersTensionSectionComponent,
    expectedLabel: 'AES MÉTIERS EN TENSION (FR)',
    expectedIcon: 'how_to_reg',
  },
  {
    toolId: 'asile-avance',
    component: AsileAvanceSectionComponent,
    expectedLabel: 'ASILE AVANCÉ (FR)',
    expectedIcon: 'support',
  },
  {
    toolId: 'mesures-eloignement',
    component: MesuresEloignementSectionComponent,
    expectedLabel: "MESURE D'ÉLOIGNEMENT (FR)",
    expectedIcon: 'flight_takeoff',
  },
  {
    toolId: 'naturalisation',
    component: NaturalisationSectionComponent,
    expectedLabel: 'NATURALISATION (FR)',
    expectedIcon: 'how_to_reg',
  },
  {
    toolId: 'mineurs-immigration',
    component: MineursImmigrationSectionComponent,
    expectedLabel: 'MINEURS ÉTRANGERS (FR)',
    expectedIcon: 'child_care',
  },
  {
    toolId: 'regime-algerien',
    component: RegimeAlgerienSectionComponent,
    expectedLabel: 'RÉGIME ALGÉRIEN (FR)',
    expectedIcon: 'flag',
  },
  {
    toolId: 'changement-statut',
    component: ChangementStatutSectionComponent,
    expectedLabel: 'CHANGEMENT DE STATUT (FR)',
    expectedIcon: 'swap_horiz',
  },
  // Immigration BE
  {
    toolId: 'annexe13-be',
    component: Annexe13BeSectionComponent,
    expectedLabel: 'ANNEXE 13 — OQT BELGE (BE)',
    expectedIcon: 'gavel',
  },
  {
    toolId: 'belgian-40bis',
    component: BelgianCohabitantUeBeSectionComponent,
    expectedLabel: 'REGROUPEMENT FAMILIAL — CITOYEN UE (ART. 40BIS)',
    expectedIcon: 'family_restroom',
  },
  {
    toolId: 'belgian-40ter',
    component: Belgian40terSectionComponent,
    expectedLabel: '40TER FAMILIAL BELGE (BE)',
    expectedIcon: 'family_restroom',
  },
  {
    toolId: 'belgian-9bis',
    component: Belgian9bisSectionComponent,
    expectedLabel: '9BIS HUMANITAIRE — RÉGULARISATION (BE)',
    expectedIcon: 'gavel',
  },
  {
    toolId: 'belgian-9ter',
    component: Belgian9terSectionComponent,
    expectedLabel: 'RÉGULARISATION 9TER MÉDICAL (BE)',
    expectedIcon: 'medical_services',
  },
];

describe('F-177 SF-177-03b — Pattern B instrumentation Immigration FR + BE', () => {
  describe.each(IMMIGRATION_TOOLS)('$toolId', ({ component, expectedLabel, expectedIcon }) => {
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

  it('les 22 composants Immigration sont tous instrumentés (cohérence du périmètre)', () => {
    IMMIGRATION_TOOLS.forEach(({ component }) => {
      const meta = getToolMetadata(component);
      expect(meta).not.toBeNull();
      expect(meta!.label).toBeTruthy();
      expect(meta!.icon).toBeTruthy();
    });
    expect(IMMIGRATION_TOOLS).toHaveLength(22);
  });
});
