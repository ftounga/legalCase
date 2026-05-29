import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RecepisseAttestationService } from './recepisse-attestation.service';
import {
  RecepisseAttestationRequest,
  RecepisseAttestationResponse,
} from '../models/recepisse-attestation.model';

/**
 * SF-214-16 — Tests du wrapper HttpClient F-IM-32 (récépissé vs attestation, FR).
 */
describe('RecepisseAttestationService', () => {
  let service: RecepisseAttestationService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/recepisse-attestation-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RecepisseAttestationService],
    });
    service = TestBed.inject(RecepisseAttestationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('STANDALONE_TOOL_ID aligné sur la clé TOOL_REGISTRY', () => {
    expect(RecepisseAttestationService.STANDALONE_TOOL_ID)
      .toBe('F-IM-32-recepisse-attestation-fr');
  });

  it('analyze : POST vers l\'endpoint figé avec le corps fourni', () => {
    const request: RecepisseAttestationRequest = {
      typeDocument: 'ATTESTATION_PROLONGATION',
      dateDelivrance: '2025-09-01',
      dateExpiration: '2026-03-01',
    };
    const response = { caseFileId: 'case-1' } as RecepisseAttestationResponse;
    service.analyze('case-1', request).subscribe((r) => expect(r).toEqual(response));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeDocument).toBe('ATTESTATION_PROLONGATION');
    req.flush(response);
  });

  it('get : GET vers l\'endpoint figé', () => {
    const response = { caseFileId: 'case-1' } as RecepisseAttestationResponse;
    service.get('case-1').subscribe((r) => expect(r).toEqual(response));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });

  it('analyze : propage une erreur HTTP', () => {
    const request: RecepisseAttestationRequest = {
      typeDocument: 'RECEPISSE',
      dateDelivrance: '2025-09-01',
      dateExpiration: '2026-03-01',
    };
    let errored = false;
    service.analyze('case-1', request).subscribe({
      next: () => fail('should not succeed'),
      error: () => (errored = true),
    });
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(errored).toBe(true);
  });
});
