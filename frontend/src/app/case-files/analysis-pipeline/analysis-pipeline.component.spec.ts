import { TestBed, ComponentFixture } from '@angular/core/testing';
import { AnalysisPipelineComponent } from './analysis-pipeline.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { AnalysisJob } from '../../core/models/analysis-job.model';

const job = (jobType: AnalysisJob['jobType'], status: AnalysisJob['status'], pct = 0, total = 0, processed = 0): AnalysisJob =>
  ({ jobType, status, progressPercentage: pct, totalItems: total, processedItems: processed });

describe('AnalysisPipelineComponent', () => {
  let fixture: ComponentFixture<AnalysisPipelineComponent>;
  let component: AnalysisPipelineComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalysisPipelineComponent],
      providers: [provideAnimationsAsync()],
    }).compileComponents();
    fixture = TestBed.createComponent(AnalysisPipelineComponent);
    component = fixture.componentInstance;
  });

  // PROG-01 : job PENDING → étape active-pending avec barre buffer
  it('PROG-01 : PENDING → étape active-pending, mode buffer', () => {
    component.jobs = [job('DOCUMENT_ANALYSIS', 'PENDING')];
    component.documentsCount = 1;
    component.ngOnChanges();
    fixture.detectChanges();

    const step = component.steps.find(s => s.jobType === 'DOCUMENT_ANALYSIS')!;
    expect(step.status).toBe('active-pending');
    expect(step.progressMode).toBe('buffer');
    expect(step.progressValue).toBe(0);
  });

  // PROG-02 : job PROCESSING 50% avec compteur
  it('PROG-02 : PROCESSING 50% → active-processing, compteur "5 / 10"', () => {
    component.jobs = [job('DOCUMENT_ANALYSIS', 'PROCESSING', 50, 10, 5)];
    component.documentsCount = 1;
    component.ngOnChanges();
    fixture.detectChanges();

    const step = component.steps.find(s => s.jobType === 'DOCUMENT_ANALYSIS')!;
    expect(step.status).toBe('active-processing');
    expect(step.progressMode).toBe('determinate');
    expect(step.progressValue).toBe(50);
    expect(step.counter).toBe('5 / 10');
  });

  // PROG-03 : job DONE → étape done, barre verte 100%
  it('PROG-03 : DONE → étape done, progressValue 100', () => {
    component.jobs = [job('DOCUMENT_ANALYSIS', 'DONE', 100, 10, 10)];
    component.documentsCount = 1;
    component.ngOnChanges();

    const step = component.steps.find(s => s.jobType === 'DOCUMENT_ANALYSIS')!;
    expect(step.status).toBe('done');
    expect(step.progressValue).toBe(100);
  });

  // PROG-04 : job FAILED → étape failed, barre rouge
  it('PROG-04 : FAILED → étape failed, progressValue 100', () => {
    component.jobs = [job('DOCUMENT_ANALYSIS', 'FAILED')];
    component.documentsCount = 1;
    component.ngOnChanges();

    const step = component.steps.find(s => s.jobType === 'DOCUMENT_ANALYSIS')!;
    expect(step.status).toBe('failed');
    expect(step.progressValue).toBe(100);
  });

  // PROG-05 : pas de job → Upload active si uploading = true
  it('PROG-05 : uploading=true → étape Upload active-processing', () => {
    component.jobs = [];
    component.uploading = true;
    component.uploadProgress = 42;
    component.documentsCount = 0;
    component.ngOnChanges();

    const uploadStep = component.steps[0];
    expect(uploadStep.jobType).toBe('UPLOAD');
    expect(uploadStep.status).toBe('active-processing');
    expect(uploadStep.progressValue).toBe(42);
    expect(uploadStep.counter).toBe('42%');
  });

  // PROG-06 : totalItems === 0 → compteur masqué
  it('PROG-06 : totalItems=0 → compteur null', () => {
    component.jobs = [job('CASE_ANALYSIS', 'PROCESSING', 0, 0, 0)];
    component.documentsCount = 1;
    component.ngOnChanges();

    const step = component.steps.find(s => s.jobType === 'CASE_ANALYSIS')!;
    expect(step.counter).toBeNull();
  });

  // Steps 5 toujours présentes dans l'ordre
  it('5 étapes dans le bon ordre', () => {
    component.jobs = [];
    component.documentsCount = 0;
    component.ngOnChanges();

    const types = component.steps.map(s => s.jobType);
    expect(types).toEqual(['UPLOAD', 'DOCUMENT_ANALYSIS', 'CASE_ANALYSIS', 'QUESTION_GENERATION', 'ENRICHED_ANALYSIS']);
  });

  // Upload done si docs > 0 et pas d'upload en cours
  it('Upload step : done si documentsCount > 0 et uploading=false', () => {
    component.jobs = [];
    component.uploading = false;
    component.documentsCount = 3;
    component.ngOnChanges();

    const uploadStep = component.steps[0];
    expect(uploadStep.status).toBe('done');
    expect(uploadStep.progressValue).toBe(100);
  });

  // Jobs sans correspondance → waiting
  it('Job sans correspondance → waiting', () => {
    component.jobs = [];
    component.documentsCount = 0;
    component.ngOnChanges();

    const caseStep = component.steps.find(s => s.jobType === 'CASE_ANALYSIS')!;
    expect(caseStep.status).toBe('waiting');
    expect(caseStep.progressValue).toBe(0);
  });

  // SF-121-04 : counter "X non analysables / Y" pour DOCUMENT_ANALYSIS FAILED
  it('U-AP-04-01 : DOCUMENT_ANALYSIS FAILED → counter "3 non analysables / 9"', () => {
    component.jobs = [job('DOCUMENT_ANALYSIS', 'FAILED', 100, 9, 3)];
    component.documentsCount = 9;
    component.ngOnChanges();

    const step = component.steps.find(s => s.jobType === 'DOCUMENT_ANALYSIS')!;
    expect(step.status).toBe('failed');
    expect(step.counter).toBe('3 non analysables / 9');
  });

  // SF-121-04 : le format spécial ne s'applique qu'à DOCUMENT_ANALYSIS FAILED
  it('U-AP-04-02 : CASE_ANALYSIS FAILED → counter classique "X / Y"', () => {
    component.jobs = [job('CASE_ANALYSIS', 'FAILED', 100, 5, 3)];
    component.documentsCount = 1;
    component.ngOnChanges();

    const step = component.steps.find(s => s.jobType === 'CASE_ANALYSIS')!;
    expect(step.status).toBe('failed');
    expect(step.counter).toBe('3 / 5');
  });
});
