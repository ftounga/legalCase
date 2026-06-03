/**
 * SF-222-04 — Tests Jest pour `AssistanceEducativeSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (parité runtime/static F-237) ;
 *  - country gate BE → formValid false, pas de pré-fill ;
 *  - pré-fill IA branche les 5 critères détectés + provenance IA ;
 *  - formValid exige FRANCE ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire (no-op) ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictLabel/verdictClass mappent les 4 verdicts.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AssistanceEducativeSectionComponent } from './assistance-educative-section.component';
import { AssistanceEducativeService } from '../../core/services/assistance-educative.service';
import { AssistanceEducativeResponse } from '../../core/models/assistance-educative.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AssistanceEducativeSectionComponent (SF-222-04)', () => {
  let component: AssistanceEducativeSectionComponent;
  let fixture: ComponentFixture<AssistanceEducativeSectionComponent>;
  let serviceSpy: jasmine.SpyObj<AssistanceEducativeService>;

  const nominalResponse: AssistanceEducativeResponse = {
    caseFileId: 'case-1',
    verdict: 'AEMO',
    juridiction: 'Juge des enfants',
    mesureOrientee: 'Action éducative en milieu ouvert (AEMO)',
    basesJuridiques: ['art. 375 Cciv', 'art. 375-2 Cciv'],
    messages: ['Danger caractérisé, maintien possible.', 'Décision : juge des enfants.'],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AssistanceEducativeService>(
      'AssistanceEducativeService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [AssistanceEducativeSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: AssistanceEducativeService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AssistanceEducativeSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(AssistanceEducativeService) as jasmine.SpyObj<AssistanceEducativeService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(AssistanceEducativeSectionComponent.TOOL_LABEL).toBe('ASSISTANCE EDUCATIVE');
      expect(AssistanceEducativeSectionComponent.TOOL_ICON).toBe('child_care');
    });

    it('getPrefillCount({}) = 0 (no aiData)', () => {
      expect(AssistanceEducativeSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + full aiData = 5', () => {
      const aiData = {
        aeDangerCaracterise: true,
        aeUrgence: false,
        aeAdhesionFamille: true,
        aeMaintienMilieu: true,
        aeMesureAmiable: true,
      } as FamilleExtractedData;
      expect(AssistanceEducativeSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'FRANCE' })).toBe(5);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      const aiData = { aeDangerCaracterise: true } as FamilleExtractedData;
      expect(AssistanceEducativeSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData complet → 5 signaux branchés + provenance IA', () => {
      component.aiData = {
        aeDangerCaracterise: true,
        aeUrgence: true,
        aeAdhesionFamille: false,
        aeMaintienMilieu: true,
        aeMesureAmiable: false,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dangerCaracterise()).toBe(true);
      expect(component.urgence()).toBe(true);
      expect(component.adhesionFamille()).toBe(false);
      expect(component.maintienMilieuFamilialPossible()).toBe(true);
      expect(component.mesureAmiableASEEnvisageable()).toBe(false);
      expect(component.provenanceDanger()).toBe('IA');
      expect(component.provenanceUrgence()).toBe('IA');
      expect(component.provenanceMesureAmiable()).toBe('IA');
    });

    it('BELGIQUE → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        aeDangerCaracterise: true,
        aeUrgence: true,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dangerCaracterise()).toBe(false);
      expect(component.urgence()).toBe(false);
    });

    it('standaloneMode → pas de pré-fill', () => {
      component.standaloneMode = true;
      component.aiData = { aeDangerCaracterise: true } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dangerCaracterise()).toBe(false);
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE → formValid true', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique résultat', () => {
      fixture.detectChanges();
      component.dangerCaracterise.set(true);
      component.urgence.set(false);
      component.adhesionFamille.set(false);
      component.maintienMilieuFamilialPossible.set(true);
      component.mesureAmiableASEEnvisageable.set(true);

      component.calculate();

      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        dangerCaracterise: true,
        urgence: false,
        adhesionFamille: false,
        maintienMilieuFamilialPossible: true,
        mesureAmiableASEEnvisageable: true,
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
  });

  describe('helpers d\'affichage', () => {
    it('verdictLabel mappe les 4 verdicts', () => {
      expect(component.verdictLabel('AED')).toContain('AED');
      expect(component.verdictLabel('AEMO')).toContain('AEMO');
      expect(component.verdictLabel('OPP_PLACEMENT')).toContain('placement');
      expect(component.verdictLabel('PAS_DE_MESURE')).toContain('Pas de mesure');
    });

    it('verdictClass mappe les 4 verdicts vers une classe CSS', () => {
      expect(component.verdictClass('AED')).toBe('verdict-info');
      expect(component.verdictClass('AEMO')).toBe('verdict-warning');
      expect(component.verdictClass('OPP_PLACEMENT')).toBe('verdict-danger');
      expect(component.verdictClass('PAS_DE_MESURE')).toBe('verdict-success');
    });
  });

  describe('handlers — modification manuelle efface badge IA', () => {
    it('onDangerChange efface provenance', () => {
      component.provenanceDanger.set('IA');
      component.onDangerChange(true);
      expect(component.dangerCaracterise()).toBe(true);
      expect(component.provenanceDanger()).toBeNull();
    });

    it('onMesureAmiableChange efface provenance', () => {
      component.provenanceMesureAmiable.set('IA');
      component.onMesureAmiableChange(true);
      expect(component.mesureAmiableASEEnvisageable()).toBe(true);
      expect(component.provenanceMesureAmiable()).toBeNull();
    });
  });
});
