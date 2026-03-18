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
  id: 'ws1', name: 'Test', slug: 'test', planCode: 'STARTER', status: 'ACTIVE'
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

  it('affiche les 3 plans', () => {
    expect(component.plans.length).toBe(3);
    expect(component.plans.map(p => p.code)).toEqual(['FREE', 'STARTER', 'PRO']);
  });

  it('isCurrentPlan — STARTER pour workspace STARTER', () => {
    expect(component.isCurrentPlan('STARTER')).toBeTrue();
    expect(component.isCurrentPlan('PRO')).toBeFalse();
  });

  it('upgrade — appelle BillingService.createCheckoutSession', () => {
    // NEVER ne complète pas → pas de redirection → pas de rechargement dans le test
    billingServiceSpy.createCheckoutSession.and.returnValue(NEVER);

    component.upgrade('PRO');

    expect(billingServiceSpy.createCheckoutSession).toHaveBeenCalledWith('PRO');
    expect(component.upgrading()).toBe('PRO');
  });
});
