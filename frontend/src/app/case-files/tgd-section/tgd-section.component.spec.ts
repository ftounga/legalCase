/**
 * SF-222-02 — Tests Jest pour `TgdSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (parité runtime/static F-237) ;
 *  - country gate BE → formValid false, pas de pré-fill ;
 *  - pré-fill IA branche les 5 critères détectés + provenance IA ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire (no-op) ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictLabel/verdictClass mappent les 3 verdicts.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { TgdSectionComponent } from './tgd-section.component';
import { TgdService } from '../../core/services/tgd.service';
import { TgdResponse } from '../../core/models/tgd.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('TgdSectionComponent (SF-222-02)', () => {
  let component: TgdSectionComponent;
  let fixture: ComponentFixture<TgdSectionComponent>;
  let serviceSpy: jasmine.SpyObj<TgdService>;

  const nominalResponse: TgdResponse = {
    caseFileId: 'case-1',
    verdict: 'ELIGIBLE_TGD',
    criteresManquants: [],
    basesJuridiques: ['art. 41-3-1 CPP'],
    messages: ['Tous les critères réunis.', 'Décision : procureur de la République.'],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<TgdService>('TgdService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [TgdSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: TgdService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TgdSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(TgdService) as jasmine.SpyObj<TgdService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(TgdSectionComponent.TOOL_LABEL).toBe('TGD TELEPHONE GRAVE DANGER');
      expect(TgdSectionComponent.TOOL_ICON).toBe('phone_in_talk');
    });

    it('getPrefillCount({}) = 0 (no aiData)', () => {
      expect(TgdSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + full aiData = 5', () => {
      const aiData = {
        tgdDangerGrave: true,
        tgdViolences: true,
        tgdInterdictionContact: true,
        tgdNonCohabitation: true,
        tgdConsentement: true,
      } as FamilleExtractedData;
      expect(TgdSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'FRANCE' })).toBe(5);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      const aiData = { tgdDangerGrave: true } as FamilleExtractedData;
      expect(TgdSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData complet → 5 signaux branchés + provenance IA', () => {
      component.aiData = {
        tgdDangerGrave: true,
        tgdViolences: true,
        tgdInterdictionContact: false,
        tgdNonCohabitation: true,
        tgdConsentement: true,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dangerGrave()).toBe(true);
      expect(component.violencesAvereesOuVraisemblables()).toBe(true);
      expect(component.interdictionContactProcedure()).toBe(false);
      expect(component.nonCohabitation()).toBe(true);
      expect(component.consentementVictime()).toBe(true);
      expect(component.provenanceDangerGrave()).toBe('IA');
      expect(component.provenanceInterdiction()).toBe('IA');
      expect(component.provenanceConsentement()).toBe('IA');
    });

    it('BELGIQUE → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        tgdDangerGrave: true,
        tgdViolences: true,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dangerGrave()).toBe(false);
      expect(component.violencesAvereesOuVraisemblables()).toBe(false);
    });

    it('standaloneMode → pas de pré-fill', () => {
      component.standaloneMode = true;
      component.aiData = { tgdDangerGrave: true } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dangerGrave()).toBe(false);
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
      component.dangerGrave.set(true);
      component.violencesAvereesOuVraisemblables.set(true);
      component.interdictionContactProcedure.set(true);
      component.nonCohabitation.set(true);
      component.consentementVictime.set(true);

      component.calculate();

      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        dangerGrave: true,
        violencesAvereesOuVraisemblables: true,
        interdictionContactProcedure: true,
        nonCohabitation: true,
        consentementVictime: true,
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
    it('verdictLabel mappe les 3 verdicts', () => {
      expect(component.verdictLabel('ELIGIBLE_TGD')).toContain('Éligible au TGD');
      expect(component.verdictLabel('ELIGIBLE_SOUS_RESERVE')).toContain('sous réserve');
      expect(component.verdictLabel('NON_ELIGIBLE')).toContain('Non éligible');
    });

    it('verdictClass mappe les 3 verdicts vers une classe CSS', () => {
      expect(component.verdictClass('ELIGIBLE_TGD')).toBe('verdict-success');
      expect(component.verdictClass('ELIGIBLE_SOUS_RESERVE')).toBe('verdict-info');
      expect(component.verdictClass('NON_ELIGIBLE')).toBe('verdict-warning');
    });
  });

  describe('handlers — modification manuelle efface badge IA', () => {
    it('onDangerGraveChange efface provenance', () => {
      component.provenanceDangerGrave.set('IA');
      component.onDangerGraveChange(true);
      expect(component.dangerGrave()).toBe(true);
      expect(component.provenanceDangerGrave()).toBeNull();
    });

    it('onConsentementChange efface provenance', () => {
      component.provenanceConsentement.set('IA');
      component.onConsentementChange(true);
      expect(component.consentementVictime()).toBe(true);
      expect(component.provenanceConsentement()).toBeNull();
    });
  });
});
