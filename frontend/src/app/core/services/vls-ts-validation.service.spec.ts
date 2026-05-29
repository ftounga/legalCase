import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { VlsTsValidationService } from './vls-ts-validation.service';
import {
  VlsTsValidationRequest,
  VlsTsValidationResponse,
} from '../models/vls-ts-validation.model';

describe('VlsTsValidationService', () => {
  let service: VlsTsValidationService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/vls-ts-validation-analysis';

  function response(overrides: Partial<VlsTsValidationResponse> = {}): VlsTsValidationResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2026-01-15',
      typeVlsTs: 'ETUDIANT',
      validationOFIIEffectuee: false,
      dateValidationOFII: null,
      country: 'FRANCE',
      statut: 'A_VALIDER',
      dateEcheanceValidation: '2026-04-15',
      joursRestantsValidation: 45,
      risqueIrregularite: false,
      procedureRecours: null,
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [VlsTsValidationService],
    });
    service = TestBed.inject(VlsTsValidationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(VlsTsValidationService.STANDALONE_TOOL_ID).toBe('F-IM-28-vls-ts-validation-ofii-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: VlsTsValidationRequest = {
      dateEntreeFrance: '2026-01-15',
      typeVlsTs: 'ETUDIANT',
      validationOFIIEffectuee: false,
      dateValidationOFII: null,
    };
    let result: VlsTsValidationResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('A_VALIDER');
    expect(result!.joursRestantsValidation).toBe(45);
  });

  it('get() GETs from the right URL', () => {
    let result: VlsTsValidationResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'EXPIRE', joursRestantsValidation: -10, procedureRecours: 'Recours gracieux préfecture' }));
    expect(result!.statut).toBe('EXPIRE');
    expect(result!.joursRestantsValidation).toBe(-10);
    expect(result!.procedureRecours).toBe('Recours gracieux préfecture');
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
