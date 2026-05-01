import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { SimpleChange } from '@angular/core';
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

  // ---- Prefill + Coherence (SF-IA-03-11 + SF-155-11) ----

  function f96(statut: string, expectedValue: string, raison: string | null = null) {
    return { id: 'c', ordre: 0, description: 'point', statut, raison,
      critereCode: 'IM07_TITRE_TYPE', expectedValue } as any;
  }
  function question(answerText: string, expectedValue: string) {
    return { id: 'q', orderIndex: 0, questionText: 'Q?', answerText,
      critereCode: 'IM07_TITRE_TYPE', expectedValue } as any;
  }

  // Prefill titre
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
    expect(component.provenanceTitreType()).toBeNull();
  });

  // SF-IM-07-05 : les nouveaux sous-types de CARTE_PLURIANNUELLE et CST_VPF_CONJOINT_FR
  // produits par le prompt IA SF-IM-07-04 doivent pré-remplir correctement.
  it('SF-IM-07-05 : prefill CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE (cas Chen)', () => {
    component.aiData = { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } as any;
    initNoExisting();
    expect(component.titreType()).toBe('CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE');
    expect(component.provenanceTitreType()).toBe('IA');
  });

  it('SF-IM-07-05 : prefill CST_VPF_CONJOINT_FR (mariage avec Français)', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF_CONJOINT_FR' } as any;
    initNoExisting();
    expect(component.titreType()).toBe('CST_VPF_CONJOINT_FR');
    expect(component.provenanceTitreType()).toBe('IA');
  });

  it('SF-IM-07-05 : prefill fires at mount (ngOnInit garde-fou)', () => {
    component.aiData = { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_SALARIE' } as any;
    initNoExisting();
    expect(component.titreType()).toBe('CARTE_PLURIANNUELLE_SALARIE');
    expect(component.provenanceTitreType()).toBe('IA');
  });

  it('should NOT prefill titre when result already exists', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initWithExisting();
    expect(component.titreType()).toBe('VLS_TS_SALARIE'); // from existing result
    expect(component.provenanceTitreType()).toBeNull();
  });

  it('should clear titre provenance when user changes titreType', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initNoExisting();
    expect(component.provenanceTitreType()).toBe('IA');
    component.titreType.set('CARTE_RESIDENT');
    component.onTitreTypeChange();
    expect(component.provenanceTitreType()).toBeNull();
  });

  // ---- SF-155-11 : pré-fill country + provenance country ----

  it('SF-155-11 : prefill country from workspaceCountry (FRANCE)', () => {
    component.workspaceCountry = 'FRANCE';
    initNoExisting();
    expect(component.country()).toBe('FRANCE');
    expect(component.provenanceCountry()).toBe('IA');
  });

  it('SF-155-11 : prefill country from workspaceCountry (BELGIQUE)', () => {
    component.workspaceCountry = 'BELGIQUE';
    initNoExisting();
    expect(component.country()).toBe('BELGIQUE');
    expect(component.provenanceCountry()).toBe('IA');
  });

  it('SF-155-11 : BELGIQUE workspace switches titreType default to CARTE_A_TRAVAIL', () => {
    component.workspaceCountry = 'BELGIQUE';
    initNoExisting();
    expect(component.titreType()).toBe('CARTE_A_TRAVAIL');
  });

  it('SF-155-11 : clear country provenance when onCountryChange() is called', () => {
    component.workspaceCountry = 'FRANCE';
    initNoExisting();
    expect(component.provenanceCountry()).toBe('IA');
    component.onCountryChange();
    expect(component.provenanceCountry()).toBeNull();
  });

  it('SF-155-11 : loadExisting (GET 200) clears both provenance signals', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initWithExisting();
    expect(component.provenanceTitreType()).toBeNull();
    expect(component.provenanceCountry()).toBeNull();
  });

  // ---- Cohérence F-IA-03 (via CoherenceAlertBuilder partagé) ----

  it('should alert warning F96 on titre mismatch (TITRE_TYPE field)', () => {
    component.procedureChecks = [f96('VERIFIED', 'CARTE_RESIDENT', 'Motif regroupement')];
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    const alert = component.coherenceAlerts().TITRE_TYPE;
    expect(alert?.source).toBe('F96');
    expect(alert?.expectedDisplay).toBe('CARTE_RESIDENT');
    expect(alert?.field).toBe('TITRE_TYPE');
  });

  it('should alert warning Question IA "oui"', () => {
    component.aiQuestions = [question('oui', 'CST_VPF')];
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlerts().TITRE_TYPE?.source).toBe('QUESTION_IA');
  });

  it('should alert warning IA when typeTitreSejourCode diverges', () => {
    component.aiData = { typeTitreSejourCode: 'CARTE_RESIDENT' } as any;
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlerts().TITRE_TYPE?.source).toBe('IA');
  });

  it('should combine sources into MULTI', () => {
    component.procedureChecks = [f96('VERIFIED', 'CST_VPF')];
    component.aiQuestions = [question('oui', 'CST_VPF')];
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    const alert = component.coherenceAlerts().TITRE_TYPE;
    expect(alert?.source).toBe('MULTI');
    expect(alert?.contributors).toEqual(expect.arrayContaining(['F96', 'QUESTION_IA', 'IA']));
  });

  it('should NOT alert when titre matches user', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_SALARIE' } as any;
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    expect(component.coherenceAlerts().TITRE_TYPE).toBeUndefined();
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
    expect(component.coherenceAlerts().TITRE_TYPE).toBeUndefined();
  });

  it('SF-IA-03-14: pièce manquante IM07_TITRE_TYPE devient contributor PIECE_MANQUANTE', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_SALARIE' } as any;
    component.piecesManquantes = [{ texte: 'Titre de séjour', critereCode: 'IM07_TITRE_TYPE' }];
    initNoExisting();
    component.titreType.set('CARTE_RESIDENT');
    const alert = component.coherenceAlerts().TITRE_TYPE;
    expect(alert?.contributors).toContain('PIECE_MANQUANTE');
    expect(alert?.source).toBe('MULTI');
    expect(alert?.pieceTexte).toBe('Titre de séjour');
  });

  // ---- SF-155-11 : alertes COUNTRY (nouveau field) ----

  it('SF-155-11 : should alert COUNTRY when IA code belongs to other country', () => {
    // FR workspace, IA detects a BE code
    component.workspaceCountry = 'FRANCE';
    component.aiData = { typeTitreSejourCode: 'CARTE_B' } as any;
    initNoExisting();
    const alert = component.coherenceAlerts().COUNTRY;
    expect(alert?.source).toBe('IA');
    expect(alert?.field).toBe('COUNTRY');
    expect(alert?.expectedDisplay).toBe('Belgique');
  });

  it('SF-155-11 : should NOT alert COUNTRY when IA code matches workspace country', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { typeTitreSejourCode: 'VLS_TS_SALARIE' } as any;
    initNoExisting();
    expect(component.coherenceAlerts().COUNTRY).toBeUndefined();
  });

  it('SF-155-11 : BE workspace + FR IA code produces COUNTRY alert (France expected)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = { typeTitreSejourCode: 'CST_SALARIE' } as any;
    initNoExisting();
    const alert = component.coherenceAlerts().COUNTRY;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('France');
  });

  it('SF-155-11 : alertsSummary counts multiple fields (TITRE_TYPE + COUNTRY)', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { typeTitreSejourCode: 'CARTE_B' } as any; // BE code in FR workspace
    initNoExisting();
    // titreType stays default VLS_TS_SALARIE (prefill rejected for wrong country).
    // -> TITRE_TYPE alert (IA says CARTE_B, user says VLS_TS_SALARIE)
    // -> COUNTRY alert (IA code is BE, workspace is FR)
    expect(component.alertsSummary().total).toBe(2);
  });

  // ---- SF-155-11 : ngOnChanges re-prefill ----

  it('SF-155-11 : ngOnChanges re-invokes prefillFromAi when aiData changes', () => {
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE'); // default
    expect(component.provenanceTitreType()).toBeNull();

    component.aiData = { typeTitreSejourCode: 'CARTE_RESIDENT' } as any;
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });
    expect(component.titreType()).toBe('CARTE_RESIDENT');
    expect(component.provenanceTitreType()).toBe('IA');
  });

  it('SF-155-11 : ngOnChanges does NOT re-prefill after result is loaded', () => {
    initWithExisting(); // result loaded, showForm=false
    component.aiData = { typeTitreSejourCode: 'CARTE_RESIDENT' } as any;
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });
    expect(component.titreType()).toBe('VLS_TS_SALARIE'); // unchanged (from result)
  });

  it('SF-155-11 : explanationFor returns entry for TITRE_TYPE field key', () => {
    initNoExisting();
    const explanations = [{ id: 'e1', sourceKey: 'IM07_TITRE_TYPE', explanation: 'Article L.421-1' } as any];
    component.sourceExplanations.set(new Map([['IM07_TITRE_TYPE', explanations]]));
    expect(component.explanationFor('TITRE_TYPE')).toEqual(explanations);
    expect(component.explanationFor('COUNTRY')).toEqual([]);
  });

  it('SF-155-11 : alertBadgeLabel formats each source correctly', () => {
    initNoExisting();
    const mkAlert = (source: any): any => ({
      field: 'TITRE_TYPE', source, contributors: [source], severity: 'WARNING',
      expectedDisplay: 'CARTE_RESIDENT', reason: 'x',
    });
    expect(component.alertBadgeLabel(mkAlert('F96'))).toContain('Checklist procédurale');
    expect(component.alertBadgeLabel(mkAlert('QUESTION_IA'))).toContain('Question complémentaire');
    expect(component.alertBadgeLabel(mkAlert('IA'))).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(mkAlert('PIECE_MANQUANTE'))).toContain('Pièce manquante');
    expect(component.alertBadgeLabel(mkAlert('MULTI'))).toContain('Incohérence multiple');
  });
});

