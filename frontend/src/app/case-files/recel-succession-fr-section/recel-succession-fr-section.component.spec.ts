/**
 * SF-216-22 — Tests Jest pour `RecelSuccessionFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { RecelSuccessionFrSectionComponent } from './recel-succession-fr-section.component';
import { RecelSuccessionFrService } from '../../core/services/recel-succession-fr.service';
import { RecelSuccessionResponse } from '../../core/models/recel-succession-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('RecelSuccessionFrSectionComponent (SF-216-22)', () => {
  let component: RecelSuccessionFrSectionComponent;
  let fixture: ComponentFixture<RecelSuccessionFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<RecelSuccessionFrService>;

  const nominalResponse: RecelSuccessionResponse = {
    caseFileId: 'case-1',
    qualificationRecel: 'CARACTERISE',
    verdictRecel: 'RECEL_CARACTERISE',
    sanctionCivile: 'Privation de la part successorale sur le bien recelé.',
    preuveRequise: 'Charge probatoire sur les cohéritiers demandeurs.',
    delaiAction: 'Prescription quinquennale (art. 2224 Cciv) — années écoulées : 1.',
    delaiForclos: false,
    voletPenalSignale: false,
    baseLegale: 'art. 778 Cciv + Cass. 1ère civ., 14/11/2012',
    messages: ['Preuve directe disponible (DOCUMENT).'],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<RecelSuccessionFrService>(
      'RecelSuccessionFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [
        RecelSuccessionFrSectionComponent,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        { provide: RecelSuccessionFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RecelSuccessionFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(RecelSuccessionFrService) as jasmine.SpyObj<RecelSuccessionFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(RecelSuccessionFrSectionComponent.TOOL_LABEL).toBe('RECEL DE SUCCESSION');
      expect(RecelSuccessionFrSectionComponent.TOOL_ICON).toBe('visibility_off');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(RecelSuccessionFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 3 champs IA = 3', () => {
      const aiData = {
        typeRecelDetecte: 'DISSIMULATION_BIEN',
        preuveRecelDetectee: 'DOCUMENT',
        dateOuvertureSuccessionDetectee: '2024-08-01',
      } as unknown as FamilleExtractedData;
      expect(
        RecelSuccessionFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(3);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        RecelSuccessionFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → typeRecel + preuveRecel + date pré-remplis', () => {
      component.aiData = {
        typeRecelDetecte: 'DESTRUCTION_TESTAMENT',
        preuveRecelDetectee: 'FAISCEAU_INDICES',
        dateOuvertureSuccessionDetectee: '2024-12-01',
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.typeRecel()).toBe('DESTRUCTION_TESTAMENT');
      expect(component.preuveRecel()).toBe('FAISCEAU_INDICES');
      expect(component.dateOuvertureSuccession()).toBe('2024-12-01');
    });

    it('BELGIQUE + aiData rempli → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        typeRecelDetecte: 'DISSIMULATION_BIEN',
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.typeRecel()).toBeNull();
      expect(component.dateOuvertureSuccession()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.typeRecel.set('DISSIMULATION_BIEN');
      component.preuveRecel.set('AVEUX');
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans type', () => {
      fixture.detectChanges();
      component.preuveRecel.set('AVEUX');
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans dateOuverture', () => {
      fixture.detectChanges();
      component.typeRecel.set('DISSIMULATION_BIEN');
      component.preuveRecel.set('AVEUX');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans preuve', () => {
      fixture.detectChanges();
      component.typeRecel.set('DISSIMULATION_BIEN');
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid true avec type + preuve + dateOuverture', () => {
      fixture.detectChanges();
      component.typeRecel.set('DISSIMULATION_BIEN');
      component.preuveRecel.set('DOCUMENT');
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('FRANCE — calcule et applique le résultat', () => {
      fixture.detectChanges();
      component.typeRecel.set('DISSIMULATION_BIEN');
      component.preuveRecel.set('DOCUMENT');
      component.dateOuvertureSuccession.set('2025-01-01');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalled();
      expect(component.result()?.qualificationRecel).toBe('CARACTERISE');
      expect(component.showForm()).toBe(false);
    });

    it('calculate no-op si formInvalide', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('qualificationLabel mappe les codes connus', () => {
      fixture.detectChanges();
      expect(component.qualificationLabel('CARACTERISE')).toContain('caractérisé');
      expect(component.qualificationLabel('PROBABLE')).toContain('probable');
      expect(component.qualificationLabel('INSUFFISANT')).toContain('insuffisante');
      expect(component.qualificationLabel('HORS_PERIMETRE')).toContain('Hors');
      expect(component.qualificationLabel('PAS_DE_RECEL')).toContain('Pas');
      expect(component.qualificationLabel('INCONNU')).toBe('INCONNU');
    });

    it('verdictClass CARACTERISE → verdict-success', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, qualificationRecel: 'CARACTERISE' });
      expect(component.verdictClass()).toBe('verdict-success');
    });

    it('verdictClass PROBABLE → verdict-warning', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        qualificationRecel: 'PROBABLE',
        verdictRecel: 'RECEL_PROBABLE_FAISCEAU',
      });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass forclos → verdict-blocked', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        qualificationRecel: 'CARACTERISE',
        delaiForclos: true,
      });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass HORS_PERIMETRE → verdict-blocked', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        qualificationRecel: 'HORS_PERIMETRE',
        verdictRecel: 'QUALITE_RECELEUR_EXCLUE',
      });
      expect(component.verdictClass()).toBe('verdict-blocked');
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
