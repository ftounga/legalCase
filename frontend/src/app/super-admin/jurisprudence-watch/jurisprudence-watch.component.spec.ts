import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { JurisprudenceWatchComponent, parseBootstrapCsv } from './jurisprudence-watch.component';
import {
  JurisprudenceBootstrapEntry,
  JurisprudenceBootstrapJobStatusResponse,
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
        jobId: 'job-default', entriesTotal: 0, startedAt: '2026-05-27T00:00:00Z',
      })),
      getBootstrapJobStatus: jest.fn().mockReturnValue(of(jobStatus('job-default', 'DONE'))),
      createManualMapping: jest.fn().mockReturnValue(of({
        id: 'm1', toolId: 'F-DT-75', brancheCalculId: 'default',
        arretRef: 'Cass. soc. ref', juridiction: 'Cass.', dateArret: '2024-03-12',
        numeroPourvoi: '22-X', lienLegifrance: 'https://x', chapeauOfficiel: 'Chap.',
      })),
      reevaluate: jest.fn().mockReturnValue(of({ totalAEvaluer: 42 })),
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

  it('T-06 — runBootstrap success: 202 then polling RUNNING → DONE updates bootstrapJob and reloads audit', fakeAsync(() => {
    fixture.detectChanges();
    client.triggerBootstrap.mockReturnValue(of({
      jobId: 'job-1', entriesTotal: 3, startedAt: '2026-05-27T01:00:00Z',
    }));
    client.getBootstrapJobStatus
      .mockReturnValueOnce(of(jobStatus('job-1', 'RUNNING', { entriesTotal: 3, entriesProcessed: 1 })))
      .mockReturnValueOnce(of(jobStatus('job-1', 'RUNNING', { entriesTotal: 3, entriesProcessed: 2, mappingsCreated: 1 })))
      .mockReturnValueOnce(of(jobStatus('job-1', 'DONE', {
        entriesTotal: 3, entriesProcessed: 3, mappingsCreated: 2, entriesSkipped: 1, durationMs: 1500,
      })));
    component['loadExample']();
    const initialAuditCalls = client.listAuditLog.mock.calls.length;

    component['runBootstrap']();
    // POST a déjà renseigné bootstrapJob (status synthétique RUNNING avec entriesTotal=3)
    expect(client.triggerBootstrap).toHaveBeenCalledWith(component.parseResult.entries);
    expect(component.bootstrapJob?.jobId).toBe('job-1');
    expect(component.bootstrapJob?.status).toBe('RUNNING');
    expect(component.loadingBootstrap).toBe(true);

    tick(5000);
    expect(component.bootstrapJob?.entriesProcessed).toBe(1);
    tick(5000);
    expect(component.bootstrapJob?.mappingsCreated).toBe(1);
    tick(5000);

    expect(component.bootstrapJob?.status).toBe('DONE');
    expect(component.bootstrapJob?.mappingsCreated).toBe(2);
    expect(component.loadingBootstrap).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Bootstrap terminé : 3 processed, 2 created, 1 skipped (1500ms)'),
      'OK',
      expect.anything()
    );
    expect(client.listAuditLog.mock.calls.length).toBe(initialAuditCalls + 1);
  }));

  it('T-07 — runBootstrap HTTP error on POST shows échec snackbar and no polling', () => {
    fixture.detectChanges();
    client.triggerBootstrap.mockReturnValue(throwError(() => ({ message: 'boom' })));
    component['loadExample']();

    component['runBootstrap']();

    expect(component.loadingBootstrap).toBe(false);
    expect(component.bootstrapJob).toBeNull();
    expect(client.getBootstrapJobStatus).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Échec du lancement'),
      'OK',
      expect.anything()
    );
  });

  it('T-07b — polling that returns FAILED shows échec snackbar and stops polling', fakeAsync(() => {
    fixture.detectChanges();
    client.triggerBootstrap.mockReturnValue(of({
      jobId: 'job-fail', entriesTotal: 5, startedAt: '2026-05-27T01:00:00Z',
    }));
    client.getBootstrapJobStatus.mockReturnValue(of(jobStatus('job-fail', 'FAILED', {
      entriesProcessed: 2, errorMessage: 'evaluator boom',
    })));
    component['loadExample']();

    component['runBootstrap']();
    tick(5000);

    expect(component.bootstrapJob?.status).toBe('FAILED');
    expect(component.loadingBootstrap).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Échec du bootstrap : evaluator boom'),
      'OK',
      expect.anything()
    );
    // 2nd tick shouldn't trigger another HTTP call (polling stopped)
    const callsAfterFirst = client.getBootstrapJobStatus.mock.calls.length;
    tick(5000);
    expect(client.getBootstrapJobStatus.mock.calls.length).toBe(callsAfterFirst);
  }));

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

  it('T-11 — onFileSelected with valid CSV fills csvInput and shows info snackbar', async () => {
    fixture.detectChanges();
    const csvContent = 'f-dt-07,b1,ancienneté\nf-im-05,b2,L. 423-23 CESEDA';
    const file = new File([csvContent], 'bootstrap-batch-1.csv', { type: 'text/csv' });
    Object.defineProperty(file, 'size', { value: csvContent.length });
    const event = buildFileSelectedEvent(file);

    // Stub FileReader for deterministic synchronous test (jsdom's async loader
    // is racy under Jest fake/real timers; we control the lifecycle explicitly)
    const stubReader: Partial<FileReader> = {
      readAsText: jest.fn(function (this: FileReader) {
        Object.defineProperty(this, 'result', { value: csvContent });
        if (this.onload) {
          this.onload({} as ProgressEvent<FileReader>);
        }
      }),
    };
    const fileReaderSpy = jest.spyOn(window, 'FileReader')
      .mockImplementation(() => stubReader as FileReader);

    try {
      component['onFileSelected'](event);
    } finally {
      fileReaderSpy.mockRestore();
    }

    expect(component.csvInput).toBe(csvContent);
    expect(component.parseResult.entries).toHaveLength(2);
    expect(component.loadingFile).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Fichier "bootstrap-batch-1.csv" chargé (2 entrées détectées)'),
      'OK',
      expect.anything(),
    );
    // input reset for re-upload of same file
    expect((event.target as HTMLInputElement).value).toBe('');
  });

  it('T-11b — onFileSelected propagates FileReader.onerror to snackbar', () => {
    fixture.detectChanges();
    const file = new File(['x'], 'broken.csv', { type: 'text/csv' });
    Object.defineProperty(file, 'size', { value: 100 });
    const event = buildFileSelectedEvent(file);

    const stubReader: Partial<FileReader> = {
      readAsText: jest.fn(function (this: FileReader) {
        if (this.onerror) {
          this.onerror({} as ProgressEvent<FileReader>);
        }
      }),
    };
    const fileReaderSpy = jest.spyOn(window, 'FileReader')
      .mockImplementation(() => stubReader as FileReader);

    try {
      component['onFileSelected'](event);
    } finally {
      fileReaderSpy.mockRestore();
    }

    expect(component.loadingFile).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur de lecture du fichier',
      'OK',
      expect.anything(),
    );
    expect((event.target as HTMLInputElement).value).toBe('');
  });

  it('T-12 — onFileSelected with empty file shows error snackbar and keeps csvInput intact', () => {
    fixture.detectChanges();
    component.csvInput = 'previous content';
    const file = new File([], 'empty.csv', { type: 'text/csv' });
    const event = buildFileSelectedEvent(file);

    component['onFileSelected'](event);

    expect(snackBar.open).toHaveBeenCalledWith('Fichier vide', 'OK', expect.anything());
    expect(component.csvInput).toBe('previous content');
    expect((event.target as HTMLInputElement).value).toBe('');
  });

  it('T-13 — onFileSelected with file > 1 Mo shows error snackbar and keeps csvInput intact', () => {
    fixture.detectChanges();
    component.csvInput = 'previous content';
    // Build a fake File whose .size exceeds 1 Mo without allocating real bytes
    const file = new File(['x'], 'huge.csv', { type: 'text/csv' });
    Object.defineProperty(file, 'size', { value: 2_000_000 });
    const event = buildFileSelectedEvent(file);

    component['onFileSelected'](event);

    expect(snackBar.open).toHaveBeenCalledWith(
      'Fichier trop volumineux (max 1 Mo)',
      'OK',
      expect.anything(),
    );
    expect(component.csvInput).toBe('previous content');
    expect((event.target as HTMLInputElement).value).toBe('');
  });

  // --- SF-JU-01-15 — création manuelle ---

  it('T-14 — submitManualMapping success calls client.createManualMapping and reloads audit', () => {
    fixture.detectChanges();
    component.manualMapping = {
      toolId: 'F-DT-75', brancheCalculId: 'default',
      arretRef: 'Cass. soc. 12 mars 2024, n° 22-XXX',
      juridiction: 'Cass.', dateArret: '2024-03-12',
      numeroPourvoi: '22-X', lienLegifrance: 'https://x',
      chapeauOfficiel: 'Chapeau.',
    };
    const initialAuditCalls = client.listAuditLog.mock.calls.length;

    component['submitManualMapping']();

    expect(client.createManualMapping).toHaveBeenCalledWith(expect.objectContaining({ toolId: 'F-DT-75' }));
    expect(component.loadingManualMapping).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Mapping créé'), 'OK', expect.anything());
    expect(client.listAuditLog.mock.calls.length).toBe(initialAuditCalls + 1);
    // Form is reset after success
    expect(component.manualMapping.toolId).toBe('');
  });

  it('T-15 — submitManualMapping 409 conflict shows "déjà existant" snackbar', () => {
    fixture.detectChanges();
    client.createManualMapping.mockReturnValue(throwError(() => ({ status: 409 })));
    component.manualMapping = {
      toolId: 'F-DT-30', brancheCalculId: 'default',
      arretRef: 'Cass. soc. X', juridiction: 'Cass.', dateArret: '2024-01-01',
      numeroPourvoi: '22-X', lienLegifrance: 'https://x', chapeauOfficiel: 'C.',
    };

    component['submitManualMapping']();

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('déjà existant'), 'OK', expect.anything());
    expect(component.loadingManualMapping).toBe(false);
  });

  it('T-16 — submitManualMapping with empty field is rejected client-side', () => {
    fixture.detectChanges();
    component.manualMapping = {
      toolId: '', brancheCalculId: 'default', arretRef: 'X',
      juridiction: 'X', dateArret: '2024-01-01', numeroPourvoi: 'X',
      lienLegifrance: 'X', chapeauOfficiel: 'X',
    };

    component['submitManualMapping']();

    expect(client.createManualMapping).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('obligatoires'), 'OK', expect.anything());
  });

  function jobStatus(jobId: string,
                     status: 'RUNNING' | 'DONE' | 'FAILED',
                     overrides: Partial<JurisprudenceBootstrapJobStatusResponse> = {}):
      JurisprudenceBootstrapJobStatusResponse {
    return {
      jobId,
      status,
      entriesTotal: overrides.entriesTotal ?? 3,
      entriesProcessed: overrides.entriesProcessed ?? 0,
      mappingsCreated: overrides.mappingsCreated ?? 0,
      entriesSkipped: overrides.entriesSkipped ?? 0,
      durationMs: overrides.durationMs ?? null,
      errorMessage: overrides.errorMessage ?? null,
      startedAt: overrides.startedAt ?? '2026-05-27T01:00:00Z',
      completedAt: overrides.completedAt ?? null,
    };
  }

  function buildFileSelectedEvent(file: File): Event {
    const input = document.createElement('input');
    input.type = 'file';
    Object.defineProperty(input, 'files', { value: [file], configurable: true });
    return { target: input } as unknown as Event;
  }

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

  // SF-JU-06-02 — ré-évaluation/archivage
  it('requestReevaluation : 1er clic demande confirmation sans appeler le client', () => {
    (component as any).requestReevaluation();
    expect(component.confirmingReevaluation).toBe(true);
    expect(client.reevaluate).not.toHaveBeenCalled();
  });

  it('requestReevaluation : 2ᵉ clic lance la ré-évaluation et notifie', () => {
    (component as any).requestReevaluation();
    (component as any).requestReevaluation();
    expect(client.reevaluate).toHaveBeenCalledTimes(1);
    expect(component.confirmingReevaluation).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('42'), 'OK', expect.anything());
  });

  it('cancelReevaluation : annule la confirmation', () => {
    (component as any).requestReevaluation();
    (component as any).cancelReevaluation();
    expect(component.confirmingReevaluation).toBe(false);
    expect(client.reevaluate).not.toHaveBeenCalled();
  });
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
