import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RappelSalaireBeService } from './rappel-salaire-be.service';
import {
  RappelSalaireBeRequest,
  RappelSalaireBeResponse,
} from '../models/rappel-salaire-be.model';

describe('RappelSalaireBeService', () => {
  let service: RappelSalaireBeService;
  let httpMock: HttpTestingController;
  const BASE_URL = '/api/v1/case-files/case-42/decision-tools/rappel-salaire-be';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RappelSalaireBeService],
    });
    service = TestBed.inject(RappelSalaireBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('POST envoie le body au bon endpoint et retourne la réponse', () => {
    const request: RappelSalaireBeRequest = {
      montantBrut: 5000,
      dateDebutPeriode: '2025-01-01',
      dateFinPeriode: '2025-12-31',
      typeArriereEnum: 'PENDANT_CONTRAT',
    };
    const response: RappelSalaireBeResponse = {
      montantBrut: 5000,
      interetsCourus: 500,
      tauxMoratoire: '10% (Loi 12/04/1965 art. 10)',
      totalReclame: 5500,
      dateLimitePrescription: '2030-12-31',
      joursRestantsAvantPrescription: 1500,
      statutPrescription: 'NON_PRESCRIT',
      baseJuridique: 'Loi 12/04/1965 art. 10 ; Loi 03/07/1978 art. 15',
      formuleCalcul: '5000 € × 10% × 1 an = 500 € intérêts ; total = 5500 €',
    };

    let received: RappelSalaireBeResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);

    expect(received).toEqual(response);
  });

  it('GET utilise le même path et retourne la dernière analyse', () => {
    const response: RappelSalaireBeResponse = {
      montantBrut: 3000,
      interetsCourus: 0,
      tauxMoratoire: '10% (Loi 12/04/1965 art. 10)',
      totalReclame: 3000,
      dateLimitePrescription: '2026-06-30',
      joursRestantsAvantPrescription: 15,
      statutPrescription: 'IMMINENT',
      baseJuridique: 'Loi 03/07/1978 art. 15',
      formuleCalcul: '',
    };

    let received: RappelSalaireBeResponse | undefined;
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
