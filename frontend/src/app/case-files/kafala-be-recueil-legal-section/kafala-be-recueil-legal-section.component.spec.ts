/**
 * SF-223-03 — Tests Jest pour `KafalaBeRecueilLegalSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 2 champs) ;
 *  - country gate FR → formValid false ;
 *  - objectif requis ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (pays + date) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { KafalaBeRecueilLegalSectionComponent } from './kafala-be-recueil-legal-section.component';
import { KafalaBeRecueilLegalService } from '../../core/services/kafala-be-recueil-legal.service';
import { KafalaBeResponse } from '../../core/models/kafala-be-recueil-legal.model';

describe('KafalaBeRecueilLegalSectionComponent (SF-223-03)', () => {
  let component: KafalaBeRecueilLegalSectionComponent;
  let fixture: ComponentFixture<KafalaBeRecueilLegalSectionComponent>;
  let serviceSpy: jasmine.SpyObj<KafalaBeRecueilLegalService>;

  const recueilResponse: KafalaBeResponse = {
    caseFileId: 'case-1',
    paysOrigineKafala: 'MA',
    dateActeKafala: '2024-01-10',
    acteOfficielJudiciaireOuAdoul: true,
    enfantAbandonneOuOrphelin: true,
    kafilDomicilieBelgique: true,
    consentementParentsBiologiquesOuAutorite: true,
    objectifEnvisage: 'RECUEIL_PROTECTION',
    commentaire: null,
    verdict: 'RECUEIL_RECONNU_COMME_PROTECTION',
    motifsConditions: [],
    actesAProduire: ['Faire valoir le recueil légal comme mesure de protection via le CDIP.'],
    basesJuridiques: ['CC art. 343 al. 2 nouveau (à vérifier)'],
    messages: ['Le recueil légal est reconnaissable comme mesure de protection.'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-03T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<KafalaBeRecueilLegalService>(
      'KafalaBeRecueilLegalService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(recueilResponse));

    await TestBed.configureTestingModule({
      imports: [KafalaBeRecueilLegalSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: KafalaBeRecueilLegalService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(KafalaBeRecueilLegalSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(KafalaBeRecueilLegalService) as jasmine.SpyObj<KafalaBeRecueilLegalService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(KafalaBeRecueilLegalSectionComponent.TOOL_LABEL).toBe('KAFALA — RECUEIL LÉGAL (BELGIQUE)');
      expect(KafalaBeRecueilLegalSectionComponent.TOOL_ICON).toBe('volunteer_activism');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(KafalaBeRecueilLegalSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte pays + date (BE)', () => {
      expect(KafalaBeRecueilLegalSectionComponent.getPrefillCount({
        aiData: { kafalaPaysOrigineDetecte: 'DZ', kafalaDateActeDetectee: '2024-01-10' },
        workspaceCountry: 'BELGIQUE',
      })).toBe(2);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.objectifEnvisage.set('RECUEIL_PROTECTION');
      expect(component.formValid()).toBe(false);
    });

    it('objectif absent → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('objectif renseigné (BE) → formValid true', () => {
      fixture.detectChanges();
      component.objectifEnvisage.set('RECUEIL_PROTECTION');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique le résultat', () => {
      fixture.detectChanges();
      component.objectifEnvisage.set('RECUEIL_PROTECTION');
      component.paysOrigineKafala.set('ma');
      component.acteOfficielJudiciaireOuAdoul.set(true);
      component.consentementParentsBiologiquesOuAutorite.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        objectifEnvisage: 'RECUEIL_PROTECTION',
        paysOrigineKafala: 'MA',
        acteOfficielJudiciaireOuAdoul: true,
        consentementParentsBiologiquesOuAutorite: true,
      }));
      expect(component.result()).toEqual(recueilResponse);
      expect(component.showForm()).toBe(false);
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('pays + date pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        kafalaPaysOrigineDetecte: 'DZ',
        kafalaDateActeDetectee: '2024-01-10',
      } as unknown as never;
      fixture.detectChanges();
      expect(component.paysOrigineKafala()).toBe('DZ');
      expect(component.dateActeKafala()).toBe('2024-01-10');
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
      expect(component.verdictBannerLabel('RECUEIL_RECONNU_COMME_PROTECTION')).toContain('protection');
      expect(component.verdictBannerLabel('RECONNAISSANCE_SOUS_CONDITIONS')).toContain('conditions');
      expect(component.verdictBannerLabel('EFFET_ADOPTIF_EXCLU')).toContain('adoptif');
      expect(component.verdictBannerLabel('QUALIFICATION_INCOMPLETE')).toContain('incomplète');
    });

    it('verdictBannerClass : rouge réservé à EFFET_ADOPTIF_EXCLU', () => {
      expect(component.verdictBannerClass('RECUEIL_RECONNU_COMME_PROTECTION')).toContain('--ok');
      expect(component.verdictBannerClass('RECONNAISSANCE_SOUS_CONDITIONS')).toContain('--warn');
      expect(component.verdictBannerClass('EFFET_ADOPTIF_EXCLU')).toContain('--danger');
      expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    });

    it('verdictBannerIcon mappe les verdicts', () => {
      expect(component.verdictBannerIcon('RECUEIL_RECONNU_COMME_PROTECTION')).toBe('check_circle');
      expect(component.verdictBannerIcon('EFFET_ADOPTIF_EXCLU')).toBe('error');
      expect(component.verdictBannerIcon('RECONNAISSANCE_SOUS_CONDITIONS')).toBe('rule');
    });

    it('toolIdForJurisprudence = kafala-be-recueil-legal (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('kafala-be-recueil-legal');
    });
  });
});
