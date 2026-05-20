import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Subscription, timer } from 'rxjs';
import { WorkspaceStateService } from '../../core/services/workspace-state.service';

/**
 * F-156 SF-156-02 — banderole d'état du workspace courant.
 *
 * <p>Affiche :
 * <ul>
 *   <li><strong>PENDING_PAYMENT</strong> : bandeau ambre persistant « Paiement en cours d'activation… »
 *       (cf. invariant SF-156-00b §3 : pas de modal, pas de toast).</li>
 *   <li><strong>ACTIVE (transition)</strong> : bandeau vert « Bienvenue dans &lt;nom&gt; — invitez vos premiers membres »
 *       auto-dismissé après 5 secondes (CA8).</li>
 *   <li>Autres statuts : pas de banderole.</li>
 * </ul>
 *
 * <p>OnPush + injection ChangeDetectorRef pour markForCheck() après le timer rxjs
 * (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-workspace-status-banner',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './workspace-status-banner.component.html',
  styleUrl: './workspace-status-banner.component.scss',
})
export class WorkspaceStatusBannerComponent implements OnInit {
  private readonly state = inject(WorkspaceStateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  /** Délai (ms) avant auto-dismiss de la bannière de bienvenue ACTIVE. */
  readonly welcomeDurationMs = 5000;

  /** Signal interne : la banderole "Bienvenue" est dismissée (auto après 5s ou clic close). */
  private readonly welcomeDismissed = signal(false);

  /** True dès qu'on a vu PENDING_PAYMENT pour le workspace courant — évite le welcome au cold-login ACTIVE. */
  private readonly sawPending = signal(false);

  /** Id du workspace courant connu — sert à réinitialiser les flags à chaque bascule. */
  private currentWorkspaceId: string | null = null;

  private welcomeSub?: Subscription;

  /**
   * variant() consomme uniquement des signaux → re-calcul automatique sur tout changement
   * de status / dismissed / sawPending.
   */
  readonly variant = computed<'pending' | 'welcome' | 'none'>(() => {
    const status = this.state.status();
    if (status === 'PENDING_PAYMENT') return 'pending';
    if (status === 'ACTIVE' && !this.welcomeDismissed() && this.sawPending()) {
      return 'welcome';
    }
    return 'none';
  });

  readonly workspaceName = computed(() => this.state.current()?.name ?? '');

  private readonly workspace$ = toObservable(this.state.current);

  ngOnInit(): void {
    this.workspace$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(ws => this.onWorkspaceChanged(ws));

    this.destroyRef.onDestroy(() => {
      this.welcomeSub?.unsubscribe();
    });
  }

  /** Permet à l'utilisateur de fermer manuellement la banderole de bienvenue avant 5s. */
  dismissWelcome(): void {
    this.welcomeDismissed.set(true);
    this.welcomeSub?.unsubscribe();
    this.welcomeSub = undefined;
    this.cdr.markForCheck();
  }

  private onWorkspaceChanged(ws: ReturnType<typeof this.state.current>): void {
    if (!ws) return;

    // Bascule de workspace → reset des flags locaux.
    if (this.currentWorkspaceId !== ws.id) {
      this.currentWorkspaceId = ws.id;
      this.sawPending.set(false);
      this.welcomeDismissed.set(false);
      this.welcomeSub?.unsubscribe();
      this.welcomeSub = undefined;
    }

    if (ws.status === 'PENDING_PAYMENT') {
      this.sawPending.set(true);
    } else if (ws.status === 'ACTIVE' && this.sawPending() && !this.welcomeDismissed()) {
      // Transition observée → arme l'auto-dismiss via rxjs timer (intercepté par fakeAsync).
      if (!this.welcomeSub) {
        this.welcomeSub = timer(this.welcomeDurationMs).subscribe(() => {
          this.welcomeDismissed.set(true);
          this.welcomeSub = undefined;
          this.cdr.markForCheck();
        });
      }
    }
  }
}
