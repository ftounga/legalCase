import { Injectable, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CaseFileService } from './case-file.service';
import { CaseFileStatusService } from './case-file-status.service';

@Injectable({ providedIn: 'root' })
export class TourService {
  private readonly STORAGE_KEY_PREFIX = 'onboarding_tour_done_';
  private readonly TOTAL_STEPS = 5;

  private _active = signal(false);
  private _step = signal(0);
  private _workspaceId: string | null = null;
  private _demoCaseFileId = signal<string | null>(null);

  isActive = this._active.asReadonly();
  currentStep = this._step.asReadonly();
  demoCaseFileId = this._demoCaseFileId.asReadonly();

  private router = inject(Router);
  private caseFileService = inject(CaseFileService);
  private caseFileStatusService = inject(CaseFileStatusService);

  shouldShow(workspaceId: string | null | undefined): boolean {
    if (!workspaceId) return false;
    try {
      return localStorage.getItem(this.STORAGE_KEY_PREFIX + workspaceId) === null;
    } catch {
      return false;
    }
  }

  start(workspaceId: string): void {
    if (!this.shouldShow(workspaceId)) return;
    this._workspaceId = workspaceId;
    this._step.set(0);
    this._active.set(true);
  }

  next(): void {
    const next = this._step() + 1;
    if (next >= this.TOTAL_STEPS) {
      this.stop();
    } else {
      this._step.set(next);
    }
  }

  /**
   * Appelé lors du passage step 1 → step 2.
   * Si aucun dossier existant : crée un dossier de démonstration et navigue dedans.
   * Sinon : avance normalement.
   */
  advanceToStep2(hasExistingFiles: boolean): void {
    if (!hasExistingFiles) {
      this.caseFileService.create({ title: 'Dossier de démonstration' }).subscribe({
        next: cf => {
          this._demoCaseFileId.set(cf.id);
          this._step.set(2);
          this.router.navigate(['/case-files', cf.id]);
        },
        error: () => {
          // Échec silencieux — on avance quand même sans dossier demo
          this._step.set(2);
        }
      });
    } else {
      this._step.set(2);
    }
  }

  skip(): void {
    this.stop();
  }

  private stop(): void {
    this.cleanup();
    if (this._workspaceId) {
      try {
        localStorage.setItem(this.STORAGE_KEY_PREFIX + this._workspaceId, '1');
      } catch { /* fail silencieux */ }
    }
    this._active.set(false);
    this._workspaceId = null;
    this.router.navigate(['/case-files']);
  }

  private cleanup(): void {
    const id = this._demoCaseFileId();
    if (id) {
      this.caseFileStatusService.delete(id).subscribe({ error: () => {} });
      this._demoCaseFileId.set(null);
    }
  }
}
