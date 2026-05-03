import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { AnalysisJobType } from '../../core/services/analysis-sse.service';
import {
  PrefillDiffDialogComponent,
  PrefillDiffDialogData,
  PrefillDiffEntry,
} from './prefill-diff-dialog/prefill-diff-dialog.component';

interface AnalysisJobLike {
  jobType: string;
  status: string;
}

export interface ToolMetadata {
  label: string;
  icon: string;
}

const FLASH_DURATION_MS = 1500;
const TOAST_DURATION_MS = 8000;

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
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  private readonly active = signal<Set<AnalysisJobType>>(new Set());
  private readonly flashed = signal<Set<string>>(new Set());

  readonly activeJobTypes = computed(() => Array.from(this.active()));
  readonly isActive = computed(() => this.active().size > 0);
  readonly flashedToolIds = this.flashed.asReadonly();

  private previousSnapshot: Map<string, number> | null = null;
  private readonly flashTimers = new Map<string, ReturnType<typeof setTimeout>>();

  constructor() {
    this.notifications.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      this.remove(event.jobType);
    });
    this.destroyRef.onDestroy(() => {
      this.flashTimers.forEach(t => clearTimeout(t));
      this.flashTimers.clear();
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

  recordSnapshot(snapshot: Map<string, number>, metadata: Map<string, ToolMetadata>): void {
    const previous = this.previousSnapshot;
    this.previousSnapshot = new Map(snapshot);

    if (previous === null) {
      return;
    }

    const entries: PrefillDiffEntry[] = [];
    snapshot.forEach((count, toolId) => {
      const before = previous.get(toolId) ?? 0;
      const delta = count - before;
      if (delta > 0) {
        this.flashTool(toolId);
        const meta = metadata.get(toolId);
        if (meta) {
          entries.push({ toolId, label: meta.label, icon: meta.icon, delta });
        }
      }
    });

    if (entries.length === 0) return;

    const totalDelta = entries.reduce((sum, e) => sum + e.delta, 0);
    const toolsCount = entries.length;
    const message = `${totalDelta} champ${totalDelta > 1 ? 's' : ''} pré-rempli${totalDelta > 1 ? 's' : ''} dans ${toolsCount} outil${toolsCount > 1 ? 's' : ''}`;

    const ref = this.snackBar.open(message, 'Voir le détail', {
      duration: TOAST_DURATION_MS,
      panelClass: ['snack-success'],
    });
    ref.onAction().subscribe(() => {
      const data: PrefillDiffDialogData = { entries };
      this.dialog.open(PrefillDiffDialogComponent, { data, autoFocus: false });
    });
  }

  private flashTool(toolId: string): void {
    const next = new Set(this.flashed());
    next.add(toolId);
    this.flashed.set(next);

    const existing = this.flashTimers.get(toolId);
    if (existing) clearTimeout(existing);

    const timer = setTimeout(() => {
      const after = new Set(this.flashed());
      after.delete(toolId);
      this.flashed.set(after);
      this.flashTimers.delete(toolId);
    }, FLASH_DURATION_MS);
    this.flashTimers.set(toolId, timer);
  }

  private remove(jobType: AnalysisJobType): void {
    if (!this.active().has(jobType)) return;
    const next = new Set(this.active());
    next.delete(jobType);
    this.active.set(next);
  }
}
