import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import {
  ConclusionsSectionComponent,
  extractPlaceholders,
  parseMarkdownSections,
  replaceSectionInDraft,
  buildExportContent,
  draftStorageKey,
  readDraft,
  writeDraft,
  clearDraft,
} from './conclusions-section.component';
import { DocxExportService } from '../../core/services/docx-export.service';
import { PdfExportService } from '../../core/services/pdf-export.service';
import {
  ConclusionResponse,
  ConclusionVersionSummary,
} from '../../core/models/conclusion.model';

describe('ConclusionsSectionComponent', () => {
  let component: ConclusionsSectionComponent;
  let fixture: ComponentFixture<ConclusionsSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let docxSpy: jasmine.SpyObj<DocxExportService>;
  let pdfSpy: jasmine.SpyObj<PdfExportService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const CASE_ID = 'case-1';
  const GET_URL = `/api/v1/case-files/${CASE_ID}/conclusions`;
  const GENERATE_URL = `/api/v1/case-files/${CASE_ID}/conclusions/generate`;
  const VERSIONS_URL = `/api/v1/case-files/${CASE_ID}/conclusions/versions`;
  // F-258 SF-258-01 — endpoints du décompte « outils non calculés ».
  const VISIBILITY_URL = `/api/v1/case-files/${CASE_ID}/decision-tools-visibility`;
  const DASHBOARD_URL = `/api/v1/case-files/${CASE_ID}/dashboard`;
  // F-98 SF-98-56 — endpoint du décompte « citations adverses marquées ».
  const CHECKS_URL = `/api/v1/case-files/${CASE_ID}/jurisprudence-checks`;
  // F-266 SF-266-01 — endpoint des documents (pièces numérotées au survol).
  const DOCUMENTS_URL = `/api/v1/case-files/${CASE_ID}/documents`;
  const versionUrl = (id: string) => `${VERSIONS_URL}/${id}`;
  const lifecycleUrl = (id: string) => `${versionUrl(id)}/lifecycle`;
  const contentUrl = (id: string) => `${versionUrl(id)}/content`;

  function response(
    overrides: Partial<ConclusionResponse> = {},
  ): ConclusionResponse {
    return {
      id: null,
      caseFileId: CASE_ID,
      status: 'NOT_GENERATED',
      versionNumber: null,
      lifecycleStatus: null,
      content: null,
      jurisdictionLabel: null,
      stageLabel: null,
      positionLabel: null,
      modelUsed: null,
      generatedAt: null,
      errorMessage: null,
      createdAt: null,
      updatedAt: null,
      ...overrides,
    };
  }

  function doneResponse(
    overrides: Partial<ConclusionResponse> = {},
  ): ConclusionResponse {
    return response({
      id: 'conc-2',
      status: 'DONE',
      versionNumber: 2,
      lifecycleStatus: 'DRAFT',
      content: 'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
      jurisdictionLabel: 'Conseil de prud\'hommes',
      stageLabel: 'Bureau de jugement (fond)',
      positionLabel: 'Demandeur (salarié)',
      modelUsed: 'claude-sonnet-4-6',
      generatedAt: '2026-05-18T10:00:00Z',
      ...overrides,
    });
  }

  function versionSummary(
    overrides: Partial<ConclusionVersionSummary> = {},
  ): ConclusionVersionSummary {
    return {
      id: 'conc-2',
      versionNumber: 2,
      lifecycleStatus: 'DRAFT',
      status: 'DONE',
      generatedAt: '2026-05-18T10:00:00Z',
      createdAt: '2026-05-18T09:55:00Z',
      ...overrides,
    };
  }

  /** Liste type : v2 (la plus récente, en tête) puis v1. */
  function versionList(): ConclusionVersionSummary[] {
    return [
      versionSummary({ id: 'conc-2', versionNumber: 2 }),
      versionSummary({
        id: 'conc-1',
        versionNumber: 1,
        lifecycleStatus: 'VALIDATED',
      }),
    ];
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    docxSpy = jasmine.createSpyObj('DocxExportService', ['exportConclusion']);
    pdfSpy = jasmine.createSpyObj('PdfExportService', ['exportConclusion']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    // Par défaut : confirmation acceptée (afterClosed → true).
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(true),
    } as never);
    await TestBed.configureTestingModule({
      imports: [
        ConclusionsSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: DocxExportService, useValue: docxSpy },
        { provide: PdfExportService, useValue: pdfSpy },
        { provide: MatDialog, useValue: dialogSpy },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConclusionsSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_ID;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  /**
   * Sert le GET initial des conclusions PUIS le `listVersions` best-effort
   * qui suit toujours (SF-98-52). `versions` vaut `[]` par défaut.
   *
   * F-258 SF-258-01 — sert aussi les deux GET du décompte « outils non
   * calculés » (visibilité + dashboard) déclenchés en parallèle au montage.
   * Par défaut visibilité et tiles sont vides ⇒ N=0 ⇒ aucun encart, ce qui
   * n'affecte pas les tests antérieurs.
   *
   * F-98 SF-98-56 — sert aussi le GET des jurisprudence-checks (décompte des
   * citations adverses marquées). Par défaut aucun check ⇒ N=0 ⇒ aucune mention.
   */
  function flushInitialLoad(
    conclusion: ConclusionResponse = response(),
    versions: ConclusionVersionSummary[] = [],
    visibility: { alwaysOn: string[]; contextual: string[]; catalog: string[] } = {
      alwaysOn: [],
      contextual: [],
      catalog: [],
    },
    tileToolIds: string[] = [],
    checks: unknown[] = [],
  ): void {
    httpMock.expectOne(VISIBILITY_URL).flush(visibility);
    httpMock.expectOne(DASHBOARD_URL).flush({
      caseFileId: CASE_ID,
      legalDomain: 'TRAVAIL',
      riskScore: null,
      riskLevel: null,
      tiles: tileToolIds.map((toolId) => ({
        toolId,
        theme: 'INDEMNITES',
        label: toolId,
        primaryValue: '—',
      })),
    });
    httpMock.expectOne(CHECKS_URL).flush({ checks });
    // F-266 SF-266-01 — GET documents (pièces) déclenché au montage.
    httpMock.expectOne(DOCUMENTS_URL).flush([]);
    httpMock.expectOne(GET_URL).flush(conclusion);
    httpMock.expectOne(VERSIONS_URL).flush(versions);
  }

  // ---------------------------------------------------------------------------
  // Montage + GET initial
  // ---------------------------------------------------------------------------

  it('montage → GET /conclusions + GET /versions déclenchés', () => {
    fixture.detectChanges();
    // F-258 — les deux GET du décompte partent aussi au montage.
    httpMock.expectOne(VISIBILITY_URL).flush({ alwaysOn: [], contextual: [], catalog: [] });
    httpMock.expectOne(DASHBOARD_URL).flush({ caseFileId: CASE_ID, tiles: [] });
    // F-98 SF-98-56 — le GET des jurisprudence-checks part aussi au montage.
    httpMock.expectOne(CHECKS_URL).flush({ checks: [] });
    // F-266 SF-266-01 — le GET des documents (pièces) part aussi au montage.
    httpMock.expectOne(DOCUMENTS_URL).flush([]);
    const req = httpMock.expectOne(GET_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    const versReq = httpMock.expectOne(VERSIONS_URL);
    expect(versReq.request.method).toBe('GET');
    versReq.flush([]);
    expect(component.loading()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // NOT_GENERATED — bouton « Générer »
  // ---------------------------------------------------------------------------

  it('NOT_GENERATED + pré-requis OK → bouton « Générer » actif visible', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="generate-btn"]',
    );
    expect(btn).not.toBeNull();
    expect(btn.disabled).toBe(false);
    expect(btn.textContent).toContain('Générer le projet de conclusions');
  });

  it('pré-requis manquant (analyse) → message guidant + bouton désactivé', () => {
    component.hasCompletedAnalysis = false;
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    const message = fixture.nativeElement.querySelector(
      '[data-testid="prerequisite-message"]',
    );
    expect(message).not.toBeNull();
    expect(message.textContent).toContain('terminez l\'analyse');

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="generate-btn"]',
    );
    expect(btn.disabled).toBe(true);
  });

  it('pré-requis manquant (stade) → message guidant stade procédural', () => {
    component.procedureStageComplete = false;
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    const message = fixture.nativeElement.querySelector(
      '[data-testid="prerequisite-message"]',
    );
    expect(message.textContent).toContain('stade procédural');
  });

  // ---------------------------------------------------------------------------
  // DONE — bandeau de transparence + texte + copier
  // ---------------------------------------------------------------------------

  it('DONE → bandeau de transparence + texte des conclusions affichés', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector(
      '[data-testid="transparency-banner"]',
    );
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('relecture par l\'avocat obligatoire');
    // Le bandeau de transparence n'est PAS rouge.
    expect(banner.className).not.toContain('error');

    const content = fixture.nativeElement.querySelector(
      '[data-testid="conclusions-content"]',
    );
    expect(content.textContent).toContain('FAITS ET PROCÉDURE');
  });

  it('DONE → « Copier » écrit le texte dans le presse-papier', fakeAsync(() => {
    const writeSpy = jasmine
      .createSpy('writeText')
      .and.returnValue(Promise.resolve());
    Object.assign(navigator, { clipboard: { writeText: writeSpy } });

    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const copyBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="copy-btn"]',
    );
    copyBtn.click();
    tick();

    expect(writeSpy).toHaveBeenCalledWith(
      'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
    );
    expect(snackSpy.open).toHaveBeenCalled();
  }));

  // ---------------------------------------------------------------------------
  // SF-98-52 — Sélecteur de version
  // ---------------------------------------------------------------------------

  it('DONE avec versions → sélecteur de version affiché, dernière sélectionnée', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector(
      '[data-testid="version-select"]',
    );
    expect(select).not.toBeNull();
    // La version la plus récente (conc-2 = v2) est sélectionnée par défaut.
    expect(component.selectedVersionId()).toBe('conc-2');
    expect(component.versions().length).toBe(2);
  });

  it('changement de version → recharge le contenu via getVersion', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    // L'avocat sélectionne la version 1.
    component.selectVersion('conc-1');
    const req = httpMock.expectOne(versionUrl('conc-1'));
    expect(req.request.method).toBe('GET');
    req.flush(
      doneResponse({
        id: 'conc-1',
        versionNumber: 1,
        lifecycleStatus: 'VALIDATED',
        content: 'Contenu version 1',
      }),
    );

    expect(component.selectedVersionId()).toBe('conc-1');
    expect(component.conclusion()?.content).toBe('Contenu version 1');
    expect(component.conclusion()?.versionNumber).toBe(1);
  });

  it('sélectionner la version déjà affichée → aucun appel réseau', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.selectVersion('conc-2');
    httpMock.expectNone(versionUrl('conc-2'));
  });

  // ---------------------------------------------------------------------------
  // SF-98-52 — Badge de cycle de vie
  // ---------------------------------------------------------------------------

  it('DONE → badge de cycle de vie affiché avec le libellé FR', () => {
    fixture.detectChanges();
    flushInitialLoad(
      doneResponse({ lifecycleStatus: 'VALIDATED' }),
      versionList(),
    );
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector(
      '[data-testid="lifecycle-badge"]',
    );
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('Validé');
    // Badge informatif — jamais rouge.
    expect(badge.className).not.toContain('error');
  });

  // ---------------------------------------------------------------------------
  // SF-98-52 — Changement de cycle de vie
  // ---------------------------------------------------------------------------

  it('changeLifecycle → PATCH lifecycle puis met à jour le contenu', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.changeLifecycle('VALIDATED');
    const req = httpMock.expectOne(lifecycleUrl('conc-2'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ lifecycleStatus: 'VALIDATED' });
    req.flush(doneResponse({ lifecycleStatus: 'VALIDATED' }));
    // Le PATCH déclenche un refreshVersions best-effort.
    httpMock.expectOne(VERSIONS_URL).flush(versionList());

    expect(component.lifecycleStatus()).toBe('VALIDATED');
    expect(component.updatingLifecycle()).toBe(false);
  });

  it('changeLifecycle → 409 affiche le message backend via snackbar', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.changeLifecycle('DEPOSITED');
    httpMock.expectOne(lifecycleUrl('conc-2')).flush(
      {
        error: 'CONCLUSION_NOT_DONE',
        message: 'Seule une version générée peut être validée ou déposée.',
      },
      { status: 409, statusText: 'Conflict' },
    );

    expect(snackSpy.open).toHaveBeenCalledWith(
      'Seule une version générée peut être validée ou déposée.',
      'Fermer',
      jasmine.objectContaining({ panelClass: ['snack-error'] }),
    );
    expect(component.updatingLifecycle()).toBe(false);
  });

  it('changeLifecycle vers l\'état courant → aucun appel réseau', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
    fixture.detectChanges();

    component.changeLifecycle('DRAFT');
    httpMock.expectNone(lifecycleUrl('conc-2'));
  });

  // ---------------------------------------------------------------------------
  // Génération + polling
  // ---------------------------------------------------------------------------

  it('clic « Générer » → POST puis polling jusqu\'à DONE', fakeAsync(() => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="generate-btn"]',
    );
    btn.click();

    const postReq = httpMock.expectOne(GENERATE_URL);
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ status: 'PENDING', versionNumber: 1 });
    // generate() relance un refreshVersions best-effort.
    httpMock.expectOne(VERSIONS_URL).flush([]);

    expect(component.status()).toBe('PENDING');
    expect(component.conclusion()?.versionNumber).toBe(1);

    // 1er tour de polling (3 s) → toujours PROCESSING
    tick(3000);
    httpMock.expectOne(GET_URL).flush(response({ status: 'PROCESSING' }));
    expect(component.status()).toBe('PROCESSING');

    // 2e tour de polling → DONE → le polling s'arrête + refreshVersions
    tick(3000);
    httpMock.expectOne(GET_URL).flush(doneResponse());
    httpMock.expectOne(VERSIONS_URL).flush(versionList());
    // SF-98-56 — la génération aboutie rafraîchit le décompte adverse.
    httpMock.expectOne(CHECKS_URL).flush({ checks: [] });
    expect(component.status()).toBe('DONE');

    // Plus aucun appel après DONE.
    tick(3000);
    httpMock.expectNone(GET_URL);

    component.ngOnDestroy();
  }));

  // F-278 — garde anti-écrasement à la régénération (couplée F-271).
  it('régénération avec versions existantes → confirmation puis POST', fakeAsync(() => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    expect(component.hasVersions()).toBe(true);
    component.requestGenerate();

    // F-278 : une confirmation a été ouverte avant la régénération.
    expect(dialogSpy.open).toHaveBeenCalledTimes(1);
    const dialogData = dialogSpy.open.calls.mostRecent().args[1]?.data as {
      title: string;
      message: string;
    };
    expect(dialogData.title).toContain('Régénérer');
    expect(dialogData.message).toContain('vos modifications incluses');

    // afterClosed → true (défaut) → la génération part.
    const postReq = httpMock.expectOne(GENERATE_URL);
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ status: 'PENDING', versionNumber: 3 });
    httpMock.expectOne(VERSIONS_URL).flush(versionList());

    component.ngOnDestroy();
  }));

  it('régénération annulée → aucune requête POST', fakeAsync(() => {
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(false),
    } as never);
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.requestGenerate();

    expect(dialogSpy.open).toHaveBeenCalledTimes(1);
    httpMock.expectNone(GENERATE_URL);

    component.ngOnDestroy();
  }));

  it('première génération (aucune version) → pas de confirmation, POST direct', fakeAsync(() => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad(); // versions = [] par défaut
    fixture.detectChanges();

    expect(component.hasVersions()).toBe(false);
    component.requestGenerate();

    expect(dialogSpy.open).not.toHaveBeenCalled();
    const postReq = httpMock.expectOne(GENERATE_URL);
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ status: 'PENDING', versionNumber: 1 });
    httpMock.expectOne(VERSIONS_URL).flush([]);

    component.ngOnDestroy();
  }));

  // SF-98-62 — une garde 409 (ex. combinaison procédurale non couverte) est un refus
  // actionnable → message PERSISTANT dans la section, plus de snackbar fugace.
  it('erreur 409 → message backend persistant dans la section (pas de snackbar)', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad();

    component.generate();
    httpMock.expectOne(GENERATE_URL).flush(
      {
        error: 'COMBINATION_NOT_SUPPORTED',
        message: "La génération de conclusions n'est pas encore disponible pour la combinaison procédurale du dossier.",
      },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(component.unsupportedMessage()).toBe(
      "La génération de conclusions n'est pas encore disponible pour la combinaison procédurale du dossier.",
    );
    expect(snackSpy.open).not.toHaveBeenCalled();
    expect(component.generating()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // FAILED — message d'échec
  // ---------------------------------------------------------------------------

  it('FAILED → message d\'échec + bouton « Réessayer »', () => {
    fixture.detectChanges();
    flushInitialLoad(
      response({ status: 'FAILED', errorMessage: 'Timeout IA.' }),
    );
    fixture.detectChanges();

    const failed = fixture.nativeElement.querySelector(
      '[data-testid="failed-message"]',
    );
    expect(failed).not.toBeNull();
    expect(failed.textContent).toContain('échoué');
    expect(failed.textContent).toContain('Timeout IA.');

    const retry = fixture.nativeElement.querySelector('[data-testid="retry-btn"]');
    expect(retry).not.toBeNull();
  });

  // ---------------------------------------------------------------------------
  // PENDING/PROCESSING au montage → indicateur + polling auto
  // ---------------------------------------------------------------------------

  it('montage en PROCESSING → indicateur « Génération en cours » + polling', fakeAsync(() => {
    fixture.detectChanges();
    flushInitialLoad(response({ status: 'PROCESSING' }));
    fixture.detectChanges();

    const indicator = fixture.nativeElement.querySelector(
      '[data-testid="generating-indicator"]',
    );
    expect(indicator).not.toBeNull();

    tick(3000);
    httpMock.expectOne(GET_URL).flush(doneResponse());
    httpMock.expectOne(VERSIONS_URL).flush(versionList());
    // SF-98-56 — la génération aboutie rafraîchit le décompte adverse.
    httpMock.expectOne(CHECKS_URL).flush({ checks: [] });
    expect(component.status()).toBe('DONE');

    component.ngOnDestroy();
  }));

  // ---------------------------------------------------------------------------
  // SF-98-49 — Éditeur de relecture
  // ---------------------------------------------------------------------------

  it('DONE + DRAFT → bouton « Modifier » visible', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
    fixture.detectChanges();

    const editBtn = fixture.nativeElement.querySelector(
      '[data-testid="edit-btn"]',
    );
    expect(editBtn).not.toBeNull();
    expect(editBtn.textContent).toContain('Modifier');
    expect(component.editable()).toBe(true);
  });

  it('DONE + VALIDATED → bouton « Modifier » masqué (lecture seule)', () => {
    fixture.detectChanges();
    flushInitialLoad(
      doneResponse({ lifecycleStatus: 'VALIDATED' }),
      versionList(),
    );
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="edit-btn"]'),
    ).toBeNull();
    expect(component.editable()).toBe(false);
  });

  it('DONE + DEPOSITED → bouton « Modifier » masqué (lecture seule)', () => {
    fixture.detectChanges();
    flushInitialLoad(
      doneResponse({ lifecycleStatus: 'DEPOSITED' }),
      versionList(),
    );
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="edit-btn"]'),
    ).toBeNull();
    expect(component.editable()).toBe(false);
  });

  it('clic « Modifier » → textarea pré-rempli avec le contenu courant', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
    fixture.detectChanges();

    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="edit-btn"]',
    );
    editBtn.click();
    fixture.detectChanges();

    const editor: HTMLTextAreaElement = fixture.nativeElement.querySelector(
      '[data-testid="conclusions-editor"]',
    );
    expect(editor).not.toBeNull();
    expect(editor.value).toBe('POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…');
    expect(component.editing()).toBe(true);
    // SF-264-01 — en édition, le bloc lecture seule disparaît mais l'aperçu live
    // (rendu via le même ConclusionDocumentComponent) reste rendu : on vérifie
    // que c'est bien dans la colonne d'aperçu, pas le bloc lecture.
    expect(
      fixture.nativeElement.querySelector('[data-testid="editor-pane"]'),
    ).not.toBeNull();
    expect(
      fixture.nativeElement.querySelector('[data-testid="preview-pane"]'),
    ).not.toBeNull();
  });

  it('« Enregistrer » → PATCH /content puis retour en lecture + rafraîchissement', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
    fixture.detectChanges();

    component.startEditing();
    component.onDraftInput('Texte révisé par l\'avocat.');
    fixture.detectChanges();

    const saveBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="save-btn"]',
    );
    saveBtn.click();

    const req = httpMock.expectOne(contentUrl('conc-2'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ content: 'Texte révisé par l\'avocat.' });
    req.flush(
      doneResponse({
        lifecycleStatus: 'DRAFT',
        content: 'Texte révisé par l\'avocat.',
      }),
    );
    // saveContent déclenche un refreshVersions best-effort.
    httpMock.expectOne(VERSIONS_URL).flush(versionList());
    fixture.detectChanges();

    expect(component.editing()).toBe(false);
    expect(component.savingContent()).toBe(false);
    expect(component.conclusion()?.content).toBe('Texte révisé par l\'avocat.');
    const content = fixture.nativeElement.querySelector(
      '[data-testid="conclusions-content"]',
    );
    expect(content.textContent).toContain('Texte révisé par l\'avocat.');
  });

  it('« Enregistrer » → 409 affiche le message backend via snackbar', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
    fixture.detectChanges();

    component.startEditing();
    component.saveContent();
    httpMock.expectOne(contentUrl('conc-2')).flush(
      {
        error: 'CONCLUSION_NOT_DRAFT',
        message: 'Seul un brouillon peut être modifié.',
      },
      { status: 409, statusText: 'Conflict' },
    );

    expect(snackSpy.open).toHaveBeenCalledWith(
      'Seul un brouillon peut être modifié.',
      'Fermer',
      jasmine.objectContaining({ panelClass: ['snack-error'] }),
    );
    expect(component.savingContent()).toBe(false);
    // En cas d'échec, on reste en mode édition.
    expect(component.editing()).toBe(true);
  });

  it('« Annuler » → restaure le texte sans appel serveur', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
    fixture.detectChanges();

    component.startEditing();
    component.onDraftInput('Modification non sauvegardée');
    fixture.detectChanges();

    const cancelBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="cancel-edit-btn"]',
    );
    cancelBtn.click();
    fixture.detectChanges();

    httpMock.expectNone(contentUrl('conc-2'));
    expect(component.editing()).toBe(false);
    const content = fixture.nativeElement.querySelector(
      '[data-testid="conclusions-content"]',
    );
    // Le texte affiché reste le contenu original.
    expect(content.textContent).toContain('FAITS ET PROCÉDURE');
  });

  it('changement de version pendant l\'édition → sort du mode édition', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
    fixture.detectChanges();

    component.startEditing();
    expect(component.editing()).toBe(true);

    component.selectVersion('conc-1');
    httpMock.expectOne(versionUrl('conc-1')).flush(
      doneResponse({ id: 'conc-1', versionNumber: 1, lifecycleStatus: 'VALIDATED' }),
    );

    expect(component.editing()).toBe(false);
  });

  it('GET initial en erreur → section indisponible + snackbar', () => {
    fixture.detectChanges();
    // F-258 — le décompte part au montage (sans effet ici, N=0).
    httpMock.expectOne(VISIBILITY_URL).flush({ alwaysOn: [], contextual: [], catalog: [] });
    httpMock.expectOne(DASHBOARD_URL).flush({ caseFileId: CASE_ID, tiles: [] });
    // F-98 SF-98-56 — le décompte des citations adverses part aussi au montage.
    httpMock.expectOne(CHECKS_URL).flush({ checks: [] });
    // F-266 SF-266-01 — le GET des documents part aussi au montage.
    httpMock.expectOne(DOCUMENTS_URL).flush([]);
    httpMock
      .expectOne(GET_URL)
      .flush({}, { status: 500, statusText: 'Server Error' });
    // Pas de listVersions sur échec du GET initial.
    expect(component.unavailable()).toBe(true);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // ---------------------------------------------------------------------------
  // SF-98-50 — Export Word des conclusions
  // ---------------------------------------------------------------------------

  it('DONE → bouton « Télécharger en Word » visible', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const btn = fixture.nativeElement.querySelector(
      '[data-testid="download-word-btn"]',
    );
    expect(btn).not.toBeNull();
    expect(btn.textContent).toContain('Télécharger en Word');
  });

  it('NOT_GENERATED → bouton « Télécharger en Word » absent', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="download-word-btn"]'),
    ).toBeNull();
  });

  it('FAILED → bouton « Télécharger en Word » absent', () => {
    fixture.detectChanges();
    flushInitialLoad(response({ status: 'FAILED', errorMessage: 'Timeout.' }));
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="download-word-btn"]'),
    ).toBeNull();
  });

  // ── SF-266-03 — garde « éléments à compléter » avant export ──────────────
  describe('SF-266-03 — alerte placeholders avant export', () => {
    it('extractPlaceholders : détecte, déduplique, ignore liens md et renvois [1]', () => {
      const md = [
        "POUR : [Nom et qualité de l'avocat]",
        'Fait à [Lieu], le [Date].',
        'Voir [Légifrance](https://x) et la note [1].',
        "Signé : [Nom et qualité de l'avocat]",
        'Reste [à compléter].',
      ].join('\n');
      expect(extractPlaceholders(md)).toEqual([
        "[Nom et qualité de l'avocat]",
        '[Lieu]',
        '[Date]',
        '[à compléter]',
      ]);
      expect(extractPlaceholders('')).toEqual([]);
      expect(extractPlaceholders('Acte complet, sans crochet.')).toEqual([]);
    });

    it('bandeau rendu + placeholdersToFill quand l\'acte contient des placeholders', () => {
      fixture.detectChanges();
      flushInitialLoad(
        doneResponse({
          content: "POUR : [à compléter]\n\nSigné : [Nom et qualité de l'avocat]",
        }),
        versionList(),
      );
      fixture.detectChanges();

      expect(component.placeholdersToFill()).toEqual([
        '[à compléter]',
        "[Nom et qualité de l'avocat]",
      ]);
      const alert = fixture.nativeElement.querySelector(
        '[data-testid="placeholder-alert"]',
      );
      expect(alert).toBeTruthy();
      expect(alert.textContent).toContain('2 élément(s) à compléter');
    });

    it('aucun bandeau quand l\'acte est complet', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();
      expect(component.placeholdersToFill()).toEqual([]);
      expect(
        fixture.nativeElement.querySelector('[data-testid="placeholder-alert"]'),
      ).toBeNull();
    });

    it('downloadPdf avec placeholders → confirmation puis export si accepté', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse({ content: 'X [à compléter]' }), versionList());
      fixture.detectChanges();

      component.downloadPdf();
      expect(dialogSpy.open).toHaveBeenCalled();
      expect(pdfSpy.exportConclusion).toHaveBeenCalled();
    });

    it('downloadPdf → confirmation refusée → pas d\'export', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as never);
      fixture.detectChanges();
      flushInitialLoad(doneResponse({ content: 'X [à compléter]' }), versionList());
      fixture.detectChanges();

      component.downloadPdf();
      expect(dialogSpy.open).toHaveBeenCalled();
      expect(pdfSpy.exportConclusion).not.toHaveBeenCalled();
    });

    it('downloadWord sans placeholder → export direct, sans dialog', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();

      component.downloadWord();
      expect(dialogSpy.open).not.toHaveBeenCalled();
      expect(docxSpy.exportConclusion).toHaveBeenCalled();
    });
  });

  it('clic « Télécharger en Word » → appelle DocxExportService.exportConclusion', () => {
    component.caseTitle = 'Affaire Dupont';
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="download-word-btn"]',
    );
    btn.click();

    // F-281 — l'export contient le corps + le cartouche métadonnées de la
    // version (juridiction/stade/position de doneResponse()).
    const passed = docxSpy.exportConclusion.calls.mostRecent().args[0] as string;
    expect(passed).toContain('POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…');
    expect(passed).toContain("**Juridiction :** Conseil de prud'hommes");
    expect(docxSpy.exportConclusion.calls.mostRecent().args[1]).toBe('Affaire Dupont');
    expect(docxSpy.exportConclusion.calls.mostRecent().args[2]).toBe(2);
  });

  it('downloadWord → ignoré si la version n\'est pas DONE', () => {
    fixture.detectChanges();
    flushInitialLoad(response({ status: 'FAILED' }));
    fixture.detectChanges();

    component.downloadWord();
    expect(docxSpy.exportConclusion).not.toHaveBeenCalled();
  });

  it('downloadWord → échec de génération affiche une snackbar d\'erreur', () => {
    docxSpy.exportConclusion.and.throwError('boom');
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.downloadWord();

    expect(snackSpy.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du document Word.',
      'Fermer',
      jasmine.objectContaining({ panelClass: ['snack-error'] }),
    );
  });

  // ---------------------------------------------------------------------------
  // SF-98-51 — Export PDF des conclusions
  // ---------------------------------------------------------------------------

  it('DONE → bouton « Télécharger en PDF » visible', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const btn = fixture.nativeElement.querySelector(
      '[data-testid="download-pdf-btn"]',
    );
    expect(btn).not.toBeNull();
    expect(btn.textContent).toContain('Télécharger en PDF');
  });

  it('NOT_GENERATED → bouton « Télécharger en PDF » absent', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="download-pdf-btn"]'),
    ).toBeNull();
  });

  it('FAILED → bouton « Télécharger en PDF » absent', () => {
    fixture.detectChanges();
    flushInitialLoad(response({ status: 'FAILED', errorMessage: 'Timeout.' }));
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="download-pdf-btn"]'),
    ).toBeNull();
  });

  it('clic « Télécharger en PDF » → appelle PdfExportService.exportConclusion', () => {
    component.caseTitle = 'Affaire Dupont';
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="download-pdf-btn"]',
    );
    btn.click();

    // F-281 — corps + cartouche métadonnées de la version.
    const passed = pdfSpy.exportConclusion.calls.mostRecent().args[0] as string;
    expect(passed).toContain('POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…');
    expect(passed).toContain("**Juridiction :** Conseil de prud'hommes");
    expect(pdfSpy.exportConclusion.calls.mostRecent().args[1]).toBe('Affaire Dupont');
    expect(pdfSpy.exportConclusion.calls.mostRecent().args[2]).toBe(2);
  });

  it('downloadPdf → ignoré si la version n\'est pas DONE', () => {
    fixture.detectChanges();
    flushInitialLoad(response({ status: 'FAILED' }));
    fixture.detectChanges();

    component.downloadPdf();
    expect(pdfSpy.exportConclusion).not.toHaveBeenCalled();
  });

  it('downloadPdf → échec de génération affiche une snackbar d\'erreur', () => {
    pdfSpy.exportConclusion.and.throwError('boom');
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.downloadPdf();

    expect(snackSpy.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du document PDF.',
      'Fermer',
      jasmine.objectContaining({ panelClass: ['snack-error'] }),
    );
  });

  // ---------------------------------------------------------------------------
  // F-266 SF-266-02 — Export à en-tête du cabinet
  // ---------------------------------------------------------------------------

  it('en-tête vide + aucune métadonnée + signature off → export Word neutre (content inchangé, non-régression)', () => {
    fixture.detectChanges();
    // Métadonnées nulles ⇒ aucun cartouche ⇒ export strictement neutre (F-281).
    flushInitialLoad(
      doneResponse({ jurisdictionLabel: null, stageLabel: null, positionLabel: null }),
      versionList(),
    );
    fixture.detectChanges();

    component.downloadWord();

    expect(docxSpy.exportConclusion).toHaveBeenCalledWith(
      'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
      jasmine.any(String),
      2,
    );
  });

  it('en-tête saisi → export Word préfixé du bloc d\'en-tête (content stocké inchangé)', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.cabinetHeader.set('Cabinet Durand\n12 rue de la Loi');
    component.downloadWord();

    const passed = docxSpy.exportConclusion.calls.mostRecent().args[0] as string;
    expect(passed).toContain('# Cabinet Durand');
    expect(passed).toContain('12 rue de la Loi');
    expect(passed).toContain('---');
    expect(passed).toContain('POUR : M. X'); // le corps suit
    // Le content de la version n'est pas modifié.
    expect(component.conclusion()?.content).toBe(
      'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
    );
  });

  it('en-tête saisi → export PDF également préfixé', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    component.cabinetHeader.set('Cabinet X');
    component.downloadPdf();

    const passed = pdfSpy.exportConclusion.calls.mostRecent().args[0] as string;
    expect(passed).toContain('# Cabinet X');
  });

  it('toggle → affiche puis masque le champ d\'en-tête (opt-in)', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="cabinet-header-input"]'),
    ).toBeNull();

    fixture.nativeElement
      .querySelector('[data-testid="toggle-cabinet-header-btn"]')
      .click();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="cabinet-header-input"]'),
    ).not.toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-281 — Aperçu avant export + métadonnées + bloc signature
  // ---------------------------------------------------------------------------
  describe('F-281 — export : aperçu, métadonnées, signature', () => {
    it('métadonnées de la version réintégrées en tête de l\'export (Word)', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();

      component.downloadWord();

      const passed = docxSpy.exportConclusion.calls.mostRecent().args[0] as string;
      expect(passed).toContain("**Juridiction :** Conseil de prud'hommes");
      expect(passed).toContain('**Stade :** Bureau de jugement');
      expect(passed).toContain('**Position :** Demandeur');
      // Le content stocké de la version n'est jamais modifié.
      expect(component.conclusion()?.content).toBe(
        'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
      );
    });

    it('bloc signature activé → ajouté en pied de l\'export', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();

      component.signatureEnabled.set(true);
      component.signatureLieu.set('Paris');
      component.signatureDate.set('12/06/2026');
      component.downloadPdf();

      const passed = pdfSpy.exportConclusion.calls.mostRecent().args[0] as string;
      expect(passed).toContain('Fait à Paris, le 12/06/2026');
      expect(passed).toContain("[Nom et qualité de l'avocat]");
    });

    it('toggle bloc signature → pré-remplit lieu (juridiction) et date (aujourd\'hui)', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();

      component.toggleSignatureBlock();

      expect(component.showSignatureBlock()).toBe(true);
      expect(component.signatureLieu()).toBe("Conseil de prud'hommes");
      expect(component.signatureDate()).toMatch(/^\d{2}\/\d{2}\/\d{4}$/);
    });

    it('openExportPreview → ouvre la modale avec le contenu augmenté', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();

      component.signatureEnabled.set(true);
      component.signatureLieu.set('Lyon');
      component.signatureDate.set('01/07/2026');
      component.openExportPreview();

      expect(dialogSpy.open).toHaveBeenCalled();
      const passedData = dialogSpy.open.calls.mostRecent().args[1]?.data as {
        content: string;
      };
      expect(passedData.content).toContain("**Juridiction :** Conseil de prud'hommes");
      expect(passedData.content).toContain('POUR : M. X');
      expect(passedData.content).toContain('Fait à Lyon, le 01/07/2026');
    });

    it('openExportPreview → ignoré si la version n\'est pas DONE', () => {
      fixture.detectChanges();
      flushInitialLoad(response({ status: 'FAILED' }));
      fixture.detectChanges();

      component.openExportPreview();
      expect(dialogSpy.open).not.toHaveBeenCalled();
    });

    it('bouton « Aperçu avant export » visible quand DONE', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();

      const btn = fixture.nativeElement.querySelector(
        '[data-testid="export-preview-btn"]',
      );
      expect(btn).not.toBeNull();
      expect(btn.textContent).toContain('Aperçu avant export');
    });
  });

  // ---------------------------------------------------------------------------
  // SF-98-48 — découvrabilité du corpus de style (b1) + indicateur style (b2)
  // ---------------------------------------------------------------------------

  it('b1 — NOT_GENERATED → lien vers /workspace/style-learning affiché', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    const hint = fixture.nativeElement.querySelector(
      '[data-testid="style-corpus-link"]',
    );
    expect(hint).not.toBeNull();
    const link: HTMLAnchorElement = hint.querySelector('a');
    expect(link.getAttribute('href')).toBe('/workspace/style-learning');
  });

  it('b2 — version DONE avec styleApplied=true → indicateur « Généré dans votre style »', () => {
    fixture.detectChanges();
    flushInitialLoad(
      doneResponse({ styleApplied: true }),
      versionList(),
    );
    fixture.detectChanges();

    const indicator = fixture.nativeElement.querySelector(
      '[data-testid="style-applied-indicator"]',
    );
    expect(indicator).not.toBeNull();
    expect(indicator.textContent).toContain('Généré dans votre style');
  });

  it('b2 — styleApplied absent → aucun indicateur (dégradation propre)', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector(
        '[data-testid="style-applied-indicator"]',
      ),
    ).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // SF-98-53 — Bandeau « conclusions à régénérer »
  // ---------------------------------------------------------------------------

  it('CA4 — DONE + stale=true → bandeau de régénération affiché', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ stale: true }), versionList());
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector(
      '[data-testid="stale-banner"]',
    );
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain(
      'L\'analyse du dossier a évolué depuis la génération',
    );
    // Avertissement non bloquant — jamais rouge (cf. DESIGN_SYSTEM).
    expect(banner.className).not.toContain('error');
    expect(component.stale()).toBe(true);
  });

  it('CA4 — DONE + stale=false → bandeau absent', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ stale: false }), versionList());
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="stale-banner"]'),
    ).toBeNull();
    expect(component.stale()).toBe(false);
  });

  it('CA4 — DONE + stale absent → bandeau absent (dégradation propre)', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="stale-banner"]'),
    ).toBeNull();
    expect(component.stale()).toBe(false);
  });

  it('CA4 — version non DONE (FAILED) + stale=true → bandeau absent', () => {
    fixture.detectChanges();
    flushInitialLoad(
      response({ status: 'FAILED', errorMessage: 'Timeout.', stale: true }),
    );
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="stale-banner"]'),
    ).toBeNull();
  });

  it('CA5 — le bandeau met en avant l\'action « Régénérer » → POST generate', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse({ stale: true }), versionList());
    fixture.detectChanges();

    const regenBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="stale-regenerate-btn"]',
    );
    expect(regenBtn).not.toBeNull();
    expect(regenBtn.textContent).toContain('Régénérer');

    regenBtn.click();
    const postReq = httpMock.expectOne(GENERATE_URL);
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ status: 'PENDING', versionNumber: 3 });
    httpMock.expectOne(VERSIONS_URL).flush(versionList());

    expect(component.status()).toBe('PENDING');
  });

  // ---------------------------------------------------------------------------
  // F-258 SF-258-01 — Alerte « outils proposés non calculés »
  // ---------------------------------------------------------------------------

  describe('F-258 — alerte outils non calculés', () => {
    const WARNING = '[data-testid="missing-tools-warning"]';
    const VIEW_BTN = '[data-testid="view-tools-btn"]';

    it('N = (alwaysOn + contextual) − tiles ; le catalog est ignoré', () => {
      component.hasCompletedAnalysis = true;
      fixture.detectChanges();
      // proposés = A,B (alwaysOn) + C (contextual) ; calculés = A ; catalog = D (ignoré)
      flushInitialLoad(
        response(),
        [],
        { alwaysOn: ['A', 'B'], contextual: ['C'], catalog: ['D'] },
        ['A'],
      );
      fixture.detectChanges();

      // N = {B, C} = 2 (D du catalog jamais compté)
      expect(component.missingToolsCount()).toBe(2);
      const warning = fixture.nativeElement.querySelector(WARNING);
      expect(warning).not.toBeNull();
      expect(warning.textContent).toContain('2 outil');
    });

    it('tous les outils proposés sont calculés → N=0, aucun encart', () => {
      component.hasCompletedAnalysis = true;
      fixture.detectChanges();
      flushInitialLoad(
        response(),
        [],
        { alwaysOn: ['A'], contextual: ['B'], catalog: ['D'] },
        ['A', 'B'],
      );
      fixture.detectChanges();

      expect(component.missingToolsCount()).toBe(0);
      expect(fixture.nativeElement.querySelector(WARNING)).toBeNull();
    });

    it('le bouton « Générer » reste actif même avec N>0 (non bloquant)', () => {
      component.hasCompletedAnalysis = true;
      fixture.detectChanges();
      flushInitialLoad(
        response(),
        [],
        { alwaysOn: ['A'], contextual: [], catalog: [] },
        [],
      );
      fixture.detectChanges();

      expect(component.missingToolsCount()).toBe(1);
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
        '[data-testid="generate-btn"]',
      );
      expect(btn.disabled).toBe(false);
    });

    it('clic « Voir les outils à calculer » → émet viewToolsRequested', () => {
      component.hasCompletedAnalysis = true;
      const spy = jasmine.createSpy('viewTools');
      component.viewToolsRequested.subscribe(spy);
      fixture.detectChanges();
      flushInitialLoad(
        response(),
        [],
        { alwaysOn: ['A'], contextual: [], catalog: [] },
        [],
      );
      fixture.detectChanges();

      const viewBtn: HTMLButtonElement =
        fixture.nativeElement.querySelector(VIEW_BTN);
      expect(viewBtn).not.toBeNull();
      viewBtn.click();
      expect(spy).toHaveBeenCalledTimes(1);
    });

    it('échec d\'un des appels (dashboard) → N=0, pas d\'encart, pas de crash', () => {
      component.hasCompletedAnalysis = true;
      fixture.detectChanges();
      httpMock
        .expectOne(VISIBILITY_URL)
        .flush({ alwaysOn: ['A'], contextual: ['B'], catalog: [] });
      httpMock
        .expectOne(DASHBOARD_URL)
        .flush('boom', { status: 500, statusText: 'Server Error' });
      // F-98 SF-98-56 — le décompte des citations adverses part aussi au montage.
      httpMock.expectOne(CHECKS_URL).flush({ checks: [] });
      // F-266 SF-266-01 — le GET des documents part aussi au montage.
      httpMock.expectOne(DOCUMENTS_URL).flush([]);
      httpMock.expectOne(GET_URL).flush(response());
      httpMock.expectOne(VERSIONS_URL).flush([]);
      fixture.detectChanges();

      expect(component.missingToolsCount()).toBe(0);
      expect(fixture.nativeElement.querySelector(WARNING)).toBeNull();
    });
  });

  // ---------------------------------------------------------------------------
  // F-98 SF-98-56 — mention « N citations adverses marquées prises en compte »
  // ---------------------------------------------------------------------------

  describe('SF-98-56 — mention citations adverses marquées', () => {
    const ADVERSE_NOTE = '[data-testid="adverse-marked-note"]';

    function checkStub(over: Record<string, unknown>): Record<string, unknown> {
      return {
        id: over['id'] ?? 'c-' + Math.random(),
        documentName: 'conclusions_adverses.pdf',
        reference: 'Cass. soc. n° 99-99.999',
        statut: over['statut'] ?? 'SUSPECT',
        explication: null,
        positionAlleguee: null,
        sourceUrl: null,
        claudeConfidence: null,
        webSearchUsed: false,
        markedAdverse: over['markedAdverse'] ?? false,
      };
    }

    it('DONE + N>0 → mention factuelle affichée avec le bon nombre', () => {
      fixture.detectChanges();
      flushInitialLoad(
        doneResponse(),
        versionList(),
        { alwaysOn: [], contextual: [], catalog: [] },
        [],
        [
          checkStub({ id: '1', statut: 'SUSPECT', markedAdverse: true }),
          checkStub({ id: '2', statut: 'NOT_FOUND', markedAdverse: true }),
          // marquée mais statut non éligible → exclue.
          checkStub({ id: '3', statut: 'VERIFIED', markedAdverse: true }),
          // éligible mais non marquée → exclue.
          checkStub({ id: '4', statut: 'SUSPECT', markedAdverse: false }),
        ],
      );
      fixture.detectChanges();

      expect(component.adverseMarkedCount()).toBe(2);
      const note = fixture.nativeElement.querySelector(ADVERSE_NOTE);
      expect(note).not.toBeNull();
      expect(note.textContent).toContain('2 citations adverses marquées prises');
    });

    it('DONE + N=0 → aucune mention (pas de rubrique vide)', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList(), undefined, [], []);
      fixture.detectChanges();

      expect(component.adverseMarkedCount()).toBe(0);
      expect(fixture.nativeElement.querySelector(ADVERSE_NOTE)).toBeNull();
    });

    it('singulier : N=1 → « citation adverse marquée prise »', () => {
      fixture.detectChanges();
      flushInitialLoad(
        doneResponse(),
        versionList(),
        undefined,
        [],
        [checkStub({ id: '1', statut: 'SUSPECT', markedAdverse: true })],
      );
      fixture.detectChanges();

      const note = fixture.nativeElement.querySelector(ADVERSE_NOTE);
      expect(note).not.toBeNull();
      expect(note.textContent).toContain('1 citation adverse marquée prise');
    });
  });

  // ---------------------------------------------------------------------------
  // F-264 SF-264-01 — Éditeur markdown enrichi + aperçu live
  // ---------------------------------------------------------------------------

  describe('SF-264-01 — barre d\'outils markdown + aperçu live', () => {
    /** Entre en mode édition avec un contenu connu et un select positionné. */
    function enterEditingWith(content: string, start = 0, end = 0): void {
      fixture.detectChanges();
      flushInitialLoad(
        doneResponse({ lifecycleStatus: 'DRAFT', content }),
        versionList(),
      );
      fixture.detectChanges();
      component.startEditing();
      fixture.detectChanges();
      const editor: HTMLTextAreaElement = fixture.nativeElement.querySelector(
        '[data-testid="conclusions-editor"]',
      );
      editor.setSelectionRange(start, end);
    }

    it('mode édition → barre d\'outils, éditeur et aperçu rendus', () => {
      enterEditingWith('Texte initial');
      expect(
        fixture.nativeElement.querySelector('[data-testid="markdown-toolbar"]'),
      ).not.toBeNull();
      expect(
        fixture.nativeElement.querySelector('[data-testid="editor-pane"]'),
      ).not.toBeNull();
      expect(
        fixture.nativeElement.querySelector('[data-testid="preview-pane"]'),
      ).not.toBeNull();
    });

    it('Gras → enveloppe la sélection de **…**', () => {
      // « Bonjour » : sélection sur « jour » (index 3..7).
      enterEditingWith('Bonjour', 3, 7);
      component.applyMarkdown('bold');
      expect(component.draftContent()).toBe('Bon**jour**');
    });

    it('Italique → enveloppe la sélection de *…*', () => {
      enterEditingWith('Bonjour', 0, 7);
      component.applyMarkdown('italic');
      expect(component.draftContent()).toBe('*Bonjour*');
    });

    it('Gras sans sélection → insère **** au curseur', () => {
      enterEditingWith('AB', 1, 1);
      component.applyMarkdown('bold');
      expect(component.draftContent()).toBe('A****B');
    });

    it('Titre H2 → préfixe la ligne courante de « ## »', () => {
      enterEditingWith('Mon titre', 2, 2);
      component.applyMarkdown('h2');
      expect(component.draftContent()).toBe('## Mon titre');
    });

    it('Sous-titre H3 → préfixe la ligne courante de « ### »', () => {
      enterEditingWith('Section', 0, 0);
      component.applyMarkdown('h3');
      expect(component.draftContent()).toBe('### Section');
    });

    it('Liste → préfixe chaque ligne sélectionnée de « - »', () => {
      // 2 lignes ; sélection couvrant les deux.
      enterEditingWith('un\ndeux', 0, 6);
      component.applyMarkdown('list');
      expect(component.draftContent()).toBe('- un\n- deux');
    });

    it('Citation → préfixe la ligne de « > »', () => {
      enterEditingWith('Une citation', 4, 4);
      component.applyMarkdown('quote');
      expect(component.draftContent()).toBe('> Une citation');
    });

    it('barre d\'outils (clic réel) → modifie le brouillon', () => {
      enterEditingWith('Titre', 0, 0);
      const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
        '[data-testid="md-h2"]',
      );
      btn.click();
      expect(component.draftContent()).toBe('## Titre');
    });

    it('aperçu reflète le brouillon courant', () => {
      enterEditingWith('Départ');
      component.onDraftInput('## Nouveau titre');
      fixture.detectChanges();
      const preview = fixture.nativeElement.querySelector(
        '[data-testid="preview-pane"]',
      );
      // Le ConclusionDocumentComponent rend le markdown → un <h2> dans l'aperçu.
      expect(preview.querySelector('h2')?.textContent).toContain(
        'Nouveau titre',
      );
    });

    it('SF-267-02 : les deux panneaux portent leur libellé (Édition / Aperçu)', () => {
      enterEditingWith('Texte');
      const labels = Array.from(
        fixture.nativeElement.querySelectorAll('.cs-preview-label'),
      ).map((el) => (el as HTMLElement).textContent?.trim());
      expect(labels).toEqual(expect.arrayContaining(['Édition', 'Aperçu']));
    });

    it('bascule éditeur/aperçu → met à jour editorView + classe', () => {
      enterEditingWith('Texte');
      expect(component.editorView()).toBe('edit');

      const previewBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
        '[data-testid="view-preview-btn"]',
      );
      previewBtn.click();
      fixture.detectChanges();
      expect(component.editorView()).toBe('preview');
      expect(
        fixture.nativeElement
          .querySelector('.cs-edit-split')
          .classList.contains('cs-edit-split--show-preview'),
      ).toBe(true);

      const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
        '[data-testid="view-edit-btn"]',
      );
      editBtn.click();
      fixture.detectChanges();
      expect(component.editorView()).toBe('edit');
    });

    it('« Enregistrer » → PATCH /content avec le markdown inchangé (non-régression)', () => {
      enterEditingWith('Contenu de départ');
      // L'avocat applique un gras puis enregistre : le content reste markdown.
      component.onDraftInput('Texte **gras** edité');
      fixture.detectChanges();

      const saveBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
        '[data-testid="save-btn"]',
      );
      saveBtn.click();

      const req = httpMock.expectOne(contentUrl('conc-2'));
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ content: 'Texte **gras** edité' });
      req.flush(
        doneResponse({
          lifecycleStatus: 'DRAFT',
          content: 'Texte **gras** edité',
        }),
      );
      httpMock.expectOne(VERSIONS_URL).flush(versionList());
      fixture.detectChanges();

      expect(component.editing()).toBe(false);
    });
  });

  // -------------------------------------------------------------------------
  // F-279 / SF-279-01 — feedback de sauvegarde explicite + autosave local
  // -------------------------------------------------------------------------
  describe('F-279 — feedback save + autosave brouillon', () => {
    /** Met le composant en mode édition d'une version DONE + DRAFT. */
    function enterEdit(): void {
      fixture.detectChanges();
      flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
      fixture.detectChanges();
      component.startEditing();
      fixture.detectChanges();
    }

    beforeEach(() => localStorage.clear());
    afterEach(() => localStorage.clear());

    it('édition + frappe → état « Modifié » (dirty)', () => {
      enterEdit();
      expect(component.dirty()).toBe(false);
      component.onDraftInput('Texte modifié par l\'avocat.');
      fixture.detectChanges();
      expect(component.dirty()).toBe(true);
      const chip = fixture.nativeElement.querySelector(
        '[data-testid="save-state-indicator"]',
      );
      expect(chip).not.toBeNull();
      expect(chip.textContent).toContain('Modifié');
    });

    it('« Enregistrer » réussi → snackbar succès + dirty=false + brouillon purgé', fakeAsync(() => {
      enterEdit();
      component.onDraftInput('Nouveau texte.');
      tick(800); // autosave local
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).not.toBeNull();

      component.saveContent();
      const req = httpMock.expectOne(contentUrl('conc-2'));
      req.flush(doneResponse({ lifecycleStatus: 'DRAFT', content: 'Nouveau texte.' }));
      httpMock.expectOne(VERSIONS_URL).flush(versionList());

      expect(snackSpy.open).toHaveBeenCalledWith(
        'Modifications enregistrées.',
        'Fermer',
        jasmine.objectContaining({ panelClass: ['snack-success'] }),
      );
      // Brouillon local purgé après enregistrement serveur.
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).toBeNull();
    }));

    it('frappe → écrit le brouillon dans localStorage après debounce', fakeAsync(() => {
      enterEdit();
      component.onDraftInput('Brouillon en cours…');
      // Avant le debounce : rien d'écrit encore.
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).toBeNull();
      tick(800);
      const raw = localStorage.getItem('lc.conclusions.draft.case-1.conc-2');
      expect(raw).not.toBeNull();
      expect(JSON.parse(raw!).content).toBe('Brouillon en cours…');
    }));

    it('brouillon revenu identique au serveur → entrée locale supprimée', fakeAsync(() => {
      enterEdit();
      const serverContent = component.savedContent();
      component.onDraftInput('Divergent');
      tick(800);
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).not.toBeNull();
      // Retour au contenu serveur → l'autosave purge l'entrée (pas de faux brouillon).
      component.onDraftInput(serverContent);
      tick(800);
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).toBeNull();
    }));

    it('réouverture avec brouillon divergent → bandeau « Brouillon récupéré »', () => {
      // Pré-positionne un brouillon local divergent du contenu serveur.
      localStorage.setItem(
        'lc.conclusions.draft.case-1.conc-2',
        JSON.stringify({
          versionId: 'conc-2',
          content: 'Travail récupéré après crash',
          savedAt: '2026-06-12T10:00:00Z',
        }),
      );
      enterEdit();
      expect(component.draftRecovered()).toBe(true);
      const banner = fixture.nativeElement.querySelector(
        '[data-testid="draft-recovery-banner"]',
      );
      expect(banner).not.toBeNull();
    });

    it('« Restaurer » → charge le brouillon local ; « Ignorer » → garde le serveur + purge', () => {
      localStorage.setItem(
        'lc.conclusions.draft.case-1.conc-2',
        JSON.stringify({
          versionId: 'conc-2',
          content: 'Travail récupéré',
          savedAt: '2026-06-12T10:00:00Z',
        }),
      );
      enterEdit();
      component.restoreDraft();
      expect(component.draftContent()).toBe('Travail récupéré');
      expect(component.draftRecovered()).toBe(false);

      // Nouveau cycle : on sort puis on repositionne un brouillon, et discard
      // doit garder le serveur et purger. (cancelEditing purge le local, on
      // réécrit donc l'entrée APRÈS être sorti d'édition.)
      component.cancelEditing();
      localStorage.setItem(
        'lc.conclusions.draft.case-1.conc-2',
        JSON.stringify({
          versionId: 'conc-2',
          content: 'Autre brouillon',
          savedAt: '2026-06-12T11:00:00Z',
        }),
      );
      component.startEditing();
      expect(component.draftRecovered()).toBe(true);
      component.discardDraft();
      expect(component.draftContent()).toBe(component.savedContent());
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).toBeNull();
    });

    it('« Annuler » → purge le brouillon local', fakeAsync(() => {
      enterEdit();
      component.onDraftInput('Jetable');
      tick(800);
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).not.toBeNull();
      component.cancelEditing();
      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).toBeNull();
    }));

    it('passage en VALIDATED → purge le brouillon local', () => {
      // Pré-positionne un brouillon, puis valide la version.
      localStorage.setItem(
        'lc.conclusions.draft.case-1.conc-2',
        JSON.stringify({ versionId: 'conc-2', content: 'x', savedAt: '' }),
      );
      fixture.detectChanges();
      flushInitialLoad(doneResponse({ lifecycleStatus: 'DRAFT' }), versionList());
      fixture.detectChanges();

      component.changeLifecycle('VALIDATED');
      httpMock
        .expectOne(lifecycleUrl('conc-2'))
        .flush(doneResponse({ lifecycleStatus: 'VALIDATED' }));
      httpMock.expectOne(VERSIONS_URL).flush(versionList());

      expect(localStorage.getItem('lc.conclusions.draft.case-1.conc-2')).toBeNull();
    });
  });

  // ---------------------------------------------------------------------------
  // F-280 / SF-280-01 — Comparaison de versions (diff)
  // ---------------------------------------------------------------------------

  describe('F-280 — comparaison de versions (diff)', () => {
    function loadWithVersions(): void {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), versionList());
      fixture.detectChanges();
    }

    it('canCompare faux avec une seule version, vrai avec deux', () => {
      fixture.detectChanges();
      flushInitialLoad(doneResponse(), [versionSummary({ id: 'conc-2', versionNumber: 2 })]);
      fixture.detectChanges();
      expect(component.canCompare()).toBe(false);
      const btn = fixture.nativeElement.querySelector(
        '[data-testid="compare-versions-btn"]',
      );
      expect(btn).toBeNull();
    });

    it('bouton « Comparer » visible avec ≥ 2 versions', () => {
      loadWithVersions();
      expect(component.canCompare()).toBe(true);
      const btn = fixture.nativeElement.querySelector(
        '[data-testid="compare-versions-btn"]',
      );
      expect(btn).not.toBeNull();
    });

    it('openCompare charge base=précédente, cible=sélectionnée et masque l\'acte', () => {
      loadWithVersions();
      // selectedVersionId = conc-2 (la plus récente).
      component.openCompare();
      // forkJoin → deux GET (base conc-1, cible conc-2).
      httpMock
        .expectOne(versionUrl('conc-1'))
        .flush(doneResponse({ id: 'conc-1', versionNumber: 1, content: 'a\nb' }));
      httpMock
        .expectOne(versionUrl('conc-2'))
        .flush(doneResponse({ id: 'conc-2', versionNumber: 2, content: 'a\nb\nc' }));
      fixture.detectChanges();

      expect(component.comparing()).toBe(true);
      expect(component.diffBaseId()).toBe('conc-1');
      expect(component.diffTargetId()).toBe('conc-2');
      const panel = fixture.nativeElement.querySelector('[data-testid="diff-panel"]');
      expect(panel).not.toBeNull();
      // ligne ajoutée « c » présente, ligne supprimée absente (que des ajouts).
      const added = fixture.nativeElement.querySelectorAll(
        '[data-testid="diff-line-added"]',
      );
      expect(added.length).toBe(1);
      const summary = fixture.nativeElement.querySelector(
        '[data-testid="diff-summary"]',
      );
      expect(summary.textContent).toContain('1 ajout');
    });

    it('changer la borne base recalcule le diff', () => {
      loadWithVersions();
      component.openCompare();
      httpMock.expectOne(versionUrl('conc-1')).flush(doneResponse({ id: 'conc-1', content: 'x' }));
      httpMock.expectOne(versionUrl('conc-2')).flush(doneResponse({ id: 'conc-2', content: 'y' }));
      fixture.detectChanges();

      // Re-choisir conc-1 comme cible → forkJoin recharge les deux bornes
      // (base conc-1, cible conc-1) ⇒ deux GET vers la même URL.
      component.setDiffTarget('conc-1');
      const reqs = httpMock.match(versionUrl('conc-1'));
      expect(reqs.length).toBe(2);
      reqs.forEach((r) => r.flush(doneResponse({ id: 'conc-1', content: 'x' })));
      fixture.detectChanges();
      expect(component.diffTargetId()).toBe('conc-1');
      expect(component.diffSummary()).toEqual({ added: 0, removed: 0 });
    });

    it('closeCompare restaure l\'acte sans changer la version sélectionnée', () => {
      loadWithVersions();
      const selectedBefore = component.selectedVersionId();
      component.openCompare();
      httpMock.expectOne(versionUrl('conc-1')).flush(doneResponse({ id: 'conc-1', content: 'a' }));
      httpMock.expectOne(versionUrl('conc-2')).flush(doneResponse({ id: 'conc-2', content: 'b' }));
      fixture.detectChanges();

      component.closeCompare();
      fixture.detectChanges();
      expect(component.comparing()).toBe(false);
      expect(component.selectedVersionId()).toBe(selectedBefore);
      const panel = fixture.nativeElement.querySelector('[data-testid="diff-panel"]');
      expect(panel).toBeNull();
      const doc = fixture.nativeElement.querySelector('app-conclusion-document');
      expect(doc).not.toBeNull();
    });

    it('échec de chargement affiche un message sans casser le panneau', () => {
      loadWithVersions();
      component.openCompare();
      // forkJoin : la première erreur fait échouer l'observable ; la seconde
      // requête est annulée (on ne flush donc qu'une seule des deux).
      httpMock
        .expectOne(versionUrl('conc-1'))
        .flush('boom', { status: 500, statusText: 'Server Error' });
      const pending = httpMock.match(versionUrl('conc-2'));
      pending.forEach((r) => {
        if (!r.cancelled) {
          r.flush('boom', { status: 500, statusText: 'Server Error' });
        }
      });
      fixture.detectChanges();
      expect(component.diffError()).not.toBeNull();
      const err = fixture.nativeElement.querySelector('[data-testid="diff-error"]');
      expect(err).not.toBeNull();
      // le panneau reste ouvert (bouton Fermer accessible).
      expect(component.comparing()).toBe(true);
    });
  });
});

