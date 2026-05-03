import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { AnalysisJobType } from '../../core/services/analysis-sse.service';

interface AnalysisJobLike {
  jobType: string;
  status: string;
}

const TRACKED_TYPES: ReadonlySet<AnalysisJobType> = new Set<AnalysisJobType>([
  'CASE_ANALYSIS',
  'ENRICHED_ANALYSIS',
  'DOCUMENT_ANALYSIS',
]);

function isTrackedType(value: string): value is AnalysisJobType {
  return TRACKED_TYPES.has(value as AnalysisJobType);
}

@Injectable()
export class DecisionalToolsProgressService {
  private readonly destroyRef = inject(DestroyRef);
  private readonly notifications = inject(GlobalAnalysisNotificationService);

  private readonly active = signal<Set<AnalysisJobType>>(new Set());

  readonly activeJobTypes = computed(() => Array.from(this.active()));
  readonly isActive = computed(() => this.active().size > 0);

  constructor() {
    this.notifications.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      this.remove(event.jobType);
    });
  }

  start(jobType: AnalysisJobType): void {
    const next = new Set(this.active());
    next.add(jobType);
    this.active.set(next);
  }

  syncFromJobs(jobs: AnalysisJobLike[]): void {
    const processing = jobs
      .filter(j => (j.status === 'PROCESSING' || j.status === 'PENDING') && isTrackedType(j.jobType))
      .map(j => j.jobType as AnalysisJobType);
    this.active.set(new Set(processing));
  }

  private remove(jobType: AnalysisJobType): void {
    if (!this.active().has(jobType)) return;
    const next = new Set(this.active());
    next.delete(jobType);
    this.active.set(next);
  }
}
