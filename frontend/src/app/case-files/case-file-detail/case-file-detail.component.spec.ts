import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFileDetailComponent } from './case-file-detail.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseAnalysisCommandService } from '../../core/services/case-analysis-command.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AiQuestionAnswerService } from '../../core/services/ai-question-answer.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { CaseFile } from '../../core/models/case-file.model';
import { Document } from '../../core/models/document.model';
import { AnalysisJob } from '../../core/models/analysis-job.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { AiQuestion } from '../../core/models/ai-question.model';

const mockCaseFile: CaseFile = {
  id: 'cf1', title: 'Dossier A', legalDomain: 'DROIT_DU_TRAVAIL',
  description: 'Description test', status: 'OPEN', createdAt: '2026-03-17T10:00:00Z'
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
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let documentServiceSpy: jasmine.SpyObj<DocumentService>;
  let analysisJobServiceSpy: jasmine.SpyObj<AnalysisJobService>;
  let caseAnalysisServiceSpy: jasmine.SpyObj<CaseAnalysisService>;
  let caseAnalysisCommandServiceSpy: jasmine.SpyObj<CaseAnalysisCommandService>;
  let aiQuestionServiceSpy: jasmine.SpyObj<AiQuestionService>;
  let aiQuestionAnswerServiceSpy: jasmine.SpyObj<AiQuestionAnswerService>;
  let reAnalysisServiceSpy: jasmine.SpyObj<ReAnalysisService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['getById']);
    documentServiceSpy = jasmine.createSpyObj('DocumentService', ['list', 'upload', 'downloadUrl']);
    analysisJobServiceSpy = jasmine.createSpyObj('AnalysisJobService', ['getJobs']);
    caseAnalysisServiceSpy = jasmine.createSpyObj('CaseAnalysisService', ['getAnalysis']);
    caseAnalysisCommandServiceSpy = jasmine.createSpyObj('CaseAnalysisCommandService', ['triggerAnalysis']);
    aiQuestionServiceSpy = jasmine.createSpyObj('AiQuestionService', ['getQuestions']);
    aiQuestionAnswerServiceSpy = jasmine.createSpyObj('AiQuestionAnswerService', ['submitAnswer']);
    reAnalysisServiceSpy = jasmine.createSpyObj('ReAnalysisService', ['reAnalyze']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    caseFileServiceSpy.getById.and.returnValue(of(mockCaseFile));
    documentServiceSpy.list.and.returnValue(of([mockDocument]));
    documentServiceSpy.downloadUrl.and.returnValue('/api/v1/case-files/cf1/documents/doc1/download');
    analysisJobServiceSpy.getJobs.and.returnValue(of([]));
    caseAnalysisServiceSpy.getAnalysis.and.returnValue(throwError(() => new Error('404')));
    aiQuestionServiceSpy.getQuestions.and.returnValue(of([]));
    aiQuestionAnswerServiceSpy.submitAnswer.and.returnValue(of(undefined));
    reAnalysisServiceSpy.reAnalyze.and.returnValue(of(undefined));
    caseAnalysisCommandServiceSpy.triggerAnalysis.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [CaseFileDetailComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: DocumentService, useValue: documentServiceSpy },
        { provide: AnalysisJobService, useValue: analysisJobServiceSpy },
        { provide: CaseAnalysisService, useValue: caseAnalysisServiceSpy },
        { provide: CaseAnalysisCommandService, useValue: caseAnalysisCommandServiceSpy },
        { provide: AiQuestionService, useValue: aiQuestionServiceSpy },
        { provide: AiQuestionAnswerService, useValue: aiQuestionAnswerServiceSpy },
        { provide: ReAnalysisService, useValue: reAnalysisServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'cf1' }) } } },
        provideAnimationsAsync()
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
    expect(component.loading()).toBeFalse();
  });

  it('ngOnInit — erreur dossier → snackbar + loading false', () => {
    caseFileServiceSpy.getById.and.returnValue(throwError(() => new Error('404')));
    component.ngOnInit();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });

  it('loadDocuments — erreur → snackbar affiché', () => {
    documentServiceSpy.list.and.returnValue(throwError(() => new Error('500')));
    component.loadDocuments('cf1');
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('upload succès → document ajouté en tête de liste', () => {
    const newDoc: Document = { ...mockDocument, id: 'doc2', originalFilename: 'avenant.pdf' };
    documentServiceSpy.upload.and.returnValue(of(newDoc));

    const file = new File(['content'], 'avenant.pdf', { type: 'application/pdf' });
    const event = { target: { files: [file], value: '' } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.documents()[0].originalFilename).toBe('avenant.pdf');
    expect(component.uploading()).toBeFalse();
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('upload erreur → snackbar erreur', () => {
    documentServiceSpy.upload.and.returnValue(throwError(() => new Error('400')));

    const file = new File(['content'], 'bad.exe', { type: 'application/octet-stream' });
    const event = { target: { files: [file], value: '' } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.uploading()).toBeFalse();
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
    analysisJobServiceSpy.getJobs.and.returnValue(of([]));
    component.loadAnalysisJobs('cf1');
    fixture.detectChanges();

    const section = fixture.nativeElement.querySelector('.analysis-card');
    expect(section).toBeNull();
  });

  it('section Analyse IA présente si jobs non vides', () => {
    analysisJobServiceSpy.getJobs.and.returnValue(of(mockJobs));
    component.loadAnalysisJobs('cf1');
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('.analysis-job-row');
    expect(rows.length).toBe(3);
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
      status: 'DONE',
      timeline: [],
      faits: ['fait1'],
      pointsJuridiques: [],
      risques: [],
      questionsOuvertes: [],
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
    aiQuestionServiceSpy.getQuestions.and.returnValue(throwError(() => new Error('500')));
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
    expect(component.canAnalyze()).toBeTrue();
  });

  it('canAnalyze — false si CASE_ANALYSIS PROCESSING', () => {
    component.analysisJobs.set([
      { jobType: 'DOCUMENT_ANALYSIS', status: 'DONE', totalItems: 1, processedItems: 1, progressPercentage: 100 },
      { jobType: 'CASE_ANALYSIS', status: 'PROCESSING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    expect(component.canAnalyze()).toBeFalse();
  });

  it('canAnalyze — false si aucun DOCUMENT_ANALYSIS DONE', () => {
    component.analysisJobs.set([
      { jobType: 'DOCUMENT_ANALYSIS', status: 'PROCESSING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    expect(component.canAnalyze()).toBeFalse();
  });

  it('caseAnalysisRunning — true si CASE_ANALYSIS PENDING', () => {
    component.analysisJobs.set([
      { jobType: 'CASE_ANALYSIS', status: 'PENDING', totalItems: 1, processedItems: 0, progressPercentage: 0 }
    ]);
    expect(component.caseAnalysisRunning()).toBeTrue();
  });

  it('triggerAnalysis — succès → service appelé + loadAnalysisJobs', () => {
    caseAnalysisCommandServiceSpy.triggerAnalysis.and.returnValue(of(undefined));
    component.triggerAnalysis();
    expect(caseAnalysisCommandServiceSpy.triggerAnalysis).toHaveBeenCalledWith('cf1');
    expect(analysisJobServiceSpy.getJobs).toHaveBeenCalled();
  });

  it('triggerAnalysis — 402 → snackbar "Limite atteinte"', () => {
    caseAnalysisCommandServiceSpy.triggerAnalysis.and.returnValue(throwError(() => ({ status: 402 })));
    component.triggerAnalysis();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Limite'), jasmine.any(String), jasmine.any(Object)
    );
  });

  it('triggerAnalysis — 409 → snackbar "déjà en cours"', () => {
    caseAnalysisCommandServiceSpy.triggerAnalysis.and.returnValue(throwError(() => ({ status: 409 })));
    component.triggerAnalysis();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      jasmine.stringContaining('cours'), jasmine.any(String), jasmine.any(Object)
    );
  });

});
