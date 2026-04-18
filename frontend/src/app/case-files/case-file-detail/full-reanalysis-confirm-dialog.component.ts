import { Component } from '@angular/core';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export type FullReanalysisConfirmResult = 'ENRICH' | 'FULL' | 'CANCEL';

@Component({
  selector: 'app-full-reanalysis-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="warn-icon">warning</mat-icon>
      Analyse complète depuis zéro
    </h2>
    <mat-dialog-content>
      <p>Cette action crée une nouvelle analyse qui <strong>ne prendra pas en compte</strong> :</p>
      <ul>
        <li>Vos réponses aux questions complémentaires</li>
        <li>Votre validation des points procéduraux (cochés vérifié / non-respecté)</li>
        <li>Vos échanges avec l'assistant chat</li>
      </ul>
      <p class="note">Ces données restent accessibles dans votre historique mais ne seront plus utilisées par l'analyse suivante.</p>
      <p><strong>Si vous souhaitez préserver votre travail</strong>, préférez "Enrichir la synthèse actuelle".</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="cancel()">Annuler</button>
      <button mat-raised-button color="primary" (click)="enrich()">
        <mat-icon>auto_fix_high</mat-icon> Enrichir la synthèse
      </button>
      <button mat-stroked-button color="warn" (click)="full()">
        Nouvelle analyse complète
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 { display: flex; align-items: center; gap: 8px; }
    .warn-icon { color: #E67E22; }
    ul { margin: 8px 0 16px; padding-left: 20px; }
    li { margin: 4px 0; color: #1A3A5C; }
    .note {
      font-size: 13px;
      color: #6B7A8D;
      background: #F8F9FB;
      padding: 8px 12px;
      border-radius: 4px;
      margin: 12px 0;
    }
    mat-dialog-actions { gap: 8px; padding: 16px 24px; }
  `]
})
export class FullReanalysisConfirmDialogComponent {
  constructor(public dialogRef: MatDialogRef<FullReanalysisConfirmDialogComponent, FullReanalysisConfirmResult>) {}

  cancel(): void { this.dialogRef.close('CANCEL'); }
  enrich(): void { this.dialogRef.close('ENRICH'); }
  full(): void { this.dialogRef.close('FULL'); }
}
