import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFileDetailComponent } from './case-file-detail.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
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
  id: 'cf1', title: 'Dossier A', legalDomain: 'EMPLOYMENT_LAW',
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
  let aiQuestionServiceSpy: jasmine.SpyObj<AiQuestionService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['getById']);
    documentServiceSpy = jasmine.createSpyObj('DocumentService', ['list', 'upload', 'downloadUrl']);
    analysisJobServiceSpy = jasmine.createSpyObj('AnalysisJobService', ['getJobs']);
    caseAnalysisServiceSpy = jasmine.createSpyObj('CaseAnalysisService', ['getAnalysis']);
    aiQuestionServiceSpy = jasmine.createSpyObj('AiQuestionService', ['getQuestions']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    caseFileServiceSpy.getById.and.returnValue(of(mockCaseFile));
    documentServiceSpy.list.and.returnValue(of([mockDocument]));
    documentServiceSpy.downloadUrl.and.returnValue('/api/v1/case-files/cf1/documents/doc1/download');
    analysisJobServiceSpy.getJobs.and.returnValue(of([]));
    caseAnalysisServiceSpy.getAnalysis.and.returnValue(throwError(() => new Error('404')));
    aiQuestionServiceSpy.getQuestions.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CaseFileDetailComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: DocumentService, useValue: documentServiceSpy },
        { provide: AnalysisJobService, useValue: analysisJobServiceSpy },
        { provide: CaseAnalysisService, useValue: caseAnalysisServiceSpy },
        { provide: AiQuestionService, useValue: aiQuestionServiceSpy },
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

  it('section Synthèse présente si synthesis() non null', () => {
    const mockSynthesis: CaseAnalysisResult = {
      status: 'DONE',
      timeline: [{ date: '2024-01-15', evenement: 'Embauche' }],
      faits: ['fait1'],
      pointsJuridiques: ['point1'],
      risques: ['risque1'],
      questionsOuvertes: ['question1'],
      modelUsed: 'claude-sonnet-4-6',
      updatedAt: '2026-03-18T10:00:00Z'
    };
    component.synthesis.set(mockSynthesis);
    fixture.detectChanges();

    const section = fixture.nativeElement.querySelector('.synthesis-card');
    expect(section).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.synthesis-section').length).toBe(5);
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

  it('section Questions IA présente si questions() non vide', () => {
    const mockQuestions: AiQuestion[] = [
      { orderIndex: 0, questionText: 'Question A ?' },
      { orderIndex: 1, questionText: 'Question B ?' }
    ];
    component.questions.set(mockQuestions);
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('.question-item');
    expect(items.length).toBe(2);
    expect(items[0].textContent.trim()).toBe('Question A ?');
    expect(items[1].textContent.trim()).toBe('Question B ?');
  });

  it('loadQuestions — erreur API → questions() reste vide', () => {
    aiQuestionServiceSpy.getQuestions.and.returnValue(throwError(() => new Error('500')));
    component.loadQuestions('cf1');
    expect(component.questions()).toEqual([]);
  });

  it('jobTypeLabel — QUESTION_GENERATION retourne le bon libellé', () => {
    expect(component.jobTypeLabel('QUESTION_GENERATION')).toBe('Génération des questions');
  });

  it('sous-section masquée si liste vide', () => {
    const mockSynthesis: CaseAnalysisResult = {
      status: 'DONE',
      timeline: [],
      faits: [],
      pointsJuridiques: [],
      risques: [],
      questionsOuvertes: ['question1'],
      modelUsed: null,
      updatedAt: null
    };
    component.synthesis.set(mockSynthesis);
    fixture.detectChanges();

    // Seule la section questionsOuvertes est affichée
    expect(fixture.nativeElement.querySelectorAll('.synthesis-section').length).toBe(1);
  });
});
