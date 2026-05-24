import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { JurisprudenceWatchComponent, parseBootstrapCsv } from './jurisprudence-watch.component';
import {
  JurisprudenceBootstrapEntry,
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
      triggerBootstrap: jest.fn().mockReturnValue(of({
        entriesProcessed: 0, mappingsCreated: 0, entriesSkipped: 0, durationMs: 0,
      })),
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

  it('T-05 — loadExample fills csvInput and parseResult', () => {
    fixture.detectChanges();
    expect(component.parseResult.entries).toHaveLength(0);

    component['loadExample']();

    expect(component.csvInput.length).toBeGreaterThan(0);
    expect(component.parseResult.entries.length).toBeGreaterThanOrEqual(3);
    expect(component.parseResult.errors).toHaveLength(0);
  });

  it('T-06 — runBootstrap success calls triggerBootstrap, reloads audit, shows snackbar', () => {
    fixture.detectChanges();
    client.triggerBootstrap.mockReturnValue(of({
      entriesProcessed: 3, mappingsCreated: 2, entriesSkipped: 1, durationMs: 1500,
    }));
    component['loadExample']();
    const initialAuditCalls = client.listAuditLog.mock.calls.length;

    component['runBootstrap']();

    expect(client.triggerBootstrap).toHaveBeenCalledWith(component.parseResult.entries);
    expect(component.lastBootstrapResult?.mappingsCreated).toBe(2);
    expect(component.loadingBootstrap).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Bootstrap terminé : 3 processed, 2 created, 1 skipped (1500ms)'),
      'OK',
      expect.anything()
    );
    expect(client.listAuditLog.mock.calls.length).toBe(initialAuditCalls + 1);
  });

  it('T-07 — runBootstrap HTTP error shows échec snackbar', () => {
    fixture.detectChanges();
    client.triggerBootstrap.mockReturnValue(throwError(() => ({ message: 'boom' })));
    component['loadExample']();

    component['runBootstrap']();

    expect(component.loadingBootstrap).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Échec du bootstrap'),
      'OK',
      expect.anything()
    );
  });

  it('T-08 — canLaunchBootstrap false on empty/over-limit/errors', () => {
    fixture.detectChanges();
    expect(component['canLaunchBootstrap']()).toBe(false);

    component.parseResult = {
      entries: Array.from({ length: 201 }, (): JurisprudenceBootstrapEntry => ({
        toolId: 't', brancheCalculId: 'b', motCleRecherche: 'm',
      })),
      errors: [],
    };
    expect(component['canLaunchBootstrap']()).toBe(false);

    component.parseResult = {
      entries: [{ toolId: 't', brancheCalculId: 'b', motCleRecherche: 'm' }],
      errors: ['Ligne 1 invalide : ...'],
    };
    expect(component['canLaunchBootstrap']()).toBe(false);

    component.parseResult = {
      entries: [{ toolId: 't', brancheCalculId: 'b', motCleRecherche: 'm' }],
      errors: [],
    };
    expect(component['canLaunchBootstrap']()).toBe(true);
  });

  it('T-09 — runBootstrap with parse errors shows error snackbar and skips HTTP', () => {
    fixture.detectChanges();
    component.parseResult = {
      entries: [],
      errors: ['Ligne 1 invalide : toolId mal formé'],
    };

    component['runBootstrap']();

    expect(client.triggerBootstrap).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Ligne 1 invalide'),
      'OK',
      expect.anything()
    );
  });

  it('T-10 — onCsvInputChange updates csvInput and re-parses', () => {
    fixture.detectChanges();

    component['onCsvInputChange']('f-dt-07,b1,ancienneté');

    expect(component.csvInput).toBe('f-dt-07,b1,ancienneté');
    expect(component.parseResult.entries).toHaveLength(1);
    expect(component.parseResult.entries[0]).toEqual({
      toolId: 'f-dt-07', brancheCalculId: 'b1', motCleRecherche: 'ancienneté',
    });
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

describe('parseBootstrapCsv', () => {
  it('parses minimal 3-column line', () => {
    const r = parseBootstrapCsv('f-dt-07,b1,ancienneté');
    expect(r.errors).toEqual([]);
    expect(r.entries).toEqual([
      { toolId: 'f-dt-07', brancheCalculId: 'b1', motCleRecherche: 'ancienneté' },
    ]);
  });

  it('parses 5-column line with juridictionFiltre and dateMin', () => {
    const r = parseBootstrapCsv('f-im-05,b1,L. 423-23 CESEDA,Conseil d\'État,2020-01-01');
    expect(r.errors).toEqual([]);
    expect(r.entries[0]).toEqual({
      toolId: 'f-im-05',
      brancheCalculId: 'b1',
      motCleRecherche: 'L. 423-23 CESEDA',
      juridictionFiltre: 'Conseil d\'État',
      dateMin: '2020-01-01',
    });
  });

  it('skips empty lines silently', () => {
    const r = parseBootstrapCsv('\n\nf-dt-07,b1,ancienneté\n\n');
    expect(r.entries).toHaveLength(1);
    expect(r.errors).toEqual([]);
  });

  it('rejects line with less than 3 columns', () => {
    const r = parseBootstrapCsv('f-dt-07,b1');
    expect(r.entries).toEqual([]);
    expect(r.errors[0]).toContain('Ligne 1 invalide');
  });

  it('rejects invalid toolId characters', () => {
    const r = parseBootstrapCsv('f-dt-07 invalid!,b1,ancienneté');
    expect(r.entries).toEqual([]);
    expect(r.errors[0]).toContain('toolId');
  });

  it('rejects invalid brancheCalculId characters', () => {
    const r = parseBootstrapCsv('f-dt-07,b1!,ancienneté');
    expect(r.entries).toEqual([]);
    expect(r.errors[0]).toContain('brancheCalculId');
  });

  it('rejects empty motCleRecherche', () => {
    const r = parseBootstrapCsv('f-dt-07,b1,');
    expect(r.entries).toEqual([]);
    expect(r.errors[0]).toContain('motCleRecherche');
  });

  it('rejects malformed dateMin', () => {
    const r = parseBootstrapCsv('f-dt-07,b1,ancienneté,,2020/01/01');
    expect(r.entries).toEqual([]);
    expect(r.errors[0]).toContain('dateMin');
  });

  it('reports errors per line with correct line numbers', () => {
    const csv = [
      'f-dt-07,b1,ok',
      'bad!,b1,ko',
      'f-dt-08,b2,ok',
    ].join('\n');
    const r = parseBootstrapCsv(csv);
    expect(r.entries).toHaveLength(2);
    expect(r.errors).toHaveLength(1);
    expect(r.errors[0]).toContain('Ligne 2');
  });
});
