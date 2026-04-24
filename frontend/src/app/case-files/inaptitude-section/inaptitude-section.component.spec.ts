import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { InaptitudeSectionComponent } from './inaptitude-section.component';
import { InaptitudeResponse } from '../../core/models/inaptitude.model';

describe('InaptitudeSectionComponent', () => {
  let component: InaptitudeSectionComponent;
  let fixture: ComponentFixture<InaptitudeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/inaptitude';

  function frResponse(): InaptitudeResponse {
    return {
      caseFileId: 'case-1',
      salaireMensuelReference: 3000,
      ancienneteAnnees: 5,
      origineInaptitude: 'PROFESSIONNELLE',
      reclassementRespecte: true,
      avisMedecinTravailDate: '2026-01-15',
      country: 'FRANCE',
      indemniteLegale: 7500,
      indemniteCompensatricePreavis: 6000,
      damagesReclassement: 0,
      total: 13500,
      formule: 'Indemnité légale doublée + préavis (origine pro)',
      baseJuridique: 'Art. L1226-14 Code du travail',
      messages: ['Indemnité légale doublée pour inaptitude professionnelle'],
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        InaptitudeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(InaptitudeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('FRANCE → 2 origines FR disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.originesDisponibles().length).toBe(2);
    const codes = component.originesDisponibles().map(o => o.code);
    expect(codes).toContain('PROFESSIONNELLE');
    expect(codes).toContain('NON_PROFESSIONNELLE');
  });

  it('BELGIQUE → 2 origines BE disponibles', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.originesDisponibles().length).toBe(2);
    const codes = component.originesDisponibles().map(o => o.code);
    expect(codes).toContain('PROFESSIONNELLE_BE');
    expect(codes).toContain('NON_PROFESSIONNELLE_BE');
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()!.total).toBe(13500);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.ancienneteAnnees()).toBe(5);
    expect(component.origineInaptitude()).toBe('PROFESSIONNELLE');
    expect(component.reclassementRespecte()).toBe(true);
    expect(component.avisMedecinTravailDate()).toBe('2026-01-15');
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si salaire ou ancienneté ou origine manquants', () => {
    component.salaireMensuelReference.set(null);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(null);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set(null);
    expect(component.formValid()).toBe(false);

    component.origineInaptitude.set('PROFESSIONNELLE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si salaire ≤ 0 ou ancienneté non entière ou négative', () => {
    component.salaireMensuelReference.set(0);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(-1);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5.5);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST + affiche résultat + snackbar succès + omet date si vide', () => {
    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    component.reclassementRespecte.set(true);
    component.avisMedecinTravailDate.set(null);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      salaireMensuelReference: 3000,
      ancienneteAnnees: 5,
      origineInaptitude: 'PROFESSIONNELLE',
      reclassementRespecte: true,
    });
    req.flush(frResponse());

    expect(component.result()!.total).toBe(13500);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() inclut avisMedecinTravailDate si renseignée', () => {
    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    component.reclassementRespecte.set(true);
    component.avisMedecinTravailDate.set('2026-01-15');
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body.avisMedecinTravailDate).toBe('2026-01-15');
    req.flush(frResponse());
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.salaireMensuelReference.set(null);
    component.ancienneteAnnees.set(null);
    component.origineInaptitude.set(null);
    component.calculate();
    httpMock.expectNone(r => r.method === 'POST');
  });
});
