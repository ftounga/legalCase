import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ClauseNonConcurrenceBeService } from './clause-non-concurrence-be.service';
import {
  ClauseNonConcurrenceBeRequest,
  ClauseNonConcurrenceBeResponse,
} from '../models/clause-non-concurrence-be.model';

describe('ClauseNonConcurrenceBeService', () => {
  let service: ClauseNonConcurrenceBeService;
  let httpMock: HttpTestingController;
  const BASE_URL = '/api/v1/case-files/case-42/decision-tools/clause-non-concurrence-be';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ClauseNonConcurrenceBeService],
    });
    service = TestBed.inject(ClauseNonConcurrenceBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('POST envoie le body au bon endpoint et retourne la réponse', () => {
    const request: ClauseNonConcurrenceBeRequest = {
      remunerationAnnuelleBrute: 100000,
      dureeMois: 6,
      zoneGeographique: 'BELGIQUE_UNIQUEMENT',
      activiteInternationaleProuvee: false,
    };
    const response: ClauseNonConcurrenceBeResponse = {
      verdict: 'VALIDE',
      raisonNullite: null,
      indemniteLegale: 25000,
      indemniteLegaleFormule: '(100000 € / 12) * (6 / 2) = 25 000,00 €',
      baseJuridique: 'Loi 03/07/1978 art. 65 ; CCT n°13 du 24/02/1971',
      avertissement: 'Seuil 2024 : 73 571 € — à vérifier selon année de signature.',
    };

    let received: ClauseNonConcurrenceBeResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);

    expect(received).toEqual(response);
  });

  it('GET utilise le même path et retourne la dernière analyse', () => {
    const response: ClauseNonConcurrenceBeResponse = {
      verdict: 'NULLE',
      raisonNullite: 'REMUNERATION_INSUFFISANTE',
      indemniteLegale: 0,
      indemniteLegaleFormule: '',
      baseJuridique: 'Loi 03/07/1978 art. 65',
      avertissement: '',
    };

    let received: ClauseNonConcurrenceBeResponse | undefined;
    service.get('case-42').subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response);

    expect(received).toEqual(response);
  });

  it('GET 404 propage l\'erreur', () => {
    let errorStatus: number | undefined;
    service.get('case-42').subscribe({
      next: () => fail('attendu 404'),
      error: (err) => (errorStatus = err.status),
    });
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(errorStatus).toBe(404);
  });
});
