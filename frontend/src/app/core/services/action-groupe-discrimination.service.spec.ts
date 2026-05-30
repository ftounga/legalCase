import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ActionGroupeDiscriminationService } from './action-groupe-discrimination.service';
import {
  ActionGroupeDiscriminationRequest,
  ActionGroupeDiscriminationResponse,
} from '../models/action-groupe-discrimination.model';

describe('ActionGroupeDiscriminationService', () => {
  let service: ActionGroupeDiscriminationService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/action-groupe-discrimination-analysis';

  function response(overrides: Partial<ActionGroupeDiscriminationResponse> = {}): ActionGroupeDiscriminationResponse {
    return {
      caseFileId: 'case-1',
      typeOrganisation: 'SYNDICAT_REPRESENTATIF',
      dateMiseEnDemeure: '2025-06-01',
      motifDiscrimination: 'SEXE',
      nombrePersonnesConcernees: 5,
      objetAction: 'LES_DEUX',
      verdict: 'RECEVABLE',
      qualiteAAgir: true,
      pluraliteEtablie: true,
      dateRecevabiliteSaisine: '2025-12-01',
      delaiCarenceRespecte: true,
      checklist: [
        { libelle: 'Mise en demeure écrite', obligatoire: true, bloquant: false, baseJuridique: 'L. 1134-9' },
      ],
      country: 'FRANCE',
      baseJuridique: 'L. 1134-7 à L. 1134-10 ; L. 1132-1 Code travail',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ActionGroupeDiscriminationService],
    });
    service = TestBed.inject(ActionGroupeDiscriminationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(ActionGroupeDiscriminationService.STANDALONE_TOOL_ID).toBe('F-DT-90-action-groupe-discrimination');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: ActionGroupeDiscriminationRequest = {
      typeOrganisation: 'SYNDICAT_REPRESENTATIF',
      dateMiseEnDemeure: '2025-06-01',
      motifDiscrimination: 'SEXE',
      nombrePersonnesConcernees: 5,
      objetAction: 'LES_DEUX',
    };
    let received: ActionGroupeDiscriminationResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.verdict).toBe('RECEVABLE');
    expect(received!.dateRecevabiliteSaisine).toBe('2025-12-01');
  });

  it('get() GETs the figured endpoint', () => {
    let received: ActionGroupeDiscriminationResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ verdict: 'PREMATURE', delaiCarenceRespecte: false }));
    expect(received!.verdict).toBe('PREMATURE');
    expect(received!.delaiCarenceRespecte).toBe(false);
  });

  it('get() propagates a 404 error when no analysis exists', () => {
    let errorStatus: number | undefined;
    service.get('case-1').subscribe({
      next: () => fail('should have errored'),
      error: (err) => (errorStatus = err.status),
    });
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(errorStatus).toBe(404);
  });
});
