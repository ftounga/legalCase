import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DecisionToolsPanelComponent } from './decisional-tools-panel.component';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { DecisionToolModalService } from './decision-tool-modal/decision-tool-modal.service';
import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';

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

  // F-177 SF-177-12 — la card du panel doit afficher le badge `auto_awesome`
  // dès que le composant outil expose un static `getPrefillCount` qui renvoie > 0.
  it('prefillCountFor returns count for instrumented Immigration tool (Chen 5 case)', () => {
    component.synthesis = {
      immigrationExtractedData: {
        nationaliteUe: false,
        typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE',
      },
      immigrationTriggerEvents: [{ eventCode: 'MARIAGE_RESSORTISSANT_FR' }],
      piecesManquantesDetails: [],
    };
    expect(component.prefillCountFor('F-IM-05-arbre-decisionnel-titre')).toBe(3);
  });

  it('prefillCountFor returns null for an unknown tool id (fallback safe)', () => {
    expect(component.prefillCountFor('F-XX-999-unknown')).toBeNull();
  });

  it('prefillCountFor returns null for a tool without static getPrefillCount', () => {
    // F-DT-07 (anciennete) n'expose pas encore `getPrefillCount` dans cette SF.
    expect(component.prefillCountFor('F-DT-07-anciennete-conges-prime')).toBeNull();
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
    // SF-DT-27-02 : motif grave BE intégré au TOOL_REGISTRY.
    expect(component.resolveEntry('F-DT-27-motif-grave-be')).not.toBeNull();
  });

  it('SF-DT-27-02: maps F-DT-27-motif-grave-be to MotifGraveBeSectionComponent with canonical inputs', () => {
    component.caseFileId = 'cf-123';
    component.workspaceCountry = 'BELGIQUE';
    component.synthesis = {
      travailExtractedData: { dateLicenciement: '2026-04-02', salaireBrutMensuel: 3000 },
      piecesManquantesDetails: { foo: 'bar' },
    };
    component.procedureChecks = [{ p: 1 }];
    component.aiQuestions = [{ q: 1 }];

    const entry = component.resolveEntry('F-DT-27-motif-grave-be')!;
    expect(entry).not.toBeNull();
    expect(entry.component.name).toBe('MotifGraveBeSectionComponent');

    const inputs = component.componentInputsFor(entry);
    expect(inputs['caseFileId']).toBe('cf-123');
    expect(inputs['workspaceCountry']).toBe('BELGIQUE');
    expect(inputs['aiData']).toEqual({ dateLicenciement: '2026-04-02', salaireBrutMensuel: 3000 });
    expect(inputs['procedureChecks']).toEqual([{ p: 1 }]);
    expect(inputs['aiQuestions']).toEqual([{ q: 1 }]);
    expect(inputs['piecesManquantes']).toEqual({ foo: 'bar' });
  });

  it('SF-FA-17-02 : resolves F-FA-17-partage-judiciaire to PartageJudiciaireSectionComponent + bindings IA', () => {
    const entry = component.resolveEntry('F-FA-17-partage-judiciaire');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('PartageJudiciaireSectionComponent');

    component.caseFileId = 'case-pj-1';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = {
      familleExtractedData: {
        pvDifficultesEtablisDetected: true,
        nombreCoindivisairesDetecte: 3,
      },
      piecesManquantesDetails: [{ texte: 'PV difficultés', critereCode: 'PARTAGE_JUDICIAIRE_PV' }],
    };
    component.procedureChecks = [{ id: 'c1' } as any];
    component.aiQuestions = [{ id: 'q1' } as any];

    const inputs = component.componentInputsFor(entry!);
    expect(inputs['caseFileId']).toBe('case-pj-1');
    expect(inputs['workspaceCountry']).toBe('FRANCE');
    expect(inputs['aiData']).toEqual({
      pvDifficultesEtablisDetected: true,
      nombreCoindivisairesDetecte: 3,
    });
    expect(inputs['procedureChecks']).toEqual([{ id: 'c1' }]);
    expect(inputs['aiQuestions']).toEqual([{ id: 'q1' }]);
    expect(inputs['piecesManquantes']).toEqual([
      { texte: 'PV difficultés', critereCode: 'PARTAGE_JUDICIAIRE_PV' },
    ]);
  });

  it('SF-FA-18-02 : resolves F-FA-18-reconnaissance-paternelle to ReconnaissancePaternelleSectionComponent + bindings IA', () => {
    const entry = component.resolveEntry('F-FA-18-reconnaissance-paternelle');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('ReconnaissancePaternelleSectionComponent');

    component.caseFileId = 'case-rp-1';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = {
      familleExtractedData: {
        consentementLibreDuPereDetected: true,
        paterniteVraisemblableDetected: true,
        dateNaissanceEnfantDetectee: '2024-03-15',
      },
      piecesManquantesDetails: [
        { texte: 'Acte naissance', critereCode: 'RECONNAISSANCE_PATERNELLE_PROCEDURE' },
      ],
    };
    component.procedureChecks = [{ id: 'c1' } as any];
    component.aiQuestions = [{ id: 'q1' } as any];

    const inputs = component.componentInputsFor(entry!);
    expect(inputs['caseFileId']).toBe('case-rp-1');
    expect(inputs['workspaceCountry']).toBe('FRANCE');
    expect(inputs['aiData']).toEqual({
      consentementLibreDuPereDetected: true,
      paterniteVraisemblableDetected: true,
      dateNaissanceEnfantDetectee: '2024-03-15',
    });
    expect(inputs['procedureChecks']).toEqual([{ id: 'c1' }]);
    expect(inputs['aiQuestions']).toEqual([{ id: 'q1' }]);
    expect(inputs['piecesManquantes']).toEqual([
      { texte: 'Acte naissance', critereCode: 'RECONNAISSANCE_PATERNELLE_PROCEDURE' },
    ]);
  });

  it('SF-DT-21-02 : resolves F-DT-21-travail-dissimule to TravailDissimuleSectionComponent + bindings IA', () => {
    const entry = component.resolveEntry('F-DT-21-travail-dissimule');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('TravailDissimuleSectionComponent');

    component.caseFileId = 'case-td-1';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = {
      travailExtractedData: { salaireBrutMensuel: 2500 },
      piecesManquantesDetails: [{ texte: 'Bulletins', critereCode: 'SALAIRE_BRUT_MENSUEL' }],
    };
    component.procedureChecks = [{ id: 'c1' } as any];
    component.aiQuestions = [{ id: 'q1' } as any];

    const inputs = component.componentInputsFor(entry!);
    expect(inputs['caseFileId']).toBe('case-td-1');
    expect(inputs['workspaceCountry']).toBe('FRANCE');
    expect(inputs['aiData']).toEqual({ salaireBrutMensuel: 2500 });
    expect(inputs['procedureChecks']).toEqual([{ id: 'c1' }]);
    expect(inputs['aiQuestions']).toEqual([{ id: 'q1' }]);
    expect(inputs['piecesManquantes']).toEqual([
      { texte: 'Bulletins', critereCode: 'SALAIRE_BRUT_MENSUEL' },
    ]);
  });

  // ── SF-169-01 — Grid 2 colonnes + groupement par thème métier ────────────

  it('SF-169-01 T-01: THEME_BY_TOOL_ID couvre tous les tool_ids du TOOL_REGISTRY', () => {
    const registryIds = Array.from(DecisionToolsPanelComponent.TOOL_REGISTRY.keys());
    const mappedIds = Array.from(DecisionToolsPanelComponent.THEME_BY_TOOL_ID.keys());
    const unmapped = registryIds.filter((id) => !mappedIds.includes(id));
    expect(unmapped).toEqual([]);
  });

  it('SF-169-01 T-02: outils classés dans le bon thème', () => {
    const map = DecisionToolsPanelComponent.THEME_BY_TOOL_ID;
    expect(map.get('F-DT-25-indemnite-preavis')).toBe('INDEMNITES');
    expect(map.get('F-DT-08-licenciement-validity')).toBe('VALIDITE');
    expect(map.get('F-DT-03-prescription-litige')).toBe('DELAIS');
    expect(map.get('F-DT-04-fiche-prudhomale')).toBe('DOCUMENTS');
    expect(map.get('F-IM-05-arbre-decisionnel-titre')).toBe('DIAGNOSTIC');
  });

  it('SF-169-01 T-03: thème sans outils est exclu de themedTools()', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: [],
    });

    const themed = component.themedTools();
    expect(themed.get('INDEMNITES')?.length).toBe(1);
    expect(themed.has('VALIDITE')).toBe(false);
    expect(themed.has('DELAIS')).toBe(false);
    expect(themed.has('DOCUMENTS')).toBe(false);
    expect(themed.has('DIAGNOSTIC')).toBe(false);
  });

  // ── SF-177-11 — Bascule cards + modal ────────────────────────────────────

  it('SF-177-11 T-01 : cardMetadataFor lit TOOL_LABEL/TOOL_ICON statics du composant', () => {
    const entry = component.resolveEntry('F-DT-07-anciennete-conges-prime')!;
    const meta = component.cardMetadataFor(entry, 'F-DT-07-anciennete-conges-prime');
    expect(meta.label).toBe(AncienneteSectionComponent.TOOL_LABEL);
    expect(meta.icon).toBe(AncienneteSectionComponent.TOOL_ICON);
  });

  it('SF-177-11 T-02 : cardMetadataFor fallback {toolId, extension} si statics absents', () => {
    class StubWithoutStatics {}
    const fakeEntry = {
      component: StubWithoutStatics as any,
      inputs: () => ({}),
    };
    const meta = component.cardMetadataFor(fakeEntry as any, 'X-FAKE');
    expect(meta).toEqual({ label: 'X-FAKE', icon: 'extension' });
  });

  it('SF-177-11 T-03 : openTool délègue au modalService avec forceExpanded:true + meta', () => {
    const openSpy = jest
      .spyOn(TestBed.inject(DecisionToolModalService), 'open')
      .mockReturnValue({ close: jest.fn() } as any);

    component.caseFileId = 'cf-177-11';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = { travailExtractedData: { foo: 'bar' } };
    component.procedureChecks = [];
    component.aiQuestions = [];

    const entry = component.resolveEntry('F-DT-07-anciennete-conges-prime')!;
    component.openTool('F-DT-07-anciennete-conges-prime', entry);

    expect(openSpy).toHaveBeenCalledTimes(1);
    const args = openSpy.mock.calls[0][0];
    expect(args.toolId).toBe('F-DT-07-anciennete-conges-prime');
    expect(args.title).toBe(AncienneteSectionComponent.TOOL_LABEL);
    expect(args.icon).toBe(AncienneteSectionComponent.TOOL_ICON);
    expect(args.component).toBe(AncienneteSectionComponent);
    expect(args.inputs['forceExpanded']).toBe(true);
    expect(args.inputs['caseFileId']).toBe('cf-177-11');
    expect(args.onSave).toBeUndefined();
  });

  it('SF-169-01 T-04: tool_id non mappé tombe sur DIAGNOSTIC + warn console', () => {
    // Note : F-IM-19-mineurs est mappé en DIAGNOSTIC, on triche en spyant le mapping
    // pour simuler un toolId présent dans TOOL_REGISTRY mais absent de THEME_BY_TOOL_ID.
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    const originalGet = DecisionToolsPanelComponent.THEME_BY_TOOL_ID.get.bind(
      DecisionToolsPanelComponent.THEME_BY_TOOL_ID
    );
    const getSpy = jest
      .spyOn(DecisionToolsPanelComponent.THEME_BY_TOOL_ID, 'get')
      .mockImplementation((id: string) =>
        id === 'F-DT-25-indemnite-preavis' ? undefined : originalGet(id)
      );

    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: [],
    });

    const themed = component.themedTools();
    expect(themed.get('DIAGNOSTIC')?.map((x) => x.toolId)).toEqual([
      'F-DT-25-indemnite-preavis',
    ]);
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('F-DT-25-indemnite-preavis')
    );

    getSpy.mockRestore();
    warnSpy.mockRestore();
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
