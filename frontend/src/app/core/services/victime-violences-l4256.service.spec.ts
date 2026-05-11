import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { VictimeViolencesL4256Service } from './victime-violences-l4256.service';
import {
  VictimeViolencesL4256Request,
  VictimeViolencesL4256Response,
} from '../models/victime-violences-l4256.model';

describe('VictimeViolencesL4256Service', () => {
  let service: VictimeViolencesL4256Service;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/victime-violences-l4256-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [VictimeViolencesL4256Service],
    });
    service = TestBed.inject(VictimeViolencesL4256Service);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: VictimeViolencesL4256Request = {
      dateOrdonnanceProtection: '2026-03-01',
      juridiction: 'JAF Paris',
      dureeProtectionMois: 6,
      dateExpirationProtection: null,
      enfantsAcharge: 2,
      nationalite: 'Marocaine',
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as VictimeViolencesL4256Response);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as VictimeViolencesL4256Response);
  });
});
