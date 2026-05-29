import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { MnaEvaluationAgeService } from './mna-evaluation-age.service';
import { MnaEvaluationAgeRequest, MnaEvaluationAgeResponse } from '../models/mna-evaluation-age.model';

describe('MnaEvaluationAgeService', () => {
  let service: MnaEvaluationAgeService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/mna-evaluation-age-analysis';

  function response(overrides: Partial<MnaEvaluationAgeResponse> = {}): MnaEvaluationAgeResponse {
    return {
      caseFileId: 'case-1',
      dateNaissanceDeclaree: '2010-06-15',
      evaluationASERefusee: true,
      dateRefusASE: '2026-05-01',
      examenOsseuxOrdonne: false,
      resultatExamenOsseux: null,
      country: 'FRANCE',
      statut: 'RECOURS_JE_URGENT',
      dateEcheanceSaisineJE: '2026-05-31',
      contestationExamenOsseux: [],
      procedureASE: ['Saisir le juge des enfants', 'Demander une mesure de placement provisoire'],
      droitsAttaches: ['Scolarisation', 'Prise en charge ASE'],
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MnaEvaluationAgeService],
    });
    service = TestBed.inject(MnaEvaluationAgeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(MnaEvaluationAgeService.STANDALONE_TOOL_ID).toBe('F-IM-38-mna-evaluation-age-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: MnaEvaluationAgeRequest = {
      dateNaissanceDeclaree: '2010-06-15',
      evaluationASERefusee: true,
      dateRefusASE: '2026-05-01',
      examenOsseuxOrdonne: false,
      resultatExamenOsseux: null,
    };
    let result: MnaEvaluationAgeResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('RECOURS_JE_URGENT');
    expect(result!.dateEcheanceSaisineJE).toBe('2026-05-31');
    expect(result!.procedureASE.length).toBe(2);
  });

  it('get() GETs from the right URL', () => {
    let result: MnaEvaluationAgeResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      statut: 'EXAMEN_OSSEUX_CONTESTE',
      examenOsseuxOrdonne: true,
      resultatExamenOsseux: 'Âge estimé 18-20 ans',
      contestationExamenOsseux: ['Marge d’erreur scientifique', 'Doute profite au mineur'],
    }));
    expect(result!.statut).toBe('EXAMEN_OSSEUX_CONTESTE');
    expect(result!.contestationExamenOsseux.length).toBe(2);
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
