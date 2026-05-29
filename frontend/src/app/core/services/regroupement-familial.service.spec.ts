import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RegroupementFamilialService } from './regroupement-familial.service';
import {
  RegroupementFamilialRequest,
  RegroupementFamilialResponse,
} from '../models/regroupement-familial.model';

describe('RegroupementFamilialService', () => {
  let service: RegroupementFamilialService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/regroupement-familial-analysis';

  function response(overrides: Partial<RegroupementFamilialResponse> = {}): RegroupementFamilialResponse {
    return {
      caseFileId: 'case-1',
      dureeSejourRegulierMois: 24,
      ressourcesMensuellesNettes: 1800,
      tailleLogementM2: 60,
      nombrePersonnesFoyer: 2,
      typeRegroupement: 'CONJOINT',
      membresFamilleARegrouper: 2,
      country: 'FRANCE',
      verdict: 'ELIGIBLE',
      ressourcesRequises: 1766.92,
      surfaceRequise: 32,
      chipsCriteresNonRemplis: [],
      baseJuridique: 'CESEDA L.434-1 à L.434-10',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RegroupementFamilialService],
    });
    service = TestBed.inject(RegroupementFamilialService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(RegroupementFamilialService.STANDALONE_TOOL_ID).toBe('F-IM-26-regroupement-familial-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: RegroupementFamilialRequest = {
      dureeSejourRegulierMois: 24,
      ressourcesMensuellesNettes: 1800,
      tailleLogementM2: 60,
      nombrePersonnesFoyer: 2,
      typeRegroupement: 'CONJOINT',
      membresFamilleARegrouper: 2,
    };
    let result: RegroupementFamilialResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.verdict).toBe('ELIGIBLE');
    expect(result!.ressourcesRequises).toBe(1766.92);
  });

  it('get() GETs from the right URL', () => {
    let result: RegroupementFamilialResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ verdict: 'NON_ELIGIBLE_RESSOURCES', surfaceRequise: 32 }));
    expect(result!.verdict).toBe('NON_ELIGIBLE_RESSOURCES');
    expect(result!.surfaceRequise).toBe(32);
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
