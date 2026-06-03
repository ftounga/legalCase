/**
 * SF-223-02 — Tests Jest pour `AdoptionBeSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 2 champs) ;
 *  - country gate FR → formValid false ;
 *  - type + âges + consentement parents requis ;
 *  - co-parentale reset des toggles quand type ≠ CO_PARENTALE ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (type + agrément) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AdoptionBeSectionComponent } from './adoption-be-section.component';
import { AdoptionBeService } from '../../core/services/adoption-be.service';
import { AdoptionBeResponse } from '../../core/models/adoption-be.model';

describe('AdoptionBeSectionComponent (SF-223-02)', () => {
  let component: AdoptionBeSectionComponent;
  let fixture: ComponentFixture<AdoptionBeSectionComponent>;
  let serviceSpy: jasmine.SpyObj<AdoptionBeService>;

  const recevableResponse: AdoptionBeResponse = {
    caseFileId: 'case-1',
    typeAdoption: 'PLENIERE',
    ageAdoptant: 40,
    ageAdopte: 8,
    ecartAgeAnnees: null,
    adoptantEnCoupleAvecParentBiologique: null,
    lienCohabitationLegaleOuMariage: null,
    agrementObtenu: true,
    consentementAdopteOuRepresentants: true,
    consentementParentsBiologiques: 'OBTENU',
    commentaire: null,
    verdict: 'RECEVABLE',
    motifsConditions: [],
    actesAProduire: ['Préparer et déposer la requête en adoption devant le tribunal de la famille.'],
    basesJuridiques: ['CC art. 343-1 et s. (à vérifier)'],
    messages: ['Les conditions de recevabilité paraissent réunies.'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-03T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AdoptionBeService>(
      'AdoptionBeService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(recevableResponse));

    await TestBed.configureTestingModule({
      imports: [AdoptionBeSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: AdoptionBeService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdoptionBeSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(AdoptionBeService) as jasmine.SpyObj<AdoptionBeService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(AdoptionBeSectionComponent.TOOL_LABEL).toBe('ADOPTION — RECEVABILITÉ (BELGIQUE)');
      expect(AdoptionBeSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(AdoptionBeSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte type + agrément (BE)', () => {
      expect(AdoptionBeSectionComponent.getPrefillCount({
        aiData: { adoptionBeTypeDetecte: 'SIMPLE', adoptionBeAgrementMentionneBeDetecte: true },
        workspaceCountry: 'BELGIQUE',
      })).toBe(2);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.typeAdoption.set('PLENIERE');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      component.consentementParentsBiologiques.set('OBTENU');
      expect(component.formValid()).toBe(false);
    });

    it('type absent → formValid false', () => {
      fixture.detectChanges();
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      component.consentementParentsBiologiques.set('OBTENU');
      expect(component.formValid()).toBe(false);
    });

    it('âges manquants → formValid false', () => {
      fixture.detectChanges();
      component.typeAdoption.set('PLENIERE');
      component.consentementParentsBiologiques.set('OBTENU');
      expect(component.formValid()).toBe(false);
    });

    it('tous champs requis → formValid true', () => {
      fixture.detectChanges();
      component.typeAdoption.set('PLENIERE');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      component.consentementParentsBiologiques.set('OBTENU');
      expect(component.formValid()).toBe(true);
    });

    it('onTypeChange remet les toggles co-parentale à false si type ≠ CO_PARENTALE', () => {
      fixture.detectChanges();
      component.adoptantEnCoupleAvecParentBiologique.set(true);
      component.lienCohabitationLegaleOuMariage.set(true);
      component.onTypeChange('PLENIERE');
      expect(component.adoptantEnCoupleAvecParentBiologique()).toBe(false);
      expect(component.lienCohabitationLegaleOuMariage()).toBe(false);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique le résultat', () => {
      fixture.detectChanges();
      component.typeAdoption.set('PLENIERE');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      component.consentementParentsBiologiques.set('OBTENU');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        typeAdoption: 'PLENIERE',
        ageAdoptant: 40,
        ageAdopte: 8,
        consentementParentsBiologiques: 'OBTENU',
        adoptantEnCoupleAvecParentBiologique: null,
      }));
      expect(component.result()).toEqual(recevableResponse);
      expect(component.showForm()).toBe(false);
    });

    it('CO_PARENTALE → envoie les flags de lien dans le body', () => {
      fixture.detectChanges();
      component.onTypeChange('CO_PARENTALE');
      component.ageAdoptant.set(40);
      component.ageAdopte.set(8);
      component.consentementParentsBiologiques.set('SANS_OBJET');
      component.adoptantEnCoupleAvecParentBiologique.set(true);
      component.lienCohabitationLegaleOuMariage.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        typeAdoption: 'CO_PARENTALE',
        adoptantEnCoupleAvecParentBiologique: true,
        lienCohabitationLegaleOuMariage: true,
      }));
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('type + agrément pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        adoptionBeTypeDetecte: 'SIMPLE',
        adoptionBeAgrementMentionneBeDetecte: true,
      } as unknown as never;
      fixture.detectChanges();
      expect(component.typeAdoption()).toBe('SIMPLE');
      expect(component.agrementObtenu()).toBe(true);
    });
  });

  describe('GET au chargement', () => {
    it('404 → reste en mode formulaire', () => {
      fixture.detectChanges();
      expect(component.showForm()).toBe(true);
      expect(component.result()).toBeNull();
    });
  });

  describe('helpers d\'affichage', () => {
    it('verdictBannerLabel mappe les 4 verdicts', () => {
      expect(component.verdictBannerLabel('RECEVABLE')).toBe('Recevable');
      expect(component.verdictBannerLabel('RECEVABLE_SOUS_CONDITIONS')).toContain('conditions');
      expect(component.verdictBannerLabel('IRRECEVABLE')).toBe('Irrecevable');
      expect(component.verdictBannerLabel('QUALIFICATION_INCOMPLETE')).toContain('incomplète');
    });

    it('verdictBannerClass : rouge réservé à IRRECEVABLE', () => {
      expect(component.verdictBannerClass('RECEVABLE')).toContain('--ok');
      expect(component.verdictBannerClass('RECEVABLE_SOUS_CONDITIONS')).toContain('--warn');
      expect(component.verdictBannerClass('IRRECEVABLE')).toContain('--danger');
      expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    });

    it('verdictBannerIcon mappe les verdicts', () => {
      expect(component.verdictBannerIcon('RECEVABLE')).toBe('check_circle');
      expect(component.verdictBannerIcon('IRRECEVABLE')).toBe('error');
      expect(component.verdictBannerIcon('RECEVABLE_SOUS_CONDITIONS')).toBe('rule');
    });

    it('toolIdForJurisprudence = adoption-be (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('adoption-be');
    });
  });
});
