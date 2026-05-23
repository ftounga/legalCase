/**
 * SF-216-14 — Tests Jest pour `AuditionMineurFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AuditionMineurFrSectionComponent } from './audition-mineur-fr-section.component';
import { AuditionMineurFrService } from '../../core/services/audition-mineur-fr.service';
import { AuditionMineurResponse } from '../../core/models/audition-mineur-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AuditionMineurFrSectionComponent (SF-216-14)', () => {
  let component: AuditionMineurFrSectionComponent;
  let fixture: ComponentFixture<AuditionMineurFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<AuditionMineurFrService>;

  const nominalResponse: AuditionMineurResponse = {
    caseFileId: 'case-1',
    conditionsRemplies: true,
    droitAuditionReconnu: true,
    modaliteRecommandee: 'AVEC_AVOCAT',
    refusContestable: false,
    verdict: 'AUDITION_RECOMMANDEE',
    baseLegale: 'art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC',
    messages: ['Modalité recommandée'],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AuditionMineurFrService>(
      'AuditionMineurFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [AuditionMineurFrSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: AuditionMineurFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditionMineurFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(AuditionMineurFrService) as jasmine.SpyObj<AuditionMineurFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(AuditionMineurFrSectionComponent.TOOL_LABEL).toBe('AUDITION DU MINEUR');
      expect(AuditionMineurFrSectionComponent.TOOL_ICON).toBe('record_voice_over');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(AuditionMineurFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 2 champs IA = 2', () => {
      const aiData = {
        agesEnfantsDetectes: [10],
        demandeAuditionFormaliseeDetectee: true,
      } as unknown as FamilleExtractedData;
      expect(
        AuditionMineurFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(2);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        AuditionMineurFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → âge + demande pré-remplis', () => {
      component.aiData = {
        agesEnfantsDetectes: [12, 8],
        demandeAuditionFormaliseeDetectee: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.ageEnfant()).toBe(12);
      expect(component.demandeFormalisee()).toBe(true);
    });

    it('BELGIQUE → pas de pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        agesEnfantsDetectes: [12],
        demandeAuditionFormaliseeDetectee: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.ageEnfant()).toBeNull();
      expect(component.demandeFormalisee()).toBe(false);
    });

    it('FRANCE + aiData null → pas de pré-fill', () => {
      component.aiData = null;
      fixture.detectChanges();
      expect(component.ageEnfant()).toBeNull();
    });
  });

  describe('isJeuneEnfant (alerte UI < 5 ans)', () => {
    it('age 3 → true', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.ageEnfant.set(3);
      expect(component.isJeuneEnfant()).toBe(true);
    });

    it('age 10 → false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.ageEnfant.set(10);
      expect(component.isJeuneEnfant()).toBe(false);
    });

    it('age null → false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      expect(component.isJeuneEnfant()).toBe(false);
    });
  });

  describe('formValid', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('BELGIQUE → false', () => {
      component.workspaceCountry = 'BELGIQUE';
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + âge null → false', () => {
      component.ageEnfant.set(null);
      component.capaciteDiscernement.set('PROBABLE');
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + âge négatif → false', () => {
      component.ageEnfant.set(-1);
      component.capaciteDiscernement.set('PROBABLE');
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + âge >= 18 → false', () => {
      component.ageEnfant.set(18);
      component.capaciteDiscernement.set('PROBABLE');
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + capacité null → false', () => {
      component.ageEnfant.set(10);
      component.capaciteDiscernement.set(null);
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + âge + capacité valides → true', () => {
      component.ageEnfant.set(10);
      component.capaciteDiscernement.set('PROBABLE');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('soumission valide → applique le résultat', () => {
      fixture.detectChanges();
      component.ageEnfant.set(10);
      component.capaciteDiscernement.set('PROBABLE');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalled();
      expect(component.result()).toEqual(nominalResponse);
      expect(component.showForm()).toBe(false);
    });

    it('soumission invalide → pas d\'appel API', () => {
      fixture.detectChanges();
      // ageEnfant null → invalid
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('handlers', () => {
    beforeEach(() => fixture.detectChanges());

    it('onRefusMotiveChange(false) reset motivation', () => {
      component.motivationRefus.set('motif');
      component.onRefusMotiveChange(false);
      expect(component.motivationRefus()).toBeNull();
    });

    it('onMotivationRefusChange(\' \') → null', () => {
      component.onMotivationRefusChange('   ');
      expect(component.motivationRefus()).toBeNull();
    });

    it('onMotivationRefusChange(\'motif\') → motif', () => {
      component.onMotivationRefusChange('motif');
      expect(component.motivationRefus()).toBe('motif');
    });
  });

  describe('helpers d\'affichage', () => {
    beforeEach(() => fixture.detectChanges());

    it('modaliteLabel SEUL', () => {
      expect(component.modaliteLabel('SEUL')).toContain('seul');
    });

    it('modaliteLabel AVEC_AVOCAT', () => {
      expect(component.modaliteLabel('AVEC_AVOCAT')).toContain('avocat');
    });

    it('modaliteLabel AVEC_TIERS', () => {
      expect(component.modaliteLabel('AVEC_TIERS')).toContain('tiers');
    });

    it('verdictClass refus contestable → warning', () => {
      component.result.set({ ...nominalResponse, refusContestable: true });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass conditions remplies → success', () => {
      component.result.set(nominalResponse);
      expect(component.verdictClass()).toBe('verdict-success');
    });

    it('verdictClass aucun résultat → \'\'', () => {
      component.result.set(null);
      expect(component.verdictClass()).toBe('');
    });
  });
});
