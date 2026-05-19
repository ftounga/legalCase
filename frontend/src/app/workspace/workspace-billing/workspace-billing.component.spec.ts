import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WorkspaceBillingComponent } from './workspace-billing.component';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { BillingService } from '../../core/services/billing.service';
import { AuthService } from '../../core/services/auth.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { LegalConsentService } from '../../core/services/legal-consent.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { of, NEVER, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Workspace } from '../../core/models/workspace.model';
import { SubscriptionState } from '../../core/models/subscription.model';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { PaymentTermsAcceptanceDialogComponent } from './payment-terms-acceptance-dialog/payment-terms-acceptance-dialog.component';

const mockWorkspace: Workspace = {
  id: 'ws1', name: 'Test', slug: 'test', planCode: 'SOLO', status: 'ACTIVE'
};

const ownerMember: WorkspaceMember = {
  userId: 'u1', email: 'owner@test.fr', firstName: 'O', lastName: 'Wner',
  memberRole: 'OWNER', createdAt: '2026-01-01T00:00:00Z'
};
const plainMember: WorkspaceMember = {
  userId: 'u1', email: 'member@test.fr', firstName: 'M', lastName: 'Ember',
  memberRole: 'MEMBER', createdAt: '2026-01-01T00:00:00Z'
};

const paidSubscription: SubscriptionState = {
  planCode: 'SOLO', status: 'ACTIVE', cancelAtPeriodEnd: false, currentPeriodEnd: null
};
const scheduledSubscription: SubscriptionState = {
  planCode: 'SOLO', status: 'ACTIVE', cancelAtPeriodEnd: true,
  currentPeriodEnd: '2026-06-30T00:00:00Z'
};
const freeSubscription: SubscriptionState = {
  planCode: 'FREE', status: 'ACTIVE', cancelAtPeriodEnd: false, currentPeriodEnd: null
};

interface BillingMockOptions {
  subscription?: SubscriptionState | 'error';
  members?: WorkspaceMember[] | 'error';
}

/**
 * Construit le composant avec des spies configurables. Couvre les besoins SF-247-02
 * tout en préservant les suites existantes (plans / topup / seats).
 */
async function buildComponent(opts: BillingMockOptions = {}): Promise<{
  fixture: ComponentFixture<WorkspaceBillingComponent>;
  component: WorkspaceBillingComponent;
  billingServiceSpy: jest.Mocked<BillingService>;
  legalConsentServiceSpy: jest.Mocked<LegalConsentService>;
  snackBarSpy: jest.Mocked<MatSnackBar>;
  dialogSpy: jest.Mocked<MatDialog>;
}> {
  const workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
  const memberServiceSpy = jasmine.createSpyObj('WorkspaceMemberService', ['getMembers']);
  const billingServiceSpy = jasmine.createSpyObj('BillingService', [
    'createCheckoutSession', 'createTopupSession', 'getSeatsSummary',
    'getSubscription', 'cancelSubscription', 'resumeSubscription'
  ]);
  const legalConsentServiceSpy = jasmine.createSpyObj('LegalConsentService', ['acceptConsent']);
  const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
  const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

  workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));
  billingServiceSpy.getSeatsSummary.mockReturnValue(of({
    planCode: 'SOLO', seatCount: 1, includedSeats: 1, maxSeats: 1,
    extraSeatPriceCents: 0, baseMonthlyCostCents: 9900, totalMonthlyCostCents: 9900
  }));

  const sub = opts.subscription ?? paidSubscription;
  billingServiceSpy.getSubscription.mockReturnValue(
    sub === 'error' ? throwError(() => new Error('boom')) : of(sub)
  );

  const members = opts.members ?? [ownerMember];
  memberServiceSpy.getMembers.mockReturnValue(
    members === 'error' ? throwError(() => new Error('boom')) : of(members)
  );

  await TestBed.configureTestingModule({
    imports: [WorkspaceBillingComponent, NoopAnimationsModule],
    providers: [
      { provide: WorkspaceService, useValue: workspaceServiceSpy },
      { provide: WorkspaceMemberService, useValue: memberServiceSpy },
      { provide: BillingService, useValue: billingServiceSpy },
      { provide: LegalConsentService, useValue: legalConsentServiceSpy },
      { provide: AuthService, useValue: { currentUser: signal({ id: 'u1', email: 'owner@test.fr' }) } },
      { provide: MatSnackBar, useValue: snackBarSpy },
      { provide: MatDialog, useValue: dialogSpy },
      { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
      { provide: AnalyticsService, useValue: jasmine.createSpyObj('AnalyticsService', ['trackEvent']) }
    ]
  }).compileComponents();

  const fixture = TestBed.createComponent(WorkspaceBillingComponent);
  const component = fixture.componentInstance;
  fixture.detectChanges();
  return { fixture, component, billingServiceSpy, legalConsentServiceSpy, snackBarSpy, dialogSpy };
}

