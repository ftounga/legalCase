import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { MatSnackBar, MatSnackBarRef } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { DecisionalToolsProgressService, ToolMetadata } from './decisional-tools-progress.service';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { AnalysisStatusEvent } from '../../core/services/analysis-sse.service';

describe('DecisionalToolsProgressService', () => {
  let service: DecisionalToolsProgressService;
  let events$: Subject<AnalysisStatusEvent>;
  let snackOpenSpy: jest.Mock;
  let snackActionSubject: Subject<void>;
  let dialogOpenSpy: jest.Mock;

  beforeEach(() => {
    events$ = new Subject<AnalysisStatusEvent>();
    snackActionSubject = new Subject<void>();
    snackOpenSpy = jest.fn().mockReturnValue({
      onAction: () => snackActionSubject.asObservable(),
    } as unknown as MatSnackBarRef<unknown>);
    dialogOpenSpy = jest.fn();

    TestBed.configureTestingModule({
      providers: [
        DecisionalToolsProgressService,
        { provide: GlobalAnalysisNotificationService, useValue: { events$: events$.asObservable() } },
        { provide: MatSnackBar, useValue: { open: snackOpenSpy } },
        { provide: MatDialog, useValue: { open: dialogOpenSpy } },
      ],
    });
    service = TestBed.inject(DecisionalToolsProgressService);
  });

  it('start ajoute un jobType — isActive passe true', () => {
    expect(service.isActive()).toBe(false);
    service.start('CASE_ANALYSIS');
    expect(service.isActive()).toBe(true);
    expect(service.activeJobTypes()).toEqual(['CASE_ANALYSIS']);
  });

  it('start sur 2 jobTypes différents — agrégation', () => {
    service.start('CASE_ANALYSIS');
    service.start('DOCUMENT_ANALYSIS');
    expect(service.activeJobTypes()).toEqual(expect.arrayContaining(['CASE_ANALYSIS', 'DOCUMENT_ANALYSIS']));
    expect(service.activeJobTypes()).toHaveLength(2);
  });

  it('events$ DONE retire le jobType correspondant', () => {
    service.start('CASE_ANALYSIS');
    service.start('DOCUMENT_ANALYSIS');

    events$.next({ caseFileId: 'c1', status: 'DONE', jobType: 'CASE_ANALYSIS' });
    expect(service.activeJobTypes()).toEqual(['DOCUMENT_ANALYSIS']);

    events$.next({ caseFileId: 'c1', status: 'DONE', jobType: 'DOCUMENT_ANALYSIS' });
    expect(service.isActive()).toBe(false);
  });

  it('events$ FAILED retire aussi le jobType', () => {
    service.start('ENRICHED_ANALYSIS');
    events$.next({ caseFileId: 'c1', status: 'FAILED', jobType: 'ENRICHED_ANALYSIS' });
    expect(service.isActive()).toBe(false);
  });

  it('syncFromJobs initialise depuis une liste de jobs PROCESSING / PENDING', () => {
    service.syncFromJobs([
      { jobType: 'CASE_ANALYSIS', status: 'PROCESSING' },
      { jobType: 'DOCUMENT_ANALYSIS', status: 'DONE' },
      { jobType: 'ENRICHED_ANALYSIS', status: 'PENDING' },
    ]);
    expect(service.activeJobTypes()).toEqual(expect.arrayContaining(['CASE_ANALYSIS', 'ENRICHED_ANALYSIS']));
    expect(service.activeJobTypes()).toHaveLength(2);
  });

  it('syncFromJobs avec liste sans PROCESSING vide l\'état', () => {
    service.start('CASE_ANALYSIS');
    service.syncFromJobs([{ jobType: 'CASE_ANALYSIS', status: 'DONE' }]);
    expect(service.isActive()).toBe(false);
  });

  it('events$ pour un jobType non actif est ignoré', () => {
    service.start('CASE_ANALYSIS');
    events$.next({ caseFileId: 'c1', status: 'DONE', jobType: 'DOCUMENT_ANALYSIS' });
    expect(service.activeJobTypes()).toEqual(['CASE_ANALYSIS']);
  });

  describe('SF-159-02 — recordSnapshot diff', () => {
    const meta = (label: string): ToolMetadata => ({ label, icon: 'auto_awesome' });

    function metadataFor(...ids: string[]): Map<string, ToolMetadata> {
      return new Map(ids.map(id => [id, meta(`Tool ${id}`)]));
    }

    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('1er snapshot — silencieux (pas de flash, pas de toast)', () => {
      service.recordSnapshot(new Map([['t1', 3]]), metadataFor('t1'));
      expect(service.flashedToolIds().size).toBe(0);
      expect(snackOpenSpy).not.toHaveBeenCalled();
    });

    it('2e snapshot avec delta positif — flash + toast', () => {
      service.recordSnapshot(new Map([['t1', 3], ['t2', 0]]), metadataFor('t1', 't2'));
      service.recordSnapshot(new Map([['t1', 5], ['t2', 2]]), metadataFor('t1', 't2'));

      const ids = service.flashedToolIds();
      expect(ids.has('t1')).toBe(true);
      expect(ids.has('t2')).toBe(true);
      expect(snackOpenSpy).toHaveBeenCalledTimes(1);
      expect(snackOpenSpy.mock.calls[0][0]).toBe('4 champs pré-remplis dans 2 outils');
      expect(snackOpenSpy.mock.calls[0][1]).toBe('Voir le détail');
    });

    it('delta nul — ni flash ni toast', () => {
      service.recordSnapshot(new Map([['t1', 3]]), metadataFor('t1'));
      service.recordSnapshot(new Map([['t1', 3]]), metadataFor('t1'));
      expect(service.flashedToolIds().size).toBe(0);
      expect(snackOpenSpy).not.toHaveBeenCalled();
    });

    it('delta négatif — ignoré (pas de flash, pas de toast)', () => {
      service.recordSnapshot(new Map([['t1', 5]]), metadataFor('t1'));
      service.recordSnapshot(new Map([['t1', 3]]), metadataFor('t1'));
      expect(service.flashedToolIds().size).toBe(0);
      expect(snackOpenSpy).not.toHaveBeenCalled();
    });

    it('cleanup timer — flashedToolIds purgé après 1500 ms', () => {
      service.recordSnapshot(new Map([['t1', 0]]), metadataFor('t1'));
      service.recordSnapshot(new Map([['t1', 2]]), metadataFor('t1'));
      expect(service.flashedToolIds().has('t1')).toBe(true);

      jest.advanceTimersByTime(1499);
      expect(service.flashedToolIds().has('t1')).toBe(true);

      jest.advanceTimersByTime(1);
      expect(service.flashedToolIds().has('t1')).toBe(false);
    });

    it('toast singulier 1 champ + 1 outil', () => {
      service.recordSnapshot(new Map([['t1', 0]]), metadataFor('t1'));
      service.recordSnapshot(new Map([['t1', 1]]), metadataFor('t1'));
      expect(snackOpenSpy).toHaveBeenCalledTimes(1);
      expect(snackOpenSpy.mock.calls[0][0]).toBe('1 champ pré-rempli dans 1 outil');
    });

    it('clic sur action toast — ouvre le dialog avec les entries', () => {
      service.recordSnapshot(new Map([['t1', 0]]), metadataFor('t1'));
      service.recordSnapshot(new Map([['t1', 4]]), metadataFor('t1'));
      expect(snackOpenSpy).toHaveBeenCalledTimes(1);

      snackActionSubject.next();
      expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
      const dialogCall = dialogOpenSpy.mock.calls[0];
      const data = dialogCall[1].data;
      expect(data.entries).toHaveLength(1);
      expect(data.entries[0]).toEqual({ toolId: 't1', label: 'Tool t1', icon: 'auto_awesome', delta: 4 });
    });

    it('toolId sans metadata — flashé mais absent du dialog', () => {
      service.recordSnapshot(new Map([['t1', 0], ['t2', 0]]), new Map());
      service.recordSnapshot(new Map([['t1', 2], ['t2', 1]]), new Map([['t1', meta('Tool 1')]]));

      expect(service.flashedToolIds().has('t1')).toBe(true);
      expect(service.flashedToolIds().has('t2')).toBe(true);
      expect(snackOpenSpy).toHaveBeenCalledTimes(1);
      // Toast fondé sur les entries présentes (1 outil avec 2 champs)
      expect(snackOpenSpy.mock.calls[0][0]).toBe('2 champs pré-remplis dans 1 outil');
    });
  });
});
