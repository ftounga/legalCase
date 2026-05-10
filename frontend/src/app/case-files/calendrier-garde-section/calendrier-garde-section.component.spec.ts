import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { CalendrierGardeSectionComponent } from './calendrier-garde-section.component';

describe('CalendrierGardeSectionComponent', () => {
  let component: CalendrierGardeSectionComponent;
  let fixture: ComponentFixture<CalendrierGardeSectionComponent>;
  let httpMock: HttpTestingController;
  const ID = '88888888-8888-8888-8888-888888888888';
  const URL = `/api/v1/case-files/${ID}/calendrier-garde`;
  const MOCK = { caseFileId: ID, gardeCode: 'ALTERNEE_FR', gardeLabel: 'Résidence alternée', country: 'FRANCE',
    parentANom: 'Marie', parentBNom: 'Pierre', repartitionType: 'ALTERNEE_1_SUR_2',
    semaineTypeParentA: ['Semaine A'], semaineTypeParentB: ['Semaine B'],
    vacancesRegle: 'Moitié', joursParAnParentA: 182, joursParAnParentB: 183,
    baseJuridique: 'Art 373-2-9', commentaire: 'Test' };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CalendrierGardeSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CalendrierGardeSectionComponent);
    component = fixture.componentInstance; component.caseFileId = ID;
  });
  afterEach(() => { httpMock.verify(); });

  function flushSE(): void {
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
  }
  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); flushSE(); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); flushSE(); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show form when no existing', () => { initNo(); expect(component.showForm()).toBe(true); });
  it('should call POST', () => {
    initNo(); component.parentANom.set('A'); component.parentBNom.set('B'); component.generate();
    const r = httpMock.expectOne(req => req.url === URL && req.method === 'POST');
    r.flush(MOCK);
    expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false);
  });
  it('should display existing', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false); });

  // ---- AI prefill mode_garde_detaille (SF-FA-06-04 + SF-155-18) ----

  it('should prefill gardeCode from IA when mode matches workspace country', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'DVH_ELARGI_FR';
    initNo();
    expect(component.gardeCode()).toBe('DVH_ELARGI_FR');
    expect(component.modeDetailleNote()).toBeNull();
    expect(component.provenanceGardeCode()).toBe('IA');
  });

  it('should prefill gardeCode for BELGIQUE workspace', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiModeGardeDetaille = 'SECONDAIRE_ELARGI_BE';
    initNo();
    expect(component.gardeCode()).toBe('SECONDAIRE_ELARGI_BE');
    expect(component.provenanceGardeCode()).toBe('IA');
  });

  it('should set note when IA mode is from opposite country', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'ALTERNEE_BE';
    initNo();
    // Default FR preserved, note shown, no IA badge.
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.modeDetailleNote()).toContain('ALTERNEE_BE');
    expect(component.provenanceGardeCode()).toBeNull();
  });

  it('should ignore invalid IA value', () => {
    component.aiModeGardeDetaille = 'UNKNOWN_MODE' as any;
    initNo();
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.modeDetailleNote()).toBeNull();
    expect(component.provenanceGardeCode()).toBeNull();
  });

  it('should NOT override saved result', () => {
    component.aiModeGardeDetaille = 'DVH_ELARGI_FR';
    initWith();
    // Existing result loaded — prefill not applied; provenance reset to null.
    expect(component.showForm()).toBe(false);
    expect(component.provenanceGardeCode()).toBeNull();
  });

  it('should clear note and IA provenance when user changes gardeCode', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'ALTERNEE_BE';
    initNo();
    expect(component.modeDetailleNote()).not.toBeNull();
    component.onGardeCodeChange();
    expect(component.modeDetailleNote()).toBeNull();
    expect(component.provenanceGardeCode()).toBeNull();
  });

  it('should clear IA provenance when user changes gardeCode after prefill', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'DVH_CLASSIQUE_FR';
    initNo();
    expect(component.provenanceGardeCode()).toBe('IA');
    component.onGardeCodeChange();
    expect(component.provenanceGardeCode()).toBeNull();
  });

  it('should do nothing when aiModeGardeDetaille is absent', () => {
    initNo();
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.modeDetailleNote()).toBeNull();
    expect(component.provenanceGardeCode()).toBeNull();
  });

  // ---- Coherence alerts (SF-IA-03-07 + SF-155-18) ----

  function f96(statut: string, expectedValue: string, raison: string | null = null) {
    return { id: 'c', ordre: 0, description: 'point', statut, raison,
      critereCode: 'FA06_MODE_GARDE', expectedValue } as any;
  }
  function question(answerText: string, expectedValue: string) {
    return { id: 'q', orderIndex: 0, questionText: 'Q?', answerText,
      critereCode: 'FA06_MODE_GARDE', expectedValue } as any;
  }
  function synthesisWith(modeGarde: string | null, modeGardeDetaille: string | null = null) {
    return { pensionAlimentaireEstimate: { modeGarde, modeGardeDetaille } } as any;
  }

  it('should alert F96 on mode mismatch (single source)', () => {
    component.procedureChecks = [f96('VERIFIED', 'DVH_ELARGI_FR', 'Convention')];
    initNo();
    component.gardeCode.set('ALTERNEE_FR');
    const alert = component.coherenceAlerts().MODE_GARDE;
    expect(alert?.source).toBe('F96');
    expect(alert?.expectedDisplay).toBe('DVH_ELARGI_FR');
  });

  it('should alert Question IA on mode mismatch', () => {
    component.aiQuestions = [question('oui', 'SECONDAIRE_BE')];
    initNo();
    component.gardeCode.set('ALTERNEE_BE');
    const alert = component.coherenceAlerts().MODE_GARDE;
    expect(alert?.source).toBe('QUESTION_IA');
    expect(alert?.expectedDisplay).toBe('SECONDAIRE_BE');
  });

  it('should alert IA modeGardeDetaille on mismatch', () => {
    component.synthesis = synthesisWith('ALTERNEE', 'ALTERNEE_FR');
    initNo();
    component.gardeCode.set('DVH_CLASSIQUE_FR');
    const alert = component.coherenceAlerts().MODE_GARDE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('ALTERNEE_FR');
  });

  it('should NOT alert when IA detailed mode matches user', () => {
    component.synthesis = synthesisWith('ALTERNEE', 'ALTERNEE_FR');
    initNo();
    component.gardeCode.set('ALTERNEE_FR');
    expect(component.coherenceAlerts().MODE_GARDE).toBeUndefined();
  });

  it('should alert IA coarse fallback when IA category mismatches', () => {
    component.synthesis = synthesisWith('ALTERNEE', null);
    initNo();
    component.gardeCode.set('DVH_CLASSIQUE_FR');
    const alert = component.coherenceAlerts().MODE_GARDE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('mode alterné');
  });

  it('should NOT alert when IA coarse category matches', () => {
    component.synthesis = synthesisWith('ALTERNEE', null);
    initNo();
    component.gardeCode.set('ALTERNEE_FR');
    expect(component.coherenceAlerts().MODE_GARDE).toBeUndefined();
  });

  it('should NOT alert when IA coarse EXCLUSIVE matches non-alternée user', () => {
    component.synthesis = synthesisWith('EXCLUSIVE', null);
    initNo();
    component.gardeCode.set('DVH_ELARGI_FR');
    expect(component.coherenceAlerts().MODE_GARDE).toBeUndefined();
  });

  it('should consolidate sources to MULTI when they align against user', () => {
    component.procedureChecks = [f96('VERIFIED', 'DVH_CLASSIQUE_FR')];
    component.aiQuestions = [question('oui', 'DVH_CLASSIQUE_FR')];
    component.synthesis = synthesisWith('EXCLUSIVE', 'DVH_CLASSIQUE_FR');
    initNo();
    component.gardeCode.set('ALTERNEE_FR');
    const alert = component.coherenceAlerts().MODE_GARDE;
    expect(alert?.source).toBe('MULTI');
    expect(alert?.contributors).toEqual(expect.arrayContaining(['F96', 'QUESTION_IA', 'IA']));
  });

  it('should NOT alert when no source available', () => {
    initNo();
    expect(component.coherenceAlerts().MODE_GARDE).toBeUndefined();
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should ignore unknown expected_value', () => {
    component.procedureChecks = [f96('VERIFIED', 'UNKNOWN_MODE')];
    initNo();
    component.gardeCode.set('ALTERNEE_FR');
    expect(component.coherenceAlerts().MODE_GARDE).toBeUndefined();
  });

  it('should freeze alert once result loaded', () => {
    component.synthesis = synthesisWith('EXCLUSIVE', 'DVH_CLASSIQUE_FR');
    initWith();
    // Result loaded, form hidden, no alert even if mismatch
    expect(component.coherenceAlerts().MODE_GARDE).toBeUndefined();
  });

  it('should ignore Question IA "non" on enum critere', () => {
    component.aiQuestions = [question('non', 'ALTERNEE_FR')];
    initNo();
    component.gardeCode.set('DVH_CLASSIQUE_FR');
    expect(component.coherenceAlerts().MODE_GARDE).toBeUndefined();
  });

  it('SF-118-04: GET 200 pré-remplit les signals du formulaire depuis la réponse sauvegardée', () => {
    initWith();
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.parentANom()).toBe('Marie');
    expect(component.parentBNom()).toBe('Pierre');
  });

  it('SF-118-04: editForm restaure les signals depuis le résultat', () => {
    initWith();
    expect(component.showForm()).toBe(false);
    component.gardeCode.set('DVH_CLASSIQUE_FR');
    component.parentANom.set('');
    component.parentBNom.set('');
    component.editForm();
    expect(component.showForm()).toBe(true);
    expect(component.gardeCode()).toBe('ALTERNEE_FR');
    expect(component.parentANom()).toBe('Marie');
    expect(component.parentBNom()).toBe('Pierre');
  });

  it('SF-IA-03-14: pièce manquante FA06_MODE_GARDE devient contributor PIECE_MANQUANTE', () => {
    // IA modeGardeDetaille ALTERNEE_FR + pièce manquante ; user saisit DVH_CLASSIQUE_FR
    component.synthesis = synthesisWith('ALTERNEE', 'ALTERNEE_FR');
    component.piecesManquantes = [{ texte: 'Jugement de garde', critereCode: 'FA06_MODE_GARDE' }];
    initNo();
    component.gardeCode.set('DVH_CLASSIQUE_FR');
    const alert = component.coherenceAlerts().MODE_GARDE;
    expect(alert?.contributors).toContain('PIECE_MANQUANTE');
    expect(alert?.source).toBe('MULTI');
    expect(alert?.pieceTexte).toBe('Jugement de garde');
  });

  it('SF-155-18: prefill via ngOnInit (avant loadExisting) puis confirmé via error 404', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiModeGardeDetaille = 'DVH_CLASSIQUE_FR';
    initNo();
    expect(component.gardeCode()).toBe('DVH_CLASSIQUE_FR');
    expect(component.provenanceGardeCode()).toBe('IA');
  });

  // F-IA-04 / F-177 SF-177-12 — couvre le static getPrefillCount exposé pour la card du panel.
  it('getPrefillCount — 0 si synthesis absent', () => {
    expect(CalendrierGardeSectionComponent.getPrefillCount({})).toBe(0);
    expect(CalendrierGardeSectionComponent.getPrefillCount({ synthesis: null })).toBe(0);
    expect(CalendrierGardeSectionComponent.getPrefillCount({ synthesis: {} })).toBe(0);
  });

  it('getPrefillCount — 0 si pensionAlimentaireEstimate.modeGardeDetaille absent', () => {
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: {} },
    })).toBe(0);
  });

  it('getPrefillCount — 1 si mode FR et workspace FR (cas nominal)', () => {
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'ALTERNEE_FR' } },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'DVH_CLASSIQUE_FR' } },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('getPrefillCount — 1 si mode BE et workspace BE (cas nominal Mengue)', () => {
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'ALTERNEE_BE' } },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'SECONDAIRE_BE' } },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('getPrefillCount — 0 si mode pays opposé (note informative seulement, pas de prefill)', () => {
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'ALTERNEE_FR' } },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'ALTERNEE_BE' } },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('getPrefillCount — 0 si mode non reconnu', () => {
    expect(CalendrierGardeSectionComponent.getPrefillCount({
      synthesis: { pensionAlimentaireEstimate: { modeGardeDetaille: 'UNKNOWN_MODE' } },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
