/**
 * SF-216-20 — Tests Jest pour `IndigniteSuccessoraleFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { IndigniteSuccessoraleFrSectionComponent } from './indignite-successorale-fr-section.component';
import { IndigniteSuccessoraleFrService } from '../../core/services/indignite-successorale-fr.service';
import { IndigniteSuccessoraleResponse } from '../../core/models/indignite-successorale-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('IndigniteSuccessoraleFrSectionComponent (SF-216-20)', () => {
  let component: IndigniteSuccessoraleFrSectionComponent;
  let fixture: ComponentFixture<IndigniteSuccessoraleFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<IndigniteSuccessoraleFrService>;

  const nominalResponse: IndigniteSuccessoraleResponse = {
    caseFileId: 'case-1',
    typeIndignite: 'PLEIN_DROIT',
    verdictIndignite: 'INDIGNITE_PLEIN_DROIT',
    pardonNeutralisant: false,
    representationPossible: false,
    delaiAction: 'Délai 5 ans (art. 729 Cciv) — années écoulées : 1.',
    delaiForclos: false,
    effetDevolution: 'L\'indigne est écarté de la succession ipso jure.',
    baseLegale: 'art. 726-729-1 Cciv',
    messages: ['Indignité de plein droit caractérisée.'],
    alertes: ['Représentation des descendants exclue (art. 729-1).'],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<IndigniteSuccessoraleFrService>(
      'IndigniteSuccessoraleFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [
        IndigniteSuccessoraleFrSectionComponent,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        { provide: IndigniteSuccessoraleFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IndigniteSuccessoraleFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(IndigniteSuccessoraleFrService) as jasmine.SpyObj<IndigniteSuccessoraleFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(IndigniteSuccessoraleFrSectionComponent.TOOL_LABEL).toBe('INDIGNITE SUCCESSORALE');
      expect(IndigniteSuccessoraleFrSectionComponent.TOOL_ICON).toBe('gavel');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(IndigniteSuccessoraleFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 3 champs IA = 3', () => {
      const aiData = {
        condamnationPenaleSuccessionDetectee: true,
        pardonTestamentaireDetecte: false,
        dateOuvertureSuccessionDetectee: '2024-08-01',
      } as unknown as FamilleExtractedData;
      expect(
        IndigniteSuccessoraleFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(3);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        IndigniteSuccessoraleFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → condamnation + pardon + date pré-remplis', () => {
      component.aiData = {
        condamnationPenaleSuccessionDetectee: true,
        pardonTestamentaireDetecte: false,
        dateOuvertureSuccessionDetectee: '2024-12-01',
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.condamnationPrononcee()).toBe(true);
      expect(component.pardonTestamentaireDetecte()).toBe(false);
      expect(component.dateOuvertureSuccession()).toBe('2024-12-01');
    });

    it('BELGIQUE + aiData rempli → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        condamnationPenaleSuccessionDetectee: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.condamnationPrononcee()).toBeNull();
      expect(component.dateOuvertureSuccession()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.motifIndignite.set('MEURTRE');
      component.condamnationPrononcee.set(true);
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans motif', () => {
      fixture.detectChanges();
      component.condamnationPrononcee.set(true);
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans dateOuverture', () => {
      fixture.detectChanges();
      component.motifIndignite.set('MEURTRE');
      component.condamnationPrononcee.set(true);
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans condamnation définie', () => {
      fixture.detectChanges();
      component.motifIndignite.set('MEURTRE');
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(false);
    });

    it('formValid true avec motif + dateOuverture + condamnation définie', () => {
      fixture.detectChanges();
      component.motifIndignite.set('MEURTRE');
      component.condamnationPrononcee.set(true);
      component.dateOuvertureSuccession.set('2025-01-01');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('FRANCE — calcule et applique le résultat', () => {
      fixture.detectChanges();
      component.motifIndignite.set('MEURTRE');
      component.condamnationPrononcee.set(true);
      component.dateOuvertureSuccession.set('2025-01-01');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalled();
      expect(component.result()?.typeIndignite).toBe('PLEIN_DROIT');
      expect(component.showForm()).toBe(false);
    });

    it('calculate no-op si formInvalide', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('typeIndigniteLabel mappe les codes connus', () => {
      fixture.detectChanges();
      expect(component.typeIndigniteLabel('PLEIN_DROIT')).toContain('plein droit');
      expect(component.typeIndigniteLabel('JUDICIAIRE')).toContain('judiciaire');
      expect(component.typeIndigniteLabel('PARDONNEE')).toContain('Pardon');
      expect(component.typeIndigniteLabel('AUCUNE')).toContain('Aucune');
      expect(component.typeIndigniteLabel('INCONNU')).toBe('INCONNU');
    });

    it('verdictClass PLEIN_DROIT → verdict-blocked', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, typeIndignite: 'PLEIN_DROIT' });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass JUDICIAIRE non forclos → verdict-warning', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        typeIndignite: 'JUDICIAIRE',
        verdictIndignite: 'INDIGNITE_JUDICIAIRE_POSSIBLE',
        delaiForclos: false,
      });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass forclos → verdict-blocked', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        typeIndignite: 'JUDICIAIRE',
        verdictIndignite: 'DELAI_FORCLOS',
        delaiForclos: true,
      });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass PARDONNEE → verdict-success', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        typeIndignite: 'PARDONNEE',
        verdictIndignite: 'PARDON_NEUTRALISANT',
        pardonNeutralisant: true,
      });
      expect(component.verdictClass()).toBe('verdict-success');
    });

    it('verdictClass sans result → chaîne vide', () => {
      fixture.detectChanges();
      expect(component.verdictClass()).toBe('');
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
