import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { PourvoiCassationSocService } from './pourvoi-cassation-soc.service';
import {
  PourvoiCassationSocRequest,
  PourvoiCassationSocResponse,
} from '../models/pourvoi-cassation-soc.model';

describe('PourvoiCassationSocService', () => {
  let service: PourvoiCassationSocService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/pourvoi-cassation-soc-analysis';

  function response(overrides: Partial<PourvoiCassationSocResponse> = {}): PourvoiCassationSocResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationArret: '2026-01-15',
      casOuverture: ['VIOLATION_LOI'],
      representationAvocatCassation: true,
      moyenSerieuxIdentifie: true,
      dateLimitePourvoi: '2026-03-15',
      joursRestants: 40,
      verdictDelai: 'DELAI_OUVERT',
      verdict: 'POURVOI_RECOMMANDE',
      risqueNonAdmission: 'FAIBLE',
      casOuvertureAnalyses: [
        { cas: 'VIOLATION_LOI', libelle: 'Violation de la loi', baseJuridique: 'art. 604 CPC', forceProbatoire: 'FORTE' },
      ],
      itemBloquantRepresentation: null,
      country: 'FRANCE',
      baseJuridique: 'art. 612 CPC ; art. 973 CPC ; art. 1014 CPC',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PourvoiCassationSocService],
    });
    service = TestBed.inject(PourvoiCassationSocService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(PourvoiCassationSocService.STANDALONE_TOOL_ID).toBe('F-DT-87-pourvoi-cassation-soc');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: PourvoiCassationSocRequest = {
      dateNotificationArret: '2026-01-15',
      casOuverture: ['VIOLATION_LOI'],
      representationAvocatCassation: true,
      moyenSerieuxIdentifie: true,
    };
    let result: PourvoiCassationSocResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.verdict).toBe('POURVOI_RECOMMANDE');
    expect(result!.risqueNonAdmission).toBe('FAIBLE');
    expect(result!.dateLimitePourvoi).toBe('2026-03-15');
    expect(result!.casOuvertureAnalyses.length).toBe(1);
  });

  it('get() GETs from the right URL and maps a POURVOI_RISQUE / ELEVE verdict', () => {
    let result: PourvoiCassationSocResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      verdict: 'POURVOI_RISQUE',
      risqueNonAdmission: 'ELEVE',
      casOuverture: ['DENATURATION'],
      moyenSerieuxIdentifie: false,
      casOuvertureAnalyses: [
        { cas: 'DENATURATION', libelle: 'Dénaturation', baseJuridique: 'art. 604 CPC', forceProbatoire: 'MOYENNE' },
      ],
    }));
    expect(result!.verdict).toBe('POURVOI_RISQUE');
    expect(result!.risqueNonAdmission).toBe('ELEVE');
    expect(result!.casOuvertureAnalyses[0].forceProbatoire).toBe('MOYENNE');
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
