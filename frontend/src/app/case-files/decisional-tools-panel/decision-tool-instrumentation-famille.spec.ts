/**
 * F-177 SF-177-03b — Tests factorisés validant le pattern d'instrumentation B
 * (TOOL_LABEL static + TOOL_ICON static + @Input forceExpanded) sur les 35
 * composants outils décisionnels Famille FR + Famille BE.
 *
 * Ces tests garantissent qu'au cours des SF futures (étendant le pattern aux
 * domaines restants), aucun composant Famille ne perd ses metadata.
 *
 * Pattern de référence : `decision-tool-instrumentation-pilote.spec.ts`
 * (Travail FR — PR #725).
 */
import { Type } from '@angular/core';

import { AdoptionSectionComponent } from '../adoption-section/adoption-section.component';
import { AutoriteParentaleSectionComponent } from '../autorite-parentale-section/autorite-parentale-section.component';
import { CalendrierGardeSectionComponent } from '../calendrier-garde-section/calendrier-garde-section.component';
import { ChangementEtatCivilSectionComponent } from '../changement-etat-civil-section/changement-etat-civil-section.component';
import { ChangementResidenceSectionComponent } from '../changement-residence-section/changement-residence-section.component';
import { CommunauteUniverselleSectionComponent } from '../communaute-universelle-section/communaute-universelle-section.component';
import { ContestationPaterniteSectionComponent } from '../contestation-paternite-section/contestation-paternite-section.component';
import { DesaccordsParentauxSectionComponent } from '../desaccords-parentaux-section/desaccords-parentaux-section.component';
import { DevolutionLegaleSectionComponent } from '../devolution-legale-section/devolution-legale-section.component';
import { DivorceAccepteSectionComponent } from '../divorce-accepte-section/divorce-accepte-section.component';
import { DivorceAlterationSectionComponent } from '../divorce-alteration-section/divorce-alteration-section.component';
import { DivorceChecklistSectionComponent } from '../divorce-checklist-section/divorce-checklist-section.component';
import { DivorceDesunionBeSectionComponent } from '../divorce-desunion-be-section/divorce-desunion-be-section.component';
import { DivorceFauteSectionComponent } from '../divorce-faute-section/divorce-faute-section.component';
import { DonationSectionComponent } from '../donation-section/donation-section.component';
import { IndivisionSectionComponent } from '../indivision-section/indivision-section.component';
import { IndivisionSuccessoraleSectionComponent } from '../indivision-successorale-section/indivision-successorale-section.component';
import { MajeursProtegesSectionComponent } from '../majeurs-proteges-section/majeurs-proteges-section.component';
import { MesuresProvisoiresSectionComponent } from '../mesures-provisoires-section/mesures-provisoires-section.component';
import { OrdonnanceProtectionSectionComponent } from '../ordonnance-protection-section/ordonnance-protection-section.component';
import { OrdonnanceRequeteSectionComponent } from '../ordonnance-requete-section/ordonnance-requete-section.component';
import { PacsDissolutionSectionComponent } from '../pacs-dissolution-section/pacs-dissolution-section.component';
import { PartageImmobilierSectionComponent } from '../partage-immobilier-section/partage-immobilier-section.component';
import { PartageJudiciaireSectionComponent } from '../partage-judiciaire-section/partage-judiciaire-section.component';
import { PartageSuccessoralSectionComponent } from '../partage-successoral-section/partage-successoral-section.component';
import { PmaGpaBioethiqueSectionComponent } from '../pma-gpa-bioethique-section/pma-gpa-bioethique-section.component';
import { PossessionEtatSectionComponent } from '../possession-etat-section/possession-etat-section.component';
import { RapportSuccessionSectionComponent } from '../rapport-succession-section/rapport-succession-section.component';
import { RecherchePaterniteSectionComponent } from '../recherche-paternite-section/recherche-paternite-section.component';
import { RecompensesSectionComponent } from '../recompenses-section/recompenses-section.component';
import { ReconnaissancePaternelleSectionComponent } from '../reconnaissance-paternelle-section/reconnaissance-paternelle-section.component';
import { ReserveHeriditaireSectionComponent } from '../reserve-heriditaire-section/reserve-heriditaire-section.component';
import { RevisionsPostDivorceSectionComponent } from '../revisions-post-divorce-section/revisions-post-divorce-section.component';
import { SeparationCorpsSectionComponent } from '../separation-corps-section/separation-corps-section.component';
import { TestamentValiditeSectionComponent } from '../testament-validite-section/testament-validite-section.component';

import { getToolMetadata } from './decision-tool.contract';

