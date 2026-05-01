/**
 * F-177 SF-177-03b — Tests factorisés validant le pattern d'instrumentation B
 * (TOOL_LABEL static + TOOL_ICON static + @Input forceExpanded) sur les ~30
 * outils Travail FR + BE étendant le pilote SF-177-03.
 *
 * Ces tests garantissent qu'au cours des SF futures, aucun composant ne perd
 * ses metadata.
 */
import { Type } from '@angular/core';

import { HarcelementLicenciementNulSectionComponent } from '../harcelement-licenciement-nul-section/harcelement-licenciement-nul-section.component';
import { LicenciementNulDetectionSectionComponent } from '../licenciement-nul-detection-section/licenciement-nul-detection-section.component';
import { DiscriminationSectionComponent } from '../discrimination-section/discrimination-section.component';
import { LicenciementEconomiqueSectionComponent } from '../licenciement-economique-section/licenciement-economique-section.component';
import { InaptitudeSectionComponent } from '../inaptitude-section/inaptitude-section.component';
import { HeuresSupSectionComponent } from '../heures-sup-section/heures-sup-section.component';
import { IndemnitePrecariteCddSectionComponent } from '../indemnite-precarite-cdd-section/indemnite-precarite-cdd-section.component';
import { CongesPayesSectionComponent } from '../conges-payes-section/conges-payes-section.component';
import { FinMissionInterimSectionComponent } from '../fin-mission-interim-section/fin-mission-interim-section.component';
import { DocumentsFinContratSectionComponent } from '../documents-fin-contrat-section/documents-fin-contrat-section.component';
import { ReferePrudhomalSectionComponent } from '../refere-prudhomal-section/refere-prudhomal-section.component';
import { TravailDissimuleSectionComponent } from '../travail-dissimule-section/travail-dissimule-section.component';
import { IndemnitePreavisSectionComponent } from '../indemnite-preavis-section/indemnite-preavis-section.component';
import { RappelSalaireSectionComponent } from '../rappel-salaire-section/rappel-salaire-section.component';
import { RequalificationCddCdiSectionComponent } from '../requalification-cdd-cdi-section/requalification-cdd-cdi-section.component';
import { RequalificationInterimCdiSectionComponent } from '../requalification-interim-cdi-section/requalification-interim-cdi-section.component';
import { NonConcurrenceSectionComponent } from '../non-concurrence-section/non-concurrence-section.component';
import { ContestationAreSectionComponent } from '../contestation-are-section/contestation-are-section.component';
import { PseSectionComponent } from '../pse-section/pse-section.component';
import { ProtectionRpSectionComponent } from '../protection-rp-section/protection-rp-section.component';
import { AtMpSectionComponent } from '../at-mp-section/at-mp-section.component';
import { TravailProcedureSectionComponent } from '../travail-procedure-section/travail-procedure-section.component';
import { RuptureConvIndemniteSectionComponent } from '../rupture-conv-indemnite-section/rupture-conv-indemnite-section.component';
import { RuptureConvSectionComponent } from '../rupture-conv-section/rupture-conv-section.component';
import { TransactionSectionComponent } from '../transaction-section/transaction-section.component';
import { CaseDeadlinesSectionComponent } from '../case-deadlines-section/case-deadlines-section.component';
import { TribunalTravailFicheSectionComponent } from '../tribunal-travail-fiche-section/tribunal-travail-fiche-section.component';
import { MotifGraveBeSectionComponent } from '../motif-grave-be-section/motif-grave-be-section.component';
import { AvantagesConventionnelsBeSectionComponent } from '../avantages-conventionnels-be-section/avantages-conventionnels-be-section.component';
import { CreditTempsBeSectionComponent } from '../credit-temps-be-section/credit-temps-be-section.component';

import { getToolMetadata } from './decision-tool.contract';

interface TravailEntry {
  name: string;
  component: Type<unknown>;
  expectedLabel: string;
  expectedIcon: string;
}

