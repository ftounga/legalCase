import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { JurisprudenceApplicableService } from './jurisprudence-applicable.service';
import { JurisprudenceApplicableResponse } from '../models/jurisprudence-applicable.model';

/**
 * F-JU-02 / SF-JU-02-02 — tests du service de lecture jurisprudence applicable.
 *
 * Couverture : appel HTTP nominal, fail-open en cas de 5xx / 404 / réseau.
 */
describe('JurisprudenceApplicableService', () => {
  let service: JurisprudenceApplicableService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        JurisprudenceApplicableService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(JurisprudenceApplicableService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('SF-JU-02-02: GET nominal → mappe la réponse JSON', (done) => {
    const expected: JurisprudenceApplicableResponse = {
      entries: [
        {
          toolId: 'f-dt-30-indemnite-licenciement-macron',
          brancheCalculId: 'default',
          citations: [
            {
              id: 'd6c8d8a3-3a3a-4444-9999-bbbbbbbbbbbb',
              arretRef: 'Cass. soc. 8 janv. 2025, n° 23-12.345',
              juridiction: 'Cour de cassation, chambre sociale',
              dateArret: '2025-01-08',
              numeroPourvoi: '23-12.345',
              lienLegifrance: 'https://legifrance.gouv.fr/x',
              chapeauOfficiel: 'Le barème Macron s\'applique.',
              lastVerifiedAt: '2026-05-01T00:00:00Z',
              confidenceScore: '0.95',
            },
          ],
        },
      ],
    };

    service.getJurisprudenceApplicable('case-1').subscribe((res) => {
      expect(res.entries).toHaveLength(1);
      expect(res.entries[0].toolId).toBe('f-dt-30-indemnite-licenciement-macron');
      expect(res.entries[0].citations[0].arretRef).toContain('Cass. soc.');
      done();
    });

    const req = httpMock.expectOne('/api/v1/case-files/case-1/jurisprudence-applicable');
    expect(req.request.method).toBe('GET');
    req.flush(expected);
  });

  it('SF-JU-02-02: fail-open sur 500 → retourne { entries: [] }', (done) => {
    service.getJurisprudenceApplicable('case-2').subscribe((res) => {
      expect(res).toEqual({ entries: [] });
      done();
    });

    httpMock
      .expectOne('/api/v1/case-files/case-2/jurisprudence-applicable')
      .flush({}, { status: 500, statusText: 'Server Error' });
  });

  it('SF-JU-02-02: fail-open sur 404 → retourne { entries: [] }', (done) => {
    service.getJurisprudenceApplicable('case-3').subscribe((res) => {
      expect(res.entries).toEqual([]);
      done();
    });

    httpMock
      .expectOne('/api/v1/case-files/case-3/jurisprudence-applicable')
      .flush({}, { status: 404, statusText: 'Not Found' });
  });
});