describe('WorkspaceBillingComponent', () => {
  let component: WorkspaceBillingComponent;
  let fixture: ComponentFixture<WorkspaceBillingComponent>;
  let billingServiceSpy: jest.Mocked<BillingService>;
  let dialogSpy: jest.Mocked<MatDialog>;

  beforeEach(async () => {
    ({ component, fixture, billingServiceSpy, dialogSpy } = await buildComponent());
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('affiche les 4 plans', () => {
    expect(component.plans.length).toBe(4);
    expect(component.plans.map(p => p.code)).toEqual(['FREE', 'SOLO', 'TEAM', 'PRO']);
  });

  // SF-123-01 : grille V2 alignée marché (SOLO 99 / TEAM 219 / PRO 429)
  it('plan SOLO à 99 €', () => {
    const solo = component.plans.find(p => p.code === 'SOLO')!;
    expect(solo.price).toBe('99 €');
    expect(solo.label).toBe('Solo');
  });

  it('plan TEAM à 219 €', () => {
    const team = component.plans.find(p => p.code === 'TEAM')!;
    expect(team.price).toBe('219 €');
    expect(team.label).toBe('Team');
  });

  it('plan PRO à 429 €', () => {
    const pro = component.plans.find(p => p.code === 'PRO')!;
    expect(pro.price).toBe('429 €');
  });

  it('chaque plan affiche son quota OCR mensuel', () => {
    const expected: Record<string, string> = {
      FREE: '100 pages OCR / mois',
      SOLO: '800 pages OCR / mois',
      TEAM: '3 000 pages OCR / mois',
      PRO:  '10 000 pages OCR / mois',
    };
    for (const plan of component.plans) {
      const ocr = plan.features.find(f => /pages OCR/.test(f.label));
      expect(ocr?.label).toBe(expected[plan.code]);
      expect(ocr?.included).toBe(true);
    }
  });

  it('SOLO est le plan featured (Recommandé)', () => {
    const solo = component.plans.find(p => p.code === 'SOLO')!;
    expect(solo.code).toBe('SOLO');
  });

  it('re-synthèse enrichie incluse sur SOLO/TEAM/PRO', () => {
    ['SOLO', 'TEAM', 'PRO'].forEach(code => {
      const plan = component.plans.find(p => p.code === code)!;
      const enriched = plan.features.find(f => f.label.toLowerCase().includes('re-synthèse'))!;
      expect(enriched.included).toBe(true);
    });
  });

  it('re-synthèse enrichie disponible (1 essai) sur FREE', () => {
    const free = component.plans.find(p => p.code === 'FREE')!;
    const enriched = free.features.find(f => f.label.toLowerCase().includes('re-synthèse'))!;
    expect(enriched.included).toBe(true);
    expect(enriched.label).toContain('1 essai');
  });

  it('isCurrentPlan — SOLO pour workspace SOLO', () => {
    expect(component.isCurrentPlan('SOLO')).toBe(true);
    expect(component.isCurrentPlan('PRO')).toBe(false);
    expect(component.isCurrentPlan('FREE')).toBe(false);
  });

  it('upgrade — ouvre PaymentTermsAcceptanceDialogComponent (WB-01)', () => {
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(false) } as never);

    component.upgrade('PRO');

    expect(dialogSpy.open).toHaveBeenCalledWith(
      PaymentTermsAcceptanceDialogComponent,
      expect.objectContaining({ data: expect.objectContaining({ planLabel: 'Pro', type: 'SUBSCRIPTION' }) })
    );
  });

  it('upgrade — trackEvent upgrade_clicked avec le plan', () => {
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(false) } as never);
    const analyticsService = TestBed.inject(AnalyticsService) as jest.Mocked<AnalyticsService>;

    component.upgrade('SOLO');

    expect(analyticsService.trackEvent).toHaveBeenCalledWith('upgrade_clicked', { plan: 'SOLO' });
  });

  it('affiche 3 packs tokens', () => {
    expect(component.tokenPacks.length).toBe(3);
    expect(component.tokenPacks.map(p => p.code)).toEqual(['TOKENS_1M', 'TOKENS_5M', 'TOKENS_20M']);
  });

  it('affiche 3 packs OCR', () => {
    expect(component.ocrPacks.length).toBe(3);
    expect(component.ocrPacks.map(p => p.code)).toEqual(['OCR_500', 'OCR_2000', 'OCR_8000']);
  });

  it('buyTopup — ouvre PaymentTermsAcceptanceDialogComponent (TOPUP)', () => {
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(false) } as never);

    component.buyTopup('TOKENS_1M');

    expect(dialogSpy.open).toHaveBeenCalledWith(
      PaymentTermsAcceptanceDialogComponent,
      expect.objectContaining({ data: expect.objectContaining({ type: 'TOPUP' }) })
    );
  });

});

