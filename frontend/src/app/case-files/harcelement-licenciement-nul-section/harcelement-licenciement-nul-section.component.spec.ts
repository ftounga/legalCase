import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HarcelementLicenciementNulSectionComponent } from './harcelement-licenciement-nul-section.component';
import { HarcelementNulliteResponse } from '../../core/models/harcelement-nullite.model';

describe('HarcelementLicenciementNulSectionComponent', () => {
  let component: HarcelementLicenciementNulSectionComponent;
  let fixture: ComponentFixture<HarcelementLicenciementNulSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/harcelement-licenciement-nul';

  function frResponse(): HarcelementNulliteResponse {
    return {
      caseFileId: 'case-1',
      salaireMensuelReference: 3000,
      motifNullite: 'HARCELEMENT_MORAL',
      country: 'FRANCE',
      indemniteMinimumNullite: 18000,
      formule: '6 × 3 000,00 € = 18 000,00 €',
      baseJuridique: 'Art. L1235-3-1 Code du travail',
      messages: ['Minimum 6 mois de salaire'],
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        HarcelementLicenciementNulSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(HarcelementLicenciementNulSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('FRANCE → 8 motifs disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.motifsDisponibles().length).toBe(8);
    const codes = component.motifsDisponibles().map(m => m.code);
    expect(codes).toContain('HARCELEMENT_MORAL');
    expect(codes).toContain('ALERTE_ETHIQUE');
  });

  it('BELGIQUE → 4 motifs disponibles', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.motifsDisponibles().length).toBe(4);
    const codes = component.motifsDisponibles().map(m => m.code);
    expect(codes).toContain('HARCELEMENT_MORAL_BE');
    expect(codes).toContain('DISCRIMINATION_BE');
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()!.indemniteMinimumNullite).toBe(18000);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.motifNullite()).toBe('HARCELEMENT_MORAL');
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si salaire manquant, motif manquant ou salaire ≤ 0', () => {
    component.salaireMensuelReference.set(null);
    component.motifNullite.set('HARCELEMENT_MORAL');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.motifNullite.set(null);
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(0);
    component.motifNullite.set('HARCELEMENT_MORAL');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.motifNullite.set('HARCELEMENT_MORAL');
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST + affiche résultat + snackbar succès', () => {
    component.salaireMensuelReference.set(3000);
    component.motifNullite.set('HARCELEMENT_MORAL');
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      salaireMensuelReference: 3000,
      motifNullite: 'HARCELEMENT_MORAL',
    });
    req.flush(frResponse());

    expect(component.result()!.indemniteMinimumNullite).toBe(18000);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.salaireMensuelReference.set(3000);
    component.motifNullite.set('HARCELEMENT_MORAL');
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
    component.motifNullite.set(null);
    component.calculate();
    httpMock.expectNone(r => r.method === 'POST');
  });
});
