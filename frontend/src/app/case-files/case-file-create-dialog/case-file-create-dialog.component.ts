import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { QuotaErrorBannerComponent } from '../../shared/quota-error-banner/quota-error-banner.component';

@Component({
  selector: 'app-case-file-create-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule,
    QuotaErrorBannerComponent
  ],
  templateUrl: './case-file-create-dialog.component.html',
  styleUrl: './case-file-create-dialog.component.scss'
})
export class CaseFileCreateDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<CaseFileCreateDialogComponent>);
  private caseFileService = inject(CaseFileService);
  private snackBar = inject(MatSnackBar);

  form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(2000)]]
  });

  saving = false;

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    this.caseFileService.create({
      title: this.form.value.title!,
      description: this.form.value.description || undefined
    }).subscribe({
      next: caseFile => this.dialogRef.close(caseFile),
      error: (err: any) => {
        this.saving = false;
        // SF-171-02 : 402 (CASE_FILE_OPEN_LIMIT_EXCEEDED) géré par paymentRequiredInterceptor + QuotaErrorState
        // → bandeau visible directement dans la dialog.
        if (err.status === 402) return;
        this.snackBar.open('Erreur lors de la création du dossier', 'Fermer', {
          duration: 4000,
          panelClass: ['snack-error']
        });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
