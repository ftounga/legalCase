import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

export interface DashboardStep {
  id: string;
  label: string;
  status: 'done' | 'in_progress' | 'pending';
  detail: string | null;
  anchorId: string | null; // null → navigate to synthesis
  /**
   * F-244 SF-244-01 — index de l'onglet (`mat-tab-group`) contenant le bloc cible
   * du `case-file-detail`. Une étape pointant un bloc d'un onglet déclenche d'abord
   * la bascule d'onglet, puis le scroll d'ancre. `null` pour les étapes navigant
   * vers une route séparée (synthèse).
   */
  tabIndex: number | null;
}

/** F-244 SF-244-01 — payload émis au clic d'étape : onglet à activer + ancre à scroller. */
export interface StepActivation {
  tabIndex: number;
  anchorId: string | null;
}

@Component({
  selector: 'app-case-dashboard-stepper',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './case-dashboard-stepper.component.html',
  styleUrl: './case-dashboard-stepper.component.scss',
})
export class CaseDashboardStepperComponent {
  @Input({ required: true }) steps: DashboardStep[] = [];
  @Input({ required: true }) caseFileId!: string;

  /**
   * F-244 SF-244-01 — émis quand l'étape cliquée pointe un bloc situé dans un
   * onglet du `case-file-detail`. Le parent bascule sur `tabIndex` puis scrolle
   * vers `anchorId`. Les étapes navigant vers la synthèse (route séparée)
   * n'émettent pas — `case-dashboard-stepper` gère lui-même la navigation.
   */
  @Output() stepActivated = new EventEmitter<StepActivation>();

  constructor(private router: Router) {}

  onStepClick(step: DashboardStep): void {
    if (step.status === 'done') return;
    if (step.tabIndex !== null) {
      // F-244 SF-244-01 — bloc dans un onglet : le parent bascule d'onglet puis scrolle.
      this.stepActivated.emit({ tabIndex: step.tabIndex, anchorId: step.anchorId });
    } else if (step.anchorId) {
      const el = document.getElementById(step.anchorId);
      el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      this.router.navigate(['/case-files', this.caseFileId, 'synthesis']);
    }
  }

  stepIcon(status: DashboardStep['status']): string {
    if (status === 'done') return 'check_circle';
    if (status === 'in_progress') return 'pending';
    return 'radio_button_unchecked';
  }
}
