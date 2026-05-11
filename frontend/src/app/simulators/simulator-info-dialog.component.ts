import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { RouterModule } from '@angular/router';

export interface SimulatorInfoDialogData {
  displayLabel: string;
}

/**
 * F-163 / SF-163-01 — Dialog pédagogique affiché au clic sur une card du
 * catalogue Simulateurs. V1 : le mode standalone réellement utilisable
 * (sans dossier) arrive en SF-163-02 — pour l'instant on guide l'utilisateur
 * vers la création de dossier (route `/case-files/new`).
 */
@Component({
  selector: 'app-simulator-info-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, RouterModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="dialog-icon">science</mat-icon>
      Utiliser le simulateur {{ data.displayLabel }}
    </h2>
    <mat-dialog-content>
      <p>
        Pour utiliser ce simulateur avec les données d'un dossier client,
        créez d'abord un dossier d'analyse.
      </p>
      <p class="dialog-note">
        Le mode simulateur autonome (sans dossier) arrivera dans une prochaine
        version.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Fermer</button>
      <button
        mat-flat-button
        color="primary"
        routerLink="/case-files/new"
        [mat-dialog-close]="true"
      >
        <mat-icon>add</mat-icon>
        Créer un dossier
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-icon {
        vertical-align: middle;
        margin-right: 8px;
        color: #1a3a5c;
      }
      .dialog-note {
        color: #6b7280;
        font-size: 0.92rem;
      }
      h2[mat-dialog-title] {
        color: #1a3a5c;
        font-family: 'Inter', sans-serif;
      }
    `,
  ],
})
export class SimulatorInfoDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SimulatorInfoDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: SimulatorInfoDialogData,
  ) {}
}
