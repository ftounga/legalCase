import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { NaturalisationRecoursTjService } from './naturalisation-recours-tj.service';
import {
  NaturalisationRecoursTjRequest,
  NaturalisationRecoursTjResponse,
} from '../models/naturalisation-recours-tj.model';

describe('NaturalisationRecoursTjService', () => {
  let service: NaturalisationRecoursTjService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/naturalisation-recours-tj-analysis';

  function response(overrides: Partial<NaturalisationRecoursTjResponse> = {}): NaturalisationRecoursTjResponse {
    return {
      caseFileId: 'case-1',
      voieNaturalisation: 'MARIAGE',
      dateRefusDeclaration: '2026-01-15',
      typeRefus: 'REFUS_ENREGISTREMENT',
      country: 'FRANCE',
      statut: 'RECOURS_POSSIBLE',
      dateEcheanceRecoursJudicaire: '2026-07-15',
      joursRestants: 120,
      tribunalCompetent: 'Tribunal judiciaire de Paris',
      basesJuridiques: ['C. civ. art. 26-3', 'C. civ. art. 26-4'],
      motifsRecoursDisponibles: ["Erreur d'appréciation", 'Vice de procédure'],
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NaturalisationRecoursTjService],
    });
    service = TestBed.inject(NaturalisationRecoursTjService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(NaturalisationRecoursTjService.STANDALONE_TOOL_ID).toBe('F-IM-39-naturalisation-recours-tj-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: NaturalisationRecoursTjRequest = {
      voieNaturalisation: 'MARIAGE',
      dateRefusDeclaration: '2026-01-15',
      typeRefus: 'REFUS_ENREGISTREMENT',
    };
    let result: NaturalisationRecoursTjResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('RECOURS_POSSIBLE');
    expect(result!.joursRestants).toBe(120);
    expect(result!.tribunalCompetent).toBe('Tribunal judiciaire de Paris');
  });

  it('get() GETs from the right URL', () => {
    let result: NaturalisationRecoursTjResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'PRESCRIT', joursRestants: -10, motifsRecoursDisponibles: [] }));
    expect(result!.statut).toBe('PRESCRIT');
    expect(result!.joursRestants).toBe(-10);
    expect(result!.motifsRecoursDisponibles).toEqual([]);
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
