import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TourService {
  private readonly STORAGE_KEY_PREFIX = 'onboarding_tour_done_';
  private readonly TOTAL_STEPS = 5;

  private _active = signal(false);
  private _step = signal(0);
  private _workspaceId: string | null = null;

  isActive = this._active.asReadonly();
  currentStep = this._step.asReadonly();

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

  skip(): void {
    this.stop();
  }

  private stop(): void {
    if (this._workspaceId) {
      try {
        localStorage.setItem(this.STORAGE_KEY_PREFIX + this._workspaceId, '1');
      } catch { /* fail silencieux */ }
    }
    this._active.set(false);
    this._workspaceId = null;
  }
}