// F-177 SF-177-12 — couvre le static `getPrefillCount` exposé pour la card du panel.
describe('ImmigrationWorkRightSectionComponent.getPrefillCount', () => {
  it('returns 0 when aiData is absent', () => {
    expect(ImmigrationWorkRightSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 0 when typeTitreSejourCode is missing', () => {
    expect(
      ImmigrationWorkRightSectionComponent.getPrefillCount({ aiData: {} }),
    ).toBe(0);
  });

  it('returns 1 for a FR titre code with workspaceCountry=FRANCE', () => {
    expect(
      ImmigrationWorkRightSectionComponent.getPrefillCount({
        aiData: { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('returns 0 for a FR titre code with workspaceCountry=BELGIQUE (mismatch pays)', () => {
    expect(
      ImmigrationWorkRightSectionComponent.getPrefillCount({
        aiData: { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('returns 1 for a BE titre code with workspaceCountry=BELGIQUE', () => {
    expect(
      ImmigrationWorkRightSectionComponent.getPrefillCount({
        aiData: { typeTitreSejourCode: 'CARTE_A_TRAVAIL' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(1);
  });

  it('returns 0 for an unknown titre code', () => {
    expect(
      ImmigrationWorkRightSectionComponent.getPrefillCount({
        aiData: { typeTitreSejourCode: 'INCONNU_XYZ' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(0);
  });
});
