/**
 * SF-216-10 — Tests Jest pour `DelegationApFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { DelegationApFrSectionComponent } from './delegation-ap-fr-section.component';
import { DelegationApFrService } from '../../core/services/delegation-ap-fr.service';
import { DelegationApFrResponse } from '../../core/models/delegation-ap-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('DelegationApFrSectionComponent (SF-216-10)', () => {
  let component: DelegationApFrSectionComponent;
  let fixture: ComponentFixture<DelegationApFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<DelegationApFrService>;

  const nominalResponse: DelegationApFrResponse = {
    caseFileId: 'case-1',
    verdictRecevabilite: 'RECEVABLE_VOLONTAIRE',
    voieProcedurale: 'VOLONTAIRE_CONJOINTE',
    etapes: ['Rédiger l\'acte', 'Saisir le JAF'],
    dureeEstimeeJours: 60,
    baseLegale: 'art. 376-1 Cciv',
    messages: [],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<DelegationApFrService>('DelegationApFrService', [
      'calculate',
      'get',
    ]);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [DelegationApFrSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: DelegationApFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DelegationApFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(DelegationApFrService) as jasmine.SpyObj<DelegationApFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(DelegationApFrSectionComponent.TOOL_LABEL).toBe('DELEGATION AUTORITE PARENTALE');
      expect(DelegationApFrSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(DelegationApFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + full aiData = 3', () => {
      const aiData: FamilleExtractedData = {
        agesEnfantsDetectes: [8],
        tiersLienFamilialDetecte: 'GRANDS_PARENTS',
        accordParentsDetecte: true,
      };
      expect(DelegationApFrSectionComponent.getPrefillCount({
        aiData,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      const aiData: FamilleExtractedData = {
        agesEnfantsDetectes: [8],
        tiersLienFamilialDetecte: 'GRANDS_PARENTS',
        accordParentsDetecte: true,
      };
      expect(DelegationApFrSectionComponent.getPrefillCount({
        aiData,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });
  });

  describe('country gate', () => {
    it('workspaceCountry BE → formValid() false', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.typeDelegation.set('VOLONTAIRE_CONJOINTE');
      component.tiersLienFamilial.set('GRANDS_PARENTS');
      component.ageEnfant.set(8);
      expect(component.formValid()).toBe(false);
    });

    it('workspaceCountry BE + ngOnInit → pas d\'appel GET', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.ngOnInit();
      expect(serviceSpy.get).not.toHaveBeenCalled();
    });

    it('workspaceCountry FR + ngOnInit → appel GET', () => {
      component.ngOnInit();
      expect(serviceSpy.get).toHaveBeenCalledWith('case-1');
      expect(component.showForm()).toBe(true);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + full aiData → 3 champs + badges IA', () => {
      component.aiData = {
        agesEnfantsDetectes: [10],
        tiersLienFamilialDetecte: 'ONCLE_TANTE',
        accordParentsDetecte: false,
      };
      component.ngOnInit();
      expect(component.ageEnfant()).toBe(10);
      expect(component.tiersLienFamilial()).toBe('ONCLE_TANTE');
      expect(component.accordParents()).toBe(false);
      expect(component.provenanceAgeEnfant()).toBe('IA');
      expect(component.provenanceTiersLien()).toBe('IA');
      expect(component.provenanceAccordParents()).toBe('IA');
    });

    it('FRANCE + standaloneMode → pas de pré-fill', () => {
      component.standaloneMode = true;
      component.aiData = {
        agesEnfantsDetectes: [10],
        tiersLienFamilialDetecte: 'GRANDS_PARENTS',
        accordParentsDetecte: true,
      };
      component.ngOnInit();
      expect(component.ageEnfant()).toBeNull();
    });

    it('handler manuel → badge IA effacé', () => {
      component.aiData = { agesEnfantsDetectes: [10] };
      component.ngOnInit();
      expect(component.provenanceAgeEnfant()).toBe('IA');
      component.onAgeEnfantChange(12);
      expect(component.ageEnfant()).toBe(12);
      expect(component.provenanceAgeEnfant()).toBeNull();
    });
  });

  describe('formValid', () => {
    it('FR + champs complets → true', () => {
      component.typeDelegation.set('VOLONTAIRE_CONJOINTE');
      component.tiersLienFamilial.set('GRANDS_PARENTS');
      component.ageEnfant.set(8);
      expect(component.formValid()).toBe(true);
    });

    it('FR + type manquant → false', () => {
      component.tiersLienFamilial.set('GRANDS_PARENTS');
      component.ageEnfant.set(8);
      expect(component.formValid()).toBe(false);
    });

    it('FR + âge > 18 → false', () => {
      component.typeDelegation.set('VOLONTAIRE_CONJOINTE');
      component.tiersLienFamilial.set('GRANDS_PARENTS');
      component.ageEnfant.set(19);
      expect(component.formValid()).toBe(false);
    });
  });

  describe('calculate', () => {
    it('appelle service avec body correct', () => {
      component.typeDelegation.set('VOLONTAIRE_CONJOINTE');
      component.tiersLienFamilial.set('GRANDS_PARENTS');
      component.ageEnfant.set(8);
      component.accordParents.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        typeDelegation: 'VOLONTAIRE_CONJOINTE',
        tiersLienFamilial: 'GRANDS_PARENTS',
        ageEnfant: 8,
        accordParents: true,
      }));
    });

    it('motivation vide → null dans le body', () => {
      component.typeDelegation.set('VOLONTAIRE_CONJOINTE');
      component.tiersLienFamilial.set('GRANDS_PARENTS');
      component.ageEnfant.set(8);
      component.motivationDelegation.set('   ');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        motivationDelegation: null,
      }));
    });

    it('résultat → showForm false', () => {
      component.typeDelegation.set('VOLONTAIRE_CONJOINTE');
      component.tiersLienFamilial.set('GRANDS_PARENTS');
      component.ageEnfant.set(8);
      component.calculate();
      expect(component.result()).toEqual(nominalResponse);
      expect(component.showForm()).toBe(false);
    });

    it('form invalide → pas d\'appel', () => {
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('verdictLabel mappe les 5 verdicts', () => {
      expect(component.verdictLabel('RECEVABLE_VOLONTAIRE')).toContain('volontaire');
      expect(component.verdictLabel('IRRECEVABLE_ENFANT_MAJEUR')).toContain('majeur');
    });

    it('verdictClass mappe success/primary/warning', () => {
      expect(component.verdictClass('RECEVABLE_VOLONTAIRE')).toBe('verdict-success');
      expect(component.verdictClass('IRRECEVABLE_ENFANT_MAJEUR')).toBe('verdict-warning');
    });
  });
});
