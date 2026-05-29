import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AesPresenceProuveeService } from './aes-presence-prouvee.service';
import {
  AesPresenceProuveeRequest,
  AesPresenceProuveeResponse,
} from '../models/aes-presence-prouvee.model';

describe('AesPresenceProuveeService', () => {
  let service: AesPresenceProuveeService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/aes-presence-prouvee-analysis';

  function response(overrides: Partial<AesPresenceProuveeResponse> = {}): AesPresenceProuveeResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      periodesPresentees: [
        { debut: '2019-01-01', fin: '2024-01-01', typePiece: 'AVIS_IMPOSITION' },
      ],
      anneesTotalesProuvees: 5,
      eligibiliteParVoie: {
        aes_famille: true,
        aes_humanitaire: false,
        aes_etudiant: true,
        aes_metiers_tension: true,
      },
      gapsPeriodes: [],
      recommandationsPieces: [],
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AesPresenceProuveeService],
    });
    service = TestBed.inject(AesPresenceProuveeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(AesPresenceProuveeService.STANDALONE_TOOL_ID).toBe('F-IM-30-aes-presence-prouvee-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: AesPresenceProuveeRequest = {
      periodesPresentees: [
        { debut: '2019-01-01', fin: '2024-01-01', typePiece: 'AVIS_IMPOSITION' },
      ],
    };
    let result: AesPresenceProuveeResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.anneesTotalesProuvees).toBe(5);
    expect(result!.eligibiliteParVoie.aes_famille).toBe(true);
  });

  it('get() GETs from the right URL', () => {
    let result: AesPresenceProuveeResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ anneesTotalesProuvees: 2, gapsPeriodes: ['2021-2022 non couvert'] }));
    expect(result!.anneesTotalesProuvees).toBe(2);
    expect(result!.gapsPeriodes.length).toBe(1);
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
