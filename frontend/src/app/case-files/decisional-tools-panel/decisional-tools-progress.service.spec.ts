import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { DecisionalToolsProgressService } from './decisional-tools-progress.service';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { AnalysisStatusEvent } from '../../core/services/analysis-sse.service';

describe('DecisionalToolsProgressService', () => {
  let service: DecisionalToolsProgressService;
  let events$: Subject<AnalysisStatusEvent>;

  beforeEach(() => {
    events$ = new Subject<AnalysisStatusEvent>();
    TestBed.configureTestingModule({
      providers: [
        DecisionalToolsProgressService,
        { provide: GlobalAnalysisNotificationService, useValue: { events$: events$.asObservable() } },
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
});
