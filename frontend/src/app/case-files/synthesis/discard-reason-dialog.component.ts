import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface DiscardReasonDialogData {
  initialReason: string | null;
  pisteTexte: string;
}

@Component({
  selector: 'app-discard-reason-dialog',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Écarter cette piste</h2>
    <mat-dialog-content>
      <p class="piste-preview">{{ data.pisteTexte }}</p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Raison (facultatif)</mat-label>
        <textarea matInput rows="3" [(ngModel)]="reason" placeholder="Ex : non applicable au dossier, déjà tenté…"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="confirm()">Confirmer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .piste-preview {
      font-style: italic;
      color: #555;
      margin-bottom: 12px;
      padding: 8px 12px;
      border-left: 3px solid #C9A24A;
      background: #FAF7E8;
    }
    .full-width { width: 100%; }
  `]
})
export class DiscardReasonDialogComponent {
  reason: string;

  constructor(
    private dialogRef: MatDialogRef<DiscardReasonDialogComponent, string | null>,
    @Inject(MAT_DIALOG_DATA) public data: DiscardReasonDialogData
  ) {
    this.reason = data.initialReason ?? '';
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  confirm(): void {
    const trimmed = this.reason.trim();
    this.dialogRef.close(trimmed.length > 0 ? trimmed : null);
  }
}
