import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { DublinRecoursSectionComponent } from './dublin-recours-section.component';
import { DublinRecoursResponse } from '../../core/models/dublin-recours.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('DublinRecoursSectionComponent', () => {
  let component: DublinRecoursSectionComponent;
  let fixture: ComponentFixture<DublinRecoursSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/dublin-recours-analysis';

  function frResponse(overrides: Partial<DublinRecoursResponse> = {}): DublinRecoursResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationDecisionTransfert: '2026-05-01',
      etatMembreResponsable: 'ITALIE',
      motifTransfert: 'DEMANDE_ASILE_AUTRE_ETAT',
      recoursForme: false,
      dateRecours: null,
      country: 'FRANCE',
      dateExpirationRecours: '2026-05-08',
      dateLimiteTransfertEffectif: '2026-11-01',
      joursRestants: 5,
      statut: 'DISPONIBLE',
      effetSuspensif: 'AUTOMATIQUE',
      formule: 'Transfert Dublin notifie le 2026-05-01',
      baseJuridique: 'CESEDA L.572-1+ + Reg. UE 604/2013',
      messages: ['Recours suspensif 7 j'],
      ...overrides,
    };
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [DublinRecoursSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DublinRecoursSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('TOOL_LABEL and TOOL_ICON statics', () => {
    expect(DublinRecoursSectionComponent.TOOL_LABEL).toContain('DUBLIN');
    expect(DublinRecoursSectionComponent.TOOL_ICON).toBe('flight_takeoff');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(DublinRecoursSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when dateNotificationDecisionContestee present', () => {
    expect(DublinRecoursSectionComponent.getPrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when BELGIQUE', () => {
    expect(DublinRecoursSectionComponent.getPrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET on ngOnInit', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.result()!.statut).toBe('DISPONIBLE');
    expect(component.dateNotificationDecisionTransfert()).toBe('2026-05-01');
    expect(component.etatMembreResponsable()).toBe('ITALIE');
    expect(component.motifTransfert()).toBe('DEMANDE_ASILE_AUTRE_ETAT');
    expect(component.showForm()).toBe(false);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
  });

  it('formValid false if date or motif missing', () => {
    component.dateNotificationDecisionTransfert.set('2026-05-01');
    component.motifTransfert.set(null);
    expect(component.formValid()).toBe(false);
    component.motifTransfert.set('DEMANDE_ASILE_AUTRE_ETAT');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if date in future', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    component.dateNotificationDecisionTransfert.set(future.toISOString().slice(0, 10));
    component.motifTransfert.set('DEMANDE_ASILE_AUTRE_ETAT');
    expect(component.formValid()).toBe(false);
  });

  it('formValid : recoursForme=true requires dateRecours >= notif', () => {
    component.dateNotificationDecisionTransfert.set('2026-05-01');
    component.motifTransfert.set('DEMANDE_ASILE_AUTRE_ETAT');
    component.recoursForme.set(true);
    component.dateRecours.set(null);
    expect(component.formValid()).toBe(false);
    component.dateRecours.set('2026-04-30');
    expect(component.formValid()).toBe(false);
    component.dateRecours.set('2026-05-03');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationDecisionTransfert.set('2026-05-01');
    component.motifTransfert.set('DEMANDE_ASILE_AUTRE_ETAT');
    component.etatMembreResponsable.set('ITALIE');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationDecisionTransfert: '2026-05-01',
      etatMembreResponsable: 'ITALIE',
      motifTransfert: 'DEMANDE_ASILE_AUTRE_ETAT',
      recoursForme: false,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('DISPONIBLE');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() recoursForme=true includes dateRecours', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationDecisionTransfert.set('2026-05-01');
    component.motifTransfert.set('DEMANDE_ASILE_AUTRE_ETAT');
    component.recoursForme.set(true);
    component.dateRecours.set('2026-05-05');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body.recoursForme).toBe(true);
    expect(req.request.body.dateRecours).toBe('2026-05-05');
    req.flush(frResponse({ recoursForme: true, dateRecours: '2026-05-05', statut: 'RECOURS_FORME' }));
    expect(component.result()!.statut).toBe('RECOURS_FORME');
  });

  it('analyze() error -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationDecisionTransfert.set('2026-05-01');
    component.motifTransfert.set('DEMANDE_ASILE_AUTRE_ETAT');
    component.analyze();
    httpMock.expectOne(r => r.method === 'POST').flush({ message: 'Bad' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String), 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' })
    );
  });

  it('pre-fill IA: dateNotificationDecisionContestee -> date + badge', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationDecisionTransfert()).toBe('2026-05-01');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    component.onDateNotificationChange('2026-05-02');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('coherenceAlerts : divergence date IA -> WARNING', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    component.onDateNotificationChange('2026-05-03');
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toBe('2026-05-01');
  });

  it('bannerClass + statutLabel cover statuses', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('dublin-banner--info');
    expect(component.bannerClass('URGENT')).toContain('dublin-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('dublin-banner--danger');
    expect(component.bannerClass('RECOURS_FORME')).toContain('dublin-banner--success');
    expect(component.statutLabel('URGENT')).toContain('URGENT');
  });

  it('showJoursRestants true for DISPONIBLE/URGENT only', () => {
    expect(component.showJoursRestants(frResponse({ statut: 'DISPONIBLE' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statut: 'EXPIRE' }))).toBe(false);
    expect(component.showJoursRestants(null)).toBe(false);
  });

  it('ngOnChanges re-prefill when aiData arrives later', () => {
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationDecisionTransfert()).toBeNull();
    component.aiData = { dateNotificationDecisionContestee: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateNotificationDecisionTransfert()).toBe('2026-05-01');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateNotificationDecisionTransfert: '2026-05-10' }));
    component.aiData = { dateNotificationDecisionContestee: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateNotificationDecisionTransfert()).toBe('2026-05-10');
  });
});
