import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ImmigrationRecoursSectionComponent } from './immigration-recours-section.component';

describe('ImmigrationRecoursSectionComponent', () => {
  let component: ImmigrationRecoursSectionComponent;
  let fixture: ComponentFixture<ImmigrationRecoursSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '22222222-2222-2222-2222-222222222222';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/immigration/recours`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    recoursType: 'RECOURS_GRACIEUX_PREFET',
    recoursLabel: 'Recours gracieux auprès du préfet',
    dateNotification: '2026-03-01',
    dateLimite: '2026-04-30',
    dateLimiteDepassee: false,
    avertissement: null,
    requerant: { nom: 'Dupont', prenom: 'Jean', nationalite: 'Marocaine', adresse: '12 rue Test' },
    decisionContestee: { autorite: 'Préfet', date: '2026-02-15', reference: 'REF-001' },
    exposeFaits: 'Faits.',
    document: {
      enTete: 'En-tête test',
      objetDemande: 'Objet test',
      visaTextes: 'Visa test',
      exposeFaits: 'Faits test',
      moyensDroit: 'Moyens test',
      conclusions: 'Conclusions test',
      piecesJointes: ['Pièce 1', 'Pièce 2']
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImmigrationRecoursSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ImmigrationRecoursSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function initNoExisting(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExisting(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
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
  });

  it('should show form when no existing recours', () => {
    initNoExisting();
    expect(component.showForm()).toBe(true);
    expect(component.recours()).toBeNull();
  });

  it('should call POST when generate() is called', () => {
    initNoExisting();

    component.recoursType.set('RECOURS_GRACIEUX_PREFET');
    component.dateNotification.set('2026-03-01');
    component.nom.set('Dupont');
    component.prenom.set('Jean');
    component.nationalite.set('Marocaine');
    component.adresse.set('12 rue Test');
    component.autorite.set('Préfet');
    component.dateDecision.set('2026-02-15');
    component.generate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.recoursType).toBe('RECOURS_GRACIEUX_PREFET');
    req.flush(MOCK_RESPONSE);

    expect(component.recours()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  it('should display existing recours from GET', () => {
    initWithExisting();
    expect(component.recours()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.nom()).toBe('Dupont');
  });

  // ---- AI prefill (SF-IM-06-04) ----

  it('should prefill recoursType from IA code', () => {
    component.aiData = { typeRecoursCode: 'RECOURS_CNDA' } as any;
    initNoExisting();
    expect(component.recoursType()).toBe('RECOURS_CNDA');
    expect(component.provenanceRecoursType()).toBe('IA');
  });

  it('should prefill dateNotification from IA', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-03-10' } as any;
    initNoExisting();
    expect(component.dateNotification()).toBe('2026-03-10');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('should prefill both fields when IA provides both', () => {
    component.aiData = {
      typeRecoursCode: 'RECOURS_CCE',
      dateNotificationDecisionContestee: '2026-02-15'
    } as any;
    initNoExisting();
    expect(component.recoursType()).toBe('RECOURS_CCE');
    expect(component.dateNotification()).toBe('2026-02-15');
  });

  it('should ignore unknown recours code', () => {
    component.aiData = { typeRecoursCode: 'UNKNOWN' } as any;
    initNoExisting();
    expect(component.recoursType()).toBe('RECOURS_GRACIEUX_PREFET'); // default unchanged
    expect(component.provenanceRecoursType()).toBeNull();
  });

  it('should ignore malformed date', () => {
    component.aiData = { dateNotificationDecisionContestee: 'invalid-date' } as any;
    initNoExisting();
    expect(component.dateNotification()).toBe(''); // default unchanged
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('should NOT prefill when recours exists (GET succeeds)', () => {
    component.aiData = {
      typeRecoursCode: 'RECOURS_CNDA',
      dateNotificationDecisionContestee: '2026-03-10'
    } as any;
    initWithExisting();
    // Existing GET flushed MOCK_RESPONSE → prefilled from resp, not from aiData
    expect(component.recoursType()).toBe('RECOURS_GRACIEUX_PREFET');
    expect(component.provenanceRecoursType()).toBeNull();
  });

  it('should clear provenance when user changes recoursType', () => {
    component.aiData = { typeRecoursCode: 'RECOURS_CNDA' } as any;
    initNoExisting();
    expect(component.provenanceRecoursType()).toBe('IA');
    component.recoursType.set('RECOURS_CGRA');
    component.onRecoursTypeChange();
    expect(component.provenanceRecoursType()).toBeNull();
  });

  it('should clear provenance when user changes dateNotification', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-03-10' } as any;
    initNoExisting();
    expect(component.provenanceDateNotification()).toBe('IA');
    component.dateNotification.set('2026-04-01');
    component.onDateNotificationChange();
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('should do nothing when aiData absent', () => {
    initNoExisting();
    expect(component.recoursType()).toBe('RECOURS_GRACIEUX_PREFET');
    expect(component.dateNotification()).toBe('');
    expect(component.provenanceRecoursType()).toBeNull();
  });

  // ---- Coherence alerts (SF-IA-03-10) ----

  function f96(statut: string, expectedValue: string, raison: string | null = null) {
    return { id: 'c', ordre: 0, description: 'point', statut, raison,
      critereCode: 'IM06_RECOURS_TYPE', expectedValue } as any;
  }
  function question(answerText: string, expectedValue: string) {
    return { id: 'q', orderIndex: 0, questionText: 'Q?', answerText,
      critereCode: 'IM06_RECOURS_TYPE', expectedValue } as any;
  }

  it('should alert warning F96 on recoursType mismatch', () => {
    component.procedureChecks = [f96('VERIFIED', 'RECOURS_CCE', 'Refus asile BE')];
    initNoExisting();
    component.recoursType.set('RECOURS_CGRA');
    const alert = component.coherenceAlerts().RECOURS_TYPE;
    expect(alert?.source).toBe('F96');
    expect(alert?.expectedDisplay).toBe('RECOURS_CCE');
  });

  it('should alert warning Question IA "oui"', () => {
    component.aiQuestions = [question('oui', 'RECOURS_CNDA')];
    initNoExisting();
    component.recoursType.set('RECOURS_GRACIEUX_PREFET');
    expect(component.coherenceAlerts().RECOURS_TYPE?.source).toBe('QUESTION_IA');
  });

  it('should alert warning IA when typeRecoursCode diverges', () => {
    component.aiData = { typeRecoursCode: 'RECOURS_CONTENTIEUX_TA' } as any;
    initNoExisting();
    component.recoursType.set('RECOURS_GRACIEUX_PREFET');
    const alert = component.coherenceAlerts().RECOURS_TYPE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('RECOURS_CONTENTIEUX_TA');
  });

  it('should combine sources to MULTI when aligned', () => {
    component.procedureChecks = [f96('VERIFIED', 'RECOURS_CNDA')];
    component.aiQuestions = [question('oui', 'RECOURS_CNDA')];
    component.aiData = { typeRecoursCode: 'RECOURS_CNDA' } as any;
    initNoExisting();
    component.recoursType.set('RECOURS_GRACIEUX_PREFET');
    const alert = component.coherenceAlerts().RECOURS_TYPE;
    expect(alert?.source).toBe('MULTI');
    expect(alert?.contributors).toEqual(expect.arrayContaining(['F96', 'QUESTION_IA', 'IA']));
  });

  it('should NOT alert when recoursType matches user', () => {
    component.aiData = { typeRecoursCode: 'RECOURS_CNDA' } as any;
    initNoExisting();
    component.recoursType.set('RECOURS_CNDA');
    expect(component.coherenceAlerts().RECOURS_TYPE).toBeUndefined();
  });

  it('should ignore expected_value outside enum', () => {
    component.procedureChecks = [f96('VERIFIED', 'UNKNOWN')];
    initNoExisting();
    component.recoursType.set('RECOURS_GRACIEUX_PREFET');
    expect(component.coherenceAlerts().RECOURS_TYPE).toBeUndefined();
  });

  // DATE_NOTIFICATION
  it('should NOT alert date when gap ≤ 7 days', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-03-10' } as any;
    initNoExisting();
    component.dateNotification.set('2026-03-15'); // 5 jours
    expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
  });

  it('should NOT alert date at exactly 7 days', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-03-10' } as any;
    initNoExisting();
    component.dateNotification.set('2026-03-17'); // 7 jours pile
    expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
  });

  it('should alert date warning when gap > 7 days', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-03-10' } as any;
    initNoExisting();
    component.dateNotification.set('2026-03-20'); // 10 jours
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('2026-03-10');
  });

  it('should NOT alert when user date empty', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-03-10' } as any;
    initNoExisting();
    component.dateNotification.set('');
    expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
  });

  it('should NOT alert when IA date malformed', () => {
    component.aiData = { dateNotificationDecisionContestee: 'invalid' } as any;
    initNoExisting();
    component.dateNotification.set('2026-03-20');
    expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
  });

  it('should count multiple alerts in summary', () => {
    component.aiData = {
      typeRecoursCode: 'RECOURS_CNDA',
      dateNotificationDecisionContestee: '2026-03-10'
    } as any;
    initNoExisting();
    component.recoursType.set('RECOURS_GRACIEUX_PREFET');
    component.dateNotification.set('2026-03-25'); // 15 jours
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 0 });
  });

  it('should freeze alerts when recours loaded', () => {
    component.aiData = {
      typeRecoursCode: 'RECOURS_CNDA',
      dateNotificationDecisionContestee: '2026-03-10'
    } as any;
    initWithExisting();
    expect(component.alertsSummary().total).toBe(0);
  });
});
