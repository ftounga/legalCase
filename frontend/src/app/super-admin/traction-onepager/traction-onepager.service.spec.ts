import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TractionOnePagerService } from './traction-onepager.service';
import { TractionOnePagerInput } from './traction-onepager.model';

describe('TractionOnePagerService', () => {
  let service: TractionOnePagerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TractionOnePagerService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TractionOnePagerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // T-07 — POST appelé avec responseType: 'blob' + URL correcte
  it('T-07: generatePdf POST sur /api/v1/super-admin/traction-onepager/pdf avec responseType blob', () => {
    const input: TractionOnePagerInput = {
      includeKpis: {
        totalWorkspaces: true,
        trialWorkspaces: true,
        paidWorkspaces: true,
        conversionRatePct: true,
        activeWorkspaces30d: true,
        analysesLast7Days: true,
        analysesLast30Days: true,
      },
      verbatims: [],
      partners: [],
      headline: 'Test headline',
      contact: { name: 'Franck', email: 'a@b.com', url: 'https://x.y' },
    };
    const fakeBlob = new Blob(['%PDF-1.4 fake'], { type: 'application/pdf' });

    let received: Blob | null = null;
    service.generatePdf(input).subscribe(b => (received = b));

    const req = httpMock.expectOne('/api/v1/super-admin/traction-onepager/pdf');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(input);
    expect(req.request.responseType).toBe('blob');
    req.flush(fakeBlob);

    expect(received).not.toBeNull();
    expect(received).toBeInstanceOf(Blob);
  });

  // T-08 — triggerDownload : crée un blob URL, clique l'anchor, libère l'URL
  it('T-08: triggerDownload crée un anchor download, clique, et libère le blob URL', () => {
    const blob = new Blob(['fake'], { type: 'application/pdf' });

    const fakeAnchor = {
      href: '',
      download: '',
      click: jest.fn(),
    };
    const createElementSpy = jest
      .spyOn(document, 'createElement')
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .mockReturnValue(fakeAnchor as any);
    const createUrlSpy = jest
      .spyOn(URL, 'createObjectURL')
      .mockReturnValue('blob:fake-url');
    const revokeUrlSpy = jest
      .spyOn(URL, 'revokeObjectURL')
      .mockImplementation(() => undefined);

    service.triggerDownload(blob, 'test.pdf');

    expect(createUrlSpy).toHaveBeenCalledWith(blob);
    expect(createElementSpy).toHaveBeenCalledWith('a');
    expect(fakeAnchor.href).toBe('blob:fake-url');
    expect(fakeAnchor.download).toBe('test.pdf');
    expect(fakeAnchor.click).toHaveBeenCalledTimes(1);
    expect(revokeUrlSpy).toHaveBeenCalledWith('blob:fake-url');

    createElementSpy.mockRestore();
    createUrlSpy.mockRestore();
    revokeUrlSpy.mockRestore();
  });

  // Bonus — buildFilename produit le format attendu YYYY-MM-DD
  it('buildFilename retourne legalcase-traction-YYYY-MM-DD.pdf', () => {
    const fixed = new Date(2026, 4, 4); // 2026-05-04
    expect(service.buildFilename(fixed)).toBe('legalcase-traction-2026-05-04.pdf');
  });
});
