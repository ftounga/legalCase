import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AccordEntrepriseValiditeService } from './accord-entreprise-validite.service';
import {
  AccordEntrepriseValiditeRequest,
  AccordEntrepriseValiditeResponse,
} from '../models/accord-entreprise-validite.model';

describe('AccordEntrepriseValiditeService', () => {
  let service: AccordEntrepriseValiditeService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/accord-entreprise-validite-analysis';

  function response(overrides: Partial<AccordEntrepriseValiditeResponse> = {}): AccordEntrepriseValiditeResponse {
    return {
      caseFileId: 'case-1',
      pourcentageSuffragesSignataires: 55,
      typeOperation: 'CONCLUSION',
      referendumOrganise: false,
      referendumApprouve: false,
      conditionMajorite: 'MAJORITE_50',
      dateDenonciation: null,
      dateFinSurvie: null,
      checklist: [
        { item: 'Majorité des suffrages exprimés', conforme: true, commentaire: '' },
      ],
      itemsNonConformes: 0,
      statut: 'VALIDE',
      consequences: [],
      country: 'FRANCE',
      baseJuridique: 'Art. L.2232-12 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AccordEntrepriseValiditeService],
    });
    service = TestBed.inject(AccordEntrepriseValiditeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(AccordEntrepriseValiditeService.STANDALONE_TOOL_ID).toBe('F-DT-67-accord-entreprise-validite');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: AccordEntrepriseValiditeRequest = {
      pourcentageSuffragesSignataires: 55,
      referendumOrganise: false,
      referendumApprouve: false,
      typeOperation: 'CONCLUSION',
      signePartiesHabilitees: false,
      preavisDenonciationRespecte: true,
      dateDenonciation: null,
    };
    let received: AccordEntrepriseValiditeResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.statut).toBe('VALIDE');
    expect(received!.conditionMajorite).toBe('MAJORITE_50');
  });

  it('get() maps a VALIDE_SOUS_RESERVE result (référendum 30 %)', () => {
    let received: AccordEntrepriseValiditeResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      pourcentageSuffragesSignataires: 35,
      referendumOrganise: true,
      referendumApprouve: true,
      conditionMajorite: 'REFERENDUM_30',
      statut: 'VALIDE_SOUS_RESERVE',
    }));
    expect(received!.statut).toBe('VALIDE_SOUS_RESERVE');
    expect(received!.conditionMajorite).toBe('REFERENDUM_30');
  });

  it('get() maps a NON_VALIDE result (majorité insuffisante)', () => {
    let received: AccordEntrepriseValiditeResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      pourcentageSuffragesSignataires: 35,
      conditionMajorite: 'INSUFFISANTE',
      itemsNonConformes: 1,
      statut: 'NON_VALIDE',
    }));
    expect(received!.statut).toBe('NON_VALIDE');
    expect(received!.conditionMajorite).toBe('INSUFFISANTE');
    expect(received!.itemsNonConformes).toBe(1);
  });

  it('get() maps a DENONCIATION result with dateFinSurvie', () => {
    let received: AccordEntrepriseValiditeResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      typeOperation: 'DENONCIATION',
      dateDenonciation: '2026-01-01',
      dateFinSurvie: '2027-04-01',
    }));
    expect(received!.typeOperation).toBe('DENONCIATION');
    expect(received!.dateFinSurvie).toBe('2027-04-01');
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
