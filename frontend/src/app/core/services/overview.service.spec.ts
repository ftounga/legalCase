import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OverviewService } from './overview.service';
import { OverviewResponse } from '../models/overview.model';

describe('OverviewService', () => {
  let service: OverviewService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OverviewService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OverviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getOverview() appelle le bon endpoint en GET', () => {
    const mock: OverviewResponse = {
      pilotage: {
        currentPhaseLabel: 'Mise en état',
        awaitingParty: 'OURS',
        nextDeadline: null,
        sante: { admissibility: 'OK', prescriptionStatus: 'OK', riskLevelAvocat: 'MOYEN' },
        analysisStale: false,
        pendingPiecesCount: 0,
        toolsCalculated: 2,
        toolsVisible: 5,
      },
      attention: [],
      attentionTotal: 0,
      fil: [],
    };

    let received: OverviewResponse | undefined;
    service.getOverview('cf-42').subscribe(r => (received = r));

    const req = httpMock.expectOne('/api/v1/case-files/cf-42/overview');
    expect(req.request.method).toBe('GET');
    req.flush(mock);

    expect(received).toEqual(mock);
  });

  it('getOverview() propage une erreur HTTP (fail-open géré par l\'appelant)', () => {
    let errored = false;
    service.getOverview('cf-err').subscribe({
      next: () => fail('ne devrait pas réussir'),
      error: () => (errored = true),
    });

    const req = httpMock.expectOne('/api/v1/case-files/cf-err/overview');
    req.flush('boom', { status: 500, statusText: 'Server Error' });

    expect(errored).toBe(true);
  });
});
