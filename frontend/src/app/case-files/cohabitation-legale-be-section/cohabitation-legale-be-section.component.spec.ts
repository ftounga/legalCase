/**
 * SF-223-01 — Tests Jest pour `CohabitationLegaleBeSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper et vaut toujours 0 (PREFILL_COUNT_ALWAYS_ZERO) ;
 *  - country gate FR → formValid false ;
 *  - vue requise + mode dissolution requis si DISSOLUTION ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire (no-op) ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { CohabitationLegaleBeSectionComponent } from './cohabitation-legale-be-section.component';
import { CohabitationLegaleBeService } from '../../core/services/cohabitation-legale-be.service';
import { CohabitationLegaleBeResponse } from '../../core/models/cohabitation-legale-be.model';

describe('CohabitationLegaleBeSectionComponent (SF-223-01)', () => {
  let component: CohabitationLegaleBeSectionComponent;
  let fixture: ComponentFixture<CohabitationLegaleBeSectionComponent>;
  let serviceSpy: jasmine.SpyObj<CohabitationLegaleBeService>;

  const formationValideResponse: CohabitationLegaleBeResponse = {
    caseFileId: 'case-1',
    vue: 'FORMATION',
    deuxPersonnesNonMariees: true,
    capaciteJuridique: true,
    pasDejaLieParMariageOuAutreCohabitation: true,
    domicileCommun: true,
    logementFamilialEnJeu: false,
    modeDissolutionEnvisage: null,
    commentaire: null,
    verdict: 'FORMATION_VALIDE',
    conditions: [],
    actesAProduire: ['Préparer la déclaration devant l\'officier de l\'état civil.'],
    basesJuridiques: ['CC art. 1475 (à vérifier)'],
    messages: ['Conditions de l\'art. 1475 CC réunies.'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-03T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<CohabitationLegaleBeService>(
      'CohabitationLegaleBeService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(formationValideResponse));

    await TestBed.configureTestingModule({
      imports: [CohabitationLegaleBeSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: CohabitationLegaleBeService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CohabitationLegaleBeSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(CohabitationLegaleBeService) as jasmine.SpyObj<CohabitationLegaleBeService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(CohabitationLegaleBeSectionComponent.TOOL_LABEL).toBe('COHABITATION LÉGALE — RÉGIME (BELGIQUE)');
      expect(CohabitationLegaleBeSectionComponent.TOOL_ICON).toBe('diversity_3');
    });

    it('PREFILL_COUNT_ALWAYS_ZERO = true et getPrefillCount toujours 0', () => {
      expect(CohabitationLegaleBeSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
      expect(CohabitationLegaleBeSectionComponent.getPrefillCount({})).toBe(0);
      expect(CohabitationLegaleBeSectionComponent.getPrefillCount({
        aiData: { cohabitationLegaleBeDetectee: true },
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.vue.set('FORMATION');
      expect(component.formValid()).toBe(false);
    });

    it('vue absente → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('FORMATION renseignée → formValid true', () => {
      fixture.detectChanges();
      component.vue.set('FORMATION');
      expect(component.formValid()).toBe(true);
    });

    it('DISSOLUTION sans mode → formValid false', () => {
      fixture.detectChanges();
      component.onVueChange('DISSOLUTION');
      expect(component.formValid()).toBe(false);
    });

    it('DISSOLUTION avec mode → formValid true', () => {
      fixture.detectChanges();
      component.onVueChange('DISSOLUTION');
      component.modeDissolutionEnvisage.set('DECLARATION_UNILATERALE');
      expect(component.formValid()).toBe(true);
    });

    it('onVueChange remet le mode à null si vue ≠ DISSOLUTION', () => {
      fixture.detectChanges();
      component.modeDissolutionEnvisage.set('MARIAGE');
      component.onVueChange('FORMATION');
      expect(component.modeDissolutionEnvisage()).toBeNull();
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique le résultat', () => {
      fixture.detectChanges();
      component.vue.set('FORMATION');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        vue: 'FORMATION',
        modeDissolutionEnvisage: null,
      }));
      expect(component.result()).toEqual(formationValideResponse);
      expect(component.showForm()).toBe(false);
    });

    it('DISSOLUTION → envoie le mode dans le body', () => {
      fixture.detectChanges();
      component.onVueChange('DISSOLUTION');
      component.modeDissolutionEnvisage.set('DECLARATION_UNILATERALE');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        vue: 'DISSOLUTION',
        modeDissolutionEnvisage: 'DECLARATION_UNILATERALE',
      }));
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
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
      expect(component.verdictBannerLabel('FORMATION_VALIDE')).toContain('valide');
      expect(component.verdictBannerLabel('FORMATION_IMPOSSIBLE')).toContain('impossible');
      expect(component.verdictBannerLabel('EFFETS_QUALIFIES')).toContain('Effets');
      expect(component.verdictBannerLabel('DISSOLUTION_QUALIFIEE')).toContain('Dissolution');
    });

    it('verdictBannerClass : rouge réservé à FORMATION_IMPOSSIBLE', () => {
      expect(component.verdictBannerClass('FORMATION_VALIDE')).toContain('--ok');
      expect(component.verdictBannerClass('FORMATION_IMPOSSIBLE')).toContain('--danger');
      expect(component.verdictBannerClass('EFFETS_QUALIFIES')).toContain('--info');
      expect(component.verdictBannerClass('DISSOLUTION_QUALIFIEE')).toContain('--info');
    });

    it('verdictBannerIcon mappe les verdicts', () => {
      expect(component.verdictBannerIcon('FORMATION_VALIDE')).toBe('check_circle');
      expect(component.verdictBannerIcon('FORMATION_IMPOSSIBLE')).toBe('error');
      expect(component.verdictBannerIcon('DISSOLUTION_QUALIFIEE')).toBe('info');
    });

    it('toolIdForJurisprudence = cohabitation-legale-be (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('cohabitation-legale-be');
    });
  });
});
