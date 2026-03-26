import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-case-file-delete-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Supprimer le dossier</h2>
    <mat-dialog-content>
      <p>Êtes-vous sûr de vouloir supprimer ce dossier ?</p>
      <p>Cette action est irréversible.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(false)">Annuler</button>
      <button mat-flat-button color="warn" (click)="dialogRef.close(true)">Supprimer</button>
    </mat-dialog-actions>
  `
})
export class CaseFileDeleteDialogComponent {
  constructor(public dialogRef: MatDialogRef<CaseFileDeleteDialogComponent>) {}
}
