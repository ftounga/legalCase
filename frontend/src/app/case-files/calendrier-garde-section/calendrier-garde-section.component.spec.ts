import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { CalendrierGardeSectionComponent } from './calendrier-garde-section.component';

describe('CalendrierGardeSectionComponent', () => {
  let component: CalendrierGardeSectionComponent;
  let fixture: ComponentFixture<CalendrierGardeSectionComponent>;
  let httpMock: HttpTestingController;
  const ID = '88888888-8888-8888-8888-888888888888';
  const URL = `/api/v1/case-files/${ID}/calendrier-garde`;
  const MOCK = { caseFileId: ID, gardeCode: 'ALTERNEE_FR', gardeLabel: 'Résidence alternée', country: 'FRANCE',
    parentANom: 'Marie', parentBNom: 'Pierre', repartitionType: 'ALTERNEE_1_SUR_2',
    semaineTypeParentA: ['Semaine A'], semaineTypeParentB: ['Semaine B'],
    vacancesRegle: 'Moitié', joursParAnParentA: 182, joursParAnParentB: 183,
    baseJuridique: 'Art 373-2-9', commentaire: 'Test' };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CalendrierGardeSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CalendrierGardeSectionComponent);
    component = fixture.componentInstance; component.caseFileId = ID;
  });
  afterEach(() => { httpMock.verify(); });

  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show form when no existing', () => { initNo(); expect(component.showForm()).toBe(true); });
  it('should call POST', () => {
    initNo(); component.parentANom.set('A'); component.parentBNom.set('B'); component.generate();
    const r = httpMock.expectOne(URL); expect(r.request.method).toBe('POST'); r.flush(MOCK);
    expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false);
  });
  it('should display existing', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false); });
});
