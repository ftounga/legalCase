/**
 * SF-223-07 — Tests Jest pour `DipBeLoiApplicableFamilleSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 4 champs) ;
 *  - country gate FR → formValid false ;
 *  - matière requise ;
 *  - calculate() appelle le service avec le bon body (ISO-2 normalisé) ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (matière + résidence + nationalité + choix de loi) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon + fondementLabel mappent les valeurs ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { DipBeLoiApplicableFamilleSectionComponent } from './dip-be-loi-applicable-famille-section.component';
import { DipBeLoiApplicableFamilleService } from '../../core/services/dip-be-loi-applicable-famille.service';
import { DipBeLoiApplicableFamilleResponse } from '../../core/models/dip-be-loi-applicable-famille.model';

describe('DipBeLoiApplicableFamilleSectionComponent (SF-223-07)', () => {
  let component: DipBeLoiApplicableFamilleSectionComponent;
  let fixture: ComponentFixture<DipBeLoiApplicableFamilleSectionComponent>;
  let serviceSpy: jasmine.SpyObj<DipBeLoiApplicableFamilleService>;

  const divorceResponse: DipBeLoiApplicableFamilleResponse = {
    caseFileId: 'case-1',
    matiere: 'DIVORCE',
    residenceHabituelleCommune: 'FR',
    nationaliteCommune: null,
    choixLoiParLesParties: null,
    dateMariageOuDeces: null,
    lieuSituationBiensImmobiliers: null,
    verdict: 'LOI_APPLICABLE_DETERMINEE',
    loiApplicableDeterminee: 'FR',
    fondementRattachement: 'RESIDENCE_HABITUELLE_COMMUNE',
    motifs: ['Résidence habituelle commune (art. 8 Rome III).'],
    conseils: ['Vérifier la compétence (Bruxelles II ter) séparément.'],
    basesJuridiques: ['Règlement (UE) n° 1259/2010 (Rome III) — à vérifier par avocat belge'],
    messages: ['Loi applicable déterminée : loi de « FR ».'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-04T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<DipBeLoiApplicableFamilleService>(
      'DipBeLoiApplicableFamilleService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(divorceResponse));

    await TestBed.configureTestingModule({
      imports: [DipBeLoiApplicableFamilleSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: DipBeLoiApplicableFamilleService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DipBeLoiApplicableFamilleSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(DipBeLoiApplicableFamilleService) as jasmine.SpyObj<DipBeLoiApplicableFamilleService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(DipBeLoiApplicableFamilleSectionComponent.TOOL_LABEL)
        .toBe('LOI APPLICABLE — DROIT DE LA FAMILLE (BELGIQUE)');
      expect(DipBeLoiApplicableFamilleSectionComponent.TOOL_ICON).toBe('public');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(DipBeLoiApplicableFamilleSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte matière + résidence + nationalité + choix (BE)', () => {
      expect(DipBeLoiApplicableFamilleSectionComponent.getPrefillCount({
        aiData: {
          dipFamilleBeMatiereDetectee: 'SUCCESSION',
          dipFamilleBeResidenceCommuneDetectee: 'FR',
          dipFamilleBeNationaliteCommuneDetectee: 'MA',
          dipFamilleBeChoixLoiDetecte: 'PT',
        },
        workspaceCountry: 'BELGIQUE',
      })).toBe(4);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.matiere.set('DIVORCE');
      expect(component.formValid()).toBe(false);
    });

    it('matière absente → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('matière renseignée (BE) → formValid true', () => {
      fixture.detectChanges();
      component.matiere.set('DIVORCE');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique le résultat (ISO-2 normalisé)', () => {
      fixture.detectChanges();
      component.matiere.set('DIVORCE');
      component.residenceHabituelleCommune.set('fr');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        matiere: 'DIVORCE',
        residenceHabituelleCommune: 'FR',
      }));
      expect(component.result()).toEqual(divorceResponse);
      expect(component.showForm()).toBe(false);
    });

    it('champ ISO-2 vide → null', () => {
      fixture.detectChanges();
      component.matiere.set('DIVORCE');
      component.nationaliteCommune.set('   ');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        nationaliteCommune: null,
      }));
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('matière + rattachements pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        dipFamilleBeMatiereDetectee: 'SUCCESSION',
        dipFamilleBeResidenceCommuneDetectee: 'FR',
        dipFamilleBeNationaliteCommuneDetectee: 'MA',
        dipFamilleBeChoixLoiDetecte: 'PT',
      } as unknown as never;
      fixture.detectChanges();
      expect(component.matiere()).toBe('SUCCESSION');
      expect(component.residenceHabituelleCommune()).toBe('FR');
      expect(component.nationaliteCommune()).toBe('MA');
      expect(component.choixLoiParLesParties()).toBe('PT');
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
    it('verdictBannerLabel mappe les 2 verdicts', () => {
      expect(component.verdictBannerLabel('LOI_APPLICABLE_DETERMINEE')).toContain('déterminée');
      expect(component.verdictBannerLabel('LOI_INDETERMINABLE')).toContain('indéterminable');
    });

    it('verdictBannerClass : --ok déterminée, --info indéterminable', () => {
      expect(component.verdictBannerClass('LOI_APPLICABLE_DETERMINEE')).toContain('--ok');
      expect(component.verdictBannerClass('LOI_INDETERMINABLE')).toContain('--info');
    });

    it('fondementLabel mappe les fondements', () => {
      expect(component.fondementLabel('CHOIX_LOI_PARTIES')).toContain('professio juris');
      expect(component.fondementLabel('LOI_DU_FOR')).toContain('for');
      expect(component.fondementLabel('RESIDENCE_HABITUELLE_DEFUNT')).toContain('défunt');
    });

    it('toolIdForJurisprudence = dip-be-loi-applicable-famille (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('dip-be-loi-applicable-famille');
    });
  });
});
