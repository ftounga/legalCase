import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WorkspaceService } from '../../core/services/workspace.service';

/**
 * F-156 SF-156-02 — modèle d'une carte plan affichée dans le dialog.
 * Source : pricing F-123 déployé prod (cf. landing.component.html).
 */
interface PlanTile {
  key: 'TEAM' | 'PRO';
  label: string;
  tagline: string;
  priceMonthly: number;
  priceLabel: string;
  seatsLabel: string;
  features: string[];
}

/**
 * F-156 SF-156-02 : dialog de création d'un workspace supplémentaire.
 *
 * <p>Diffère du dialog F-154 (onboarding initial) :
 * <ul>
 *   <li>Réservé OWNER TEAM/PRO (gate amont dans le shell : option « + Créer » invisible aux FREE/SOLO).</li>
 *   <li>Sélecteur de plan obligatoire (cards TEAM / PRO réutilisant le pricing F-123).</li>
 *   <li>Redirection vers Stripe Checkout sur succès (contrat figé SF-156-01).</li>
 * </ul>
 *
 * <p>Au succès, le navigateur quitte l'app vers `stripeCheckoutUrl` — pas de switch interne
 * (le switch se fera au retour Stripe via le query param `?workspace_created=success`).
 */
@Component({
  selector: 'app-workspace-create-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatDialogModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './workspace-create-dialog.component.html',
  styleUrl: './workspace-create-dialog.component.scss',
})
export class WorkspaceCreateDialogComponent {
  /**
   * Cards plan — alignées sur F-123 (landing/pricing), pas de hardcode dispersé.
   * Si le pricing évolue, mettre à jour ici (et la landing).
   */
  readonly plans: ReadonlyArray<PlanTile> = [
    {
      key: 'TEAM',
      label: 'Team',
      tagline: 'Pour les cabinets en croissance',
      priceMonthly: 219,
      priceLabel: '219 € / mois',
      seatsLabel: '3 utilisateurs inclus (cap 6)',
      features: ['40 dossiers actifs', '15 analyses / mois', '3 000 pages OCR / mois'],
    },
    {
      key: 'PRO',
      label: 'Pro',
      tagline: 'Pour les grands cabinets',
      priceMonthly: 429,
      priceLabel: '429 € / mois',
      seatsLabel: '5 utilisateurs inclus (extras autorisés)',
      features: ['Dossiers illimités', 'Analyses illimitées', '10 000 pages OCR / mois'],
    },
  ];

  /** Longueur maximale du nom de workspace (alignée sur la contrainte backend SF-156-01). */
  readonly maxNameLength = 100;

  name = '';
  selectedPlan = signal<'TEAM' | 'PRO' | null>(null);
  readonly submitting = signal(false);

  private readonly dialogRef =
    inject<MatDialogRef<WorkspaceCreateDialogComponent, { workspaceId: string } | null>>(MatDialogRef);
  private readonly workspaceService = inject(WorkspaceService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  /** CA3 — bouton « Créer + payer » disabled tant que nom + plan ne sont pas tous deux renseignés. */
  get canSubmit(): boolean {
    return (
      !this.submitting() &&
      this.name.trim().length > 0 &&
      this.name.trim().length <= this.maxNameLength &&
      this.selectedPlan() !== null
    );
  }

  selectPlan(plan: 'TEAM' | 'PRO'): void {
    if (this.submitting()) return;
    this.selectedPlan.set(plan);
  }

  cancel(): void {
    if (this.submitting()) return;
    this.dialogRef.close(null);
  }

  submit(): void {
    if (!this.canSubmit) return;
    const plan = this.selectedPlan()!;
    this.submitting.set(true);
    this.cdr.markForCheck();

    this.workspaceService.createWorkspaceWithPlan(this.name.trim(), plan).subscribe({
      next: (response) => {
        // CA5 — redirection complète du navigateur vers Stripe Checkout.
        // window.location.href (pas router.navigate) car URL externe.
        try {
          if (typeof window !== 'undefined' && response.stripeCheckoutUrl) {
            window.location.href = response.stripeCheckoutUrl;
          }
        } finally {
          this.dialogRef.close({ workspaceId: response.workspaceId });
        }
      },
      error: (err) => {
        this.submitting.set(false);
        const code: string | undefined = err?.error?.code;
        const status: number | undefined = err?.status;
        let message = 'Création impossible. Réessayez dans un instant.';
        if (code === 'PLAN_REQUIRED' || status === 403) {
          message = 'Plan TEAM ou PRO requis pour créer un workspace supplémentaire.';
        } else if (code === 'PLAN_INVALID' || status === 400) {
          message = 'Plan invalide, veuillez sélectionner TEAM ou PRO.';
        } else if (status === 502 || status === 503) {
          message = 'Service de paiement indisponible, réessayez dans quelques minutes.';
        }
        this.snackBar.open(message, 'Fermer', { duration: 6000, panelClass: ['snack-error'] });
        if (code === 'PLAN_REQUIRED' || status === 403) {
          // Cas inattendu (gate amont passée par erreur) — on ferme et on laisse le shell rediriger.
          this.dialogRef.close(null);
        }
        this.cdr.markForCheck();
      },
    });
  }
}
