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
import { ProcedureCheckService } from '../../core/services/procedure-check.service';
import { of, throwError } from 'rxjs';
import { AnalysisItem, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

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

describe('SynthesisComponent', () => {
  let fixture: ComponentFixture<SynthesisComponent>;
  let component: SynthesisComponent;
  let caseAnalysisService: jasmine.SpyObj<CaseAnalysisService>;
  let aiQuestionService: jasmine.SpyObj<AiQuestionService>;
  let procedureCheckService: jasmine.SpyObj<ProcedureCheckService>;

  beforeEach(async () => {
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion', 'getAnalysis']);
    aiQuestionService = jasmine.createSpyObj('AiQuestionService', ['getQuestions', 'getQuestionsByAnalysisId']);
    procedureCheckService = jasmine.createSpyObj('ProcedureCheckService', ['list', 'updateStatus']);

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.and.returnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    aiQuestionService.getQuestionsByAnalysisId.and.returnValue(of([]));
    aiQuestionService.getQuestions.and.returnValue(of([]));
    procedureCheckService.list.and.returnValue(of([]));

    const chatService = jasmine.createSpyObj('ChatService', ['getHistory', 'sendMessage']);
    chatService.getHistory.and.returnValue(of([]));

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
        { provide: PdfExportService, useValue: jasmine.createSpyObj('PdfExportService', ['export']) },
        { provide: ProcedureCheckService, useValue: procedureCheckService },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SynthesisComponent);
    component = fixture.componentInstance;
  });

  // T-01 : au chargement → version la plus récente sélectionnée (index 0)
  it('selects most recent version on load', () => {
    const versions = [makeVersion(3, 'ENRICHED'), makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(3, 'ENRICHED')));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 3);
    expect(component.synthesis()?.version).toBe(3);
  });

  // T-02 : changement de version → loadSynthesisForVersion et loadQuestionsForVersion appelés
  it('reloads synthesis and questions on version change', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    component.onVersionChange(1);

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 1);
    expect(aiQuestionService.getQuestionsByAnalysisId).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
  });

  // T-03 : analysisType ENRICHED → isEnriched() true
  it('returns true for isEnriched when analysisType is ENRICHED', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'ENRICHED')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'ENRICHED')));
    fixture.detectChanges();

    expect(component.isEnriched()).toBeTrue();
  });

  // T-04 : analysisType STANDARD → isEnriched() false
  it('returns false for isEnriched when analysisType is STANDARD', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    expect(component.isEnriched()).toBeFalse();
  });

  // T-05 : une seule version → versions().length === 1
  it('exposes single version without selector interaction', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    expect(component.versions().length).toBe(1);
  });

  // T-06 : versions vides → synthesis() null, loading false
  it('leaves synthesis null when no versions available', () => {
    caseAnalysisService.getVersions.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.synthesis()).toBeNull();
    expect(component.loading()).toBeFalse();
  });

  // T-07 : changement de version ne recharge pas le chat
  it('does not reload chat on version change', () => {
    const chatService = TestBed.inject(ChatService) as jasmine.SpyObj<ChatService>;
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    const callsBefore = chatService.getHistory.calls.count();
    component.onVersionChange(1);

    expect(chatService.getHistory.calls.count()).toBe(callsBefore);
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
    const answerService = TestBed.inject(AiQuestionAnswerService) as jasmine.SpyObj<AiQuestionAnswerService>;
    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.submitEdit(q, '   ');
    expect(answerService.submitAnswer).not.toHaveBeenCalled();
  });

  // T-14 : submitEdit nominal → service appelé, question mise à jour, editingQuestionId null
  it('submitEdit calls service and updates question on success', () => {
    const answerService = TestBed.inject(AiQuestionAnswerService) as jasmine.SpyObj<AiQuestionAnswerService>;
    answerService.submitAnswer.and.returnValue(of(undefined));

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
    const answerService = TestBed.inject(AiQuestionAnswerService) as jasmine.SpyObj<AiQuestionAnswerService>;
    const snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    answerService.submitAnswer.and.returnValue(throwError(() => new Error('API error')));
    spyOn(snackBar, 'open');

    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.startEdit(q);
    component.submitEdit(q, 'Nouvelle réponse');

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('modification'), 'Fermer', jasmine.any(Object)
    );
    expect(component.editingQuestionId()).toBe('q-1');
  });

  // T-A1 : reAnalyze succès → trackEvent analysis_launched ENRICHED
  it('reAnalyze success → trackEvent analysis_launched ENRICHED', () => {
    const reAnalysisService = TestBed.inject(ReAnalysisService) as jasmine.SpyObj<ReAnalysisService>;
    const analyticsService = TestBed.inject(AnalyticsService) as jasmine.SpyObj<AnalyticsService>;
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    reAnalysisService.reAnalyze.and.returnValue(of(undefined));
    spyOn(router, 'navigate');
    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });

    component.reAnalyze();

    expect(analyticsService.trackEvent).toHaveBeenCalledWith('analysis_launched', { type: 'ENRICHED' });
  });

  // T-A2 : exportPdf succès → trackEvent pdf_exported
  it('exportPdf success → trackEvent pdf_exported', () => {
    const analyticsService = TestBed.inject(AnalyticsService) as jasmine.SpyObj<AnalyticsService>;
    const pdfExportService = TestBed.inject(PdfExportService) as jasmine.SpyObj<PdfExportService>;
    component.caseFile.set({ id: CASE_FILE_ID, title: 'Test', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '', lastDocumentDeletedAt: null, riskLevel: null, riskScore: null });
    component.synthesis.set(makeSynthesis(1, 'STANDARD'));

    component.exportPdf();

    expect(pdfExportService.export).toHaveBeenCalled();
    expect(analyticsService.trackEvent).toHaveBeenCalledWith('pdf_exported');
  });

  // T-16 : onVersionChange réinitialise editingQuestionId
  it('onVersionChange resets editingQuestionId', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    fixture.detectChanges();

    const q: AiQuestion = { id: 'q-1', orderIndex: 0, questionText: 'Q?', answerText: 'R' };
    component.startEdit(q);
    component.onVersionChange(1);

    expect(component.editingQuestionId()).toBeNull();
  });

  // T-20 : piecesManquantes non vides → section rendue dans le template
  it('renders pieces manquantes section when list is non-empty', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD', ['Contrat de travail', 'Bulletins de salaire'])));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Pièces manquantes');
    expect(el.textContent).toContain('Contrat de travail');
  });

  // T-21 : piecesManquantes vides → section absente du template
  it('does not render pieces manquantes section when list is empty', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD', [])));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).not.toContain('Pièces manquantes');
  });

  // TC-01 : loadChecksForVersion appelé au chargement initial
  it('calls loadChecksForVersion for the most recent version on load', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(2, 'STANDARD')));

    fixture.detectChanges();

    expect(procedureCheckService.list).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-2');
  });

  // TC-02 : onVersionChange réinitialise procedureChecks puis recharge
  it('onVersionChange resets procedureChecks and reloads for new version', () => {
    const versions = [makeVersion(2, 'STANDARD'), makeVersion(1, 'STANDARD')];
    caseAnalysisService.getVersions.and.returnValue(of(versions));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    component.procedureChecks.set([makeCheck('c1', 1)]);

    fixture.detectChanges();
    component.onVersionChange(1);

    expect(procedureCheckService.list).toHaveBeenCalledWith(CASE_FILE_ID, 'analysis-1');
  });

  // TC-03 : checks vides → panneau checklist absent du template
  it('does not render checklist panel when procedureChecks is empty', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD')));
    procedureCheckService.list.and.returnValue(of([]));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).not.toContain('Checklist procédurale');
  });

  // TC-04 : updateCheckStatus succès → statut mis à jour localement
  it('updateCheckStatus updates check status locally on success', () => {
    const check = makeCheck('c1', 1, 'TO_CHECK');
    component.procedureChecks.set([check]);
    const updated: ProcedureCheck = { ...check, statut: 'VERIFIED' };
    procedureCheckService.updateStatus.and.returnValue(of(updated));

    component.updateCheckStatus(check, 'VERIFIED');

    expect(component.procedureChecks()[0].statut).toBe('VERIFIED');
    expect(component.updatingCheckId()).toBeNull();
  });

  // TS-01 : item avec source → badge source affiché
  it('renders source badge when item has a source', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of({
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
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of({
      ...makeSynthesis(1, 'STANDARD'),
      faits: [makeItem('Fait sans source')]
    }));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Fait sans source');
    expect(el.querySelectorAll('.source-badge').length).toBe(0);
  });

  // TC-05 : updateCheckStatus erreur → snackbar, statut non modifié
  it('updateCheckStatus shows snackbar on error without modifying status', () => {
    const snackBar = TestBed.inject(MatSnackBar);
    spyOn(snackBar, 'open');

    const check = makeCheck('c1', 1, 'TO_CHECK');
    component.procedureChecks.set([check]);
    procedureCheckService.updateStatus.and.returnValue(throwError(() => ({ status: 500 })));

    component.updateCheckStatus(check, 'VERIFIED');

    expect(component.procedureChecks()[0].statut).toBe('TO_CHECK');
    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('statut'), 'Fermer', jasmine.any(Object)
    );
    expect(component.updatingCheckId()).toBeNull();
  });

  // R-03 : badge de risque présent dans le header si riskLevel non null
  it('R-03: synthesis avec riskLevel → badge .risk-badge présent dans le header', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD', [], 'ELEVE', 82)));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.risk-badge');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('Élevé');
    expect(badge.textContent).toContain('82');
  });

  // R-04 : badge absent si riskLevel null
  it('R-04: synthesis sans riskLevel → badge .risk-badge absent', () => {
    caseAnalysisService.getVersions.and.returnValue(of([makeVersion(1, 'STANDARD')]));
    caseAnalysisService.getByVersion.and.returnValue(of(makeSynthesis(1, 'STANDARD', [], null, null)));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.risk-badge');
    expect(badge).toBeNull();
  });
});
