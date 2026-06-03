import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { RttMonetisationSectionComponent } from './rtt-monetisation-section.component';
import { RttMonetisationResponse } from '../../core/models/rtt-monetisation.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('RttMonetisationSectionComponent', () => {
  let component: RttMonetisationSectionComponent;
  let fixture: ComponentFixture<RttMonetisationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/rtt-monetisation-analysis';

  function eligibleResponse(overrides: Partial<RttMonetisationResponse> = {}): RttMonetisationResponse {
    return {
      caseFileId: 'case-1',
      nombreJoursRttRenonces: 5,
      salaireJournalierBrut: 200,
      tauxApplique: 25,
      joursAcquisDansFenetre: true,
      montantBrut: 1250,
      regimeSocialFiscal: 'ALIGNE_HEURES_SUPPLEMENTAIRES',
      statut: 'ELIGIBLE',
      notes: ['Régime aligné sur les heures supplémentaires (à vérifier par avocat).'],
      country: 'FRANCE',
      baseJuridique: 'loi n° 2022-1157 du 16/08/2022 art. 5 (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonEligibleResponse(): RttMonetisationResponse {
    return eligibleResponse({
      statut: 'NON_ELIGIBLE',
      montantBrut: null,
      joursAcquisDansFenetre: false,
      notes: ['Jours hors de la fenêtre du dispositif (01/01/2022 → 31/12/2026).'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [RttMonetisationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RttMonetisationSectionComponent);
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
    req.flush(eligibleResponse());
    expect(component.result()?.statut).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
  });

  it('affiche le formulaire si aucune analyse (GET 404)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid() exige jours > 0 et salaire > 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onNbJoursChange(5);
    expect(component.formValid()).toBe(false);
    component.onSalaireChange(200);
    expect(component.formValid()).toBe(true);
    component.onSalaireChange(0);
    expect(component.formValid()).toBe(false);
  });

  it('POST nominal → badge ELIGIBLE + montant brut affiché', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNbJoursChange(5);
    component.onSalaireChange(200);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nombreJoursRttRenonces).toBe(5);
    req.flush(eligibleResponse());
    expect(component.result()?.statut).toBe('ELIGIBLE');
    expect(component.result()?.montantBrut).toBe(1250);
    expect(component.statutChipClass('ELIGIBLE')).toContain('is-chip--success');
  });

  it('POST hors fenêtre → badge NON_ELIGIBLE rouge sans montant', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNbJoursChange(5);
    component.onSalaireChange(200);
    component.onDateRenonciationChange('2027-03-01');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.joursAcquisDansFenetre).toBe(false);
    req.flush(nonEligibleResponse());
    expect(component.result()?.statut).toBe('NON_ELIGIBLE');
    expect(component.result()?.montantBrut).toBeNull();
    expect(component.statutChipClass('NON_ELIGIBLE')).toContain('is-chip--danger');
  });

  it('joursAcquisDansFenetre dérive de dateRenonciation (fenêtre 01/01/2022–31/12/2026)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.joursAcquisDansFenetre()).toBe(true); // pas de date → ouvert
    component.onDateRenonciationChange('2023-06-15');
    expect(component.joursAcquisDansFenetre()).toBe(true);
    component.onDateRenonciationChange('2021-12-31');
    expect(component.joursAcquisDansFenetre()).toBe(false);
    component.onDateRenonciationChange('2027-01-01');
    expect(component.joursAcquisDansFenetre()).toBe(false);
  });

  it('pré-remplit jours et salaire depuis Sf218dDetail (clés snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = {
      nombre_jours_rtt_renonces: 8,
      salaire_journalier_brut: 180,
    } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.nombreJoursRttRenonces()).toBe(8);
    expect(component.salaireJournalierBrut()).toBe(180);
    expect(component.provenanceNbJours()).toBe('IA');
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('getPrefillCount = 2 quand jours + salaire fournis (FR)', () => {
    const count = RttMonetisationSectionComponent.getPrefillCount({
      aiData: { nombre_jours_rtt_renonces: 8, salaire_journalier_brut: 180 } as any,
      workspaceCountry: 'FRANCE',
    });
    expect(count).toBe(2);
  });

  it('getPrefillCount = 1 (partiel) et 0 (vide)', () => {
    expect(RttMonetisationSectionComponent.getPrefillCount({
      aiData: { nombre_jours_rtt_renonces: 8 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(RttMonetisationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('coherenceAlerts signale une divergence > 10 % entre IA et saisie', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { salaire_journalier_brut: 200 } as unknown as TravailExtractedData;
    component.onSalaireChange(300); // +50 % > 10 %
    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE_JOURNALIER).toBeTruthy();
    expect(alerts.SALAIRE_JOURNALIER!.field).toBe('SALAIRE_JOURNALIER');
  });

  it('coherenceAlerts vide si écart ≤ 10 %', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { nombre_jours_rtt_renonces: 10 } as unknown as TravailExtractedData;
    component.onNbJoursChange(10);
    expect(component.coherenceAlerts().NB_JOURS).toBeUndefined();
  });

  it('gate FR : en BELGIQUE pas d\'appel réseau et isFrance() false', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('exposes label, icon et thème INDEMNITES dans le panel (metadata statique)', () => {
    expect(RttMonetisationSectionComponent.TOOL_LABEL).toContain('RTT');
    expect(RttMonetisationSectionComponent.TOOL_ICON).toBe('savings');
  });

  it('appelle markForCheck et triggerRefresh après POST (OnPush)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNbJoursChange(5);
    component.onSalaireChange(200);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(eligibleResponse());
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalled();
  });
});