const TRAVAIL_FR: TravailEntry[] = [
  {
    name: 'harcelement-licenciement-nul',
    component: HarcelementLicenciementNulSectionComponent,
    expectedLabel: 'INDEMNITÉ LICENCIEMENT NUL — HARCÈLEMENT',
    expectedIcon: 'gavel',
  },
  {
    name: 'licenciement-nul-detection',
    component: LicenciementNulDetectionSectionComponent,
    expectedLabel: 'LICENCIEMENT NUL — DÉTECTION (ART. L.1235-3-1)',
    expectedIcon: 'policy',
  },
  {
    name: 'discrimination',
    component: DiscriminationSectionComponent,
    expectedLabel: 'DISCRIMINATION — DOMMAGES-INTÉRÊTS',
    expectedIcon: 'balance',
  },
  {
    name: 'licenciement-economique',
    component: LicenciementEconomiqueSectionComponent,
    expectedLabel: 'LICENCIEMENT ÉCONOMIQUE (FR) — ART. L.1233-3/4/5/45',
    expectedIcon: 'gavel',
  },
  {
    name: 'inaptitude',
    component: InaptitudeSectionComponent,
    expectedLabel: 'LICENCIEMENT POUR INAPTITUDE',
    expectedIcon: 'medical_services',
  },
  {
    name: 'heures-sup',
    component: HeuresSupSectionComponent,
    expectedLabel: 'RAPPEL HEURES SUPPLÉMENTAIRES',
    expectedIcon: 'schedule',
  },
  {
    name: 'indemnite-precarite-cdd',
    component: IndemnitePrecariteCddSectionComponent,
    expectedLabel: 'INDEMNITÉ PRÉCARITÉ CDD',
    expectedIcon: 'savings',
  },
  {
    name: 'conges-payes',
    component: CongesPayesSectionComponent,
    expectedLabel: 'INDEMNITÉ COMPENSATRICE DE CONGÉS PAYÉS',
    expectedIcon: 'beach_access',
  },
  {
    name: 'fin-mission-interim',
    component: FinMissionInterimSectionComponent,
    expectedLabel: 'INDEMNITÉ FIN MISSION INTÉRIM',
    expectedIcon: 'work_history',
  },
  {
    name: 'documents-fin-contrat',
    component: DocumentsFinContratSectionComponent,
    expectedLabel: 'DOCUMENTS DE FIN DE CONTRAT — CONFORMITÉ',
    expectedIcon: 'fact_check',
  },
  {
    name: 'refere-prudhomal',
    component: ReferePrudhomalSectionComponent,
    expectedLabel: "RÉFÉRÉ PRUD'HOMAL R.1454-1 (FR)",
    expectedIcon: 'gavel',
  },
  {
    name: 'travail-dissimule',
    component: TravailDissimuleSectionComponent,
    expectedLabel: 'INDEMNITÉ TRAVAIL DISSIMULÉ — L.8223-1',
    expectedIcon: 'work_off',
  },
  {
    name: 'indemnite-preavis',
    component: IndemnitePreavisSectionComponent,
    expectedLabel: 'INDEMNITÉ COMPENSATRICE DE PRÉAVIS',
    expectedIcon: 'schedule',
  },
  {
    name: 'rappel-salaire',
    component: RappelSalaireSectionComponent,
    expectedLabel: 'RAPPEL DE SALAIRE',
    expectedIcon: 'payments',
  },
  {
    name: 'requalification-cdd-cdi',
    component: RequalificationCddCdiSectionComponent,
    expectedLabel: 'REQUALIFICATION CDD → CDI (FR) — ART. L.1245',
    expectedIcon: 'swap_horiz',
  },
  {
    name: 'requalification-interim-cdi',
    component: RequalificationInterimCdiSectionComponent,
    expectedLabel: 'REQUALIFICATION INTÉRIM → CDI (FR) — ART. L.1251-40',
    expectedIcon: 'swap_horiz',
  },
  {
    name: 'non-concurrence',
    component: NonConcurrenceSectionComponent,
    expectedLabel: 'CLAUSE DE NON-CONCURRENCE (FR) — CASS. SOC. 10/07/2002',
    expectedIcon: 'block',
  },
  {
    name: 'contestation-are',
    component: ContestationAreSectionComponent,
    expectedLabel: 'CONTESTATION ARE — FRANCE TRAVAIL (EX-PÔLE EMPLOI)',
    expectedIcon: 'how_to_vote',
  },
  {
    name: 'pse',
    component: PseSectionComponent,
    expectedLabel: 'PSE — CRITÈRES DE VALIDITÉ (FR)',
    expectedIcon: 'groups',
  },
  {
    name: 'protection-rp',
    component: ProtectionRpSectionComponent,
    expectedLabel: 'PROTECTION DES REPRÉSENTANTS DU PERSONNEL (FR)',
    expectedIcon: 'shield',
  },
  {
    name: 'at-mp',
    component: AtMpSectionComponent,
    expectedLabel: 'ACCIDENT DU TRAVAIL / MALADIE PROFESSIONNELLE (FR)',
    expectedIcon: 'healing',
  },
  {
    name: 'travail-procedure',
    component: TravailProcedureSectionComponent,
    expectedLabel: 'CALENDRIER PROCÉDURAL — DROIT DU TRAVAIL',
    expectedIcon: 'event',
  },
  {
    name: 'rupture-conv-indemnite',
    component: RuptureConvIndemniteSectionComponent,
    expectedLabel: 'INDEMNITÉ RUPTURE CONVENTIONNELLE',
    expectedIcon: 'euro_symbol',
  },
  {
    name: 'rupture-conv',
    component: RuptureConvSectionComponent,
    expectedLabel: 'VALIDITÉ DE LA RUPTURE CONVENTIONNELLE',
    expectedIcon: 'gavel',
  },
  {
    name: 'transaction',
    component: TransactionSectionComponent,
    expectedLabel: 'TRANSACTION (FR) — ART. 2044 CCIV',
    expectedIcon: 'handshake',
  },
  {
    name: 'case-deadlines',
    component: CaseDeadlinesSectionComponent,
    expectedLabel: 'DÉLAIS LÉGAUX',
    expectedIcon: 'event',
  },
  {
    name: 'tribunal-travail-fiche',
    component: TribunalTravailFicheSectionComponent,
    expectedLabel: 'REQUÊTE TRIBUNAL DU TRAVAIL',
    expectedIcon: 'balance',
  },
];

