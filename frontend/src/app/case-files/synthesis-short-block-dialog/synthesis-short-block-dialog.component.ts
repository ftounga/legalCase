import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

export interface SynthesisShortBlockDialogData {
  title: string;
  icon: string;
  iconClass?: string;
  items: string[];
}

/**
 * F-162 SF-162-06 — dialog réutilisable pour afficher une liste d'items courts
 * (pièces manquantes, questions ouvertes…). Paramétrable par `data` —
 * pourra être réutilisé si d'autres blocs courts émergent.
 */
@Component({
  selector: 'app-synthesis-short-block-dialog',
  standalone: true,
  imports: [MatDialogModule, MatIconModule, MatButtonModule],
  templateUrl: './synthesis-short-block-dialog.component.html',
  styleUrl: './synthesis-short-block-dialog.component.scss',
})
export class SynthesisShortBlockDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SynthesisShortBlockDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SynthesisShortBlockDialogData,
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
