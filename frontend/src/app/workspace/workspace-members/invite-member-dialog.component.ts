import { Component, Inject, signal, computed } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SeatsSummary } from '../../core/models/seats-summary.model';

export interface InviteMemberDialogResult {
  email: string;
  role: string;
}

@Component({
  selector: 'app-invite-member-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Inviter un membre</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="invite-form">
        <mat-form-field appearance="outline">
          <mat-label>Adresse email</mat-label>
          <input matInput formControlName="email" type="email" placeholder="avocat@cabinet.fr" />
          @if (form.get('email')?.hasError('required') && form.get('email')?.touched) {
            <mat-error>L'email est requis</mat-error>
          }
          @if (form.get('email')?.hasError('email') && form.get('email')?.touched) {
            <mat-error>Email invalide</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Rôle</mat-label>
          <mat-select formControlName="role">
            <mat-option value="ADMIN">Admin</mat-option>
            <mat-option value="LAWYER">Avocat</mat-option>
            <mat-option value="MEMBER">Membre</mat-option>
          </mat-select>
        </mat-form-field>

        <div class="cost-preview" [class.cost-preview--warn]="capReached()">
          <mat-icon class="cost-icon">{{ capReached() ? 'lock' : 'info' }}</mat-icon>
          <span>{{ costMessage() }}</span>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="submit()" [disabled]="form.invalid || capReached()">
        Envoyer l'invitation
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .invite-form { display: flex; flex-direction: column; gap: 16px; min-width: 400px; }
    .cost-preview {
      display: flex; align-items: center; gap: 8px;
      padding: 12px 16px; border-radius: 8px;
      background: #eef6ff; color: #1a4a7a;
      font-size: 14px; line-height: 1.4;
    }
    .cost-preview--warn { background: #fff4e6; color: #8a4a00; }
    .cost-icon { font-size: 20px; width: 20px; height: 20px; }
  `]
})
export class InviteMemberDialogComponent {
  form: FormGroup;

  readonly summary = signal<SeatsSummary | null>(null);

  readonly capReached = computed(() => {
    const s = this.summary();
    if (!s) return false;
    return s.seatCount >= s.maxSeats;
  });

  readonly costMessage = computed(() => {
    const s = this.summary();
    if (!s) return 'Chargement des informations de facturation…';
    if (s.seatCount >= s.maxSeats) {
      if (s.planCode === 'FREE') return 'Le plan FREE ne permet pas d\'inviter de collaborateur. Passez à SOLO ou plus.';
      if (s.planCode === 'SOLO') return 'Passez à TEAM pour inviter des collaborateurs (jusqu\'à 6 utilisateurs).';
      if (s.planCode === 'TEAM') return 'Plan TEAM limité à 6 utilisateurs. Passez à PRO pour en ajouter davantage.';
      return 'Limite d\'utilisateurs atteinte.';
    }
    if (s.seatCount < s.includedSeats) {
      return 'Invitation incluse dans votre plan — aucun coût supplémentaire.';
    }
    const euros = (s.extraSeatPriceCents / 100).toFixed(0);
    return `Ajoutera ${euros} €/mois à votre abonnement (proratisé au jour près).`;
  });

  constructor(
    private dialogRef: MatDialogRef<InviteMemberDialogComponent, InviteMemberDialogResult>,
    @Inject(MAT_DIALOG_DATA) data: { summary: SeatsSummary | null },
    fb: FormBuilder
  ) {
    this.form = fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['LAWYER', Validators.required]
    });
    this.summary.set(data?.summary ?? null);
  }

  cancel(): void { this.dialogRef.close(); }

  submit(): void {
    if (this.form.invalid || this.capReached()) return;
    this.dialogRef.close({ email: this.form.value.email, role: this.form.value.role });
  }
}
