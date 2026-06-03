import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { PpvExonerationSectionComponent } from './ppv-exoneration-section.component';
import { PpvExonerationResponse } from '../../core/models/ppv-exoneration.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('PpvExonerationSectionComponent', () => {
  let component: PpvExonerationSectionComponent;
  let fixture: ComponentFixture<PpvExonerationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/ppv-exoneration-analysis';

  function conformeResponse(overrides: Partial<PpvExonerationResponse> = {}): PpvExonerationResponse {
    return {
      caseFileId: 'case-1',
      montantPrime: 2500,
      accordInteressementPresent: false,
      remunerationAnnuelleBrute: 70000,
      effectifMoins50: false,
      versementPlanEpargne: false,
      plafondSocialApplique: 3000,
      montantExonere: 2500,
      montantImposable: 0,
      exonerationFiscaleIr: false,
      statut: 'CONFORME',
      notes: ['Montant de la PPV intégralement exonéré de cotisations sociales.'],
      country: 'FRANCE',
      baseJuridique: 'loi n° 2022-1158 du 16/08/2022, art. 1 (à vérifier par avocat)',
      ...overrides,
    };
  }

  function plafondDepasseResponse(): PpvExonerationResponse {
    return conformeResponse({
      montantPrime: 4000,
      plafondSocialApplique: 3000,
      montantExonere: 3000,
      montantImposable: 1000,
      statut: 'PLAFOND_DEPASSE',
      notes: ['Montant de la PPV (4000 €) supérieur au plafond (3000 €).'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [PpvExonerationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PpvExonerationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('se crée', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('charge l\'analyse existante au ngOnInit (GET 200) et masque le formulaire', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(conformeResponse());
    expect(component.result()?.statut).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
  });

  it('affiche le formulaire si aucune analyse (GET 404)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid() exige montant > 0 et rémunération > 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onMontantChange(2500);
    expect(component.formValid()).toBe(false);
    component.onRemunerationChange(70000);
    expect(component.formValid()).toBe(true);
    component.onMontantChange(0);
    expect(component.formValid()).toBe(false);
  });

  it('plafondPrevisible = 3000 par défaut, 6000 avec accord ou effectif < 50', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.plafondPrevisible()).toBe(3000);
    component.onAccordChange(true);
    expect(component.plafondPrevisible()).toBe(6000);
    component.onAccordChange(false);
    expect(component.plafondPrevisible()).toBe(3000);
    component.onEffectifChange(true);
    expect(component.plafondPrevisible()).toBe(6000);
  });

  it('POST conforme → badge CONFORME vert + montant exonéré, plafond 3000', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onMontantChange(2500);
    component.onRemunerationChange(70000);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.montantPrime).toBe(2500);
    req.flush(conformeResponse());
    expect(component.result()?.statut).toBe('CONFORME');
    expect(component.result()?.montantExonere).toBe(2500);
    expect(component.result()?.montantImposable).toBe(0);
    expect(component.result()?.plafondSocialApplique).toBe(3000);
    expect(component.statutChipClass('CONFORME')).toContain('is-chip--success');
  });

  it('POST dépassement → badge PLAFOND_DEPASSE rouge + montant imposable', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onMontantChange(4000);
    component.onRemunerationChange(70000);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.montantPrime).toBe(4000);
    req.flush(plafondDepasseResponse());
    expect(component.result()?.statut).toBe('PLAFOND_DEPASSE');
    expect(component.result()?.montantExonere).toBe(3000);
    expect(component.result()?.montantImposable).toBe(1000);
    expect(component.statutChipClass('PLAFOND_DEPASSE')).toContain('is-chip--danger');
  });

  it('POST avec accord d\'intéressement → plafond applicable 6000 €', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onMontantChange(4500);
    component.onRemunerationChange(70000);
    component.onAccordChange(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.accordInteressementPresent).toBe(true);
    req.flush(conformeResponse({ montantPrime: 4500, accordInteressementPresent: true, plafondSocialApplique: 6000, montantExonere: 4500 }));
    expect(component.result()?.plafondSocialApplique).toBe(6000);
  });

  it('pré-remplit montant et accord depuis Sf218dDetail (clés snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = {
      montant_ppv: 3500,
      accord_interessement_present: true,
    } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.montantPrime()).toBe(3500);
    expect(component.accordInteressementPresent()).toBe(true);
    expect(component.provenanceMontant()).toBe('IA');
    expect(component.provenanceAccord()).toBe('IA');
  });

  it('getPrefillCount = 2 quand montant + accord fournis (FR)', () => {
    const count = PpvExonerationSectionComponent.getPrefillCount({
      aiData: { montant_ppv: 3500, accord_interessement_present: true } as any,
      workspaceCountry: 'FRANCE',
    });
    expect(count).toBe(2);
  });

  it('getPrefillCount = 1 (partiel) et 0 (vide)', () => {
    expect(PpvExonerationSectionComponent.getPrefillCount({
      aiData: { montant_ppv: 3500 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(PpvExonerationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('coherenceAlerts signale une divergence > 10 % sur le montant', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { montant_ppv: 2000 } as unknown as TravailExtractedData;
    component.onMontantChange(3000); // +50 % > 10 %
    const alerts = component.coherenceAlerts();
    expect(alerts.MONTANT_PRIME).toBeTruthy();
    expect(alerts.MONTANT_PRIME!.field).toBe('MONTANT_PRIME');
  });

  it('coherenceAlerts signale une divergence sur le booléen accord', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { accord_interessement_present: true } as unknown as TravailExtractedData;
    component.onAccordChange(false); // IA dit présent, saisie dit absent
    const alerts = component.coherenceAlerts();
    expect(alerts.ACCORD_INTERESSEMENT).toBeTruthy();
  });

  it('coherenceAlerts vide si écart ≤ 10 % sur le montant', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { montant_ppv: 2500 } as unknown as TravailExtractedData;
    component.onMontantChange(2500);
    expect(component.coherenceAlerts().MONTANT_PRIME).toBeUndefined();
  });

  it('gate FR : en BELGIQUE pas d\'appel réseau et isFrance() false', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('exposes label et icon (metadata statique)', () => {
    expect(PpvExonerationSectionComponent.TOOL_LABEL).toContain('PPV');
    expect(PpvExonerationSectionComponent.TOOL_ICON).toBe('redeem');
  });

  it('appelle markForCheck et triggerRefresh après POST (OnPush)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onMontantChange(2500);
    component.onRemunerationChange(70000);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalled();
  });
});
