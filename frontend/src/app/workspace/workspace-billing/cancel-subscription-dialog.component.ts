import { Component, Inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

// F-247 / SF-247-02 — dialog de confirmation de résiliation d'abonnement.
export interface CancelSubscriptionDialogData {
  /** Fin de la période de facturation en cours (ISO-8601). */
  currentPeriodEnd: string | null;
}

@Component({
  selector: 'app-cancel-subscription-dialog',
  standalone: true,
  imports: [DatePipe, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Résilier l'abonnement ?</h2>
    <mat-dialog-content>
      <p>
        La résiliation prendra effet
        @if (data.currentPeriodEnd) {
          le <strong>{{ data.currentPeriodEnd | date:'longDate' }}</strong>,
        } @else {
          en fin de période de facturation en cours,
        }
        à la fin de la période de facturation déjà réglée.
      </p>
      <ul class="cancel-points">
        <li>Vous conservez l'accès complet jusqu'à cette date.</li>
        <li>Vos dossiers et documents sont conservés.</li>
        <li>L'espace repassera ensuite au plan gratuit.</li>
        <li>Vous pourrez réactiver l'abonnement à tout moment avant l'échéance.</li>
      </ul>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="warn" (click)="confirm()">Résilier l'abonnement</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .cancel-points {
      margin: 8px 0 0;
      padding-left: 20px;
      color: #34425a;
      font-size: 14px;
      line-height: 1.6;
    }
  `]
})
export class CancelSubscriptionDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<CancelSubscriptionDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: CancelSubscriptionDialogData
  ) {}

  cancel(): void { this.dialogRef.close(false); }
  confirm(): void { this.dialogRef.close(true); }
}
