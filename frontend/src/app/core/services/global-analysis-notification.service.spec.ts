import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GlobalAnalysisNotificationService } from './global-analysis-notification.service';
import { AnalysisSseService, AnalysisStatusEvent } from './analysis-sse.service';

describe('GlobalAnalysisNotificationService', () => {
  let service: GlobalAnalysisNotificationService;
  let sseSubject: Subject<AnalysisStatusEvent>;
  let snackBarSpy: { open: jest.Mock };

  beforeEach(() => {
    sseSubject = new Subject<AnalysisStatusEvent>();
    snackBarSpy = { open: jest.fn() };

    TestBed.configureTestingModule({
      providers: [
        GlobalAnalysisNotificationService,
        { provide: AnalysisSseService, useValue: { stream: () => sseSubject.asObservable() } },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    });

    service = TestBed.inject(GlobalAnalysisNotificationService);
  });

  it('réémet plusieurs événements consécutifs sur events$', () => {
    const received: AnalysisStatusEvent[] = [];
    service.events$.subscribe(e => received.push(e));

    service.track('case-1');

    sseSubject.next({ caseFileId: 'case-1', status: 'DONE', jobType: 'CASE_ANALYSIS' });
    sseSubject.next({ caseFileId: 'case-1', status: 'DONE', jobType: 'DOCUMENT_ANALYSIS' });
    sseSubject.next({ caseFileId: 'case-1', status: 'DONE', jobType: 'ENRICHED_ANALYSIS' });

    expect(received).toHaveLength(3);
    expect(received.map(e => e.jobType)).toEqual(['CASE_ANALYSIS', 'DOCUMENT_ANALYSIS', 'ENRICHED_ANALYSIS']);
  });

  it('affiche un toast par événement reçu', () => {
    service.track('case-1');

    sseSubject.next({ caseFileId: 'case-1', status: 'DONE', jobType: 'CASE_ANALYSIS' });
    sseSubject.next({ caseFileId: 'case-1', status: 'DONE', jobType: 'ENRICHED_ANALYSIS' });

    expect(snackBarSpy.open).toHaveBeenCalledTimes(2);
    expect(snackBarSpy.open.mock.calls[0][0]).toBe('Synthèse du dossier terminée');
    expect(snackBarSpy.open.mock.calls[1][0]).toBe('Re-synthèse enrichie terminée');
  });

  it('toast d\'erreur pour FAILED avec panelClass snack-error', () => {
    service.track('case-1');
    sseSubject.next({ caseFileId: 'case-1', status: 'FAILED', jobType: 'DOCUMENT_ANALYSIS' });

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      "L'analyse des documents a échoué",
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
  });
});
