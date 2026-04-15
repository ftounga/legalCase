import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { LicenciementSectionComponent } from './licenciement-section.component';

describe('LicenciementSectionComponent', () => {
  let component: LicenciementSectionComponent;
  let fixture: ComponentFixture<LicenciementSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/licenciement`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID, country: 'FRANCE', scoreRisque: 35, verdict: 'RISQUE_MODERE',
    criteres: [
      { code: 'FR_CONVOCATION', label: 'Convocation', reponse: 'OUI', pointsRisque: 0, bloquant: true, commentaire: 'Conforme' },
      { code: 'FR_MOTIVATION', label: 'Motivation', reponse: 'NON', pointsRisque: 20, bloquant: true, commentaire: 'NON CONFORME' },
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LicenciementSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LicenciementSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => { httpMock.verify(); });

  function initNoExisting(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExisting(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
  }

  it('should create', () => { initNoExisting(); expect(component).toBeTruthy(); });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
  });

  it('should show form when no existing', () => {
    initNoExisting();
    expect(component.showForm()).toBe(true);
  });

  it('should call POST when analyze()', () => {
    initNoExisting();
    component.analyze();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    req.flush(MOCK_RESPONSE);
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  it('should display existing from GET', () => {
    initWithExisting();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  it('should pre-fill form from aiData when no saved result (FRANCE)', () => {
    component.aiData = {
      detections: {
        FR_CONVOCATION: { reponse: 'OUI', justification: 'LRAR trouvée' },
        FR_MOTIVATION: { reponse: 'NON', justification: 'Motif vague' },
        FR_ENTRETIEN: { reponse: 'INCONNU', justification: '' },
      },
    };
    initNoExisting();
    const form = component.criteresForm();
    expect(form.find(c => c.code === 'FR_CONVOCATION')?.reponse).toBe('OUI');
    expect(form.find(c => c.code === 'FR_MOTIVATION')?.reponse).toBe('NON');
    expect(form.find(c => c.code === 'FR_ENTRETIEN')?.reponse).toBe('INCONNU');
  });

  it('should pre-fill from aiData for Belgique workspace', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = {
      detections: {
        BE_NOTIFICATION: { reponse: 'OUI', justification: 'LRAR' },
        BE_PREAVIS: { reponse: 'NON', justification: 'Préavis non respecté' },
      },
    };
    initNoExisting();
    const form = component.criteresForm();
    expect(form.find(c => c.code === 'BE_NOTIFICATION')?.reponse).toBe('OUI');
    expect(form.find(c => c.code === 'BE_PREAVIS')?.reponse).toBe('NON');
  });

  it('should NOT override saved responses with aiData', () => {
    component.aiData = {
      detections: {
        FR_CONVOCATION: { reponse: 'NON', justification: 'Absent' },
      },
    };
    initWithExisting();
    const form = component.criteresForm();
    expect(form.find(c => c.code === 'FR_CONVOCATION')?.reponse).toBe('OUI');
  });

  it('should leave INCONNU when detection is INCONNU or absent', () => {
    component.aiData = {
      detections: {
        FR_CONVOCATION: { reponse: 'INCONNU', justification: '' },
      },
    };
    initNoExisting();
    const form = component.criteresForm();
    expect(form.find(c => c.code === 'FR_CONVOCATION')?.reponse).toBe('INCONNU');
    expect(form.find(c => c.code === 'FR_MOTIVATION')?.reponse).toBe('INCONNU');
  });

  it('should allow user to override a pre-filled answer', () => {
    component.aiData = {
      detections: { FR_CONVOCATION: { reponse: 'OUI', justification: 'LRAR' } },
    };
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'NON');
    expect(component.criteresForm().find(c => c.code === 'FR_CONVOCATION')?.reponse).toBe('NON');
  });

  // ---- Coherence alerts (SF-IA-03-01) ----

  it('should emit no alert when avocat answer matches AI detection', () => {
    component.aiData = {
      detections: { FR_CONVOCATION: { reponse: 'OUI', justification: 'LRAR' } },
    };
    initNoExisting();
    expect(component.coherenceAlerts()['FR_CONVOCATION']).toBeUndefined();
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should emit blocker alert on divergence for a blocking criterion (FR)', () => {
    component.aiData = {
      detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'Motif précis dans la lettre' } },
    };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert).toBeDefined();
    expect(alert.level).toBe('blocker');
    expect(alert.aiReponse).toBe('OUI');
    expect(alert.justification).toBe('Motif précis dans la lettre');
    expect(component.alertsSummary()).toEqual({ total: 1, blockers: 1 });
  });

  it('should emit warning alert on divergence for a non-blocking criterion (FR)', () => {
    component.aiData = {
      detections: { FR_DELAI_NOTIFICATION: { reponse: 'OUI', justification: 'Notif J+3' } },
    };
    initNoExisting();
    component.onReponseChange('FR_DELAI_NOTIFICATION', 'NON');
    const alert = component.coherenceAlerts()['FR_DELAI_NOTIFICATION'];
    expect(alert.level).toBe('warning');
    expect(component.alertsSummary()).toEqual({ total: 1, blockers: 0 });
  });

  it('should NOT emit alert when AI detection is INCONNU', () => {
    component.aiData = {
      detections: { FR_CONVOCATION: { reponse: 'INCONNU', justification: '' } },
    };
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'OUI');
    expect(component.coherenceAlerts()['FR_CONVOCATION']).toBeUndefined();
  });

  it('should NOT emit alert when avocat answer is INCONNU', () => {
    component.aiData = {
      detections: { FR_CONVOCATION: { reponse: 'NON', justification: 'Pas de LRAR' } },
    };
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
    component.onReponseChange('FR_CONVOCATION', 'INCONNU');
    expect(component.coherenceAlerts()['FR_CONVOCATION']).toBeUndefined();
  });

  it('should produce no alert when aiData is absent', () => {
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'NON');
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should count blockers and warnings correctly', () => {
    component.aiData = {
      detections: {
        FR_MOTIVATION: { reponse: 'OUI', justification: 'OK' },
        FR_DELAI_NOTIFICATION: { reponse: 'OUI', justification: 'OK' },
      },
    };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    component.onReponseChange('FR_DELAI_NOTIFICATION', 'NON');
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 1 });
  });

  it('should react when avocat changes answer (computed reactivity)', () => {
    component.aiData = {
      detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'OK' } },
    };
    initNoExisting();
    expect(component.coherenceAlerts()['FR_MOTIVATION']).toBeUndefined();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    expect(component.coherenceAlerts()['FR_MOTIVATION']).toBeDefined();
    component.onReponseChange('FR_MOTIVATION', 'OUI');
    expect(component.coherenceAlerts()['FR_MOTIVATION']).toBeUndefined();
  });

  it('should handle BE criteria (blocker on BE_MOTIVATION, warning on BE_AUDITION)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = {
      detections: {
        BE_MOTIVATION: { reponse: 'OUI', justification: 'CCT 109' },
        BE_AUDITION: { reponse: 'OUI', justification: 'Audition tenue' },
      },
    };
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
    component.onReponseChange('BE_MOTIVATION', 'NON');
    component.onReponseChange('BE_AUDITION', 'NON');
    expect(component.coherenceAlerts()['BE_MOTIVATION'].level).toBe('blocker');
    expect(component.coherenceAlerts()['BE_AUDITION'].level).toBe('warning');
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 1 });
  });

  it('should default justification to fallback text when empty', () => {
    component.aiData = {
      detections: { FR_MOTIVATION: { reponse: 'OUI', justification: '' } },
    };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    expect(component.coherenceAlerts()['FR_MOTIVATION'].justification).toBe('Aucune justification fournie');
  });

  // ---- F-96 procedure checks as source (SF-IA-03-02) ----

  function f96(overrides: Partial<{ id: string; ordre: number; description: string; statut: 'TO_CHECK' | 'VERIFIED' | 'NON_COMPLIANT'; raison: string | null; critereCode: string | null }>) {
    return {
      id: overrides.id ?? 'c-' + Math.random(),
      ordre: overrides.ordre ?? 0,
      description: overrides.description ?? '',
      statut: overrides.statut ?? 'TO_CHECK',
      raison: overrides.raison ?? null,
      critereCode: overrides.critereCode ?? null,
    } as any;
  }

  it('should emit F-96 blocker when VERIFIED point contradicts avocat NON', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_MOTIVATION', statut: 'VERIFIED', description: 'Lettre motivée' })];
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert.source).toBe('F96');
    expect(alert.level).toBe('blocker');
    expect(alert.expectedReponse).toBe('OUI');
    expect(alert.f96Statut).toBe('VERIFIED');
  });

  it('should emit F-96 blocker when NON_COMPLIANT point contradicts avocat OUI', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_CONVOCATION', statut: 'NON_COMPLIANT', raison: 'Pas de LRAR' })];
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'OUI');
    const alert = component.coherenceAlerts()['FR_CONVOCATION'];
    expect(alert.source).toBe('F96');
    expect(alert.level).toBe('blocker');
    expect(alert.expectedReponse).toBe('NON');
    expect(alert.f96Raison).toBe('Pas de LRAR');
  });

  it('should emit blocker (not warning) for F-96 even on non-blocking criterion', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_DELAI_NOTIFICATION', statut: 'VERIFIED' })];
    initNoExisting();
    component.onReponseChange('FR_DELAI_NOTIFICATION', 'NON');
    expect(component.coherenceAlerts()['FR_DELAI_NOTIFICATION'].level).toBe('blocker');
  });

  it('should NOT emit alert when F-96 VERIFIED matches avocat OUI', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_MOTIVATION', statut: 'VERIFIED' })];
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'OUI');
    expect(component.coherenceAlerts()['FR_MOTIVATION']).toBeUndefined();
  });

  it('should ignore F-96 TO_CHECK and fall back to AI', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_MOTIVATION', statut: 'TO_CHECK' })];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert.source).toBe('IA');
    expect(alert.level).toBe('blocker');
  });

  it('should combine F96 and IA into source MULTI when both contradict avocat', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_MOTIVATION', statut: 'VERIFIED', raison: 'Conforme' })];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA détecte motif précis' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert.source).toBe('MULTI');
    expect(alert.contributors).toContain('F96');
    expect(alert.contributors).toContain('IA');
    expect(alert.level).toBe('blocker');
    expect(alert.aiReponse).toBe('OUI');
    expect(alert.f96Statut).toBe('VERIFIED');
  });

  it('should override IA with F-96 when they disagree', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_MOTIVATION', statut: 'NON_COMPLIANT' })];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA dit OUI' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'OUI');
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert.source).toBe('F96');
    expect(alert.expectedReponse).toBe('NON');
  });

  it('should pick NON_COMPLIANT when multiple F-96 points collide on same critere', () => {
    component.procedureChecks = [
      f96({ critereCode: 'FR_MOTIVATION', statut: 'VERIFIED', ordre: 0 }),
      f96({ critereCode: 'FR_MOTIVATION', statut: 'NON_COMPLIANT', ordre: 1 }),
    ];
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    // NON_COMPLIANT expects NON, avocat says NON → no alert
    expect(component.coherenceAlerts()['FR_MOTIVATION']).toBeUndefined();
    component.onReponseChange('FR_MOTIVATION', 'OUI');
    // NON_COMPLIANT expects NON, avocat says OUI → blocker
    expect(component.coherenceAlerts()['FR_MOTIVATION']?.expectedReponse).toBe('NON');
  });

  it('should ignore F-96 with unknown critereCode', () => {
    component.procedureChecks = [f96({ critereCode: 'UNKNOWN_CODE', statut: 'VERIFIED' })];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    // Falls through to IA
    expect(component.coherenceAlerts()['FR_MOTIVATION']?.source).toBe('IA');
  });

  it('should fall back to SF-IA-03-01 behaviour when procedureChecks is empty', () => {
    component.procedureChecks = [];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    expect(component.coherenceAlerts()['FR_MOTIVATION']?.source).toBe('IA');
  });

  it('should count F-96 blockers in alertsSummary', () => {
    component.procedureChecks = [
      f96({ critereCode: 'FR_MOTIVATION', statut: 'VERIFIED' }),
      f96({ critereCode: 'FR_DELAI_NOTIFICATION', statut: 'VERIFIED' }),
    ];
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    component.onReponseChange('FR_DELAI_NOTIFICATION', 'NON');
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 2 });
  });

  // ---- AI questions as source (SF-IA-03-03) ----

  function question(overrides: Partial<{ id: string; orderIndex: number; questionText: string; answerText: string | null; critereCode: string | null }>) {
    return {
      id: overrides.id ?? 'q-' + Math.random(),
      orderIndex: overrides.orderIndex ?? 0,
      questionText: overrides.questionText ?? '',
      answerText: overrides.answerText ?? null,
      critereCode: overrides.critereCode ?? null,
    } as any;
  }

  it('should emit QUESTION_IA blocker when answer "oui" contradicts avocat NON', () => {
    component.aiQuestions = [question({ critereCode: 'FR_MOTIVATION', questionText: 'Motif précis ?', answerText: 'oui' })];
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert.source).toBe('QUESTION_IA');
    expect(alert.level).toBe('blocker');
    expect(alert.expectedReponse).toBe('OUI');
    expect(alert.questionText).toBe('Motif précis ?');
  });

  it('should emit QUESTION_IA blocker when answer "non" contradicts avocat OUI', () => {
    component.aiQuestions = [question({ critereCode: 'FR_CONVOCATION', questionText: 'LRAR ?', answerText: 'Non, pas de preuve' })];
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'OUI');
    const alert = component.coherenceAlerts()['FR_CONVOCATION'];
    expect(alert.source).toBe('QUESTION_IA');
    expect(alert.expectedReponse).toBe('NON');
  });

  it('should ignore ambiguous answer and fall back to IA', () => {
    component.aiQuestions = [question({ critereCode: 'FR_MOTIVATION', answerText: 'peut-être, pas sûr' })];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    expect(component.coherenceAlerts()['FR_MOTIVATION'].source).toBe('IA');
  });

  it('should ignore conflicting answers on same critere', () => {
    component.aiQuestions = [
      question({ critereCode: 'FR_MOTIVATION', answerText: 'oui' }),
      question({ critereCode: 'FR_MOTIVATION', answerText: 'non', orderIndex: 1 }),
    ];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    // Conflicting questions ignored → fall back to IA
    expect(component.coherenceAlerts()['FR_MOTIVATION'].source).toBe('IA');
  });

  it('should respect F-96 priority over QUESTION_IA', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_MOTIVATION', statut: 'NON_COMPLIANT' })];
    component.aiQuestions = [question({ critereCode: 'FR_MOTIVATION', answerText: 'oui' })];
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'OUI');
    // F-96 NON_COMPLIANT expects NON; question IA expects OUI (concorde avocat)
    // F-96 wins priority → blocker F96, expected NON
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert.source).toBe('F96');
    expect(alert.expectedReponse).toBe('NON');
  });

  it('should combine F-96 + QUESTION_IA into MULTI when they align against avocat', () => {
    component.procedureChecks = [f96({ critereCode: 'FR_MOTIVATION', statut: 'VERIFIED' })];
    component.aiQuestions = [question({ critereCode: 'FR_MOTIVATION', answerText: 'oui' })];
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    const alert = component.coherenceAlerts()['FR_MOTIVATION'];
    expect(alert.source).toBe('MULTI');
    expect(alert.contributors).toContain('F96');
    expect(alert.contributors).toContain('QUESTION_IA');
    expect(alert.expectedReponse).toBe('OUI');
  });

  // ---- Missing pieces as source (SF-IA-03-03) ----

  it('should emit PIECE_MANQUANTE warning when piece missing and avocat coche OUI', () => {
    component.piecesManquantes = [{ texte: 'LRAR de convocation', critereCode: 'FR_CONVOCATION' }];
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'OUI');
    const alert = component.coherenceAlerts()['FR_CONVOCATION'];
    expect(alert.source).toBe('PIECE_MANQUANTE');
    expect(alert.level).toBe('warning');
    expect(alert.expectedReponse).toBe('NON');
    expect(alert.pieceTexte).toBe('LRAR de convocation');
  });

  it('should NOT emit alert when piece missing and avocat coche NON', () => {
    component.piecesManquantes = [{ texte: 'LRAR', critereCode: 'FR_CONVOCATION' }];
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'NON');
    expect(component.coherenceAlerts()['FR_CONVOCATION']).toBeUndefined();
  });

  it('should upgrade IA alert with PIECE_MANQUANTE contributor when piece expects NON too', () => {
    component.piecesManquantes = [{ texte: 'LRAR', critereCode: 'FR_CONVOCATION' }];
    component.aiData = { detections: { FR_CONVOCATION: { reponse: 'NON', justification: 'pas détecté' } } };
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'OUI');
    const alert = component.coherenceAlerts()['FR_CONVOCATION'];
    // IA primary (expected NON), PIECE supports (expects NON too) → MULTI
    expect(alert.source).toBe('MULTI');
    expect(alert.contributors).toContain('IA');
    expect(alert.contributors).toContain('PIECE_MANQUANTE');
  });

  it('should ignore piece with unknown critereCode', () => {
    component.piecesManquantes = [{ texte: 'Doc X', critereCode: 'UNKNOWN' }];
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'OUI');
    expect(component.coherenceAlerts()['FR_CONVOCATION']).toBeUndefined();
  });

  it('should count PIECE_MANQUANTE in alertsSummary but not as blocker', () => {
    component.piecesManquantes = [
      { texte: 'Doc1', critereCode: 'FR_CONVOCATION' },
      { texte: 'Doc2', critereCode: 'FR_ENTRETIEN' },
    ];
    initNoExisting();
    component.onReponseChange('FR_CONVOCATION', 'OUI');
    component.onReponseChange('FR_ENTRETIEN', 'OUI');
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 0 });
  });

  it('should preserve full hierarchy fallback when no new source is present', () => {
    component.aiQuestions = [];
    component.piecesManquantes = [];
    component.procedureChecks = [];
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'IA' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    expect(component.coherenceAlerts()['FR_MOTIVATION'].source).toBe('IA');
  });

  it('SF-IA-03-14: coherenceAlerts retourne {} quand showForm=false', () => {
    component.aiData = { detections: { FR_MOTIVATION: { reponse: 'OUI', justification: 'Motif précis' } } };
    initNoExisting();
    component.onReponseChange('FR_MOTIVATION', 'NON');
    expect(Object.keys(component.coherenceAlerts()).length).toBeGreaterThan(0);
    component.showForm.set(false);
    expect(component.coherenceAlerts()).toEqual({});
  });
});
