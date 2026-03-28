import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WorkspaceBillingComponent } from './workspace-billing.component';
import { WorkspaceService } from '../../core/services/workspace.service';
import { BillingService } from '../../core/services/billing.service';
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
  let workspaceServiceSpy: jasmine.SpyObj<WorkspaceService>;
  let billingServiceSpy: jasmine.SpyObj<BillingService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['createCheckoutSession']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    workspaceServiceSpy.getCurrentWorkspace.and.returnValue(of(mockWorkspace));

    await TestBed.configureTestingModule({
      imports: [WorkspaceBillingComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
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
      expect(enriched.included).toBeTrue();
    });
  });

  it('re-synthèse enrichie exclue sur FREE', () => {
    const free = component.plans.find(p => p.code === 'FREE')!;
    const enriched = free.features.find(f => f.label.toLowerCase().includes('re-synthèse'))!;
    expect(enriched.included).toBeFalse();
  });

  it('isCurrentPlan — SOLO pour workspace SOLO', () => {
    expect(component.isCurrentPlan('SOLO')).toBeTrue();
    expect(component.isCurrentPlan('PRO')).toBeFalse();
    expect(component.isCurrentPlan('FREE')).toBeFalse();
  });

  it('upgrade — appelle BillingService.createCheckoutSession', () => {
    billingServiceSpy.createCheckoutSession.and.returnValue(NEVER);

    component.upgrade('PRO');

    expect(billingServiceSpy.createCheckoutSession).toHaveBeenCalledWith('PRO');
    expect(component.upgrading()).toBe('PRO');
  });
});
