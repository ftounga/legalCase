import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkspaceService } from '../core/services/workspace.service';

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
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]]
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    this.workspaceService.createWorkspace(this.form.value.name.trim()).subscribe({
      next: () => this.router.navigate(['/case-files']),
      error: () => {
        this.saving = false;
        this.snackBar.open('Erreur lors de la création du workspace. Veuillez réessayer.', 'Fermer', {
          duration: 5000, panelClass: ['snack-error']
        });
      }
    });
  }
}
