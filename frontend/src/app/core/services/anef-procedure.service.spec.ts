import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AnefProcedureService } from './anef-procedure.service';
import {
  AnefProcedureRequest,
  AnefProcedureResponse,
} from '../models/anef-procedure.model';

/**
 * SF-214-26 — Tests du wrapper HttpClient F-IM-37 (ANEF procédure / pannes, FR).
 */
describe('AnefProcedureService', () => {
  let service: AnefProcedureService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/anef-procedure-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnefProcedureService],
    });
    service = TestBed.inject(AnefProcedureService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('STANDALONE_TOOL_ID aligné sur la clé TOOL_REGISTRY', () => {
    expect(AnefProcedureService.STANDALONE_TOOL_ID)
      .toBe('F-IM-37-anef-procedure-fr');
  });

  it('analyze : POST vers l\'endpoint figé avec le corps fourni', () => {
    const request: AnefProcedureRequest = {
      typeTitreConcerne: 'VPF',
      dateExpirationTitre: '2026-03-10',
      panneeANEFSignalee: true,
      dateTentativeDepot: '2026-02-01',
      demandeAdresseePrefecture: true,
    };
    const response = { caseFileId: 'case-1' } as AnefProcedureResponse;
    service.analyze('case-1', request).subscribe((r) => expect(r).toEqual(response));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeTitreConcerne).toBe('VPF');
    expect(req.request.body.panneeANEFSignalee).toBe(true);
    req.flush(response);
  });

  it('get : GET vers l\'endpoint figé', () => {
    const response = { caseFileId: 'case-1' } as AnefProcedureResponse;
    service.get('case-1').subscribe((r) => expect(r).toEqual(response));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });

  it('analyze : propage une erreur HTTP', () => {
    const request: AnefProcedureRequest = {
      typeTitreConcerne: 'VPF',
      dateExpirationTitre: '2026-03-10',
      panneeANEFSignalee: false,
      demandeAdresseePrefecture: false,
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
