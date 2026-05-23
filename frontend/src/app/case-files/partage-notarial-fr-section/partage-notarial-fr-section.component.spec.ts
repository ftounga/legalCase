/**
 * SF-216-28 — Tests Jest pour `PartageNotarialFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { PartageNotarialFrSectionComponent } from './partage-notarial-fr-section.component';
import { PartageNotarialFrService } from '../../core/services/partage-notarial-fr.service';
import { PartageNotarialResponse } from '../../core/models/partage-notarial-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('PartageNotarialFrSectionComponent (SF-216-28)', () => {
  let component: PartageNotarialFrSectionComponent;
  let fixture: ComponentFixture<PartageNotarialFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<PartageNotarialFrService>;

  const nominalResponse: PartageNotarialResponse = {
    caseFileId: 'case-1',
    notaireObligatoire: true,
    calendrierEtapes: [
      '1. Désignation du notaire — …',
      '2. Bilan patrimonial — …',
      '3. Attestation immobilière après décès — …',
      '4. Projet de partage — …',
      '5. Signature de l\'acte de partage — …',
    ],
    delaiDeclarationFiscale: '2025-06-15',
    alerteDelai: false,
    orientationJudiciaire: false,
    baseLegale: 'art. 816 Cciv + art. 1592 CGI + art. 641 CGI + art. 840 Cciv',
    messages: [],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<PartageNotarialFrService>(
      'PartageNotarialFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [
        PartageNotarialFrSectionComponent,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        { provide: PartageNotarialFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PartageNotarialFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(PartageNotarialFrService) as jasmine.SpyObj<PartageNotarialFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(PartageNotarialFrSectionComponent.TOOL_LABEL).toBe('PARTAGE SUCCESSORAL NOTARIÉ');
      expect(PartageNotarialFrSectionComponent.TOOL_ICON).toBe('gavel');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(PartageNotarialFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 4 champs IA = 4', () => {
      const aiData = {
        dateOuvertureSuccessionDetectee: '2024-12-10',
        nombreCoheritiersDetecte: 3,
        montantSuccessionEurDetecte: 250000,
        presenceImmeubleSuccessionDetecte: true,
      } as unknown as FamilleExtractedData;
      expect(
        PartageNotarialFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        PartageNotarialFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → 4 champs pré-remplis', () => {
      component.aiData = {
        dateOuvertureSuccessionDetectee: '2024-12-10',
        nombreCoheritiersDetecte: 3,
        montantSuccessionEurDetecte: 250000,
        presenceImmeubleSuccessionDetecte: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dateOuvertureSuccession()).toBe('2024-12-10');
      expect(component.nombreCoheritiers()).toBe(3);
      expect(component.valeurMasseSuccessoraleEur()).toBe(250000);
      expect(component.presenceImmeuble()).toBe(true);
    });

    it('BELGIQUE + aiData rempli → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        dateOuvertureSuccessionDetectee: '2024-12-10',
        nombreCoheritiersDetecte: 3,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.dateOuvertureSuccession()).toBeNull();
      expect(component.nombreCoheritiers()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.dateOuvertureSuccession.set('2024-12-10');
      component.nombreCoheritiers.set(2);
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans date ouverture', () => {
      fixture.detectChanges();
      component.nombreCoheritiers.set(2);
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans cohéritiers', () => {
      fixture.detectChanges();
      component.dateOuvertureSuccession.set('2024-12-10');
      expect(component.formValid()).toBe(false);
    });

    it('formValid true avec date + cohéritiers', () => {
      fixture.detectChanges();
      component.dateOuvertureSuccession.set('2024-12-10');
      component.nombreCoheritiers.set(2);
      expect(component.formValid()).toBe(true);
    });

    it('formValid false avec cohéritiers = 0', () => {
      fixture.detectChanges();
      component.dateOuvertureSuccession.set('2024-12-10');
      component.nombreCoheritiers.set(0);
      expect(component.formValid()).toBe(false);
    });
  });

  describe('handlers', () => {
    it('onNombreChange rejette les valeurs <= 0', () => {
      fixture.detectChanges();
      component.onNombreChange(0);
      expect(component.nombreCoheritiers()).toBeNull();
      component.onNombreChange(-2);
      expect(component.nombreCoheritiers()).toBeNull();
    });

    it('onValeurChange rejette les valeurs négatives', () => {
      fixture.detectChanges();
      component.onValeurChange(-100);
      expect(component.valeurMasseSuccessoraleEur()).toBeNull();
    });

    it('onDateOuvertureChange normalise chaîne vide en null', () => {
      fixture.detectChanges();
      component.onDateOuvertureChange('   ');
      expect(component.dateOuvertureSuccession()).toBeNull();
    });
  });

  describe('calculate()', () => {
    it('FRANCE — calcule et applique le résultat', () => {
      fixture.detectChanges();
      component.dateOuvertureSuccession.set('2024-12-10');
      component.nombreCoheritiers.set(3);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalled();
      expect(component.result()?.notaireObligatoire).toBe(true);
      expect(component.showForm()).toBe(false);
    });

    it('calculate no-op si formInvalide', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('verdictClass orientation judiciaire → verdict-blocked', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, orientationJudiciaire: true });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass alerteDelai → verdict-warning', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, alerteDelai: true });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass nominal → verdict-success', () => {
      fixture.detectChanges();
      component['result'].set(nominalResponse);
      expect(component.verdictClass()).toBe('verdict-success');
    });

    it('verdictClass sans result → chaîne vide', () => {
      fixture.detectChanges();
      expect(component.verdictClass()).toBe('');
    });

    it('verdictLabel mentionne le partage judiciaire si orienté', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, orientationJudiciaire: true });
      expect(component.verdictLabel()).toContain('judiciaire');
    });

    it('verdictLabel mentionne 1592 CGI si notaire obligatoire', () => {
      fixture.detectChanges();
      component['result'].set(nominalResponse);
      expect(component.verdictLabel()).toContain('1592 CGI');
    });

    it('verdictLabel sans notaire obligatoire → recommandé', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, notaireObligatoire: false });
      expect(component.verdictLabel()).toContain('recommandé');
    });
  });

  describe('signaux par défaut', () => {
    it('collapsed=true initialement', () => {
      expect(component.collapsed()).toBe(true);
    });

    it('forceExpanded → ouvre la section', () => {
      component.forceExpanded = true;
      fixture.detectChanges();
      expect(component.collapsed()).toBe(false);
    });
  });
});
