import { TestBed } from '@angular/core/testing';
import { WorkspaceStateService } from './workspace-state.service';
import { Workspace } from '../models/workspace.model';

describe('WorkspaceStateService (F-156 SF-156-02)', () => {
  let service: WorkspaceStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WorkspaceStateService);
  });

  function ws(overrides: Partial<Workspace>): Workspace {
    return {
      id: 'ws-1', name: 'Cabinet Alpha', slug: 'alpha',
      planCode: 'FREE', status: 'ACTIVE', primary: true,
      ...overrides,
    };
  }

  it('S-01 — état initial : current = null, canCreateWorkspace = false', () => {
    expect(service.current()).toBeNull();
    expect(service.canCreateWorkspace()).toBe(false);
    expect(service.isPendingPayment()).toBe(false);
    expect(service.isActive()).toBe(false);
  });

  it('S-02 — plan FREE → canCreateWorkspace = false', () => {
    service.setCurrent(ws({ planCode: 'FREE' }));
    expect(service.canCreateWorkspace()).toBe(false);
  });

  it('S-03 — plan SOLO → canCreateWorkspace = false', () => {
    service.setCurrent(ws({ planCode: 'SOLO' }));
    expect(service.canCreateWorkspace()).toBe(false);
  });

  it('S-04 — plan TEAM → canCreateWorkspace = true', () => {
    service.setCurrent(ws({ planCode: 'TEAM' }));
    expect(service.canCreateWorkspace()).toBe(true);
  });

  it('S-05 — plan PRO → canCreateWorkspace = true', () => {
    service.setCurrent(ws({ planCode: 'PRO' }));
    expect(service.canCreateWorkspace()).toBe(true);
  });

  it('S-06 — status PENDING_PAYMENT → isPendingPayment = true, isActive = false', () => {
    service.setCurrent(ws({ status: 'PENDING_PAYMENT' }));
    expect(service.isPendingPayment()).toBe(true);
    expect(service.isActive()).toBe(false);
  });

  it('S-07 — status ACTIVE → isActive = true, isPendingPayment = false', () => {
    service.setCurrent(ws({ status: 'ACTIVE' }));
    expect(service.isActive()).toBe(true);
    expect(service.isPendingPayment()).toBe(false);
  });

  it('S-08 — planAllowsAdditionalWorkspace static : true pour TEAM/PRO, false pour FREE/SOLO/null', () => {
    expect(WorkspaceStateService.planAllowsAdditionalWorkspace('TEAM')).toBe(true);
    expect(WorkspaceStateService.planAllowsAdditionalWorkspace('PRO')).toBe(true);
    expect(WorkspaceStateService.planAllowsAdditionalWorkspace('team')).toBe(true);
    expect(WorkspaceStateService.planAllowsAdditionalWorkspace('FREE')).toBe(false);
    expect(WorkspaceStateService.planAllowsAdditionalWorkspace('SOLO')).toBe(false);
    expect(WorkspaceStateService.planAllowsAdditionalWorkspace(null)).toBe(false);
    expect(WorkspaceStateService.planAllowsAdditionalWorkspace(undefined)).toBe(false);
  });
});
