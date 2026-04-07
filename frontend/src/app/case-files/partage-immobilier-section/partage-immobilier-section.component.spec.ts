import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { PartageImmobilierSectionComponent } from './partage-immobilier-section.component';

describe('PartageImmobilierSectionComponent', () => {
  let component: PartageImmobilierSectionComponent;
  let fixture: ComponentFixture<PartageImmobilierSectionComponent>;
  let httpMock: HttpTestingController;

  const ID = '77777777-7777-7777-7777-777777777777';
  const URL = `/api/v1/case-files/${ID}/partage-immobilier`;
  const MOCK = {
    caseFileId: ID, country: 'FRANCE', valeurVenale: 300000, capitalRestantDu: 100000,
    valeurNette: 200000, quotePartAttributaire: 0.5, quotePartCedant: 0.5,
    partAttributaire: 100000, partCedant: 100000, soulte: 100000,
    droitPartage: 2200, tauxDroitPartage: 1.1, fraisNotaireEstimes: 4500,
    coutTotal: 106700, baseJuridique: 'Art. 746 CGI', commentaire: 'Test'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PartageImmobilierSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PartageImmobilierSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = ID;
  });

  afterEach(() => { httpMock.verify(); });

  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show form when no existing', () => { initNo(); expect(component.showForm()).toBe(true); });
  it('should call POST when calculate()', () => {
    initNo();
    component.valeurVenale.set(300000); component.calculate();
    const r = httpMock.expectOne(URL); expect(r.request.method).toBe('POST'); r.flush(MOCK);
    expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false);
  });
  it('should display existing from GET', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false); });
});
