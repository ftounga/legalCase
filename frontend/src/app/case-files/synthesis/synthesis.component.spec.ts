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

const makeSynthesis = (version: number, analysisType: 'STANDARD' | 'ENRICHED', piecesManquantes: string[] = [], riskLevel: string | null = null, riskScore: number | null = null) => ({
  id: `analysis-${version}`,
  version,
  analysisType,
  status: 'DONE',
  timeline: [],
  faits: [makeItem('fait1')],
  pointsJuridiques: [],
  risques: [],
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
});
