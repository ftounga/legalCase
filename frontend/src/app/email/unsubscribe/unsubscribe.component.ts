import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EmailSubscriptionService } from '../../core/services/email-subscription.service';

/** États affichables de la page publique de gestion d'abonnement email. */
type UnsubscribeState =
  | 'loading'        // appel backend en cours
  | 'unsubscribed'   // utilisateur désinscrit (confirmation)
  | 'resubscribed'   // utilisateur réabonné (confirmation)
  | 'no-token'       // token absent de l'URL
  | 'not-found'      // token inconnu (404)
  | 'error';         // échec réseau / 5xx

/**
 * Page publique `/unsubscribe` — désinscription / réabonnement aux emails
 * non-transactionnels à partir d'un token (F-248 / SF-248-02).
 *
 * Page autonome, hors `ShellComponent`, sans `authGuard` : le token fait foi.
 * Bidirectionnelle : on peut désinscrire puis réabonner depuis la même page.
 */
@Component({
  selector: 'app-unsubscribe',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './unsubscribe.component.html',
  styleUrl: './unsubscribe.component.scss'
})
export class UnsubscribeComponent implements OnInit {
  state = signal<UnsubscribeState>('loading');
  /** Une action (unsubscribe / resubscribe) est en cours — désactive les boutons. */
  actionInProgress = signal(false);

  private token = '';

  constructor(
    private route: ActivatedRoute,
    private emailSubscription: EmailSubscriptionService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.state.set('no-token');
      return;
    }
    this.loadStatus();
  }

  /** Charge l'état d'abonnement ; si l'utilisateur est encore abonné, le désinscrit. */
  loadStatus(): void {
    this.state.set('loading');
    this.emailSubscription.getStatus(this.token).subscribe({
      next: status => {
        if (status.optedOut) {
          this.state.set('unsubscribed');
        } else {
          this.unsubscribe();
        }
      },
      error: err => this.handleError(err)
    });
  }

  /** Déclenche la désinscription. */
  unsubscribe(): void {
    this.state.set('loading');
    this.actionInProgress.set(true);
    this.emailSubscription.unsubscribe(this.token).subscribe({
      next: () => {
        this.actionInProgress.set(false);
        this.state.set('unsubscribed');
      },
      error: err => {
        this.actionInProgress.set(false);
        this.handleError(err);
      }
    });
  }

  /** Déclenche le réabonnement. */
  resubscribe(): void {
    this.actionInProgress.set(true);
    this.emailSubscription.resubscribe(this.token).subscribe({
      next: () => {
        this.actionInProgress.set(false);
        this.state.set('resubscribed');
      },
      error: err => {
        this.actionInProgress.set(false);
        this.handleError(err);
      }
    });
  }

  /** Réessaie après un échec réseau, en repartant de la lecture du statut. */
  retry(): void {
    this.loadStatus();
  }

  private handleError(err: { status?: number }): void {
    this.state.set(err?.status === 404 ? 'not-found' : 'error');
  }
}