// ── F-279 / SF-279-01 — stockage local du brouillon (helpers purs) ──────────
describe('F-279 — conclusion-draft-storage', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => localStorage.clear());

  it('clé déterministe par (dossier, version)', () => {
    expect(draftStorageKey('c1', 'v2')).toBe('lc.conclusions.draft.c1.v2');
  });

  it('write → read round-trip', () => {
    writeDraft('c1', 'v2', 'Mon brouillon');
    const rec = readDraft('c1', 'v2');
    expect(rec).not.toBeNull();
    expect(rec!.content).toBe('Mon brouillon');
    expect(rec!.versionId).toBe('v2');
  });

  it('clearDraft supprime l\'entrée', () => {
    writeDraft('c1', 'v2', 'x');
    clearDraft('c1', 'v2');
    expect(readDraft('c1', 'v2')).toBeNull();
  });

  it('JSON invalide → readDraft retourne null et purge', () => {
    localStorage.setItem('lc.conclusions.draft.c1.v2', '{pasdujson');
    expect(readDraft('c1', 'v2')).toBeNull();
    expect(localStorage.getItem('lc.conclusions.draft.c1.v2')).toBeNull();
  });

  it('forme inattendue → readDraft retourne null', () => {
    localStorage.setItem('lc.conclusions.draft.c1.v2', JSON.stringify({ foo: 1 }));
    expect(readDraft('c1', 'v2')).toBeNull();
  });

  it('localStorage.setItem qui jette → writeDraft no-op (pas d\'exception)', () => {
    const orig = localStorage.setItem;
    localStorage.setItem = () => {
      throw new DOMException('QuotaExceededError');
    };
    try {
      expect(() => writeDraft('c1', 'v2', 'x')).not.toThrow();
    } finally {
      localStorage.setItem = orig;
    }
  });

  it('localStorage.getItem qui jette → readDraft retourne null (pas d\'exception)', () => {
    const orig = localStorage.getItem;
    localStorage.getItem = () => {
      throw new DOMException('SecurityError');
    };
    try {
      expect(readDraft('c1', 'v2')).toBeNull();
    } finally {
      localStorage.getItem = orig;
    }
  });
});

