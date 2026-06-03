/**
 * SF-223-04 — Tests Jest pour `GpaBeSituationContentieuseSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 2 champs) ;
 *  - country gate FR → formValid false ;
 *  - lieu + lien génétique requis ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (lieu + lien) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { GpaBeSituationContentieuseSectionComponent } from './gpa-be-situation-contentieuse-section.component';
import { GpaBeSituationContentieuseService } from '../../core/services/gpa-be-situation-contentieuse.service';
import { GpaBeResponse } from '../../core/models/gpa-be-situation-contentieuse.model';

describe('GpaBeSituationContentieuseSectionComponent (SF-223-04)', () => {
  let component: GpaBeSituationContentieuseSectionComponent;
  let fixture: ComponentFixture<GpaBeSituationContentieuseSectionComponent>;
  let serviceSpy: jasmine.SpyObj<GpaBeSituationContentieuseService>;

  const reconnaissanceResponse: GpaBeResponse = {
    caseFileId: 'case-1',
    gpaRealiseeEnBelgiqueOuEtranger: 'BELGIQUE',
    lienGenetiqueParentIntentionnel: 'PERE_INTENTIONNEL',
    acteNaissanceEtrangerEtabli: false,
    merePorteuseDesignee: true,
    consentementMerePorteuse: true,
    coupleIntentionnelMarieOuCohabitant: true,
    commentaire: null,
    verdict: 'FILIATION_PAR_RECONNAISSANCE',
    cheminContentieux: ['Établir la filiation par reconnaissance.'],
    risques: ['La convention de GPA n\'est pas opposable.'],
    basesJuridiques: ['Principe mater semper certa (CC — à vérifier)'],
    messages: ['Un parent intentionnel a un lien génétique avec l\'enfant.'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-03T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<GpaBeSituationContentieuseService>(
      'GpaBeSituationContentieuseService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(reconnaissanceResponse));

    await TestBed.configureTestingModule({
      imports: [GpaBeSituationContentieuseSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: GpaBeSituationContentieuseService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GpaBeSituationContentieuseSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(GpaBeSituationContentieuseService) as jasmine.SpyObj<GpaBeSituationContentieuseService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(GpaBeSituationContentieuseSectionComponent.TOOL_LABEL).toBe('GPA — FILIATION (BELGIQUE)');
      expect(GpaBeSituationContentieuseSectionComponent.TOOL_ICON).toBe('family_restroom');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(GpaBeSituationContentieuseSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte lieu + lien (BE)', () => {
      expect(GpaBeSituationContentieuseSectionComponent.getPrefillCount({
        aiData: { gpaBeLieuDetecte: 'ETRANGER', gpaBeLienGenetiqueDetecte: 'AUCUN' },
        workspaceCountry: 'BELGIQUE',
      })).toBe(2);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.gpaRealiseeEnBelgiqueOuEtranger.set('BELGIQUE');
      component.lienGenetiqueParentIntentionnel.set('PERE_INTENTIONNEL');
      expect(component.formValid()).toBe(false);
    });

    it('lieu ou lien absent → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
      component.gpaRealiseeEnBelgiqueOuEtranger.set('BELGIQUE');
      expect(component.formValid()).toBe(false);
    });

    it('lieu + lien renseignés (BE) → formValid true', () => {
      fixture.detectChanges();
      component.gpaRealiseeEnBelgiqueOuEtranger.set('BELGIQUE');
      component.lienGenetiqueParentIntentionnel.set('PERE_INTENTIONNEL');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique le résultat', () => {
      fixture.detectChanges();
      component.gpaRealiseeEnBelgiqueOuEtranger.set('BELGIQUE');
      component.lienGenetiqueParentIntentionnel.set('PERE_INTENTIONNEL');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        gpaRealiseeEnBelgiqueOuEtranger: 'BELGIQUE',
        lienGenetiqueParentIntentionnel: 'PERE_INTENTIONNEL',
      }));
      expect(component.result()).toEqual(reconnaissanceResponse);
      expect(component.showForm()).toBe(false);
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('lieu + lien pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        gpaBeLieuDetecte: 'ETRANGER',
        gpaBeLienGenetiqueDetecte: 'AUCUN',
      } as unknown as never;
      fixture.detectChanges();
      expect(component.gpaRealiseeEnBelgiqueOuEtranger()).toBe('ETRANGER');
      expect(component.lienGenetiqueParentIntentionnel()).toBe('AUCUN');
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
      expect(component.verdictBannerLabel('FILIATION_PAR_RECONNAISSANCE')).toContain('reconnaissance');
      expect(component.verdictBannerLabel('FILIATION_PAR_ADOPTION_POST_NAISSANCE')).toContain('adoption');
      expect(component.verdictBannerLabel('RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE')).toContain('étranger');
      expect(component.verdictBannerLabel('QUALIFICATION_INCOMPLETE')).toContain('incomplète');
    });

    it('verdictBannerClass : pas de rouge (vide juridique — orientation)', () => {
      expect(component.verdictBannerClass('FILIATION_PAR_RECONNAISSANCE')).toContain('--ok');
      expect(component.verdictBannerClass('FILIATION_PAR_ADOPTION_POST_NAISSANCE')).toContain('--warn');
      expect(component.verdictBannerClass('RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE')).toContain('--warn');
      expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
      expect(component.verdictBannerClass('FILIATION_PAR_ADOPTION_POST_NAISSANCE')).not.toContain('--danger');
    });

    it('verdictBannerIcon mappe les verdicts', () => {
      expect(component.verdictBannerIcon('FILIATION_PAR_RECONNAISSANCE')).toBe('check_circle');
      expect(component.verdictBannerIcon('RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE')).toBe('public');
      expect(component.verdictBannerIcon('QUALIFICATION_INCOMPLETE')).toBe('info');
    });

    it('toolIdForJurisprudence = gpa-be-situation-contentieuse (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('gpa-be-situation-contentieuse');
    });
  });
});
