/**
 * F-240 / SF-240-03 — Modale d'acceptation des CGV avant Stripe Checkout.
 *
 * Affichée avant tout appel à billingService.createCheckoutSession ou createTopupSession.
 * Requiert que l'utilisateur coche la checkbox pour activer le bouton « Confirmer et payer ».
 * Retourne `true` (confirmé) ou `false` / `undefined` (annulé) via MatDialogRef.
 */
import { Component, ChangeDetectionStrategy, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';

export interface PaymentTermsDialogData {
  /** Libellé du plan ou du pack (ex : « Solo », « 1M tokens »). */
  planLabel: string;
  /** Prix affiché (ex : « 99 €/mois », « 9,90 € »). */
  price: string;
  /** Distingue le flow souscription plan du flow top-up. */
  type: 'SUBSCRIPTION' | 'TOPUP';
}

@Component({
  selector: 'app-payment-terms-acceptance-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatCheckboxModule, FormsModule],
  templateUrl: './payment-terms-acceptance-dialog.component.html',
  styles: [`
    mat-dialog-content p {
      font-size: 14px;
      color: #34425a;
      line-height: 1.6;
      margin-bottom: 16px;
    }
    .terms-list {
      margin: 0 0 16px 0;
      padding-left: 20px;
      font-size: 14px;
      color: #34425a;
      line-height: 1.8;
    }
    .terms-list a {
      color: #1a73e8;
      text-decoration: none;
    }
    .terms-list a:hover {
      text-decoration: underline;
    }
    .checkbox-row {
      display: flex;
      align-items: flex-start;
      margin-top: 8px;
    }
    mat-dialog-actions {
      padding-top: 8px;
    }
  `]
})
export class PaymentTermsAcceptanceDialogComponent {
  acceptedTerms = false;

  constructor(
    private dialogRef: MatDialogRef<PaymentTermsAcceptanceDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: PaymentTermsDialogData
  ) {}

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    if (this.acceptedTerms) {
      this.dialogRef.close(true);
    }
  }
}
