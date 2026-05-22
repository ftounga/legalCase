/**
 * SF-216-12 — Tests Jest pour `RetraitApFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { RetraitApFrSectionComponent } from './retrait-ap-fr-section.component';
import { RetraitApFrService } from '../../core/services/retrait-ap-fr.service';
import { RetraitAutoriteParentaleResponse } from '../../core/models/retrait-ap-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('RetraitApFrSectionComponent (SF-216-12)', () => {
  let component: RetraitApFrSectionComponent;
  let fixture: ComponentFixture<RetraitApFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<RetraitApFrService>;

  const nominalResponse: RetraitAutoriteParentaleResponse = {
    caseFileId: 'case-1',
    verdictRetrait: 'RETRAIT_CIVIL_JAF',
    voieProcedurale: 'JAF_TRIBUNAL_JUDICIAIRE',
    admissibiliteAdoption: true,
    consequencesJuridiques: ['Délégation à un tiers possible'],
    etapes: ['Saisir le JAF', 'Documenter le motif'],
    dureeEstimeeJours: 240,
    baseLegale: 'art. 378-381 Cciv',
    messages: [],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<RetraitApFrService>('RetraitApFrService', [
      'calculate',
      'get',
    ]);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [RetraitApFrSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: RetraitApFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RetraitApFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(RetraitApFrService) as jasmine.SpyObj<RetraitApFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(RetraitApFrSectionComponent.TOOL_LABEL).toBe('RETRAIT AUTORITE PARENTALE');
      expect(RetraitApFrSectionComponent.TOOL_ICON).toBe('gpp_bad');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(RetraitApFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + full aiData = 4', () => {
      const aiData: FamilleExtractedData = {
        agesEnfantsDetectes: [8],
        condamnationPenaleDetectee: true,
        dangerImmediatDetected: true,
        violencesLmvss2022Detectees: true,
      };
      expect(RetraitApFrSectionComponent.getPrefillCount({
        aiData,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      const aiData: FamilleExtractedData = {
        agesEnfantsDetectes: [8],
        condamnationPenaleDetectee: true,
        dangerImmediatDetected: true,
        violencesLmvss2022Detectees: true,
      };
      expect(RetraitApFrSectionComponent.getPrefillCount({
        aiData,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });
  });

  describe('country gate', () => {
    it('workspaceCountry BE → formValid() false', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.typeRetrait.set('TOTAL');
      component.motifRetrait.set('CONDAMNATION_PENALE');
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
    it('FRANCE + full aiData → 4 champs + badges IA', () => {
      component.aiData = {
        agesEnfantsDetectes: [10],
        condamnationPenaleDetectee: true,
        dangerImmediatDetected: true,
        violencesLmvss2022Detectees: false,
        violencesAllegueesDetectees: ['PHYSIQUES'],
      };
      component.ngOnInit();
      expect(component.ageEnfant()).toBe(10);
      expect(component.condamnationPenaleDetectee()).toBe(true);
      expect(component.dangerCaracterise()).toBe(true);
      // LMVSS=false mais violences alléguées non vides → true via fallback F-246
      expect(component.violencesConjugalesDetectees()).toBe(true);
      expect(component.provenanceAgeEnfant()).toBe('IA');
      expect(component.provenanceCondamnationPenale()).toBe('IA');
      expect(component.provenanceDanger()).toBe('IA');
      expect(component.provenanceViolencesConjugales()).toBe('IA');
    });

    it('FRANCE + standaloneMode → pas de pré-fill', () => {
      component.standaloneMode = true;
      component.aiData = {
        agesEnfantsDetectes: [10],
        condamnationPenaleDetectee: true,
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
      component.typeRetrait.set('TOTAL');
      component.motifRetrait.set('CONDAMNATION_PENALE');
      component.ageEnfant.set(8);
      expect(component.formValid()).toBe(true);
    });

    it('FR + type manquant → false', () => {
      component.motifRetrait.set('CONDAMNATION_PENALE');
      component.ageEnfant.set(8);
      expect(component.formValid()).toBe(false);
    });

    it('FR + motif manquant → false', () => {
      component.typeRetrait.set('TOTAL');
      component.ageEnfant.set(8);
      expect(component.formValid()).toBe(false);
    });

    it('FR + âge > 18 → false', () => {
      component.typeRetrait.set('TOTAL');
      component.motifRetrait.set('CONDAMNATION_PENALE');
      component.ageEnfant.set(19);
      expect(component.formValid()).toBe(false);
    });
  });

  describe('calculate', () => {
    it('appelle service avec body correct', () => {
      component.typeRetrait.set('TOTAL');
      component.motifRetrait.set('CONDAMNATION_PENALE');
      component.ageEnfant.set(8);
      component.condamnationPenaleDetectee.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        typeRetrait: 'TOTAL',
        motifRetrait: 'CONDAMNATION_PENALE',
        ageEnfant: 8,
        condamnationPenaleDetectee: true,
      }));
    });

    it('résultat → showForm false + result populé', () => {
      component.typeRetrait.set('TOTAL');
      component.motifRetrait.set('CONDAMNATION_PENALE');
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
      expect(component.verdictLabel('RETRAIT_PLEIN_DROIT')).toContain('plein droit');
      expect(component.verdictLabel('RETRAIT_CIVIL_JAF')).toContain('civil');
      expect(component.verdictLabel('SUSPENSION_ACCELEREE_LMVSS_2022')).toContain('Suspension');
      expect(component.verdictLabel('IRRECEVABLE_ENFANT_MAJEUR')).toContain('majeur');
      expect(component.verdictLabel('IRRECEVABLE_MOTIF_NON_CARACTERISE')).toContain('caractérisé');
    });

    it('verdictClass mappe danger/primary/warning', () => {
      expect(component.verdictClass('RETRAIT_PLEIN_DROIT')).toBe('verdict-danger');
      expect(component.verdictClass('RETRAIT_CIVIL_JAF')).toBe('verdict-primary');
      expect(component.verdictClass('SUSPENSION_ACCELEREE_LMVSS_2022')).toBe('verdict-danger');
      expect(component.verdictClass('IRRECEVABLE_ENFANT_MAJEUR')).toBe('verdict-warning');
    });

    it('voieLabel mappe les 5 voies', () => {
      expect(component.voieLabel('JURIDICTION_PENALE_ACCESSOIRE')).toContain('pénale');
      expect(component.voieLabel('JAF_TRIBUNAL_JUDICIAIRE')).toContain('JAF');
      expect(component.voieLabel('PROCUREUR_REPUBLIQUE_ASSISTANCE_EDUCATIVE')).toContain('Procureur');
      expect(component.voieLabel('LMVSS_2022_SUSPENSION_AUTOMATIQUE')).toContain('Suspension');
      expect(component.voieLabel('SANS_OBJET')).toBe('Sans objet');
    });
  });
});
