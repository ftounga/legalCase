import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ImmigrationWorkRightSectionComponent } from './immigration-work-right-section.component';

describe('ImmigrationWorkRightSectionComponent', () => {
  let component: ImmigrationWorkRightSectionComponent;
  let fixture: ComponentFixture<ImmigrationWorkRightSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '33333333-3333-3333-3333-333333333333';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/immigration/work-right`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    titreType: 'VLS_TS_SALARIE',
    titreLabel: 'VLS-TS Salarié',
    country: 'FRANCE',
    droitTravail: 'OUI',
    conditions: 'Droit au travail inclus',
    obligationsEmployeur: ['Vérification préfecture', 'DPAE'],
    baseJuridique: 'Articles L. 421-1 du CESEDA'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImmigrationWorkRightSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ImmigrationWorkRightSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function flushSE(): void {
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
  }
  function initNoExisting(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
    flushSE();
  }

  function initWithExisting(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
    flushSE();
  }

  it('should create', () => {
    initNoExisting();
    expect(component).toBeTruthy();
  });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
    flushSE();
  });

  it('should show form when no existing result', () => {
    initNoExisting();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('should call POST when resolve() is called', () => {
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    component.country.set('FRANCE');
    component.resolve();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.titreType).toBe('VLS_TS_SALARIE');
    req.flush(MOCK_RESPONSE);

    expect(component.result()).toBeTruthy();
    expect(component.result()!.droitTravail).toBe('OUI');
    expect(component.showForm()).toBe(false);
  });

  it('should display existing result from GET', () => {
    initWithExisting();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.titreType()).toBe('VLS_TS_SALARIE');
  });

  // ---- Prefill + Coherence (SF-IA-03-11) ----

  function f96(statut: string, expectedValue: string, raison: string | null = null) {
    return { id: 'c', ordre: 0, description: 'point', statut, raison,
      critereCode: 'IM07_TITRE_TYPE', expectedValue } as any;
  }
  function question(answerText: string, expectedValue: string) {
    return { id: 'q', orderIndex: 0, questionText: 'Q?', answerText,
      critereCode: 'IM07_TITRE_TYPE', expectedValue } as any;
  }

  // Prefill
  it('should prefill titreType from IA when FR code + FR workspace', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initNoExisting();
    expect(component.titreType()).toBe('CST_VPF');
    expect(component.provenanceTitreType()).toBe('IA');
  });

  it('should NOT prefill when IA code is from other country', () => {
    // country default FR, IA code is BE
    component.aiData = { typeTitreSejourCode: 'CARTE_B' } as any;
    initNoExisting();
    expect(component.titreType()).toBe('VLS_TS_SALARIE'); // default
    expect(component.provenanceTitreType()).toBeNull();
  });

  it('should ignore unknown IA code', () => {
    component.aiData = { typeTitreSejourCode: 'UNKNOWN' } as any;
    initNoExisting();
    expect(component.titreType()).toBe('VLS_TS_SALARIE');
  });

  it('should NOT prefill when result already exists', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initWithExisting();
    expect(component.titreType()).toBe('VLS_TS_SALARIE'); // from existing result
  });

  it('should clear provenance when user changes titreType', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initNoExisting();
    expect(component.provenanceTitreType()).toBe('IA');
    component.titreType.set('CARTE_RESIDENT');
    component.onTitreTypeChange();
    expect(component.provenanceTitreType()).toBeNull();
  });

  // Cohérence
  it('should alert warning F96 on titre mismatch', () => {
    component.procedureChecks = [f96('VERIFIED', 'CARTE_RESIDENT', 'Motif regroupement')];
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    const alert = component.coherenceAlert();
    expect(alert?.source).toBe('F96');
    expect(alert?.expectedDisplay).toBe('CARTE_RESIDENT');
  });

  it('should alert warning Question IA "oui"', () => {
    component.aiQuestions = [question('oui', 'CST_VPF')];
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlert()?.source).toBe('QUESTION_IA');
  });

  it('should alert warning IA when typeTitreSejourCode diverges', () => {
    component.aiData = { typeTitreSejourCode: 'CARTE_RESIDENT' } as any;
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlert()?.source).toBe('IA');
  });

  it('should combine sources into MULTI', () => {
    component.procedureChecks = [f96('VERIFIED', 'CST_VPF')];
    component.aiQuestions = [question('oui', 'CST_VPF')];
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlert()?.source).toBe('MULTI');
    expect(component.coherenceAlert()?.contributors).toEqual(expect.arrayContaining(['F96', 'QUESTION_IA', 'IA']));
  });

  it('should NOT alert when titre matches user', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_SALARIE' } as any;
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlert()).toBeNull();
  });

  it('should freeze alert when result loaded', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initWithExisting();
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should ignore expected_value outside enum', () => {
    component.procedureChecks = [f96('VERIFIED', 'UNKNOWN')];
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlert()).toBeNull();
  });

  it('SF-IA-03-14: pièce manquante IM07_TITRE_TYPE devient contributor PIECE_MANQUANTE', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_SALARIE' } as any;
    component.piecesManquantes = [{ texte: 'Titre de séjour', critereCode: 'IM07_TITRE_TYPE' }];
    initNoExisting();
    component.titreType.set('CARTE_RESIDENT');
    const alert = component.coherenceAlert();
    expect(alert?.contributors).toContain('PIECE_MANQUANTE');
    expect(alert?.source).toBe('MULTI');
  });
});
