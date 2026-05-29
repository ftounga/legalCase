import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AppelCaaCassationService } from './appel-caa-cassation.service';
import {
  AppelCaaCassationRequest,
  AppelCaaCassationResponse,
} from '../models/appel-caa-cassation.model';

describe('AppelCaaCassationService', () => {
  let service: AppelCaaCassationService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/appel-caa-cassation-analysis';

  function response(overrides: Partial<AppelCaaCassationResponse> = {}): AppelCaaCassationResponse {
    return {
      caseFileId: 'case-1',
      dateJugementTA: '2026-01-15',
      typeDecisionTA: 'REJET',
      typeContentieux: 'OQTF',
      delaiSpecialOQTF: true,
      country: 'FRANCE',
      statut: 'APPEL_POSSIBLE',
      dateEcheanceAppelCaa: '2026-01-30',
      joursRestants: 12,
      courAppelCompetente: "Cour administrative d'appel de Paris",
      motifsAppelPossibles: ["Erreur de droit", "Erreur d'appréciation"],
      filtrePourvoisCassation: true,
      delaiCassationCeMois: 2,
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AppelCaaCassationService],
    });
    service = TestBed.inject(AppelCaaCassationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(AppelCaaCassationService.STANDALONE_TOOL_ID).toBe('F-IM-41-appel-caa-cassation-ce-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: AppelCaaCassationRequest = {
      dateJugementTA: '2026-01-15',
      typeDecisionTA: 'REJET',
      typeContentieux: 'OQTF',
      delaiSpecialOQTF: true,
    };
    let result: AppelCaaCassationResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('APPEL_POSSIBLE');
    expect(result!.joursRestants).toBe(12);
    expect(result!.filtrePourvoisCassation).toBe(true);
    expect(result!.delaiCassationCeMois).toBe(2);
  });

  it('get() GETs from the right URL', () => {
    let result: AppelCaaCassationResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'PRESCRIT', joursRestants: -10, motifsAppelPossibles: [], filtrePourvoisCassation: false }));
    expect(result!.statut).toBe('PRESCRIT');
    expect(result!.joursRestants).toBe(-10);
    expect(result!.motifsAppelPossibles).toEqual([]);
    expect(result!.filtrePourvoisCassation).toBe(false);
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
