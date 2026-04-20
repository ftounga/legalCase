import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RuptureConvIndemniteSectionComponent } from './rupture-conv-indemnite-section.component';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';

function createSynthesis(overrides: Partial<CaseAnalysisResult['compensationEstimate']>): CaseAnalysisResult {
  return {
    compensationEstimate: {
      indemnite: 0,
      salaireReference: 2979,
      ancienneteAnnees: 4,
      ancienneteMois: 0,
      typeRupture: 'RUPTURE_CONVENTIONNELLE',
      plafondMinMois: 0,
      plafondMaxMois: 0,
      donneesPartielles: false,
      ...(overrides ?? {}),
    },
  } as CaseAnalysisResult;
}

describe('RuptureConvIndemniteSectionComponent', () => {
  let component: RuptureConvIndemniteSectionComponent;
  let fixture: ComponentFixture<RuptureConvIndemniteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [RuptureConvIndemniteSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RuptureConvIndemniteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne('/api/v1/case-files/case-1/rupture-conv-indemnite');
    req.flush({
      caseFileId: 'case-1', ancienneteAnnees: 4, salaireMensuel: 2979,
      indemniteLegaleMinimum: 2979, formule: '¼ × 4 ans × 2979 €',
      baseJuridique: 'Art. R1234-2 Code du travail', messages: ['L1237-13'],
    });
    expect(component.result()!.indemniteLegaleMinimum).toBe(2979);
    expect(component.showForm()).toBe(false);
  });

  it('reste en mode formulaire si GET 404, puis prefill IA', () => {
    component.synthesis = createSynthesis({});
    component.ngOnInit();
    const req = httpMock.expectOne('/api/v1/case-files/case-1/rupture-conv-indemnite');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.ancienneteAnnees()).toBe(4);
    expect(component.salaireMensuel()).toBe(2979);
  });

  it('formValid false si inputs manquants ou négatifs', () => {
    component.ancienneteAnnees.set(null);
    component.salaireMensuel.set(3000);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5);
    component.salaireMensuel.set(0);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5);
    component.salaireMensuel.set(3000);
    expect(component.formValid()).toBe(true);

    component.ancienneteAnnees.set(-1);
    component.salaireMensuel.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('calculate() POST + affiche résultat + snackbar succès', () => {
    component.ancienneteAnnees.set(4);
    component.salaireMensuel.set(2979);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === '/api/v1/case-files/case-1/rupture-conv-indemnite');
    expect(req.request.body).toEqual({ ancienneteAnnees: 4, salaireMensuel: 2979 });
    req.flush({
      caseFileId: 'case-1', ancienneteAnnees: 4, salaireMensuel: 2979,
      indemniteLegaleMinimum: 2979, formule: '¼ × 4 ans × 2979 €',
      baseJuridique: 'Art. R1234-2 Code du travail', messages: [],
    });

    expect(component.result()!.indemniteLegaleMinimum).toBe(2979);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.ancienneteAnnees.set(5);
    component.salaireMensuel.set(3000);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(jasmine.any(String), 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' }));
    expect(component.calculating()).toBe(false);
  });

  it('ignore le prefill IA si des inputs sont déjà remplis', () => {
    // L'analyse GET 200 signale "déjà pré-rempli depuis la persistance"
    component.ngOnInit();
    const req = httpMock.expectOne('/api/v1/case-files/case-1/rupture-conv-indemnite');
    req.flush({
      caseFileId: 'case-1', ancienneteAnnees: 10, salaireMensuel: 4000,
      indemniteLegaleMinimum: 10000, formule: '…',
      baseJuridique: 'Art. R1234-2', messages: [],
    });

    // Injection d'une nouvelle synthèse IA : ne doit pas écraser la valeur utilisateur
    component.synthesis = createSynthesis({ ancienneteAnnees: 2, salaireReference: 1500 });
    component.ngOnChanges({ synthesis: { currentValue: component.synthesis, previousValue: null, firstChange: false, isFirstChange: () => false } });
    expect(component.ancienneteAnnees()).toBe(10);
    expect(component.salaireMensuel()).toBe(4000);
  });
});
