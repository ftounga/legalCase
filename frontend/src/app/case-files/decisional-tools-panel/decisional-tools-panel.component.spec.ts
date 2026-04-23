import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DecisionToolsPanelComponent } from './decisional-tools-panel.component';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DecisionToolsPanelComponent', () => {
  let component: DecisionToolsPanelComponent;
  let fixture: ComponentFixture<DecisionToolsPanelComponent>;
  let httpMock: HttpTestingController;
  let snackBar: jest.Mocked<MatSnackBar>;

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/decision-tools-visibility`;

  beforeEach(async () => {
    snackBar = { open: jest.fn() } as unknown as jest.Mocked<MatSnackBar>;

    await TestBed.configureTestingModule({
      imports: [DecisionToolsPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DecisionToolsPanelComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('renders both always-on and contextual groups, keeps catalog chips', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush({
      alwaysOn: ['F-DT-07-anciennete-conges-prime', 'F-DT-04-fiche-prudhomale'],
      contextual: ['F-DT-08-licenciement-validity'],
      catalog: ['F-DT-10-rupture-conv-validity', 'F-132-rupture-conv-indemnite'],
    });

    expect(component.resolvedAlwaysOn().map((x) => x.toolId))
      .toEqual(['F-DT-07-anciennete-conges-prime', 'F-DT-04-fiche-prudhomale']);
    expect(component.resolvedContextual().map((x) => x.toolId))
      .toEqual(['F-DT-08-licenciement-validity']);
    expect(component.visibility()!.catalog).toHaveLength(2);
    expect(component.isEmpty()).toBe(false);
  });

  it('shows empty state when alwaysOn and contextual are both empty', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({ alwaysOn: [], contextual: [], catalog: [] });

    expect(component.isEmpty()).toBe(true);
  });

  it('skips unknown tool_id with a console warning', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-07-anciennete-conges-prime', 'F-XX-999-unknown'],
      contextual: [],
      catalog: [],
    });

    const resolved = component.resolvedAlwaysOn().map((x) => x.toolId);
    expect(resolved).toEqual(['F-DT-07-anciennete-conges-prime']);
    expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('F-XX-999-unknown'));
    warnSpy.mockRestore();
  });

  it('shows snackbar on HTTP error and leaves lists empty', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush('err', { status: 500, statusText: 'Server Error' });

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Impossible de charger'),
      'Fermer',
      expect.any(Object)
    );
    expect(component.visibility()).toEqual({ alwaysOn: [], contextual: [], catalog: [] });
  });

  it('forwards tool-specific inputs for F-DT-08 licenciement (workspaceCountry + aiData + procedureChecks)', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-08-licenciement-validity'],
      contextual: [],
      catalog: [],
    });

    component.workspaceCountry = 'BELGIQUE';
    component.synthesis = {
      licenciementValidityDetection: { foo: 'bar' },
      piecesManquantesDetails: { any: 'thing' },
    };
    component.procedureChecks = [{ id: 'c1' }];
    component.aiQuestions = [{ id: 'q1' }];

    const entry = component.resolveEntry('F-DT-08-licenciement-validity')!;
    const inputs = component.componentInputsFor(entry);

    expect(inputs).toEqual({
      caseFileId: CASE_FILE_ID,
      workspaceCountry: 'BELGIQUE',
      aiData: { foo: 'bar' },
      procedureChecks: [{ id: 'c1' }],
      aiQuestions: [{ id: 'q1' }],
      piecesManquantes: { any: 'thing' },
    });
  });

  it('forwards F-IM-05 inputs including triggerEvents and piecesManquantes', () => {
    component.synthesis = {
      immigrationExtractedData: { inferredChecklistType: 'X' },
      immigrationTriggerEvents: [{ e: 1 }],
      piecesManquantesDetails: { p: 1 },
    };
    component.procedureChecks = [];
    component.aiQuestions = [];

    const entry = component.resolveEntry('F-IM-05-arbre-decisionnel-titre')!;
    const inputs = component.componentInputsFor(entry);

    expect(inputs['aiData']).toEqual({ inferredChecklistType: 'X' });
    expect(inputs['triggerEvents']).toEqual([{ e: 1 }]);
    expect(inputs['piecesManquantes']).toEqual({ p: 1 });
  });

  it('resolves F-132-rupture-amiable-info to RuptureAmiableInfoSectionComponent', () => {
    const entry = component.resolveEntry('F-132-rupture-amiable-info');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('RuptureAmiableInfoSectionComponent');
  });

  it('resolves registered tool IDs to their Angular component types', () => {
    expect(component.resolveEntry('F-DT-07-anciennete-conges-prime')).not.toBeNull();
    expect(component.resolveEntry('F-IM-05-arbre-decisionnel-titre')).not.toBeNull();
    expect(component.resolveEntry('F-132-rupture-conv-indemnite')).not.toBeNull();
  });
});

describe('DecisionToolsPanelComponent — SF-IA-04-04 refresh on CaseDashboardRefreshService', () => {
  let component: DecisionToolsPanelComponent;
  let fixture: ComponentFixture<DecisionToolsPanelComponent>;
  let httpMock: HttpTestingController;
  let refreshService: CaseDashboardRefreshService;

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/decision-tools-visibility`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecisionToolsPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
        CaseDashboardRefreshService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    refreshService = TestBed.inject(CaseDashboardRefreshService);
    fixture = TestBed.createComponent(DecisionToolsPanelComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('reloads visibility silently when refresh service emits', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({ alwaysOn: [], contextual: [], catalog: [] });
    expect(component.loading()).toBe(false);

    refreshService.triggerRefresh();
    tick(300);

    const reloadReq = httpMock.expectOne(API_URL);
    expect(component.loading()).toBe(false);
    reloadReq.flush({ alwaysOn: ['F-DT-08-licenciement-validity'], contextual: [], catalog: [] });

    expect(component.visibility()!.alwaysOn).toEqual(['F-DT-08-licenciement-validity']);
  }));
});
