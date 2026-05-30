import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { StagiaireGratificationService } from './stagiaire-gratification.service';
import {
  StagiaireGratificationRequest,
  StagiaireGratificationResponse,
} from '../models/stagiaire-gratification.model';

describe('StagiaireGratificationService', () => {
  let service: StagiaireGratificationService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/stagiaire-gratification-analysis';

  function response(): StagiaireGratificationResponse {
    return {
      caseFileId: 'case-1',
      dateDebutStage: '2026-01-06',
      dateFinStage: '2026-05-06',
      nombreJoursPresence: 80,
      gratificationMensuelleVersee: 0,
      tauxHoraireConventionnel: null,
      missionsHorsProjetPedagogique: false,
      posteTravailPermanent: false,
      gratificationObligatoire: true,
      seuilAtteint: true,
      gratificationMinimaleDue: 2400,
      rappelGratification: 2400,
      risqueRequalification: 'FAIBLE',
      motifs: [],
      verdictGlobal: 'RAPPEL_GRATIFICATION',
      country: 'FRANCE',
      baseJuridique: 'Art. L.124-6 Code de l\'éducation (à vérifier par avocat)',
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [StagiaireGratificationService],
    });
    service = TestBed.inject(StagiaireGratificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes the STANDALONE_TOOL_ID aligned on TOOL_REGISTRY', () => {
    expect(StagiaireGratificationService.STANDALONE_TOOL_ID)
      .toBe('F-DT-109-stagiaire-gratification-requalification');
  });

  it('analyze() POSTs the request to the analysis endpoint', () => {
    const request: StagiaireGratificationRequest = {
      dateDebutStage: '2026-01-06',
      dateFinStage: '2026-05-06',
      nombreJoursPresence: 80,
      gratificationMensuelleVersee: 0,
      tauxHoraireConventionnel: null,
      missionsHorsProjetPedagogique: false,
      posteTravailPermanent: false,
    };
    let received: StagiaireGratificationResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.verdictGlobal).toBe('RAPPEL_GRATIFICATION');
    expect(received!.rappelGratification).toBe(2400);
  });

  it('get() GETs the analysis endpoint', () => {
    let received: StagiaireGratificationResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    expect(received!.caseFileId).toBe('case-1');
  });

  it('get() propagates a 404 when no analysis exists', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({ error: (e) => (errStatus = e.status) });
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'Not Found' });
    expect(errStatus).toBe(404);
  });
});
