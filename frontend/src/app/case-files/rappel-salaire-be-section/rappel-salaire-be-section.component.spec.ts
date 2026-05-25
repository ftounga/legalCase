import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RappelSalaireBeSectionComponent } from './rappel-salaire-be-section.component';
import { RappelSalaireBeResponse } from '../../core/models/rappel-salaire-be.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('RappelSalaireBeSectionComponent', () => {
  let component: RappelSalaireBeSectionComponent;
  let fixture: ComponentFixture<RappelSalaireBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL = '/api/v1/case-files/case-1/decision-tools/rappel-salaire-be';

  function response(overrides: Partial<RappelSalaireBeResponse> = {}): RappelSalaireBeResponse {
    return {
      montantBrut: 5000,
      interetsCourus: 500,
      tauxMoratoire: '10% (Loi 12/04/1965 art. 10)',
      totalReclame: 5500,
      dateLimitePrescription: '2030-12-31',
      joursRestantsAvantPrescription: 1500,
      statutPrescription: 'NON_PRESCRIT',
      baseJuridique: 'Loi 12/04/1965 art. 10 ; Loi 03/07/1978 art. 15',
      formuleCalcul: '5000 € × 10% × 1 an = 500 € intérêts ; total = 5500 €',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        RappelSalaireBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RappelSalaireBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Flush silencieusement les requêtes jurisprudence-citations émises par
    // le composant injecté `<app-tool-jurisprudence-citations>` (F-JU-03).
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE → isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE → bannière « réservé droit BE » + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
  });

  it('GET 200 → hydrate result + mode résultat', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.statutPrescription).toBe('NON_PRESCRIT');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceMontantBrut()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // formValid()
  // ============================================================

  it('formValid false si montant absent ou ≤ 0', () => {
    component.dateDebutPeriode.set('2025-01-01');
    component.dateFinPeriode.set('2025-12-31');
    component.typeArriereEnum.set('PENDANT_CONTRAT');

    component.montantBrut.set(null);
    expect(component.formValid()).toBe(false);

    component.montantBrut.set(0);
    expect(component.formValid()).toBe(false);

    component.montantBrut.set(-100);
    expect(component.formValid()).toBe(false);

    component.montantBrut.set(5000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateFin < dateDebut', () => {
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2025-12-31');
    component.dateFinPeriode.set('2025-01-01');
    component.typeArriereEnum.set('PENDANT_CONTRAT');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si POST_RUPTURE sans dateRupture', () => {
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2025-01-01');
    component.dateFinPeriode.set('2025-12-31');
    component.typeArriereEnum.set('POST_RUPTURE');
    expect(component.formValid()).toBe(false);

    component.dateRupture.set('2025-12-31');
    expect(component.formValid()).toBe(true);
  });

  it('formValid true si PENDANT_CONTRAT sans dateRupture (cas nominal)', () => {
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2025-01-01');
    component.dateFinPeriode.set('2025-12-31');
    component.typeArriereEnum.set('PENDANT_CONTRAT');
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // calculate() / POST
  // ============================================================

  it('calculate() POST body correct → succès hydrate result + snackbar', () => {
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2025-01-01');
    component.dateFinPeriode.set('2025-12-31');
    component.typeArriereEnum.set('PENDANT_CONTRAT');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      montantBrut: 5000,
      dateDebutPeriode: '2025-01-01',
      dateFinPeriode: '2025-12-31',
      dateRupture: null,
      typeArriereEnum: 'PENDANT_CONTRAT',
    });
    req.flush(response());

    expect(component.result()!.statutPrescription).toBe('NON_PRESCRIT');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Rappel de salaire BE calculé',
      'OK',
      expect.any(Object),
    );
  });

  it('calculate() PRESCRIT → result hydraté + badge critical', () => {
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2018-01-01');
    component.dateFinPeriode.set('2018-12-31');
    component.typeArriereEnum.set('PENDANT_CONTRAT');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush(response({
      statutPrescription: 'PRESCRIT',
      joursRestantsAvantPrescription: -100,
      dateLimitePrescription: '2023-12-31',
    }));

    expect(component.result()!.statutPrescription).toBe('PRESCRIT');
    expect(component.prescriptionClass('PRESCRIT')).toBe('verdict-critical');
  });

  it('calculate() POST_RUPTURE envoie dateRupture dans le body', () => {
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2025-01-01');
    component.dateFinPeriode.set('2025-06-30');
    component.dateRupture.set('2025-06-30');
    component.typeArriereEnum.set('POST_RUPTURE');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body).toEqual({
      montantBrut: 5000,
      dateDebutPeriode: '2025-01-01',
      dateFinPeriode: '2025-06-30',
      dateRupture: '2025-06-30',
      typeArriereEnum: 'POST_RUPTURE',
    });
    req.flush(response({ statutPrescription: 'IMMINENT', joursRestantsAvantPrescription: 15 }));
    expect(component.result()!.statutPrescription).toBe('IMMINENT');
  });

  it('calculate() erreur backend → snackbar rouge + calculating reset', () => {
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2025-01-01');
    component.dateFinPeriode.set('2025-12-31');
    component.typeArriereEnum.set('PENDANT_CONTRAT');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      expect.any(String),
      'Fermer',
      expect.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() form invalide → pas de POST', () => {
    component.montantBrut.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() workspace FR → snackbar + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    component.montantBrut.set(5000);
    component.dateDebutPeriode.set('2025-01-01');
    component.dateFinPeriode.set('2025-12-31');
    component.typeArriereEnum.set('PENDANT_CONTRAT');
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('prefillFromAi : 4 champs IA + type dérivé POST_RUPTURE', () => {
    component.aiData = {
      montantArrieresSalaireBrut: 5000,
      dateDebutArrieresSalaire: '2025-01-01',
      dateFinArrieresSalaire: '2025-06-30',
      dateRuptureContrat: '2025-06-30',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.montantBrut()).toBe(5000);
    expect(component.dateDebutPeriode()).toBe('2025-01-01');
    expect(component.dateFinPeriode()).toBe('2025-06-30');
    expect(component.dateRupture()).toBe('2025-06-30');
    expect(component.typeArriereEnum()).toBe('POST_RUPTURE');
    expect(component.provenanceMontantBrut()).toBe('IA');
    expect(component.provenanceDateDebutPeriode()).toBe('IA');
    expect(component.provenanceDateFinPeriode()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceTypeArriere()).toBe('IA');
  });

  it('prefillFromAi : sans dateRupture → type dérivé PENDANT_CONTRAT', () => {
    component.aiData = {
      montantArrieresSalaireBrut: 5000,
      dateDebutArrieresSalaire: '2025-01-01',
      dateFinArrieresSalaire: '2025-12-31',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.typeArriereEnum()).toBe('PENDANT_CONTRAT');
    expect(component.dateRupture()).toBeNull();
  });

  it('prefillFromAi : aiData absent → aucun pré-fill', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.montantBrut()).toBeNull();
    expect(component.dateDebutPeriode()).toBeNull();
    expect(component.dateFinPeriode()).toBeNull();
    expect(component.dateRupture()).toBeNull();
    expect(component.typeArriereEnum()).toBeNull();
  });

  it('prefillFromAi : FRANCE → aucun pré-fill malgré aiData présent', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      montantArrieresSalaireBrut: 5000,
      dateDebutArrieresSalaire: '2025-01-01',
      dateFinArrieresSalaire: '2025-12-31',
      dateRuptureContrat: '2025-06-30',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.montantBrut()).toBeNull();
  });

  it('onMontantBrutChange efface provenance IA', () => {
    component.provenanceMontantBrut.set('IA');
    component.onMontantBrutChange(7500);
    expect(component.provenanceMontantBrut()).toBeNull();
    expect(component.montantBrut()).toBe(7500);
  });

  it('onTypeArriereChange efface provenance IA', () => {
    component.provenanceTypeArriere.set('IA');
    component.onTypeArriereChange('MIXTE');
    expect(component.provenanceTypeArriere()).toBeNull();
    expect(component.typeArriereEnum()).toBe('MIXTE');
  });

  it('onDateRuptureChange string vide → null + provenance reset', () => {
    component.provenanceDateRupture.set('IA');
    component.dateRupture.set('2025-06-30');
    component.onDateRuptureChange('');
    expect(component.dateRupture()).toBeNull();
    expect(component.provenanceDateRupture()).toBeNull();
  });

  // ============================================================
  // Prescription helpers / format
  // ============================================================

  it('prescriptionClass : NON_PRESCRIT → ok, IMMINENT → warn, PRESCRIT → critical', () => {
    expect(component.prescriptionClass('NON_PRESCRIT')).toBe('verdict-ok');
    expect(component.prescriptionClass('IMMINENT')).toBe('verdict-warn');
    expect(component.prescriptionClass('PRESCRIT')).toBe('verdict-critical');
  });

  it('prescriptionIcon : NON_PRESCRIT → check_circle, IMMINENT → warning, PRESCRIT → error', () => {
    expect(component.prescriptionIcon('NON_PRESCRIT')).toBe('check_circle');
    expect(component.prescriptionIcon('IMMINENT')).toBe('warning');
    expect(component.prescriptionIcon('PRESCRIT')).toBe('error');
  });

  it('formatEuro : 5500 → format fr-BE', () => {
    const formatted = component.formatEuro(5500);
    expect(formatted).toContain('€');
    expect(formatted).toMatch(/5.500,00/);
  });

  it('formatEuro : null / NaN → "—"', () => {
    expect(component.formatEuro(null)).toBe('—');
    expect(component.formatEuro(undefined)).toBe('—');
    expect(component.formatEuro(NaN)).toBe('—');
  });

  // ============================================================
  // Toggle / editMode
  // ============================================================

  it('toggleCollapse() inverse l\'état', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // Contrats statiques (F-177 / F-236 / F-237)
  // ============================================================

  it('expose TOOL_LABEL et TOOL_ICON statiques', () => {
    expect(RappelSalaireBeSectionComponent.TOOL_LABEL).toBe('RAPPEL DE SALAIRE (BE)');
    expect(RappelSalaireBeSectionComponent.TOOL_ICON).toBe('paid');
  });

  it('getPrefillCount({}) → 0 (input vide)', () => {
    expect(RappelSalaireBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount BELGIQUE + 4 champs → 4 (parité runtime)', () => {
    expect(RappelSalaireBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        montantArrieresSalaireBrut: 5000,
        dateDebutArrieresSalaire: '2025-01-01',
        dateFinArrieresSalaire: '2025-12-31',
        dateRuptureContrat: '2025-06-30',
      },
    })).toBe(4);
  });

  it('getPrefillCount FRANCE → 0 (gate strict)', () => {
    expect(RappelSalaireBeSectionComponent.getPrefillCount({
      workspaceCountry: 'FRANCE',
      aiData: {
        montantArrieresSalaireBrut: 5000,
        dateDebutArrieresSalaire: '2025-01-01',
        dateFinArrieresSalaire: '2025-12-31',
        dateRuptureContrat: '2025-06-30',
      },
    })).toBe(0);
  });
});
