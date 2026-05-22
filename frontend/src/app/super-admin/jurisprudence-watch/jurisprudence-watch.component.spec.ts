import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { JurisprudenceWatchComponent } from './jurisprudence-watch.component';
import {
  JurisprudenceWatchAdminClientService,
  JurisprudenceWatchFlag,
  Page,
} from './jurisprudence-watch-admin.service';

describe('JurisprudenceWatchComponent', () => {
  let component: JurisprudenceWatchComponent;
  let fixture: ComponentFixture<JurisprudenceWatchComponent>;
  let client: jest.Mocked<JurisprudenceWatchAdminClientService>;
  let snackBar: jest.Mocked<MatSnackBar>;

  function emptyPage<T>(): Page<T> {
    return { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 };
  }

  function flagPage(flags: JurisprudenceWatchFlag[]): Page<JurisprudenceWatchFlag> {
    return { content: flags, totalElements: flags.length, totalPages: 1, number: 0, size: 20 };
  }

  beforeEach(async () => {
    client = {
      listFlags: jest.fn().mockReturnValue(of(emptyPage())),
      listAuditLog: jest.fn().mockReturnValue(of(emptyPage())),
      arbitrate: jest.fn().mockReturnValue(of({})),
    } as any;
    snackBar = { open: jest.fn() } as any;

    await TestBed.configureTestingModule({
      imports: [JurisprudenceWatchComponent, NoopAnimationsModule],
      providers: [
        { provide: JurisprudenceWatchAdminClientService, useValue: client },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(JurisprudenceWatchComponent);
    component = fixture.componentInstance;
  });

  it('T-01 — ngOnInit loads flags and audit log', () => {
    fixture.detectChanges();
    expect(client.listFlags).toHaveBeenCalledWith('PENDING', 0, 50);
    expect(client.listAuditLog).toHaveBeenCalled();
  });

  it('T-02 — arbitrate removes flag from list and reloads audit', () => {
    const flag = buildFlag('abc');
    client.listFlags.mockReturnValue(of(flagPage([flag])));
    client.arbitrate.mockReturnValue(of(flag));

    fixture.detectChanges();
    expect(component.flags).toHaveLength(1);

    component['arbitrate']('abc', 'IGNORE');

    expect(client.arbitrate).toHaveBeenCalledWith('abc', 'IGNORE', undefined);
    expect(component.flags).toHaveLength(0);
    expect(snackBar.open).toHaveBeenCalledWith('Décision appliquée.', 'OK', expect.anything());
    expect(client.listAuditLog).toHaveBeenCalledTimes(2);
  });

  it('T-03 — arbitrate failure shows error snackbar', () => {
    client.arbitrate.mockReturnValue(throwError(() => new Error('fail')));
    fixture.detectChanges();

    component['arbitrate']('xxx', 'REPLACE');

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Échec'), 'OK', expect.anything());
  });

  it('T-04 — openArbitrate / cancelArbitrate toggle inline form', () => {
    fixture.detectChanges();
    expect(component.arbitratingId).toBeNull();

    component['openArbitrate']('flag1');
    expect(component.arbitratingId).toBe('flag1');

    component['cancelArbitrate']();
    expect(component.arbitratingId).toBeNull();
  });

  function buildFlag(id: string): JurisprudenceWatchFlag {
    return {
      id,
      toolId: 'f-dt-30',
      brancheCalculId: 'b1',
      arretEntrantRef: 'Cass. soc. 8 janv. 2025',
      mappingActuelId: 'm1',
      source: 'CRON',
      confidenceScore: 0.75,
      explication: 'potentiel revirement',
      statut: 'PENDING',
      createdAt: '2026-05-22T10:00:00Z',
      reviewedAt: null,
      decision: null,
      commentUser: null,
    };
  }
});
