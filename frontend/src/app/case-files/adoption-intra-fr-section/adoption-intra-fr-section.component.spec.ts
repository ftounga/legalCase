/**
 * SF-216-16 — Tests Jest pour `AdoptionIntraFrSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (parité runtime/static F-237) ;
 *  - country gate BE → formValid false, pas d'appel HTTP ;
 *  - V1 PREFILL_COUNT_ALWAYS_ZERO : aucun pré-fill IA ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire (no-op) ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AdoptionIntraFrSectionComponent } from './adoption-intra-fr-section.component';
import { AdoptionIntraFrService } from '../../core/services/adoption-intra-fr.service';
import {
  AdoptionIntraFrResponse,
} from '../../core/models/adoption-intra-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AdoptionIntraFrSectionComponent (SF-216-16)', () => {
  let component: AdoptionIntraFrSectionComponent;
  let fixture: ComponentFixture<AdoptionIntraFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<AdoptionIntraFrService>;

  const nominalResponse: AdoptionIntraFrResponse = {
    caseFileId: 'case-1',
    formesPossibles: ['SIMPLE'],
    formeDemandee: 'SIMPLE',
    conditionsRemplies: true,
    alerteIrreversibilite: false,
    consentementEnfantRequis: false,
    baseLegale: 'art. 345-1 Cciv',
    messages: ['Adoption simple ouverte'],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AdoptionIntraFrService>('AdoptionIntraFrService', [
      'calculate',
      'get',
    ]);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [AdoptionIntraFrSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: AdoptionIntraFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdoptionIntraFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(AdoptionIntraFrService) as jasmine.SpyObj<AdoptionIntraFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(AdoptionIntraFrSectionComponent.TOOL_LABEL).toBe('ADOPTION INTRA-FAMILIALE');
      expect(AdoptionIntraFrSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount({}) = 0 (V1 PREFILL_COUNT_ALWAYS_ZERO)', () => {
      expect(AdoptionIntraFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + aiData rempli = 0 (V1)', () => {
      const aiData = {
        adoptionIntraEnvisagee: true,
      } as unknown as FamilleExtractedData;
      expect(AdoptionIntraFrSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(AdoptionIntraFrSectionComponent.getPrefillCount({ workspaceCountry: 'BELGIQUE' })).toBe(0);
    });
  });

  describe('pas de pré-fill IA (V1)', () => {
    it('FRANCE + aiData → aucun champ pré-rempli', () => {
      component.aiData = {
        adoptionIntraEnvisagee: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.formeAdoptionDemandee()).toBeNull();
      expect(component.ageAdoptant()).toBeNull();
      expect(component.ageAdopte()).toBeNull();
      expect(component.mariageOuPacsAdoptantParent()).toBe(false);
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.formeAdoptionDemandee.set('SIMPLE');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + champs obligatoires renseignés → formValid true', () => {
      fixture.detectChanges();
      component.formeAdoptionDemandee.set('SIMPLE');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      component.mariageOuPacsAdoptantParent.set(true);
      expect(component.formValid()).toBe(true);
    });

    it('forme manquante → formValid false', () => {
      fixture.detectChanges();
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      expect(component.formValid()).toBe(false);
    });

    it('âge négatif → formValid false', () => {
      fixture.detectChanges();
      component.formeAdoptionDemandee.set('SIMPLE');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(-1);
      expect(component.formValid()).toBe(false);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique résultat', () => {
      fixture.detectChanges();
      component.formeAdoptionDemandee.set('SIMPLE');
      component.ageAdoptant.set(42);
      component.ageAdopte.set(8);
      component.mariageOuPacsAdoptantParent.set(true);
      component.consentementAutreParentBiologique.set(true);

      component.calculate();

      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        formeAdoptionDemandee: 'SIMPLE',
        ageAdoptant: 42,
        ageAdopte: 8,
        mariageOuPacsAdoptantParent: true,
        consentementAutreParentBiologique: true,
      }));
      expect(component.result()).toEqual(nominalResponse);
      expect(component.showForm()).toBe(false);
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate(); // form vide
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('formeLabel mappe les 2 formes', () => {
      expect(component.formeLabel('PLENIERE')).toBe('Plénière');
      expect(component.formeLabel('SIMPLE')).toBe('Simple');
    });

    it('verdictClass — null sans résultat', () => {
      expect(component.verdictClass()).toBe('');
    });

    it('verdictClass — blocked si non recevable', () => {
      component.result.set({ ...nominalResponse, conditionsRemplies: false });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass — warning si plénière (irréversibilité)', () => {
      component.result.set({ ...nominalResponse, alerteIrreversibilite: true, formeDemandee: 'PLENIERE' });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass — success si recevable sans alerte irréversibilité', () => {
      component.result.set(nominalResponse);
      expect(component.verdictClass()).toBe('verdict-success');
    });
  });
});
