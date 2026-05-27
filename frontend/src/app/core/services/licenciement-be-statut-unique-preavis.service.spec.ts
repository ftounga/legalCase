import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LicenciementBeStatutUniquePreavisService } from './licenciement-be-statut-unique-preavis.service';
import {
  LicenciementBeStatutUniquePreavisRequest,
  LicenciementBeStatutUniquePreavisResponse,
} from '../models/licenciement-be-statut-unique-preavis.model';

describe('LicenciementBeStatutUniquePreavisService', () => {
  let service: LicenciementBeStatutUniquePreavisService;
  let httpMock: HttpTestingController;
  const BASE_URL =
    '/api/v1/case-files/case-42/decision-tools/licenciement-be-statut-unique-preavis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [LicenciementBeStatutUniquePreavisService],
    });
    service = TestBed.inject(LicenciementBeStatutUniquePreavisService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('POST envoie le body au bon endpoint et retourne la réponse', () => {
    const request: LicenciementBeStatutUniquePreavisRequest = {
      ancienneteAnnees: 5,
      ancienneteMoisSupplementaires: 6,
      salaireHebdomadaireBrut: 800,
      dateNotificationLicenciement: '2026-03-15',
      partieStatutUniqueSeulement: true,
    };
    const response: LicenciementBeStatutUniquePreavisResponse = {
      caseFileId: 'case-42',
      ancienneteAnnees: 5,
      ancienneteMoisSupplementaires: 6,
      salaireHebdomadaireBrut: 800,
      dateNotificationLicenciement: '2026-03-15',
      partieStatutUniqueSeulement: true,
      dureePreavisEnSemaines: 18,
      dateFinPreavis: '2026-07-19',
      indemniteCompensatoire: 14400,
      formuleCalcul:
        'Statut unique : 5 ans 6 mois → 18 semaines (Loi 03/07/1978 art. 37/2 §1er). Indemnité = 18 × 800 € = 14400 €',
      baseJuridique: 'Loi 03/07/1978 art. 37/2 §1er ; Loi 26/12/2013',
      avertissement: null,
    };

    let received: LicenciementBeStatutUniquePreavisResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);

    expect(received).toEqual(response);
  });

  it('GET utilise le même path et retourne la dernière analyse', () => {
    const response: LicenciementBeStatutUniquePreavisResponse = {
      caseFileId: 'case-42',
      ancienneteAnnees: 10,
      ancienneteMoisSupplementaires: 0,
      salaireHebdomadaireBrut: 1000,
      dateNotificationLicenciement: '2026-03-15',
      partieStatutUniqueSeulement: false,
      dureePreavisEnSemaines: 30,
      dateFinPreavis: '2026-10-12',
      indemniteCompensatoire: 30000,
      formuleCalcul: '...',
      baseJuridique: 'Loi 03/07/1978 art. 37/2 §1er ; Loi 26/12/2013',
      avertissement:
        'Contrat mixte : la fraction Claeys (pré-2014) doit être calculée séparément.',
    };

    let received: LicenciementBeStatutUniquePreavisResponse | undefined;
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
