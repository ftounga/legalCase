import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { CaseFileDetailComponent } from './case-file-detail.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseFileStatusService } from '../../core/services/case-file-status.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { TypeLitigeOverrideService } from '../../core/services/type-litige-override.service';
import { CaseAnalysisCommandService } from '../../core/services/case-analysis-command.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AiQuestionAnswerService } from '../../core/services/ai-question-answer.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { CaseFileStatsService } from '../../core/services/case-file-stats.service';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { CaseNoteService } from '../../core/services/case-note.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { PrudhomeFicheService } from '../../core/services/prudhome-fiche.service';
import { ImmigrationChecklistService } from '../../core/services/immigration-checklist.service';
import { TimeService } from '../../core/services/time.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError, Subject } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { CaseFile } from '../../core/models/case-file.model';
import { Document } from '../../core/models/document.model';
import { AnalysisJob } from '../../core/models/analysis-job.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { AiQuestion } from '../../core/models/ai-question.model';

const mockCaseFile: CaseFile = {
  id: 'cf1', title: 'Dossier A', legalDomain: 'DROIT_DU_TRAVAIL',
  description: 'Description test', status: 'OPEN', createdAt: '2026-03-17T10:00:00Z',
  lastDocumentDeletedAt: null, riskLevel: null, riskScore: null
};

const mockDocument: Document = {
  id: 'doc1', caseFileId: 'cf1', originalFilename: 'contrat.pdf',
  contentType: 'application/pdf', fileSize: 12345, createdAt: '2026-03-17T10:00:00Z'
};

