import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { NaturalisationRecoursTaService } from './naturalisation-recours-ta.service';
import {
  NaturalisationRecoursTaRequest,
  NaturalisationRecoursTaResponse,
} from '../models/naturalisation-recours-ta.model';

describe('NaturalisationRecoursTaService', () => {
  let service: NaturalisationRecoursTaService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/naturalisation-recours-ta-analysis';

  function response(overrides: Partial<NaturalisationRecoursTaResponse> = {}): NaturalisationRecoursTaResponse {
    return {
      caseFileId: 'case-1',
      dateRefusDecret: '2026-01-15',
      motivationRefus: 'Défaut d\'assimilation',
      recoursPrerequis: true,
      country: 'FRANCE',
      statut: 'RECOURS_POSSIBLE',
      dateEcheanceRecoursTa: '2026-03-15',
      joursRestants: 45,
      tribunalCompetent: 'Tribunal administratif de Nantes',
      basesJuridiques: ['CJA art. R. 421-1'],
      motifsRecoursDisponibles: ["Erreur manifeste d'appréciation", 'Vice de procédure'],
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NaturalisationRecoursTaService],
    });
    service = TestBed.inject(NaturalisationRecoursTaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(NaturalisationRecoursTaService.STANDALONE_TOOL_ID).toBe('F-IM-40-naturalisation-recours-ta-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: NaturalisationRecoursTaRequest = {
      dateRefusDecret: '2026-01-15',
      motivationRefus: 'Défaut d\'assimilation',
      recoursPrerequis: true,
    };
    let result: NaturalisationRecoursTaResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('RECOURS_POSSIBLE');
    expect(result!.joursRestants).toBe(45);
    expect(result!.tribunalCompetent).toBe('Tribunal administratif de Nantes');
  });

  it('get() GETs from the right URL', () => {
    let result: NaturalisationRecoursTaResponse | undefined;
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
