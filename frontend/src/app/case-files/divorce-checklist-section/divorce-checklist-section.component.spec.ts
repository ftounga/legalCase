import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { SimpleChange } from '@angular/core';
import { DivorceChecklistSectionComponent } from './divorce-checklist-section.component';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

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

  function flushSE(): void {
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
  }
  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); flushSE(); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); flushSE(); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show init when no existing', () => { initNo(); expect(component.result()).toBeNull(); });
  it('should display existing', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.progress()).toBe(0); });
  it('should toggle etape and save', () => {
    initWith();
    component.toggleEtape(component.result()!.etapes[0]);
    const req = httpMock.expectOne(r => r.url === URL && r.method === 'POST');
    req.flush({ ...MOCK, etapesCompletees: 1, etapes: [{ ...MOCK.etapes[0], statut: 'FAIT' }] });
    expect(component.result()!.etapesCompletees).toBe(1);
  });

  // ---- Coherence alerts (SF-IA-03-06 / SF-155-19 — migré CoherenceAlertBuilder) ----

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
    flushSE();
  }

  // ÉTAPES
  it('should alert CRITICAL on step FAIT + F-96 NON_COMPLIANT', () => {
    component.procedureChecks = [f96('NON_COMPLIANT', 'FR_CHOIX_AVOCATS', 'Pas fait')];
    withResult([{ ...MOCK.etapes[0], statut: 'FAIT' }]);
    const alert = component.coherenceAlerts()['FR_CHOIX_AVOCATS'];
    expect(alert?.severity).toBe('CRITICAL');
    expect(alert?.source).toBe('F96');
  });

  it('should alert WARNING on step A_FAIRE + F-96 VERIFIED', () => {
    component.procedureChecks = [f96('VERIFIED', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'A_FAIRE' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']?.severity).toBe('WARNING');
  });

  it('should NOT alert on step FAIT + F-96 VERIFIED (concordance)', () => {
    component.procedureChecks = [f96('VERIFIED', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'FAIT' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']).toBeUndefined();
  });

  it('should alert CRITICAL on step FAIT + Question IA "non"', () => {
    component.aiQuestions = [question('non, pas encore', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'FAIT' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']?.severity).toBe('CRITICAL');
  });

  it('should alert WARNING on step A_FAIRE + Question IA "oui"', () => {
    component.aiQuestions = [question('oui, déjà choisis', 'FR_CHOIX_AVOCATS')];
    withResult([{ ...MOCK.etapes[0], statut: 'A_FAIRE' }]);
    expect(component.coherenceAlerts()['FR_CHOIX_AVOCATS']?.severity).toBe('WARNING');
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
  it('should alert WARNING on piece PRESENTE + pieces_manquantes IA (PIECE_MANQUANTE source)', () => {
    component.piecesManquantes = [piece('FR_ACTE_MARIAGE', 'acte de mariage manquant')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'PRESENTE' }]);
    const alert = component.coherenceAlerts()['FR_ACTE_MARIAGE'];
    expect(alert?.severity).toBe('WARNING');
    expect(alert?.source).toBe('PIECE_MANQUANTE');
    expect(alert?.pieceTexte).toBe('acte de mariage manquant');
  });

  it('should NOT alert on piece MANQUANTE + pieces_manquantes IA (concordance)', () => {
    component.piecesManquantes = [piece('FR_ACTE_MARIAGE')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'MANQUANTE' }]);
    expect(component.coherenceAlerts()['FR_ACTE_MARIAGE']).toBeUndefined();
  });

  it('should alert CRITICAL on piece PRESENTE + F-96 NON_COMPLIANT', () => {
    component.procedureChecks = [f96('NON_COMPLIANT', 'FR_ACTE_MARIAGE')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'PRESENTE' }]);
    expect(component.coherenceAlerts()['FR_ACTE_MARIAGE']?.severity).toBe('CRITICAL');
  });

  it('should alert WARNING on piece MANQUANTE + F-96 VERIFIED', () => {
    component.procedureChecks = [f96('VERIFIED', 'FR_ACTE_MARIAGE')];
    withResult([], [{ ...MOCK.pieces[0], statut: 'MANQUANTE' }]);
    expect(component.coherenceAlerts()['FR_ACTE_MARIAGE']?.severity).toBe('WARNING');
  });

  it('should combine PIECE_MANQUANTE + F-96 into MULTI', () => {
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

  // ---- SF-155-19 : Pré-fill IA (FamilleExtractedData.dateAcceptationPV) ----

  function withFrSignatureEtape(status: string = 'A_FAIRE') {
    return {
      ...MOCK,
      etapes: [
        { code: 'FR_SIGNATURE_CONVENTION', label: 'Signature convention', ordre: 5,
          description: 'Signature par les époux', delai: '—', obligatoire: true, statut: status },
        { ...MOCK.etapes[0] },
      ],
      etapesTotal: 2,
    };
  }

  it('should prefill FR_SIGNATURE_CONVENTION as FAIT when aiData.dateAcceptationPV is set', () => {
    component.aiData = { dateAcceptationPV: '2026-04-15' };
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(withFrSignatureEtape('A_FAIRE'));
    flushSE();
    const etape = component.result()!.etapes.find(e => e.code === 'FR_SIGNATURE_CONVENTION');
    expect(etape?.statut).toBe('FAIT');
    expect(component.isPrefilledFromAi('FR_SIGNATURE_CONVENTION')).toBe(true);
  });

  it('should NOT overwrite existing FAIT status when prefilling', () => {
    component.aiData = { dateAcceptationPV: '2026-04-15' };
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(withFrSignatureEtape('FAIT'));
    flushSE();
    // Étape déjà FAIT → pas de provenance IA (saisie avocat préservée).
    expect(component.isPrefilledFromAi('FR_SIGNATURE_CONVENTION')).toBe(false);
  });

  it('should NOT prefill when aiData.dateAcceptationPV is null/absent', () => {
    component.aiData = { dateAcceptationPV: null };
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(withFrSignatureEtape('A_FAIRE'));
    flushSE();
    const etape = component.result()!.etapes.find(e => e.code === 'FR_SIGNATURE_CONVENTION');
    expect(etape?.statut).toBe('A_FAIRE');
    expect(component.isPrefilledFromAi('FR_SIGNATURE_CONVENTION')).toBe(false);
  });

  it('should clear provenance IA when avocat re-toggles a prefilled etape', () => {
    component.aiData = { dateAcceptationPV: '2026-04-15' };
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(withFrSignatureEtape('A_FAIRE'));
    flushSE();
    expect(component.isPrefilledFromAi('FR_SIGNATURE_CONVENTION')).toBe(true);

    const etape = component.result()!.etapes.find(e => e.code === 'FR_SIGNATURE_CONVENTION')!;
    component.onEtapeStatutChange(etape);
    httpMock.expectOne(r => r.url === URL && r.method === 'POST').flush({
      ...withFrSignatureEtape('A_FAIRE'),
      etapes: component.result()!.etapes,
    });
    expect(component.isPrefilledFromAi('FR_SIGNATURE_CONVENTION')).toBe(false);
  });

  it('should re-prefill when aiData input changes after first resolution', () => {
    component.aiData = null;
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(withFrSignatureEtape('A_FAIRE'));
    flushSE();
    expect(component.isPrefilledFromAi('FR_SIGNATURE_CONVENTION')).toBe(false);

    component.aiData = { dateAcceptationPV: '2026-04-20' };
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.isPrefilledFromAi('FR_SIGNATURE_CONVENTION')).toBe(true);
  });

  it('should clear provenance IA on onPieceStatutChange', () => {
    // Setup : on simule manuellement un état "pièce pré-remplie" via le signal.
    component.aiData = { dateAcceptationPV: '2026-04-15' };
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(withFrSignatureEtape('A_FAIRE'));
    flushSE();
    // Forcer une provenance IA pour la pièce pour tester onPieceStatutChange.
    (component as any).provenanceByCode.set({ FR_ACTE_MARIAGE: 'IA' });
    expect(component.isPrefilledFromAi('FR_ACTE_MARIAGE')).toBe(true);

    const piece = component.result()!.pieces.find(p => p.code === 'FR_ACTE_MARIAGE')!;
    component.onPieceStatutChange(piece);
    httpMock.expectOne(r => r.url === URL && r.method === 'POST').flush(withFrSignatureEtape('A_FAIRE'));
    expect(component.isPrefilledFromAi('FR_ACTE_MARIAGE')).toBe(false);
  });

  it('should expose provenanceByCode signal as record', () => {
    initNo();
    expect(component.provenanceByCode()).toEqual({});
  });

  // F-IA-04 / F-177 SF-177-12 — couvre le static getPrefillCount exposé pour la card du panel.
  it('getPrefillCount — 0 si aiData absent', () => {
    expect(DivorceChecklistSectionComponent.getPrefillCount({})).toBe(0);
    expect(DivorceChecklistSectionComponent.getPrefillCount({ aiData: null })).toBe(0);
    expect(DivorceChecklistSectionComponent.getPrefillCount({ aiData: {} as FamilleExtractedData })).toBe(0);
  });

  it('getPrefillCount — 0 si dateAcceptationPV format invalide', () => {
    expect(DivorceChecklistSectionComponent.getPrefillCount({
      aiData: { dateAcceptationPV: '01/01/2026' } as FamilleExtractedData,
    })).toBe(0);
    expect(DivorceChecklistSectionComponent.getPrefillCount({
      aiData: { dateAcceptationPV: '' } as FamilleExtractedData,
    })).toBe(0);
  });

  it('getPrefillCount — 1 si dateAcceptationPV YYYY-MM-DD valide (cas nominal)', () => {
    expect(DivorceChecklistSectionComponent.getPrefillCount({
      aiData: { dateAcceptationPV: '2026-01-15' } as FamilleExtractedData,
    })).toBe(1);
  });
});
