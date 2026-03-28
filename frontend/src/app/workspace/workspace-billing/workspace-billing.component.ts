import { Component, Inject, OnDestroy, OnInit, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { interval, Subscription, switchMap, takeWhile } from 'rxjs';
import { WorkspaceService } from '../../core/services/workspace.service';
import { BillingService } from '../../core/services/billing.service';
import { Workspace } from '../../core/models/workspace.model';

@Component({
  selector: 'app-workspace-billing',
  standalone: true,
  imports: [MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './workspace-billing.component.html',
  styleUrl: './workspace-billing.component.scss'
})
export class WorkspaceBillingComponent implements OnInit, OnDestroy {
  workspace = signal<Workspace | null>(null);
  upgrading = signal<string | null>(null);
  private pollSub?: Subscription;

  readonly plans = [
    {
      code: 'FREE',
      label: 'Essai gratuit',
      price: '0 €',
      period: '14 jours',
      features: [
        { label: '2 dossiers actifs',              included: true },
        { label: '5 documents par dossier',        included: true },
        { label: '2 analyses de dossier',          included: true },
        { label: '10 messages chat / mois',        included: true },
        { label: '500K tokens / mois',             included: true },
        { label: 'Synthèse et questions IA',       included: true },
        { label: 'Re-synthèse enrichie',           included: false },
      ]
    },
    {
      code: 'SOLO',
      label: 'Solo',
      price: '59 €',
      period: '/ mois',
      features: [
        { label: '15 dossiers actifs',               included: true },
        { label: '15 documents par dossier',         included: true },
        { label: '8 analyses de dossier',            included: true },
        { label: '100 messages chat / mois',         included: true },
        { label: '6M tokens / mois',                 included: true },
        { label: 'Synthèse et questions IA',         included: true },
        { label: 'Re-synthèse enrichie (3/dossier)', included: true },
      ]
    },
    {
      code: 'TEAM',
      label: 'Team',
      price: '119 €',
      period: '/ mois',
      features: [
        { label: '40 dossiers actifs',               included: true },
        { label: '30 documents par dossier',         included: true },
        { label: '15 analyses de dossier',           included: true },
        { label: '300 messages chat / mois',         included: true },
        { label: '18M tokens / mois',                included: true },
        { label: 'Synthèse et questions IA',         included: true },
        { label: 'Re-synthèse enrichie (8/dossier)', included: true },
      ]
    },
    {
      code: 'PRO',
      label: 'Pro',
      price: '249 €',
      period: '/ mois',
      features: [
        { label: 'Dossiers illimités',               included: true },
        { label: '50 documents par dossier',         included: true },
        { label: 'Analyses illimitées',              included: true },
        { label: '1000 messages chat / mois',        included: true },
        { label: '60M tokens / mois',                included: true },
        { label: 'Synthèse et questions IA',         included: true },
        { label: 'Re-synthèse enrichie illimitée',   included: true },
      ]
    }
  ];

  constructor(
    private workspaceService: WorkspaceService,
    private billingService: BillingService,
    private snackBar: MatSnackBar,
    private route: ActivatedRoute,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnInit(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => this.workspace.set(ws),
      error: () => {}
    });

    this.route.queryParams.subscribe(params => {
      if (params['success'] === 'true') {
        this.snackBar.open('Paiement confirmé — mise à jour du plan en cours…', 'Fermer', {
          duration: 10000, panelClass: ['snack-success']
        });
        this.pollForPlanUpdate();
      } else if (params['canceled'] === 'true') {
        this.snackBar.open('Paiement annulé.', 'Fermer', {
          duration: 4000
        });
      }
    });
  }

  private pollForPlanUpdate(): void {
    let attempts = 0;
    this.pollSub = interval(2000).pipe(
      switchMap(() => this.workspaceService.getCurrentWorkspace()),
      takeWhile(ws => ws.planCode === 'FREE' && attempts++ < 15, true)
    ).subscribe({
      next: ws => {
        this.workspace.set(ws);
        if (ws.planCode !== 'FREE') {
          this.snackBar.open('Plan mis à jour avec succès !', 'Fermer', {
            duration: 5000, panelClass: ['snack-success']
          });
        }
      },
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  upgrade(planCode: string): void {
    this.upgrading.set(planCode);
    this.billingService.createCheckoutSession(planCode).subscribe({
      next: ({ checkoutUrl }) => {
        this.document.location.href = checkoutUrl;
      },
      error: () => {
        this.snackBar.open('Erreur lors de la redirection vers le paiement.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
        this.upgrading.set(null);
      }
    });
  }

  isCurrentPlan(planCode: string): boolean {
    return this.workspace()?.planCode === planCode;
  }

  isExpired(): boolean {
    const ws = this.workspace();
    if (!ws || ws.planCode !== 'FREE' || !ws.expiresAt) return false;
    return new Date(ws.expiresAt) < new Date();
  }
}