// ── F-265 / SF-265-02 — parsing & remplacement de section (helpers purs) ────
describe('F-265 — parseMarkdownSections', () => {
  it('détecte les sections ## et ### dans l\'ordre du document', () => {
    const md = [
      'Préambule sans titre.',
      '',
      '## Sur la prescription',
      'Corps prescription.',
      '',
      '## Sur le licenciement',
      '### Validité',
      'Corps validité.',
      '',
      '## Dispositif',
      'PAR CES MOTIFS.',
    ].join('\n');

    const sections = parseMarkdownSections(md);

    expect(sections.map((s) => s.title)).toEqual([
      'Sur la prescription',
      'Sur le licenciement',
      'Validité',
      'Dispositif',
    ]);
  });

  it('rattache un ### enfant à la SECTION ## parente (fin au prochain titre de niveau ≤)', () => {
    const md = [
      '## Sur le licenciement',
      'Intro.',
      '### Validité',
      'Détail.',
      '## Dispositif',
      'Fin.',
    ].join('\n');

    const sections = parseMarkdownSections(md);
    const parent = sections.find((s) => s.title === 'Sur le licenciement')!;

    // La section ## couvre jusqu'au prochain ## (le ### enfant reste dedans).
    expect(parent.markdown).toContain('### Validité');
    expect(parent.markdown).not.toContain('## Dispositif');
  });

  it('retourne une liste vide quand aucun titre ##/###', () => {
    expect(parseMarkdownSections('Juste du texte sans titre.')).toEqual([]);
    expect(parseMarkdownSections('')).toEqual([]);
  });

  it('ignore les titres # de niveau 1 (cible moyens ##/###)', () => {
    const sections = parseMarkdownSections('# Titre acte\nText\n## Moyen\nCorps');
    expect(sections.map((s) => s.title)).toEqual(['Moyen']);
  });
});

