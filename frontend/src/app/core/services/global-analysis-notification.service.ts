import { Injectable } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AnalysisSseService, AnalysisStatusEvent } from './analysis-sse.service';

@Injectable({ providedIn: 'root' })
export class GlobalAnalysisNotificationService {

  private readonly subscriptions = new Map<string, Subscription>();
  private readonly eventsSubject = new Subject<AnalysisStatusEvent>();
  readonly events$ = this.eventsSubject.asObservable();

  constructor(
    private analysisSseService: AnalysisSseService,
    private snackBar: MatSnackBar
  ) {}

  track(caseFileId: string): void {
    this.subscriptions.get(caseFileId)?.unsubscribe();
    const sub = this.analysisSseService.stream(caseFileId).subscribe({
      next: event => {
        this.eventsSubject.next(event);
        this.showToast(event);
      },
      complete: () => this.subscriptions.delete(caseFileId)
    });
    this.subscriptions.set(caseFileId, sub);
  }

  private showToast(event: AnalysisStatusEvent): void {
    const toasts: Record<string, Record<string, string>> = {
      CASE_ANALYSIS: {
        DONE: 'Synthèse du dossier terminée',
        FAILED: 'La synthèse du dossier a échoué'
      },
      ENRICHED_ANALYSIS: {
        DONE: 'Re-synthèse enrichie terminée',
        FAILED: 'La re-synthèse a échoué'
      },
      DOCUMENT_ANALYSIS: {
        DONE: 'Analyse des documents terminée',
        FAILED: "L'analyse des documents a échoué"
      }
    };
    const msg = toasts[event.jobType]?.[event.status];
    if (!msg) return;
    const panelClass = event.status === 'DONE' ? ['snack-success'] : ['snack-error'];
    this.snackBar.open(msg, 'Fermer', { duration: 4000, panelClass });
  }
}
