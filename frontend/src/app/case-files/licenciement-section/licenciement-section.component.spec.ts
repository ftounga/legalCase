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
});
