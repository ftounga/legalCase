import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmRemoveMemberDialogData {
  memberName: string;
  currentCostEuros: number;
  newCostEuros: number;
  currency: string;
  savesCost: boolean;
}

@Component({
  selector: 'app-confirm-remove-member-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Retirer ce membre ?</h2>
    <mat-dialog-content>
      <p><strong>{{ data.memberName }}</strong> perdra l'accès à l'espace et à ses dossiers.</p>
      @if (data.savesCost) {
        <p class="cost-note">
          Le coût mensuel passera de {{ data.currentCostEuros }} {{ data.currency }}
          à {{ data.newCostEuros }} {{ data.currency }} (proratisation au prochain prélèvement).
        </p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="warn" (click)="confirm()">Retirer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .cost-note {
      margin-top: 8px;
      padding: 12px;
      border-radius: 8px;
      background: #f3f6fb;
      color: #34425a;
      font-size: 14px;
    }
  `]
})
export class ConfirmRemoveMemberDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<ConfirmRemoveMemberDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmRemoveMemberDialogData
  ) {}

  cancel(): void { this.dialogRef.close(false); }
  confirm(): void { this.dialogRef.close(true); }
}
