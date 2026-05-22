/**
 * SF-216-18 — Tests Jest pour `AdoptionInternationaleFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AdoptionInternationaleFrSectionComponent } from './adoption-internationale-fr-section.component';
import { AdoptionInternationaleFrService } from '../../core/services/adoption-internationale-fr.service';
import { AdoptionInternationaleResponse } from '../../core/models/adoption-internationale-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AdoptionInternationaleFrSectionComponent (SF-216-18)', () => {
  let component: AdoptionInternationaleFrSectionComponent;
  let fixture: ComponentFixture<AdoptionInternationaleFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<AdoptionInternationaleFrService>;

  const nominalResponse: AdoptionInternationaleResponse = {
    caseFileId: 'case-1',
    conditionsRemplies: true,
    voieProcedure: 'OAA_AGREE',
    conventionApplicable: true,
    alerteKafala: false,
    exequaturRequis: false,
    delaiEstime: '2 à 5 ans',
    verdict: 'OAA_CONVENTION',
    baseLegale: 'art. 370-3 Cciv + Convention La Haye 1993',
    messages: ['Convention applicable'],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AdoptionInternationaleFrService>(
      'AdoptionInternationaleFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [AdoptionInternationaleFrSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: AdoptionInternationaleFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdoptionInternationaleFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(AdoptionInternationaleFrService) as jasmine.SpyObj<AdoptionInternationaleFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(AdoptionInternationaleFrSectionComponent.TOOL_LABEL).toBe('ADOPTION INTERNATIONALE');
      expect(AdoptionInternationaleFrSectionComponent.TOOL_ICON).toBe('public');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(AdoptionInternationaleFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 3 champs IA = 3', () => {
      const aiData = {
        paysOrigineAdopteDetecte: 'VIETNAM',
        agrement2025DetecteValide: true,
        exequaturRequisDetecte: false,
      } as unknown as FamilleExtractedData;
      expect(
        AdoptionInternationaleFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(3);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        AdoptionInternationaleFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → pays + agrément + exequatur pré-remplis', () => {
      component.aiData = {
        paysOrigineAdopteDetecte: 'COLOMBIE',
        agrement2025DetecteValide: true,
        exequaturRequisDetecte: false,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.paysOrigineEnfant()).toBe('COLOMBIE');
      expect(component.agrement2025()).toBe(true);
      expect(component.exequaturRequis()).toBe(false);
    });

    it('BELGIQUE + aiData rempli → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        paysOrigineAdopteDetecte: 'VIETNAM',
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.paysOrigineEnfant()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.paysOrigineEnfant.set('VIETNAM');
      component.agrement2025.set(true);
      component.ageAdoptant.set(40);
      component.ageAdopte.set(4);
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + champs requis → formValid true', () => {
      fixture.detectChanges();
      component.paysOrigineEnfant.set('VIETNAM');
      component.agrement2025.set(true);
      component.ageAdoptant.set(40);
      component.ageAdopte.set(4);
      expect(component.formValid()).toBe(true);
    });

    it('pays vide → formValid false', () => {
      fixture.detectChanges();
      component.agrement2025.set(true);
      component.ageAdoptant.set(40);
      component.ageAdopte.set(4);
      expect(component.formValid()).toBe(false);
    });

    it('agrement2025 null → formValid false', () => {
      fixture.detectChanges();
      component.paysOrigineEnfant.set('VIETNAM');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(4);
      expect(component.formValid()).toBe(false);
    });
  });

  describe('isKafalaPays', () => {
    it('MAROC → true', () => {
      component.paysOrigineEnfant.set('MAROC');
      expect(component.isKafalaPays()).toBe(true);
    });

    it('ALGERIE → true', () => {
      component.paysOrigineEnfant.set('ALGERIE');
      expect(component.isKafalaPays()).toBe(true);
    });

    it('TUNISIE → true', () => {
      component.paysOrigineEnfant.set('TUNISIE');
      expect(component.isKafalaPays()).toBe(true);
    });

    it('VIETNAM → false', () => {
      component.paysOrigineEnfant.set('VIETNAM');
      expect(component.isKafalaPays()).toBe(false);
    });

    it('pays null → false', () => {
      expect(component.isKafalaPays()).toBe(false);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique résultat', () => {
      fixture.detectChanges();
      component.paysOrigineEnfant.set('VIETNAM');
      component.conventionLaHayeApplicable.set(true);
      component.agrement2025.set(true);
      component.ageAdoptant.set(42);
      component.ageAdopte.set(4);
      component.voieProcedure.set('OAA_AGREE');
      component.formeAdoptionDemandee.set('PLENIERE');

      component.calculate();

      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        paysOrigineEnfant: 'VIETNAM',
        agrement2025: true,
        voieProcedure: 'OAA_AGREE',
        formeAdoptionDemandee: 'PLENIERE',
      }));
      expect(component.result()).toEqual(nominalResponse);
      expect(component.showForm()).toBe(false);
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('voieLabel mappe les 3 voies', () => {
      expect(component.voieLabel('OAA_AGREE')).toContain('OAA');
      expect(component.voieLabel('VOIE_AUTONOME')).toContain('autonome');
      expect(component.voieLabel('ORGANISME_ETAT')).toContain('État');
    });

    it('verdictClass — null sans résultat', () => {
      expect(component.verdictClass()).toBe('');
    });

    it('verdictClass — blocked si kafala', () => {
      component.result.set({ ...nominalResponse, alerteKafala: true, conditionsRemplies: false });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass — warning si voie autonome risque', () => {
      component.result.set({ ...nominalResponse, verdict: 'VOIE_AUTONOME_RISQUE' });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass — success en nominal', () => {
      component.result.set(nominalResponse);
      expect(component.verdictClass()).toBe('verdict-success');
    });
  });
});
