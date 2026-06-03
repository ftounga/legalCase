import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { DroitDeconnexionConformiteSectionComponent } from './droit-deconnexion-conformite-section.component';
import { DroitDeconnexionConformiteResponse } from '../../core/models/droit-deconnexion-conformite.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DroitDeconnexionConformiteSectionComponent', () => {
  let component: DroitDeconnexionConformiteSectionComponent;
  let fixture: ComponentFixture<DroitDeconnexionConformiteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/droit-deconnexion-conformite-analysis';

  function conformeResponse(overrides: Partial<DroitDeconnexionConformiteResponse> = {}): DroitDeconnexionConformiteResponse {
    return {
      caseFileId: 'case-1',
      effectif: 120,
      delegueSyndicalPresent: true,
      accordOuChartePresent: true,
      plagesDeconnexionDefinies: true,
      actionsSensibilisation: true,
      avisCseRecueilliPourCharte: true,
      obligationDeNegocier: true,
      checklist: [
        { item: 'Accord négocié ou charte employeur', conforme: true, type: 'OBLIGATION', commentaire: 'Art. L.2242-17 7° CT.' },
        { item: 'Plages définies', conforme: true, type: 'PROCEDURE', commentaire: 'Modalités définies.' },
      ],
      itemsManquants: 0,
      statut: 'CONFORME',
      notes: ['Obligation satisfaite.'],
      country: 'FRANCE',
      baseJuridique: 'art. L.2242-17 7° CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonConformeResponse(): DroitDeconnexionConformiteResponse {
    return conformeResponse({
      accordOuChartePresent: false,
      checklist: [
        { item: 'Accord négocié ou charte employeur', conforme: false, type: 'OBLIGATION', commentaire: 'Obligation non remplie.' },
        { item: 'Plages définies', conforme: true, type: 'PROCEDURE', commentaire: 'Modalités définies.' },
      ],
      itemsManquants: 1,
      statut: 'NON_CONFORME',
      notes: ['Obligation NON satisfaite.'],
    });
  }

  function nonRequisResponse(): DroitDeconnexionConformiteResponse {
    return conformeResponse({
      effectif: 30,
      obligationDeNegocier: false,
      checklist: [
        { item: 'Obligation de négocier non applicable', conforme: true, type: 'INFORMATION', commentaire: 'Effectif < 50.' },
      ],
      itemsManquants: 0,
      statut: 'NON_REQUIS',
      notes: ['Obligation non déclenchée.'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [DroitDeconnexionConformiteSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DroitDeconnexionConformiteSectionComponent);
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
    component.onEffectifChange(120);
    expect(component.formValid()).toBe(true);
    component.onEffectifChange(0);
    expect(component.formValid()).toBe(false);
  });

  it('obligationPrevisible bascule au seuil effectif 50 + DS', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    component.onEffectifChange(30);
    component.onDelegueSyndicalChange(true);
    expect(component.obligationPrevisible()).toBe(false);
    component.onEffectifChange(50);
    expect(component.obligationPrevisible()).toBe(true);
    component.onDelegueSyndicalChange(false);
    expect(component.obligationPrevisible()).toBe(false);
  });

  it('POST conforme → badge CONFORME vert + items manquants 0', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(120);
    component.onDelegueSyndicalChange(true);
    component.onAccordOuCharteChange(true);
    component.onPlagesChange(true);
    component.onSensibilisationChange(true);
    component.onAvisCseChange(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.effectif).toBe(120);
    expect(req.request.body.accordOuChartePresent).toBe(true);
    req.flush(conformeResponse());
    expect(component.result()?.statut).toBe('CONFORME');
    expect(component.result()?.itemsManquants).toBe(0);
    expect(component.statutChipClass('CONFORME')).toContain('is-chip--success');
  });

  it('POST sans accord/charte à effectif ≥ 50 + DS → NON_CONFORME rouge + compteur ≥ 1', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(120);
    component.onDelegueSyndicalChange(true);
    component.onAccordOuCharteChange(false);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.accordOuChartePresent).toBe(false);
    req.flush(nonConformeResponse());
    expect(component.result()?.statut).toBe('NON_CONFORME');
    expect(component.result()?.itemsManquants).toBeGreaterThanOrEqual(1);
    expect(component.statutChipClass('NON_CONFORME')).toContain('is-chip--danger');
  });

  it('expose la checklist avec items OBLIGATION non conformes', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonConformeResponse());
    const checklist = component.result()?.checklist ?? [];
    expect(checklist.length).toBeGreaterThan(0);
    expect(checklist.some((c) => c.type === 'OBLIGATION' && !c.conforme)).toBe(true);
  });

  it('POST effectif < 50 → NON_REQUIS gris', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(30);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(nonRequisResponse());
    expect(component.result()?.statut).toBe('NON_REQUIS');
    expect(component.result()?.obligationDeNegocier).toBe(false);
    expect(component.statutChipClass('NON_REQUIS')).toContain('is-chip--neutral');
  });

  it('pré-remplit accord/charte depuis Sf218dDetail (clé snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = {
      accord_deconnexion_present: true,
    } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.accordOuChartePresent()).toBe(true);
    expect(component.provenanceAccordOuCharte()).toBe('IA');
  });

  it('getPrefillCount = 1 (nominal) et 0 (vide)', () => {
    expect(DroitDeconnexionConformiteSectionComponent.getPrefillCount({
      aiData: { accord_deconnexion_present: true } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(DroitDeconnexionConformiteSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('coherenceAlerts signale une divergence sur le booléen accord/charte', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { accord_deconnexion_present: true } as unknown as TravailExtractedData;
    component.onAccordOuCharteChange(false); // IA dit présent, saisie dit absent
    const alerts = component.coherenceAlerts();
    expect(alerts.ACCORD_OU_CHARTE).toBeTruthy();
    expect(alerts.ACCORD_OU_CHARTE!.field).toBe('ACCORD_OU_CHARTE');
  });

  it('coherenceAlerts vide si IA et saisie concordent', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { accord_deconnexion_present: true } as unknown as TravailExtractedData;
    component.onAccordOuCharteChange(true);
    expect(component.coherenceAlerts().ACCORD_OU_CHARTE).toBeUndefined();
  });

  it('gate FR : en BELGIQUE pas d\'appel réseau et isFrance() false', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('expose label et icon (metadata statique)', () => {
    expect(DroitDeconnexionConformiteSectionComponent.TOOL_LABEL).toContain('DÉCONNEXION');
    expect(DroitDeconnexionConformiteSectionComponent.TOOL_ICON).toBe('phonelink_erase');
  });

  it('appelle markForCheck et triggerRefresh après POST (OnPush)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEffectifChange(120);
    component.onDelegueSyndicalChange(true);
    component.onAccordOuCharteChange(true);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(conformeResponse());
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalled();
  });
});
