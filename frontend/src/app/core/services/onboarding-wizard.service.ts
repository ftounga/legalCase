import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class OnboardingWizardService {
  private key(workspaceId: string): string {
    return `onboarding_wizard_done_${workspaceId}`;
  }

  shouldShow(workspaceId: string | null | undefined): boolean {
    if (!workspaceId) return false;
    try {
      return localStorage.getItem(this.key(workspaceId)) === null;
    } catch {
      return false;
    }
  }

  markDone(workspaceId: string): void {
    try {
      localStorage.setItem(this.key(workspaceId), '1');
    } catch {
      // localStorage indisponible — fail silencieux
    }
  }
}
