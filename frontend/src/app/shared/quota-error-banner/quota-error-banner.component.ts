import { Component, Input, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { QuotaErrorStateService } from '../../core/services/quota-error-state.service';

interface QuotaCodeMapping {
  title: string;
  primaryLabel: string;
  primaryRoute: string;
  /** Si renseigné, action secondaire affichée (V8+ — désactivée tant que la feature backing n'existe pas). */
  secondaryLabel?: string;
  /** True quand l'action secondaire est encore gated (F-148 tokens / packs OCR). */
  secondaryDisabled?: boolean;
}

const DEFAULT_MAPPING: QuotaCodeMapping = {
  title: 'Limite atteinte',
  primaryLabel: 'Voir les plans',
  primaryRoute: '/workspace/billing',
};

const QUOTA_MAPPINGS: Record<string, QuotaCodeMapping> = {
  TOKEN_BUDGET_EXCEEDED: {
    title: 'Quota tokens mensuel atteint',
    primaryLabel: 'Upgrader le plan',
    primaryRoute: '/workspace/billing',
    secondaryLabel: 'Acheter des tokens',
    secondaryDisabled: true,
  },
  CASE_ANALYSIS_LIMIT_EXCEEDED: {
    title: "Limite d'analyses atteinte pour ce dossier",
    primaryLabel: 'Upgrader le plan',
    primaryRoute: '/workspace/billing',
  },
  CHAT_MESSAGE_LIMIT_EXCEEDED: {
    title: 'Limite de messages chat atteinte',
    primaryLabel: 'Upgrader le plan',
    primaryRoute: '/workspace/billing',
  },
  DOCUMENT_LIMIT_EXCEEDED: {
    title: 'Limite de documents atteinte',
    primaryLabel: 'Upgrader le plan',
    primaryRoute: '/workspace/billing',
  },
  CASE_FILE_OPEN_LIMIT_EXCEEDED: {
    title: 'Limite de dossiers actifs atteinte',
    primaryLabel: 'Upgrader le plan',
    primaryRoute: '/workspace/billing',
  },
  OCR_QUOTA_EXCEEDED: {
    title: 'Quota OCR mensuel atteint',
    primaryLabel: 'Upgrader le plan',
    primaryRoute: '/workspace/billing',
    secondaryLabel: 'Pack OCR ponctuel',
    secondaryDisabled: true,
  },
  SEAT_LIMIT_EXCEEDED: {
    title: 'Limite de sièges atteinte',
    primaryLabel: 'Voir les plans',
    primaryRoute: '/workspace/billing',
  },
  // SF-231-03 — Quota mensuel minutes vidéo (SOLO 5 min, TEAM 30 min, PRO 120 min).
  // Pas d'achat à la carte V1 (pas de bouton secondaire).
  VIDEO_QUOTA_EXCEEDED: {
    title: 'Quota vidéo mensuel atteint — passez au plan supérieur pour débloquer plus de minutes vidéo',
    primaryLabel: 'Upgrader le plan',
    primaryRoute: '/workspace/billing',
  },
};

/**
 * SF-171-02 — Bandeau persistant inline pour erreurs 402 (quota / budget tokens).
 *
 * Soit branché directement sur le state global `QuotaErrorStateService` (mode auto :
 * `<app-quota-error-banner />`), soit alimenté manuellement via inputs `code` + `message`
 * pour des contextes locaux. Tant que `QuotaErrorStateService.error()` est non null,
 * le bandeau reste affiché — pas d'auto-dismiss.
 */
@Component({
  selector: 'app-quota-error-banner',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './quota-error-banner.component.html',
  styleUrl: './quota-error-banner.component.scss',
})
export class QuotaErrorBannerComponent {
  private readonly router = inject(Router);
  private readonly quotaErrorState = inject(QuotaErrorStateService);

  /**
   * Optionnel : permet de forcer l'affichage avec un code donné (tests, contextes
   * isolés). Si non renseigné, le composant lit `quotaErrorState.error()`.
   */
  private readonly _codeOverride = signal<string | null | undefined>(undefined);
  private readonly _messageOverride = signal<string | undefined>(undefined);

  @Input() set code(value: string | null) {
    this._codeOverride.set(value);
  }

  @Input() set message(value: string) {
    this._messageOverride.set(value);
  }

  readonly currentCode = computed<string | null>(() => {
    const override = this._codeOverride();
    if (override !== undefined) return override;
    return this.quotaErrorState.error()?.code ?? null;
  });

  readonly currentMessage = computed<string>(() => {
    const override = this._messageOverride();
    if (override !== undefined) return override;
    return this.quotaErrorState.error()?.message ?? '';
  });

  readonly visible = computed<boolean>(() => {
    if (this._codeOverride() !== undefined || this._messageOverride() !== undefined) {
      return (this._messageOverride() ?? '').length > 0 || this._codeOverride() != null;
    }
    return this.quotaErrorState.error() !== null;
  });

  readonly mapping = computed<QuotaCodeMapping>(() => {
    const code = this.currentCode();
    if (!code) return DEFAULT_MAPPING;
    const mapping = QUOTA_MAPPINGS[code];
    if (!mapping) {
      console.warn(`[QuotaErrorBanner] Unknown quota code: ${code}`);
      return DEFAULT_MAPPING;
    }
    return mapping;
  });

  goToPrimaryAction(): void {
    this.router.navigate([this.mapping().primaryRoute]);
  }
}
