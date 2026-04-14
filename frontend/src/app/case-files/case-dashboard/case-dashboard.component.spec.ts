import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { CaseDashboardComponent } from './case-dashboard.component';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';
import { DashboardResponse } from '../../core/models/case-dashboard.model';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

describe('CaseDashboardComponent — refresh integration (SF-IA-02-03)', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let refreshService: CaseDashboardRefreshService;

  const emptyDashboard: DashboardResponse = {
    caseFileId: 'case-1',
    riskScore: null, riskLevel: null,
    licenciement: null, indemnites: null, anciennete: null,
    titleDecision: null, workRight: null, recours: null,
    partage: null, garde: null, divorce: null,
  } as unknown as DashboardResponse;

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(emptyDashboard)) } as any;
    refreshService = new CaseDashboardRefreshService();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: CaseDashboardRefreshService, useValue: refreshService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
  });

  it('loads dashboard once on init', () => {
    fixture.detectChanges();
    expect(dashboardService.get).toHaveBeenCalledTimes(1);
  });

  it('reloads dashboard when refresh service emits (debounced)', fakeAsync(() => {
    fixture.detectChanges(); // initial load
    refreshService.triggerRefresh();
    tick(300);
    expect(dashboardService.get).toHaveBeenCalledTimes(2);
  }));

  it('coalesces bursts of triggerRefresh() via debounce', fakeAsync(() => {
    fixture.detectChanges();
    refreshService.triggerRefresh();
    refreshService.triggerRefresh();
    refreshService.triggerRefresh();
    tick(300);
    expect(dashboardService.get).toHaveBeenCalledTimes(2);
  }));

  it('does not reload before debounce window elapses', fakeAsync(() => {
    fixture.detectChanges();
    refreshService.triggerRefresh();
    tick(100);
    expect(dashboardService.get).toHaveBeenCalledTimes(1);
    tick(250);
    expect(dashboardService.get).toHaveBeenCalledTimes(2);
  }));
});
