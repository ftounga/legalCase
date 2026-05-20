import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * F-156 SF-156-02 — page d'erreur affichée à un OWNER FREE/SOLO qui tente d'accéder
 * directement à la route `/workspaces/new` (CA11).
 *
 * <p>Invariant SF-156-00b §5 : le CTA « Découvrir les plans » ouvre `/pricing` en
 * **nouvelle page** (target="_blank") pour préserver le contexte de l'app.
 * Pas de modal, pas de toast — vraie page d'erreur explicite.
 *
 * <p>Standalone + OnPush (page sans état dynamique).
 */
@Component({
  selector: 'app-workspace-upgrade-required-page',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './workspace-upgrade-required-page.component.html',
  styleUrl: './workspace-upgrade-required-page.component.scss',
})
export class WorkspaceUpgradeRequiredPageComponent {
  /** URL cible du CTA — la landing pricing publique. Ouverte en nouvelle page. */
  readonly pricingUrl = '/#pricing';
}
