import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';

export interface CaseFileEditDialogData {
  id: string;
  title: string;
  description: string | null;
}

@Component({
  selector: 'app-case-file-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule
  ],
  templateUrl: './case-file-edit-dialog.component.html',
  styleUrl: './case-file-edit-dialog.component.scss'
})
export class CaseFileEditDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<CaseFileEditDialogComponent>);
  private data = inject<CaseFileEditDialogData>(MAT_DIALOG_DATA);
  private caseFileService = inject(CaseFileService);
  private snackBar = inject(MatSnackBar);

  form = this.fb.group({
    title: [this.data.title, [Validators.required, Validators.maxLength(255)]],
    description: [this.data.description ?? '', [Validators.maxLength(2000)]]
  });

  saving = false;

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    this.caseFileService.update(this.data.id, {
      title: this.form.value.title!,
      description: this.form.value.description || null
    }).subscribe({
      next: caseFile => this.dialogRef.close(caseFile),
      error: () => {
        this.saving = false;
        this.snackBar.open('Erreur lors de la modification du dossier', 'Fermer', {
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
