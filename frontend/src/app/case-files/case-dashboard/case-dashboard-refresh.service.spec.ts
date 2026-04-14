import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';

describe('CaseDashboardRefreshService', () => {
  it('emits on triggerRefresh()', () => {
    const service = new CaseDashboardRefreshService();
    let emissions = 0;
    service.refresh$.subscribe(() => emissions++);
    service.triggerRefresh();
    service.triggerRefresh();
    expect(emissions).toBe(2);
  });

  it('supports multiple subscribers', () => {
    const service = new CaseDashboardRefreshService();
    let a = 0;
    let b = 0;
    service.refresh$.subscribe(() => a++);
    service.refresh$.subscribe(() => b++);
    service.triggerRefresh();
    expect(a).toBe(1);
    expect(b).toBe(1);
  });

  it('does not emit without trigger', () => {
    const service = new CaseDashboardRefreshService();
    let emitted = false;
    service.refresh$.subscribe(() => { emitted = true; });
    expect(emitted).toBe(false);
  });
});
