import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

export interface ReferentialReportDialogData {
  entryLabel: string;
}

export interface ReferentialReportDialogResult {
  comment: string;
}

@Component({
  selector: 'app-referential-report-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>Signaler une anomalie</h2>
    <mat-dialog-content>
      <p class="entry-name"><strong>{{ data.entryLabel }}</strong></p>
      <form [formGroup]="form">
        <mat-form-field appearance="outline" style="width:100%">
          <mat-label>Commentaire</mat-label>
          <textarea matInput formControlName="comment" rows="4"
                    placeholder="Décrivez l'anomalie constatée…" maxlength="500"></textarea>
          <mat-hint align="end">{{ form.value.comment?.length ?? 0 }}/500</mat-hint>
          @if (form.get('comment')?.hasError('required')) {
            <mat-error>Le commentaire est requis.</mat-error>
          }
          @if (form.get('comment')?.hasError('maxlength')) {
            <mat-error>500 caractères maximum.</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid" (click)="submit()">
        Envoyer le signalement
      </button>
    </mat-dialog-actions>
  `,
})
export class ReferentialReportDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<ReferentialReportDialogComponent, ReferentialReportDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: ReferentialReportDialogData
  ) {
    this.form = this.fb.group({
      comment: ['', [Validators.required, Validators.maxLength(500)]],
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.dialogRef.close({ comment: this.form.value.comment });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
