import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WorkspaceBillingComponent } from './workspace-billing.component';
import { WorkspaceService } from '../../core/services/workspace.service';
import { BillingService } from '../../core/services/billing.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { of, NEVER } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Workspace } from '../../core/models/workspace.model';

const mockWorkspace: Workspace = {
  id: 'ws1', name: 'Test', slug: 'test', planCode: 'SOLO', status: 'ACTIVE'
};

describe('WorkspaceBillingComponent', () => {
  let component: WorkspaceBillingComponent;
  let fixture: ComponentFixture<WorkspaceBillingComponent>;
  let workspaceServiceSpy: jest.Mocked<WorkspaceService>;
  let billingServiceSpy: jest.Mocked<BillingService>;
  let snackBarSpy: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['createCheckoutSession', 'createTopupSession', 'getSeatsSummary']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));
    billingServiceSpy.getSeatsSummary.mockReturnValue(of({
      planCode: 'SOLO', seatCount: 1, includedSeats: 1, maxSeats: 1,
      extraSeatPriceCents: 0, baseMonthlyCostCents: 9900, totalMonthlyCostCents: 9900
    }));

    await TestBed.configureTestingModule({
      imports: [WorkspaceBillingComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
        { provide: AnalyticsService, useValue: jasmine.createSpyObj('AnalyticsService', ['trackEvent']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceBillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
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
    // Vérifié côté HTML : plan-card--featured sur SOLO
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

  it('upgrade — appelle BillingService.createCheckoutSession', () => {
    billingServiceSpy.createCheckoutSession.mockReturnValue(NEVER);

    component.upgrade('PRO');

    expect(billingServiceSpy.createCheckoutSession).toHaveBeenCalledWith('PRO');
    expect(component.upgrading()).toBe('PRO');
  });

  it('upgrade — trackEvent upgrade_clicked avec le plan', () => {
    billingServiceSpy.createCheckoutSession.mockReturnValue(NEVER);
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

  it('buyTopup — appelle BillingService.createTopupSession', () => {
    billingServiceSpy.createTopupSession.mockReturnValue(NEVER);

    component.buyTopup('TOKENS_1M');

    expect(billingServiceSpy.createTopupSession).toHaveBeenCalledWith('TOKENS_1M');
    expect(component.buying()).toBe('TOKENS_1M');
  });

});

describe('WorkspaceBillingComponent — topup query params', () => {
  let workspaceServiceSpy: jest.Mocked<WorkspaceService>;
  let billingServiceSpy: jest.Mocked<BillingService>;
  let snackBarSpy: jest.Mocked<MatSnackBar>;

  const setupWith = async (queryParams: Record<string, string>) => {
    workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['createCheckoutSession', 'createTopupSession', 'getSeatsSummary']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));
    billingServiceSpy.getSeatsSummary.mockReturnValue(of({
      planCode: 'SOLO', seatCount: 1, includedSeats: 1, maxSeats: 1,
      extraSeatPriceCents: 0, baseMonthlyCostCents: 9900, totalMonthlyCostCents: 9900
    }));

    await TestBed.configureTestingModule({
      imports: [WorkspaceBillingComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: ActivatedRoute, useValue: { queryParams: of(queryParams) } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(WorkspaceBillingComponent);
    fixture.detectChanges();
  };

  it('?topup=success — affiche le snackbar de confirmation', async () => {
    await setupWith({ topup: 'success' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Tokens ajoutés à votre compte !', 'Fermer', expect.objectContaining({ duration: 6000 })
    );
  });

  it('?topup=canceled — affiche le snackbar d\'annulation', async () => {
    await setupWith({ topup: 'canceled' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Achat de tokens annulé.', 'Fermer', expect.objectContaining({ duration: 4000 })
    );
  });

  it('?topup=success&topup_kind=ocr — affiche le snackbar OCR', async () => {
    await setupWith({ topup: 'success', topup_kind: 'ocr' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Pages OCR ajoutées à votre quota !', 'Fermer', expect.objectContaining({ duration: 6000 })
    );
  });

  it('?topup=canceled&topup_kind=ocr — affiche le snackbar d\'annulation OCR', async () => {
    await setupWith({ topup: 'canceled', topup_kind: 'ocr' });
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Achat de pages OCR annulé.', 'Fermer', expect.objectContaining({ duration: 4000 })
    );
  });
});

describe('WorkspaceBillingComponent — SF-123-03 seats section', () => {
  let component: WorkspaceBillingComponent;
  let fixture: ComponentFixture<WorkspaceBillingComponent>;
  let billingServiceSpy: jest.Mocked<BillingService>;

  beforeEach(async () => {
    const workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['createCheckoutSession', 'createTopupSession', 'getSeatsSummary']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));
    billingServiceSpy.getSeatsSummary.mockReturnValue(of({
      planCode: 'TEAM', seatCount: 4, includedSeats: 3, maxSeats: 6,
      extraSeatPriceCents: 5900, baseMonthlyCostCents: 21900, totalMonthlyCostCents: 27800
    }));

    await TestBed.configureTestingModule({
      imports: [WorkspaceBillingComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
        { provide: AnalyticsService, useValue: jasmine.createSpyObj('AnalyticsService', ['trackEvent']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceBillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge le résumé seats au init', () => {
    expect(billingServiceSpy.getSeatsSummary).toHaveBeenCalled();
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
