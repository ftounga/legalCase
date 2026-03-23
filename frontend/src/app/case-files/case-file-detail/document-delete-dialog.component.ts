import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-document-delete-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Supprimer le document</h2>
    <mat-dialog-content>
      <p>Supprimer <strong>{{ data.documentName }}</strong> ?</p>
      <p>Cette action est irréversible.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(false)">Annuler</button>
      <button mat-flat-button color="warn" (click)="dialogRef.close(true)">Supprimer</button>
    </mat-dialog-actions>
  `
})
export class DocumentDeleteDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<DocumentDeleteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { documentName: string }
  ) {}
}
