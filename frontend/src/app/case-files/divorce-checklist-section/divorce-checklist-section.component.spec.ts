import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { DivorceChecklistSectionComponent } from './divorce-checklist-section.component';

describe('DivorceChecklistSectionComponent', () => {
  let component: DivorceChecklistSectionComponent;
  let fixture: ComponentFixture<DivorceChecklistSectionComponent>;
  let httpMock: HttpTestingController;
  const ID = '99999999-9999-9999-9999-999999999999';
  const URL = `/api/v1/case-files/${ID}/divorce-checklist`;
  const MOCK = { caseFileId: ID, country: 'FRANCE',
    etapes: [{ code: 'FR_CHOIX_AVOCATS', label: 'Choix avocats', ordre: 1, description: 'Desc', delai: '—', obligatoire: true, statut: 'A_FAIRE' }],
    pieces: [{ code: 'FR_ACTE_MARIAGE', label: 'Acte mariage', description: 'Desc', obligatoire: true, statut: 'MANQUANTE' }],
    etapesCompletees: 0, etapesTotal: 1, piecesPresentes: 0, piecesTotal: 1, baseJuridique: 'Art 229' };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DivorceChecklistSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DivorceChecklistSectionComponent);
    component = fixture.componentInstance; component.caseFileId = ID;
  });
  afterEach(() => { httpMock.verify(); });

  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show init when no existing', () => { initNo(); expect(component.result()).toBeNull(); });
  it('should display existing', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.progress()).toBe(0); });
  it('should toggle etape and save', () => {
    initWith();
    component.toggleEtape(component.result()!.etapes[0]);
    const req = httpMock.expectOne(URL); expect(req.request.method).toBe('POST');
    req.flush({ ...MOCK, etapesCompletees: 1, etapes: [{ ...MOCK.etapes[0], statut: 'FAIT' }] });
    expect(component.result()!.etapesCompletees).toBe(1);
  });
});
