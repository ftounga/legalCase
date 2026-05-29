import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AjCndaService } from './aj-cnda.service';
import { AjCndaRequest, AjCndaResponse } from '../models/aj-cnda.model';

describe('AjCndaService', () => {
  let service: AjCndaService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/aj-cnda-analysis';

  function response(overrides: Partial<AjCndaResponse> = {}): AjCndaResponse {
    return {
      caseFileId: 'case-1',
      dateDecisionOFPRA: '2026-01-15',
      ressourcesMensuellesNettes: 800,
      procedureAcceleree: false,
      demandeAJDeposee: false,
      dateDepotAJ: null,
      country: 'FRANCE',
      statut: 'AJ_A_DEMANDER',
      eligibleAJ: true,
      dateEcheanceRecoursCNDA: '2026-02-15',
      dateEcheanceDemandeAJ: '2026-02-15',
      procedureAccelereeDureeReduite: false,
      piecesAJ: ['Décision OFPRA', "Justificatifs de ressources"],
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AjCndaService],
    });
    service = TestBed.inject(AjCndaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(AjCndaService.STANDALONE_TOOL_ID).toBe('F-IM-34-aj-cnda-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: AjCndaRequest = {
      dateDecisionOFPRA: '2026-01-15',
      ressourcesMensuellesNettes: 800,
      procedureAcceleree: false,
      demandeAJDeposee: false,
      dateDepotAJ: null,
    };
    let result: AjCndaResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('AJ_A_DEMANDER');
    expect(result!.eligibleAJ).toBe(true);
    expect(result!.piecesAJ.length).toBe(2);
  });

  it('get() GETs from the right URL', () => {
    let result: AjCndaResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'NON_ELIGIBLE_RESSOURCES', eligibleAJ: false }));
    expect(result!.statut).toBe('NON_ELIGIBLE_RESSOURCES');
    expect(result!.eligibleAJ).toBe(false);
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
