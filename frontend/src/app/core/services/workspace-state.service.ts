import { Injectable, computed, signal } from '@angular/core';
import { Workspace, WorkspacePlan, WorkspaceStatus } from '../models/workspace.model';

/**
 * F-156 SF-156-02 — service applicatif exposant l'état du workspace courant
 * sous forme de signaux réactifs.
 *
 * <p>Utilisé par :
 * <ul>
 *   <li>{@link WorkspaceStatusBannerComponent} — pour afficher la banderole `PENDING_PAYMENT` / `ACTIVE`.</li>
 *   <li>Boutons d'écriture (créer dossier, inviter, uploader) — pour les désactiver quand `status == PENDING_PAYMENT`.</li>
 *   <li>{@link WorkspacePlanGuard} — pour gater la route `/workspaces/new` selon le plan OWNER courant.</li>
 *   <li>{@link ShellComponent} — pour afficher l'option « + Créer un workspace » seulement aux OWNER TEAM/PRO.</li>
 * </ul>
 *
 * <p>Source de vérité = le workspace courant chargé par {@link WorkspaceService#getCurrentWorkspace}.
 * Cette source d'écriture remplie par {@link ShellComponent} reste la seule autorisée — pas de duplication
 * de données (invariant SF-156-00 §5).
 */
@Injectable({ providedIn: 'root' })
export class WorkspaceStateService {
  private readonly _current = signal<Workspace | null>(null);

  /** Workspace courant — null pendant le chargement initial. */
  readonly current = this._current.asReadonly();

  /** `status` du workspace courant — `null` si pas encore chargé. */
  readonly status = computed<WorkspaceStatus | null>(() => this._current()?.status ?? null);

  /** `planCode` du workspace courant — `null` si pas encore chargé. */
  readonly planCode = computed<string | null>(() => this._current()?.planCode ?? null);

  /** true si le workspace courant est en attente d'activation Stripe (actions d'écriture désactivées). */
  readonly isPendingPayment = computed(() => this.status() === 'PENDING_PAYMENT');

  /** true si le workspace courant est actif (banderole bienvenue auto-dismiss). */
  readonly isActive = computed(() => this.status() === 'ACTIVE');

  /**
   * true si l'OWNER courant peut créer un workspace supplémentaire (plan ∈ {TEAM, PRO}).
   * - FREE / SOLO : false → option « + Créer » invisible dans le switcher (CA1/CA2).
   */
  readonly canCreateWorkspace = computed(() => {
    const plan = this.planCode();
    return plan === 'TEAM' || plan === 'PRO';
  });

  /** Met à jour le workspace courant — appelé par le shell après chargement / bascule. */
  setCurrent(workspace: Workspace | null): void {
    this._current.set(workspace);
  }

  /**
   * Helper utilitaire static pour vérifier qu'un plan autorise la création de workspace supplémentaire.
   * Utilisable hors composant (guards) sans injection.
   */
  static planAllowsAdditionalWorkspace(planCode: string | undefined | null): boolean {
    const normalized = (planCode ?? '').toUpperCase() as WorkspacePlan;
    return normalized === 'TEAM' || normalized === 'PRO';
  }
}
