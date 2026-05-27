import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LicenciementBeActeEquivalentService } from './licenciement-be-acte-equivalent.service';
import {
  LicenciementBeActeEquivalentRequest,
  LicenciementBeActeEquivalentResponse,
} from '../models/licenciement-be-acte-equivalent.model';

describe('LicenciementBeActeEquivalentService', () => {
  let service: LicenciementBeActeEquivalentService;
  let httpMock: HttpTestingController;
  const BASE_URL =
    '/api/v1/case-files/case-42/decision-tools/licenciement-be-acte-equivalent';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [LicenciementBeActeEquivalentService],
    });
    service = TestBed.inject(LicenciementBeActeEquivalentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('POST envoie le body au bon endpoint et retourne la réponse', () => {
    const request: LicenciementBeActeEquivalentRequest = {
      typeModification: 'SALAIRE',
      ampleurModification: 'SUBSTANTIELLE',
      dateModification: '2026-04-15',
      elementEssentielDuContrat: true,
      salarieAProteste: true,
      delaiDepuisModificationJours: 12,
      remunerationHebdomadaireBrute: 1000,
      dureePreavisCalculeeSemaines: 16,
    };
    const response: LicenciementBeActeEquivalentResponse = {
      caseFileId: 'case-42',
      typeModification: 'SALAIRE',
      ampleurModification: 'SUBSTANTIELLE',
      elementEssentielDuContrat: true,
      dateModification: '2026-04-15',
      salarieAProteste: true,
      delaiDepuisModificationJours: 12,
      remunerationHebdomadaireBrute: 1000,
      dureePreavisCalculeeSemaines: 16,
      verdict: 'ACTE_EQUIPOLLENT_PROBABLE',
      fondementJuridique: 'Loi 03/07/1978 art. 20 + Cass. BE 23/12/1957',
      icpIndicatif: 16000,
      risqueAcceptationTacite: false,
      delaiRecommandeProtestationJours: 30,
      baseJuridique: 'Loi 03/07/1978 art. 20',
      avertissement: null,
    };

    let received: LicenciementBeActeEquivalentResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);

    expect(received).toEqual(response);
  });

  it('GET utilise le même path et retourne la dernière analyse', () => {
    const response: LicenciementBeActeEquivalentResponse = {
      caseFileId: 'case-42',
      typeModification: 'HORAIRE',
      ampleurModification: 'MINEURE',
      elementEssentielDuContrat: false,
      dateModification: '2026-02-01',
      salarieAProteste: false,
      delaiDepuisModificationJours: 60,
      remunerationHebdomadaireBrute: null,
      dureePreavisCalculeeSemaines: null,
      verdict: 'PAS_ACTE_EQUIPOLLENT',
      fondementJuridique: 'Loi 03/07/1978 art. 20 — Ius Variandi',
      icpIndicatif: null,
      risqueAcceptationTacite: false,
      delaiRecommandeProtestationJours: 30,
      baseJuridique: 'Loi 03/07/1978 art. 20',
      avertissement: null,
    };

    let received: LicenciementBeActeEquivalentResponse | undefined;
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

  it('POST silence prolongé → response RISQUE_ACCEPTATION_TACITE', () => {
    const request: LicenciementBeActeEquivalentRequest = {
      typeModification: 'FONCTION',
      ampleurModification: 'SUBSTANTIELLE',
      dateModification: '2026-01-15',
      elementEssentielDuContrat: true,
      salarieAProteste: false,
      delaiDepuisModificationJours: 60,
      remunerationHebdomadaireBrute: null,
      dureePreavisCalculeeSemaines: null,
    };
    let received: LicenciementBeActeEquivalentResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.salarieAProteste).toBe(false);
    req.flush({
      caseFileId: 'case-42',
      ...request,
      verdict: 'RISQUE_ACCEPTATION_TACITE',
      fondementJuridique: 'Loi 03/07/1978 art. 20 — risque acceptation tacite',
      icpIndicatif: null,
      risqueAcceptationTacite: true,
      delaiRecommandeProtestationJours: 30,
      baseJuridique: 'Loi 03/07/1978 art. 20',
      avertissement: 'Silence prolongé > 30 j',
    } as LicenciementBeActeEquivalentResponse);
    expect(received!.verdict).toBe('RISQUE_ACCEPTATION_TACITE');
    expect(received!.risqueAcceptationTacite).toBe(true);
  });

  it('POST ampleur INDETERMINEES → response A_ANALYSER', () => {
    const request: LicenciementBeActeEquivalentRequest = {
      typeModification: 'AUTRE',
      ampleurModification: 'INDETERMINEES',
      dateModification: '2026-04-01',
      elementEssentielDuContrat: false,
      salarieAProteste: true,
      delaiDepuisModificationJours: null,
      remunerationHebdomadaireBrute: null,
      dureePreavisCalculeeSemaines: null,
    };
    let received: LicenciementBeActeEquivalentResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    req.flush({
      caseFileId: 'case-42',
      ...request,
      verdict: 'A_ANALYSER',
      fondementJuridique: 'Loi 03/07/1978 art. 20 — analyse approfondie requise',
      icpIndicatif: null,
      risqueAcceptationTacite: false,
      delaiRecommandeProtestationJours: 30,
      baseJuridique: 'Loi 03/07/1978 art. 20',
      avertissement: null,
    } as LicenciementBeActeEquivalentResponse);
    expect(received!.verdict).toBe('A_ANALYSER');
  });
});
