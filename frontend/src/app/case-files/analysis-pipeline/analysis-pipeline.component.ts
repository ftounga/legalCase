import { Component, Input, OnChanges, OnDestroy } from '@angular/core';
import { NgClass } from '@angular/common';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { AnalysisJob } from '../../core/models/analysis-job.model';

export type StepStatus = 'done' | 'active-processing' | 'active-pending' | 'waiting' | 'failed';

interface PipelineStep {
  jobType: AnalysisJob['jobType'] | 'UPLOAD';
  label: string;
  status: StepStatus;
  progressMode: 'determinate' | 'buffer' | 'indeterminate';
  progressValue: number;
  counter: string | null;
  elapsed: string | null;
}

@Component({
  selector: 'app-analysis-pipeline',
  standalone: true,
  imports: [NgClass, MatProgressBarModule, MatIconModule],
  templateUrl: './analysis-pipeline.component.html',
  styleUrl: './analysis-pipeline.component.scss',
})
export class AnalysisPipelineComponent implements OnChanges, OnDestroy {
  @Input() jobs: AnalysisJob[] = [];
  @Input() uploading = false;
  @Input() uploadProgress = 0;
  @Input() documentsCount = 0;

  steps: PipelineStep[] = [];

  private readonly startedAt = new Map<string, number>();
  private readonly elapsedSec = new Map<string, number>();
  private ticker: ReturnType<typeof setInterval> | null = null;

  private static readonly JOB_DEFS: Array<{ jobType: AnalysisJob['jobType']; label: string }> = [
    { jobType: 'DOCUMENT_ANALYSIS', label: 'Analyse des documents' },
    { jobType: 'CASE_ANALYSIS',     label: 'Synthèse du dossier'   },
    { jobType: 'QUESTION_GENERATION', label: 'Génération des questions' },
    { jobType: 'ENRICHED_ANALYSIS', label: 'Synthèse enrichie'     },
  ];

  ngOnChanges(): void {
    this.syncElapsed();
    this.buildSteps();
  }

  ngOnDestroy(): void {
    this.stopTicker();
  }

  private syncElapsed(): void {
    for (const job of this.jobs) {
      if (job.status === 'PROCESSING') {
        if (!this.startedAt.has(job.jobType)) {
          this.startedAt.set(job.jobType, Date.now());
        }
      } else {
        this.startedAt.delete(job.jobType);
        this.elapsedSec.delete(job.jobType);
      }
    }

    const hasProcessing = this.jobs.some(j => j.status === 'PROCESSING');
    if (hasProcessing && !this.ticker) {
      this.ticker = setInterval(() => {
        for (const [type, start] of this.startedAt.entries()) {
          this.elapsedSec.set(type, Math.floor((Date.now() - start) / 1000));
        }
        this.buildSteps();
      }, 1000);
    } else if (!hasProcessing) {
      this.stopTicker();
    }
  }

  private stopTicker(): void {
    if (this.ticker) {
      clearInterval(this.ticker);
      this.ticker = null;
    }
  }

  private job(type: AnalysisJob['jobType']): AnalysisJob | undefined {
    return this.jobs.find(j => j.jobType === type);
  }

  private stepStatus(j: AnalysisJob | undefined): StepStatus {
    if (!j) return 'waiting';
    if (j.status === 'DONE')       return 'done';
    if (j.status === 'FAILED')     return 'failed';
    if (j.status === 'PROCESSING') return 'active-processing';
    return 'active-pending';
  }

  private progressMode(j: AnalysisJob | undefined): PipelineStep['progressMode'] {
    if (!j || j.status === 'PENDING') return 'buffer';
    return 'determinate';
  }

  private progressValue(j: AnalysisJob | undefined): number {
    if (!j) return 0;
    if (j.status === 'DONE' || j.status === 'FAILED') return 100;
    return Math.min(100, Math.max(0, j.progressPercentage));
  }

  private counter(j: AnalysisJob | undefined): string | null {
    if (!j || j.totalItems === 0) return null;
    return `${j.processedItems} / ${j.totalItems}`;
  }

  private elapsed(type: AnalysisJob['jobType']): string | null {
    const secs = this.elapsedSec.get(type);
    if (secs === undefined) return null;
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return m === 0 ? `${s}s` : `${m}m ${s}s`;
  }

  buildSteps(): void {
    let uploadStatus: StepStatus;
    if (this.uploading) {
      uploadStatus = 'active-processing';
    } else if (this.documentsCount > 0) {
      uploadStatus = 'done';
    } else {
      uploadStatus = 'waiting';
    }

    const uploadStep: PipelineStep = {
      jobType: 'UPLOAD',
      label: 'Upload des documents',
      status: uploadStatus,
      progressMode: 'determinate',
      progressValue: this.uploading ? this.uploadProgress : (this.documentsCount > 0 ? 100 : 0),
      counter: this.uploading && this.uploadProgress > 0 ? `${Math.round(this.uploadProgress)}%` : null,
      elapsed: null,
    };

    const jobSteps = AnalysisPipelineComponent.JOB_DEFS.map(({ jobType, label }) => {
      const j = this.job(jobType);
      const status = this.stepStatus(j);
      return {
        jobType,
        label,
        status,
        progressMode: this.progressMode(j),
        progressValue: this.progressValue(j),
        counter: this.counter(j),
        elapsed: status === 'active-processing' ? this.elapsed(jobType) : null,
      } as PipelineStep;
    });

    this.steps = [uploadStep, ...jobSteps];
  }

  stepIcon(status: StepStatus): string {
    if (status === 'done')             return 'check_circle';
    if (status === 'failed')           return 'error';
    if (status === 'active-pending')   return 'hourglass_empty';
    if (status === 'active-processing') return 'pending';
    return 'radio_button_unchecked';
  }
}
