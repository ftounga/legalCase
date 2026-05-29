import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ItfJudiciaireService } from './itf-judiciaire.service';
import {
  ItfJudiciaireRequest,
  ItfJudiciaireResponse,
} from '../models/itf-judiciaire.model';

describe('ItfJudiciaireService', () => {
  let service: ItfJudiciaireService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/itf-judiciaire-analysis';

  function response(overrides: Partial<ItfJudiciaireResponse> = {}): ItfJudiciaireResponse {
    return {
      caseFileId: 'case-1',
      dateCondamnation: '2026-01-15',
      dureeITFAnnees: 5,
      infractionPrincipale: 'Trafic de stupéfiants',
      condamnationDefinitive: false,
      dateEcheanceRecoursPenal: '2026-01-25',
      country: 'FRANCE',
      statut: 'APPEL_POSSIBLE',
      dateEcheanceReleve: '2027-01-15',
      voiesRecours: ['Appel : 10 jours à compter du prononcé', 'Pourvoi en cassation : 5 jours'],
      requisReleve: ['Résider hors de France', 'Justifier de garanties de réinsertion'],
      distinctionItfVsIrtf: "L'ITF est une peine pénale (juge) ; l'IRTF est une mesure administrative (préfet).",
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ItfJudiciaireService],
    });
    service = TestBed.inject(ItfJudiciaireService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(ItfJudiciaireService.STANDALONE_TOOL_ID).toBe('F-IM-43-itf-judiciaire-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: ItfJudiciaireRequest = {
      dateCondamnation: '2026-01-15',
      dureeITFAnnees: 5,
      infractionPrincipale: 'Trafic de stupéfiants',
      condamnationDefinitive: false,
      dateEcheanceRecoursPenal: '2026-01-25',
    };
    let result: ItfJudiciaireResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('APPEL_POSSIBLE');
    expect(result!.voiesRecours.length).toBe(2);
    expect(result!.distinctionItfVsIrtf).toContain('IRTF');
  });

  it('get() GETs from the right URL', () => {
    let result: ItfJudiciaireResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'RECOURS_PRESCRIT', voiesRecours: [], requisReleve: [] }));
    expect(result!.statut).toBe('RECOURS_PRESCRIT');
    expect(result!.voiesRecours).toEqual([]);
    expect(result!.requisReleve).toEqual([]);
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
