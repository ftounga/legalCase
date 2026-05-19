import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DisabledIfPendingPaymentDirective } from './disabled-if-pending-payment.directive';
import { WorkspaceStateService } from '../../core/services/workspace-state.service';
import { Workspace } from '../../core/models/workspace.model';

@Component({
  standalone: true,
  imports: [DisabledIfPendingPaymentDirective],
  template: `<button appDisabledIfPendingPayment data-testid="btn">Action</button>`,
})
class HostComponent {}

describe('DisabledIfPendingPaymentDirective (F-156 SF-156-02 — CA9)', () => {
  let fixture: ComponentFixture<HostComponent>;
  let state: WorkspaceStateService;

  function ws(status: string): Workspace {
    return { id: 'w', name: 'X', slug: 'x', planCode: 'TEAM', status, primary: true };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent],
    }).compileComponents();

    state = TestBed.inject(WorkspaceStateService);
    fixture = TestBed.createComponent(HostComponent);
  });

  it('D-01 — workspace ACTIVE → bouton non disabled (état initial)', () => {
    state.setCurrent(ws('ACTIVE'));
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('[data-testid="btn"]') as HTMLButtonElement;
    expect(btn.hasAttribute('disabled')).toBe(false);
    expect(btn.classList.contains('is-disabled-pending-payment')).toBe(false);
  });

  it('D-02 — workspace PENDING_PAYMENT → bouton disabled + classe + title + aria-disabled', () => {
    state.setCurrent(ws('PENDING_PAYMENT'));
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('[data-testid="btn"]') as HTMLButtonElement;
    expect(btn.hasAttribute('disabled')).toBe(true);
    expect(btn.classList.contains('is-disabled-pending-payment')).toBe(true);
    expect(btn.getAttribute('title')).toContain('attente d\'activation');
    expect(btn.getAttribute('aria-disabled')).toBe('true');
  });

  it('D-03 — transition PENDING_PAYMENT → ACTIVE → attributs retirés', () => {
    state.setCurrent(ws('PENDING_PAYMENT'));
    fixture.detectChanges();
    state.setCurrent(ws('ACTIVE'));
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('[data-testid="btn"]') as HTMLButtonElement;
    expect(btn.hasAttribute('disabled')).toBe(false);
    expect(btn.hasAttribute('title')).toBe(false);
    expect(btn.classList.contains('is-disabled-pending-payment')).toBe(false);
  });
});
