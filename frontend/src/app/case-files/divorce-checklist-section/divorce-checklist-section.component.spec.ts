import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { DivorceChecklistSectionComponent } from './divorce-checklist-section.component';

describe('DivorceChecklistSectionComponent', () => {
  let component: DivorceChecklistSectionComponent;
  let fixture: ComponentFixture<DivorceChecklistSectionComponent>;
  let httpMock: HttpTestingController;
  const ID = '99999999-9999-9999-9999-999999999999';
  const URL = `/api/v1/case-files/${ID}/divorce-checklist`;
  const MOCK = { caseFileId: ID, country: 'FRANCE',
    etapes: [{ code: 'FR_CHOIX_AVOCATS', label: 'Choix avocats', ordre: 1, description: 'Desc', delai: '—', obligatoire: true, statut: 'A_FAIRE' }],
    pieces: [{ code: 'FR_ACTE_MARIAGE', label: 'Acte mariage', description: 'Desc', obligatoire: true, statut: 'MANQUANTE' }],
    etapesCompletees: 0, etapesTotal: 1, piecesPresentes: 0, piecesTotal: 1, baseJuridique: 'Art 229' };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DivorceChecklistSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DivorceChecklistSectionComponent);
    component = fixture.componentInstance; component.caseFileId = ID;
  });
  afterEach(() => { httpMock.verify(); });

  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show init when no existing', () => { initNo(); expect(component.result()).toBeNull(); });
  it('should display existing', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.progress()).toBe(0); });
  it('should toggle etape and save', () => {
    initWith();
    component.toggleEtape(component.result()!.etapes[0]);
    const req = httpMock.expectOne(URL); expect(req.request.method).toBe('POST');
    req.flush({ ...MOCK, etapesCompletees: 1, etapes: [{ ...MOCK.etapes[0], statut: 'FAIT' }] });
    expect(component.result()!.etapesCompletees).toBe(1);
  });

  // ---- Coherence alerts (SF-IA-03-06) ----

  function f96(statut: string, critereCode: string, raison: string | null = null) {
    return { id: 'c', ordre: 0, description: 'point', statut, raison, critereCode, expectedValue: null } as any;
  }
  function question(answerText: string, critereCode: string, expectedValue: string | null = null) {
    return { id: 'q', orderIndex: 0, questionText: 'Q?', answerText, critereCode, expectedValue } as any;
  }
  function piece(critereCode: string, texte: string = 'pièce') {
    return { texte, critereCode };
  }
  function withResult(etapes: any[], pieces: any[] = []) {
    const resp = {
      ...MOCK,
      etapes: etapes.length ? etapes : MOCK.etapes,
      pieces: pieces.length ? pieces : MOCK.pieces,
    };
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(resp);
  }

  // ÉTAPES
  it('should alert blocker on step FAIT + F-96 NON_COMPLIANT', () => {
    component.procedureChecks = [f96('NON_COMPLIANT', 'FR_CHOIX_AVOCATS', 'Pas fait')];
    withResult([{ ...MOCK.etapes[0], statut: 'FAIT' }]);
    const alert = component.coherenceAlerts()['FR_CHOIX_AVOCATS'];
    expect(alert?.level).toBe('blocker');
    expect(alert?.source).toBe('F96');
  });

  it('should alert warning on step A_FAIRE + F-96 VERIFIED', () => {
    component.procedureChecks = [f96('VERIFIED', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'A_FAIRE' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']?.level).toBe('warning');
  });

  it('should NOT alert on step FAIT + F-96 VERIFIED (concordance)', () => {
    component.procedureChecks = [f96('VERIFIED', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'FAIT' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']).toBeUndefined();
  });

  it('should alert blocker on step FAIT + Question IA "non"', () => {
    component.aiQuestions = [question('non, pas encore', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'FAIT' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']?.level).toBe('blocker');
  });

  it('should alert warning on step A_FAIRE + Question IA "oui"', () => {
    component.aiQuestions = [question('oui, déjà choisis', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'A_FAIRE' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']?.level).toBe('warning');
  });

  it('should combine sources to MULTI on step', () => {
    component.procedureChecks = [f96('NON_COMPLIANT', 'FR_CHOIX_AVOCATS')];
    component.aiQuestions = [question('non', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'FAIT' }]);
    const alert = component.coherenceAlerts()['FR_CHOIX_AVOCATS'];
    expect(alert?.source).toBe('MULTI');
    expect(alert?.contributors).toEqual(expect.arrayContaining(['F96', 'QUESTION_IA']));
  });

  // PIÈCES
  it('should alert warning on piece PRESENTE + pieces_manquantes IA', () => {
    component.piecesManquantes = [piece('FR_ACTE_MARIAGE', 'acte de mariage manquant')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'PRESENTE' }]);
    const alert = component.coherenceAlerts()['FR_ACTE_MARIAGE'];
    expect(alert?.level).toBe('warning');
    expect(alert?.source).toBe('PIECE_IA');
  });

  it('should NOT alert on piece MANQUANTE + pieces_manquantes IA (concordance)', () => {
    component.piecesManquantes = [piece('FR_ACTE_MARIAGE')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'MANQUANTE' }]);
    expect(component.coherenceAlerts()['FR_ACTE_MARIAGE']).toBeUndefined();
  });

  it('should alert blocker on piece PRESENTE + F-96 NON_COMPLIANT', () => {
    component.procedureChecks = [f96('NON_COMPLIANT', 'FR_ACTE_MARIAGE')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'PRESENTE' }]);
    expect(component.coherenceAlerts()['FR_ACTE_MARIAGE']?.level).toBe('blocker');
  });

  it('should alert warning on piece MANQUANTE + F-96 VERIFIED', () => {
    component.procedureChecks = [f96('VERIFIED', 'FR_ACTE_MARIAGE')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'MANQUANTE' }]);
    expect(component.coherenceAlerts()['FR_ACTE_MARIAGE']?.level).toBe('warning');
  });

  it('should combine PIECE_IA + F-96 into MULTI', () => {
    component.piecesManquantes = [piece('FR_ACTE_MARIAGE')];
    component.procedureChecks = [f96('NON_COMPLIANT', 'FR_ACTE_MARIAGE')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'PRESENTE' }]);
    const alert = component.coherenceAlerts()['FR_ACTE_MARIAGE'];
    expect(alert?.source).toBe('MULTI');
  });

  it('should ignore unknown critereCode', () => {
    component.procedureChecks = [f96('VERIFIED', 'UNKNOWN_CODE')];
    withResult([{ ...MOCK.etapes[0], statut: 'A_FAIRE' }]);
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should count blockers and warnings correctly', () => {
    component.procedureChecks = [
      f96('NON_COMPLIANT', 'FR_CHOIX_AVOCATS'),
      f96('VERIFIED', 'FR_ACTE_MARIAGE'),
    ];
    withResult(
      [{ ...MOCK.etapes[0], statut: 'FAIT' }],
      [{ ...MOCK.pieces[0], statut: 'MANQUANTE' }]
    );
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 1 });
  });

  it('should produce no alert when no source', () => {
    initWith();
    expect(component.alertsSummary().total).toBe(0);
  });
});