describe('WorkspaceBillingComponent — topup query params', () => {
  const setupWith = async (queryParams: Record<string, string>) => {
    const workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    const memberServiceSpy = jasmine.createSpyObj('WorkspaceMemberService', ['getMembers']);
    const billingServiceSpy = jasmine.createSpyObj('BillingService', [
      'createCheckoutSession', 'createTopupSession', 'getSeatsSummary',
      'getSubscription', 'cancelSubscription', 'resumeSubscription'
    ]);
    const legalConsentServiceSpy = jasmine.createSpyObj('LegalConsentService', ['acceptConsent']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));
    memberServiceSpy.getMembers.mockReturnValue(of([ownerMember]));
    billingServiceSpy.getSubscription.mockReturnValue(of(paidSubscription));
    billingServiceSpy.getSeatsSummary.mockReturnValue(of({
      planCode: 'SOLO', seatCount: 1, includedSeats: 1, maxSeats: 1,
      extraSeatPriceCents: 0, baseMonthlyCostCents: 9900, totalMonthlyCostCents: 9900
    }));

    await TestBed.configureTestingModule({
      imports: [WorkspaceBillingComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: WorkspaceMemberService, useValue: memberServiceSpy },
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: LegalConsentService, useValue: legalConsentServiceSpy },
        { provide: AuthService, useValue: { currentUser: signal({ id: 'u1', email: 'owner@test.fr' }) } },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) },
        { provide: ActivatedRoute, useValue: { queryParams: of(queryParams) } },
        { provide: AnalyticsService, useValue: jasmine.createSpyObj('AnalyticsService', ['trackEvent']) }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(WorkspaceBillingComponent);
    fixture.detectChanges();
    return snackBarSpy;
  };

  it('?topup=success — affiche le snackbar de confirmation', async () => {
    const snackBarSpy = await setupWith({ topup: 'success' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Tokens ajoutés à votre compte !', 'Fermer', expect.objectContaining({ duration: 6000 })
    );
  });

  it('?topup=canceled — affiche le snackbar d\'annulation', async () => {
    const snackBarSpy = await setupWith({ topup: 'canceled' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Achat de tokens annulé.', 'Fermer', expect.objectContaining({ duration: 4000 })
    );
  });

  it('?topup=success&topup_kind=ocr — affiche le snackbar OCR', async () => {
    const snackBarSpy = await setupWith({ topup: 'success', topup_kind: 'ocr' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Pages OCR ajoutées à votre quota !', 'Fermer', expect.objectContaining({ duration: 6000 })
    );
  });

  it('?topup=canceled&topup_kind=ocr — affiche le snackbar d\'annulation OCR', async () => {
    const snackBarSpy = await setupWith({ topup: 'canceled', topup_kind: 'ocr' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Achat de pages OCR annulé.', 'Fermer', expect.objectContaining({ duration: 4000 })
    );
  });
});

describe('WorkspaceBillingComponent — SF-123-03 seats section', () => {
  let component: WorkspaceBillingComponent;
  let fixture: ComponentFixture<WorkspaceBillingComponent>;

  beforeEach(async () => {
    const workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    const memberServiceSpy = jasmine.createSpyObj('WorkspaceMemberService', ['getMembers']);
    const billingServiceSpy = jasmine.createSpyObj('BillingService', [
      'createCheckoutSession', 'createTopupSession', 'getSeatsSummary',
      'getSubscription', 'cancelSubscription', 'resumeSubscription'
    ]);
    const legalConsentServiceSpy = jasmine.createSpyObj('LegalConsentService', ['acceptConsent']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));
    memberServiceSpy.getMembers.mockReturnValue(of([ownerMember]));
    billingServiceSpy.getSubscription.mockReturnValue(of(paidSubscription));
    billingServiceSpy.getSeatsSummary.mockReturnValue(of({
      planCode: 'TEAM', seatCount: 4, includedSeats: 3, maxSeats: 6,
      extraSeatPriceCents: 5900, baseMonthlyCostCents: 21900, totalMonthlyCostCents: 27800
    }));

    await TestBed.configureTestingModule({
      imports: [WorkspaceBillingComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: WorkspaceMemberService, useValue: memberServiceSpy },
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: LegalConsentService, useValue: legalConsentServiceSpy },
        { provide: AuthService, useValue: { currentUser: signal({ id: 'u1', email: 'owner@test.fr' }) } },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
        { provide: AnalyticsService, useValue: jasmine.createSpyObj('AnalyticsService', ['trackEvent']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceBillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge le résumé seats au init', () => {
    expect(component.seatsSummary()?.planCode).toBe('TEAM');
  });

  it('seatsCostEuros — 27800 cents → 278 €', () => {
    const s = component.seatsSummary()!;
    expect(component.seatsCostEuros(s)).toBe(278);
  });

  it('extraSeats — seatCount - includedSeats', () => {
    const s = component.seatsSummary()!;
    expect(component.extraSeats(s)).toBe(1);
  });

  it('section Utilisateurs actifs rendue dans le DOM', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Utilisateurs actifs');
    expect(html.textContent).toContain('Team');
    expect(html.textContent).toContain('278 €');
  });
});

// SF-247-02 — résiliation d'abonnement self-service
describe('WorkspaceBillingComponent — SF-247-02 résiliation', () => {

  it('section « Résilier » visible si payant + OWNER + non programmé', async () => {
    const { component, fixture } = await buildComponent({
      subscription: paidSubscription, members: [ownerMember]
    });
    expect(component.canCancel()).toBe(true);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.cancel-section')).toBeTruthy();
    expect(html.textContent).toContain('Résilier l\'abonnement');
  });

  it('section « Résilier » masquée si non-OWNER', async () => {
    const { component, fixture } = await buildComponent({
      subscription: paidSubscription, members: [plainMember]
    });
    expect(component.currentUserRole()).toBe('MEMBER');
    expect(component.canCancel()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).querySelector('.cancel-section')).toBeNull();
  });

  it('section « Résilier » masquée si plan FREE', async () => {
    const { component, fixture } = await buildComponent({
      subscription: freeSubscription, members: [ownerMember]
    });
    expect(component.canCancel()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).querySelector('.cancel-section')).toBeNull();
  });

  it('section « Résilier » masquée si résiliation déjà programmée', async () => {
    const { component } = await buildComponent({
      subscription: scheduledSubscription, members: [ownerMember]
    });
    expect(component.canCancel()).toBe(false);
  });

  it('bandeau « résiliation programmée » + date affichés si cancelAtPeriodEnd', async () => {
    const { component, fixture } = await buildComponent({
      subscription: scheduledSubscription, members: [ownerMember]
    });
    expect(component.cancellationScheduled()).toBe(true);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.cancellation-banner')).toBeTruthy();
    expect(html.textContent).toContain('Résiliation programmée');
    // 2026-06-30 formaté en longDate FR
    expect(html.textContent).toMatch(/2026/);
  });

  it('aucun bandeau ni section si GET subscription échoue (fail-safe)', async () => {
    const { component, fixture } = await buildComponent({
      subscription: 'error', members: [ownerMember]
    });
    expect(component.subscription()).toBeNull();
    expect(component.canCancel()).toBe(false);
    expect(component.cancellationScheduled()).toBe(false);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.cancel-section')).toBeNull();
    expect(html.querySelector('.cancellation-banner')).toBeNull();
  });

  it('openCancelDialog — n\'appelle pas le backend si le dialog est annulé', async () => {
    const { component, dialogSpy, billingServiceSpy } = await buildComponent();
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(false) } as never);

    component.openCancelDialog();

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(billingServiceSpy.cancelSubscription).not.toHaveBeenCalled();
  });

  it('openCancelDialog — confirme → appelle cancelSubscription', async () => {
    const { component, dialogSpy, billingServiceSpy } = await buildComponent();
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(true) } as never);
    billingServiceSpy.cancelSubscription.mockReturnValue(of(scheduledSubscription));

    component.openCancelDialog();

    expect(billingServiceSpy.cancelSubscription).toHaveBeenCalled();
  });

  it('confirmCancel — succès : bascule l\'affichage et notifie', async () => {
    const { component, billingServiceSpy, snackBarSpy } = await buildComponent();
    billingServiceSpy.cancelSubscription.mockReturnValue(of(scheduledSubscription));

    component.confirmCancel();

    expect(component.subscription()?.cancelAtPeriodEnd).toBe(true);
    expect(component.cancellationScheduled()).toBe(true);
    expect(component.canCancel()).toBe(false);
    expect(component.cancelInFlight()).toBe(false);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      expect.stringContaining('Résiliation programmée'), 'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] })
    );
  });

  it('confirmCancel — erreur backend : snackbar avec error.error.message', async () => {
    const { component, billingServiceSpy, snackBarSpy } = await buildComponent();
    billingServiceSpy.cancelSubscription.mockReturnValue(
      throwError(() => ({ error: { message: 'Seul le propriétaire peut résilier l\'abonnement' } }))
    );

    component.confirmCancel();

    expect(component.cancelInFlight()).toBe(false);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Seul le propriétaire peut résilier l\'abonnement', 'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  it('resume — succès : masque le bandeau et notifie', async () => {
    const { component, billingServiceSpy, snackBarSpy } = await buildComponent({
      subscription: scheduledSubscription, members: [ownerMember]
    });
    billingServiceSpy.resumeSubscription.mockReturnValue(of(paidSubscription));

    component.resume();

    expect(component.cancellationScheduled()).toBe(false);
    expect(component.cancelInFlight()).toBe(false);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Abonnement réactivé.', 'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] })
    );
  });

  it('resume — erreur backend : snackbar avec le message renvoyé', async () => {
    const { component, billingServiceSpy, snackBarSpy } = await buildComponent({
      subscription: scheduledSubscription, members: [ownerMember]
    });
    billingServiceSpy.resumeSubscription.mockReturnValue(
      throwError(() => ({ error: { message: 'Aucune résiliation programmée' } }))
    );

    component.resume();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Aucune résiliation programmée', 'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });
});

