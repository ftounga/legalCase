import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkspaceService } from '../core/services/workspace.service';
import { AnalyticsService } from '../core/services/analytics.service';
import { LegalConsentService, CURRENT_CONSENT_VERSION } from '../core/services/legal-consent.service';
import { DomainPickerDialogComponent, DomainPickerResult } from './domain-picker-dialog/domain-picker-dialog.component';
import { fadeInUp } from '../shared/animations';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatCardModule, MatProgressSpinnerModule, MatCheckboxModule
  ],
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class OnboardingComponent {
  form: FormGroup;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private workspaceService: WorkspaceService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private analytics: AnalyticsService,
    private legalConsentService: LegalConsentService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      acceptedTerms: [false, Validators.requiredTrue]
    });
  }

  toUppercaseName(event: Event): void {
    const input = event.target as HTMLInputElement;
    const pos = input.selectionStart ?? 0;
    this.form.get('name')!.setValue(input.value.toUpperCase(), { emitEvent: false });
    input.setSelectionRange(pos, pos);
  }

  submit(): void {
    if (this.form.invalid) return;
    const name = this.form.value.name.trim().toUpperCase();

    this.saving = true;

    // Étape A — Enregistrer l'acceptation CGU/Privacy AVANT la création du workspace (D-01)
    this.legalConsentService.acceptConsent({
      consentTypes: ['SIGNUP_TERMS', 'PRIVACY_POLICY'],
      version: CURRENT_CONSENT_VERSION
    }).subscribe({
      next: () => {
        // Étape B — Consent OK : ouvrir le dialog de sélection de domaine puis créer le workspace
        const ref = this.dialog.open(DomainPickerDialogComponent, {
          width: '640px',
          data: { workspaceName: name },
          disableClose: true
        });

        ref.afterClosed().subscribe((result: DomainPickerResult | undefined) => {
          if (!result) {
            this.saving = false;
            return;
          }
          this.workspaceService.createWorkspace(name, result.legalDomain, result.country).subscribe({
            next: () => {
              this.analytics.trackConversion();
              this.router.navigate(['/case-files']);
            },
            error: () => {
              this.saving = false;
              this.snackBar.open('Erreur lors de la création du workspace. Veuillez réessayer.', 'Fermer', {
                duration: 5000, panelClass: ['snack-error']
              });
            }
          });
        });
      },
      error: (err) => {
        // Étape C — Consent échoue : snackbar, workspace non créé
        this.saving = false;
        console.error('[SF-240-02] POST /api/v1/consent/accept erreur :', err);
        if (err?.status === 400) {
          this.snackBar.open('Une erreur est survenue. Veuillez réessayer.', 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        } else {
          this.snackBar.open('Impossible d\'enregistrer votre acceptation, merci de réessayer.', 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        }
      }
    });
  }
}
