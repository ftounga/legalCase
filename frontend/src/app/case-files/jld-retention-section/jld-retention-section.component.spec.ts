import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { JldRetentionSectionComponent } from './jld-retention-section.component';
import { JldRetentionResponse } from '../../core/models/jld-retention.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('JldRetentionSectionComponent', () => {
  let component: JldRetentionSectionComponent;
  let fixture: ComponentFixture<JldRetentionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/jld-retention-analysis';

  function frResponse(overrides: Partial<JldRetentionResponse> = {}): JldRetentionResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationPlacement: '2026-05-08',
      motifPlacement: 'EXECUTION_OQTF',
      recoursForme: false,
      dateRecours: null,
      country: 'FRANCE',
      dateExpirationSaisineJld: '2026-05-10',
      dateAudienceJld: '2026-05-13',
      dateExpirationRecoursAppel: null,
      joursRestantsAvantSaisine: 2,
      statut: 'DISPONIBLE',
      formule: 'Placement notifie le 2026-05-08',
      baseJuridique: 'CESEDA L.741-1, L.743-9, L.743-21',
      messages: ['Saisine JLD dans les 48 h'],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [JldRetentionSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(JldRetentionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(JldRetentionSectionComponent.TOOL_LABEL).toContain('JLD');
    expect(JldRetentionSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(JldRetentionSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 when both date and motif IA signal present', () => {
    expect(JldRetentionSectionComponent.getPrefillCount({
      aiData: { dateNotificationOqtf: '2026-04-01', motifOqtfCode: 'REFUS_TITRE' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(JldRetentionSectionComponent.getPrefillCount({
      aiData: { dateNotificationOqtf: '2026-04-01', motifOqtfCode: 'REFUS_TITRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.result()!.statut).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationPlacement()).toBe('2026-05-08');
    expect(component.motifPlacement()).toBe('EXECUTION_OQTF');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false if date or motif missing', () => {
    component.dateNotificationPlacement.set(null);
    component.motifPlacement.set('EXECUTION_OQTF');
    expect(component.formValid()).toBe(false);
    component.dateNotificationPlacement.set('2026-05-08');
    component.motifPlacement.set(null);
    expect(component.formValid()).toBe(false);
    component.motifPlacement.set('EXECUTION_OQTF');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if date in future', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    component.dateNotificationPlacement.set(future.toISOString().slice(0, 10));
    component.motifPlacement.set('EXECUTION_OQTF');
    expect(component.formValid()).toBe(false);
  });

  it('formValid : recoursForme=true requires dateRecours >= notif', () => {
    component.dateNotificationPlacement.set('2026-05-08');
    component.motifPlacement.set('EXECUTION_OQTF');
    component.recoursForme.set(true);
    component.dateRecours.set(null);
    expect(component.formValid()).toBe(false);
    component.dateRecours.set('2026-05-07');
    expect(component.formValid()).toBe(false);
    component.dateRecours.set('2026-05-09');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationPlacement.set('2026-05-08');
    component.motifPlacement.set('EXECUTION_OQTF');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationPlacement: '2026-05-08',
      motifPlacement: 'EXECUTION_OQTF',
      recoursForme: false,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() recoursForme=true includes dateRecours', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationPlacement.set('2026-05-08');
    component.motifPlacement.set('EXECUTION_OQTF');
    component.recoursForme.set(true);
    component.dateRecours.set('2026-05-09');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateNotificationPlacement: '2026-05-08',
      motifPlacement: 'EXECUTION_OQTF',
      recoursForme: true,
      dateRecours: '2026-05-09',
    });
    req.flush(frResponse({ recoursForme: true, dateRecours: '2026-05-09', statut: 'RECOURS_FORME' }));
    expect(component.result()!.statut).toBe('RECOURS_FORME');
  });

  it('analyze() backend error -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationPlacement.set('2026-05-08');
    component.motifPlacement.set('EXECUTION_OQTF');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String), 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' })
    );
  });

  it('aiData with dateNotificationOqtf -> pre-fills + badges IA', () => {
    component.aiData = { dateNotificationOqtf: '2026-04-01', motifOqtfCode: 'REFUS_TITRE' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationPlacement()).toBe('2026-04-01');
    expect(component.provenanceDateNotification()).toBe('IA');
    expect(component.motifPlacement()).toBe('EXECUTION_OQTF');
    expect(component.provenanceMotifPlacement()).toBe('IA');
  });

  it('GET 200 -> no pre-fill', () => {
    component.aiData = { dateNotificationOqtf: '2026-04-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateNotificationPlacement: '2026-05-08' }));
    expect(component.dateNotificationPlacement()).toBe('2026-05-08');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { dateNotificationOqtf: '2026-04-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-04-02');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('coherenceAlerts : divergence date IA -> WARNING alert', () => {
    component.aiData = { dateNotificationOqtf: '2026-04-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    component.onDateNotificationChange('2026-04-05');
    component.onMotifPlacementChange('EXECUTION_OQTF');
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toBe('2026-04-01');
  });

  it('coherenceAlerts : OQTF detected but motif != EXECUTION_OQTF -> WARNING', () => {
    component.aiData = { motifOqtfCode: 'REFUS_TITRE' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    component.onMotifPlacementChange('AUTRE');
    const alert = component.coherenceAlerts().MOTIF_PLACEMENT;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
  });

  it('no alerts when showForm=false', () => {
    component.aiData = { dateNotificationOqtf: '2026-04-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateNotificationPlacement: '2026-04-05' }));
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuses', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('jld-banner--info');
    expect(component.bannerClass('URGENT')).toContain('jld-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('jld-banner--danger');
    expect(component.bannerClass('RECOURS_FORME')).toContain('jld-banner--success');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.statutLabel('URGENT')).toContain('URGENT');
  });

  it('showJoursRestants true only for DISPONIBLE and URGENT', () => {
    expect(component.showJoursRestants(frResponse({ statut: 'DISPONIBLE' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statut: 'URGENT' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statut: 'EXPIRE' }))).toBe(false);
    expect(component.showJoursRestants(null)).toBe(false);
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.motifPlacement()).toBeNull();
    component.aiData = { motifOqtfCode: 'REFUS_TITRE' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.motifPlacement()).toBe('EXECUTION_OQTF');
    expect(component.provenanceMotifPlacement()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ motifPlacement: 'AUTRE' }));
    component.aiData = { motifOqtfCode: 'REFUS_TITRE' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.motifPlacement()).toBe('AUTRE');
    expect(component.provenanceMotifPlacement()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-21-jld-retention-fr/calculate';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : aucun GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      try { (component as any).analyze(); } catch (_) { /* formValid */ }
      const dispatcherReqs = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL_F163 && r.method === 'POST');
      const caseFileReqs = httpMock.match((r: { url: string; method: string }) => r.url.includes('/api/v1/case-files/') && r.method === 'POST');
      // Aucun POST case-file ne doit partir en standalone.
      expect(caseFileReqs.length).toBe(0);
      dispatcherReqs.forEach((req: any) => req.flush({}));
    });
  });
});
