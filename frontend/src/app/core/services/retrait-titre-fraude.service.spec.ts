import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RetraitTitreFraudeService } from './retrait-titre-fraude.service';
import {
  RetraitTitreFraudeRequest,
  RetraitTitreFraudeResponse,
} from '../models/retrait-titre-fraude.model';

describe('RetraitTitreFraudeService', () => {
  let service: RetraitTitreFraudeService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/retrait-titre-fraude-analysis';

  function response(overrides: Partial<RetraitTitreFraudeResponse> = {}): RetraitTitreFraudeResponse {
    return {
      caseFileId: 'case-1',
      dateRetrait: '2026-01-15',
      motifRetrait: 'FAUSSES_DECLARATIONS',
      miseEnDemeurePrealable: false,
      dateMiseEnDemeure: null,
      country: 'FRANCE',
      statut: 'RECOURS_POSSIBLE',
      vicesDeProcedure: ['Défaut de procédure contradictoire préalable'],
      motifsContestation: ['Absence de fraude caractérisée', 'Atteinte à la vie privée'],
      delaiRecoursTA: '2026-03-16',
      baseJuridique: 'CESEDA, art. L. 432-4 et s.',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RetraitTitreFraudeService],
    });
    service = TestBed.inject(RetraitTitreFraudeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(RetraitTitreFraudeService.STANDALONE_TOOL_ID).toBe('F-IM-45-retrait-titre-fraude-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: RetraitTitreFraudeRequest = {
      dateRetrait: '2026-01-15',
      motifRetrait: 'MARIAGE_GRIS',
      miseEnDemeurePrealable: true,
      dateMiseEnDemeure: '2026-01-05',
    };
    let result: RetraitTitreFraudeResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response({ statut: 'URGENT' }));
    expect(result!.statut).toBe('URGENT');
    expect(result!.vicesDeProcedure.length).toBe(1);
    expect(result!.delaiRecoursTA).toBe('2026-03-16');
  });

  it('get() GETs from the right URL', () => {
    let result: RetraitTitreFraudeResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'PRESCRIT', vicesDeProcedure: [], motifsContestation: [] }));
    expect(result!.statut).toBe('PRESCRIT');
    expect(result!.vicesDeProcedure).toEqual([]);
    expect(result!.motifsContestation).toEqual([]);
  });

  it('propagates backend error to the subscriber', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({
      next: () => fail('should not succeed'),
      error: (e) => (errStatus = e.status),
    });
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    expect(errStatus).toBe(500);
  });
});
