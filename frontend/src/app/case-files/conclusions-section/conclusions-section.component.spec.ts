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
import { provideRouter } from '@angular/router';
import {
  ConclusionsSectionComponent,
  parseMarkdownSections,
  replaceSectionInDraft,
  buildExportContent,
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

  it('clic « Télécharger en Word » → appelle DocxExportService.exportConclusion', () => {
    component.caseTitle = 'Affaire Dupont';
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="download-word-btn"]',
    );
    btn.click();

    expect(docxSpy.exportConclusion).toHaveBeenCalledWith(
      'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
      'Affaire Dupont',
      2,
    );
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

    expect(pdfSpy.exportConclusion).toHaveBeenCalledWith(
      'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
      'Affaire Dupont',
      2,
    );
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

  it('en-tête vide → export Word neutre (content inchangé, non-régression)', () => {
    fixture.detectChanges();
    flushInitialLoad(doneResponse(), versionList());
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
