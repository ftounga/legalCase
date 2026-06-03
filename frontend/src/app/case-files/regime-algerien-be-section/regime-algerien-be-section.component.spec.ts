/**
 * SF-223-05 — Tests Jest pour `RegimeAlgerienBeSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 3 champs) ;
 *  - country gate FR → formValid false ;
 *  - nature + lien de rattachement requis ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (nature + date + montant) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts (dont --danger ordre public) ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { RegimeAlgerienBeSectionComponent } from './regime-algerien-be-section.component';
import { RegimeAlgerienBeService } from '../../core/services/regime-algerien-be.service';
import { RegimeAlgerienBeResponse } from '../../core/models/regime-algerien-be.model';

describe('RegimeAlgerienBeSectionComponent (SF-223-05)', () => {
  let component: RegimeAlgerienBeSectionComponent;
  let fixture: ComponentFixture<RegimeAlgerienBeSectionComponent>;
  let serviceSpy: jasmine.SpyObj<RegimeAlgerienBeService>;

  const mariageResponse: RegimeAlgerienBeResponse = {
    caseFileId: 'case-1',
    natureActe: 'MARIAGE_ALGERIEN',
    dateActe: '2024-01-01',
    consentementEpouxEpouse: true,
    dotMahrPrevue: false,
    montantDotConnu: null,
    conventionAlgeroBelgeInvoquee: false,
    lienRattachementBelgique: 'RESIDENCE',
    verdict: 'RECONNAISSANCE_DE_PLEIN_DROIT',
    motifs: ['Mariage algérien consenti et rattaché à la Belgique.'],
    effetsDot: 'Aucune dot renseignée.',
    basesJuridiques: ['CDIP — loi du 16/07/2004 (à vérifier par avocat belge)'],
    messages: ['Reconnaissance de plein droit, sous réserve du contrôle de l\'ordre public.'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-04T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<RegimeAlgerienBeService>(
      'RegimeAlgerienBeService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(mariageResponse));

    await TestBed.configureTestingModule({
      imports: [RegimeAlgerienBeSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: RegimeAlgerienBeService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegimeAlgerienBeSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(RegimeAlgerienBeService) as jasmine.SpyObj<RegimeAlgerienBeService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(RegimeAlgerienBeSectionComponent.TOOL_LABEL).toBe('RÉGIME ALGÉRIEN (BELGIQUE)');
      expect(RegimeAlgerienBeSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(RegimeAlgerienBeSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte nature + date + montant (BE)', () => {
      expect(RegimeAlgerienBeSectionComponent.getPrefillCount({
        aiData: {
          regimeAlgerienBeNatureActeDetecte: 'DOT_MAHR',
          regimeAlgerienBeDateActeDetectee: '2024-01-15',
          regimeAlgerienBeMontantDotDetecte: '5000',
        },
        workspaceCountry: 'BELGIQUE',
      })).toBe(3);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.natureActe.set('MARIAGE_ALGERIEN');
      component.lienRattachementBelgique.set('RESIDENCE');
      expect(component.formValid()).toBe(false);
    });

    it('nature ou rattachement absent → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
      component.natureActe.set('MARIAGE_ALGERIEN');
      expect(component.formValid()).toBe(false);
    });

    it('nature + rattachement renseignés (BE) → formValid true', () => {
      fixture.detectChanges();
      component.natureActe.set('MARIAGE_ALGERIEN');
      component.lienRattachementBelgique.set('RESIDENCE');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique le résultat', () => {
      fixture.detectChanges();
      component.natureActe.set('MARIAGE_ALGERIEN');
      component.lienRattachementBelgique.set('RESIDENCE');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        natureActe: 'MARIAGE_ALGERIEN',
        lienRattachementBelgique: 'RESIDENCE',
      }));
      expect(component.result()).toEqual(mariageResponse);
      expect(component.showForm()).toBe(false);
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('nature + date + montant pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        regimeAlgerienBeNatureActeDetecte: 'DOT_MAHR',
        regimeAlgerienBeDateActeDetectee: '2023-06-01',
        regimeAlgerienBeMontantDotDetecte: '5000',
      } as unknown as never;
      fixture.detectChanges();
      expect(component.natureActe()).toBe('DOT_MAHR');
      expect(component.dateActe()).toBe('2023-06-01');
      expect(component.montantDotConnu()).toBe(5000);
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
      expect(component.verdictBannerLabel('RECONNAISSANCE_DE_PLEIN_DROIT')).toContain('plein droit');
      expect(component.verdictBannerLabel('RECONNAISSANCE_SOUS_CONDITIONS')).toContain('conditions');
      expect(component.verdictBannerLabel('RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC')).toContain('ordre public');
      expect(component.verdictBannerLabel('QUALIFICATION_INCOMPLETE')).toContain('incomplète');
    });

    it('verdictBannerClass : --danger pour le refus ordre public', () => {
      expect(component.verdictBannerClass('RECONNAISSANCE_DE_PLEIN_DROIT')).toContain('--ok');
      expect(component.verdictBannerClass('RECONNAISSANCE_SOUS_CONDITIONS')).toContain('--warn');
      expect(component.verdictBannerClass('RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC')).toContain('--danger');
      expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    });

    it('verdictBannerIcon mappe les verdicts', () => {
      expect(component.verdictBannerIcon('RECONNAISSANCE_DE_PLEIN_DROIT')).toBe('check_circle');
      expect(component.verdictBannerIcon('RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC')).toBe('gavel');
      expect(component.verdictBannerIcon('QUALIFICATION_INCOMPLETE')).toBe('info');
    });

    it('toolIdForJurisprudence = regime-algerien-be (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('regime-algerien-be');
    });
  });
});
