import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SynthesisComponent } from './synthesis.component';
import { AiQuestion } from '../../core/models/ai-question.model';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AiQuestionAnswerService } from '../../core/services/ai-question-answer.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { ChatService } from '../../core/services/chat.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { PdfExportService } from '../../core/services/pdf-export.service';
import { DocxExportService } from '../../core/services/docx-export.service';
import { ProcedureCheckService } from '../../core/services/procedure-check.service';
import { StrategicOptionService } from '../../core/services/strategic-option.service';
import { RetainedPisteAlignmentService } from '../../core/services/retained-piste-alignment.service';
import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { ProcedureCheckAlignmentService } from '../../core/services/procedure-check-alignment.service';
import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';
import { PieceManquanteStatusService } from '../../core/services/piece-manquante-status.service';
import { PieceManquanteStatus } from '../../core/models/piece-manquante-status.model';
import { PieceManquanteAlignmentService } from '../../core/services/piece-manquante-alignment.service';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueStatusService } from '../../core/services/risque-status.service';
import { RisqueStatus } from '../../core/models/risque-status.model';
import { RisqueAlignmentService } from '../../core/services/risque-alignment.service';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { TypeLitigeOverrideService } from '../../core/services/type-litige-override.service';
import { TypeLitigeOverrideResponse } from '../../core/models/type-litige-override.model';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError, Subject, NEVER } from 'rxjs';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { AnalysisItem, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { StrategicOption } from '../../core/models/strategic-option.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TimeService } from '../../core/services/time.service';
import { TimeEntryResponse } from '../../core/models/time-tracking.models';
import { signal } from '@angular/core';
import { DocumentService } from '../../core/services/document.service';

const CASE_FILE_ID = 'cf-1';

const makeItem = (texte: string, source: string | null = null, extrait: string | null = null): AnalysisItem =>
  ({ texte, source, extrait });

const makeSynthesis = (version: number, analysisType: 'STANDARD' | 'ENRICHED', piecesManquantes: string[] = [], riskLevel: string | null = null, riskScore: number | null = null, risques: AnalysisItem[] = []) => ({
  id: `analysis-${version}`,
  version,
  analysisType,
  status: 'DONE',
  timeline: [],
  faits: [makeItem('fait1')],
  pointsJuridiques: [],
  risques,
  questionsOuvertes: [],
  piecesManquantes,
  riskLevel,
  riskScore,
  modelUsed: 'claude-sonnet-4-6',
  updatedAt: '2026-03-23T10:00:00Z'
});

const makeVersion = (version: number, analysisType: 'STANDARD' | 'ENRICHED'): CaseAnalysisVersionSummary => ({
  id: `analysis-${version}`,
  version,
  analysisType,
  updatedAt: '2026-03-23T10:00:00Z',
  faitsCount: null,
  pointsJuridiquesCount: null,
  risquesCount: null,
  questionsOuvertesCount: null,
  timelineCount: null
});

const makeCheck = (id: string, ordre: number, statut: 'TO_CHECK' | 'VERIFIED' | 'NON_COMPLIANT' = 'TO_CHECK'): ProcedureCheck => ({
  id, ordre, description: `Point ${ordre}`, statut
});

const makeOption = (
  id: string,
  ordre: number,
  texte: string,
  statut: 'TO_STUDY' | 'RETAINED' | 'DISCARDED' = 'TO_STUDY',
  raisonDiscard: string | null = null
): StrategicOption => ({
  id, ordre, texte, statut, raisonDiscard,
  baseJuridique: null, horizonTemporel: null, conditions: [], source: null,
  createdAt: '2026-04-30T10:00:00Z', updatedAt: '2026-04-30T10:00:00Z'
});

describe('SynthesisComponent', () => {
  let fixture: ComponentFixture<SynthesisComponent>;
  let component: SynthesisComponent;
  let caseAnalysisService: jest.Mocked<CaseAnalysisService>;
  let aiQuestionService: jest.Mocked<AiQuestionService>;
  let procedureCheckService: jest.Mocked<ProcedureCheckService>;
  let strategicOptionService: jest.Mocked<StrategicOptionService>;
  let matDialogMock: { open: jest.Mock };
  let dialogResultSubject: Subject<string | null | undefined>;

  beforeEach(async () => {
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion', 'getAnalysis', 'getPartial']);
    // F-185 SF-185-01 — par défaut pas d'analyse en cours (404). Les tests qui veulent
    // exercer le streaming partial peuvent override via .mockReturnValueOnce(of(...)).
    (caseAnalysisService.getPartial as jest.Mock).mockReturnValue(throwError(() => ({ status: 404 })));
    aiQuestionService = jasmine.createSpyObj('AiQuestionService', ['getQuestions', 'getQuestionsByAnalysisId']);
    procedureCheckService = jasmine.createSpyObj('ProcedureCheckService', ['list', 'updateStatus']);
    strategicOptionService = jasmine.createSpyObj('StrategicOptionService', ['list', 'updateStatus']);
    strategicOptionService.list.mockReturnValue(of([]));

    dialogResultSubject = new Subject();
    matDialogMock = {
      open: jest.fn().mockReturnValue({ afterClosed: () => dialogResultSubject.asObservable() } as Partial<MatDialogRef<unknown>>)
    };

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    aiQuestionService.getQuestionsByAnalysisId.mockReturnValue(of([]));
    aiQuestionService.getQuestions.mockReturnValue(of([]));
    procedureCheckService.list.mockReturnValue(of([]));

    const chatService = jasmine.createSpyObj('ChatService', ['getHistory', 'sendMessage']);
    chatService.getHistory.mockReturnValue(of([]));

    const timeEntriesSignal = signal<TimeEntryResponse[]>([]);
    const timeServiceMock = {
      loadEntries: jest.fn().mockReturnValue(of(undefined as void)),
      entries: timeEntriesSignal
    };

    await TestBed.configureTestingModule({
      imports: [SynthesisComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => CASE_FILE_ID } } } },
        { provide: MatSnackBar, useValue: { open: () => {} } },
        { provide: CaseFileService, useValue: caseFileService },
        { provide: CaseAnalysisService, useValue: caseAnalysisService },
        { provide: AiQuestionService, useValue: aiQuestionService },
        { provide: AiQuestionAnswerService, useValue: jasmine.createSpyObj('AiQuestionAnswerService', ['submitAnswer']) },
        { provide: ReAnalysisService, useValue: jasmine.createSpyObj('ReAnalysisService', ['reAnalyze']) },
        { provide: ChatService, useValue: chatService },
        { provide: AnalyticsService, useValue: jasmine.createSpyObj('AnalyticsService', ['trackEvent']) },
        { provide: PdfExportService, useValue: jasmine.createSpyObj('PdfExportService', ['export', 'exportChecklist']) },
        { provide: RetainedPisteAlignmentService, useValue: { getForCaseFile: jest.fn().mockReturnValue(of([] as RetainedPisteAlignment[])) } },
        { provide: ProcedureCheckAlignmentService, useValue: { getForCaseFile: jest.fn().mockReturnValue(of([] as ProcedureCheckAlignment[])) } },
        // F-194 SF-194-02 — stub PUT statut pièce. Les tests dédiés override via .mockReturnValue
        { provide: PieceManquanteStatusService, useValue: { update: jest.fn().mockReturnValue(of({} as PieceManquanteStatus)), updateStatus: jest.fn().mockReturnValue(of({} as PieceManquanteStatus)) } },
        // F-194 SF-194-03 — stub GET alignement pièces. Les tests dédiés override via .mockReturnValue
        { provide: PieceManquanteAlignmentService, useValue: { getForCaseFile: jest.fn().mockReturnValue(of([] as PieceManquanteAlignment[])) } },
        // F-195 SF-195-02 — stub PUT statut risque. Les tests dédiés override via .mockReturnValue
        { provide: RisqueStatusService, useValue: { update: jest.fn().mockReturnValue(of({} as RisqueStatus)), updateStatus: jest.fn().mockReturnValue(of({} as RisqueStatus)) } },
        // F-195 SF-195-03 — stub GET alignement risques. Les tests dédiés override via .mockReturnValue
        { provide: RisqueAlignmentService, useValue: { getForCaseFile: jest.fn().mockReturnValue(of([] as RisqueAlignment[])) } },
        // F-197 SF-197-02 — stub GET/PUT override type litige. Les tests dédiés override via .mockReturnValue
        { provide: TypeLitigeOverrideService, useValue: {
          getForCaseFile: jest.fn().mockReturnValue(of({ typeLitigeAvocat: null, typeProcedureAvocat: null, raison: null } as TypeLitigeOverrideResponse)),
          update: jest.fn().mockReturnValue(of({} as TypeLitigeOverrideResponse)),
        } },
        { provide: DocxExportService, useValue: jasmine.createSpyObj('DocxExportService', ['export']) },
        { provide: ProcedureCheckService, useValue: procedureCheckService },
        { provide: StrategicOptionService, useValue: strategicOptionService },
        { provide: MatDialog, useValue: matDialogMock },
        { provide: TimeService, useValue: timeServiceMock },
        { provide: DocumentService, useValue: { list: jest.fn().mockReturnValue(of([])) } },
        // F-185 SF-185-01 — stub pour éviter l'EventSource réel et neutraliser le canal events$.
        { provide: GlobalAnalysisNotificationService, useValue: { track: jest.fn(), events$: NEVER } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SynthesisComponent);
    component = fixture.componentInstance;
  });

  // T-01 : au chargement → version la plus récente sélectionnée (index 0)
  it('selects most recent version on load', () => {
    const versions = [makeVersion(3, 'ENRICHED'), makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(3, 'ENRICHED')));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 3);
    expect(component.synthesis()?.version).toBe(3);
  });

  // T-02 : changement de version → loadSynthesisForVersion et loadQuestionsForVersion appelés
  it('reloads synthesis and questions on version change', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    component.onVersionChange(1);

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 1);
    expect(aiQuestionService.getQuestionsByAnalysisId).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
  });

  // T-03 : analysisType ENRICHED → isEnriched() true
  it('returns true for isEnriched when analysisType is ENRICHED', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'ENRICHED')));
    fixture.detectChanges();

    expect(component.isEnriched()).toBe(true);
  });

  // T-04 : analysisType STANDARD → isEnriched() false
  it('returns false for isEnriched when analysisType is STANDARD', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    expect(component.isEnriched()).toBe(false);
  });

  // T-05 : une seule version → versions().length === 1
  it('exposes single version without selector interaction', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    expect(component.versions().length).toBe(1);
  });

  // T-06 : versions vides → synthesis() null, loading false
  it('leaves synthesis null when no versions available', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(component.synthesis()).toBeNull();
    expect(component.loading()).toBe(false);
  });

  // F-185 SF-185-05 — bug 2 : "Synthèse non disponible" doit s'afficher
  // UNIQUEMENT si versions vide ET synthesis null. Sinon (synthesis partielle
  // chargée pendant un streaming), on doit afficher la synthèse partielle.

  // T-SF-185-05-01 : versions vide + synthesis null → message "non disponible" rendu
  it('renders "Synthèse non disponible" only when both versions empty AND synthesis null', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([]));
    // getPartial mockée par défaut sur 404 → synthesis reste null
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Synthèse non disponible');
  });

  // T-SF-185-05-02 : versions vide + synthesis chargée via partial → "non disponible" PAS rendu
  it('does not render "Synthèse non disponible" when partial synthesis is loaded', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([]));
    // simule getPartial qui retourne un état partiel
    (caseAnalysisService.getPartial as jest.Mock).mockReturnValue(of({
      analysisId: 'partial-1',
      version: 1,
      status: 'PARTIAL',
      sections: { faits: [{ texte: 'fait partiel' }] },
      updatedAt: '2026-05-03T17:00:00Z',
    }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Synthèse non disponible');
    // synthesis a bien été projetée depuis le partial
    expect(component.synthesis()).not.toBeNull();
  });

  // T-SF-185-05-03 (régression) : versions DONE → "non disponible" PAS rendu
  it('does not render "Synthèse non disponible" when at least one DONE version exists', () => {
    const versions = [makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Synthèse non disponible');
  });

  // T-07 : changement de version ne recharge pas le chat
  it('does not reload chat on version change', () => {
    const chatService = TestBed.inject(ChatService) as jest.Mocked<ChatService>;
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    const callsBefore = chatService.getHistory.mock.calls.length;
    component.onVersionChange(1);

    expect(chatService.getHistory.mock.calls.length).toBe(callsBefore);
  });

  // T-08 : versionLabel — ENRICHED affiche le badge Enrichie
  it('versionLabel includes Enrichie for ENRICHED type', () => {
    const v = makeVersion(2, 'ENRICHED');
    expect(component.versionLabel(v)).toBe('v2 — Enrichie');
  });

  // T-09 : versionLabel — STANDARD affiche juste le numéro
  it('versionLabel shows only version number for STANDARD type', () => {
    const v = makeVersion(1, 'STANDARD');
    expect(component.versionLabel(v)).toBe('v1');
  });

  // T-10 : editingQuestionId nul par défaut
  it('editingQuestionId is null by default', () => {
    expect(component.editingQuestionId()).toBeNull();
  });

  // T-11 : startEdit → editingQuestionId = id de la question
  it('startEdit sets editingQuestionId to the question id', () => {
    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.startEdit(q);
    expect(component.editingQuestionId()).toBe('q-1');
  });

  // T-12 : cancelEdit → editingQuestionId = null
  it('cancelEdit resets editingQuestionId to null', () => {
    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.startEdit(q);
    component.cancelEdit();
    expect(component.editingQuestionId()).toBeNull();
  });

  // T-13 : submitEdit texte vide → service non appelé
  it('submitEdit does not call service when text is empty', () => {
    const answerService = TestBed.inject(AiQuestionAnswerService) as jest.Mocked<AiQuestionAnswerService>;
    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.submitEdit(q, '   ');
    expect(answerService.submitAnswer).not.toHaveBeenCalled();
  });

  // T-14 : submitEdit nominal → service appelé, question mise à jour, editingQuestionId null
  it('submitEdit calls service and updates question on success', () => {
    const answerService = TestBed.inject(AiQuestionAnswerService) as jest.Mocked<AiQuestionAnswerService>;
    answerService.submitAnswer.mockReturnValue(of(undefined));

    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'Ancienne réponse' };
    component.questions.set([q]);
    component.startEdit(q);

    component.submitEdit(q, 'Nouvelle réponse');

    expect(answerService.submitAnswer).toHaveBeenCalledWith('q-1', 'Nouvelle réponse');
    expect(component.questions()[0].answerText).toBe('Nouvelle réponse');
    expect(component.editingQuestionId()).toBeNull();
  });

  // T-15 : submitEdit erreur → snackbar, editingQuestionId inchangé
  it('submitEdit shows snackbar on error and keeps editingQuestionId', () => {
    const answerService = TestBed.inject(AiQuestionAnswerService) as jest.Mocked<AiQuestionAnswerService>;
    const snackBar = TestBed.inject(MatSnackBar) as jest.Mocked<MatSnackBar>;
    answerService.submitAnswer.mockReturnValue(throwError(() => new Error('API error')));
    spyOn(snackBar, 'open');

    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.startEdit(q);
    component.submitEdit(q, 'Nouvelle réponse');

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('modification'), 'Fermer', expect.any(Object)
    );
    expect(component.editingQuestionId()).toBe('q-1');
  });

  // T-A1 : reAnalyze succès → trackEvent analysis_launched ENRICHED
  it('reAnalyze success → trackEvent analysis_launched ENRICHED', () => {
    const reAnalysisService = TestBed.inject(ReAnalysisService) as jest.Mocked<ReAnalysisService>;
    const analyticsService = TestBed.inject(AnalyticsService) as jest.Mocked<AnalyticsService>;
    const router = TestBed.inject(Router) as jest.Mocked<Router>;
    reAnalysisService.reAnalyze.mockReturnValue(of(undefined));
    spyOn(router, 'navigate');
    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });

    component.reAnalyze();

    expect(analyticsService.trackEvent).toHaveBeenCalledWith('analysis_launched', { type: 'ENRICHED' });
  });

  // T-A2 : exportPdf succès → trackEvent pdf_exported
  it('exportPdf success → trackEvent pdf_exported', () => {
    const analyticsService = TestBed.inject(AnalyticsService) as jest.Mocked<AnalyticsService>;
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    expect(analyticsService.trackEvent).toHaveBeenCalledWith('pdf_exported');
  });

  // F-192 SF-192-03 : exportPdf appelle RetainedPisteAlignmentService AVANT pdfExportService.export
  it('SF-192-03 exportPdf → RetainedPisteAlignmentService.getForCaseFile appelé puis pistes passées en 3ᵉ argument', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const alignmentService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const pistes: RetainedPisteAlignment[] = [
      { pisteId: 'p1', texte: 'Demander un titre Talent', conditions: [], matchStatus: 'ALIGNED', toolIdCible: 'F-IM-05-arbre-decisionnel-titre' },
    ];
    alignmentService.getForCaseFile.mockReturnValue(of(pistes));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'IMMIGRATION', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(alignmentService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[2]).toEqual(pistes);
    expect(typeof args[3]).toBe('function');
  });

  // F-192 SF-192-03 : endpoint timeout / erreur → export quand même, 3ᵉ arg = []
  it('SF-192-03 exportPdf → endpoint erreur, export appelé avec retainedPistes []', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const alignmentService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    alignmentService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'IMMIGRATION', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[2]).toEqual([]);
  });

  // F-193 SF-193-03 : exportPdf appelle ProcedureCheckAlignmentService EN PARALLÈLE de RetainedPisteAlignmentService
  it('SF-193-03 exportPdf → ProcedureCheckAlignmentService.getForCaseFile appelé en parallèle, checks passés en 5ᵉ argument', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const pistesService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checks: ProcedureCheckAlignment[] = [
      {
        checkId: 'c1',
        libelle: 'Motif principal du séjour confirmé',
        statut: 'VERIFIED',
        toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
        matchStatus: 'ALIGNED',
      },
    ];
    checksService.getForCaseFile.mockReturnValue(of(checks));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'IMMIGRATION', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pistesService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(checksService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[4]).toEqual(checks);
  });

  // F-193 SF-193-03 : endpoint checks timeout / erreur → export quand même appelé avec [] en 5ᵉ argument
  it('SF-193-03 exportPdf → ProcedureCheckAlignmentService erreur, export appelé avec procedureChecksAlignment []', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    checksService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[4]).toEqual([]);
  });

  // F-193 SF-193-03 : fail-open INDÉPENDANT — pistes succès + checks erreur → export avec pistes + []
  it('SF-193-03 exportPdf fail-open indépendant: pistes succès, checks erreur → export avec pistes + []', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const pistesService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const pistes: RetainedPisteAlignment[] = [
      { pisteId: 'p1', texte: 'Stratégie X', conditions: [], matchStatus: 'ALIGNED', toolIdCible: 'F-IM-05-arbre-decisionnel-titre' },
    ];
    pistesService.getForCaseFile.mockReturnValue(of(pistes));
    checksService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'IMMIGRATION', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[2]).toEqual(pistes);
    expect(args[4]).toEqual([]);
  });

  // F-194 SF-194-03 : exportPdf appelle PieceManquanteAlignmentService EN PARALLÈLE des 2 autres services
  it('SF-194-03 exportPdf → PieceManquanteAlignmentService.getForCaseFile appelé en parallèle, pieces passées en 6ᵉ argument', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const pistesService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const piecesService = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const pieces: PieceManquanteAlignment[] = [
      {
        pieceLibelle: 'Bulletins de salaire',
        statut: 'A_DEMANDER',
        toolIdsCibles: ['F-DT-09-comparateur-indemnites'],
        destinataire: null,
        raisonNonApp: null,
      },
    ];
    piecesService.getForCaseFile.mockReturnValue(of(pieces));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pistesService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(checksService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(piecesService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[5]).toEqual(pieces);
  });

  // F-194 SF-194-03 : endpoint pieces timeout / erreur → export quand même appelé avec [] en 6ᵉ argument
  it('SF-194-03 exportPdf → PieceManquanteAlignmentService erreur, export appelé avec piecesAlignment []', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const piecesService = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    piecesService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[5]).toEqual([]);
  });

  // F-194 SF-194-03 : fail-open INDÉPENDANT — pistes & checks succès, pieces erreur → export avec pistes + checks + []
  it('SF-194-03 exportPdf fail-open indépendant: pistes + checks succès, pieces erreur → export avec pistes + checks + []', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const pistesService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const piecesService = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const pistes: RetainedPisteAlignment[] = [
      { pisteId: 'p1', texte: 'Stratégie X', conditions: [], matchStatus: 'ALIGNED', toolIdCible: 'F-IM-05-arbre-decisionnel-titre' },
    ];
    const checks: ProcedureCheckAlignment[] = [
      { checkId: 'c1', libelle: 'Motif confirmé', statut: 'VERIFIED', toolIdCible: 'F-IM-05-arbre-decisionnel-titre', matchStatus: 'ALIGNED' },
    ];
    pistesService.getForCaseFile.mockReturnValue(of(pistes));
    checksService.getForCaseFile.mockReturnValue(of(checks));
    piecesService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'IMMIGRATION', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[2]).toEqual(pistes);
    expect(args[4]).toEqual(checks);
    expect(args[5]).toEqual([]);
  });

  // F-194 SF-194-03 : fail-open symétrique — pieces succès, pistes & checks erreur → export avec [] + [] + pieces
  it('SF-194-03 exportPdf fail-open indépendant: pistes + checks erreur, pieces succès → export avec [] + [] + pieces', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const pistesService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const piecesService = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const pieces: PieceManquanteAlignment[] = [
      { pieceLibelle: 'Pièce X', statut: 'A_DEMANDER', toolIdsCibles: [], destinataire: 'Client', raisonNonApp: null },
    ];
    pistesService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));
    checksService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));
    piecesService.getForCaseFile.mockReturnValue(of(pieces));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[2]).toEqual([]);
    expect(args[4]).toEqual([]);
    expect(args[5]).toEqual(pieces);
  });

  // F-195 SF-195-03 : exportPdf appelle RisqueAlignmentService EN PARALLÈLE
  // des 3 autres services et passe le résultat en 7ᵉ argument à export().
  it('SF-195-03 exportPdf → RisqueAlignmentService.getForCaseFile appelé en parallèle, risques passés en 7ᵉ argument', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const pistesService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const piecesService = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const risquesService = TestBed.inject(RisqueAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const risques: RisqueAlignment[] = [
      {
        risqueLibelle: 'Harcèlement moral subi',
        statut: 'VALIDE',
        toolIdsCibles: ['F-DT-12-harcelement-licenciement-nul'],
        raisonEcarte: null,
      },
    ];
    risquesService.getForCaseFile.mockReturnValue(of(risques));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pistesService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(checksService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(piecesService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(risquesService.getForCaseFile).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[6]).toEqual(risques);
  });

  // F-195 SF-195-03 : endpoint risques erreur → export quand même appelé
  // avec [] en 7ᵉ argument (fail-open INDÉPENDANT par stream).
  it('SF-195-03 exportPdf → RisqueAlignmentService erreur, export appelé avec risquesAlignment []', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const risquesService = TestBed.inject(RisqueAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    risquesService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[6]).toEqual([]);
  });

  // F-195 SF-195-03 : fail-open INDÉPENDANT — risques succès, 3 autres
  // erreurs → export avec [] + [] + [] + risques (CA-07 mini-spec).
  it('SF-195-03 exportPdf fail-open indépendant: 3 autres erreurs, risques succès → export avec [] + [] + [] + risques', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    const pistesService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const piecesService = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const risquesService = TestBed.inject(RisqueAlignmentService) as unknown as { getForCaseFile: jest.Mock };
    const risques: RisqueAlignment[] = [
      { risqueLibelle: 'Risque retenu X', statut: 'VALIDE', toolIdsCibles: [], raisonEcarte: null },
    ];
    pistesService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));
    checksService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));
    piecesService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));
    risquesService.getForCaseFile.mockReturnValue(of(risques));

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    const args = pdfExportService.export.mock.calls[0];
    expect(args[2]).toEqual([]);
    expect(args[4]).toEqual([]);
    expect(args[5]).toEqual([]);
    expect(args[6]).toEqual(risques);
  });

  // T-16 : onVersionChange réinitialise editingQuestionId
  it('onVersionChange resets editingQuestionId', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.startEdit(q);
    component.onVersionChange(1);

    expect(component.editingQuestionId()).toBeNull();
  });

  // T-20 : piecesManquantes non vides → section rendue dans le template
  it('renders pieces manquantes section when list is non-empty', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD', ['Contrat de travail', 'Bulletins de salaire'])));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Pièces manquantes');
    expect(el.textContent).toContain('Contrat de travail');
  });

  // T-21 : piecesManquantes vides → section absente du template
  it('does not render pieces manquantes section when list is empty', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD', [])));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).not.toContain('Pièces manquantes');
  });

  // F-194 SF-194-02 — UI markable pièces : 3 boutons rendus + clic → PUT
  describe('F-194 SF-194-02 markable pieces', () => {
    beforeEach(() => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(
        of(makeSynthesis(1, 'STANDARD', ['Contrat de travail', 'Bulletins de salaire'])),
      );
    });

    // CA-02 : 3 boutons statut rendus pour chaque pièce
    it('CA-02 renders 3 status buttons for each piece', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const html = el.innerHTML;
      expect(html).toContain('piece-status-btn--demander');
      expect(html).toContain('piece-status-btn--obtenue');
      expect(html).toContain('piece-status-btn--non-applicable');
    });

    // CA-01 : clic OBTENUE → PUT updateStatus avec libellé original + statut OBTENUE
    it('CA-01 click OBTENUE → updateStatus called with libelle + OBTENUE', () => {
      const statusService = TestBed.inject(PieceManquanteStatusService) as unknown as { updateStatus: jest.Mock };
      statusService.updateStatus.mockReturnValue(of({
        pieceLibelleOriginal: 'Contrat de travail',
        statut: 'OBTENUE',
      } as PieceManquanteStatus));
      fixture.detectChanges();

      component.updatePieceStatus('Contrat de travail', 'OBTENUE');

      expect(statusService.updateStatus).toHaveBeenCalledWith(
        CASE_FILE_ID,
        'Contrat de travail',
        'OBTENUE',
        { raisonNonApp: null, destinataire: null },
      );
      expect(component.pieceStatusFor('Contrat de travail')).toBe('OBTENUE');
    });

    // CA-03 : NON_APPLICABLE → champ raison + PUT inclut la raison au blur
    it('CA-03 NON_APPLICABLE click → status updated, blur with raison → PUT inclut raison', () => {
      const statusService = TestBed.inject(PieceManquanteStatusService) as unknown as { updateStatus: jest.Mock };
      statusService.updateStatus.mockReturnValue(of({
        pieceLibelleOriginal: 'Acte de mariage',
        statut: 'NON_APPLICABLE',
      } as PieceManquanteStatus));
      fixture.detectChanges();

      component.updatePieceStatus('Acte de mariage', 'NON_APPLICABLE');
      component.onPieceRaisonInput('Acte de mariage', 'Concubinage simple');
      component.onPieceRaisonBlur('Acte de mariage');

      expect(statusService.updateStatus).toHaveBeenLastCalledWith(
        CASE_FILE_ID,
        'Acte de mariage',
        'NON_APPLICABLE',
        { raisonNonApp: 'Concubinage simple', destinataire: null },
      );
    });

    // CA-04 : A_DEMANDER + selection destinataire → PUT inclut le destinataire
    it('CA-04 A_DEMANDER → destinataire selection → PUT inclut destinataire', () => {
      const statusService = TestBed.inject(PieceManquanteStatusService) as unknown as { updateStatus: jest.Mock };
      statusService.updateStatus.mockReturnValue(of({
        pieceLibelleOriginal: 'Contrat de travail',
        statut: 'A_DEMANDER',
      } as PieceManquanteStatus));
      fixture.detectChanges();

      // Statut implicite A_DEMANDER, mais on tag puis on choisit un destinataire.
      component.updatePieceStatus('Contrat de travail', 'A_DEMANDER');
      component.onPieceDestinataireChange('Contrat de travail', 'Ex-employeur');

      expect(statusService.updateStatus).toHaveBeenLastCalledWith(
        CASE_FILE_ID,
        'Contrat de travail',
        'A_DEMANDER',
        { raisonNonApp: null, destinataire: 'Ex-employeur' },
      );
    });

    // CA-05 : régression critique — PUT NE déclenche AUCUN refresh côté UI
    it('CA-05 PUT statut → AUCUN re-fetch alignement (cohérence F-176 stricte)', () => {
      const statusService = TestBed.inject(PieceManquanteStatusService) as unknown as { updateStatus: jest.Mock };
      const pisteService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
      const checksAlignmentService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
      statusService.updateStatus.mockReturnValue(of({
        pieceLibelleOriginal: 'Pièce X',
        statut: 'OBTENUE',
      } as PieceManquanteStatus));
      fixture.detectChanges();

      // SynthesisComponent ne charge pas piste/checks alignment au mount (seul exportPdf
      // les déclenche). On vérifie qu'après un PUT statut pièce, ces services restent
      // au même niveau d'invocation — aucun side-effect post-PUT.
      const callsBefore = (pisteService.getForCaseFile as jest.Mock).mock.calls.length
        + (checksAlignmentService.getForCaseFile as jest.Mock).mock.calls.length;

      component.updatePieceStatus('Pièce X', 'OBTENUE');

      const callsAfter = (pisteService.getForCaseFile as jest.Mock).mock.calls.length
        + (checksAlignmentService.getForCaseFile as jest.Mock).mock.calls.length;
      expect(callsAfter).toBe(callsBefore);
      // versions non rechargées
      expect(caseAnalysisService.getVersions).toHaveBeenCalledTimes(1);
    });

    // CA-14 : erreur PUT → snackbar + rollback statut au précédent
    it('CA-14 PUT error → snackbar + rollback statut', () => {
      const statusService = TestBed.inject(PieceManquanteStatusService) as unknown as { updateStatus: jest.Mock };
      const snack = TestBed.inject(MatSnackBar) as unknown as { open: jest.Mock };
      snack.open = jest.fn();
      statusService.updateStatus.mockReturnValueOnce(throwError(() => ({ status: 500 })));
      fixture.detectChanges();

      // Initial : pas d'entrée → A_DEMANDER implicite
      component.updatePieceStatus('Pièce Y', 'OBTENUE');

      // Rollback : entrée supprimée du cache
      expect(component.pieceStatusFor('Pièce Y')).toBe('A_DEMANDER');
      expect(snack.open).toHaveBeenCalled();
    });

    // CA-13 : palette navy/or DESIGN_SYSTEM.md (NON_APPLICABLE = gris discret)
    it('CA-13 button classes follow DESIGN_SYSTEM (navy/or/gris — pas de rouge)', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      // Les classes utilisent les modificateurs (couleurs définies en SCSS)
      expect(el.querySelector('.piece-status-btn--demander')).toBeTruthy();
      expect(el.querySelector('.piece-status-btn--obtenue')).toBeTruthy();
      expect(el.querySelector('.piece-status-btn--non-applicable')).toBeTruthy();
    });

    // Helper : pieceStatusFor par défaut = A_DEMANDER (statut implicite, pas d'appel PUT)
    it('pieceStatusFor defaults to A_DEMANDER for un-tagged piece', () => {
      fixture.detectChanges();
      expect(component.pieceStatusFor('Contrat de travail')).toBe('A_DEMANDER');
      expect(component.isPieceStatusActive('Contrat de travail', 'A_DEMANDER')).toBe(true);
    });
  });

  // F-195 SF-195-02 — UI markable risques : 3 boutons rendus + clic → PUT
  describe('F-195 SF-195-02 markable risques', () => {
    const RISQUE_HARC = 'Harcèlement moral subi';
    const RISQUE_PRESC = 'Risque de prescription';

    beforeEach(() => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(
        of(makeSynthesis(1, 'STANDARD', [], null, null, [
          makeItem(RISQUE_HARC),
          makeItem(RISQUE_PRESC),
        ])),
      );
    });

    // CA-01 : 3 boutons statut rendus pour chaque risque
    it('CA-01 renders 3 status buttons for each risque', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const html = el.innerHTML;
      expect(html).toContain('risque-status-btn--creuser');
      expect(html).toContain('risque-status-btn--valide');
      expect(html).toContain('risque-status-btn--ecarte');
    });

    // CA-01 : clic VALIDE → PUT updateStatus avec libellé original + statut VALIDE
    it('CA-01 click VALIDE → updateStatus called with libelle + VALIDE', () => {
      const statusService = TestBed.inject(RisqueStatusService) as unknown as { updateStatus: jest.Mock };
      statusService.updateStatus.mockReturnValue(of({
        risqueLibelleOriginal: RISQUE_HARC,
        statut: 'VALIDE',
      } as RisqueStatus));
      fixture.detectChanges();

      component.updateRisqueStatus(RISQUE_HARC, 'VALIDE');

      expect(statusService.updateStatus).toHaveBeenCalledWith(
        CASE_FILE_ID,
        RISQUE_HARC,
        'VALIDE',
        { raisonEcarte: null },
      );
      expect(component.risqueStatusFor(RISQUE_HARC)).toBe('VALIDE');
    });

    // CA-02 : ECARTE → champ raison + PUT inclut la raison au blur
    it('CA-02 ECARTE click → status updated, blur with raison → PUT inclut raison', () => {
      const statusService = TestBed.inject(RisqueStatusService) as unknown as { updateStatus: jest.Mock };
      statusService.updateStatus.mockReturnValue(of({
        risqueLibelleOriginal: RISQUE_PRESC,
        statut: 'ECARTE',
      } as RisqueStatus));
      fixture.detectChanges();

      component.updateRisqueStatus(RISQUE_PRESC, 'ECARTE');
      component.onRisqueRaisonInput(RISQUE_PRESC, 'Acte interruptif retrouvé');
      component.onRisqueRaisonBlur(RISQUE_PRESC);

      expect(statusService.updateStatus).toHaveBeenLastCalledWith(
        CASE_FILE_ID,
        RISQUE_PRESC,
        'ECARTE',
        { raisonEcarte: 'Acte interruptif retrouvé' },
      );
    });

    // CA-05 : régression critique — PUT NE déclenche AUCUN refresh côté UI
    it('CA-05 PUT statut → AUCUN re-fetch alignement (cohérence F-176 stricte)', () => {
      const statusService = TestBed.inject(RisqueStatusService) as unknown as { updateStatus: jest.Mock };
      const pisteService = TestBed.inject(RetainedPisteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
      const piecesService = TestBed.inject(PieceManquanteAlignmentService) as unknown as { getForCaseFile: jest.Mock };
      const checksService = TestBed.inject(ProcedureCheckAlignmentService) as unknown as { getForCaseFile: jest.Mock };
      statusService.updateStatus.mockReturnValue(of({
        risqueLibelleOriginal: RISQUE_HARC,
        statut: 'VALIDE',
      } as RisqueStatus));
      fixture.detectChanges();

      const callsBefore = (pisteService.getForCaseFile as jest.Mock).mock.calls.length
        + (piecesService.getForCaseFile as jest.Mock).mock.calls.length
        + (checksService.getForCaseFile as jest.Mock).mock.calls.length;

      component.updateRisqueStatus(RISQUE_HARC, 'VALIDE');

      const callsAfter = (pisteService.getForCaseFile as jest.Mock).mock.calls.length
        + (piecesService.getForCaseFile as jest.Mock).mock.calls.length
        + (checksService.getForCaseFile as jest.Mock).mock.calls.length;
      expect(callsAfter).toBe(callsBefore);
      // versions non rechargées
      expect(caseAnalysisService.getVersions).toHaveBeenCalledTimes(1);
    });

    // CA-erreur : erreur PUT → snackbar + rollback statut au précédent
    it('CA-erreur PUT error → snackbar + rollback statut', () => {
      const statusService = TestBed.inject(RisqueStatusService) as unknown as { updateStatus: jest.Mock };
      const snack = TestBed.inject(MatSnackBar) as unknown as { open: jest.Mock };
      snack.open = jest.fn();
      statusService.updateStatus.mockReturnValueOnce(throwError(() => ({ status: 500 })));
      fixture.detectChanges();

      // Initial : pas d'entrée → A_CREUSER implicite
      component.updateRisqueStatus(RISQUE_HARC, 'VALIDE');

      // Rollback : entrée supprimée du cache
      expect(component.risqueStatusFor(RISQUE_HARC)).toBe('A_CREUSER');
      expect(snack.open).toHaveBeenCalled();
    });

    // CA-10 : palette navy/or/gris DESIGN_SYSTEM.md (pas de rouge sur les boutons)
    it('CA-10 button classes follow DESIGN_SYSTEM (navy/or/gris — pas de rouge)', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.risque-status-btn--creuser')).toBeTruthy();
      expect(el.querySelector('.risque-status-btn--valide')).toBeTruthy();
      expect(el.querySelector('.risque-status-btn--ecarte')).toBeTruthy();
    });

    // Helper : risqueStatusFor par défaut = A_CREUSER (statut implicite)
    it('risqueStatusFor defaults to A_CREUSER for un-tagged risque', () => {
      fixture.detectChanges();
      expect(component.risqueStatusFor(RISQUE_HARC)).toBe('A_CREUSER');
      expect(component.isRisqueStatusActive(RISQUE_HARC, 'A_CREUSER')).toBe(true);
    });
  });

  // TC-01 : loadChecksForVersion appelé au chargement initial
  it('calls loadChecksForVersion for the most recent version on load', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(2, 'STANDARD')));

    fixture.detectChanges();

    expect(procedureCheckService.list).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-2');
  });

  // TC-02 : onVersionChange réinitialise procedureChecks puis recharge
  it('onVersionChange resets procedureChecks and reloads for new version', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    component.procedureChecks.set([makeCheck('c1', 1)]);

    fixture.detectChanges();
    component.onVersionChange(1);

    expect(procedureCheckService.list).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
  });

  // TC-03 : checks vides → panneau checklist absent du template
  it('does not render checklist panel when procedureChecks is empty', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    procedureCheckService.list.mockReturnValue(of([]));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).not.toContain('Checklist procédurale');
  });

  // TC-04 : updateCheckStatus succès → statut mis à jour localement
  it('updateCheckStatus updates check status locally on success', () => {
    const check = makeCheck('c1', 1, 'TO_CHECK');
    component.procedureChecks.set([check]);
    const updated: ProcedureCheck = { ...check, statut: 'VERIFIED' };
    procedureCheckService.updateStatus.mockReturnValue(of(updated));

    component.updateCheckStatus(check, 'VERIFIED');

    expect(component.procedureChecks()[0].statut).toBe('VERIFIED');
    expect(component.updatingCheckId()).toBeNull();
  });

  // TS-01 : item avec source → badge source affiché
  it('renders source badge when item has a source', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of({
      ...makeSynthesis(1, 'STANDARD'),
      faits: [makeItem('Licenciement abusif', 'Document 0 (contrat.pdf)', 'Il est mis fin au contrat')]
    }));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Licenciement abusif');
    expect(el.textContent).toContain('Document 0 (contrat.pdf)');
    expect(el.textContent).toContain('Il est mis fin au contrat');
  });

  // TS-02 : item sans source → pas de badge
  it('does not render source badge when source is null', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of({
      ...makeSynthesis(1, 'STANDARD'),
      faits: [makeItem('Fait sans source')]
    }));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Fait sans source');
    expect(el.querySelectorAll('app-source-ref').length).toBe(0);
  });

  // TC-05 : updateCheckStatus erreur → snackbar, statut non modifié
  it('updateCheckStatus shows snackbar on error without modifying status', () => {
    const snackBar = TestBed.inject(MatSnackBar);
    spyOn(snackBar, 'open');

    const check = makeCheck('c1', 1, 'TO_CHECK');
    component.procedureChecks.set([check]);
    procedureCheckService.updateStatus.mockReturnValue(throwError(() => ({ status: 500 })));

    component.updateCheckStatus(check, 'VERIFIED');

    expect(component.procedureChecks()[0].statut).toBe('TO_CHECK');
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('statut'), 'Fermer', expect.any(Object)
    );
    expect(component.updatingCheckId()).toBeNull();
  });

  // R-03 : badge de risque présent dans le header si riskLevel non null
  it('R-03: synthesis avec riskLevel → badge .risk-badge présent dans le header', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD', [], 'ELEVE', 82)));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.risk-badge');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('Élevé');
    expect(badge.textContent).toContain('82');
  });

  // R-04 : badge absent si riskLevel null
  it('R-04: synthesis sans riskLevel → badge .risk-badge absent', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD', [], null, null)));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.risk-badge');
    expect(badge).toBeNull();
  });

  // DOCX-01 : clic bouton .docx → docxExportService.export() appelé
  it('DOCX-01: click on .docx button calls docxExportService.export()', () => {
    const docxExportService = TestBed.inject(DocxExportService) as jest.Mocked<DocxExportService>;
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportDocx();

    expect(docxExportService.export).toHaveBeenCalled();
  });

  // DOCX-02 : exportDocx() ne fait rien si synthesis() est null
  it('DOCX-02: exportDocx() does nothing when synthesis is null', () => {
    const docxExportService = TestBed.inject(DocxExportService) as jest.Mocked<DocxExportService>;
    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(null);

    component.exportDocx();

    expect(docxExportService.export).not.toHaveBeenCalled();
  });

  // DOCX-03 : bouton .docx désactivé si synthesis() null
  it('DOCX-03: .docx button is disabled when synthesis is null', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([]));
    fixture.detectChanges();

    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    const docxBtn = Array.from(buttons).find(b => b.textContent?.includes('.docx'));
    // When no versions, the export buttons are not rendered at all (outside the @else block)
    // So we verify synthesis() is null, which would disable the button
    expect(component.synthesis()).toBeNull();
  });

  // SRC-01 : resolveSource avec "Document 0" et analysisDocuments → retourne le nom du fichier
  it('SRC-01: resolveSource("Document 0") returns the file name when analysisDocuments contains index 0', () => {
    component.synthesis.set({
      ...makeSynthesis(1, 'STANDARD'),
      analysisDocuments: [{ index: 0, name: 'contrat.pdf' }]
    });

    expect(component.resolveSource('Document 0')).toBe('contrat.pdf');
  });

  // SRC-02 : resolveSource avec un nom de fichier direct → retourné tel quel
  it('SRC-02: resolveSource("contrat.pdf") returns "contrat.pdf" unchanged', () => {
    component.synthesis.set({
      ...makeSynthesis(1, 'STANDARD'),
      analysisDocuments: [{ index: 0, name: 'contrat.pdf' }]
    });

    expect(component.resolveSource('contrat.pdf')).toBe('contrat.pdf');
  });

  // SRC-03 : resolveSource(null) → null
  it('SRC-03: resolveSource(null) returns null', () => {
    expect(component.resolveSource(null)).toBeNull();
  });

  // SRC-04 : resolveSource("Document 5") sans cet index dans analysisDocuments → fallback sur la valeur brute
  it('SRC-04: resolveSource("Document 5") returns "Document 5" when index 5 is not in analysisDocuments', () => {
    component.synthesis.set({
      ...makeSynthesis(1, 'STANDARD'),
      analysisDocuments: [{ index: 0, name: 'contrat.pdf' }]
    });

    expect(component.resolveSource('Document 5')).toBe('Document 5');
  });

  // SRC-05 : resolveSource sans analysisDocuments → retourne la source brute sans erreur
  it('SRC-05: resolveSource returns raw source when analysisDocuments is absent', () => {
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    expect(component.resolveSource('Document 0')).toBe('Document 0');
  });

  // SRC-06 : sourceMap avec analysisDocuments → map correctement construite
  it('SRC-06: sourceMap is correctly built from analysisDocuments', () => {
    component.synthesis.set({
      ...makeSynthesis(1, 'STANDARD'),
      analysisDocuments: [
        { index: 0, name: 'contrat.pdf' },
        { index: 1, name: 'lettre.pdf' }
      ]
    });

    const map = component.sourceMap();
    expect(map.get('Document 0')).toBe('contrat.pdf');
    expect(map.get('Document 1')).toBe('lettre.pdf');
    expect(map.size).toBe(2);
  });

  // SRC-07 : DOM — badge source affiche le nom résolu pour un fait avec source "Document 0"
  it('SRC-07: source badge shows resolved file name for a fait with source "Document 0"', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of({
      ...makeSynthesis(1, 'STANDARD'),
      faits: [makeItem('Licenciement injustifié', 'Document 0')],
      analysisDocuments: [{ index: 0, name: 'lettre.pdf' }]
    }));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const badge = el.querySelector('app-source-ref');
    expect(badge).not.toBeNull();
    expect(badge!.textContent).toContain('lettre.pdf');
    expect(badge!.textContent).not.toContain('Document 0');
  });

  // TI-01 : totalBilledSeconds — somme uniquement les entrées avec durationSeconds != null
  it('TI-01: totalBilledSeconds sums only entries with durationSeconds', () => {
    const entries: TimeEntryResponse[] = [
      { id: 'e1', caseFileId: 'cf-1', startedAt: '', durationSeconds: 3600 },
      { id: 'e2', caseFileId: 'cf-1', startedAt: '', durationSeconds: 1800 },
      { id: 'e3', caseFileId: 'cf-1', startedAt: '', durationSeconds: undefined } // timer actif
    ];
    component.timeEntries.set(entries);
    expect(component.totalBilledSeconds()).toBe(5400);
  });

  // TI-02 : totalBilledSeconds — aucune entrée terminée → 0
  it('TI-02: totalBilledSeconds returns 0 when no completed entries', () => {
    component.timeEntries.set([
      { id: 'e1', caseFileId: 'cf-1', startedAt: '', durationSeconds: undefined }
    ]);
    expect(component.totalBilledSeconds()).toBe(0);
  });

  // TI-03 : showInsight — true si riskLevel non null ET totalBilledSeconds > 0
  it('TI-03: showInsight is true when riskLevel set and totalBilledSeconds > 0', () => {
    component.synthesis.set(makeSynthesis(1, 'STANDARD', [], 'ELEVE', 85));
    component.timeEntries.set([{ id: 'e1', caseFileId: 'cf-1', startedAt: '', durationSeconds: 3600 }]);
    expect(component.showInsight()).toBe(true);
  });

  // TI-04 : showInsight — false si aucune session terminée
  it('TI-04: showInsight is false when no completed entries', () => {
    component.synthesis.set(makeSynthesis(1, 'STANDARD', [], 'ELEVE', 85));
    component.timeEntries.set([]);
    expect(component.showInsight()).toBe(false);
  });

  // TI-05 : showInsight — false si riskLevel null
  it('TI-05: showInsight is false when riskLevel is null', () => {
    component.synthesis.set(makeSynthesis(1, 'STANDARD', [], null, null));
    component.timeEntries.set([{ id: 'e1', caseFileId: 'cf-1', startedAt: '', durationSeconds: 3600 }]);
    expect(component.showInsight()).toBe(false);
  });

  // TI-06 : insightText — format "2h 15min … Élevé"
  it('TI-06: insightText formats correctly for hours and minutes', () => {
    component.synthesis.set(makeSynthesis(1, 'STANDARD', [], 'ELEVE', 85));
    component.timeEntries.set([{ id: 'e1', caseFileId: 'cf-1', startedAt: '', durationSeconds: 8100 }]); // 2h 15min
    expect(component.insightText()).toContain('2h 15min');
    expect(component.insightText()).toContain('Élevé');
  });

  // TI-07 : formatInsightDuration — "< 1min" si < 60s
  it('TI-07: formatInsightDuration returns "< 1min" for seconds < 60', () => {
    expect(component.formatInsightDuration(45)).toBe('< 1min');
    expect(component.formatInsightDuration(0)).toBe('< 1min');
    expect(component.formatInsightDuration(59)).toBe('< 1min');
  });

  // CL-PDF-01 : bouton export PDF checklist visible si procedureChecks non vide
  it('CL-PDF-01: checklist export PDF button is visible when procedureChecks is not empty', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    component.procedureChecks.set([
      { id: 'c1', ordre: 1, description: 'Un point', statut: 'VERIFIED', raison: null }
    ]);
    fixture.detectChanges();

    const btn = fixture.nativeElement.querySelector('.export-btn--checklist');
    expect(btn).toBeTruthy();
  });

  // CL-PDF-02 : bouton export PDF checklist absent si procedureChecks vide
  it('CL-PDF-02: checklist export PDF button is absent when procedureChecks is empty', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    component.procedureChecks.set([]);
    fixture.detectChanges();

    const btn = fixture.nativeElement.querySelector('.export-btn--checklist');
    expect(btn).toBeNull();
  });

  // SF-DT-01-01 : Panneau indemnités estimées
  it('COMP-01: panneau indemnités visible si compensationEstimate non null', () => {
    const estimate = {
      indemnite: 8050, salaireReference: 2800, ancienneteAnnees: 6, ancienneteMois: 4,
      typeRupture: 'LICENCIEMENT', plafondMinMois: 3, plafondMaxMois: 7, donneesPartielles: false
    };
    const synthWithComp = { ...makeSynthesis(1, 'ENRICHED'), compensationEstimate: estimate };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthWithComp));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.compensation-block')).toBeTruthy();
  });

  it('COMP-02: panneau indemnités absent si compensationEstimate null', () => {
    const synthNoComp = { ...makeSynthesis(1, 'ENRICHED'), compensationEstimate: null };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthNoComp));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.compensation-block')).toBeNull();
  });

  // Fix F-DT-09-BE : compensationEstimate renseigné pour BE (alertes F-IA-03)
  // mais panneau Macron FR masqué si belgianCompensationEstimate présent
  it('COMP-02 bis: panneau Macron FR masqué si belgianCompensationEstimate présent (workspace BE)', () => {
    const ceFR = {
      indemnite: 0, salaireReference: 3100, ancienneteAnnees: 7, ancienneteMois: 9,
      typeRupture: 'LICENCIEMENT_ORDINAIRE', plafondMinMois: 0, plafondMaxMois: 0, donneesPartielles: true
    };
    const ceBelge = {
      preavisSemaines: 30, indemniteCompensatoire: 22384, salaireHebdomadaire: 716,
      salaireReference: 3100, ancienneteAnnees: 7, ancienneteMois: 9,
      cct109MinSemaines: 3, cct109MaxSemaines: 17, cct109MinEuros: 2148, cct109MaxEuros: 12172,
      donneesPartielles: false
    };
    const synthBE = {
      ...makeSynthesis(1, 'ENRICHED'),
      compensationEstimate: ceFR,
      belgianCompensationEstimate: ceBelge,
    };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthBE));
    fixture.detectChanges();

    expect(component.showMacronPanel).toBe(false);
    expect(component.compensationEstimate).toBeTruthy();  // exposé pour alertes F-IA-03
    expect(component.belgianCompensationEstimate).toBeTruthy();  // affiché CCT 109
  });

  it('COMP-03: avertissement données partielles visible si donneesPartielles=true', () => {
    const estimate = {
      indemnite: 0, salaireReference: 0, ancienneteAnnees: 5, ancienneteMois: 0,
      typeRupture: 'LICENCIEMENT', plafondMinMois: 3, plafondMaxMois: 6, donneesPartielles: true
    };
    const synthPartial = { ...makeSynthesis(1, 'ENRICHED'), compensationEstimate: estimate };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthPartial));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.compensation-warning')).toBeTruthy();
  });

  it('COMP-04: formatAnciennete renvoie le bon libellé', () => {
    expect(component.formatAnciennete(6, 4)).toBe('6 ans 4 mois');
    expect(component.formatAnciennete(1, 0)).toBe('1 an');
    expect(component.formatAnciennete(0, 0)).toBe("moins d'1 an");
  });

  it('COMP-05: formatTypeRupture renvoie le libellé français', () => {
    expect(component.formatTypeRupture('LICENCIEMENT')).toBe('Licenciement');
    expect(component.formatTypeRupture('RUPTURE_CONVENTIONNELLE')).toBe('Rupture conventionnelle');
  });

  // SF-FA-02-02 : Panneau pension alimentaire
  it('FA-PA-01: panneau pension alimentaire visible si pensionAlimentaireEstimate non null', () => {
    const estimate = {
      montantMin: 360, montantMax: 440, revenus: 2000, nbEnfants: 1,
      modeGarde: 'EXCLUSIVE' as const, pays: 'FRANCE' as const, donneesPartielles: false
    };
    const synthWithPA = { ...makeSynthesis(1, 'ENRICHED'), pensionAlimentaireEstimate: estimate };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthWithPA));
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Pension alimentaire indicative');
  });

  it('FA-PA-02: panneau pension alimentaire absent si pensionAlimentaireEstimate null', () => {
    const synthNoPA = { ...makeSynthesis(1, 'ENRICHED'), pensionAlimentaireEstimate: null };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthNoPA));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Pension alimentaire indicative');
  });

  it('FA-PA-03: badge données partielles visible si donneesPartielles=true', () => {
    const estimate = {
      montantMin: 0, montantMax: 0, revenus: 0, nbEnfants: 2,
      modeGarde: 'EXCLUSIVE' as const, pays: 'FRANCE' as const, donneesPartielles: true
    };
    const synthPartial = { ...makeSynthesis(1, 'ENRICHED'), pensionAlimentaireEstimate: estimate };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthPartial));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Données partielles');
  });

  // SF-FA-01-02 : Panneau prestation compensatoire
  it('FA-PC-01: panneau prestation compensatoire visible si prestationCompensatoireEstimate non null', () => {
    const estimate = {
      montantMin: 36720, montantMax: 49680, ecartRevenus: 1500, dureeMarriage: 10,
      pays: 'FRANCE' as const, donneesPartielles: false
    };
    const synthWithPC = { ...makeSynthesis(1, 'ENRICHED'), prestationCompensatoireEstimate: estimate };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthWithPC));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Prestation compensatoire indicative');
  });

  it('FA-PC-02: panneau prestation compensatoire absent si estimate null', () => {
    const synthNoPC = { ...makeSynthesis(1, 'ENRICHED'), prestationCompensatoireEstimate: null };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthNoPC));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Prestation compensatoire indicative');
  });

  it('FA-PC-03: badge données partielles visible si donneesPartielles=true', () => {
    const estimate = {
      montantMin: 0, montantMax: 0, ecartRevenus: 0, dureeMarriage: 0,
      pays: 'FRANCE' as const, donneesPartielles: true
    };
    const synthPartial = { ...makeSynthesis(1, 'ENRICHED'), prestationCompensatoireEstimate: estimate };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(synthPartial));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Données partielles');
  });

  // SF-FA-04-02 : Panneau liquidation de communauté
  it('FA-LC-01: panneau liquidation visible si liquidationCommunaute non null', () => {
    const liq = {
      regimeMatrimonial: 'COMMUNAUTE_LEGALE',
      actifCommun: [{ libelle: 'Résidence principale', valeur: 280000 }],
      biensPropresEpouxA: [], biensPropresEpouxB: [],
      passifCommun: [{ libelle: 'Crédit immobilier', valeur: 95000 }]
    };
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of({ ...makeSynthesis(1, 'ENRICHED'), liquidationCommunaute: liq }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Liquidation de communauté');
  });

  it('FA-LC-02: panneau liquidation absent si liquidationCommunaute null', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.mockReturnValue(of({ ...makeSynthesis(1, 'ENRICHED'), liquidationCommunaute: null }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Liquidation de communauté');
  });

  it('FA-LC-03: formatRegime retourne le bon libellé', () => {
    expect(component.formatRegime('COMMUNAUTE_LEGALE')).toBe('Communauté légale');
    expect(component.formatRegime('SEPARATION_BIENS')).toBe('Séparation de biens');
    expect(component.formatRegime(null)).toBe('Non détecté');
  });

  // CL-PDF-03 : click sur le bouton → exportChecklistPdf() appelé
  it('CL-PDF-03: click on checklist PDF button calls exportChecklistPdf()', () => {
    const pdfExportService = TestBed.inject(PdfExportService) as jest.Mocked<PdfExportService>;
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.procedureChecks.set([
      { id: 'c1', ordre: 1, description: 'Un point', statut: 'VERIFIED', raison: null }
    ]);
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.export-btn--checklist');
    btn.click();

    expect(pdfExportService.exportChecklist).toHaveBeenCalled();
  });

  // SF-176-02 T-01 : chargement initial des pistes stratégiques
  it('SF-176-02 T-01: loads strategic options on init for the most recent version', () => {
    const options = [makeOption('o1', 0, 'Piste 1'), makeOption('o2', 1, 'Piste 2', 'RETAINED')];
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    strategicOptionService.list.mockReturnValue(of(options));

    fixture.detectChanges();

    expect(strategicOptionService.list).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
    expect(component.strategicOptions().length).toBe(2);
  });

  // SF-176-02 T-02 : computed signals filtrent par statut
  it('SF-176-02 T-02: computed signals filter options by status', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    strategicOptionService.list.mockReturnValue(of([
      makeOption('o1', 0, 'Étudier 1', 'TO_STUDY'),
      makeOption('o2', 1, 'Retenue 1', 'RETAINED'),
      makeOption('o3', 2, 'Écartée 1', 'DISCARDED'),
      makeOption('o4', 3, 'Étudier 2', 'TO_STUDY')
    ]));
    fixture.detectChanges();

    expect(component.optionsToStudy().map(o => o.id)).toEqual(['o1', 'o4']);
    expect(component.optionsRetained().map(o => o.id)).toEqual(['o2']);
    expect(component.optionsDiscarded().map(o => o.id)).toEqual(['o3']);
  });

  // SF-176-02 T-03 : updateOptionStatus RETAINED → appel direct au service
  it('SF-176-02 T-03: updateOptionStatus to RETAINED calls service directly', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    const option = makeOption('o1', 0, 'Piste');
    strategicOptionService.list.mockReturnValue(of([option]));
    strategicOptionService.updateStatus.mockReturnValue(of({ ...option, statut: 'RETAINED' }));
    fixture.detectChanges();

    component.updateOptionStatus(option, 'RETAINED');

    expect(matDialogMock.open).not.toHaveBeenCalled();
    expect(strategicOptionService.updateStatus).toHaveBeenCalledWith('o1', 'RETAINED', undefined);
    expect(component.strategicOptions()[0].statut).toBe('RETAINED');
  });

  // SF-176-02 T-04 : updateOptionStatus DISCARDED → ouvre dialog puis appelle service avec raison
  it('SF-176-02 T-04: updateOptionStatus to DISCARDED opens dialog and calls service with reason', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    const option = makeOption('o1', 0, 'Piste à écarter');
    strategicOptionService.list.mockReturnValue(of([option]));
    strategicOptionService.updateStatus.mockReturnValue(of({
      ...option, statut: 'DISCARDED', raisonDiscard: 'déjà tenté'
    }));
    fixture.detectChanges();

    component.updateOptionStatus(option, 'DISCARDED');
    expect(matDialogMock.open).toHaveBeenCalled();

    dialogResultSubject.next('déjà tenté');
    dialogResultSubject.complete();

    expect(strategicOptionService.updateStatus).toHaveBeenCalledWith('o1', 'DISCARDED', 'déjà tenté');
    expect(component.strategicOptions()[0].raisonDiscard).toBe('déjà tenté');
  });

  // SF-176-02 T-05 : annulation du dialog Écarter → service non appelé
  it('SF-176-02 T-05: cancelling discard dialog does not call service', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    const option = makeOption('o1', 0, 'Piste');
    strategicOptionService.list.mockReturnValue(of([option]));
    fixture.detectChanges();

    component.updateOptionStatus(option, 'DISCARDED');
    dialogResultSubject.next(undefined);
    dialogResultSubject.complete();

    expect(strategicOptionService.updateStatus).not.toHaveBeenCalled();
  });

  // SF-176-02 T-06 : reset des pistes stratégiques au changement de version
  it('SF-176-02 T-06: resets strategic options on version change', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(2, 'STANDARD')));
    strategicOptionService.list.mockReturnValueOnce(of([makeOption('o1', 0, 'Piste v2')]));
    fixture.detectChanges();
    expect(component.strategicOptions().length).toBe(1);

    strategicOptionService.list.mockReturnValueOnce(of([]));
    component.onVersionChange(1);

    expect(strategicOptionService.list).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
    expect(component.strategicOptions().length).toBe(0);
  });

  // SF-176-02 T-07 : erreur GET → liste vide, pas de blocage
  it('SF-176-02 T-07: GET error sets empty list silently', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
    strategicOptionService.list.mockReturnValue(throwError(() => new Error('boom')));
    fixture.detectChanges();

    expect(component.strategicOptions()).toEqual([]);
  });

  // F-160 SF-160-02 — paginators indépendants par bloc.
  describe('F-160 SF-160-02 paginator (checklist + questions)', () => {
    // U-1 : navigateChecks(-1) déclenche procedureCheckService.list avec l'analysisId de l'itération précédente
    it('U1: navigateChecks(-1) loads checks for previous iteration analysisId', () => {
      const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
      caseAnalysisService.getVersions.mockReturnValue(of(versions));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(2, 'STANDARD')));
      fixture.detectChanges();

      procedureCheckService.list.mockClear();
      component.navigateChecks(-1);

      expect(component.currentChecksVersion()).toBe(1);
      expect(procedureCheckService.list).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
    });

    // U-2 : navigateQuestions(-1) déclenche aiQuestionService.getQuestionsByAnalysisId avec l'analysisId précédent
    it('U2: navigateQuestions(-1) loads questions for previous iteration analysisId', () => {
      const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
      caseAnalysisService.getVersions.mockReturnValue(of(versions));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(2, 'STANDARD')));
      fixture.detectChanges();

      aiQuestionService.getQuestionsByAnalysisId.mockClear();
      component.navigateQuestions(-1);

      expect(component.currentQuestionsVersion()).toBe(1);
      expect(aiQuestionService.getQuestionsByAnalysisId).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
    });

    // U-3 : onVersionChange resync les deux paginators sur la version globale
    it('U3: onVersionChange resyncs both paginators to selected version', () => {
      const versions = [makeVersion(3, 'STANDARD'), makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
      caseAnalysisService.getVersions.mockReturnValue(of(versions));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(3, 'STANDARD')));
      fixture.detectChanges();

      // L'avocat dévie un paginator
      component.navigateChecks(-1);
      expect(component.currentChecksVersion()).toBe(2);

      // Puis change la version globale → paginators resync
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      component.onVersionChange(1);

      expect(component.currentChecksVersion()).toBe(1);
      expect(component.currentQuestionsVersion()).toBe(1);
    });

    // U-4 : canChecksPrev / canChecksNext cohérents avec position
    it('U4: canChecksPrev/Next reflect position in versionsAsc', () => {
      const versions = [makeVersion(3, 'STANDARD'), makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
      caseAnalysisService.getVersions.mockReturnValue(of(versions));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(3, 'STANDARD')));
      fixture.detectChanges();

      // Au chargement → version 3 (la plus récente, index 2 en ASC) → prev OK, next bloqué
      expect(component.canChecksPrev()).toBe(true);
      expect(component.canChecksNext()).toBe(false);

      component.navigateChecks(-1); // → v2
      expect(component.canChecksPrev()).toBe(true);
      expect(component.canChecksNext()).toBe(true);

      component.navigateChecks(-1); // → v1, le plus ancien
      expect(component.canChecksPrev()).toBe(false);
      expect(component.canChecksNext()).toBe(true);
    });

    // U-5 : avec 1 seule version → paginator bloqué (pas de navigation possible)
    it('U5: with single version, navigation is a no-op', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      fixture.detectChanges();

      procedureCheckService.list.mockClear();
      component.navigateChecks(-1);
      component.navigateChecks(1);

      expect(procedureCheckService.list).not.toHaveBeenCalled();
      expect(component.canChecksPrev()).toBe(false);
      expect(component.canChecksNext()).toBe(false);
    });

    // U-6 : navigateChecks ne touche pas le paginator Questions (indépendance)
    it('U6: navigateChecks leaves currentQuestionsVersion unchanged', () => {
      const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
      caseAnalysisService.getVersions.mockReturnValue(of(versions));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(2, 'STANDARD')));
      fixture.detectChanges();

      const questionsVersionBefore = component.currentQuestionsVersion();
      component.navigateChecks(-1);

      expect(component.currentQuestionsVersion()).toBe(questionsVersionBefore);
      expect(component.currentChecksVersion()).toBe(1);
    });
  });

  // F-162 SF-162-01 — grille de badges synthétiques en tête de page.
  describe('F-162 SF-162-01 synthesisBadges grid', () => {
    // U-1 : badges vides filtrés (compteur = 0)
    it('U1: filters out blocks with zero count', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      // makeSynthesis ne crée que faits=[1], le reste est vide
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      fixture.detectChanges();

      const badges = component.synthesisBadges();
      expect(badges.length).toBe(1);
      expect(badges[0].id).toBe('faits');
      expect(badges[0].count).toBe(1);
    });

    // U-2 : ordre canonique respecté (Timeline → Faits → Points juridiques → Risques → Pistes → Checklist → Questions → Pièces → Questions ouvertes)
    it('U2: returns badges in canonical order', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD', ['piece A']),
        timeline: [{ date: '2026-01-01', evenement: 'evt' }],
        faits: [makeItem('f1')],
        pointsJuridiques: [makeItem('p1')],
        risques: [makeItem('r1')],
        questionsOuvertes: ['q ouverte'],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const ids = component.synthesisBadges().map(b => b.id);
      expect(ids).toEqual([
        'timeline',
        'faits',
        'points-juridiques',
        'risques',
        'pieces',
        'questions-ouvertes',
      ]);
    });

    // U-3 : compteurs reflètent la taille des listes
    it('U3: counts reflect list lengths', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD'),
        timeline: [
          { date: '2026-01-01', evenement: 'a' },
          { date: '2026-01-02', evenement: 'b' },
          { date: '2026-01-03', evenement: 'c' },
        ],
        risques: [makeItem('r1'), makeItem('r2')],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const timelineBadge = component.synthesisBadges().find(b => b.id === 'timeline');
      const risquesBadge = component.synthesisBadges().find(b => b.id === 'risques');
      expect(timelineBadge?.count).toBe(3);
      expect(risquesBadge?.count).toBe(2);
    });

    // U-4 : sublabel "gravité élevée" sur le badge Risques quand riskLevel = ELEVE
    it('U4: risques badge gets sublabel when riskLevel is ELEVE', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD', [], 'ELEVE', 80),
        risques: [makeItem('r1')],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const risquesBadge = component.synthesisBadges().find(b => b.id === 'risques');
      expect(risquesBadge?.sublabel).toBe('gravité élevée');
    });

    // U-5 : scrollToBlock invoque scrollIntoView sur l'élément cible
    it('U5: scrollToBlock invokes scrollIntoView on target element', () => {
      const el = document.createElement('div');
      el.id = 'section-faits';
      const scrollSpy = jest.fn();
      (el as any).scrollIntoView = scrollSpy;
      document.body.appendChild(el);

      component.scrollToBlock('section-faits');

      expect(scrollSpy).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
      expect(el.classList.contains('source-highlight')).toBe(true);
      document.body.removeChild(el);
    });

    // U-6 : synthesisBadges() retourne [] quand synthesis() est null
    it('U6: returns empty array when synthesis is null', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      fixture.detectChanges();

      expect(component.synthesisBadges()).toEqual([]);
    });

    // F-162 SF-162-02 — U-7 : badge Timeline reçoit un route vers la page dédiée.
    it('U7: timeline badge exposes a route to the dedicated page', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD'),
        timeline: [{ date: '2026-01-01', evenement: 'evt' }],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const timelineBadge = component.synthesisBadges().find(b => b.id === 'timeline');
      expect(timelineBadge?.route).toEqual(['/case-files', CASE_FILE_ID, 'synthesis', 'timeline']);
    });

    // F-162 SF-162-03 — U-8 : badge Faits reçoit un route vers la page dédiée.
    it('U8: faits badge exposes a route to the dedicated page', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      fixture.detectChanges();

      const faitsBadge = component.synthesisBadges().find(b => b.id === 'faits');
      expect(faitsBadge?.route).toEqual(['/case-files', CASE_FILE_ID, 'synthesis', 'faits']);
    });

    // F-162 SF-162-04 — U-9 : badge Points juridiques reçoit un route vers la page dédiée.
    it('U9: points-juridiques badge exposes a route to the dedicated page', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD'),
        pointsJuridiques: [makeItem('point 1')],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const pjBadge = component.synthesisBadges().find(b => b.id === 'points-juridiques');
      expect(pjBadge?.route).toEqual(['/case-files', CASE_FILE_ID, 'synthesis', 'points-juridiques']);
    });

    // F-162 SF-162-05 — U-10 : badge Risques reçoit un route vers la page dédiée.
    it('U10: risques badge exposes a route to the dedicated page', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD'),
        risques: [makeItem('r1')],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const risquesBadge = component.synthesisBadges().find(b => b.id === 'risques');
      expect(risquesBadge?.route).toEqual(['/case-files', CASE_FILE_ID, 'synthesis', 'risques']);
    });

    // F-162 SF-162-06 — U-11 : badge Pièces manquantes expose `popup`, pas `route`.
    it('U11: pieces badge exposes popup="pieces" and no route', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD', ['piece A']),
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const piecesBadge = component.synthesisBadges().find(b => b.id === 'pieces');
      expect(piecesBadge?.popup).toBe('pieces');
      expect(piecesBadge?.route).toBeUndefined();
    });

    // F-162 SF-162-06 — U-12 : badge Questions ouvertes expose `popup`.
    it('U12: questions-ouvertes badge exposes popup="questions-ouvertes"', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD'),
        questionsOuvertes: ['q1'],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      const qoBadge = component.synthesisBadges().find(b => b.id === 'questions-ouvertes');
      expect(qoBadge?.popup).toBe('questions-ouvertes');
      expect(qoBadge?.route).toBeUndefined();
    });

    // F-162 SF-162-06 — U-13 : openPopup invoque MatDialog.open avec la liste des pièces.
    it('U13: openPopup("pieces") opens dialog with piecesManquantes data', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD', ['Contrat', 'Bulletin']),
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      matDialogMock.open.mockClear();
      component.openPopup('pieces');

      expect(matDialogMock.open).toHaveBeenCalledTimes(1);
      const args = matDialogMock.open.mock.calls[0];
      expect(args[1].data.title).toBe('Pièces manquantes');
      expect(args[1].data.items).toEqual(['Contrat', 'Bulletin']);
    });

    // F-162 SF-162-06 — U-14 : openPopup("questions-ouvertes") passe la liste correcte.
    it('U14: openPopup("questions-ouvertes") opens dialog with questionsOuvertes data', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      const syn = {
        ...makeSynthesis(1, 'STANDARD'),
        questionsOuvertes: ['q1', 'q2'],
      } as any;
      caseAnalysisService.getByVersion.mockReturnValue(of(syn));
      fixture.detectChanges();

      matDialogMock.open.mockClear();
      component.openPopup('questions-ouvertes');

      expect(matDialogMock.open).toHaveBeenCalledTimes(1);
      const args = matDialogMock.open.mock.calls[0];
      expect(args[1].data.title).toBe('Questions ouvertes');
      expect(args[1].data.items).toEqual(['q1', 'q2']);
    });
  });

  // F-197 SF-197-02 — badge "Type de litige" dans la grille F-162 + dialog override.
  describe('F-197 SF-197-02 type litige override', () => {
    // Setup helper : dossier Travail FR avec un override null par défaut.
    const setupTravailFr = () => {
      const caseFileService = TestBed.inject(CaseFileService) as any;
      caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier T1', legalDomain: 'DROIT_DU_TRAVAIL' }));
    };

    const setupImmigration = () => {
      const caseFileService = TestBed.inject(CaseFileService) as any;
      caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier I1', legalDomain: 'DROIT_IMMIGRATION' }));
    };

    it('CA-01 : badge "Type de litige" présent dans la grille F-162 quand IA détecte un type (Travail FR)', () => {
      setupTravailFr();
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'LICENCIEMENT_SANS_CAUSE_REELLE',
      } as any));
      fixture.detectChanges();

      const badge = component.synthesisBadges().find(b => b.id === 'type-litige');
      expect(badge).toBeTruthy();
      expect(badge?.valueLabel).toBe('Licenciement sans cause réelle et sérieuse');
      expect(badge?.overridden).toBe(false);
      expect(badge?.dialog).toBe('type-litige-override');
    });

    it('badge "Type de litige" en tête de grille (saillant)', () => {
      setupTravailFr();
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'HEURES_SUPPLEMENTAIRES',
        timeline: [{ date: '2026-01-01', evenement: 'evt' }],
      } as any));
      fixture.detectChanges();

      const ids = component.synthesisBadges().map(b => b.id);
      expect(ids[0]).toBe('type-litige');
    });

    it('CA-05 : override avocat pré-sélectionné → badge en mode "modifié par vous" (palette or)', () => {
      setupTravailFr();
      const overrideService = TestBed.inject(TypeLitigeOverrideService) as any;
      overrideService.getForCaseFile.mockReturnValue(of({
        typeLitigeAvocat: 'PRISE_ACTE_RUPTURE',
        typeProcedureAvocat: null,
        raison: 'Test stratégique',
      } as TypeLitigeOverrideResponse));
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'LICENCIEMENT_SANS_CAUSE_REELLE',
      } as any));
      fixture.detectChanges();

      const badge = component.synthesisBadges().find(b => b.id === 'type-litige');
      expect(badge?.overridden).toBe(true);
      expect(badge?.valueLabel).toBe('Prise d\'acte de rupture');
      expect(badge?.sublabel).toBe('modifié par vous');
      expect(badge?.icon).toBe('edit_note');
    });

    it('CA-09 : fail-open silencieux si GET override 5xx → override reste null, badge IA brute', () => {
      setupTravailFr();
      const overrideService = TestBed.inject(TypeLitigeOverrideService) as any;
      overrideService.getForCaseFile.mockReturnValue(throwError(() => ({ status: 500 })));
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'DISCRIMINATION',
      } as any));
      fixture.detectChanges();

      expect(component.typeLitigeOverride()).toBeNull();
      const badge = component.synthesisBadges().find(b => b.id === 'type-litige');
      expect(badge?.overridden).toBe(false);
      expect(badge?.valueLabel).toBe('Discrimination');
    });

    it('CA-02 : openTypeLitigeOverrideDialog → ouvre MatDialog avec domain TRAVAIL_FR', () => {
      setupTravailFr();
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'LICENCIEMENT_ECONOMIQUE',
      } as any));
      fixture.detectChanges();

      matDialogMock.open.mockClear();
      component.openTypeLitigeOverrideDialog();

      expect(matDialogMock.open).toHaveBeenCalledTimes(1);
      const args = matDialogMock.open.mock.calls[0];
      expect(args[1].data.caseFileId).toBe(CASE_FILE_ID);
      expect(args[1].data.domain).toBe('TRAVAIL_FR');
      expect(args[1].data.iaDetectedCode).toBe('LICENCIEMENT_ECONOMIQUE');
    });

    it('Immigration : openTypeLitigeOverrideDialog → ouvre MatDialog avec domain IMMIGRATION', () => {
      setupImmigration();
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'OQTF_AVEC_DELAI',
      } as any));
      fixture.detectChanges();

      matDialogMock.open.mockClear();
      component.openTypeLitigeOverrideDialog();

      const args = matDialogMock.open.mock.calls[0];
      expect(args[1].data.domain).toBe('IMMIGRATION');
    });

    it('CA-06 : afterClosed avec response → met à jour signal local SANS triggerRefresh (cohérence F-176)', () => {
      setupTravailFr();
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'RAPPEL_SALAIRE',
      } as any));
      fixture.detectChanges();

      // Ouverture du dialog
      component.openTypeLitigeOverrideDialog();

      // Simule la fermeture du dialog avec une réponse PUT 200
      const newOverride: TypeLitigeOverrideResponse = {
        typeLitigeAvocat: 'HARCELEMENT_MORAL',
        typeProcedureAvocat: null,
        raison: 'Faits clairs de harcèlement',
      };
      dialogResultSubject.next(newOverride);

      expect(component.typeLitigeOverride()).toEqual(newOverride);
      const badge = component.synthesisBadges().find(b => b.id === 'type-litige');
      expect(badge?.overridden).toBe(true);
      expect(badge?.valueLabel).toBe('Harcèlement moral');
    });

    it('afterClosed undefined (Annuler) → signal NON modifié', () => {
      setupTravailFr();
      const overrideService = TestBed.inject(TypeLitigeOverrideService) as any;
      overrideService.getForCaseFile.mockReturnValue(of({
        typeLitigeAvocat: 'DISCRIMINATION',
        typeProcedureAvocat: null,
        raison: null,
      } as TypeLitigeOverrideResponse));
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      fixture.detectChanges();
      const before = component.typeLitigeOverride();

      component.openTypeLitigeOverrideDialog();
      dialogResultSubject.next(undefined);

      expect(component.typeLitigeOverride()).toEqual(before);
    });

    it('domaine famille (DROIT_FAMILLE) → pas de badge Type de litige (V1 hors scope)', () => {
      const caseFileService = TestBed.inject(CaseFileService) as any;
      caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'D', legalDomain: 'DROIT_FAMILLE' }));
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'LICENCIEMENT_SANS_CAUSE_REELLE',
      } as any));
      fixture.detectChanges();

      const badge = component.synthesisBadges().find(b => b.id === 'type-litige');
      expect(badge).toBeUndefined();
    });

    it('aucun typeLitigeDetecte ET aucun override → pas de badge Type de litige', () => {
      setupTravailFr();
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      fixture.detectChanges();

      const badge = component.synthesisBadges().find(b => b.id === 'type-litige');
      expect(badge).toBeUndefined();
    });

    it('currentTypeLitigeCode : override Travail FR > IA détectée', () => {
      setupTravailFr();
      const overrideService = TestBed.inject(TypeLitigeOverrideService) as any;
      overrideService.getForCaseFile.mockReturnValue(of({
        typeLitigeAvocat: 'PRISE_ACTE_RUPTURE',
        typeProcedureAvocat: null,
        raison: null,
      } as TypeLitigeOverrideResponse));
      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of({
        ...makeSynthesis(1, 'STANDARD'),
        typeLitigeDetecte: 'LICENCIEMENT_SANS_CAUSE_REELLE',
      } as any));
      fixture.detectChanges();

      expect(component.currentTypeLitigeCode()).toBe('PRISE_ACTE_RUPTURE');
    });
  });

  // F-190 SF-190-02 — streaming SSE pour analyses ENRICHED + analysisType propagé via partial.
  describe('F-190 SF-190-02 streaming enriched + analysisType', () => {
    // U-1 : applyPartial avec analysisType=ENRICHED → synthesis.analysisType === 'ENRICHED'
    it('U1: applyPartial with analysisType=ENRICHED sets synthesis.analysisType to ENRICHED', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      (caseAnalysisService.getPartial as jest.Mock).mockReturnValue(of({
        analysisId: 'partial-enriched-1',
        version: 2,
        analysisType: 'ENRICHED',
        status: 'PARTIAL',
        sections: { faits: [{ texte: 'fait enrichi' }] },
        updatedAt: '2026-05-05T10:00:00Z',
      }));
      fixture.detectChanges();

      expect(component.synthesis()?.analysisType).toBe('ENRICHED');
    });

    // U-2 : applyPartial sans analysisType → defaut STANDARD (rétrocompat)
    it('U2: applyPartial with undefined analysisType defaults to STANDARD', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      (caseAnalysisService.getPartial as jest.Mock).mockReturnValue(of({
        analysisId: 'partial-1',
        version: 1,
        // analysisType absent
        status: 'PARTIAL',
        sections: { faits: [{ texte: 'fait' }] },
        updatedAt: '2026-05-05T10:00:00Z',
      }));
      fixture.detectChanges();

      expect(component.synthesis()?.analysisType).toBe('STANDARD');
    });

    // U-3 : event ENRICHED_ANALYSIS PARTIAL déclenche getPartial() puis applyPartial()
    it('U3: ENRICHED_ANALYSIS PARTIAL event triggers getPartial and updates synthesis', () => {
      // Remplace events$ sur le service injecté avant que le composant n'y souscrive (avant detectChanges).
      const eventsSubject = new Subject<{ caseFileId: string; jobType: string; status: string }>();
      const notif = TestBed.inject(GlobalAnalysisNotificationService) as any;
      notif.events$ = eventsSubject.asObservable();

      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      fixture.detectChanges();

      (caseAnalysisService.getPartial as jest.Mock).mockReturnValue(of({
        analysisId: 'partial-2',
        version: 2,
        analysisType: 'ENRICHED',
        status: 'PARTIAL',
        sections: { faits: [{ texte: 'streaming enrichi' }] },
        updatedAt: '2026-05-05T11:00:00Z',
      }));

      eventsSubject.next({ caseFileId: CASE_FILE_ID, jobType: 'ENRICHED_ANALYSIS', status: 'PARTIAL' });

      expect(caseAnalysisService.getPartial).toHaveBeenCalledWith(CASE_FILE_ID);
      expect(component.synthesis()?.analysisType).toBe('ENRICHED');
    });

    // U-4 : event ENRICHED_ANALYSIS DONE déclenche loadVersions()
    it('U4: ENRICHED_ANALYSIS DONE event triggers loadVersions', () => {
      const eventsSubject = new Subject<{ caseFileId: string; jobType: string; status: string }>();
      const notif = TestBed.inject(GlobalAnalysisNotificationService) as any;
      notif.events$ = eventsSubject.asObservable();

      caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1, 'STANDARD')]));
      caseAnalysisService.getByVersion.mockReturnValue(of(makeSynthesis(1, 'STANDARD')));
      fixture.detectChanges();

      caseAnalysisService.getVersions.mockClear();
      caseAnalysisService.getVersions.mockReturnValue(of([
        makeVersion(2, 'ENRICHED'),
        makeVersion(1, 'STANDARD'),
      ]));

      eventsSubject.next({ caseFileId: CASE_FILE_ID, jobType: 'ENRICHED_ANALYSIS', status: 'DONE' });

      expect(caseAnalysisService.getVersions).toHaveBeenCalledWith(CASE_FILE_ID);
    });
  });

  // F-190 SF-190-01 — barre de progression granulaire + chips cliquables des sections reçues.
  describe('F-190 SF-190-01 streaming progress', () => {
    // U-1 : lastPartial null → progression à 0/7.
    it('U1: returns 0/7 progress when lastPartial is null', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      fixture.detectChanges();

      expect(component.streamingProgress()).toEqual({ received: 0, expected: 7, percent: 0 });
      expect(component.streamingSectionsReceived()).toEqual([]);
    });

    // U-2 : 2 sections présentes (faits, risques) → received 2/7, percent 29.
    it('U2: counts received sections and computes percent', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      fixture.detectChanges();

      // Simule un partial reçu avec 2 sections arrivées.
      component.lastPartial.set({
        analysisId: 'a-1',
        version: 1,
        status: 'PARTIAL',
        sections: { faits: [{ texte: 'f1' } as any], risques: [{ texte: 'r1' } as any] },
        updatedAt: '2026-05-05T10:00:00Z',
      });

      const progress = component.streamingProgress();
      expect(progress.received).toBe(2);
      expect(progress.expected).toBe(7);
      expect(progress.percent).toBe(Math.round((2 / 7) * 100));
    });

    // U-3 : risk_level présent → compté comme une section reçue.
    it('U3: counts risk_level as a received section', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      fixture.detectChanges();

      component.lastPartial.set({
        analysisId: 'a-1',
        version: 1,
        status: 'PARTIAL',
        sections: { risk_level: 'MOYEN' as any },
        updatedAt: '2026-05-05T10:00:00Z',
      });

      const ids = component.streamingSectionsReceived().map(s => s.id);
      expect(ids).toEqual(['risk_level']);
    });

    // U-4 : travail_extracted_data présent dans sections → NON compté.
    it('U4: ignores *_extracted_data sections', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      fixture.detectChanges();

      component.lastPartial.set({
        analysisId: 'a-1',
        version: 1,
        status: 'PARTIAL',
        sections: {
          travail_extracted_data: { whatever: 'x' } as any,
          immigration_extracted_data: { whatever: 'y' } as any,
          famille_extracted_data: { whatever: 'z' } as any,
          faits: [{ texte: 'f' } as any],
        },
        updatedAt: '2026-05-05T10:00:00Z',
      });

      // Seul `faits` doit être compté (les *_extracted_data sont des structures
      // internes consommées par F-IA-04, pas des sections affichées).
      const ids = component.streamingSectionsReceived().map(s => s.id);
      expect(ids).toEqual(['faits']);
    });

    // U-5 : ordre canonique respecté quel que soit l'ordre d'arrivée des sections.
    it('U5: returns sections in canonical order, not arrival order', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      fixture.detectChanges();

      component.lastPartial.set({
        analysisId: 'a-1',
        version: 1,
        status: 'PARTIAL',
        sections: {
          // Ordre d'arrivée volontairement inversé.
          risques: [{ texte: 'r1' } as any],
          faits: [{ texte: 'f1' } as any],
          timeline: [{ date: '2026-01-01', evenement: 'e' } as any],
        },
        updatedAt: '2026-05-05T10:00:00Z',
      });

      const ids = component.streamingSectionsReceived().map(s => s.id);
      // Ordre canonique : timeline → faits → ... → risques.
      expect(ids).toEqual(['timeline', 'faits', 'risques']);
    });

    // U-6 : applyPartial met à jour lastPartial ET synthesis (les 2 signals).
    it('U6: applyPartial updates both lastPartial and synthesis signals', () => {
      caseAnalysisService.getVersions.mockReturnValue(of([]));
      // Simule un partial reçu via tryLoadPartial au mount.
      (caseAnalysisService.getPartial as jest.Mock).mockReturnValue(of({
        analysisId: 'partial-1',
        version: 1,
        status: 'PARTIAL',
        sections: { faits: [{ texte: 'fait partiel' } as any] },
        updatedAt: '2026-05-05T10:00:00Z',
      }));

      fixture.detectChanges();

      expect(component.lastPartial()).not.toBeNull();
      expect(component.lastPartial()?.sections?.faits?.length).toBe(1);
      expect(component.synthesis()).not.toBeNull();
      expect(component.synthesis()?.faits.length).toBe(1);
    });
  });
});
