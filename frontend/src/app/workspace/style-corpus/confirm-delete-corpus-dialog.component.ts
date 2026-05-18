import { Component, Inject } from '@angular/core';
import {
  MatDialogRef,
  MAT_DIALOG_DATA,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

/** Données du dialogue de confirmation de suppression d'un document de corpus. */
export interface ConfirmDeleteCorpusDialogData {
  filename: string;
}

/**
 * F-98 / SF-98-48 — Confirmation de suppression d'un document du corpus de
 * style. Action destructive → `MatDialog` (jamais `confirm()`).
 */
@Component({
  selector: 'app-confirm-delete-corpus-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Retirer ce document du corpus ?</h2>
    <mat-dialog-content>
      <p>
        <strong>{{ data.filename }}</strong> ne contribuera plus au style
        appris par le cabinet. Cette action est définitive.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="warn" (click)="confirm()">Retirer</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDeleteCorpusDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<
      ConfirmDeleteCorpusDialogComponent,
      boolean
    >,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDeleteCorpusDialogData,
  ) {}

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }
}
