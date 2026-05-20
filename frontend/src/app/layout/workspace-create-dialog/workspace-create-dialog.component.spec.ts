import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { WorkspaceCreateDialogComponent } from './workspace-create-dialog.component';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceCreateResponse } from '../../core/models/workspace.model';

describe('WorkspaceCreateDialogComponent (F-156 SF-156-02)', () => {
  let fixture: ComponentFixture<WorkspaceCreateDialogComponent>;
  let component: WorkspaceCreateDialogComponent;
  let workspaceServiceSpy: jest.Mocked<WorkspaceService>;
  let dialogRefSpy: jest.Mocked<MatDialogRef<WorkspaceCreateDialogComponent>>;
  let snackBarSpy: jest.Mocked<MatSnackBar>;

  const stripeResponse: WorkspaceCreateResponse = {
    workspaceId: 'ws-new',
    stripeCheckoutUrl: 'https://checkout.stripe.com/c/pay/cs_test_123',
    status: 'PENDING_PAYMENT',
  };

  // window.location.href stub — on remplace l'objet location pour ne pas vraiment naviguer.
  const originalLocation = window.location;

  beforeAll(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: { href: '' },
    });
  });

  afterAll(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: originalLocation,
    });
  });

  async function setup() {
    workspaceServiceSpy = {
      createWorkspaceWithPlan: jest.fn(),
    } as any;
    dialogRefSpy = { close: jest.fn() } as any;
    snackBarSpy = { open: jest.fn() } as any;

    await TestBed.configureTestingModule({
      imports: [WorkspaceCreateDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (window as any).location.href = '';
  }

  // CA3 — bouton désactivé tant que name + plan non renseignés.
  it('U-01 — formulaire vide → bouton Créer désactivé', async () => {
    await setup();
    expect(component.canSubmit).toBe(false);
  });

  it('U-02 — name vide + plan TEAM choisi → bouton désactivé', async () => {
    await setup();
    component.selectedPlan.set('TEAM');
    expect(component.canSubmit).toBe(false);
  });

  it('U-03 — name renseigné + plan absent → bouton désactivé', async () => {
    await setup();
    component.name = 'Cabinet Bordeaux';
    expect(component.canSubmit).toBe(false);
  });

  it('U-04 — name + plan renseignés → bouton activé', async () => {
    await setup();
    component.name = 'Cabinet Bordeaux';
    component.selectedPlan.set('TEAM');
    expect(component.canSubmit).toBe(true);
  });

  it('U-04b — name avec uniquement des espaces → bouton désactivé', async () => {
    await setup();
    component.name = '   ';
    component.selectedPlan.set('PRO');
    expect(component.canSubmit).toBe(false);
  });

  // CA4 — cards plan rendues avec données pricing F-123
  it('U-05 — 2 cards plan TEAM + PRO rendues dans le template', async () => {
    await setup();
    const el: HTMLElement = fixture.nativeElement;
    const cards = el.querySelectorAll('.wcd-plan-card');
    expect(cards.length).toBe(2);
    const text = el.textContent || '';
    expect(text).toContain('Team');
    expect(text).toContain('Pro');
    expect(text).toContain('219 €');
    expect(text).toContain('429 €');
    expect(text).toContain('3 utilisateurs inclus');
    expect(text).toContain('5 utilisateurs inclus');
  });

  // CA5 — succès → window.location.href vers stripeCheckoutUrl
  it('U-06 — submit succès → createWorkspaceWithPlan appelé puis redirection navigateur', async () => {
    await setup();
    workspaceServiceSpy.createWorkspaceWithPlan.mockReturnValue(of(stripeResponse));
    component.name = '  Cabinet Bordeaux  ';
    component.selectedPlan.set('TEAM');

    component.submit();

    expect(workspaceServiceSpy.createWorkspaceWithPlan).toHaveBeenCalledWith('Cabinet Bordeaux', 'TEAM');
    expect((window as any).location.href).toBe('https://checkout.stripe.com/c/pay/cs_test_123');
    expect(dialogRefSpy.close).toHaveBeenCalledWith({ workspaceId: 'ws-new' });
  });

  // Cas erreur 403 PLAN_REQUIRED (CA défense en profondeur)
  it('U-07 — erreur 403 PLAN_REQUIRED → snackbar + dialog fermé', async () => {
    await setup();
    workspaceServiceSpy.createWorkspaceWithPlan.mockReturnValue(
      throwError(() => ({ status: 403, error: { code: 'PLAN_REQUIRED' } }))
    );
    component.name = 'Cabinet Bordeaux';
    component.selectedPlan.set('TEAM');

    component.submit();

    expect(snackBarSpy.open).toHaveBeenCalled();
    const message = (snackBarSpy.open as jest.Mock).mock.calls[0][0];
    expect(message).toContain('TEAM ou PRO requis');
    expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
    expect(component.submitting()).toBe(false);
  });

  // Cas erreur 400 PLAN_INVALID
  it('U-08 — erreur 400 PLAN_INVALID → snackbar, dialog reste ouvert', async () => {
    await setup();
    workspaceServiceSpy.createWorkspaceWithPlan.mockReturnValue(
      throwError(() => ({ status: 400, error: { code: 'PLAN_INVALID' } }))
    );
    component.name = 'Cabinet Bordeaux';
    component.selectedPlan.set('PRO');

    component.submit();

    expect(snackBarSpy.open).toHaveBeenCalled();
    const message = (snackBarSpy.open as jest.Mock).mock.calls[0][0];
    expect(message).toContain('Plan invalide');
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
    expect(component.submitting()).toBe(false);
  });

  // Cas erreur 502 Stripe down
  it('U-09 — erreur 502 Stripe down → snackbar dédié, dialog reste ouvert', async () => {
    await setup();
    workspaceServiceSpy.createWorkspaceWithPlan.mockReturnValue(
      throwError(() => ({ status: 502 }))
    );
    component.name = 'Cabinet Bordeaux';
    component.selectedPlan.set('TEAM');

    component.submit();

    expect(snackBarSpy.open).toHaveBeenCalled();
    const message = (snackBarSpy.open as jest.Mock).mock.calls[0][0];
    expect(message).toContain('Service de paiement indisponible');
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  // CA13 — pas d'usage de window.alert/confirm/prompt
  it('U-10 — cancel ferme le dialog avec null', async () => {
    await setup();
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
  });

  it('U-11 — selectPlan met à jour selectedPlan', async () => {
    await setup();
    component.selectPlan('PRO');
    expect(component.selectedPlan()).toBe('PRO');
  });

  it('U-12 — submit bloque les re-soumissions pendant submitting', async () => {
    await setup();
    workspaceServiceSpy.createWorkspaceWithPlan.mockReturnValue(of(stripeResponse));
    component.name = 'Cabinet Bordeaux';
    component.selectedPlan.set('TEAM');
    component.submitting.set(true);
    component.submit();
    expect(workspaceServiceSpy.createWorkspaceWithPlan).not.toHaveBeenCalled();
  });
});
