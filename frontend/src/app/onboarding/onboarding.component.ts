import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkspaceService } from '../core/services/workspace.service';
import { DomainPickerDialogComponent, DomainPickerResult } from './domain-picker-dialog/domain-picker-dialog.component';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatCardModule, MatProgressSpinnerModule
  ],
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.scss'
})
export class OnboardingComponent {
  form: FormGroup;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private workspaceService: WorkspaceService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]]
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

    const ref = this.dialog.open(DomainPickerDialogComponent, {
      width: '640px',
      data: { workspaceName: name },
      disableClose: true
    });

    ref.afterClosed().subscribe((result: DomainPickerResult | undefined) => {
      if (!result) return;
      this.saving = true;
      this.workspaceService.createWorkspace(name, result.legalDomain, result.country).subscribe({
        next: () => this.router.navigate(['/case-files']),
        error: () => {
          this.saving = false;
          this.snackBar.open('Erreur lors de la création du workspace. Veuillez réessayer.', 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        }
      });
    });
  }
}
