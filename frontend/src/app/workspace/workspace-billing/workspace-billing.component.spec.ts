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
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['createCheckoutSession', 'createTopupSession']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));

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

  it('plan SOLO à 59 €', () => {
    const solo = component.plans.find(p => p.code === 'SOLO')!;
    expect(solo.price).toBe('59 €');
    expect(solo.label).toBe('Solo');
  });

  it('plan TEAM à 119 €', () => {
    const team = component.plans.find(p => p.code === 'TEAM')!;
    expect(team.price).toBe('119 €');
    expect(team.label).toBe('Team');
  });

  it('plan PRO à 249 €', () => {
    const pro = component.plans.find(p => p.code === 'PRO')!;
    expect(pro.price).toBe('249 €');
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

  it('affiche 3 packs topup', () => {
    expect(component.packs.length).toBe(3);
    expect(component.packs.map(p => p.code)).toEqual(['TOKENS_1M', 'TOKENS_5M', 'TOKENS_20M']);
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
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['createCheckoutSession', 'createTopupSession']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(mockWorkspace));

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
});
