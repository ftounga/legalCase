/**
 * SF-216-30 — Tests Jest pour `DonationPartageFrSectionComponent`.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { DonationPartageFrSectionComponent } from './donation-partage-fr-section.component';
import { DonationPartageFrService } from '../../core/services/donation-partage-fr.service';
import { DonationPartageResponse } from '../../core/models/donation-partage-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('DonationPartageFrSectionComponent (SF-216-30)', () => {
  let component: DonationPartageFrSectionComponent;
  let fixture: ComponentFixture<DonationPartageFrSectionComponent>;
  let serviceSpy: jasmine.SpyObj<DonationPartageFrService>;

  const nominalResponse: DonationPartageResponse = {
    caseFileId: 'case-1',
    conditionsRemplies: true,
    interet: 'FORT',
    gelValeurEffet: 'Gel de la valeur au jour de la donation (art. 1078 Cciv).',
    rapportExclu: true,
    alerteQuotite: false,
    etapesNotariales: ['Consultation notariale préalable.'],
    baseLegale: 'art. 1075 à 1075-5 Cciv + art. 1078 + art. 1078-1 + art. 1080 + art. 912-928',
    messages: ['Donation-partage envisageable.'],
    alertes: [],
    country: 'FRANCE',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<DonationPartageFrService>(
      'DonationPartageFrService',
      ['calculate', 'get'],
    );
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(nominalResponse));

    await TestBed.configureTestingModule({
      imports: [
        DonationPartageFrSectionComponent,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        { provide: DonationPartageFrService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DonationPartageFrSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(DonationPartageFrService) as jasmine.SpyObj<DonationPartageFrService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(DonationPartageFrSectionComponent.TOOL_LABEL).toBe('DONATION-PARTAGE');
      expect(DonationPartageFrSectionComponent.TOOL_ICON).toBe('group');
    });

    it('getPrefillCount({}) = 0', () => {
      expect(DonationPartageFrSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount FRANCE + 4 champs IA = 4', () => {
      const aiData = {
        nbDescendantsDetecte: 3,
        respectQuotiteDisponibleDetected: true,
        presencePetitsEnfantsSubstitutionDetectee: true,
        donationPartageConjonctiveDetectee: true,
      } as unknown as FamilleExtractedData;
      expect(
        DonationPartageFrSectionComponent.getPrefillCount({
          aiData,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });

    it('getPrefillCount BELGIQUE = 0', () => {
      expect(
        DonationPartageFrSectionComponent.getPrefillCount({
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('pré-fill IA', () => {
    it('FRANCE + aiData rempli → 4 champs pré-remplis', () => {
      component.aiData = {
        nbDescendantsDetecte: 3,
        respectQuotiteDisponibleDetected: true,
        presencePetitsEnfantsSubstitutionDetectee: true,
        donationPartageConjonctiveDetectee: false,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.nombreDescendants()).toBe(3);
      expect(component.respectQuotiteDisponible()).toBe(true);
      expect(component.presencePetitsEnfantsParSubstitution()).toBe(true);
      expect(component.donationPartageConjonctive()).toBe(false);
    });

    it('BELGIQUE + aiData rempli → aucun pré-fill', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = {
        nbDescendantsDetecte: 2,
        respectQuotiteDisponibleDetected: true,
      } as unknown as FamilleExtractedData;
      fixture.detectChanges();
      expect(component.nombreDescendants()).toBeNull();
      expect(component.respectQuotiteDisponible()).toBeNull();
    });
  });

  describe('validation formulaire', () => {
    it('country gate BE → formValid false', () => {
      component.workspaceCountry = 'BELGIQUE';
      fixture.detectChanges();
      component.nombreDescendants.set(2);
      expect(component.formValid()).toBe(false);
    });

    it('formValid false sans descendants', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('formValid true avec descendants', () => {
      fixture.detectChanges();
      component.nombreDescendants.set(2);
      expect(component.formValid()).toBe(true);
    });

    it('formValid false avec 0 descendant', () => {
      fixture.detectChanges();
      component.nombreDescendants.set(0);
      expect(component.formValid()).toBe(false);
    });
  });

  describe('handlers', () => {
    it('onNombreDescendantsChange normalise', () => {
      fixture.detectChanges();
      component.onNombreDescendantsChange('3');
      expect(component.nombreDescendants()).toBe(3);
      component.onNombreDescendantsChange('0');
      expect(component.nombreDescendants()).toBeNull();
      component.onNombreDescendantsChange('abc');
      expect(component.nombreDescendants()).toBeNull();
    });

    it('onValeurChange rejette le négatif', () => {
      fixture.detectChanges();
      component.onValeurChange('100');
      expect(component.valeurPartageTotal()).toBe(100);
      component.onValeurChange('-1');
      expect(component.valeurPartageTotal()).toBeNull();
    });

    it('onConjonctiveChange + onPetitsEnfantsChange + onRespectQuotiteChange + onReincorporationChange', () => {
      fixture.detectChanges();
      component.onConjonctiveChange(true);
      expect(component.donationPartageConjonctive()).toBe(true);
      component.onPetitsEnfantsChange(true);
      expect(component.presencePetitsEnfantsParSubstitution()).toBe(true);
      component.onRespectQuotiteChange(true);
      expect(component.respectQuotiteDisponible()).toBe(true);
      component.onReincorporationChange(true);
      expect(component.donationsAnterieuresAReinorporer()).toBe(true);
    });

    it('onAge1Change / onAge2Change', () => {
      fixture.detectChanges();
      component.onAge1Change('60');
      expect(component.ageDonateur1()).toBe(60);
      component.onAge2Change('58');
      expect(component.ageDonateur2()).toBe(58);
    });
  });

  describe('calculate()', () => {
    it('FRANCE — calcule et applique le résultat', () => {
      fixture.detectChanges();
      component.nombreDescendants.set(2);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalled();
      expect(component.result()?.interet).toBe('FORT');
      expect(component.showForm()).toBe(false);
    });

    it('calculate envoie agesDonateurs assemblés', () => {
      fixture.detectChanges();
      component.nombreDescendants.set(2);
      component.ageDonateur1.set(60);
      component.ageDonateur2.set(58);
      component.calculate();
      const callArg = serviceSpy.calculate.calls.mostRecent().args[1];
      expect(callArg.agesDonateurs).toEqual([60, 58]);
    });

    it('calculate envoie agesDonateurs=null si aucun âge', () => {
      fixture.detectChanges();
      component.nombreDescendants.set(2);
      component.calculate();
      const callArg = serviceSpy.calculate.calls.mostRecent().args[1];
      expect(callArg.agesDonateurs).toBeNull();
    });

    it('calculate no-op si formInvalide', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('helpers d\'affichage', () => {
    it('interetLabel mappe les codes connus', () => {
      fixture.detectChanges();
      expect(component.interetLabel('FORT')).toContain('fort');
      expect(component.interetLabel('MOYEN')).toContain('moyen');
      expect(component.interetLabel('FAIBLE')).toContain('faible');
      expect(component.interetLabel('INADAPTE')).toContain('Inadapté');
      expect(component.interetLabel('INCONNU')).toBe('INCONNU');
    });

    it('verdictClass alerte quotité → warning', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, alerteQuotite: true });
      expect(component.verdictClass()).toBe('verdict-warning');
    });

    it('verdictClass conditions non remplies → blocked', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, conditionsRemplies: false });
      expect(component.verdictClass()).toBe('verdict-blocked');
    });

    it('verdictClass intérêt fort → success', () => {
      fixture.detectChanges();
      component['result'].set(nominalResponse);
      expect(component.verdictClass()).toBe('verdict-success');
    });

    it('verdictClass intérêt moyen sans alerte → warning', () => {
      fixture.detectChanges();
      component['result'].set({ ...nominalResponse, interet: 'MOYEN' });
      expect(component.verdictClass()).toBe('verdict-warning');
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