describe('F-265 — replaceSectionInDraft', () => {
  it('remplace EN PLACE le bloc, le reste byte-identique', () => {
    const draft = '## A\nun\n\n## B\ndeux\n\n## C\ntrois';
    const original = '## B\ndeux';
    const result = replaceSectionInDraft(draft, original, '## B\nDEUX renforcé');

    expect(result).toBe('## A\nun\n\n## B\nDEUX renforcé\n\n## C\ntrois');
  });

  it('retourne null si le bloc original n\'est plus retrouvé (édition manuelle)', () => {
    const result = replaceSectionInDraft('## A\nun', '## Z\ninconnu', 'x');
    expect(result).toBeNull();
  });

  it('round-trip markdown : un seul bloc change', () => {
    const sections = parseMarkdownSections('## A\nun\n## B\ndeux');
    const b = sections.find((s) => s.title === 'B')!;
    const result = replaceSectionInDraft('## A\nun\n## B\ndeux', b.markdown, '## B\nnouveau');
    expect(result).toContain('## A\nun');
    expect(result).toContain('## B\nnouveau');
    expect(result).not.toContain('deux');
  });
});

describe('F-266 SF-266-02 — buildExportContent', () => {
  const BODY = 'POUR : M. X\n\nFAITS\nLe salarié…';

  it('en-tête vide → content inchangé (export neutre)', () => {
    expect(buildExportContent('', BODY)).toBe(BODY);
    expect(buildExportContent('   \n  ', BODY)).toBe(BODY);
  });

  it('en-tête une ligne → titre markdown + filet + corps', () => {
    const out = buildExportContent('Cabinet Durand', BODY);
    expect(out).toBe(`# Cabinet Durand\n\n---\n\n${BODY}`);
  });

  it('en-tête multi-lignes → 1ʳᵉ ligne titre, suivantes paragraphes', () => {
    const out = buildExportContent('Cabinet Durand\n12 rue de la Loi\nBarreau de Paris', BODY);
    expect(out).toContain('# Cabinet Durand');
    expect(out).toContain('12 rue de la Loi');
    expect(out).toContain('Barreau de Paris');
    expect(out).toContain('---');
    expect(out.endsWith(BODY)).toBe(true);
  });

  it('caractères markdown de l\'en-tête échappés (traités comme texte)', () => {
    const out = buildExportContent('Cabinet *Durand* #1', BODY);
    // Les `*` et `#` du libellé sont échappés (ne créent pas de gras/titre).
    expect(out).toContain('\\*Durand\\*');
    expect(out).toContain('\\#1');
  });

  it('ne modifie jamais le corps fourni', () => {
    const out = buildExportContent('X', BODY);
    expect(out.endsWith(BODY)).toBe(true);
  });
});
