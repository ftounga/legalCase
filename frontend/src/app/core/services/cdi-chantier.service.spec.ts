import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CdiChantierService } from './cdi-chantier.service';
import { CdiChantierRequest, CdiChantierResponse } from '../models/cdi-chantier.model';

describe('CdiChantierService', () => {
  let service: CdiChantierService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/cdi-chantier-analysis';

  function response(): CdiChantierResponse {
    return {
      caseFileId: 'case-1',
      dateEntree: '2023-01-06',
      dateRupture: '2026-04-06',
      fondementRecours: 'ACCORD_BRANCHE_ETENDU',
      secteur: 'BTP',
      chantierAcheve: true,
      salaireMensuelMoyen: 3000,
      reclassementAutreChantierPropose: false,
      recoursValide: true,
      motifRecours: 'Recours valide (accord de branche étendu).',
      motifLicenciement: 'FIN_CHANTIER_CRS',
      indemniteLicenciement: 2250,
      procedureRequise: true,
      verdictGlobal: 'LICENCIEMENT_FONDE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.1223-8 CT (à vérifier par avocat)',
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CdiChantierService],
    });
    service = TestBed.inject(CdiChantierService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes the STANDALONE_TOOL_ID aligned with TOOL_REGISTRY', () => {
    expect(CdiChantierService.STANDALONE_TOOL_ID).toBe('F-DT-37-licenciement-cdi-chantier');
  });

  it('analyze() POSTs the request to the case-file scoped URL', () => {
    const req: CdiChantierRequest = {
      dateEntree: '2023-01-06',
      dateRupture: '2026-04-06',
      fondementRecours: 'ACCORD_BRANCHE_ETENDU',
      secteur: 'BTP',
      chantierAcheve: true,
      salaireMensuelMoyen: 3000,
      reclassementAutreChantierPropose: false,
    };
    let received: CdiChantierResponse | undefined;
    service.analyze('case-1', req).subscribe((r) => (received = r));
    const http = httpMock.expectOne(BASE_URL);
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual(req);
    http.flush(response());
    expect(received!.verdictGlobal).toBe('LICENCIEMENT_FONDE');
    expect(received!.indemniteLicenciement).toBe(2250);
  });

  it('get() GETs the case-file scoped URL', () => {
    let received: CdiChantierResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const http = httpMock.expectOne(BASE_URL);
    expect(http.request.method).toBe('GET');
    http.flush(response());
    expect(received!.recoursValide).toBe(true);
  });

  it('get() propagates a 404 (no analysis yet)', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({ error: (e) => (errStatus = e.status) });
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'Not Found' });
    expect(errStatus).toBe(404);
  });
});
