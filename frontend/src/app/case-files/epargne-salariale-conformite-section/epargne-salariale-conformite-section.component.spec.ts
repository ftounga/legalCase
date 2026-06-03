import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { EpargneSalarialeConformiteSectionComponent } from './epargne-salariale-conformite-section.component';
import { EpargneSalarialeConformiteResponse } from '../../core/models/epargne-salariale-conformite.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('EpargneSalarialeConformiteSectionComponent', () => {
  let component: EpargneSalarialeConformiteSectionComponent;
  let fixture: ComponentFixture<EpargneSalarialeConformiteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/epargne-salariale-conformite-analysis';

  function conformeResponse(overrides: Partial<EpargneSalarialeConformiteResponse> = {}): EpargneSalarialeConformiteResponse {
    return {
      caseFileId: 'case-1',
      effectif: 80,
      accordParticipationPresent: true,
      accordInteressementPresent: false,
      beneficeNetFiscalPositif3Ans: false,
      entreprise11a49: false,
      checklist: [
        { item: 'Accord de participation obligatoire', conforme: true, type: 'OBLIGATION', commentaire: 'Art. L.3322-2 CT.' },
        { item: 'Accord d\'intéressement (facultatif)', conforme: true, type: 'INFORMATION', commentaire: 'Art. L.3311-1 CT.' },
      ],
      obligationsApplicables: 1,
      obligationsNonRemplies: 0,
      statut: 'CONFORME',
      notes: ['Participation obligatoire satisfaite.'],
      country: 'FRANCE',
      baseJuridique: 'art. L.3322-2 CT ; loi n° 2023-1107 du 29/11/2023 (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonRemplieResponse(): EpargneSalarialeConformiteResponse {
    return conformeResponse({
      accordParticipationPresent: false,
      checklist: [
        { item: 'Accord de participation obligatoire', conforme: false, type: 'OBLIGATION', commentaire: 'Obligation non remplie.' },
        { item: 'Accord d\'intéressement (facultatif)', conforme: true, type: 'INFORMATION', commentaire: 'Art. L.3311-1 CT.' },
      ],
      obligationsApplicables: 1,
      obligationsNonRemplies: 1,
      statut: 'OBLIGATION_NON_REMPLIE',
      notes: ['Participation obligatoire NON satisfaite.'],
    });
  }

  function nonRequisResponse(): EpargneSalarialeConformiteResponse {
    return conformeResponse({
      effectif: 8,
      accordParticipationPresent: false,
      checklist: [
        { item: 'Aucune obligation applicable', conforme: true, type: 'INFORMATION', commentaire: 'Effectif < 11.' },
      ],
      obligationsApplicables: 0,
      obligationsNonRemplies: 0,
      statut: 'NON_REQUIS',
      notes: ['Aucune obligation légale applicable.'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [EpargneSalarialeConformiteSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EpargneSalarialeConformiteSectionComponent);
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

  it('formValid() exige un effectif > 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onEffectifChange(80);
    expect(component.formValid()).toBe(true);
    component.onEffectifChange(0);
    expect(component.formValid()).toBe(false);
  });

  it('participationObligatoirePrevisible bascule au seuil effectif 50', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    component.onEffectifChange(30);
    expect(component.participationObligatoirePrevisible()).toBe(false);
    component.onEffectifChange(50);
    expect(component.participationObligatoirePrevisible()).toBe(true);
    component.onEffectifChange(80);
    expect(component.participationObligatoirePrevisible()).toBe(true);
  });

  it('POST conforme → badge CONFORME vert + obligations non remplies 0', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(80);
    component.onParticipationChange(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.effectif).toBe(80);
    expect(req.request.body.accordParticipationPresent).toBe(true);
    req.flush(conformeResponse());
    expect(component.result()?.statut).toBe('CONFORME');
    expect(component.result()?.obligationsNonRemplies).toBe(0);
    expect(component.statutChipClass('CONFORME')).toContain('is-chip--success');
  });

  it('POST sans participation à effectif ≥ 50 → OBLIGATION_NON_REMPLIE rouge + compteur ≥ 1', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(80);
    component.onParticipationChange(false);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.accordParticipationPresent).toBe(false);
    req.flush(nonRemplieResponse());
    expect(component.result()?.statut).toBe('OBLIGATION_NON_REMPLIE');
    expect(component.result()?.obligationsNonRemplies).toBeGreaterThanOrEqual(1);
    expect(component.statutChipClass('OBLIGATION_NON_REMPLIE')).toContain('is-chip--danger');
  });

  it('expose la checklist avec items OBLIGATION non conformes', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonRemplieResponse());
    const checklist = component.result()?.checklist ?? [];
    expect(checklist.length).toBeGreaterThan(0);
    expect(checklist.some((c) => c.type === 'OBLIGATION' && !c.conforme)).toBe(true);
  });

  it('POST < 11 salariés → NON_REQUIS gris', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(8);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(nonRequisResponse());
    expect(component.result()?.statut).toBe('NON_REQUIS');
    expect(component.result()?.obligationsApplicables).toBe(0);
    expect(component.statutChipClass('NON_REQUIS')).toContain('is-chip--neutral');
  });

  it('pré-remplit les accords depuis Sf218dDetail (clés snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = {
      accord_participation_present: true,
      accord_interessement_present: true,
    } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.accordParticipationPresent()).toBe(true);
    expect(component.accordInteressementPresent()).toBe(true);
    expect(component.provenanceParticipation()).toBe('IA');
    expect(component.provenanceInteressement()).toBe('IA');
  });

  it('getPrefillCount = 2 (nominal), 1 (partiel) et 0 (vide)', () => {
    expect(EpargneSalarialeConformiteSectionComponent.getPrefillCount({
      aiData: { accord_participation_present: true, accord_interessement_present: false } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(2);
    expect(EpargneSalarialeConformiteSectionComponent.getPrefillCount({
      aiData: { accord_participation_present: true } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(EpargneSalarialeConformiteSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('coherenceAlerts signale une divergence sur le booléen participation', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { accord_participation_present: true } as unknown as TravailExtractedData;
    component.onParticipationChange(false); // IA dit présent, saisie dit absent
    const alerts = component.coherenceAlerts();
    expect(alerts.ACCORD_PARTICIPATION).toBeTruthy();
    expect(alerts.ACCORD_PARTICIPATION!.field).toBe('ACCORD_PARTICIPATION');
  });

  it('coherenceAlerts signale une divergence sur le booléen intéressement', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { accord_interessement_present: true } as unknown as TravailExtractedData;
    component.onInteressementChange(false);
    expect(component.coherenceAlerts().ACCORD_INTERESSEMENT).toBeTruthy();
  });

  it('coherenceAlerts vide si IA et saisie concordent', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { accord_participation_present: true } as unknown as TravailExtractedData;
    component.onParticipationChange(true);
    expect(component.coherenceAlerts().ACCORD_PARTICIPATION).toBeUndefined();
  });

  it('gate FR : en BELGIQUE pas d\'appel réseau et isFrance() false', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('expose label et icon (metadata statique)', () => {
    expect(EpargneSalarialeConformiteSectionComponent.TOOL_LABEL).toContain('ÉPARGNE');
    expect(EpargneSalarialeConformiteSectionComponent.TOOL_ICON).toBe('savings');
  });

  it('appelle markForCheck et triggerRefresh après POST (OnPush)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(80);
    component.onParticipationChange(true);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalled();
  });
});
