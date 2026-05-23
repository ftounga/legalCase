/**
 * SF-216-26 — Tests Jest pour `PresomptionPaterniteFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { PresomptionPaterniteFrSectionComponent } from './presomption-paternite-fr-section.component';
import { PresomptionPaterniteFrService } from '../../core/services/presomption-paternite-fr.service';
import { PresomptionPaterniteResponse } from '../../core/models/presomption-paternite-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('PresomptionPaterniteFrSectionComponent (SF-216-26)', () => {
  let component: PresomptionPaterniteFrSectionComponent;
  let fixture: ComponentFixture<PresomptionPaterniteFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<PresomptionPaterniteFrService>;

  const nominalResponse: PresomptionPaterniteResponse = {
    caseFileId: 'case-1',
    presomptionApplicable: true,
    presomptionRenversee: false,
    voieDesaveu: 'INDETERMINE',
    delaiDesaveu: 'Désaveu non envisagé.',
    possessionEtatImpact: 'Aucune possession d\'état conforme documentée.',
    baseLegale: 'art. 312-316 Cciv + art. 333 al. 1',
    messages: ['Enfant né pendant le mariage (art. 312 Cciv).'],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<PresomptionPaterniteFrService>(
      'PresomptionPaterniteFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [
        PresomptionPaterniteFrSectionComponent,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        { provide: PresomptionPaterniteFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PresomptionPaterniteFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(PresomptionPaterniteFrService) as jasmine.SpyObj<PresomptionPaterniteFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(PresomptionPaterniteFrSectionComponent.TOOL_LABEL).toBe('PRÉSOMPTION DE PATERNITÉ');
      expect(PresomptionPaterniteFrSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(PresomptionPaterniteFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 5 champs IA = 5', () => {
      const aiData = {
        dateNaissanceEnfantDetectee: '2025-09-01',
        possessionEtatConforme5AnsDetected: true,
        dateConclusionMariageDetectee: '2020-06-01',
        dateDissolutionMariageDetectee: '2024-12-01',
        desaveuEnvisage: true,
      } as unknown as FamilleExtractedData;
      expect(
        PresomptionPaterniteFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(5);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        PresomptionPaterniteFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → 5 champs pré-remplis', () => {
      component.aiData = {
        dateNaissanceEnfantDetectee: '2025-09-01',
        possessionEtatConforme5AnsDetected: true,
        dateConclusionMariageDetectee: '2020-06-01',
        dateDissolutionMariageDetectee: '2024-12-01',
        desaveuEnvisage: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dateNaissanceEnfant()).toBe('2025-09-01');
      expect(component.possessionEtatConformeDetecte()).toBe(true);
      expect(component.dateConclusionMariage()).toBe('2020-06-01');
      expect(component.dateDissolutionMariage()).toBe('2024-12-01');
      expect(component.desaveuEnvisage()).toBe(true);
    });

    it('BELGIQUE + aiData rempli → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        dateNaissanceEnfantDetectee: '2025-09-01',
        desaveuEnvisage: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dateNaissanceEnfant()).toBeNull();
      expect(component.desaveuEnvisage()).toBe(false);
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.dateNaissanceEnfant.set('2025-09-01');
      component.dateConclusionMariage.set('2020-06-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans date naissance', () => {
      fixture.detectChanges();
      component.dateConclusionMariage.set('2020-06-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans date conclusion mariage', () => {
      fixture.detectChanges();
      component.dateNaissanceEnfant.set('2025-09-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid true avec dates obligatoires', () => {
      fixture.detectChanges();
      component.dateNaissanceEnfant.set('2025-09-01');
      component.dateConclusionMariage.set('2020-06-01');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('handlers', () => {
    it('onDateNaissanceChange vide → null', () => {
      fixture.detectChanges();
      component.onDateNaissanceChange('');
      expect(component.dateNaissanceEnfant()).toBeNull();
    });

    it('onPossessionEtatChange(true) coche le flag', () => {
      fixture.detectChanges();
      component.onPossessionEtatChange(true);
      expect(component.possessionEtatConformeDetecte()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('FRANCE — calcule et applique le résultat', () => {
      fixture.detectChanges();
      component.dateNaissanceEnfant.set('2025-09-01');
      component.dateConclusionMariage.set('2020-06-01');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalled();
      expect(component.result()?.presomptionApplicable).toBe(true);
      expect(component.showForm()).toBe(false);
    });

    it('calculate no-op si formInvalide', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('voieDesaveuLabel mappe les codes connus', () => {
      fixture.detectChanges();
      expect(component.voieDesaveuLabel('DESAVEU_RECEVABLE')).toContain('316');
      expect(component.voieDesaveuLabel('DESAVEU_DELAI_FORCLOS')).toContain('forclos');
      expect(component.voieDesaveuLabel('DESAVEU_DIFFICILE_POSSESSION_ETAT')).toContain('possession');
      expect(component.voieDesaveuLabel('INCONNU')).toBe('INCONNU');
    });

    it('presomptionLabel sans result → ""', () => {
      fixture.detectChanges();
      expect(component.presomptionLabel()).toBe('');
    });

    it('presomptionLabel applicable → 312', () => {
      fixture.detectChanges();
      component['result'].set(nominalResponse);
      expect(component.presomptionLabel()).toContain('312');
    });

    it('presomptionLabel écartée → 313', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        presomptionApplicable: false,
        presomptionRenversee: true,
      });
      expect(component.presomptionLabel()).toContain('313');
    });

    it('verdictClass forclos → verdict-blocked', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, voieDesaveu: 'DESAVEU_DELAI_FORCLOS' });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass difficile → verdict-warning', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        voieDesaveu: 'DESAVEU_DIFFICILE_POSSESSION_ETAT',
      });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass applicable sans renversement → verdict-success', () => {
      fixture.detectChanges();
      component['result'].set(nominalResponse);
      expect(component.verdictClass()).toBe('verdict-success');
    });

    it('verdictClass sans result → chaîne vide', () => {
      fixture.detectChanges();
      expect(component.verdictClass()).toBe('');
    });
  });

  describe('signaux par défaut', () => {
    it('collapsed=true initialement', () => {
      expect(component.collapsed()).toBe(true);
    });

    it('forceExpanded → ouvre la section', () => {
      component.forceExpanded = true;
      fixture.detectChanges();
      expect(component.collapsed()).toBe(false);
    });
  });
});
