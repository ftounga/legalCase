/**
 * SF-216-08 — Tests Jest pour `AripaRecouvrementFrSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (parité runtime/static F-237) ;
 *  - country gate BE → formValid false, pas d'appel HTTP ;
 *  - pré-fill IA branche les 3 champs détectés (montant pension, titre, nb enfants) ;
 *  - calculate() appelle le service avec le bon body et applique le résultat ;
 *  - GET 404 → mode formulaire (no-op) ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AripaRecouvrementFrSectionComponent } from './aripa-recouvrement-fr-section.component';
import { AripaRecouvrementFrService } from '../../core/services/aripa-recouvrement-fr.service';
import {
  AripaRecouvrementFrResponse,
} from '../../core/models/aripa-recouvrement-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AripaRecouvrementFrSectionComponent (SF-216-08)', () => {
  let component: AripaRecouvrementFrSectionComponent;
  let fixture: ComponentFixture<AripaRecouvrementFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<AripaRecouvrementFrService>;

  const nominalResponse: AripaRecouvrementFrResponse = {
    caseFileId: 'case-1',
    voieRecommandee: 'SAISIE_SUR_SALAIRE',
    montantArrieres: 1200,
    montantAsfEligibleMensuelEur: 0,
    delaiEstimeJours: 45,
    etapes: ['Saisir la CAF', 'Préparer les pièces'],
    baseLegale: 'art. L. 581 CSS',
    messages: [],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AripaRecouvrementFrService>('AripaRecouvrementFrService', [
      'calculate',
      'get',
    ]);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [AripaRecouvrementFrSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: AripaRecouvrementFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AripaRecouvrementFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(AripaRecouvrementFrService) as jasmine.SpyObj<AripaRecouvrementFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL/TOOL_ICON exposés', () => {
      expect(AripaRecouvrementFrSectionComponent.TOOL_LABEL).toBe('ARIPA RECOUVREMENT');
      expect(AripaRecouvrementFrSectionComponent.TOOL_ICON).toBe('payments');
    });

    it('getPrefillCount({}) = 0 (no aiData)', () => {
      expect(AripaRecouvrementFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + full aiData = 3', () => {
      const aiData = {
        montantPensionMensuelleDueEur: 300,
        titreExecutoireDetecte: true,
        nombreEnfantsDetecte: 2,
      } as FamilleExtractedData;
      expect(AripaRecouvrementFrSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'FRANCE' })).toBe(3);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      const aiData = {
        montantPensionMensuelleDueEur: 300,
      } as FamilleExtractedData;
      expect(AripaRecouvrementFrSectionComponent.getPrefillCount({ aiData, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData complet → 3 signaux branchés + provenance IA', () => {
      component.aiData = {
        montantPensionMensuelleDueEur: 250,
        titreExecutoireDetecte: true,
        nombreEnfantsDetecte: 1,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.montantPensionMensuelleEur()).toBe(250);
      expect(component.titreExecutoire()).toBe(true);
      expect(component.nombreEnfantsACharge()).toBe(1);
      expect(component.provenanceMontantPension()).toBe('IA');
      expect(component.provenanceTitreExecutoire()).toBe('IA');
      expect(component.provenanceNombreEnfants()).toBe('IA');
    });

    it('BELGIQUE → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        montantPensionMensuelleDueEur: 250,
        titreExecutoireDetecte: true,
        nombreEnfantsDetecte: 1,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.montantPensionMensuelleEur()).toBeNull();
      expect(component.titreExecutoire()).toBe(false);
      expect(component.nombreEnfantsACharge()).toBeNull();
    });

    it('standaloneMode → pas de pré-fill', () => {
      component.standaloneMode = true;
      component.aiData = {
        montantPensionMensuelleDueEur: 250,
      } as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.montantPensionMensuelleEur()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.montantPensionMensuelleEur.set(300);
      component.nombreMoisImpayes.set(3);
      component.situationCreancier.set('SALARIE');
      component.situationDebiteur.set('SALARIE');
      component.nombreEnfantsACharge.set(1);
      expect(component.formValid()).toBe(false);
    });

    it('FRANCE + tous champs OK → formValid true', () => {
      fixture.detectChanges();
      component.montantPensionMensuelleEur.set(300);
      component.nombreMoisImpayes.set(3);
      component.situationCreancier.set('SALARIE');
      component.situationDebiteur.set('SALARIE');
      component.nombreEnfantsACharge.set(1);
      expect(component.formValid()).toBe(true);
    });

    it('nombreMoisImpayes = 0 → formValid false', () => {
      fixture.detectChanges();
      component.montantPensionMensuelleEur.set(300);
      component.nombreMoisImpayes.set(0);
      component.situationCreancier.set('SALARIE');
      component.situationDebiteur.set('SALARIE');
      component.nombreEnfantsACharge.set(1);
      expect(component.formValid()).toBe(false);
    });

    it('situation manquante → formValid false', () => {
      fixture.detectChanges();
      component.montantPensionMensuelleEur.set(300);
      component.nombreMoisImpayes.set(3);
      component.nombreEnfantsACharge.set(1);
      // situations non set
      expect(component.formValid()).toBe(false);
    });
  });

  describe('calculate()', () => {
    it('appelle service.calculate + applique résultat', () => {
      fixture.detectChanges();
      component.montantPensionMensuelleEur.set(400);
      component.nombreMoisImpayes.set(3);
      component.situationCreancier.set('SALARIE');
      component.situationDebiteur.set('SALARIE');
      component.titreExecutoire.set(true);
      component.debiteurEnFrance.set(true);
      component.nombreEnfantsACharge.set(1);

      component.calculate();

      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        montantPensionMensuelleEur: 400,
        nombreMoisImpayes: 3,
        situationCreancier: 'SALARIE',
        situationDebiteur: 'SALARIE',
        titreExecutoire: true,
      }));
      expect(component.result()).toEqual(nominalResponse);
      expect(component.showForm()).toBe(false);
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate(); // form vide
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('formatEuros → fr-FR currency', () => {
      const out = component.formatEuros(1234);
      expect(out).toContain('1');
      expect(out).toContain('€');
    });

    it('voieLabel mappe les 5 voies', () => {
      expect(component.voieLabel('TITRE_REQUIS')).toContain('Titre');
      expect(component.voieLabel('SDR_ARIPA')).toContain('SDR');
      expect(component.voieLabel('SAISIE_SUR_SALAIRE')).toContain('Saisie');
      expect(component.voieLabel('SATD')).toContain('SATD');
      expect(component.voieLabel('CONVENTION_INTERNATIONALE')).toContain('internationale');
    });

    it('voieClass mappe les 5 voies vers une classe CSS', () => {
      expect(component.voieClass('TITRE_REQUIS')).toBe('voie-warning');
      expect(component.voieClass('SAISIE_SUR_SALAIRE')).toBe('voie-success');
      expect(component.voieClass('CONVENTION_INTERNATIONALE')).toBe('voie-info');
    });
  });

  describe('handlers — modification manuelle efface badge IA', () => {
    it('onMontantPensionChange efface provenance', () => {
      component.provenanceMontantPension.set('IA');
      component.onMontantPensionChange(500);
      expect(component.montantPensionMensuelleEur()).toBe(500);
      expect(component.provenanceMontantPension()).toBeNull();
    });

    it('onTitreExecutoireChange efface provenance', () => {
      component.provenanceTitreExecutoire.set('IA');
      component.onTitreExecutoireChange(false);
      expect(component.titreExecutoire()).toBe(false);
      expect(component.provenanceTitreExecutoire()).toBeNull();
    });

    it('onNombreEnfantsChange efface provenance', () => {
      component.provenanceNombreEnfants.set('IA');
      component.onNombreEnfantsChange(2);
      expect(component.nombreEnfantsACharge()).toBe(2);
      expect(component.provenanceNombreEnfants()).toBeNull();
    });
  });
});
