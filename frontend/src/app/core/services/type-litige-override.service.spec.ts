import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { TypeLitigeOverrideService } from './type-litige-override.service';
import { TypeLitigeOverrideResponse } from '../models/type-litige-override.model';

describe('TypeLitigeOverrideService — F-197 SF-197-02', () => {
  let service: TypeLitigeOverrideService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '11111111-1111-1111-1111-111111111111';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/type-litige-override`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TypeLitigeOverrideService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TypeLitigeOverrideService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('PUT 200 — Travail FR : envoie {type, raison} et retourne typeLitigeAvocat', () => {
    const expected: TypeLitigeOverrideResponse = {
      typeLitigeAvocat: 'LICENCIEMENT_ECONOMIQUE',
      typeProcedureAvocat: null,
      raison: 'Motif économique évident dans la lettre',
    };
    let received: TypeLitigeOverrideResponse | undefined;
    service.update(CASE_FILE_ID, {
      type: 'LICENCIEMENT_ECONOMIQUE',
      raison: 'Motif économique évident dans la lettre',
    }).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      type: 'LICENCIEMENT_ECONOMIQUE',
      raison: 'Motif économique évident dans la lettre',
    });
    req.flush(expected);
    expect(received).toEqual(expected);
  });

  it('PUT 200 — Immigration : envoie {type, raison=null par défaut}', () => {
    const expected: TypeLitigeOverrideResponse = {
      typeLitigeAvocat: null,
      typeProcedureAvocat: 'OQTF_SANS_DELAI',
      raison: null,
    };
    let received: TypeLitigeOverrideResponse | undefined;
    service.update(CASE_FILE_ID, { type: 'OQTF_SANS_DELAI' })
      .subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({
      type: 'OQTF_SANS_DELAI',
      raison: null,
    });
    req.flush(expected);
    expect(received).toEqual(expected);
  });

  it('PUT 400 — propage l\'erreur (le composant doit garder le dialog ouvert)', () => {
    const errorSpy = jest.fn();
    service.update(CASE_FILE_ID, { type: 'HARCELEMENT_MORAL' })
      .subscribe({ next: () => {}, error: errorSpy });
    http.expectOne(URL).flush(
      { message: 'type invalide pour ce domaine' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(errorSpy).toHaveBeenCalled();
  });

  it('PUT 5xx — propage l\'erreur', () => {
    const errorSpy = jest.fn();
    service.update(CASE_FILE_ID, { type: 'RAPPEL_SALAIRE' })
      .subscribe({ next: () => {}, error: errorSpy });
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(errorSpy).toHaveBeenCalled();
  });

  it('GET 200 — retourne l\'override courant (Travail FR)', () => {
    const expected: TypeLitigeOverrideResponse = {
      typeLitigeAvocat: 'PRISE_ACTE_RUPTURE',
      typeProcedureAvocat: null,
      raison: 'Le client souhaite tester cet angle',
    };
    let received: TypeLitigeOverrideResponse | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush(expected);
    expect(received).toEqual(expected);
  });

  it('GET 200 — retourne tous les champs null si aucun override posé', () => {
    let received: TypeLitigeOverrideResponse | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    req.flush({
      typeLitigeAvocat: null,
      typeProcedureAvocat: null,
      raison: null,
    });
    expect(received?.typeLitigeAvocat).toBeNull();
    expect(received?.typeProcedureAvocat).toBeNull();
  });

  it('GET 5xx — propage l\'erreur (le composant fail-open silencieux)', () => {
    const errorSpy = jest.fn();
    service.getForCaseFile(CASE_FILE_ID)
      .subscribe({ next: () => {}, error: errorSpy });
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(errorSpy).toHaveBeenCalled();
  });
});
