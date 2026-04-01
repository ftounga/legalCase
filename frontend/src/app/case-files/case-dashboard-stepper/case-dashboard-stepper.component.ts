import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

export interface DashboardStep {
  id: string;
  label: string;
  status: 'done' | 'in_progress' | 'pending';
  detail: string | null;
  anchorId: string | null; // null → navigate to synthesis
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

  constructor(private router: Router) {}

  onStepClick(step: DashboardStep): void {
    if (step.status === 'done') return;
    if (step.anchorId) {
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