// SF-240-03 — modale d'acceptation CGV avant paiement Stripe
describe('WorkspaceBillingComponent — SF-240-03 modale consent paiement', () => {

  // WB-01 : clic sur "Passer au plan" → MatDialog.open est appelé avec PaymentTermsAcceptanceDialogComponent
  it('WB-01 — upgrade ouvre PaymentTermsAcceptanceDialogComponent', async () => {
    const { component, dialogSpy } = await buildComponent();
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(false) } as never);

    component.upgrade('SOLO');

    expect(dialogSpy.open).toHaveBeenCalledWith(
      PaymentTermsAcceptanceDialogComponent,
      expect.objectContaining({ data: expect.objectContaining({ planLabel: 'Solo', type: 'SUBSCRIPTION' }) })
    );
  });

  // WB-02 : dialogRef.afterClosed() retourne true → consentService.acceptConsent est appelé AVANT billingService.createCheckoutSession
  it('WB-02 — confirm → acceptConsent puis createCheckoutSession (ordre respecté)', async () => {
    const { component, dialogSpy, legalConsentServiceSpy, billingServiceSpy } = await buildComponent();
    const callOrder: string[] = [];

    dialogSpy.open.mockReturnValue({ afterClosed: () => of(true) } as never);
    legalConsentServiceSpy.acceptConsent.mockImplementation(() => {
      callOrder.push('consent');
      return of({ acceptances: [] });
    });
    billingServiceSpy.createCheckoutSession.mockImplementation(() => {
      callOrder.push('checkout');
      return NEVER;
    });

    component.upgrade('SOLO');

    expect(legalConsentServiceSpy.acceptConsent).toHaveBeenCalledWith({
      consentTypes: ['PAYMENT_TERMS'],
      version: '2026-05-11'
    });
    expect(billingServiceSpy.createCheckoutSession).toHaveBeenCalledWith('SOLO');
    expect(callOrder).toEqual(['consent', 'checkout']);
  });

  // WB-03 : dialogRef.afterClosed() retourne false → aucun POST
  it('WB-03 — annulation → ni acceptConsent ni createCheckoutSession', async () => {
    const { component, dialogSpy, legalConsentServiceSpy, billingServiceSpy } = await buildComponent();
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(false) } as never);

    component.upgrade('SOLO');

    expect(legalConsentServiceSpy.acceptConsent).not.toHaveBeenCalled();
    expect(billingServiceSpy.createCheckoutSession).not.toHaveBeenCalled();
  });

  // WB-04 : consentService.acceptConsent échoue → billingService.createCheckoutSession non appelé + MatSnackBar
  it('WB-04 — erreur acceptConsent → pas de createCheckoutSession + snackbar erreur', async () => {
    const { component, dialogSpy, legalConsentServiceSpy, billingServiceSpy, snackBarSpy } = await buildComponent();
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(true) } as never);
    legalConsentServiceSpy.acceptConsent.mockReturnValue(throwError(() => new Error('network')));

    component.upgrade('SOLO');

    expect(billingServiceSpy.createCheckoutSession).not.toHaveBeenCalled();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Impossible d\'enregistrer votre acceptation, réessayez.',
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  // WB-05 : flow top-up similaire
  it('WB-05 — buyTopup ouvre la modale, confirm → acceptConsent puis createTopupSession', async () => {
    const { component, dialogSpy, legalConsentServiceSpy, billingServiceSpy } = await buildComponent();
    const callOrder: string[] = [];

    dialogSpy.open.mockReturnValue({ afterClosed: () => of(true) } as never);
    legalConsentServiceSpy.acceptConsent.mockImplementation(() => {
      callOrder.push('consent');
      return of({ acceptances: [] });
    });
    billingServiceSpy.createTopupSession.mockImplementation(() => {
      callOrder.push('topup');
      return NEVER;
    });

    component.buyTopup('TOKENS_1M');

    expect(dialogSpy.open).toHaveBeenCalledWith(
      PaymentTermsAcceptanceDialogComponent,
      expect.objectContaining({ data: expect.objectContaining({ type: 'TOPUP' }) })
    );
    expect(legalConsentServiceSpy.acceptConsent).toHaveBeenCalledWith({
      consentTypes: ['PAYMENT_TERMS'],
      version: '2026-05-11'
    });
    expect(billingServiceSpy.createTopupSession).toHaveBeenCalledWith('TOKENS_1M');
    expect(callOrder).toEqual(['consent', 'topup']);
  });

  it('WB-05b — buyTopup annulé → ni acceptConsent ni createTopupSession', async () => {
    const { component, dialogSpy, legalConsentServiceSpy, billingServiceSpy } = await buildComponent();
    dialogSpy.open.mockReturnValue({ afterClosed: () => of(false) } as never);

    component.buyTopup('TOKENS_1M');

    expect(legalConsentServiceSpy.acceptConsent).not.toHaveBeenCalled();
    expect(billingServiceSpy.createTopupSession).not.toHaveBeenCalled();
  });
});
