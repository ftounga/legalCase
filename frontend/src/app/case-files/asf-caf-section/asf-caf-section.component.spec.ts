/**
 * SF-222-01 — Tests Jest pour `AsfCafSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (parité runtime/static F-237) ;
 *  - country gate BE → formValid false, pas de pré-fill ;
 *  - pré-fill IA branche les 5 champs détectés + provenance IA ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - bouton désactivé (formValid false) si nb enfants vide ;
 *  - GET 404 → mode formulaire (no-op) ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictLabel/verdictClass mappent les 4 verdicts.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AsfCafSectionComponent } from './asf-caf-section.component';
import { AsfCafService } from '../../core/services/asf-caf.service';
import { AsfCafResponse } from '../../core/models/asf-caf.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AsfCafSectionComponent (SF-222-01)', () => {
  let component: AsfCafSectionComponent;
  let fixture: ComponentFixture<AsfCafSectionComponent>;
  let serviceSpy: jasmine.SpyObj<AsfCafService>;

  const nominalResponse: AsfCafResponse = {
    caseFileId: 'case-1',
    verdict: 'DROIT_AVEC_RECOUVREMENT',
    montantMensuelEstime: 187,
    recouvrementApplicable: true,
    basesJuridiques: ['art. L. 523-1 CSS'],
    messages: ['Voir l\'outil ARIPA recouvrement (F-FA-ARIPA-RECOUVREMENT).'],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AsfCafService>('AsfCafService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [AsfCafSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: AsfCafService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AsfCafSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(AsfCafService) as jasmine.SpyObj<AsfCafService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(AsfCafSectionComponent.TOOL_LABEL).toBe('ASF ALLOCATION SOUTIEN FAMILIAL');
      expect(AsfCafSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount({}) = 0 (no aiData)', () => {
      expect(AsfCafSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + full aiData = 5', () => {
      const aiData = {
        asfParentIsole: true,
        asfPensionFixee: true,
        asfMontantPension: 300,
        asfPensionPayee: 'NON_PAYEE',
        asfNbEnfants: 2,
      } as FamilleExtractedData;
      expect(AsfCafSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'FRANCE' })).toBe(5);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      const aiData = { asfParentIsole: true } as FamilleExtractedData;
      expect(AsfCafSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData complet → 5 signaux branchés + provenance IA', () => {
      component.aiData = {
        asfParentIsole: true,
        asfPensionFixee: true,
        asfMontantPension: 250,
        asfPensionPayee: 'PARTIELLE',
        asfNbEnfants: 1,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.parentIsole()).toBe(true);
      expect(component.pensionFixee()).toBe(true);
      expect(component.montantPensionMensuel()).toBe(250);
      expect(component.pensionPayee()).toBe('PARTIELLE');
      expect(component.nbEnfantsACharge()).toBe(1);
      expect(component.provenanceParentIsole()).toBe('IA');
      expect(component.provenancePensionPayee()).toBe('IA');
      expect(component.provenanceNbEnfants()).toBe('IA');
    });

    it('BELGIQUE → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        asfParentIsole: true,
        asfMontantPension: 250,
        asfNbEnfants: 1,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.parentIsole()).toBe(false);
      expect(component.montantPensionMensuel()).toBeNull();
      expect(component.nbEnfantsACharge()).toBeNull();
    });

    it('standaloneMode → pas de pré-fill', () => {
      component.standaloneMode = true;
      component.aiData = { asfNbEnfants: 2 } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.nbEnfantsACharge()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.nbEnfantsACharge.set(1);
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + nb enfants ≥ 1 → formValid true', () => {
      fixture.detectChanges();
      component.nbEnfantsACharge.set(1);
      expect(component.formValid()).toBe(true);
    });

    it('nb enfants vide → formValid false (bouton désactivé)', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('nb enfants = 0 → formValid false', () => {
      fixture.detectChanges();
      component.nbEnfantsACharge.set(0);
      expect(component.formValid()).toBe(false);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique résultat', () => {
      fixture.detectChanges();
      component.parentIsole.set(true);
      component.pensionFixee.set(true);
      component.montantPensionMensuel.set(300);
      component.pensionPayee.set('NON_PAYEE');
      component.nbEnfantsACharge.set(1);

      component.calculate();

      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        parentIsole: true,
        pensionFixee: true,
        montantPensionMensuel: 300,
        pensionPayee: 'NON_PAYEE',
        nbEnfantsACharge: 1,
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
    it('formatEuros → fr-FR currency', () => {
      const out = component.formatEuros(187);
      expect(out).toContain('1');
      expect(out).toContain('€');
    });

    it('verdictLabel mappe les 4 verdicts', () => {
      expect(component.verdictLabel('DROIT_ASF_PLEIN')).toContain('taux plein');
      expect(component.verdictLabel('DROIT_ASF_DIFFERENTIELLE')).toContain('différentielle');
      expect(component.verdictLabel('DROIT_AVEC_RECOUVREMENT')).toContain('recouvrement');
      expect(component.verdictLabel('PAS_DE_DROIT')).toContain('Pas de droit');
    });

    it('verdictClass mappe les 4 verdicts vers une classe CSS', () => {
      expect(component.verdictClass('DROIT_ASF_PLEIN')).toBe('verdict-success');
      expect(component.verdictClass('DROIT_AVEC_RECOUVREMENT')).toBe('verdict-info');
      expect(component.verdictClass('PAS_DE_DROIT')).toBe('verdict-warning');
    });
  });

  describe('handlers — modification manuelle efface badge IA', () => {
    it('onMontantPensionChange efface provenance', () => {
      component.provenanceMontantPension.set('IA');
      component.onMontantPensionChange(500);
      expect(component.montantPensionMensuel()).toBe(500);
      expect(component.provenanceMontantPension()).toBeNull();
    });

    it('onNbEnfantsChange efface provenance', () => {
      component.provenanceNbEnfants.set('IA');
      component.onNbEnfantsChange(3);
      expect(component.nbEnfantsACharge()).toBe(3);
      expect(component.provenanceNbEnfants()).toBeNull();
    });
  });
});
