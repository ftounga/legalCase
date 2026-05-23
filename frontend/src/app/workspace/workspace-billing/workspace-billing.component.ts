import { Component, computed, Inject, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe, DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { interval, Subscription, switchMap, takeWhile } from 'rxjs';
import { RouterLink } from '@angular/router';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { BillingService } from '../../core/services/billing.service';
import { AuthService } from '../../core/services/auth.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { LegalConsentService, CURRENT_CONSENT_VERSION } from '../../core/services/legal-consent.service';
import { Workspace } from '../../core/models/workspace.model';
import { SeatsSummary } from '../../core/models/seats-summary.model';
import { SubscriptionState } from '../../core/models/subscription.model';
import { fadeInUp } from '../../shared/animations';
import { CancelSubscriptionDialogComponent } from './cancel-subscription-dialog.component';
import { PaymentTermsAcceptanceDialogComponent, PaymentTermsDialogData } from './payment-terms-acceptance-dialog/payment-terms-acceptance-dialog.component';
import { PromoCodeRedemptionService, mapPromoCodeError } from './promo-code-redemption.service';

@Component({
  selector: 'app-workspace-billing',
  standalone: true,
  imports: [
    RouterLink, DatePipe, FormsModule,
    MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './workspace-billing.component.html',
  styleUrl: './workspace-billing.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class WorkspaceBillingComponent implements OnInit, OnDestroy {
  workspace = signal<Workspace | null>(null);
  upgrading = signal<string | null>(null);
  buying = signal<string | null>(null);
  seatsSummary = signal<SeatsSummary | null>(null);

  // SF-247-02 : état d'abonnement + résiliation self-service.
  subscription = signal<SubscriptionState | null>(null);
  /** Rôle du membre courant dans le workspace courant — null tant que non résolu. */
  currentUserRole = signal<string | null>(null);
  /** true pendant un appel cancel/resume — désactive les boutons concernés. */
  cancelInFlight = signal(false);

  /** Une résiliation est programmée pour la fin de période en cours. */
  readonly cancellationScheduled = computed(
    () => this.subscription()?.cancelAtPeriodEnd === true
  );

  /** L'OWNER d'un plan payant sans résiliation déjà programmée peut résilier. */
  readonly canCancel = computed(() => {
    const sub = this.subscription();
    if (!sub) return false;
    return sub.planCode !== 'FREE'
      && !sub.cancelAtPeriodEnd
      && this.currentUserRole() === 'OWNER';
  });

  // SF-255-03 : bandeau d'essai + sous-bloc « Avantage adhérents ».
  /** Saisie utilisateur du code partenaire (normalisée trim + uppercase à l'envoi). */
  promoCodeInput = signal('');
  /** Désactive l'input + le bouton « Appliquer » pendant l'appel HTTP. */
  submittingPromo = signal(false);
  /**
   * Vrai si le workspace est en essai gratuit ACTIF (FREE + expiresAt > now).
   * Inv. 2 étape 0 bis : sous-bloc absent du DOM hors de cet état.
   * Inv. 4 étape 0 bis : après succès TRIAL_EXTENSION, le backend ré-applique
   * la contrainte unique partielle ; le sous-bloc reste rendu (l'essai est
   * encore actif) mais une nouvelle tentative renverra
   * WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION → message FR explicite.
   */
  readonly isTrialActive = computed(() => {
    const ws = this.workspace();
    if (!ws || ws.planCode !== 'FREE' || !ws.expiresAt) return false;
    return new Date(ws.expiresAt).getTime() > Date.now();
  });
  /** Garde de sortie après un succès de redemption pour faire disparaître le sous-bloc immédiatement. */
  private promoRedeemed = signal(false);
  /** Visibilité finale du sous-bloc « Avantage adhérents » (inv. 2 + inv. 4). */
  readonly showPromoCodeInput = computed(
    () => this.isTrialActive() && !this.promoRedeemed()
  );

  private pollSub?: Subscription;

  readonly tokenPacks = [
    { code: 'TOKENS_1M',  label: '1M tokens',  tokens: '1 000 000',  price: '9,90 €'  },
    { code: 'TOKENS_5M',  label: '5M tokens',  tokens: '5 000 000',  price: '39,90 €' },
    { code: 'TOKENS_20M', label: '20M tokens', tokens: '20 000 000', price: '129,90 €' },
  ];

  readonly ocrPacks = [
    { code: 'OCR_500',  label: 'Pack OCR 500',   pages: '500',    price: '19,00 €'  },
    { code: 'OCR_2000', label: 'Pack OCR 2 000', pages: '2 000',  price: '59,00 €'  },
    { code: 'OCR_8000', label: 'Pack OCR 8 000', pages: '8 000',  price: '199,00 €' },
  ];

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
        { label: '100 pages OCR / mois',           included: true },
        { label: 'Synthèse et questions complémentaires',       included: true },
        { label: 'Re-synthèse enrichie (1 essai)',   included: true },
      ]
    },
    {
      code: 'SOLO',
      label: 'Solo',
      // SF-123-01 : grille V2 — +40 € pour aligner sur le marché (Jimini ~50-100 €, Doctrine/Predictice 100-200 €).
      price: '99 €',
      period: '/ mois',
      features: [
        { label: '1 utilisateur inclus',             included: true },
        { label: '15 dossiers actifs',               included: true },
        { label: '15 documents par dossier',         included: true },
        { label: '8 analyses de dossier',            included: true },
        { label: '100 messages chat / mois',         included: true },
        { label: '6M tokens / mois',                 included: true },
        { label: '800 pages OCR / mois',             included: true },
        { label: 'Synthèse et questions complémentaires',         included: true },
        { label: 'Re-synthèse enrichie (3/dossier)', included: true },
      ]
    },
    {
      code: 'TEAM',
      label: 'Team',
      // SF-123-01 : TEAM V2 — 3 users inclus, +59 €/user au-delà prévu en SF-123-02 (quota extensible jusqu'à 6).
      price: '219 €',
      period: '/ mois',
      features: [
        { label: '3 utilisateurs inclus (extensibles)', included: true },
        { label: '40 dossiers actifs',               included: true },
        { label: '30 documents par dossier',         included: true },
        { label: '15 analyses de dossier',           included: true },
        { label: '300 messages chat / mois',         included: true },
        { label: '18M tokens / mois',                included: true },
        { label: '3 000 pages OCR / mois',           included: true },
        { label: 'Synthèse et questions complémentaires',         included: true },
        { label: 'Re-synthèse enrichie (8/dossier)', included: true },
      ]
    },
    {
      code: 'PRO',
      label: 'Pro',
      // SF-123-01 : PRO V2 — 5 users inclus, +79 €/user supp prévu en SF-123-02.
      price: '429 €',
      period: '/ mois',
      features: [
        { label: '5 utilisateurs inclus (extensibles)', included: true },
        { label: 'Dossiers illimités',               included: true },
        { label: '50 documents par dossier',         included: true },
        { label: 'Analyses illimitées',              included: true },
        { label: '1000 messages chat / mois',        included: true },
        { label: '60M tokens / mois',                included: true },
        { label: '10 000 pages OCR / mois',          included: true },
        { label: 'Synthèse et questions complémentaires',         included: true },
        { label: 'Re-synthèse enrichie illimitée',   included: true },
      ]
    }
  ];

  constructor(
    private workspaceService: WorkspaceService,
    private memberService: WorkspaceMemberService,
    private billingService: BillingService,
    private auth: AuthService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    @Inject(DOCUMENT) private document: Document,
    private analyticsService: AnalyticsService,
    private legalConsentService: LegalConsentService,
    // SF-255-03 : service dédié à la redemption d'un code partenaire.
    private promoCodeRedemptionService: PromoCodeRedemptionService
  ) {}

  ngOnInit(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => this.workspace.set(ws),
      error: () => {}
    });

    // SF-123-03 : résumé seats — silencieux en cas d'erreur (non-critique pour la page)
    this.billingService.getSeatsSummary().subscribe({
      next: s => this.seatsSummary.set(s),
      error: () => this.seatsSummary.set(null)
    });

    // SF-247-02 : état d'abonnement — fail-safe : en cas d'échec, ni bandeau ni
    // section résiliation, l'écran billing reste fonctionnel.
    this.billingService.getSubscription().subscribe({
      next: s => this.subscription.set(s),
      error: () => this.subscription.set(null)
    });

    // SF-247-02 : rôle du membre courant — lu depuis la liste des membres du
    // workspace courant (pas de nouvel endpoint). Silencieux en cas d'échec :
    // sans rôle confirmé, la section « Résilier » n'est pas rendue (fail-safe).
    this.memberService.getMembers().subscribe({
      next: members => {
        const myId = this.auth.currentUser()?.id;
        const me = members.find(m => m.userId === myId);
        this.currentUserRole.set(me?.memberRole ?? null);
      },
      error: () => this.currentUserRole.set(null)
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
      } else if (params['topup'] === 'success') {
        const isOcr = params['topup_kind'] === 'ocr';
        this.snackBar.open(
          isOcr ? 'Pages OCR ajoutées à votre quota !' : 'Tokens ajoutés à votre compte !',
          'Fermer',
          { duration: 6000, panelClass: ['snack-success'] }
        );
      } else if (params['topup'] === 'canceled') {
        const isOcr = params['topup_kind'] === 'ocr';
        this.snackBar.open(
          isOcr ? 'Achat de pages OCR annulé.' : 'Achat de tokens annulé.',
          'Fermer',
          { duration: 4000 }
        );
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
    this.analyticsService.trackEvent('upgrade_clicked', { plan: planCode });
    const plan = this.plans.find(p => p.code === planCode);
    const dialogData: PaymentTermsDialogData = {
      planLabel: plan?.label ?? planCode,
      price: plan?.price ?? '',
      type: 'SUBSCRIPTION'
    };
    const ref = this.dialog.open<PaymentTermsAcceptanceDialogComponent, PaymentTermsDialogData, boolean>(
      PaymentTermsAcceptanceDialogComponent,
      { data: dialogData, autoFocus: 'dialog' }
    );
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.upgrading.set(planCode);
      this.legalConsentService.acceptConsent({
        consentTypes: ['PAYMENT_TERMS'],
        version: CURRENT_CONSENT_VERSION
      }).subscribe({
        next: () => {
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
        },
        error: () => {
          this.snackBar.open(
            'Impossible d\'enregistrer votre acceptation, réessayez.',
            'Fermer',
            { duration: 4000, panelClass: ['snack-error'] }
          );
          this.upgrading.set(null);
        }
      });
    });
  }

  buyTopup(packCode: string): void {
    const tokenPack = this.tokenPacks.find(p => p.code === packCode);
    const ocrPack = this.ocrPacks.find(p => p.code === packCode);
    const pack = tokenPack ?? ocrPack;
    const dialogData: PaymentTermsDialogData = {
      planLabel: pack?.label ?? packCode,
      price: pack?.price ?? '',
      type: 'TOPUP'
    };
    const ref = this.dialog.open<PaymentTermsAcceptanceDialogComponent, PaymentTermsDialogData, boolean>(
      PaymentTermsAcceptanceDialogComponent,
      { data: dialogData, autoFocus: 'dialog' }
    );
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.buying.set(packCode);
      this.legalConsentService.acceptConsent({
        consentTypes: ['PAYMENT_TERMS'],
        version: CURRENT_CONSENT_VERSION
      }).subscribe({
        next: () => {
          this.billingService.createTopupSession(packCode).subscribe({
            next: ({ checkoutUrl }) => {
              this.document.location.href = checkoutUrl;
            },
            error: () => {
              this.snackBar.open('Erreur lors de la redirection vers le paiement.', 'Fermer', {
                duration: 4000, panelClass: ['snack-error']
              });
              this.buying.set(null);
            }
          });
        },
        error: () => {
          this.snackBar.open(
            'Impossible d\'enregistrer votre acceptation, réessayez.',
            'Fermer',
            { duration: 4000, panelClass: ['snack-error'] }
          );
          this.buying.set(null);
        }
      });
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

  /** Date d'expiration courante de l'essai (pour affichage banner). */
  trialExpiresAt(): string | null {
    return this.workspace()?.expiresAt ?? null;
  }

  // SF-255-03 — application d'un code partenaire.

  /**
   * Soumission du code saisi :
   *  - normalise (trim + uppercase),
   *  - bloque double-click via {@code submittingPromo},
   *  - succès : maj optimiste de {@code workspace.expiresAt}, toast 3s,
   *    {@code promoRedeemed = true} → sous-bloc disparaît automatiquement.
   *  - erreur : message FR depuis {@code mapPromoCodeError}, code conservé pour retry.
   */
  applyPromoCode(): void {
    if (this.submittingPromo()) return;
    const ws = this.workspace();
    if (!ws) return;
    const code = this.promoCodeInput().trim().toUpperCase();
    if (!code) {
      this.snackBar.open('Veuillez saisir un code partenaire.', 'Fermer', { duration: 3000 });
      return;
    }
    this.submittingPromo.set(true);
    this.analyticsService.trackEvent('promo_code_submitted', { workspaceId: ws.id });
    this.promoCodeRedemptionService.redeem(ws.id, code).subscribe({
      next: response => {
        // Maj optimiste : la nouvelle date d'expiration vient directement du backend.
        this.workspace.set({ ...ws, expiresAt: response.newExpiresAt });
        this.promoRedeemed.set(true);
        this.promoCodeInput.set('');
        this.submittingPromo.set(false);
        this.snackBar.open(
          `Avantage adhérents ${response.partnerLabel} appliqué — essai prolongé de ${response.addedDays} jours.`,
          'Fermer',
          { duration: 3000, panelClass: ['snack-success'] }
        );
        // Refresh autoritaire depuis le backend pour cohérence (workspace + subscription).
        this.workspaceService.getCurrentWorkspace().subscribe({
          next: fresh => this.workspace.set(fresh),
          error: () => {}
        });
      },
      error: err => {
        this.submittingPromo.set(false);
        // Code conservé dans l'input pour retry — pas de reset de promoCodeInput.
        this.snackBar.open(mapPromoCodeError(err), 'Fermer', {
          duration: 4000,
          panelClass: ['snack-error']
        });
      }
    });
  }

  // SF-123-03 : helpers section Utilisateurs actifs
  seatsCostEuros(s: SeatsSummary): number {
    return Math.round(s.totalMonthlyCostCents / 100);
  }

  extraSeats(s: SeatsSummary): number {
    return Math.max(0, s.seatCount - s.includedSeats);
  }

  seatsPlanLabel(plan: string): string {
    const l: Record<string, string> = { FREE: 'Free', SOLO: 'Solo', TEAM: 'Team', PRO: 'Pro' };
    return l[plan] ?? plan;
  }

  // SF-247-02 : résiliation d'abonnement self-service.

  /** Date de fin de période à afficher dans le bandeau « résiliation programmée ». */
  cancellationEndDate(): string | null {
    return this.subscription()?.currentPeriodEnd ?? null;
  }

  /** Ouvre le dialog de confirmation ; au confirm, déclenche la résiliation. */
  openCancelDialog(): void {
    if (!this.canCancel()) return;
    const ref = this.dialog.open<CancelSubscriptionDialogComponent, { currentPeriodEnd: string | null }, boolean>(
      CancelSubscriptionDialogComponent,
      { data: { currentPeriodEnd: this.subscription()?.currentPeriodEnd ?? null }, autoFocus: 'dialog' }
    );
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) this.confirmCancel();
    });
  }

  /** Appelle le backend pour programmer la résiliation. */
  confirmCancel(): void {
    if (this.cancelInFlight()) return;
    this.cancelInFlight.set(true);
    this.analyticsService.trackEvent('subscription_cancel_clicked', {
      plan: this.subscription()?.planCode ?? ''
    });
    this.billingService.cancelSubscription().subscribe({
      next: state => {
        this.subscription.set(state);
        this.cancelInFlight.set(false);
        this.snackBar.open(
          'Résiliation programmée. Vous gardez l\'accès jusqu\'à la fin de la période.',
          'Fermer', { duration: 6000, panelClass: ['snack-success'] }
        );
      },
      error: err => {
        this.cancelInFlight.set(false);
        this.snackBar.open(
          err?.error?.message ?? 'La résiliation a échoué, réessayez.',
          'Fermer', { duration: 6000, panelClass: ['snack-error'] }
        );
      }
    });
  }

  /** Annule une résiliation programmée. */
  resume(): void {
    if (this.cancelInFlight()) return;
    this.cancelInFlight.set(true);
    this.billingService.resumeSubscription().subscribe({
      next: state => {
        this.subscription.set(state);
        this.cancelInFlight.set(false);
        this.snackBar.open(
          'Abonnement réactivé.', 'Fermer',
          { duration: 5000, panelClass: ['snack-success'] }
        );
      },
      error: err => {
        this.cancelInFlight.set(false);
        this.snackBar.open(
          err?.error?.message ?? 'La réactivation a échoué, réessayez.',
          'Fermer', { duration: 6000, panelClass: ['snack-error'] }
        );
      }
    });
  }
}
