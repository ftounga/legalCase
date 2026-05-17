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
import { ConclusionResponse } from '../../core/models/conclusion.model';

describe('ConclusionsSectionComponent', () => {
  let component: ConclusionsSectionComponent;
  let fixture: ComponentFixture<ConclusionsSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const CASE_ID = 'case-1';
  const GET_URL = `/api/v1/case-files/${CASE_ID}/conclusions`;
  const GENERATE_URL = `/api/v1/case-files/${CASE_ID}/conclusions/generate`;

  function response(
    overrides: Partial<ConclusionResponse> = {},
  ): ConclusionResponse {
    return {
      id: null,
      caseFileId: CASE_ID,
      status: 'NOT_GENERATED',
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

  function doneResponse(): ConclusionResponse {
    return response({
      id: 'conc-1',
      status: 'DONE',
      content: 'POUR : M. X\n\nFAITS ET PROCÉDURE\nLe salarié…',
      jurisdictionLabel: 'Conseil de prud\'hommes',
      stageLabel: 'Bureau de jugement (fond)',
      positionLabel: 'Demandeur (salarié)',
      modelUsed: 'claude-sonnet-4-6',
      generatedAt: '2026-05-18T10:00:00Z',
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        ConclusionsSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ConclusionsSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_ID;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Montage + GET initial
  // ---------------------------------------------------------------------------

  it('montage → GET /conclusions déclenché', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(GET_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    expect(component.loading()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // NOT_GENERATED — bouton « Générer »
  // ---------------------------------------------------------------------------

  it('NOT_GENERATED + pré-requis OK → bouton « Générer » actif visible', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    httpMock.expectOne(GET_URL).flush(response());
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
    httpMock.expectOne(GET_URL).flush(response());
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
    httpMock.expectOne(GET_URL).flush(response());
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
    httpMock.expectOne(GET_URL).flush(doneResponse());
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
    httpMock.expectOne(GET_URL).flush(doneResponse());
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
  // Génération + polling
  // ---------------------------------------------------------------------------

  it('clic « Générer » → POST puis polling jusqu\'à DONE', fakeAsync(() => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    httpMock.expectOne(GET_URL).flush(response());
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="generate-btn"]',
    );
    btn.click();

    const postReq = httpMock.expectOne(GENERATE_URL);
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ status: 'PENDING' });

    expect(component.status()).toBe('PENDING');

    // 1er tour de polling (3 s) → toujours PROCESSING
    tick(3000);
    httpMock.expectOne(GET_URL).flush(response({ status: 'PROCESSING' }));
    expect(component.status()).toBe('PROCESSING');

    // 2e tour de polling → DONE → le polling s'arrête
    tick(3000);
    httpMock.expectOne(GET_URL).flush(doneResponse());
    expect(component.status()).toBe('DONE');

    // Plus aucun appel après DONE.
    tick(3000);
    httpMock.expectNone(GET_URL);

    component.ngOnDestroy();
  }));

  it('erreur 409 → message backend affiché via snackbar', () => {
    component.hasCompletedAnalysis = true;
    fixture.detectChanges();
    httpMock.expectOne(GET_URL).flush(response());

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
    httpMock
      .expectOne(GET_URL)
      .flush(response({ status: 'FAILED', errorMessage: 'Timeout IA.' }));
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
    httpMock.expectOne(GET_URL).flush(response({ status: 'PROCESSING' }));
    fixture.detectChanges();

    const indicator = fixture.nativeElement.querySelector(
      '[data-testid="generating-indicator"]',
    );
    expect(indicator).not.toBeNull();

    tick(3000);
    httpMock.expectOne(GET_URL).flush(doneResponse());
    expect(component.status()).toBe('DONE');

    component.ngOnDestroy();
  }));

  it('GET initial en erreur → section indisponible + snackbar', () => {
    fixture.detectChanges();
    httpMock
      .expectOne(GET_URL)
      .flush({}, { status: 500, statusText: 'Server Error' });
    expect(component.unavailable()).toBe(true);
    expect(snackSpy.open).toHaveBeenCalled();
  });
});
