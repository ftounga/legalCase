import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ImmigrationTitleDecisionSectionComponent } from './immigration-title-decision-section.component';

describe('ImmigrationTitleDecisionSectionComponent', () => {
  let component: ImmigrationTitleDecisionSectionComponent;
  let fixture: ComponentFixture<ImmigrationTitleDecisionSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '11111111-1111-1111-1111-111111111111';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/immigration/title-decision`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    country: 'FRANCE',
    nationaliteUe: false,
    motif: 'TRAVAIL',
    duree: 'LONG_SEJOUR',
    situationFamiliale: null,
    recommendations: [{
      code: 'VLS_TS_SALARIE', label: 'VLS-TS Salarié', country: 'FRANCE',
      motif: 'TRAVAIL', conditions: 'Test', pieces: ['Passeport'], delaiMoyenJours: 120
    }]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImmigrationTitleDecisionSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ImmigrationTitleDecisionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function flushSE(): void {
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
  }
  function initWithNoExistingDecision(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
    flushSE();
  }

  function initWithExistingDecision(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
    flushSE();
  }

  it('should create', () => {
    initWithNoExistingDecision();
    expect(component).toBeTruthy();
  });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
    flushSE();
  });

  it('should show form when no existing decision', () => {
    initWithNoExistingDecision();
    expect(component.showForm()).toBe(true);
    expect(component.decision()).toBeNull();
  });

  it('should call POST when resolve() is called', () => {
    initWithNoExistingDecision();

    component.country.set('FRANCE');
    component.nationaliteUe.set(false);
    component.motif.set('TRAVAIL');
    component.duree.set('LONG_SEJOUR');
    component.resolve();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.country).toBe('FRANCE');
    expect(req.request.body.motif).toBe('TRAVAIL');

    req.flush(MOCK_RESPONSE);

    expect(component.decision()).toBeTruthy();
    expect(component.decision()!.recommendations.length).toBe(1);
    expect(component.showForm()).toBe(false);
  });

  it('should display existing decision from GET', () => {
    initWithExistingDecision({
      ...MOCK_RESPONSE,
      country: 'BELGIQUE',
      motif: 'ETUDES',
      duree: 'COURT_SEJOUR',
      recommendations: [{
        code: 'CARTE_A_ETUDES', label: 'Carte A Études', country: 'BELGIQUE',
        motif: 'ETUDES', conditions: 'Test', pieces: ['Passeport'], delaiMoyenJours: 90
      }]
    });

    expect(component.decision()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.country()).toBe('BELGIQUE');
  });

  // ---- SF-IM-05-04 : normalized code + nationaliteUe prefill ----

  it('should prefill motif from typeTitreSejourCode VLS_TS_ETUDIANT', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as any;
    initWithNoExistingDecision();
    expect(component.motif()).toBe('ETUDES');
  });

  it('should prefill motif TRAVAIL from PERMIS_UNIQUE', () => {
    component.aiData = { typeTitreSejourCode: 'PERMIS_UNIQUE' } as any;
    initWithNoExistingDecision();
    expect(component.motif()).toBe('TRAVAIL');
  });

  it('should prefill motif FAMILLE from CST_VPF', () => {
    component.aiData = { typeTitreSejourCode: 'CST_VPF' } as any;
    initWithNoExistingDecision();
    expect(component.motif()).toBe('FAMILLE');
  });

  it('should NOT prefill motif for CARTE_RESIDENT (generic title)', () => {
    component.aiData = { typeTitreSejourCode: 'CARTE_RESIDENT' } as any;
    initWithNoExistingDecision();
    // Default motif kept
    expect(component.motif()).toBe('TRAVAIL');
  });

  it('should prefill nationaliteUe from IA boolean', () => {
    component.aiData = { nationaliteUe: true } as any;
    initWithNoExistingDecision();
    expect(component.nationaliteUe()).toBe(true);
  });

  it('should fallback to typeTitreSejour heuristic when code absent', () => {
    component.aiData = { typeTitreSejour: 'Titre étudiant' } as any;
    initWithNoExistingDecision();
    expect(component.motif()).toBe('ETUDES');
  });

  it('should prefer code over heuristic text', () => {
    component.aiData = {
      typeTitreSejourCode: 'VLS_TS_SALARIE',
      typeTitreSejour: 'Titre étudiant', // conflicting text
    } as any;
    initWithNoExistingDecision();
    expect(component.motif()).toBe('TRAVAIL');
  });

  // SF-IM-05-04 : trigger_events prennent priorité sur le code titre courant
  // pour déduire le motif de la VOIE JURIDIQUE CIBLE (ex: Chen, pluriannuelle
  // Étudiant-Recherche + mariage avec Française → motif FAMILLE, pas ETUDES).
  describe('SF-IM-05-04 — prefill from trigger_events', () => {
    it('MARIAGE_RESSORTISSANT_FR → motif FAMILLE + situation MARIE (cas Chen)', () => {
      component.aiData = { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } as any;
      component.triggerEvents = [{ eventCode: 'MARIAGE_RESSORTISSANT_FR', eventDate: '2025-03-15' } as any];
      initWithNoExistingDecision();
      expect(component.motif()).toBe('FAMILLE');
      expect(component.situationFamiliale()).toBe('MARIE');
      expect(component.provenanceMotif()).toBe('IA');
      expect(component.provenanceSituationFamiliale()).toBe('IA');
    });

    it('PACS_RESSORTISSANT_FR → FAMILLE + PACS_COHABITATION', () => {
      component.triggerEvents = [{ eventCode: 'PACS_RESSORTISSANT_FR' } as any];
      initWithNoExistingDecision();
      expect(component.motif()).toBe('FAMILLE');
      expect(component.situationFamiliale()).toBe('PACS_COHABITATION');
    });

    it('NAISSANCE_ENFANT_FR → FAMILLE (pas de situation spécifique)', () => {
      component.triggerEvents = [{ eventCode: 'NAISSANCE_ENFANT_FR' } as any];
      initWithNoExistingDecision();
      expect(component.motif()).toBe('FAMILLE');
      expect(component.situationFamiliale()).toBeNull();
    });

    it('CDI_OBTENU_SALARIE → motif TRAVAIL', () => {
      component.triggerEvents = [{ eventCode: 'CDI_OBTENU_SALARIE' } as any];
      initWithNoExistingDecision();
      expect(component.motif()).toBe('TRAVAIL');
    });

    it('trigger prioritaire sur le code titre courant (Chen : titre étudiant mais mariage)', () => {
      // Sans trigger, CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE → ETUDES
      // Avec trigger MARIAGE, la voie cible est FAMILLE → doit l'emporter
      component.aiData = { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } as any;
      component.triggerEvents = [{ eventCode: 'MARIAGE_RESSORTISSANT_FR' } as any];
      initWithNoExistingDecision();
      expect(component.motif()).toBe('FAMILLE');
      expect(component.provenanceMotif()).toBe('IA');
    });

    it('pas de trigger → fallback sur code titre (comportement legacy)', () => {
      component.aiData = { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } as any;
      component.triggerEvents = [];
      initWithNoExistingDecision();
      expect(component.motif()).toBe('ETUDES');
    });

    it('onMotifChange clears provenance badge', () => {
      component.aiData = { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } as any;
      component.triggerEvents = [{ eventCode: 'MARIAGE_RESSORTISSANT_FR' } as any];
      initWithNoExistingDecision();
      expect(component.provenanceMotif()).toBe('IA');
      component.motif.set('TRAVAIL');
      component.onMotifChange();
      expect(component.provenanceMotif()).toBeNull();
      // Quitter FAMILLE efface aussi la situation familiale
      expect(component.situationFamiliale()).toBeNull();
      expect(component.provenanceSituationFamiliale()).toBeNull();
    });

    it('nouveaux codes sous-types mappés : CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE → ETUDES', () => {
      component.aiData = { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } as any;
      initWithNoExistingDecision();
      expect(component.motif()).toBe('ETUDES');
    });

    it('CST_VPF_CONJOINT_FR → FAMILLE', () => {
      component.aiData = { typeTitreSejourCode: 'CST_VPF_CONJOINT_FR' } as any;
      initWithNoExistingDecision();
      expect(component.motif()).toBe('FAMILLE');
    });
  });

  it('should do nothing when both aiData fields absent', () => {
    initWithNoExistingDecision();
    expect(component.motif()).toBe('TRAVAIL'); // default unchanged
    expect(component.nationaliteUe()).toBe(false);
  });

  // ---- Coherence alerts (SF-IA-03-09) ----

  function f96(statut: string, expectedValue: string, raison: string | null = null) {
    return { id: 'c', ordre: 0, description: 'point', statut, raison,
      critereCode: 'IM05_MOTIF', expectedValue } as any;
  }
  function question(answerText: string, expectedValue: string) {
    return { id: 'q', orderIndex: 0, questionText: 'Q?', answerText,
      critereCode: 'IM05_MOTIF', expectedValue } as any;
  }

  it('should alert MOTIF warning F96 when diverges', () => {
    component.procedureChecks = [f96('VERIFIED', 'ETUDES', 'Inscription université')];
    initWithNoExistingDecision();
    component.motif.set('TRAVAIL');
    const alert = component.coherenceAlerts().MOTIF;
    expect(alert?.source).toBe('F96');
    expect(alert?.expectedDisplay).toBe('ETUDES');
  });

  it('should alert MOTIF warning Question IA "oui"', () => {
    component.aiQuestions = [question('oui', 'FAMILLE')];
    initWithNoExistingDecision();
    component.motif.set('TRAVAIL');
    expect(component.coherenceAlerts().MOTIF?.source).toBe('QUESTION_IA');
  });

  it('should alert MOTIF warning IA via CODE_TO_MOTIF', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as any;
    // Override the prefill by setting motif manually different
    initWithNoExistingDecision();
    component.motif.set('TRAVAIL');
    expect(component.coherenceAlerts().MOTIF?.source).toBe('IA');
    expect(component.coherenceAlerts().MOTIF?.expectedDisplay).toBe('ETUDES');
  });

  it('should NOT alert when IA code is CARTE_RESIDENT (no mapping)', () => {
    component.aiData = { typeTitreSejourCode: 'CARTE_RESIDENT' } as any;
    initWithNoExistingDecision();
    component.motif.set('TRAVAIL');
    expect(component.coherenceAlerts().MOTIF).toBeUndefined();
  });

  it('should combine sources into MULTI when aligned', () => {
    component.procedureChecks = [f96('VERIFIED', 'ETUDES')];
    component.aiQuestions = [question('oui', 'ETUDES')];
    component.aiData = { typeTitreSejourCode: 'CARTE_A_ETUDES' } as any;
    initWithNoExistingDecision();
    component.motif.set('TRAVAIL');
    const alert = component.coherenceAlerts().MOTIF;
    expect(alert?.source).toBe('MULTI');
    expect(alert?.contributors).toEqual(expect.arrayContaining(['F96', 'QUESTION_IA', 'IA']));
  });

  // Nationalité
  it('should alert NATIONALITE_UE when IA diverges', () => {
    component.aiData = { nationaliteUe: true } as any;
    initWithNoExistingDecision();
    // After prefill, nationaliteUe = true ; now user flips to false
    component.nationaliteUe.set(false);
    const alert = component.coherenceAlerts().NATIONALITE_UE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('Ressortissant UE');
  });

  it('should NOT alert NATIONALITE_UE when matches', () => {
    component.aiData = { nationaliteUe: false } as any;
    initWithNoExistingDecision();
    component.nationaliteUe.set(false);
    expect(component.coherenceAlerts().NATIONALITE_UE).toBeUndefined();
  });

  it('should NOT alert NATIONALITE_UE when IA null', () => {
    component.aiData = { nationaliteUe: null } as any;
    initWithNoExistingDecision();
    component.nationaliteUe.set(true);
    expect(component.coherenceAlerts().NATIONALITE_UE).toBeUndefined();
  });

  it('should count alerts summary', () => {
    component.procedureChecks = [f96('VERIFIED', 'ETUDES')];
    component.aiData = { nationaliteUe: true } as any;
    initWithNoExistingDecision();
    component.motif.set('TRAVAIL');
    component.nationaliteUe.set(false);
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 0 });
  });

  it('should freeze alerts when decision is loaded', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT', nationaliteUe: true } as any;
    initWithExistingDecision(); // decision loaded, showForm=false
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should ignore expected_value outside enum', () => {
    component.procedureChecks = [f96('VERIFIED', 'UNKNOWN_MOTIF')];
    initWithNoExistingDecision();
    component.motif.set('TRAVAIL');
    expect(component.coherenceAlerts().MOTIF).toBeUndefined();
  });

  it('SF-IA-03-12: alertes actives après Résoudre → editForm → modification', () => {
    // Décision déjà chargée via GET, l'avocat clique Modifier pour corriger le motif,
    // un badge de cohérence doit réapparaître.
    component.procedureChecks = [f96('VERIFIED', 'TRAVAIL')];
    initWithExistingDecision();
    component.editCriteria();
    component.motif.set('ETUDES');
    expect(component.coherenceAlerts().MOTIF?.expectedDisplay).toBe('TRAVAIL');
  });

  it('SF-IA-03-12: pas d\'alertes quand decision affichée (showForm=false)', () => {
    component.procedureChecks = [f96('VERIFIED', 'TRAVAIL')];
    initWithExistingDecision();
    expect(component.showForm()).toBe(false);
    expect(component.coherenceAlerts().MOTIF).toBeUndefined();
  });

  it('SF-IA-03-14: pièce manquante IM05_MOTIF devient contributor PIECE_MANQUANTE', () => {
    // IA détecte VLS_TS_SALARIE → motif TRAVAIL ; pièce manquante IM05_MOTIF ; user saisit ETUDES
    component.aiData = { typeTitreSejourCode: 'VLS_TS_SALARIE' } as any;
    component.piecesManquantes = [{ texte: 'Contrat de travail', critereCode: 'IM05_MOTIF' }];
    initWithNoExistingDecision();
    component.motif.set('ETUDES');
    const alert = component.coherenceAlerts().MOTIF;
    expect(alert).toBeTruthy();
    expect(alert?.contributors).toContain('PIECE_MANQUANTE');
    expect(alert?.source).toBe('MULTI');
  });
});

// F-177 SF-177-12 — couvre le static `getPrefillCount` exposé pour la card du panel.
describe('ImmigrationTitleDecisionSectionComponent.getPrefillCount', () => {
  it('returns 0 when neither aiData nor triggerEvents present', () => {
    expect(ImmigrationTitleDecisionSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only typeTitreSejourCode is present', () => {
    expect(
      ImmigrationTitleDecisionSectionComponent.getPrefillCount({
        aiData: { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' },
      }),
    ).toBe(1);
  });

  it('returns 3 for the Chen 5 case (nationaliteUe + typeTitreSejourCode + MARIAGE_RESSORTISSANT_FR trigger)', () => {
    expect(
      ImmigrationTitleDecisionSectionComponent.getPrefillCount({
        aiData: {
          nationaliteUe: false,
          typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE',
        },
        triggerEvents: [{ eventCode: 'MARIAGE_RESSORTISSANT_FR' }],
      }),
    ).toBe(3);
  });

  it('returns 2 for a trigger event with no situationFamiliale (NAISSANCE_ENFANT_FR) + nationaliteUe', () => {
    expect(
      ImmigrationTitleDecisionSectionComponent.getPrefillCount({
        aiData: { nationaliteUe: true },
        triggerEvents: [{ eventCode: 'NAISSANCE_ENFANT_FR' }],
      }),
    ).toBe(2); // motif (FAMILLE) + nationaliteUe
  });

  it('accepts both eventCode and event_code field conventions', () => {
    expect(
      ImmigrationTitleDecisionSectionComponent.getPrefillCount({
        triggerEvents: [{ event_code: 'MARIAGE_RESSORTISSANT_FR' }],
      }),
    ).toBe(2); // motif + situationFamiliale (no nationaliteUe → no aiData)
  });
});

// F-192 SF-192-02 — sortie outil enrichie (badge top-1 + bloc pistes
// retenues non recommandées) + helper getRetainedPistesBadge.
describe('ImmigrationTitleDecisionSectionComponent — F-192 SF-192-02', () => {
  let component: ImmigrationTitleDecisionSectionComponent;
  let fixture: ComponentFixture<ImmigrationTitleDecisionSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '11111111-1111-1111-1111-111111111111';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/immigration/title-decision`;
  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    country: 'FRANCE',
    nationaliteUe: false,
    motif: 'TRAVAIL',
    duree: 'LONG_SEJOUR',
    situationFamiliale: null,
    recommendations: [{
      code: 'CARTE_PLURIANNUELLE_PASSEPORT_TALENT',
      label: 'Passeport talent — Chercheur',
      country: 'FRANCE',
      motif: 'TRAVAIL',
      conditions: 'Doctorat',
      pieces: ['Diplôme'],
      delaiMoyenJours: 90,
    }],
  };

  const PISTE_ALIGNED = {
    pisteId: 'p1',
    texte: 'Demander Passeport Talent — Chercheur',
    baseJuridique: 'L.421-14 CESEDA',
    horizonTemporel: 'Court terme',
    conditions: ['Doctorat obtenu'],
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
    matchStatus: 'ALIGNED' as const,
  };
  const PISTE_DIVERGENT = {
    pisteId: 'p2',
    texte: 'Tenter régularisation 10 ans',
    baseJuridique: 'L.435-1 CESEDA',
    horizonTemporel: 'Long terme',
    conditions: ['Présence > 10 ans'],
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
    matchStatus: 'DIVERGENT' as const,
  };
  const PISTE_NOT_ANALYZED = {
    pisteId: 'p3',
    texte: 'Demander VLS-TS Salarié',
    baseJuridique: null,
    horizonTemporel: null,
    conditions: [],
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
    matchStatus: 'NOT_ANALYZED' as const,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImmigrationTitleDecisionSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ImmigrationTitleDecisionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  function initWithDecision(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(MOCK_RESPONSE);
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
    component.collapsed.set(false);
    fixture.detectChanges();
  }

  it('CA-04 sans pistes : aucun bloc supplémentaire ni badge or', () => {
    component.pistesRetenues = [];
    initWithDecision();
    const native: HTMLElement = fixture.nativeElement;
    expect(native.querySelector('[data-testid="retained-piste-aligned-badge"]')).toBeNull();
    expect(native.querySelector('[data-testid="retained-pistes-divergent-block"]')).toBeNull();
  });

  it('CA-02 convergence ALIGNED : badge or « Retenu par vous » sur top-1 + pas de bloc divergent', () => {
    component.pistesRetenues = [PISTE_ALIGNED];
    initWithDecision();
    expect(component.hasRetainedPistesAligned()).toBe(true);
    const native: HTMLElement = fixture.nativeElement;
    expect(native.querySelector('[data-testid="retained-piste-aligned-badge"]')).not.toBeNull();
    expect(native.querySelector('[data-testid="retained-pistes-divergent-block"]')).toBeNull();
  });

  it('CA-03 divergence DIVERGENT : bloc « Stratégies retenues par vous (non recommandées) » rendu', () => {
    component.pistesRetenues = [PISTE_DIVERGENT];
    initWithDecision();
    const native: HTMLElement = fixture.nativeElement;
    const block = native.querySelector('[data-testid="retained-pistes-divergent-block"]');
    expect(block).not.toBeNull();
    expect(block!.textContent).toContain('Tenter régularisation 10 ans');
    expect(block!.textContent).toContain('L.435-1 CESEDA');
    expect(block!.textContent).toContain("Cette stratégie n'apparaît pas dans le top recommandé");
  });

  it('CA-05 NOT_ANALYZED : mention « Outil pas encore lancé »', () => {
    component.pistesRetenues = [PISTE_NOT_ANALYZED];
    initWithDecision();
    const native: HTMLElement = fixture.nativeElement;
    const block = native.querySelector('[data-testid="retained-pistes-divergent-block"]');
    expect(block).not.toBeNull();
    expect(block!.textContent).toContain('Outil pas encore lancé');
  });

  // getRetainedPistesBadge static helper (pattern miroir getPrefillCount).
  it('getRetainedPistesBadge — 0 piste → kind=none, count=0', () => {
    expect(ImmigrationTitleDecisionSectionComponent.getRetainedPistesBadge({})).toEqual({
      kind: 'none', count: 0,
    });
  });

  it('getRetainedPistesBadge — 2 ALIGNED → kind=aligned, count=2', () => {
    expect(
      ImmigrationTitleDecisionSectionComponent.getRetainedPistesBadge({
        pistesRetenues: [PISTE_ALIGNED, { ...PISTE_ALIGNED, pisteId: 'pX' }],
      }),
    ).toEqual({ kind: 'aligned', count: 2 });
  });

  it('getRetainedPistesBadge — 1 ALIGNED + 1 DIVERGENT → kind=divergent (priorité), count=2', () => {
    expect(
      ImmigrationTitleDecisionSectionComponent.getRetainedPistesBadge({
        pistesRetenues: [PISTE_ALIGNED, PISTE_DIVERGENT],
      }),
    ).toEqual({ kind: 'divergent', count: 2 });
  });

  it('getRetainedPistesBadge — pistes mappées sur autre toolId ignorées', () => {
    expect(
      ImmigrationTitleDecisionSectionComponent.getRetainedPistesBadge({
        pistesRetenues: [{ ...PISTE_ALIGNED, toolIdCible: 'F-IM-06-recours' }],
      }),
    ).toEqual({ kind: 'none', count: 0 });
  });
});