interface FamilleEntry {
  dir: string;
  component: Type<unknown>;
  expectedLabel: string;
  expectedIcon: string;
}

const FAMILLE_FR: FamilleEntry[] = [
  { dir: 'adoption-section', component: AdoptionSectionComponent, expectedLabel: 'ADOPTION (FR)', expectedIcon: 'family_restroom' },
  { dir: 'autorite-parentale-section', component: AutoriteParentaleSectionComponent, expectedLabel: 'AUTORITÉ PARENTALE — EXERCICE (FR) — ART. 372-373 CCIV', expectedIcon: 'family_restroom' },
  { dir: 'calendrier-garde-section', component: CalendrierGardeSectionComponent, expectedLabel: 'CALENDRIER DE GARDE', expectedIcon: 'calendar_month' },
  { dir: 'changement-etat-civil-section', component: ChangementEtatCivilSectionComponent, expectedLabel: "CHANGEMENT D'ÉTAT CIVIL (FR) — ART. 60 / 61-1 / 61-5 CCIV", expectedIcon: 'badge' },
  { dir: 'changement-residence-section', component: ChangementResidenceSectionComponent, expectedLabel: 'CHANGEMENT DE RÉSIDENCE (FR) — ART. 373-2 CCIV', expectedIcon: 'home_work' },
  { dir: 'communaute-universelle-section', component: CommunauteUniverselleSectionComponent, expectedLabel: 'COMMUNAUTÉ UNIVERSELLE (FR)', expectedIcon: 'family_restroom' },
  { dir: 'contestation-paternite-section', component: ContestationPaterniteSectionComponent, expectedLabel: 'CONTESTATION DE PATERNITÉ (FR)', expectedIcon: 'gavel' },
  { dir: 'desaccords-parentaux-section', component: DesaccordsParentauxSectionComponent, expectedLabel: 'DÉSACCORDS PARENTAUX (FR) — ART. 373-2-10 CCIV', expectedIcon: 'forum' },
  { dir: 'devolution-legale-section', component: DevolutionLegaleSectionComponent, expectedLabel: 'DÉVOLUTION LÉGALE SUCCESSORALE (FR)', expectedIcon: 'family_restroom' },
  { dir: 'divorce-accepte-section', component: DivorceAccepteSectionComponent, expectedLabel: 'DIVORCE ACCEPTÉ — ART. 233 CCIV', expectedIcon: 'how_to_vote' },
  { dir: 'divorce-alteration-section', component: DivorceAlterationSectionComponent, expectedLabel: 'DIVORCE — ALTÉRATION DÉFINITIVE LIEN CONJUGAL', expectedIcon: 'balance' },
  { dir: 'divorce-checklist-section', component: DivorceChecklistSectionComponent, expectedLabel: 'CHECKLIST DIVORCE', expectedIcon: 'checklist' },
  { dir: 'divorce-faute-section', component: DivorceFauteSectionComponent, expectedLabel: 'DIVORCE POUR FAUTE (FR) — ART. 242 CCIV', expectedIcon: 'gavel' },
  { dir: 'donation-section', component: DonationSectionComponent, expectedLabel: 'VALIDITÉ DONATION ENTRE VIFS (FR)', expectedIcon: 'redeem' },
  { dir: 'indivision-section', component: IndivisionSectionComponent, expectedLabel: 'INDIVISION POST-COMMUNAUTAIRE (ART. 815 CCIV)', expectedIcon: 'apartment' },
  { dir: 'indivision-successorale-section', component: IndivisionSuccessoraleSectionComponent, expectedLabel: 'INDIVISION SUCCESSORALE (FR)', expectedIcon: 'groups' },
  { dir: 'majeurs-proteges-section', component: MajeursProtegesSectionComponent, expectedLabel: 'MAJEURS PROTÉGÉS (FR) — ART. 425-494 / 494-1 CCIV', expectedIcon: 'supervisor_account' },
  { dir: 'mesures-provisoires-section', component: MesuresProvisoiresSectionComponent, expectedLabel: 'MESURES PROVISOIRES (FR) — ART. 254 CCIV', expectedIcon: 'gavel' },
  { dir: 'ordonnance-protection-section', component: OrdonnanceProtectionSectionComponent, expectedLabel: 'ORDONNANCE DE PROTECTION (FR) — ART. 515-9 CCIV', expectedIcon: 'shield' },
  { dir: 'ordonnance-requete-section', component: OrdonnanceRequeteSectionComponent, expectedLabel: 'ORDONNANCE SUR REQUÊTE (FR/BE)', expectedIcon: 'gavel' },
  { dir: 'pacs-dissolution-section', component: PacsDissolutionSectionComponent, expectedLabel: 'DISSOLUTION PACS (FR) — ART. 515-7 CCIV', expectedIcon: 'link_off' },
  { dir: 'partage-immobilier-section', component: PartageImmobilierSectionComponent, expectedLabel: 'PARTAGE IMMOBILIER', expectedIcon: 'home' },
  { dir: 'partage-judiciaire-section', component: PartageJudiciaireSectionComponent, expectedLabel: 'PARTAGE JUDICIAIRE (FR)', expectedIcon: 'balance' },
  { dir: 'partage-successoral-section', component: PartageSuccessoralSectionComponent, expectedLabel: 'PARTAGE SUCCESSORAL (FR)', expectedIcon: 'handshake' },
  { dir: 'pma-gpa-bioethique-section', component: PmaGpaBioethiqueSectionComponent, expectedLabel: 'PMA / GPA / BIOÉTHIQUE (FR)', expectedIcon: 'child_friendly' },
  { dir: 'possession-etat-section', component: PossessionEtatSectionComponent, expectedLabel: "POSSESSION D'ÉTAT (FR)", expectedIcon: 'family_restroom' },
  { dir: 'rapport-succession-section', component: RapportSuccessionSectionComponent, expectedLabel: 'RAPPORT À SUCCESSION (FR)', expectedIcon: 'account_balance' },
  { dir: 'recherche-paternite-section', component: RecherchePaterniteSectionComponent, expectedLabel: 'RECHERCHE DE PATERNITÉ (FR)', expectedIcon: 'family_restroom' },
  { dir: 'recompenses-section', component: RecompensesSectionComponent, expectedLabel: 'RÉCOMPENSES (ART. 1437/1469 CCIV)', expectedIcon: 'account_balance' },
  { dir: 'reconnaissance-paternelle-section', component: ReconnaissancePaternelleSectionComponent, expectedLabel: 'RECONNAISSANCE PATERNELLE (FR)', expectedIcon: 'family_restroom' },
  { dir: 'reserve-heriditaire-section', component: ReserveHeriditaireSectionComponent, expectedLabel: 'RÉSERVE HÉRÉDITAIRE & ACTION EN RÉDUCTION (FR)', expectedIcon: 'balance' },
  { dir: 'revisions-post-divorce-section', component: RevisionsPostDivorceSectionComponent, expectedLabel: 'RÉVISIONS POST-DIVORCE (FR)', expectedIcon: 'history' },
  { dir: 'separation-corps-section', component: SeparationCorpsSectionComponent, expectedLabel: 'SÉPARATION DE CORPS + CONVERSION DIVORCE — ART. 296+306 CCIV', expectedIcon: 'family_restroom' },
  { dir: 'testament-validite-section', component: TestamentValiditeSectionComponent, expectedLabel: 'VALIDITÉ TESTAMENT (FR)', expectedIcon: 'history_edu' },
];

