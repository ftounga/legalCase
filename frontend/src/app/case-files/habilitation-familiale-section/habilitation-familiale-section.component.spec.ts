/**
 * SF-222-03 — Tests Jest pour `HabilitationFamilialeSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (parité runtime/static F-237) ;
 *  - country gate BE → formValid false, pas de pré-fill ;
 *  - pré-fill IA branche les 6 critères détectés + provenance IA ;
 *  - formValid exige une étendue choisie ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire (no-op) ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictLabel/verdictClass/modaliteLabel mappent les valeurs.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { HabilitationFamilialeSectionComponent } from './habilitation-familiale-section.component';
import { HabilitationFamilialeService } from '../../core/services/habilitation-familiale.service';
import { HabilitationFamilialeResponse } from '../../core/models/habilitation-familiale.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('HabilitationFamilialeSectionComponent (SF-222-03)', () => {
  let component: HabilitationFamilialeSectionComponent;
  let fixture: ComponentFixture<HabilitationFamilialeSectionComponent>;
  let serviceSpy: jasmine.SpyObj<HabilitationFamilialeService>;

  const nominalResponse: HabilitationFamilialeResponse = {
    caseFileId: 'case-1',
    verdict: 'ELIGIBLE_HABILITATION_GENERALE',
    modalite: 'REPRESENTATION',
    actesCouverts: ['Actes patrimoniaux'],
    conditionsManquantes: [],
    basesJuridiques: ['art. 494-1 Cciv', 'art. 494-6 Cciv'],
    messages: ['Conditions réunies.', 'Décision : juge des contentieux de la protection.'],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<HabilitationFamilialeService>(
      'HabilitationFamilialeService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [HabilitationFamilialeSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: HabilitationFamilialeService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HabilitationFamilialeSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(HabilitationFamilialeService) as jasmine.SpyObj<HabilitationFamilialeService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(HabilitationFamilialeSectionComponent.TOOL_LABEL).toBe('HABILITATION FAMILIALE');
      expect(HabilitationFamilialeSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount({}) = 0 (no aiData)', () => {
      expect(HabilitationFamilialeSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + full aiData = 6', () => {
      const aiData = {
        hfAlteration: true,
        hfLienFamilial: 'DESCENDANT',
        hfConsensus: true,
        hfActesPatrimoniaux: true,
        hfActesPersonnels: true,
        hfEtendue: 'GENERALE',
      } as FamilleExtractedData;
      expect(HabilitationFamilialeSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'FRANCE' })).toBe(6);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      const aiData = { hfAlteration: true } as FamilleExtractedData;
      expect(HabilitationFamilialeSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData complet → 6 signaux branchés + provenance IA', () => {
      component.aiData = {
        hfAlteration: true,
        hfLienFamilial: 'ASCENDANT',
        hfConsensus: false,
        hfActesPatrimoniaux: true,
        hfActesPersonnels: false,
        hfEtendue: 'PONCTUELLE',
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.alterationFacultesMedicalementConstatee()).toBe(true);
      expect(component.lienFamilialEligible()).toBe('ASCENDANT');
      expect(component.consensusFamilial()).toBe(false);
      expect(component.besoinActesPatrimoniaux()).toBe(true);
      expect(component.besoinActesPersonnels()).toBe(false);
      expect(component.protectionPonctuelleOuGenerale()).toBe('PONCTUELLE');
      expect(component.provenanceAlteration()).toBe('IA');
      expect(component.provenanceLien()).toBe('IA');
      expect(component.provenanceEtendue()).toBe('IA');
    });

    it('BELGIQUE → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        hfAlteration: true,
        hfLienFamilial: 'DESCENDANT',
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.alterationFacultesMedicalementConstatee()).toBe(false);
      expect(component.lienFamilialEligible()).toBeNull();
    });

    it('standaloneMode → pas de pré-fill', () => {
      component.standaloneMode = true;
      component.aiData = { hfAlteration: true } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.alterationFacultesMedicalementConstatee()).toBe(false);
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE sans étendue → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + étendue → formValid true', () => {
      fixture.detectChanges();
      component.protectionPonctuelleOuGenerale.set('GENERALE');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique résultat', () => {
      fixture.detectChanges();
      component.alterationFacultesMedicalementConstatee.set(true);
      component.lienFamilialEligible.set('DESCENDANT');
      component.consensusFamilial.set(true);
      component.besoinActesPatrimoniaux.set(true);
      component.besoinActesPersonnels.set(true);
      component.protectionPonctuelleOuGenerale.set('GENERALE');

      component.calculate();

      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        alterationFacultesMedicalementConstatee: true,
        lienFamilialEligible: 'DESCENDANT',
        consensusFamilial: true,
        besoinActesPatrimoniaux: true,
        besoinActesPersonnels: true,
        protectionPonctuelleOuGenerale: 'GENERALE',
      }));
      expect(component.result()).toEqual(nominalResponse);
      expect(component.showForm()).toBe(false);
    });

    it('country gate BE → pas d\'appel HTTP', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });

    it('sans étendue → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('verdictLabel mappe les 3 verdicts', () => {
      expect(component.verdictLabel('ELIGIBLE_HABILITATION_GENERALE')).toContain('générale');
      expect(component.verdictLabel('ELIGIBLE_HABILITATION_SPECIALE')).toContain('spéciale');
      expect(component.verdictLabel('ORIENTER_VERS_MESURE_JUDICIAIRE')).toContain('F-FA-25');
    });

    it('verdictClass mappe les 3 verdicts vers une classe CSS', () => {
      expect(component.verdictClass('ELIGIBLE_HABILITATION_GENERALE')).toBe('verdict-success');
      expect(component.verdictClass('ELIGIBLE_HABILITATION_SPECIALE')).toBe('verdict-info');
      expect(component.verdictClass('ORIENTER_VERS_MESURE_JUDICIAIRE')).toBe('verdict-warning');
    });

    it('modaliteLabel mappe assistance / représentation / null', () => {
      expect(component.modaliteLabel('ASSISTANCE')).toContain('Assistance');
      expect(component.modaliteLabel('REPRESENTATION')).toContain('Représentation');
      expect(component.modaliteLabel(null)).toBeNull();
    });
  });

  describe('handlers — modification manuelle efface badge IA', () => {
    it('onAlterationChange efface provenance', () => {
      component.provenanceAlteration.set('IA');
      component.onAlterationChange(true);
      expect(component.alterationFacultesMedicalementConstatee()).toBe(true);
      expect(component.provenanceAlteration()).toBeNull();
    });

    it('onEtendueChange efface provenance', () => {
      component.provenanceEtendue.set('IA');
      component.onEtendueChange('GENERALE');
      expect(component.protectionPonctuelleOuGenerale()).toBe('GENERALE');
      expect(component.provenanceEtendue()).toBeNull();
    });
  });
});
