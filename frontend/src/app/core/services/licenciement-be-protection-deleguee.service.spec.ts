import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LicenciementBeProtectionDelegueeService } from './licenciement-be-protection-deleguee.service';
import {
  LicenciementBeProtectionDelegueeRequest,
  LicenciementBeProtectionDelegueeResponse,
} from '../models/licenciement-be-protection-deleguee.model';

describe('LicenciementBeProtectionDelegueeService', () => {
  let service: LicenciementBeProtectionDelegueeService;
  let httpMock: HttpTestingController;
  const BASE_URL =
    '/api/v1/case-files/case-42/decision-tools/licenciement-be-protection-deleguee';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [LicenciementBeProtectionDelegueeService],
    });
    service = TestBed.inject(LicenciementBeProtectionDelegueeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('POST envoie le body au bon endpoint et retourne la réponse', () => {
    const request: LicenciementBeProtectionDelegueeRequest = {
      statutProtege: 'DELEGUE_SYNDICAL_ELU',
      ancienneteAnnees: 8,
      remunerationAnnuelleBrute: 42000,
      dateLicenciement: '2026-05-10',
      dateElectionOuMandat: '2024-05-15',
      demandeReintegrationDansTrente: true,
      employeurRefuseReintegration: true,
      circonstancesAggravantes: false,
    };
    const response: LicenciementBeProtectionDelegueeResponse = {
      caseFileId: 'case-42',
      statutProtege: 'DELEGUE_SYNDICAL_ELU',
      ancienneteAnnees: 8,
      remunerationAnnuelleBrute: 42000,
      dateLicenciement: '2026-05-10',
      dateElectionOuMandat: '2024-05-15',
      demandeReintegrationDansTrente: true,
      employeurRefuseReintegration: true,
      circonstancesAggravantes: false,
      verdict: 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE',
      licenciementDansProtection: true,
      dateFinProtection: '2028-05-15',
      indemniteForfaitaire: 84000,
      anneesForfait: 2,
      dateLimiteDemandeReintegration: '2026-06-09',
      joursRestantsReintegration: 13,
      delaiReintegrationDepasse: false,
      baseJuridique: 'Loi du 19/03/1991 ; CCT n° 5 du 24/05/1971',
      avertissement: null,
    };

    let received: LicenciementBeProtectionDelegueeResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);

    expect(received).toEqual(response);
  });

  it('GET utilise le même path et retourne la dernière analyse', () => {
    const response: LicenciementBeProtectionDelegueeResponse = {
      caseFileId: 'case-42',
      statutProtege: 'CANDIDAT_NON_ELU',
      ancienneteAnnees: 2,
      remunerationAnnuelleBrute: 28000,
      dateLicenciement: '2026-04-01',
      dateElectionOuMandat: '2024-05-15',
      demandeReintegrationDansTrente: false,
      employeurRefuseReintegration: false,
      circonstancesAggravantes: false,
      verdict: 'HORS_PERIODE_PROTECTION',
      licenciementDansProtection: false,
      dateFinProtection: '2026-05-15',
      indemniteForfaitaire: null,
      anneesForfait: null,
      dateLimiteDemandeReintegration: null,
      joursRestantsReintegration: null,
      delaiReintegrationDepasse: false,
      baseJuridique: 'Loi du 19/03/1991 ; CCT n° 5 du 24/05/1971',
      avertissement: null,
    };

    let received: LicenciementBeProtectionDelegueeResponse | undefined;
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

  it('POST circonstances aggravantes → response forfait 4 ans', () => {
    const request: LicenciementBeProtectionDelegueeRequest = {
      statutProtege: 'DELEGUE_SYNDICAL_ELU',
      ancienneteAnnees: 10,
      remunerationAnnuelleBrute: 50000,
      dateLicenciement: '2026-05-10',
      dateElectionOuMandat: '2024-05-15',
      demandeReintegrationDansTrente: true,
      employeurRefuseReintegration: false,
      circonstancesAggravantes: true,
    };
    let received: LicenciementBeProtectionDelegueeResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.circonstancesAggravantes).toBe(true);
    req.flush({
      caseFileId: 'case-42',
      ...request,
      verdict: 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE',
      licenciementDansProtection: true,
      dateFinProtection: '2028-05-15',
      indemniteForfaitaire: 200000,
      anneesForfait: 4,
      dateLimiteDemandeReintegration: '2026-06-09',
      joursRestantsReintegration: 13,
      delaiReintegrationDepasse: false,
      baseJuridique: 'Loi du 19/03/1991 ; CCT n° 5 du 24/05/1971',
      avertissement: null,
    } as LicenciementBeProtectionDelegueeResponse);
    expect(received!.anneesForfait).toBe(4);
    expect(received!.indemniteForfaitaire).toBe(200000);
  });
});
