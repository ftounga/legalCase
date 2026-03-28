import { Component, Inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';
import { OnboardingWizardService } from '../../core/services/onboarding-wizard.service';

export interface OnboardingWizardData {
  workspaceId: string;
}

interface WizardStep {
  icon: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-onboarding-wizard-dialog',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatDialogModule],
  templateUrl: './onboarding-wizard-dialog.component.html',
  styleUrl: './onboarding-wizard-dialog.component.scss'
})
export class OnboardingWizardDialogComponent {
  currentStep = signal(0);

  readonly steps: WizardStep[] = [
    {
      icon: 'celebration',
      title: 'Bienvenue sur AI LegalCase !',
      description: 'Votre workspace est prêt. En quelques étapes, découvrez comment tirer le meilleur de votre assistant juridique IA.'
    },
    {
      icon: 'folder_open',
      title: 'Créez votre premier dossier',
      description: 'Un dossier regroupe tous les documents d\'une affaire. Donnez-lui un titre et commencez à centraliser vos pièces.'
    },
    {
      icon: 'upload_file',
      title: 'Ajoutez vos documents',
      description: 'Uploadez vos contrats, courriers, décisions de justice ou notes. L\'IA prend en charge PDF, Word et images.'
    },
    {
      icon: 'auto_awesome',
      title: 'Lancez une analyse IA',
      description: 'En un clic, l\'IA analyse l\'ensemble de vos documents et génère une synthèse juridique complète : faits, risques, timeline et points clés.'
    }
  ];

  constructor(
    private dialogRef: MatDialogRef<OnboardingWizardDialogComponent>,
    @Inject(MAT_DIALOG_DATA) private data: OnboardingWizardData,
    private wizardService: OnboardingWizardService
  ) {}

  get isLastStep(): boolean {
    return this.currentStep() === this.steps.length - 1;
  }

  next(): void {
    if (this.isLastStep) {
      this.close();
    } else {
      this.currentStep.update(s => s + 1);
    }
  }

  skip(): void {
    this.close();
  }

  private close(): void {
    this.wizardService.markDone(this.data.workspaceId);
    this.dialogRef.close();
  }
}