const TRAVAIL_BE: TravailEntry[] = [
  {
    name: 'motif-grave-be',
    component: MotifGraveBeSectionComponent,
    expectedLabel: 'MOTIF GRAVE BE (ART. 35 LOI 03/07/1978)',
    expectedIcon: 'gavel',
  },
  {
    name: 'avantages-conventionnels-be',
    component: AvantagesConventionnelsBeSectionComponent,
    expectedLabel: 'AVANTAGES CONVENTIONNELS BE',
    expectedIcon: 'card_giftcard',
  },
  {
    name: 'credit-temps-be',
    component: CreditTempsBeSectionComponent,
    expectedLabel: 'CRÉDIT-TEMPS / INTERRUPTION DE CARRIÈRE BE',
    expectedIcon: 'schedule',
  },
];

const ALL_TRAVAIL: TravailEntry[] = [...TRAVAIL_FR, ...TRAVAIL_BE];

describe('F-177 SF-177-03b — Pattern B instrumentation Travail FR + BE', () => {
  describe.each(ALL_TRAVAIL)('$name', ({ component, expectedLabel, expectedIcon }) => {
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

  it('Travail FR — les 27 outils sont tous instrumentés', () => {
    TRAVAIL_FR.forEach(({ component }) => {
      const meta = getToolMetadata(component);
      expect(meta).not.toBeNull();
      expect(meta!.label).toBeTruthy();
      expect(meta!.icon).toBeTruthy();
    });
    expect(TRAVAIL_FR).toHaveLength(27);
  });

  it('Travail BE — les 3 outils sont tous instrumentés', () => {
    TRAVAIL_BE.forEach(({ component }) => {
      const meta = getToolMetadata(component);
      expect(meta).not.toBeNull();
      expect(meta!.label).toBeTruthy();
      expect(meta!.icon).toBeTruthy();
    });
    expect(TRAVAIL_BE).toHaveLength(3);
  });

  it('périmètre total Travail (FR + BE) — 30 outils instrumentés', () => {
    expect(ALL_TRAVAIL).toHaveLength(30);
  });
});
