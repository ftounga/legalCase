import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ExecutionJugementCphService } from './execution-jugement-cph.service';
import {
  ExecutionJugementCphRequest,
  ExecutionJugementCphResponse,
} from '../models/execution-jugement-cph.model';

describe('ExecutionJugementCphService', () => {
  let service: ExecutionJugementCphService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/execution-jugement-cph-analysis';

  function response(overrides: Partial<ExecutionJugementCphResponse> = {}): ExecutionJugementCphResponse {
    return {
      caseFileId: 'case-1',
      dateJugement: '2026-01-15',
      montantCondamnation: 12000,
      executionProvisoireOrdonnee: true,
      situationEmployeur: 'IN_BONIS',
      dateOuvertureProcedureCollective: null,
      creancesSuperPrivilegiees: null,
      verdict: 'EXECUTION_DIRECTE',
      agsEligible: false,
      agsInfo: null,
      checklist: [
        { libelle: 'Signification du jugement', obligatoire: true, bloquant: false, baseJuridique: 'art. 503 CPC' },
      ],
      country: 'FRANCE',
      baseJuridique: 'art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 et s.',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExecutionJugementCphService],
    });
    service = TestBed.inject(ExecutionJugementCphService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(ExecutionJugementCphService.STANDALONE_TOOL_ID).toBe('F-DT-88-execution-jugement-cph');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: ExecutionJugementCphRequest = {
      dateJugement: '2026-01-15',
      montantCondamnation: 12000,
      executionProvisoireOrdonnee: true,
      situationEmployeur: 'IN_BONIS',
      dateOuvertureProcedureCollective: null,
      creancesSuperPrivilegiees: null,
    };
    let result: ExecutionJugementCphResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.verdict).toBe('EXECUTION_DIRECTE');
    expect(result!.agsEligible).toBe(false);
    expect(result!.checklist.length).toBe(1);
  });

  it('get() GETs and maps a RELAIS_AGS verdict with agsInfo plafonds', () => {
    let result: ExecutionJugementCphResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      situationEmployeur: 'LIQUIDATION',
      dateOuvertureProcedureCollective: '2026-01-05',
      verdict: 'RELAIS_AGS',
      agsEligible: true,
      agsInfo: {
        plafondMensuelSs: 3925,
        plafondGarantie: 94200,
        coefficientPlafond: 6,
        relaisAgsRecommande: true,
        demarches: [
          { libelle: 'Déclaration de créance au mandataire', obligatoire: true, bloquant: false, baseJuridique: 'L. 622-24 C. com.' },
        ],
      },
    }));
    expect(result!.verdict).toBe('RELAIS_AGS');
    expect(result!.agsEligible).toBe(true);
    expect(result!.agsInfo!.plafondGarantie).toBe(94200);
    expect(result!.agsInfo!.coefficientPlafond).toBe(6);
  });

  it('propagates backend error to the subscriber', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({
      next: () => fail('should not succeed'),
      error: (e) => (errStatus = e.status),
    });
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 400, statusText: 'Bad Request' });
    expect(errStatus).toBe(400);
  });
});