const mockJobs: AnalysisJob[] = [
  { jobType: 'CHUNK_ANALYSIS', status: 'DONE', totalItems: 10, processedItems: 10, progressPercentage: 100 },
  { jobType: 'DOCUMENT_ANALYSIS', status: 'PROCESSING', totalItems: 3, processedItems: 1, progressPercentage: 33 },
  { jobType: 'CASE_ANALYSIS', status: 'PENDING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
];

describe('CaseFileDetailComponent', () => {
  let fixture: ComponentFixture<CaseFileDetailComponent>;
  let component: CaseFileDetailComponent;
  let caseFileServiceSpy: jest.Mocked<CaseFileService>;
  let caseFileStatusServiceSpy: jest.Mocked<CaseFileStatusService>;
  let documentServiceSpy: jest.Mocked<DocumentService>;
  let analysisJobServiceSpy: jest.Mocked<AnalysisJobService>;
  let caseAnalysisServiceSpy: jest.Mocked<CaseAnalysisService>;
  let caseAnalysisCommandServiceSpy: jest.Mocked<CaseAnalysisCommandService>;
  let aiQuestionServiceSpy: jest.Mocked<AiQuestionService>;
  let aiQuestionAnswerServiceSpy: jest.Mocked<AiQuestionAnswerService>;
  let reAnalysisServiceSpy: jest.Mocked<ReAnalysisService>;
  let workspaceMemberServiceSpy: jest.Mocked<WorkspaceMemberService>;
  let caseFileStatsServiceSpy: jest.Mocked<CaseFileStatsService>;
  let snackBarSpy: jest.Mocked<MatSnackBar>;
  let dialogSpy: jest.Mocked<MatDialog>;
  let caseNoteServiceSpy: jest.Mocked<CaseNoteService>;
  let caseDeadlineServiceSpy: jest.Mocked<CaseDeadlineService>;
  let analyticsServiceSpy: jest.Mocked<AnalyticsService>;
  let analysisEventsSubject: Subject<any>;

  beforeEach(async () => {
    caseNoteServiceSpy = jasmine.createSpyObj('CaseNoteService', ['list', 'create', 'update', 'delete']);
    caseNoteServiceSpy.list.mockReturnValue(of([]));
    caseDeadlineServiceSpy = jasmine.createSpyObj('CaseDeadlineService', ['list', 'create', 'update', 'delete']);
    caseDeadlineServiceSpy.list.mockReturnValue(of([]));
    analyticsServiceSpy = jasmine.createSpyObj('AnalyticsService', ['trackEvent']);
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['getById', 'exportZip', 'getDecisionToolsVisibility']);
    (caseFileServiceSpy as any).getDecisionToolsVisibility.mockReturnValue(of({ alwaysOn: [], contextual: [], catalog: [] }));
    caseFileStatusServiceSpy = jasmine.createSpyObj('CaseFileStatusService', ['close', 'reopen', 'delete']);
    documentServiceSpy = jasmine.createSpyObj('DocumentService', ['list', 'upload', 'uploadWithProgress', 'downloadUrl', 'delete']);
    analysisJobServiceSpy = jasmine.createSpyObj('AnalysisJobService', ['getJobs']);
    caseAnalysisServiceSpy = jasmine.createSpyObj('CaseAnalysisService', ['getAnalysis', 'getPartial']);
    caseAnalysisCommandServiceSpy = jasmine.createSpyObj('CaseAnalysisCommandService', ['triggerAnalysis']);
    aiQuestionServiceSpy = jasmine.createSpyObj('AiQuestionService', ['getQuestions']);
    aiQuestionAnswerServiceSpy = jasmine.createSpyObj('AiQuestionAnswerService', ['submitAnswer']);
    reAnalysisServiceSpy = jasmine.createSpyObj('ReAnalysisService', ['reAnalyze']);
    workspaceMemberServiceSpy = jasmine.createSpyObj('WorkspaceMemberService', ['getMembers']);
    caseFileStatsServiceSpy = jasmine.createSpyObj('CaseFileStatsService', ['getStats']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

    const workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of({ id: 'ws1', name: 'Test', slug: 'test', planCode: 'STARTER', status: 'ACTIVE', country: 'FRANCE' }));
    (workspaceServiceSpy as any).workspaceSwitched$ = new Subject<void>().asObservable();

    caseFileServiceSpy.getById.mockReturnValue(of(mockCaseFile));
    caseFileStatusServiceSpy.close.mockReturnValue(of({ ...mockCaseFile, status: 'CLOSED' }));
    caseFileStatusServiceSpy.reopen.mockReturnValue(of({ ...mockCaseFile, status: 'OPEN' }));
    caseFileStatusServiceSpy.delete.mockReturnValue(of(undefined));
    documentServiceSpy.list.mockReturnValue(of([mockDocument]));
    documentServiceSpy.downloadUrl.mockReturnValue('/api/v1/case-files/cf1/documents/doc1/download');
    analysisJobServiceSpy.getJobs.mockReturnValue(of([]));
    caseAnalysisServiceSpy.getAnalysis.mockReturnValue(throwError(() => new Error('404')));
    caseAnalysisServiceSpy.getPartial.mockReturnValue(throwError(() => new Error('404')));
    aiQuestionServiceSpy.getQuestions.mockReturnValue(of([]));
    aiQuestionAnswerServiceSpy.submitAnswer.mockReturnValue(of(undefined));
    reAnalysisServiceSpy.reAnalyze.mockReturnValue(of(undefined));
    caseAnalysisCommandServiceSpy.triggerAnalysis.mockReturnValue(of(undefined));
    caseFileStatsServiceSpy.getStats.mockReturnValue(of({ documentCount: 1, analysisCount: 0, totalTokens: 0 }));
    workspaceMemberServiceSpy.getMembers.mockReturnValue(of([
      { userId: 'user-1', email: 'owner@test.com', firstName: null, lastName: null, memberRole: 'OWNER', createdAt: '' }
    ]));

    await TestBed.configureTestingModule({
      imports: [CaseFileDetailComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: CaseFileStatusService, useValue: caseFileStatusServiceSpy },
        { provide: DocumentService, useValue: documentServiceSpy },
        { provide: AnalysisJobService, useValue: analysisJobServiceSpy },
        { provide: CaseAnalysisService, useValue: caseAnalysisServiceSpy },
        { provide: CaseAnalysisCommandService, useValue: caseAnalysisCommandServiceSpy },
        { provide: AiQuestionService, useValue: aiQuestionServiceSpy },
        { provide: AiQuestionAnswerService, useValue: aiQuestionAnswerServiceSpy },
        { provide: ReAnalysisService, useValue: reAnalysisServiceSpy },
        { provide: WorkspaceMemberService, useValue: workspaceMemberServiceSpy },
        { provide: CaseFileStatsService, useValue: caseFileStatsServiceSpy },
        (() => {
          analysisEventsSubject = new Subject<any>();
          return {
            provide: GlobalAnalysisNotificationService,
            useValue: { events$: analysisEventsSubject.asObservable(), track: jest.fn() }
          };
        })(),
        {
          provide: AuthService,
          useValue: { currentUser: signal({ id: 'user-1', email: 'owner@test.com', firstName: null, lastName: null, provider: 'GOOGLE', isSuperAdmin: false }) }
        },
        { provide: CaseNoteService, useValue: caseNoteServiceSpy },
        { provide: CaseDeadlineService, useValue: caseDeadlineServiceSpy },
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: PrudhomeFicheService, useValue: { get: jest.fn().mockReturnValue(of(null)), save: jest.fn() } },
        { provide: ImmigrationChecklistService, useValue: { get: jest.fn().mockReturnValue(of(null)), upsert: jest.fn() } },
        {
          provide: TimeService,
          useValue: {
            activeEntry: signal(null),
            entries: signal([]),
            loadEntries: jest.fn().mockReturnValue(of(void 0)),
            getBillingRate: jest.fn().mockReturnValue(of(null)),
            startTimer: jest.fn(),
            stopTimer: jest.fn(),
            formatDuration: jest.fn().mockReturnValue('0s')
          }
        },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        // F-197 SF-197-02 — stub GET override (par défaut : aucun override posé).
        { provide: TypeLitigeOverrideService, useValue: {
          getForCaseFile: jest.fn().mockReturnValue(of({ typeLitigeAvocat: null, typeProcedureAvocat: null, raison: null })),
          update: jest.fn().mockReturnValue(of({})),
        } },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'cf1' }) } } },
        provideAnimationsAsync(),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseFileDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit — charge le dossier et les documents', () => {
    expect(caseFileServiceSpy.getById).toHaveBeenCalledWith('cf1');
    expect(documentServiceSpy.list).toHaveBeenCalledWith('cf1');
    expect(component.caseFile()).toEqual(mockCaseFile);
    expect(component.documents().length).toBe(1);
    expect(component.loading()).toBe(false);
  });

  it('ngOnInit — erreur dossier → snackbar + loading false', () => {
    caseFileServiceSpy.getById.mockReturnValue(throwError(() => new Error('404')));
    component.ngOnInit();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
  });

  it('loadDocuments — erreur → snackbar affiché', () => {
    documentServiceSpy.list.mockReturnValue(throwError(() => new Error('500')));
    component.loadDocuments('cf1');
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('onFileSelected — ajoute les fichiers valides au panier sans uploader', () => {
    const file = new File(['content'], 'avenant.pdf', { type: 'application/pdf' });
    const event = { target: { files: [file], value: '' } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.pendingFiles().length).toBe(1);
    expect(component.pendingFiles()[0].name).toBe('avenant.pdf');
    expect(documentServiceSpy.upload).not.toHaveBeenCalled();
  });

  it('onFileSelected — fichier > 50 Mo rejeté + snackbar, panier intact', () => {
    const bigFile = new File(['x'], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(bigFile, 'size', { value: 51 * 1024 * 1024 });
    const event = { target: { files: [bigFile], value: '' } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.pendingFiles().length).toBe(0);
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(documentServiceSpy.upload).not.toHaveBeenCalled();
  });

  it('removePendingFile — retire le fichier du panier', () => {
    const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' });
    component.pendingFiles.set([file]);

    component.removePendingFile(file);

    expect(component.pendingFiles().length).toBe(0);
  });

  it('canSubmitUpload — true si panier non vide et canUpload', () => {
    const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' });
    component.pendingFiles.set([file]);

    expect(component.canSubmitUpload()).toBe(true);
  });

  it('canSubmitUpload — false si panier vide', () => {
    component.pendingFiles.set([]);
    expect(component.canSubmitUpload()).toBe(false);
  });

  it('uploadPendingFiles — succès → documents mis à jour, panier vidé', () => {
    const newDoc: Document = { ...mockDocument, id: 'doc2', originalFilename: 'avenant.pdf' };
    documentServiceSpy.uploadWithProgress.mockReturnValue(of(new HttpResponse({ body: newDoc })));

    const file = new File(['content'], 'avenant.pdf', { type: 'application/pdf' });
    component.pendingFiles.set([file]);

    component.uploadPendingFiles();

    expect(component.pendingFiles().length).toBe(0);
    expect(component.documents()[0].originalFilename).toBe('avenant.pdf');
    expect(component.uploading()).toBe(false);
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('uploadPendingFiles — erreur → snackbar erreur, panier vidé', () => {
    documentServiceSpy.uploadWithProgress.mockReturnValue(throwError(() => ({ status: 500 })));

    const file = new File(['content'], 'bad.pdf', { type: 'application/pdf' });
    component.pendingFiles.set([file]);

    component.uploadPendingFiles();

    expect(component.pendingFiles().length).toBe(0);
    expect(component.uploading()).toBe(false);
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('formatSize — octets', () => {
    expect(component.formatSize(500)).toBe('500 o');
  });

  it('formatSize — kilooctets', () => {
    expect(component.formatSize(2048)).toBe('2.0 Ko');
  });

  it('formatSize — mégaoctets', () => {
    expect(component.formatSize(5 * 1024 * 1024)).toBe('5.0 Mo');
  });

  it('downloadUrl — retourne l\'URL de téléchargement', () => {
    const url = component.downloadUrl(mockDocument);
    expect(url).toBe('/api/v1/case-files/cf1/documents/doc1/download');
  });

  it('statusLabel OPEN → "Ouvert"', () => {
    expect(component.statusLabel('OPEN')).toBe('Ouvert');
  });

  // --- Tests SF-11-03 : section Analyse IA ---

  it('section Analyse IA absente si aucun job', () => {
    analysisJobServiceSpy.getJobs.mockReturnValue(of([]));
    component.loadAnalysisJobs('cf1');
    fixture.detectChanges();

    const section = fixture.nativeElement.querySelector('.analysis-card');
    expect(section).toBeNull();
  });

  it('section Analyse IA présente si jobs non vides', () => {
    analysisJobServiceSpy.getJobs.mockReturnValue(of(mockJobs));
    component.loadAnalysisJobs('cf1');
    fixture.detectChanges();

    const pipeline = fixture.nativeElement.querySelector('app-analysis-pipeline');
    expect(pipeline).not.toBeNull(); // pipeline remplace les analysis-job-row
  });

  it('jobTypeLabel — retourne le libellé correct par jobType', () => {
    expect(component.jobTypeLabel('CHUNK_ANALYSIS')).toBe('Analyse des segments');
    expect(component.jobTypeLabel('DOCUMENT_ANALYSIS')).toBe('Analyse des documents');
    expect(component.jobTypeLabel('CASE_ANALYSIS')).toBe('Synthèse du dossier');
  });

  it('jobStatusClass — retourne la bonne classe CSS par statut', () => {
    expect(component.jobStatusClass('DONE')).toBe('badge--success');
    expect(component.jobStatusClass('PROCESSING')).toBe('badge--success');
    expect(component.jobStatusClass('PENDING')).toBe('badge--warning');
    expect(component.jobStatusClass('FAILED')).toBe('badge--error');
  });

  // --- Tests SF-12-02 : section Synthèse ---

  it('section Synthèse absente si synthesis() est null', () => {
    fixture.detectChanges();
    const section = fixture.nativeElement.querySelector('.synthesis-card');
    expect(section).toBeNull();
  });

  it('timeline masquée si tableau vide', () => {
    const mockSynthesis: CaseAnalysisResult = {
      id: 'analysis-1',
      version: 1,
      analysisType: 'STANDARD',
      status: 'DONE',
      timeline: [],
      faits: [{ texte: 'fait1', source: null, extrait: null }],
      pointsJuridiques: [],
      risques: [],
      questionsOuvertes: [],
      piecesManquantes: [],
      riskLevel: null,
      riskScore: null,
      modelUsed: null,
      updatedAt: null
    };
    component.synthesis.set(mockSynthesis);
    fixture.detectChanges();

    const timelineEntries = fixture.nativeElement.querySelectorAll('.timeline-entry');
    expect(timelineEntries.length).toBe(0);
  });

  // --- Tests SF-13-03 : section Questions IA ---

  it('section Questions IA absente si questions() vide', () => {
    fixture.detectChanges();
    const section = fixture.nativeElement.querySelector('.questions-card');
    expect(section).toBeNull();
  });

  it('loadQuestions — erreur API → questions() reste vide', () => {
    aiQuestionServiceSpy.getQuestions.mockReturnValue(throwError(() => new Error('500')));
    component.loadQuestions('cf1');
    expect(component.questions()).toEqual([]);
  });

  it('jobTypeLabel — QUESTION_GENERATION retourne le bon libellé', () => {
    expect(component.jobTypeLabel('QUESTION_GENERATION')).toBe('Génération des questions');
  });

  it('jobTypeLabel — ENRICHED_ANALYSIS retourne le bon libellé', () => {
    expect(component.jobTypeLabel('ENRICHED_ANALYSIS')).toBe('Re-synthèse enrichie');
  });

  // --- Tests SF-36-02 : bouton Analyser le dossier ---

  it('canAnalyze — true si DOCUMENT_ANALYSIS DONE et pas de CASE_ANALYSIS actif', () => {
    component.analysisJobs.set([
      { jobType: 'DOCUMENT_ANALYSIS', status: 'DONE', totalItems: 1, processedItems: 1, progressPercentage: 100 }
    ]);
    expect(component.canAnalyze()).toBe(true);
  });

  it('canAnalyze — false si CASE_ANALYSIS PROCESSING', () => {
    component.analysisJobs.set([
      { jobType: 'DOCUMENT_ANALYSIS', status: 'DONE', totalItems: 1, processedItems: 1, progressPercentage: 100 },
      { jobType: 'CASE_ANALYSIS', status: 'PROCESSING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    expect(component.canAnalyze()).toBe(false);
  });

  it('canAnalyze — false si aucun DOCUMENT_ANALYSIS DONE', () => {
    component.analysisJobs.set([
      { jobType: 'DOCUMENT_ANALYSIS', status: 'PROCESSING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    expect(component.canAnalyze()).toBe(false);
  });

  it('caseAnalysisRunning — true si CASE_ANALYSIS PENDING', () => {
    component.analysisJobs.set([
      { jobType: 'CASE_ANALYSIS', status: 'PENDING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    expect(component.caseAnalysisRunning()).toBe(true);
  });

  it('triggerAnalysis — succès → service appelé + loadAnalysisJobs', () => {
    caseAnalysisCommandServiceSpy.triggerAnalysis.mockReturnValue(of(undefined));
    component.triggerAnalysis();
    expect(caseAnalysisCommandServiceSpy.triggerAnalysis).toHaveBeenCalledWith('cf1');
    expect(analysisJobServiceSpy.getJobs).toHaveBeenCalled();
  });

  it('triggerAnalysis — succès → trackEvent analysis_launched STANDARD', () => {
    caseAnalysisCommandServiceSpy.triggerAnalysis.mockReturnValue(of(undefined));
    component.triggerAnalysis();
    expect(analyticsServiceSpy.trackEvent).toHaveBeenCalledWith('analysis_launched', { type: 'STANDARD' });
  });

  it('triggerAnalysis — 402 ne déclenche plus de snackbar local (SF-171-02 : géré par paymentRequiredInterceptor + QuotaErrorState)', () => {
    caseAnalysisCommandServiceSpy.triggerAnalysis.mockReturnValue(throwError(() => ({ status: 402 })));
    snackBarSpy.open.mockClear();
    component.triggerAnalysis();
    const callsWithLimit = snackBarSpy.open.mock.calls.filter(
      (call: any[]) => typeof call[0] === 'string' && (call[0].includes('Limite') || call[0].includes('plan'))
    );
    expect(callsWithLimit.length).toBe(0);
  });

  it('triggerAnalysis — 409 → snackbar "déjà en cours"', () => {
    caseAnalysisCommandServiceSpy.triggerAnalysis.mockReturnValue(throwError(() => ({ status: 409 })));
    component.triggerAnalysis();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      expect.stringContaining('cours'), expect.any(String), expect.any(Object)
    );
  });

  // --- Tests SF-38-02 : suppression de documents ---

  it('bouton delete présent dans le DOM quand documents.length > 0', () => {
    fixture.detectChanges();
    const deleteButtons = fixture.nativeElement.querySelectorAll('button[title="Supprimer"]');
    expect(deleteButtons.length).toBe(1);
  });

  it('deletedSinceLastAnalysis — false si lastDocumentDeletedAt est null', () => {
    component.caseFile.set({ ...mockCaseFile, lastDocumentDeletedAt: null });
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [], questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    expect(component.deletedSinceLastAnalysis()).toBe(false);
  });

  it('deletedSinceLastAnalysis — true si lastDocumentDeletedAt > synthesis.updatedAt', () => {
    component.caseFile.set({ ...mockCaseFile, lastDocumentDeletedAt: '2026-03-21T10:00:00Z' });
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [], questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    expect(component.deletedSinceLastAnalysis()).toBe(true);
  });

  it('message adaptatif — additions + suppressions → message combiné', () => {
    component.caseFile.set({ ...mockCaseFile, lastDocumentDeletedAt: '2026-03-21T10:00:00Z' });
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [], questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    // doc added after synthesis
    component.documents.set([{ ...mockDocument, createdAt: '2026-03-22T10:00:00Z' }]);
    fixture.detectChanges();

    const warning = fixture.nativeElement.querySelector('.synthesis-outdated span');
    expect(warning.textContent).toContain('ajoutés et/ou supprimés');
  });

  it('canDeleteDocument — false si fullAnalysisRunning', () => {
    component.analysisJobs.set([
      { jobType: 'DOCUMENT_ANALYSIS', status: 'DONE', totalItems: 1, processedItems: 1, progressPercentage: 100 },
      { jobType: 'CASE_ANALYSIS', status: 'PROCESSING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    expect(component.canDeleteDocument()).toBe(false);
  });

  // --- Tests SF-53-02 : gestion statut dossier ---

  it('statusLabel CLOSED → "Clôturé"', () => {
    expect(component.statusLabel('CLOSED')).toBe('Clôturé');
  });

  it('statusClass OPEN → badge--success', () => {
    expect(component.statusClass('OPEN')).toBe('badge--success');
  });

  it('statusClass CLOSED → badge--neutral', () => {
    expect(component.statusClass('CLOSED')).toBe('badge--neutral');
  });

  it('canReopen — true si rôle OWNER', () => {
    component.currentMemberRole.set('OWNER');
    expect(component.canReopen()).toBe(true);
  });

  it('canReopen — true si rôle ADMIN', () => {
    component.currentMemberRole.set('ADMIN');
    expect(component.canReopen()).toBe(true);
  });

  it('canReopen — false si rôle LAWYER', () => {
    component.currentMemberRole.set('LAWYER');
    expect(component.canReopen()).toBe(false);
  });

  it('canDelete — true si rôle OWNER', () => {
    component.currentMemberRole.set('OWNER');
    expect(component.canDelete()).toBe(true);
  });

  it('canDelete — false si rôle ADMIN', () => {
    component.currentMemberRole.set('ADMIN');
    expect(component.canDelete()).toBe(false);
  });

  it('canDelete — false si rôle LAWYER', () => {
    component.currentMemberRole.set('LAWYER');
    expect(component.canDelete()).toBe(false);
  });

  // --- Contrats data-tour-target (C-02, C-03, C-04) ---

  // C-02 : bouton "Ajouter des documents" a data-tour-target="upload-trigger-btn"
  it('C-02: bouton "Ajouter des documents" a data-tour-target="upload-trigger-btn"', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const btn = el.querySelector('[data-tour-target="upload-trigger-btn"]');
    expect(btn).not.toBeNull(); // data-tour-target="upload-trigger-btn" manquant dans le template
  });

  // C-03 : bouton "Analyser le dossier" a data-tour-target="analyze-btn" quand canAnalyze() est vrai
  it('C-03: bouton "Analyser le dossier" a data-tour-target="analyze-btn" quand canAnalyze() est vrai', () => {
    component.analysisJobs.set([
      { jobType: 'DOCUMENT_ANALYSIS', status: 'DONE', totalItems: 1, processedItems: 1, progressPercentage: 100 }
    ]);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const btn = el.querySelector('[data-tour-target="analyze-btn"]');
    expect(btn).not.toBeNull(); // data-tour-target="analyze-btn" manquant dans le template
  });

  // C-04 : lien "Voir la synthèse" a data-tour-target="synthesis-link" quand synthesis() est non null
  it('C-04: lien "Voir la synthèse" a data-tour-target="synthesis-link" quand synthesis() est non null', () => {
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [{ texte: 'un fait', source: null, extrait: null }], pointsJuridiques: [], risques: [], questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-29T10:00:00Z'
    });
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const link = el.querySelector('[data-tour-target="synthesis-link"]');
    expect(link).not.toBeNull(); // data-tour-target="synthesis-link" manquant dans le template
  });

  // --- Tests SF-87-01 : export ZIP ---

  it('exportZip — appelle caseFileService.exportZip avec l\'id du dossier', () => {
    const blob = new Blob(['PK'], { type: 'application/zip' });
    caseFileServiceSpy.exportZip.mockReturnValue(of(blob));

    spyOn(URL, 'createObjectURL').mockReturnValue('blob:test-url');
    spyOn(URL, 'revokeObjectURL');

    const fakeAnchor = document.createElement('a');
    spyOn(document, 'createElement').mockReturnValue(fakeAnchor);
    spyOn(fakeAnchor, 'click');

    component.exportZip();

    expect(caseFileServiceSpy.exportZip).toHaveBeenCalledWith('cf1');
    expect(fakeAnchor.download).toBe('dossier.zip');
    expect(fakeAnchor.click).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test-url');
  });

  it('exportZip — erreur API → snackbar d\'erreur affiché', () => {
    caseFileServiceSpy.exportZip.mockReturnValue(throwError(() => ({ status: 500 })));

    component.exportZip();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      expect.stringContaining("Erreur lors de l'export"),
      expect.any(String),
      expect.any(Object)
    );
  });

  it('bouton Exporter présent dans le DOM', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const buttons = el.querySelectorAll('button');
    const exportBtn = Array.from(buttons).find(b => b.textContent?.includes('Exporter'));
    expect(exportBtn).not.toBeUndefined(); // Bouton Exporter manquant dans le template
  });

  // --- Tests SF-101-01 : dashboardSteps computed ---

  it('SF101-C-01: étape documents pending si aucun document', () => {
    component.documents.set([]);
    const step = component.dashboardSteps().find(s => s.id === 'documents')!;
    expect(step.status).toBe('pending');
    expect(step.detail).toBeNull();
  });

  it('SF101-C-02: étape documents done avec detail si N documents', () => {
    component.documents.set([mockDocument, { ...mockDocument, id: 'doc2' }]);
    const step = component.dashboardSteps().find(s => s.id === 'documents')!;
    expect(step.status).toBe('done');
    expect(step.detail).toBe('2 documents');
  });

  it('SF101-C-03: étape analyse in_progress si fullAnalysisRunning', () => {
    component.analysisJobs.set([
      { jobType: 'CASE_ANALYSIS', status: 'PROCESSING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    const step = component.dashboardSteps().find(s => s.id === 'analyse')!;
    expect(step.status).toBe('in_progress');
    expect(step.detail).toBe('En cours…');
  });

  it('SF101-C-04: étape analyse done si synthesis non null', () => {
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [],
      questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    const step = component.dashboardSteps().find(s => s.id === 'analyse')!;
    expect(step.status).toBe('done');
  });

  it('SF101-C-05: étape questions pending si synthesis null', () => {
    component.synthesis.set(null);
    const step = component.dashboardSteps().find(s => s.id === 'questions')!;
    expect(step.status).toBe('pending');
    expect(step.detail).toBeNull();
  });

  it('SF101-C-06: étape questions done si toutes répondues', () => {
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [],
      questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    component.questions.set([
      { id: 'q1', orderIndex: 0, questionText: 'Q1?', answerText: 'Réponse' }
    ]);
    const step = component.dashboardSteps().find(s => s.id === 'questions')!;
    expect(step.status).toBe('done');
  });

  it('SF101-C-07: étape questions affiche le nombre en attente', () => {
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [],
      questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    component.questions.set([
      { id: 'q1', orderIndex: 0, questionText: 'Q1?', answerText: null }
    ]);
    const step = component.dashboardSteps().find(s => s.id === 'questions')!;
    expect(step.detail).toBe('1 en attente');
  });

  it('SF101-C-08: étape délais done si synthèse présente et aucun délai IA en attente', () => {
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [],
      questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    component.deadlines.set([
      { id: 'd1', label: 'Appel', dueDate: '2026-05-01', createdAt: '', updatedAt: '', source: 'MANUAL', aiStatus: null }
    ]);
    const step = component.dashboardSteps().find(s => s.id === 'delais')!;
    expect(step.status).toBe('done');
  });

  it('SF101-C-09: étape délais pending avec detail si délais IA en attente', () => {
    component.deadlines.set([
      { id: 'd1', label: 'Délai IA', dueDate: '2026-05-01', createdAt: '', updatedAt: '', source: 'AI', aiStatus: 'PENDING' }
    ]);
    const step = component.dashboardSteps().find(s => s.id === 'delais')!;
    expect(step.status).toBe('pending');
    expect(step.detail).toBe('1 proposition IA en attente');
  });

  it('SF101-C-10: étape pièces done si piecesManquantes vide', () => {
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [],
      questionsOuvertes: [], piecesManquantes: [],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    const step = component.dashboardSteps().find(s => s.id === 'pieces')!;
    expect(step.status).toBe('done');
  });

  it('SF101-C-11: étape pièces affiche le nombre identifié', () => {
    component.synthesis.set({
      id: 's1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [],
      questionsOuvertes: [], piecesManquantes: ['Convocation', 'Avertissement'],
      riskLevel: null, riskScore: null, modelUsed: null, updatedAt: '2026-03-20T10:00:00Z'
    });
    const step = component.dashboardSteps().find(s => s.id === 'pieces')!;
    expect(step.status).toBe('pending');
    expect(step.detail).toBe('2 identifiées');
  });

  it('SF101-C-12: loadDeadlines — erreur API → deadlines() reste vide (fail-open)', () => {
    caseDeadlineServiceSpy.list.mockReturnValue(throwError(() => new Error('500')));
    component.loadDeadlines('cf1');
    expect(component.deadlines()).toEqual([]);
  });

  it('SF101-C-13: stepper rendu dans le DOM avec 5 étapes', () => {
    fixture.detectChanges();
    const stepper = fixture.nativeElement.querySelector('app-case-dashboard-stepper');
    expect(stepper).not.toBeNull();
  });

  it('SF-IA-04-03: panel décisionnel monté une fois le dossier chargé', () => {
    fixture.detectChanges();
    const panel = fixture.nativeElement.querySelector('app-decisional-tools-panel');
    expect(panel).not.toBeNull();
  });

  it('SF-184-01 T-04 (adapté SF-184-02): tableau de bord décisionnel rendu dans col-right (carte premium)', () => {
    fixture.detectChanges();
    const dashboard = fixture.nativeElement.querySelector('app-case-dashboard');
    expect(dashboard).not.toBeNull();
    // Doit être enveloppé dans le wrapper premium SF-184-01.
    const wrapper = dashboard.closest('.decisional-summary-panel');
    expect(wrapper).not.toBeNull();
    // Ce wrapper doit être à l'intérieur de col-right.
    expect(wrapper.closest('.col-right')).not.toBeNull();
    // SF-184-02 : le conteneur .bottom-sections n'existe plus, le wrapper ne peut donc
    // pas y être imbriqué.
    expect(wrapper.closest('.bottom-sections')).toBeNull();
  });

  it('SF-184-02 T-01: Délais + Notes + Outils décisionnels remontés dans col-left, .bottom-sections supprimé', () => {
    fixture.detectChanges();

    // Le conteneur .bottom-sections n'existe plus dans le DOM.
    const bottomSections = fixture.nativeElement.querySelector('.bottom-sections');
    expect(bottomSections).toBeNull();

    // Les 3 composants ex-bottom-sections sont maintenant dans .col-left.
    const colLeft = fixture.nativeElement.querySelector('.col-left');
    expect(colLeft).not.toBeNull();

    const deadlines = colLeft.querySelector('app-case-deadlines-section');
    const notes = colLeft.querySelector('app-case-notes-section');
    const tools = colLeft.querySelector('app-decisional-tools-panel');
    expect(deadlines).not.toBeNull();
    expect(notes).not.toBeNull();
    expect(tools).not.toBeNull();

    // Ordre vertical : DOCUMENTS (section) → Délais → Notes → Outils décisionnels.
    const docsSection = colLeft.querySelector('.td-section--documents');
    expect(docsSection).not.toBeNull();
    const colLeftChildren = Array.from(colLeft.children) as HTMLElement[];
    const idxDocs = colLeftChildren.findIndex(c => c.contains(docsSection));
    const idxDeadlines = colLeftChildren.findIndex(c => c.contains(deadlines));
    const idxNotes = colLeftChildren.findIndex(c => c.contains(notes));
    const idxTools = colLeftChildren.findIndex(c => c.contains(tools));
    expect(idxDocs).toBeGreaterThanOrEqual(0);
    expect(idxDeadlines).toBeGreaterThan(idxDocs);
    expect(idxNotes).toBeGreaterThan(idxDeadlines);
    expect(idxTools).toBeGreaterThan(idxNotes);
  });

  // ----- SF-125-01 : bouton d'analyse contextuel -----

  describe('SF-125-01 bouton contextuel', () => {
    const makeAnalysis = (): any => ({
      id: 'a1', version: 1, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [], questionsOuvertes: [],
      piecesManquantes: [], pointsProcedure: [], compensationEstimate: null,
      belgianCompensationEstimate: null, pensionAlimentaireEstimate: null,
      prestationCompensatoireEstimate: null, liquidationCommunaute: null,
      travailExtractedData: null, immigrationExtractedData: null,
      licenciementValidityDetection: null, ruptureConvValidityDetection: null,
      piecesManquantesDetails: null, analysisDocuments: [],
    });
    const makeQuestion = (answered: boolean): any => ({
      id: 'q1', orderIndex: 0, questionText: 'Q?', answerText: answered ? 'réponse' : null,
    });

    // U-CFD-B01 : aucune analyse → STANDARD
    it('U-CFD-B01 : aucune analyse → label "Analyser le dossier" + clic lance STANDARD', () => {
      component.synthesis.set(null);
      component.questions.set([]);
      expect(component.hasAnyAnalysis()).toBe(false);
      expect(component.canEnrichSynthesis()).toBe(false);
      expect(component.analysisButtonLabel()).toBe('Analyser le dossier');

      component.onAnalysisButtonClick();
      expect(caseAnalysisCommandServiceSpy.triggerAnalysis).toHaveBeenCalled();
      expect(reAnalysisServiceSpy.reAnalyze).not.toHaveBeenCalled();
    });

    // U-CFD-B02 : analyse DONE mais aucune réponse Q&A → STANDARD
    it('U-CFD-B02 : analyse DONE sans réponses Q&A → label reste "Analyser le dossier" + STANDARD', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(false), makeQuestion(false)]);
      expect(component.hasAnyAnalysis()).toBe(true);
      expect(component.canEnrichSynthesis()).toBe(false);
      expect(component.analysisButtonLabel()).toBe('Analyser le dossier');

      component.onAnalysisButtonClick();
      expect(caseAnalysisCommandServiceSpy.triggerAnalysis).toHaveBeenCalled();
      expect(reAnalysisServiceSpy.reAnalyze).not.toHaveBeenCalled();
    });

    // U-CFD-B03 : analyse DONE + réponse Q&A → ENRICHED
    it('U-CFD-B03 : analyse DONE + ≥1 réponse Q&A → label "Enrichir la synthèse" + ENRICHED', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(true), makeQuestion(false)]);
      expect(component.canEnrichSynthesis()).toBe(true);
      expect(component.analysisButtonLabel()).toBe('Enrichir la synthèse');

      component.onAnalysisButtonClick();
      expect(reAnalysisServiceSpy.reAnalyze).toHaveBeenCalled();
      expect(caseAnalysisCommandServiceSpy.triggerAnalysis).not.toHaveBeenCalled();
    });

    // U-CFD-B04 : ENRICHED échoue 409 → snackbar clair mentionnant les 3 sources (Q&A / chat / checklist)
    it('U-CFD-B04 : ENRICHED renvoie 409 → snackbar informant de la règle Q&A / chat / checklist', () => {
      reAnalysisServiceSpy.reAnalyze.mockReturnValue(throwError(() => ({ status: 409 })));
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(true)]);

      component.onAnalysisButtonClick();
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        expect.stringContaining('checklist procédurale'),
        'Fermer',
        expect.objectContaining({ panelClass: ['snack-error'] })
      );
    });

    // U-CFD-B05 : handleFullReanalysisResult dispatche selon le résultat
    it('U-CFD-B05 : handleFullReanalysisResult(FULL) → STANDARD appelé, pas ENRICHED', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(true)]);

      component.handleFullReanalysisResult('FULL');
      expect(caseAnalysisCommandServiceSpy.triggerAnalysis).toHaveBeenCalled();
      expect(reAnalysisServiceSpy.reAnalyze).not.toHaveBeenCalled();
    });

    it('U-CFD-B05 bis : handleFullReanalysisResult(ENRICH) → ENRICHED appelé', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(true)]);

      component.handleFullReanalysisResult('ENRICH');
      expect(reAnalysisServiceSpy.reAnalyze).toHaveBeenCalled();
      expect(caseAnalysisCommandServiceSpy.triggerAnalysis).not.toHaveBeenCalled();
    });

    it('U-CFD-B05 ter : handleFullReanalysisResult(CANCEL) → aucun appel', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(true)]);

      component.handleFullReanalysisResult('CANCEL');
      expect(caseAnalysisCommandServiceSpy.triggerAnalysis).not.toHaveBeenCalled();
      expect(reAnalysisServiceSpy.reAnalyze).not.toHaveBeenCalled();
    });

    it('U-CFD-B05 quater : handleFullReanalysisResult(undefined) → aucun appel', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(true)]);

      component.handleFullReanalysisResult(undefined);
      expect(caseAnalysisCommandServiceSpy.triggerAnalysis).not.toHaveBeenCalled();
      expect(reAnalysisServiceSpy.reAnalyze).not.toHaveBeenCalled();
    });

    // U-CFD-B07 : fix checklist — canEnrichSynthesis=true si au moins 1 check VERIFIED/NON_COMPLIANT
    it('U-CFD-B07 : analyse DONE + check VERIFIED (sans réponse Q&A) → label "Enrichir la synthèse"', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([]); // pas de Q&A du tout
      component.procedureChecks.set([
        { id: 'c1', ordre: 0, description: 'Point 1', statut: 'VERIFIED' } as any,
      ]);

      expect(component.canEnrichSynthesis()).toBe(true);
      expect(component.analysisButtonLabel()).toBe('Enrichir la synthèse');

      component.onAnalysisButtonClick();
      expect(reAnalysisServiceSpy.reAnalyze).toHaveBeenCalled();
    });

    it('U-CFD-B07 bis : analyse DONE + checks tous TO_CHECK → bouton reste STANDARD', () => {
      component.synthesis.set(makeAnalysis());
      component.questions.set([]);
      component.procedureChecks.set([
        { id: 'c1', ordre: 0, description: 'P1', statut: 'TO_CHECK' } as any,
        { id: 'c2', ordre: 1, description: 'P2', statut: 'TO_CHECK' } as any,
      ]);

      expect(component.canEnrichSynthesis()).toBe(false);
      expect(component.analysisButtonLabel()).toBe('Analyser le dossier');
    });

    // U-CFD-B06 : reAnalyzing signal désactive les boutons
    it('U-CFD-B06 : reAnalyzing signal est true pendant l\'appel ENRICHED, false après succès', () => {
      const delayed = new Subject<void>();
      reAnalysisServiceSpy.reAnalyze.mockReturnValue(delayed.asObservable());
      component.synthesis.set(makeAnalysis());
      component.questions.set([makeQuestion(true)]);

      component.onAnalysisButtonClick();
      expect(component.reAnalyzing()).toBe(true);

      delayed.next();
      delayed.complete();
      expect(component.reAnalyzing()).toBe(false);
    });
  });

  // ----- F-124 : refresh auto du dashboard après ré-analyse -----

  describe('F-124 dashboard refresh après ré-analyse', () => {
    const makeResult = (version: number): any => ({
      id: 'a' + version, version, analysisType: 'STANDARD', status: 'DONE',
      timeline: [], faits: [], pointsJuridiques: [], risques: [], questionsOuvertes: [],
      piecesManquantes: [], pointsProcedure: [],
      compensationEstimate: null, belgianCompensationEstimate: null,
      pensionAlimentaireEstimate: null, prestationCompensatoireEstimate: null,
      liquidationCommunaute: null, travailExtractedData: null,
      immigrationExtractedData: null, licenciementValidityDetection: null,
      ruptureConvValidityDetection: null, piecesManquantesDetails: null,
      analysisDocuments: [],
    });

    // U-CFD-124-01 : premier chargement → pas de refresh (dashboard charge seul via ngOnInit)
    it('U-CFD-124-01 : premier chargement de synthèse → pas de triggerRefresh', () => {
      const spy = jest.spyOn(component['dashboardRefreshService'], 'triggerRefresh');
      caseAnalysisServiceSpy.getAnalysis.mockReturnValue(of(makeResult(1)));

      component.loadSynthesis('cf1');

      expect(spy).not.toHaveBeenCalled();
    });

    // U-CFD-124-02 : nouvelle version après polling → triggerRefresh
    it('U-CFD-124-02 : version change de 1 à 2 → triggerRefresh appelé', () => {
      const spy = jest.spyOn(component['dashboardRefreshService'], 'triggerRefresh');

      // Premier load : version 1, pas de refresh
      caseAnalysisServiceSpy.getAnalysis.mockReturnValueOnce(of(makeResult(1)));
      component.loadSynthesis('cf1');
      expect(spy).not.toHaveBeenCalled();

      // Deuxième load (ré-analyse terminée) : version 2 → refresh
      caseAnalysisServiceSpy.getAnalysis.mockReturnValueOnce(of(makeResult(2)));
      component.loadSynthesis('cf1');
      expect(spy).toHaveBeenCalledTimes(1);
    });

    // U-CFD-124-03 : même version rechargée (polling tick) → pas de refresh répété
    it('U-CFD-124-03 : même version rechargée 3 fois → triggerRefresh appelé 0 fois', () => {
      const spy = jest.spyOn(component['dashboardRefreshService'], 'triggerRefresh');

      caseAnalysisServiceSpy.getAnalysis.mockReturnValueOnce(of(makeResult(5)));
      component.loadSynthesis('cf1');
      caseAnalysisServiceSpy.getAnalysis.mockReturnValueOnce(of(makeResult(5)));
      component.loadSynthesis('cf1');
      caseAnalysisServiceSpy.getAnalysis.mockReturnValueOnce(of(makeResult(5)));
      component.loadSynthesis('cf1');

      expect(spy).not.toHaveBeenCalled();
    });

    // U-CFD-124-04 : résultat null (pas d'analyse existante) → pas de refresh
    it('U-CFD-124-04 : getAnalysis retourne null → pas de triggerRefresh', () => {
      const spy = jest.spyOn(component['dashboardRefreshService'], 'triggerRefresh');
      caseAnalysisServiceSpy.getAnalysis.mockReturnValue(of(null));

      component.loadSynthesis('cf1');

      expect(spy).not.toHaveBeenCalled();
    });

  });

  // SF-121-04 : step 2 FAILED dès qu'au moins 1 doc est FAILED (règle "any failed")
  describe('SF-121-04 propage tout échec d\'extraction sur step 2', () => {
    const failedDoc = (id: string): Document => ({
      ...mockDocument, id, extractionStatus: 'FAILED', failureReason: 'EMPTY_TEXT',
    });
    const doneDoc = (id: string): Document => ({
      ...mockDocument, id, extractionStatus: 'DONE',
    });
    const pendingDoc = (id: string): Document => ({
      ...mockDocument, id, extractionStatus: 'PENDING',
    });

    beforeEach(() => {
      component['docAnalysisPending'].set(true);
    });

    // U-CFD-121-04-01 : tous FAILED → virtual FAILED "9 non analysables / 9"
    it('U-CFD-121-04-01 : 9/9 FAILED → virtual FAILED 9/9', () => {
      component.documents.set(Array.from({ length: 9 }, (_, i) => failedDoc('d' + i)));

      (component as any).applyExtractionFailedOverride();

      const job = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');
      expect(job).toBeDefined();
      expect(job!.status).toBe('FAILED');
      expect(job!.totalItems).toBe(9);
      expect(job!.processedItems).toBe(9);
      expect(component['docAnalysisPending']()).toBe(false);
    });

    // U-CFD-121-04-02 : 3 FAILED + 6 DONE sans job backend → virtual FAILED "3 / 9"
    it('U-CFD-121-04-02 : 3 FAILED + 6 DONE → virtual FAILED 3/9', () => {
      const docs = [
        ...Array.from({ length: 3 }, (_, i) => failedDoc('f' + i)),
        ...Array.from({ length: 6 }, (_, i) => doneDoc('d' + i)),
      ];
      component.documents.set(docs);

      (component as any).applyExtractionFailedOverride();

      const job = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');
      expect(job!.status).toBe('FAILED');
      expect(job!.processedItems).toBe(3);
      expect(job!.totalItems).toBe(9);
    });

    // U-CFD-121-04-03 : backend job DONE + 3 docs FAILED → override en FAILED 3/9
    it('U-CFD-121-04-03 : backend DOCUMENT_ANALYSIS DONE + 3 docs FAILED → override en FAILED', () => {
      const docs = [
        ...Array.from({ length: 3 }, (_, i) => failedDoc('f' + i)),
        ...Array.from({ length: 6 }, (_, i) => doneDoc('d' + i)),
      ];
      component.documents.set(docs);
      component.analysisJobs.set([
        { jobType: 'DOCUMENT_ANALYSIS', status: 'DONE', totalItems: 6, processedItems: 6, progressPercentage: 100 },
      ]);

      (component as any).applyExtractionFailedOverride();

      const job = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');
      expect(job!.status).toBe('FAILED');
      expect(job!.processedItems).toBe(3);
      expect(job!.totalItems).toBe(9);
    });

    // U-CFD-121-04-04 : 0 FAILED → pas d'override, backend l'emporte
    it('U-CFD-121-04-04 : 9/9 DONE → pas d\'override', () => {
      component.documents.set(Array.from({ length: 9 }, (_, i) => doneDoc('d' + i)));

      (component as any).applyExtractionFailedOverride();

      const job = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');
      expect(job).toBeUndefined();
      expect(component['docAnalysisPending']()).toBe(true); // inchangé
    });

    // U-CFD-121-04-05 : extraction pas stable (PENDING) → attendre
    it('U-CFD-121-04-05 : 1 PENDING + FAILED → pas d\'override (unstable)', () => {
      component.documents.set([pendingDoc('p0'), failedDoc('f1')]);

      (component as any).applyExtractionFailedOverride();

      const job = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');
      expect(job).toBeUndefined();
    });

    // U-CFD-121-04-06 : 0 docs → safeguard
    it('U-CFD-121-04-06 : aucun document → pas d\'override (safeguard)', () => {
      component.documents.set([]);

      (component as any).applyExtractionFailedOverride();

      const job = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');
      expect(job).toBeUndefined();
    });

    // U-CFD-121-04-07 : idempotence — ne repose pas le virtuel si déjà en place
    it('U-CFD-121-04-07 : appels multiples → un seul virtuel posé (idempotent)', () => {
      const docs = [failedDoc('f0'), failedDoc('f1'), doneDoc('d0')];
      component.documents.set(docs);

      (component as any).applyExtractionFailedOverride();
      const firstJob = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');

      (component as any).applyExtractionFailedOverride();
      (component as any).applyExtractionFailedOverride();
      const secondJob = component.analysisJobs().find(j => j.jobType === 'DOCUMENT_ANALYSIS');

      expect(firstJob).toEqual(secondJob);
      expect(component.analysisJobs().filter(j => j.jobType === 'DOCUMENT_ANALYSIS')).toHaveLength(1);
    });
  });

  // SF-145-02 : chips pièces dans la table des docs
  describe('SF-145-02 — chips pièces', () => {
    const pieces = [
      { id: 'p1', type: 'CONTRAT' as const, label: 'CDI', pageStart: 1, pageEnd: 2, orderIndex: 0 },
      { id: 'p2', type: 'PIECE_IDENTITE' as const, label: 'CNI', pageStart: 3, pageEnd: 3, orderIndex: 1 },
    ];

    it('U-01 : 2 pièces → 2 chips rendus sous le nom du doc', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([{ ...mockDocument, extractionStatus: 'DONE', pieces }]);
      fixture.detectChanges();
      const chips = fixture.nativeElement.querySelectorAll('.piece-chip');
      expect(chips.length).toBe(2);
    });

    it('U-02 : 1 seule pièce → 1 chip affiché (hotfix 2026-04-23 : cohérence avec ≥2 pièces)', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([{ ...mockDocument, extractionStatus: 'DONE', pieces: [pieces[0]] }]);
      fixture.detectChanges();
      const chips = fixture.nativeElement.querySelectorAll('.piece-chip');
      expect(chips.length).toBe(1);
    });

    it('U-03 : pas de pieces (legacy) → pas de chips', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([{ ...mockDocument, extractionStatus: 'DONE' }]);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.piece-chip')).toBeFalsy();
    });
  });

  // SF-144-01 : feedback UX OCR (ocrRunning + ocrExtracted)
  describe('SF-144-01 — feedback OCR', () => {
    const baseDoc = (overrides: Partial<Document> = {}): Document => ({
      ...mockDocument, id: 'd1', ...overrides,
    });

    // U-03 : documentsContentEqual détecte un changement de ocrRunning → refresh
    it('U-03 : change ocrRunning → documentsContentEqual retourne false', () => {
      const before = [baseDoc({ ocrRunning: false })];
      const after = [baseDoc({ ocrRunning: true })];
      expect((component as any).documentsContentEqual(before, after)).toBe(false);
    });

    // U-04 : documentsContentEqual détecte un changement de ocrExtracted → refresh
    it('U-04 : change ocrExtracted → documentsContentEqual retourne false', () => {
      const before = [baseDoc({ ocrExtracted: false })];
      const after = [baseDoc({ ocrExtracted: true })];
      expect((component as any).documentsContentEqual(before, after)).toBe(false);
    });

    // U-05 : badge "OCR en cours" rendu quand ocrRunning === true
    it('U-05 : badge .badge-ocr-running rendu quand ocrRunning', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([baseDoc({ extractionStatus: 'PROCESSING', ocrRunning: true })]);
      fixture.detectChanges();
      const badge = fixture.nativeElement.querySelector('.badge-ocr-running');
      expect(badge).toBeTruthy();
      expect(badge.textContent).toContain('OCR en cours');
    });

    // U-06 : chip .chip-ocr rendu quand ocrExtracted === true (sans ocrRunning)
    it('U-06 : chip .chip-ocr rendu quand ocrExtracted et pas ocrRunning', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([baseDoc({ extractionStatus: 'DONE', ocrExtracted: true, ocrRunning: false })]);
      fixture.detectChanges();
      const chip = fixture.nativeElement.querySelector('.chip-ocr');
      expect(chip).toBeTruthy();
      expect(chip.textContent.trim()).toBe('OCR');
    });

    // U-07 : aucun chip OCR si extraction normale (ocrExtracted=false)
    it('U-07 : pas de chip OCR si ocrExtracted false', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([baseDoc({ extractionStatus: 'DONE', ocrExtracted: false })]);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.chip-ocr')).toBeFalsy();
      expect(fixture.nativeElement.querySelector('.badge-ocr-running')).toBeFalsy();
    });
  });

  // SF-148-04 : indicateurs vision globaux (badge par doc + bandeau + polling)
  describe('SF-148-04 — indicateurs vision', () => {
    const baseDoc = (overrides: Partial<Document> = {}): Document => ({
      ...mockDocument, id: 'd1', extractionStatus: 'DONE', ...overrides,
    });

    it('U-01 : change visionStatus d\'une pièce → documentsContentEqual retourne false', () => {
      const before = [baseDoc({ pieces: [{ id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'PENDING' }] })];
      const after = [baseDoc({ pieces: [{ id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'DONE' }] })];
      expect((component as any).documentsContentEqual(before, after)).toBe(false);
    });

    it('U-02 : ajout d\'une pièce (async pipeline) → documentsContentEqual retourne false', () => {
      const before = [baseDoc({ pieces: [] })];
      const after = [baseDoc({ pieces: [{ id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'NOT_APPLICABLE' }] })];
      expect((component as any).documentsContentEqual(before, after)).toBe(false);
    });

    it('U-03 : pièces identiques → documentsContentEqual retourne true (pas de re-render inutile)', () => {
      const piece = { id: 'p1', type: 'CONTRAT' as const, label: 'CDI', pageStart: 1, pageEnd: 2, orderIndex: 0, visionStatus: 'DONE' as const, visualDescription: 'desc' };
      const before = [baseDoc({ pieces: [piece] })];
      const after = [baseDoc({ pieces: [{ ...piece }] })];
      expect((component as any).documentsContentEqual(before, after)).toBe(true);
    });

    it('U-04 : visionPendingCount compte toutes les pièces PENDING du dossier', () => {
      component.documents.set([
        baseDoc({ id: 'd1', pieces: [
          { id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'PENDING' },
          { id: 'p2', type: 'ATTESTATION', label: null, pageStart: 2, pageEnd: 2, orderIndex: 1, visionStatus: 'DONE' },
        ]}),
        baseDoc({ id: 'd2', pieces: [
          { id: 'p3', type: 'PASSEPORT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'PENDING' },
        ]}),
      ]);
      expect(component.visionPendingCount()).toBe(2);
      expect(component.visionDoneCount()).toBe(1);
    });

    it('U-05 : documentVisionState retourne PENDING dès qu\'une pièce est en cours', () => {
      const doc = baseDoc({ pieces: [
        { id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'DONE' },
        { id: 'p2', type: 'ATTESTATION', label: null, pageStart: 2, pageEnd: 2, orderIndex: 1, visionStatus: 'PENDING' },
      ]});
      expect(component.documentVisionState(doc)).toBe('PENDING');
    });

    it('U-06 : documentVisionState DONE si ≥1 DONE et aucune PENDING', () => {
      const doc = baseDoc({ pieces: [
        { id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'DONE' },
        { id: 'p2', type: 'ATTESTATION', label: null, pageStart: 2, pageEnd: 2, orderIndex: 1, visionStatus: 'NOT_APPLICABLE' },
      ]});
      expect(component.documentVisionState(doc)).toBe('DONE');
    });

    it('U-07 : documentVisionState NONE si rien d\'éligible à vision', () => {
      const doc = baseDoc({ pieces: [
        { id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'NOT_APPLICABLE' },
      ]});
      expect(component.documentVisionState(doc)).toBe('NONE');
      expect(component.documentVisionState(baseDoc({ pieces: [] }))).toBe('NONE');
      expect(component.documentVisionState(baseDoc())).toBe('NONE');
    });

    it('U-08 : bandeau global rendu quand visionPendingCount > 0', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([
        baseDoc({ pieces: [{ id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'PENDING' }] }),
      ]);
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('.vision-banner');
      expect(banner).toBeTruthy();
      expect(banner.textContent).toContain('Analyse visuelle en cours');
    });

    it('U-09 : pas de bandeau si aucune pièce PENDING', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([baseDoc()]);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.vision-banner')).toBeFalsy();
    });

    it('U-10 : badge --pending rendu sur le doc avec pièce PENDING', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([
        baseDoc({ pieces: [{ id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'PENDING' }] }),
      ]);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.badge-vision--pending')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.badge-vision--done')).toBeFalsy();
    });

    it('U-11 : badge --done rendu sur le doc avec pièce DONE (aucune PENDING)', () => {
      component.caseFile.set(mockCaseFile);
      component.documents.set([
        baseDoc({ pieces: [{ id: 'p1', type: 'CONTRAT', label: null, pageStart: 1, pageEnd: 1, orderIndex: 0, visionStatus: 'DONE' }] }),
      ]);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.badge-vision--done')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.badge-vision--pending')).toBeFalsy();
    });
  });

  // ── SF-170-02 — Section Documents en accordéon (sans persistance, auto-expand) ─────

  describe('SF-170-02 docsCollapsed accordion (no persistence)', () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('SF-170-02 T-01: docsCollapsed est false par défaut au ngOnInit', () => {
      expect(component.docsCollapsed()).toBe(false);
    });

    it('SF-170-02 T-03: toggleDocsCollapsed() bascule la valeur SANS écrire en sessionStorage', () => {
      const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');
      expect(component.docsCollapsed()).toBe(false);

      component.toggleDocsCollapsed();
      expect(component.docsCollapsed()).toBe(true);

      component.toggleDocsCollapsed();
      expect(component.docsCollapsed()).toBe(false);

      const docsKeyCalls = setItemSpy.mock.calls.filter(([k]) =>
        typeof k === 'string' && k.includes('docs-collapsed')
      );
      expect(docsKeyCalls.length).toBe(0);
    });

    it('SF-170-02 T-05: onFileSelected déplie la section si elle était repliée', () => {
      component.docsCollapsed.set(true);
      expect(component.docsCollapsed()).toBe(true);

      const fakeFile = new File(['x'], 'a.pdf', { type: 'application/pdf' });
      const fakeEvent = { target: { files: [fakeFile], value: '' } } as unknown as Event;
      component.onFileSelected(fakeEvent);

      expect(component.pendingFiles().length).toBe(1);
      expect(component.docsCollapsed()).toBe(false);
    });

    it('SF-170-02 T-06: onFileSelected ne perturbe pas l\'état si déjà déplié', () => {
      expect(component.docsCollapsed()).toBe(false);

      const fakeFile = new File(['x'], 'a.pdf', { type: 'application/pdf' });
      const fakeEvent = { target: { files: [fakeFile], value: '' } } as unknown as Event;
      component.onFileSelected(fakeEvent);

      expect(component.docsCollapsed()).toBe(false);
    });

    it('SF-170-02 T-07: removePendingFile et upload-clear ne re-collapse jamais', () => {
      component.docsCollapsed.set(true);
      const fakeFile = new File(['x'], 'a.pdf', { type: 'application/pdf' });
      const fakeEvent = { target: { files: [fakeFile], value: '' } } as unknown as Event;
      component.onFileSelected(fakeEvent);
      expect(component.docsCollapsed()).toBe(false);

      component.removePendingFile(fakeFile);
      expect(component.docsCollapsed()).toBe(false);

      component.pendingFiles.set([]);
      expect(component.docsCollapsed()).toBe(false);
    });

    it('SF-170-02 T-08: onFileSelected ne déplie pas si tous les fichiers sont rejetés (oversized)', () => {
      component.docsCollapsed.set(true);
      const oversized = new File([new ArrayBuffer(60 * 1024 * 1024)], 'big.pdf', { type: 'application/pdf' });
      const fakeEvent = { target: { files: [oversized], value: '' } } as unknown as Event;
      component.onFileSelected(fakeEvent);

      expect(component.pendingFiles().length).toBe(0);
      // Aucun fichier valide ajouté → pas de raison d'auto-déplier.
      expect(component.docsCollapsed()).toBe(true);
    });
  });

  describe('SF-171-02 — bandeau quota persistant + état disabled-quota', () => {
    it('analysisQuotaBlocked = true quand QuotaErrorState a TOKEN_BUDGET_EXCEEDED', () => {
      const stateService = (component as any).quotaErrorState;
      stateService.set({ code: 'TOKEN_BUDGET_EXCEEDED', message: 'Budget tokens dépassé', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
      expect(component.analysisQuotaBlocked()).toBe(true);
    });

    it('analysisQuotaBlocked = true quand QuotaErrorState a CASE_ANALYSIS_LIMIT_EXCEEDED', () => {
      const stateService = (component as any).quotaErrorState;
      stateService.set({ code: 'CASE_ANALYSIS_LIMIT_EXCEEDED', message: 'msg', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
      expect(component.analysisQuotaBlocked()).toBe(true);
    });

    it('analysisQuotaBlocked = false sur autre code (ex. SEAT_LIMIT)', () => {
      const stateService = (component as any).quotaErrorState;
      stateService.set({ code: 'SEAT_LIMIT_EXCEEDED', message: 'msg', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
      expect(component.analysisQuotaBlocked()).toBe(false);
    });

    it('analysisQuotaBlocked = false quand QuotaErrorState est null', () => {
      const stateService = (component as any).quotaErrorState;
      stateService.clear();
      expect(component.analysisQuotaBlocked()).toBe(false);
    });

    it('triggerAnalysis — 402 ne declenche plus de snackbar local (gere par interceptor)', () => {
      caseAnalysisCommandServiceSpy.triggerAnalysis.mockReturnValue(throwError(() => ({ status: 402, error: { code: 'TOKEN_BUDGET_EXCEEDED', message: 'Budget tokens depasse' } })));
      caseFileServiceSpy.getById.mockReturnValue(of(mockCaseFile));
      component.caseFile.set(mockCaseFile);
      snackBarSpy.open.mockClear();

      component.triggerAnalysis();

      const callsWithLimit = snackBarSpy.open.mock.calls.filter(
        (call: any[]) => typeof call[0] === 'string' && (call[0].includes('Limite') || call[0].includes('plan'))
      );
      expect(callsWithLimit.length).toBe(0);
      expect(component.analyzing()).toBe(false);
    });

    it('reopenCaseFile — 402 ne declenche plus de snackbar local (gere par interceptor)', () => {
      caseFileStatusServiceSpy.reopen.mockReturnValue(throwError(() => ({ status: 402, error: { code: 'CASE_FILE_OPEN_LIMIT_EXCEEDED', message: 'Limite atteinte' } })));
      component.caseFile.set(mockCaseFile);
      snackBarSpy.open.mockClear();

      component.reopenCaseFile();

      const callsWithLimit = snackBarSpy.open.mock.calls.filter(
        (call: any[]) => typeof call[0] === 'string' && call[0].includes('Limite')
      );
      expect(callsWithLimit.length).toBe(0);
    });

    it('le bandeau app-quota-error-banner est present dans le DOM', () => {
      const banner = fixture.nativeElement.querySelector('app-quota-error-banner');
      expect(banner).not.toBeNull();
    });
  });

  // SF-186-01 — refetch défensif au retour de visibilité (pallier un éventuel
  // SSE perdu après navigation detail ↔ synthesis ↔ detail).
  describe('SF-186-01 — visibilitychange refetch', () => {
    function setVisibilityState(state: 'visible' | 'hidden'): void {
      Object.defineProperty(document, 'visibilityState', {
        configurable: true,
        get: () => state,
      });
    }

    it('document visible → loadAnalysisJobs + loadDocuments appelés', () => {
      analysisJobServiceSpy.getJobs.mockClear();
      documentServiceSpy.list.mockClear();

      setVisibilityState('visible');
      document.dispatchEvent(new Event('visibilitychange'));

      expect(analysisJobServiceSpy.getJobs).toHaveBeenCalledWith('cf1');
      expect(documentServiceSpy.list).toHaveBeenCalledWith('cf1');
    });

    it('document hidden → pas de refetch', () => {
      analysisJobServiceSpy.getJobs.mockClear();
      documentServiceSpy.list.mockClear();

      setVisibilityState('hidden');
      document.dispatchEvent(new Event('visibilitychange'));

      expect(analysisJobServiceSpy.getJobs).not.toHaveBeenCalled();
      expect(documentServiceSpy.list).not.toHaveBeenCalled();
    });

    it('ngOnDestroy → listener visibilitychange retiré', () => {
      component.ngOnDestroy();

      analysisJobServiceSpy.getJobs.mockClear();
      setVisibilityState('visible');
      document.dispatchEvent(new Event('visibilitychange'));

      expect(analysisJobServiceSpy.getJobs).not.toHaveBeenCalled();
    });
  });

  describe('SF-159-04 — instrumentation transition vers FAILED', () => {
    let warnSpy: jest.SpyInstance;

    beforeEach(() => {
      warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
      warnSpy.mockRestore();
    });

    it('U1: log une fois la transition PROCESSING → FAILED', () => {
      analysisJobServiceSpy.getJobs.mockReturnValueOnce(of([
        { jobType: 'CASE_ANALYSIS', status: 'PROCESSING', totalItems: 1, processedItems: 0, progressPercentage: 50 } as any
      ]));
      component.loadAnalysisJobs('cf1');
      expect(warnSpy).not.toHaveBeenCalled();

      analysisJobServiceSpy.getJobs.mockReturnValueOnce(of([
        { jobType: 'CASE_ANALYSIS', status: 'FAILED', totalItems: 1, processedItems: 0, progressPercentage: 100 } as any
      ]));
      component.loadAnalysisJobs('cf1');

      expect(warnSpy).toHaveBeenCalledTimes(1);
      const args = warnSpy.mock.calls[0];
      expect(args[0]).toContain('SF-159-04');
      expect(args[1]).toMatchObject({
        caseFileId: 'cf1',
        jobType: 'CASE_ANALYSIS',
        previousStatus: 'PROCESSING',
        source: 'loadAnalysisJobs',
      });
    });

    it('U2: réception successive de FAILED ne re-log pas', () => {
      const failedJobs = [
        { jobType: 'CASE_ANALYSIS', status: 'FAILED', totalItems: 1, processedItems: 0, progressPercentage: 100 } as any
      ];
      analysisJobServiceSpy.getJobs.mockReturnValueOnce(of(failedJobs));
      component.loadAnalysisJobs('cf1');
      expect(warnSpy).toHaveBeenCalledTimes(1);

      analysisJobServiceSpy.getJobs.mockReturnValueOnce(of(failedJobs));
      component.loadAnalysisJobs('cf1');
      expect(warnSpy).toHaveBeenCalledTimes(1); // pas de second log
    });
  });

  // F-190 SF-190-03 — compteur "X/7 sections reçues" dans le bandeau de progression.
  describe('SF-190-03 — streamingProgress (compteur sections)', () => {
    it('streamingProgress() retourne {received: 0, expected: 7} quand lastPartial est null', () => {
      component.lastPartial.set(null);
      const progress = component.streamingProgress();
      expect(progress).toEqual({ received: 0, expected: 7 });
    });

    it('streamingProgress() compte les sections présentes dans lastPartial', () => {
      component.lastPartial.set({
        sections: {
          timeline: 'des faits chronologiques',
          faits: 'des faits',
          points_juridiques: 'analyse juridique'
        }
      } as any);
      const progress = component.streamingProgress();
      expect(progress).toEqual({ received: 3, expected: 7 });
    });

    it('event PARTIAL CASE_ANALYSIS → caseAnalysisService.getPartial appelé et lastPartial mis à jour', () => {
      const partialResponse = {
        sections: { timeline: 'A', faits: 'B' }
      };
      caseAnalysisServiceSpy.getPartial.mockReturnValue(of(partialResponse as any));

      analysisEventsSubject.next({
        caseFileId: 'cf1',
        status: 'PARTIAL',
        jobType: 'CASE_ANALYSIS'
      });

      expect(caseAnalysisServiceSpy.getPartial).toHaveBeenCalledWith('cf1');
      expect(component.lastPartial()).toEqual(partialResponse);
      expect(component.streamingProgress()).toEqual({ received: 2, expected: 7 });
    });
  });

});
