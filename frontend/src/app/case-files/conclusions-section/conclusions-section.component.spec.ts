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
import { ConclusionsSectionComponent } from './conclusions-section.component';
import { DocxExportService } from '../../core/services/docx-export.service';
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

  const CASE_ID = 'case-1';
  const GET_URL = `/api/v1/case-files/${CASE_ID}/conclusions`;
  const GENERATE_URL = `/api/v1/case-files/${CASE_ID}/conclusions/generate`;
  const VERSIONS_URL = `/api/v1/case-files/${CASE_ID}/conclusions/versions`;
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
    await TestBed.configureTestingModule({
      imports: [
        ConclusionsSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: DocxExportService, useValue: docxSpy },
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
   */
  function flushInitialLoad(
    conclusion: ConclusionResponse = response(),
    versions: ConclusionVersionSummary[] = [],
  ): void {
    httpMock.expectOne(GET_URL).flush(conclusion);
    httpMock.expectOne(VERSIONS_URL).flush(versions);
  }

  // ---------------------------------------------------------------------------
  // Montage + GET initial
  // ---------------------------------------------------------------------------

  it('montage → GET /conclusions + GET /versions déclenchés', () => {
    fixture.detectChanges();
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
    expect(component.status()).toBe('DONE');

    // Plus aucun appel après DONE.
    tick(3000);
    httpMock.expectNone(GET_URL);

    component.ngOnDestroy();
  }));

  it('erreur 409 → message backend affiché via snackbar', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    flushInitialLoad();

    component.generate();
    httpMock.expectOne(GENERATE_URL).flush(
      { error: 'STAGE_NOT_SET', message: 'Renseignez le stade procédural.' },
      { status: 409, statusText: 'Conflict' },
    );

    expect(snackSpy.open).toHaveBeenCalledWith(
      'Renseignez le stade procédural.',
      'Fermer',
      jasmine.objectContaining({ panelClass: ['snack-error'] }),
    );
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
    // En édition, le bloc lecture seule disparaît.
    expect(
      fixture.nativeElement.querySelector('[data-testid="conclusions-content"]'),
    ).toBeNull();
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
});
