import { Component, Inject, OnInit, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
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
export class WorkspaceBillingComponent implements OnInit {
  workspace = signal<Workspace | null>(null);
  upgrading = signal<string | null>(null);

  readonly plans = [
    {
      code: 'FREE',
      label: 'Essai gratuit',
      price: '0 €',
      period: '14 jours',
      features: ['1 dossier actif', '3 documents par dossier', 'Analyse IA complète', 'Questions interactives']
    },
    {
      code: 'STARTER',
      label: 'Starter',
      price: '49 €',
      period: '/ mois',
      features: ['3 dossiers actifs', 'Documents illimités', 'Analyse IA complète', 'Questions interactives']
    },
    {
      code: 'PRO',
      label: 'Pro',
      price: '129 €',
      period: '/ mois',
      features: ['20 dossiers actifs', 'Documents illimités', 'Analyse IA complète', 'Re-analyse enrichie', 'Questions interactives']
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
        this.snackBar.open('Paiement confirmé — votre plan sera mis à jour sous peu.', 'Fermer', {
          duration: 6000, panelClass: ['snack-success']
        });
      } else if (params['canceled'] === 'true') {
        this.snackBar.open('Paiement annulé.', 'Fermer', {
          duration: 4000
        });
      }
    });
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
