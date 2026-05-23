/**
 * SF-216-24 — Tests Jest pour `DonationEntreEpouxFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { DonationEntreEpouxFrSectionComponent } from './donation-entre-epoux-fr-section.component';
import { DonationEntreEpouxFrService } from '../../core/services/donation-entre-epoux-fr.service';
import { DonationEntreEpouxResponse } from '../../core/models/donation-entre-epoux-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('DonationEntreEpouxFrSectionComponent (SF-216-24)', () => {
  let component: DonationEntreEpouxFrSectionComponent;
  let fixture: ComponentFixture<DonationEntreEpouxFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<DonationEntreEpouxFrService>;

  const nominalResponse: DonationEntreEpouxResponse = {
    caseFileId: 'case-1',
    validite: 'VALIDE',
    revocabilite: 'REVOCABLE_AD_NUTUM',
    effetDissolution: 'Mariage non dissous — la donation produit ses effets.',
    actionRetranchementPossible: false,
    impactReserve: 'Quotité disponible spéciale entre époux (art. 1094-1).',
    baseLegale: 'art. 1091-1100 Cciv + art. 265 al. 2 + art. 1527 al. 2 + art. 912-928',
    messages: ['Donation au dernier vivant — révocable ad nutum.'],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<DonationEntreEpouxFrService>(
      'DonationEntreEpouxFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [
        DonationEntreEpouxFrSectionComponent,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        { provide: DonationEntreEpouxFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DonationEntreEpouxFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(DonationEntreEpouxFrService) as jasmine.SpyObj<DonationEntreEpouxFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(DonationEntreEpouxFrSectionComponent.TOOL_LABEL).toBe('DONATION ENTRE ÉPOUX');
      expect(DonationEntreEpouxFrSectionComponent.TOOL_ICON).toBe('favorite');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(DonationEntreEpouxFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 5 champs IA = 5', () => {
      const aiData = {
        regimeMatrimonialDetecte: 'COMMUNAUTE_UNIVERSELLE',
        clauseAttributionIntegraleDetected: true,
        enfantsNonCommunsDetected: true,
        revocabiliteDetectee: false,
        bienDonnePrincipalType: 'IMMOBILIER',
      } as unknown as FamilleExtractedData;
      expect(
        DonationEntreEpouxFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(5);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        DonationEntreEpouxFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → 5 champs pré-remplis + type avantage déduit', () => {
      component.aiData = {
        regimeMatrimonialDetecte: 'COMMUNAUTE_UNIVERSELLE',
        clauseAttributionIntegraleDetected: true,
        enfantsNonCommunsDetected: true,
        revocabiliteDetectee: false,
        bienDonnePrincipalType: 'IMMOBILIER',
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.regimeMatrimonial()).toBe('COMMUNAUTE_UNIVERSELLE');
      expect(component.clauseAttributionIntegrale()).toBe(true);
      expect(component.enfantsNonCommuns()).toBe(true);
      expect(component.revocabiliteDetectee()).toBe(false);
      expect(component.bienDonneType()).toBe('IMMOBILIER');
      // Clause attribution intégrale détectée → type déduit
      expect(component.avantageMatrimonialType()).toBe('CLAUSE_ATTRIBUTION_INTEGRALE');
    });

    it('BELGIQUE + aiData rempli → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE',
        clauseAttributionIntegraleDetected: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.regimeMatrimonial()).toBeNull();
      expect(component.clauseAttributionIntegrale()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.avantageMatrimonialType.set('DONATION_BIENS_FUTURS');
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans type avantage', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('formValid true avec type avantage', () => {
      fixture.detectChanges();
      component.avantageMatrimonialType.set('DONATION_BIENS_FUTURS');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('handlers', () => {
    it('onClauseChange(true) → type devient CLAUSE_ATTRIBUTION_INTEGRALE', () => {
      fixture.detectChanges();
      component.onClauseChange(true);
      expect(component.avantageMatrimonialType()).toBe('CLAUSE_ATTRIBUTION_INTEGRALE');
    });

    it('onAvantageChange(CLAUSE_ATTRIBUTION_INTEGRALE) → case clause cochée', () => {
      fixture.detectChanges();
      component.onAvantageChange('CLAUSE_ATTRIBUTION_INTEGRALE');
      expect(component.clauseAttributionIntegrale()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('FRANCE — calcule et applique le résultat', () => {
      fixture.detectChanges();
      component.avantageMatrimonialType.set('DONATION_BIENS_FUTURS');
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalled();
      expect(component.result()?.validite).toBe('VALIDE');
      expect(component.showForm()).toBe(false);
    });

    it('calculate no-op si formInvalide', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('validiteLabel mappe les codes connus', () => {
      fixture.detectChanges();
      expect(component.validiteLabel('VALIDE')).toContain('valide');
      expect(component.validiteLabel('REVOQUEE')).toContain('révoquée');
      expect(component.validiteLabel('A_VERIFIER')).toContain('vérifier');
      expect(component.validiteLabel('INCONNU')).toBe('INCONNU');
    });

    it('revocabiliteLabel mappe les codes connus', () => {
      fixture.detectChanges();
      expect(component.revocabiliteLabel('REVOCABLE_AD_NUTUM')).toContain('1096');
      expect(component.revocabiliteLabel('REVOCATION_DE_PLEIN_DROIT')).toContain('265');
      expect(component.revocabiliteLabel('IRREVOCABLE')).toContain('Irrévocable');
    });

    it('verdictClass REVOQUEE → verdict-blocked', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, validite: 'REVOQUEE' });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass action retranchement → verdict-warning', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        actionRetranchementPossible: true,
      });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass A_VERIFIER → verdict-warning', () => {
      fixture.detectChanges();
      component['result'].set({
        ...nominalResponse,
        validite: 'A_VERIFIER',
      });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass VALIDE → verdict-success', () => {
      fixture.detectChanges();
      component['result'].set(nominalResponse);
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
