import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LicenciementBeCct109DeraisonnableService } from './licenciement-be-cct109-deraisonnable.service';
import {
  LicenciementBeCct109DeraisonnableRequest,
  LicenciementBeCct109DeraisonnableResponse,
} from '../models/licenciement-be-cct109-deraisonnable.model';

describe('LicenciementBeCct109DeraisonnableService', () => {
  let service: LicenciementBeCct109DeraisonnableService;
  let httpMock: HttpTestingController;
  const BASE_URL =
    '/api/v1/case-files/case-42/decision-tools/licenciement-be-cct109-deraisonnable';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [LicenciementBeCct109DeraisonnableService],
    });
    service = TestBed.inject(LicenciementBeCct109DeraisonnableService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('POST envoie le body au bon endpoint et retourne la réponse', () => {
    const request: LicenciementBeCct109DeraisonnableRequest = {
      motifCommunique: true,
      motifLieAPersonne: 'MOTIF_VAGUE',
      discriminationSuspectee: false,
      represaillesSuspectees: false,
      proceduresRespectees: true,
      remunerationHebdomadaireBrute: 700,
      argumentsPatronal: null,
    };
    const response: LicenciementBeCct109DeraisonnableResponse = {
      caseFileId: 'case-42',
      motifCommunique: true,
      motifLieAPersonne: 'MOTIF_VAGUE',
      discriminationSuspectee: false,
      represaillesSuspectees: false,
      proceduresRespectees: true,
      remunerationHebdomadaireBrute: 700,
      argumentsPatronal: null,
      echelonCct109: '3_SEMAINES',
      nombreSemaines: 3,
      indemniteCct109: 2100,
      justificationEchelon: 'Motif vague sans aggravant → 3 semaines.',
      cumulAvecIcp: true,
      baseJuridique: 'CCT n° 109 du 12/02/2014, art. 8-9',
      avertissement: null,
    };

    let received: LicenciementBeCct109DeraisonnableResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);

    expect(received).toEqual(response);
  });

  it('GET utilise le même path et retourne la dernière analyse', () => {
    const response: LicenciementBeCct109DeraisonnableResponse = {
      caseFileId: 'case-42',
      motifCommunique: false,
      motifLieAPersonne: 'SANS_MOTIF',
      discriminationSuspectee: true,
      represaillesSuspectees: true,
      proceduresRespectees: false,
      remunerationHebdomadaireBrute: 800,
      argumentsPatronal: 'Réorganisation profonde',
      echelonCct109: '17_SEMAINES',
      nombreSemaines: 17,
      indemniteCct109: 13600,
      justificationEchelon: 'Discrimination + représailles + procédures non respectées → 17 sem. max.',
      cumulAvecIcp: true,
      baseJuridique: 'CCT n° 109 du 12/02/2014, art. 8-9',
      avertissement: 'Recours tribunal du travail recommandé.',
    };

    let received: LicenciementBeCct109DeraisonnableResponse | undefined;
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

  it('POST NON_DERAISONNABLE → 0 sem. + cumul ICP true', () => {
    const request: LicenciementBeCct109DeraisonnableRequest = {
      motifCommunique: true,
      motifLieAPersonne: 'MOTIF_PROUVE_VALIDE',
      discriminationSuspectee: false,
      represaillesSuspectees: false,
      proceduresRespectees: true,
      remunerationHebdomadaireBrute: 750,
      argumentsPatronal: null,
    };
    let received: LicenciementBeCct109DeraisonnableResponse | undefined;
    service.calculate('case-42', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.motifLieAPersonne).toBe('MOTIF_PROUVE_VALIDE');
    req.flush({
      caseFileId: 'case-42',
      ...request,
      echelonCct109: 'NON_DERAISONNABLE',
      nombreSemaines: 0,
      indemniteCct109: 0,
      justificationEchelon: 'Motif valable + procédures respectées → NON_DERAISONNABLE.',
      cumulAvecIcp: true,
      baseJuridique: 'CCT n° 109 du 12/02/2014, art. 8-9',
      avertissement: null,
    } as LicenciementBeCct109DeraisonnableResponse);
    expect(received!.echelonCct109).toBe('NON_DERAISONNABLE');
    expect(received!.nombreSemaines).toBe(0);
    expect(received!.cumulAvecIcp).toBe(true);
  });
});
