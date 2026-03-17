import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';

@Component({
  selector: 'app-case-file-create-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule
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
      legalDomain: 'EMPLOYMENT_LAW',
      description: this.form.value.description || undefined
    }).subscribe({
      next: caseFile => this.dialogRef.close(caseFile),
      error: () => {
        this.saving = false;
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
