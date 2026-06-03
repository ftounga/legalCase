/**
 * SF-223-08 — Tests Jest pour
 * `DipBeReconnaissanceDecisionEtrangereSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 3 champs) ;
 *  - country gate FR → formValid false ;
 *  - nature requise ;
 *  - calculate() appelle le service avec le bon body (ISO-2 normalisé, booleans
 *    de fond uniquement pour le jugement) ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (nature + pays + date) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { DipBeReconnaissanceDecisionEtrangereSectionComponent } from './dip-be-reconnaissance-decision-etrangere-section.component';
import { DipBeReconnaissanceDecisionEtrangereService } from '../../core/services/dip-be-reconnaissance-decision-etrangere.service';
import { DipBeReconnaissanceDecisionEtrangereResponse } from '../../core/models/dip-be-reconnaissance-decision-etrangere.model';

describe('DipBeReconnaissanceDecisionEtrangereSectionComponent (SF-223-08)', () => {
  let component: DipBeReconnaissanceDecisionEtrangereSectionComponent;
  let fixture: ComponentFixture<DipBeReconnaissanceDecisionEtrangereSectionComponent>;
  let serviceSpy: jasmine.SpyObj<DipBeReconnaissanceDecisionEtrangereService>;

  const jugementResponse: DipBeReconnaissanceDecisionEtrangereResponse = {
    caseFileId: 'case-1',
    natureDecision: 'JUGEMENT_ETRANGER_HORS_UE',
    paysOrigine: 'MA',
    dateDecision: '2023-01-01',
    decisionDefinitive: true,
    droitsDefenseRespectes: true,
    conformiteOrdrePublicBelge: true,
    absenceFraude: true,
    mariageCivilPrealable: null,
    verdict: 'RECONNAISSANCE_DE_PLEIN_DROIT',
    motifs: ['La décision est définitive et conforme.'],
    conseils: ['Vérifier que la décision est hors UE.'],
    actesAProduire: ['Légalisation / apostille.'],
    basesJuridiques: ['CDIP belge (loi du 16/07/2004) — à vérifier par avocat belge'],
    messages: ['Reconnaissance de plein droit (CDIP art. 22).'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-04T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<DipBeReconnaissanceDecisionEtrangereService>(
      'DipBeReconnaissanceDecisionEtrangereService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(jugementResponse));

    await TestBed.configureTestingModule({
      imports: [DipBeReconnaissanceDecisionEtrangereSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: DipBeReconnaissanceDecisionEtrangereService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DipBeReconnaissanceDecisionEtrangereSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(DipBeReconnaissanceDecisionEtrangereService) as jasmine.SpyObj<DipBeReconnaissanceDecisionEtrangereService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(DipBeReconnaissanceDecisionEtrangereSectionComponent.TOOL_LABEL)
        .toContain('RECONNAISSANCE');
      expect(DipBeReconnaissanceDecisionEtrangereSectionComponent.TOOL_ICON).toBe('gavel');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(DipBeReconnaissanceDecisionEtrangereSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte nature + pays + date (BE)', () => {
      expect(DipBeReconnaissanceDecisionEtrangereSectionComponent.getPrefillCount({
        aiData: {
          dipReconnaissanceNatureDetectee: 'JUGEMENT_ETRANGER_HORS_UE',
          dipReconnaissancePaysDetecte: 'MA',
          dipReconnaissanceDateDetectee: '2023-01-01',
        },
        workspaceCountry: 'BELGIQUE',
      })).toBe(3);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.natureDecision.set('JUGEMENT_ETRANGER_HORS_UE');
      expect(component.formValid()).toBe(false);
    });

    it('nature absente → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('nature renseignée (BE) → formValid true', () => {
      fixture.detectChanges();
      component.natureDecision.set('JUGEMENT_ETRANGER_HORS_UE');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('jugement : envoie les booleans de fond + ISO-2 normalisé', () => {
      fixture.detectChanges();
      component.natureDecision.set('JUGEMENT_ETRANGER_HORS_UE');
      component.paysOrigine.set('ma');
      component.decisionDefinitive.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        natureDecision: 'JUGEMENT_ETRANGER_HORS_UE',
        paysOrigine: 'MA',
        decisionDefinitive: true,
      }));
      expect(component.result()).toEqual(jugementResponse);
      expect(component.showForm()).toBe(false);
    });

    it('mariage religieux : booleans de fond du jugement → null', () => {
      fixture.detectChanges();
      component.natureDecision.set('MARIAGE_RELIGIEUX_NON_CIVIL');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        decisionDefinitive: null,
        droitsDefenseRespectes: null,
        absenceFraude: null,
      }));
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('nature + pays + date pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        dipReconnaissanceNatureDetectee: 'MARIAGE_RELIGIEUX_NON_CIVIL',
        dipReconnaissancePaysDetecte: 'DZ',
        dipReconnaissanceDateDetectee: '2022-05-01',
      } as unknown as never;
      fixture.detectChanges();
      expect(component.natureDecision()).toBe('MARIAGE_RELIGIEUX_NON_CIVIL');
      expect(component.paysOrigine()).toBe('DZ');
      expect(component.dateDecision()).toBe('2022-05-01');
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
      expect(component.verdictBannerLabel('EXEQUATUR_REQUIS')).toContain('Exequatur');
      expect(component.verdictBannerLabel('RECONNAISSANCE_REFUSEE')).toContain('refusée');
      expect(component.verdictBannerLabel('QUALIFICATION_INCOMPLETE')).toContain('incomplète');
    });

    it('verdictBannerClass : ok / warn / ko / info', () => {
      expect(component.verdictBannerClass('RECONNAISSANCE_DE_PLEIN_DROIT')).toContain('--ok');
      expect(component.verdictBannerClass('EXEQUATUR_REQUIS')).toContain('--warn');
      expect(component.verdictBannerClass('RECONNAISSANCE_REFUSEE')).toContain('--ko');
      expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    });

    it('toolIdForJurisprudence = dip-be-reconnaissance-decision-etrangere (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('dip-be-reconnaissance-decision-etrangere');
    });
  });
});
