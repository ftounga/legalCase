/**
 * SF-223-06 — Tests Jest pour `RegimeBeSeparationBiensSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 4 champs) ;
 *  - country gate FR → formValid false ;
 *  - variante requise ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - clause de participation null hors variante participation ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (variante + date + patrimoines) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { RegimeBeSeparationBiensSectionComponent } from './regime-be-separation-biens-section.component';
import { RegimeBeSeparationBiensService } from '../../core/services/regime-be-separation-biens.service';
import { RegimeBeSeparationBiensResponse } from '../../core/models/regime-be-separation-biens.model';

describe('RegimeBeSeparationBiensSectionComponent (SF-223-06)', () => {
  let component: RegimeBeSeparationBiensSectionComponent;
  let fixture: ComponentFixture<RegimeBeSeparationBiensSectionComponent>;
  let serviceSpy: jasmine.SpyObj<RegimeBeSeparationBiensService>;

  const pureResponse: RegimeBeSeparationBiensResponse = {
    caseFileId: 'case-1',
    varianteRegime: 'SEPARATION_PURE',
    contratMariageNotarie: true,
    clauseParticipationPrevue: null,
    disproportionPatrimonialeAllegee: false,
    dateContrat: '2020-01-01',
    patrimoinePropreEpoux1Eur: null,
    patrimoinePropreEpoux2Eur: null,
    verdict: 'SEPARATION_PURE_QUALIFIEE',
    motifs: ['Séparation de biens pure : aucune masse commune.'],
    effetsPatrimoniaux: 'Aucune masse commune : chaque époux conserve ses biens.',
    basesJuridiques: ['Livre 3 du Code civil belge (à vérifier par avocat belge)'],
    messages: ['Séparation de biens pure : pas de créance de participation.'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-04T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<RegimeBeSeparationBiensService>(
      'RegimeBeSeparationBiensService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(pureResponse));

    await TestBed.configureTestingModule({
      imports: [RegimeBeSeparationBiensSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: RegimeBeSeparationBiensService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegimeBeSeparationBiensSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(RegimeBeSeparationBiensService) as jasmine.SpyObj<RegimeBeSeparationBiensService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(RegimeBeSeparationBiensSectionComponent.TOOL_LABEL).toBe('SÉPARATION DE BIENS (BELGIQUE)');
      expect(RegimeBeSeparationBiensSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(RegimeBeSeparationBiensSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte variante + date + 2 patrimoines (BE)', () => {
      expect(RegimeBeSeparationBiensSectionComponent.getPrefillCount({
        aiData: {
          regimeSeparationBiensBeVarianteDetectee: 'SEPARATION_AVEC_PARTICIPATION_ACQUETS',
          regimeSeparationBiensBeDateContratDetectee: '2020-01-15',
          regimeSeparationBiensBePatrimoineEpoux1Detecte: '120000',
          regimeSeparationBiensBePatrimoineEpoux2Detecte: '80000',
        },
        workspaceCountry: 'BELGIQUE',
      })).toBe(4);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.varianteRegime.set('SEPARATION_PURE');
      expect(component.formValid()).toBe(false);
    });

    it('variante absente → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('variante renseignée (BE) → formValid true', () => {
      fixture.detectChanges();
      component.varianteRegime.set('SEPARATION_PURE');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique le résultat', () => {
      fixture.detectChanges();
      component.varianteRegime.set('SEPARATION_PURE');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        varianteRegime: 'SEPARATION_PURE',
      }));
      expect(component.result()).toEqual(pureResponse);
      expect(component.showForm()).toBe(false);
    });

    it('clause de participation null hors variante participation', () => {
      fixture.detectChanges();
      component.varianteRegime.set('SEPARATION_PURE');
      component.clauseParticipationPrevue.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        clauseParticipationPrevue: null,
      }));
    });

    it('clause de participation transmise pour variante participation', () => {
      fixture.detectChanges();
      component.varianteRegime.set('SEPARATION_AVEC_PARTICIPATION_ACQUETS');
      component.clauseParticipationPrevue.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        clauseParticipationPrevue: true,
      }));
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('variante + date + patrimoines pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        regimeSeparationBiensBeVarianteDetectee: 'SEPARATION_AVEC_PARTICIPATION_ACQUETS',
        regimeSeparationBiensBeDateContratDetectee: '2019-06-01',
        regimeSeparationBiensBePatrimoineEpoux1Detecte: '120000',
        regimeSeparationBiensBePatrimoineEpoux2Detecte: '80000',
      } as unknown as never;
      fixture.detectChanges();
      expect(component.varianteRegime()).toBe('SEPARATION_AVEC_PARTICIPATION_ACQUETS');
      expect(component.dateContrat()).toBe('2019-06-01');
      expect(component.patrimoinePropreEpoux1Eur()).toBe(120000);
      expect(component.patrimoinePropreEpoux2Eur()).toBe(80000);
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
      expect(component.verdictBannerLabel('SEPARATION_PURE_QUALIFIEE')).toContain('pure');
      expect(component.verdictBannerLabel('SOCIETE_ACQUETS_QUALIFIEE')).toContain('acquêts');
      expect(component.verdictBannerLabel('PARTICIPATION_ACQUETS_QUALIFIEE')).toContain('Participation');
      expect(component.verdictBannerLabel('QUALIFICATION_INCOMPLETE')).toContain('incomplète');
    });

    it('verdictBannerClass : --ok pure, --warn acquêts/participation, --info incomplète', () => {
      expect(component.verdictBannerClass('SEPARATION_PURE_QUALIFIEE')).toContain('--ok');
      expect(component.verdictBannerClass('SOCIETE_ACQUETS_QUALIFIEE')).toContain('--warn');
      expect(component.verdictBannerClass('PARTICIPATION_ACQUETS_QUALIFIEE')).toContain('--warn');
      expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    });

    it('verdictBannerIcon mappe les verdicts', () => {
      expect(component.verdictBannerIcon('SEPARATION_PURE_QUALIFIEE')).toBe('check_circle');
      expect(component.verdictBannerIcon('PARTICIPATION_ACQUETS_QUALIFIEE')).toBe('rule');
      expect(component.verdictBannerIcon('QUALIFICATION_INCOMPLETE')).toBe('info');
    });

    it('toolIdForJurisprudence = regime-be-separation-biens (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('regime-be-separation-biens');
    });
  });
});
