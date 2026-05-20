import { ComponentFixture, TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { WorkspaceStatusBannerComponent } from './workspace-status-banner.component';
import { WorkspaceStateService } from '../../core/services/workspace-state.service';
import { Workspace } from '../../core/models/workspace.model';

describe('WorkspaceStatusBannerComponent (F-156 SF-156-02)', () => {
  let fixture: ComponentFixture<WorkspaceStatusBannerComponent>;
  let component: WorkspaceStatusBannerComponent;
  let state: WorkspaceStateService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkspaceStatusBannerComponent, NoopAnimationsModule],
    }).compileComponents();
  });

  function createInZone(): void {
    // Le composant doit être créé dans la fakeAsync zone pour que les setTimeout
    // armés par ses subscribers soient interceptés (sinon le tick(...) ne les avance pas).
    state = TestBed.inject(WorkspaceStateService);
    state.setCurrent(null);
    fixture = TestBed.createComponent(WorkspaceStatusBannerComponent);
    component = fixture.componentInstance;
  }

  function ws(overrides: Partial<Workspace>): Workspace {
    return {
      id: 'ws-pp', name: 'Cabinet Bordeaux', slug: 'bordeaux',
      planCode: 'TEAM', status: 'ACTIVE', primary: true,
      ...overrides,
    };
  }

  // CA7
  it('B-01 — status PENDING_PAYMENT → banderole ambre affichée', () => {
    createInZone();
    state.setCurrent(ws({ status: 'PENDING_PAYMENT' }));
    fixture.detectChanges();
    expect(component.variant()).toBe('pending');
    const el: HTMLElement = fixture.nativeElement;
    const banner = el.querySelector('[data-testid="workspace-status-banner-pending"]');
    expect(banner).toBeTruthy();
    expect(banner!.textContent).toContain('Paiement en cours');
  });

  // CA8 — transition PENDING → ACTIVE déclenche le welcome puis auto-dismiss
  it('B-02 — transition PENDING_PAYMENT → ACTIVE → banderole bienvenue puis dismiss après 5s', fakeAsync(() => {
    createInZone();
    fixture.detectChanges();
    tick();
    state.setCurrent(ws({ status: 'PENDING_PAYMENT' }));
    fixture.detectChanges();
    tick();
    state.setCurrent(ws({ status: 'ACTIVE' }));
    fixture.detectChanges();
    tick();

    expect(component.variant()).toBe('welcome');
    let el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="workspace-status-banner-welcome"]')).toBeTruthy();
    expect(el.textContent).toContain('Bienvenue');
    expect(el.textContent).toContain('Cabinet Bordeaux');

    // Avance de >5s pour déclencher l'auto-dismiss (setTimeout est intercepté par fakeAsync
    // car le composant est créé dans la fakeAsync zone via createInZone()).
    tick(component.welcomeDurationMs + 10);
    fixture.detectChanges();
    expect(component.variant()).toBe('none');
    el = fixture.nativeElement;
    expect(el.querySelector('[data-testid="workspace-status-banner-welcome"]')).toBeFalsy();
  }));

  // ACTIVE direct (sans avoir vu PENDING) → pas de banderole bienvenue (évite le spam à chaque login).
  it('B-03 — workspace ACTIVE direct (sans transition) → pas de banderole', () => {
    createInZone();
    state.setCurrent(ws({ status: 'ACTIVE' }));
    fixture.detectChanges();
    expect(component.variant()).toBe('none');
  });

  // dismissWelcome manuel
  it('B-04 — clic close ferme la banderole bienvenue avant 5s', fakeAsync(() => {
    createInZone();
    state.setCurrent(ws({ status: 'PENDING_PAYMENT' }));
    fixture.detectChanges();
    tick();
    state.setCurrent(ws({ status: 'ACTIVE' }));
    fixture.detectChanges();
    tick();

    expect(component.variant()).toBe('welcome');
    component.dismissWelcome();
    fixture.detectChanges();
    expect(component.variant()).toBe('none');
  }));
});