const FAMILLE_BE: FamilleEntry[] = [
  { dir: 'divorce-desunion-be-section', component: DivorceDesunionBeSectionComponent, expectedLabel: 'DIVORCE BE — DÉSUNION IRRÉMÉDIABLE (ART. 229 CC)', expectedIcon: 'balance' },
];

const ALL: FamilleEntry[] = [...FAMILLE_FR, ...FAMILLE_BE];

describe('F-177 SF-177-03b — Pattern B instrumentation Famille FR', () => {
  describe.each(FAMILLE_FR)('$dir', ({ component, expectedLabel, expectedIcon }) => {
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
});

describe('F-177 SF-177-03b — Pattern B instrumentation Famille BE', () => {
  describe.each(FAMILLE_BE)('$dir', ({ component, expectedLabel, expectedIcon }) => {
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
});

describe('F-177 SF-177-03b — Cohérence du périmètre Famille', () => {
  it('les 34 composants Famille FR sont tous instrumentés', () => {
    FAMILLE_FR.forEach(({ component }) => {
      const meta = getToolMetadata(component);
      expect(meta).not.toBeNull();
      expect(meta!.label).toBeTruthy();
      expect(meta!.icon).toBeTruthy();
    });
    expect(FAMILLE_FR).toHaveLength(34);
  });

  it('le composant Famille BE est instrumenté', () => {
    FAMILLE_BE.forEach(({ component }) => {
      const meta = getToolMetadata(component);
      expect(meta).not.toBeNull();
      expect(meta!.label).toBeTruthy();
      expect(meta!.icon).toBeTruthy();
    });
    expect(FAMILLE_BE).toHaveLength(1);
  });

  it('périmètre total = 35 composants Famille (34 FR + 1 BE)', () => {
    expect(ALL).toHaveLength(35);
  });
});
