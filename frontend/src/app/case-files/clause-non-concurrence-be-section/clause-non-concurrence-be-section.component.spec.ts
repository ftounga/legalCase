import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ClauseNonConcurrenceBeSectionComponent } from './clause-non-concurrence-be-section.component';
import { ClauseNonConcurrenceBeResponse } from '../../core/models/clause-non-concurrence-be.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ClauseNonConcurrenceBeSectionComponent', () => {
  let component: ClauseNonConcurrenceBeSectionComponent;
  let fixture: ComponentFixture<ClauseNonConcurrenceBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL = '/api/v1/case-files/case-1/decision-tools/clause-non-concurrence-be';

  function response(overrides: Partial<ClauseNonConcurrenceBeResponse> = {}): ClauseNonConcurrenceBeResponse {
    return {
      verdict: 'VALIDE',
      raisonNullite: null,
      indemniteLegale: 25000,
      indemniteLegaleFormule: '(100000 € / 12) * (6 / 2) = 25 000,00 €',
      baseJuridique: 'Loi 03/07/1978 art. 65 ; CCT n°13 du 24/02/1971',
      avertissement: 'Seuil 2024 : 73 571 € — à vérifier selon année de signature.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        ClauseNonConcurrenceBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ClauseNonConcurrenceBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Flush silencieusement les requêtes jurisprudence-citations émises par le
    // composant injecté `<app-tool-jurisprudence-citations>` (F-JU-03) pour
    // éviter les fuites `expectNone` du HttpTestingController.
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
    expect(component.result()!.verdict).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceRemuneration()).toBeNull();
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

  it('formValid false si rémunération absente ou ≤ 0', () => {
    component.remunerationAnnuelleBrute.set(null);
    component.dureeMois.set(6);
    component.zoneGeographique.set('BELGIQUE_UNIQUEMENT');
    expect(component.formValid()).toBe(false);

    component.remunerationAnnuelleBrute.set(0);
    expect(component.formValid()).toBe(false);

    component.remunerationAnnuelleBrute.set(-100);
    expect(component.formValid()).toBe(false);

    component.remunerationAnnuelleBrute.set(80000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si durée hors [1, 12] ou décimale', () => {
    component.remunerationAnnuelleBrute.set(80000);
    component.zoneGeographique.set('BELGIQUE_UNIQUEMENT');

    component.dureeMois.set(0);
    expect(component.formValid()).toBe(false);

    component.dureeMois.set(13);
    expect(component.formValid()).toBe(false);

    component.dureeMois.set(6.5);
    expect(component.formValid()).toBe(false);

    component.dureeMois.set(6);
    expect(component.formValid()).toBe(true);

    component.dureeMois.set(1);
    expect(component.formValid()).toBe(true);

    component.dureeMois.set(12);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si zone absente', () => {
    component.remunerationAnnuelleBrute.set(80000);
    component.dureeMois.set(6);
    component.zoneGeographique.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() / POST
  // ============================================================

  it('calculate() POST body correct → succès hydrate result + snackbar', () => {
    component.remunerationAnnuelleBrute.set(100000);
    component.dureeMois.set(6);
    component.zoneGeographique.set('BELGIQUE_UNIQUEMENT');
    component.activiteInternationaleProuvee.set(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      remunerationAnnuelleBrute: 100000,
      dureeMois: 6,
      zoneGeographique: 'BELGIQUE_UNIQUEMENT',
      activiteInternationaleProuvee: false,
    });
    req.flush(response());

    expect(component.result()!.verdict).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Clause de non-concurrence BE analysée',
      'OK',
      expect.any(Object),
    );
  });

  it('calculate() verdict NULLE → result hydraté + showForm false', () => {
    component.remunerationAnnuelleBrute.set(60000);
    component.dureeMois.set(6);
    component.zoneGeographique.set('BELGIQUE_UNIQUEMENT');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush(response({
      verdict: 'NULLE',
      raisonNullite: 'REMUNERATION_INSUFFISANTE',
      indemniteLegale: 0,
      indemniteLegaleFormule: '',
    }));

    expect(component.result()!.verdict).toBe('NULLE');
    expect(component.result()!.raisonNullite).toBe('REMUNERATION_INSUFFISANTE');
  });

  it('calculate() erreur backend → snackbar rouge + calculating reset', () => {
    component.remunerationAnnuelleBrute.set(100000);
    component.dureeMois.set(6);
    component.zoneGeographique.set('BELGIQUE_UNIQUEMENT');
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
    component.remunerationAnnuelleBrute.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() workspace FR → snackbar + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    component.remunerationAnnuelleBrute.set(100000);
    component.dureeMois.set(6);
    component.zoneGeographique.set('BELGIQUE_UNIQUEMENT');
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('prefillFromAi : 3 champs IA → 3 valeurs + badges IA', () => {
    component.aiData = {
      salaireBrutAnnuel: 90000,
      clauseNonConcurrenceDureeMois: 6,
      clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.remunerationAnnuelleBrute()).toBe(90000);
    expect(component.dureeMois()).toBe(6);
    expect(component.zoneGeographique()).toBe('BELGIQUE_UNIQUEMENT');
    expect(component.provenanceRemuneration()).toBe('IA');
    expect(component.provenanceDureeMois()).toBe('IA');
    expect(component.provenanceZone()).toBe('IA');
  });

  it('prefillFromAi : aiData absent → aucun pré-fill', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.remunerationAnnuelleBrute()).toBeNull();
    expect(component.dureeMois()).toBeNull();
    expect(component.zoneGeographique()).toBeNull();
  });

  it('prefillFromAi : FRANCE → aucun pré-fill malgré aiData présent', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      salaireBrutAnnuel: 90000,
      clauseNonConcurrenceDureeMois: 6,
      clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT',
    } as TravailExtractedData;
    component.ngOnInit();
    // FRANCE → pas d'appel HTTP, donc pas de prefill (load() pas appelé).
    httpMock.expectNone(BASE_URL);
    expect(component.remunerationAnnuelleBrute()).toBeNull();
  });

  it('onRemunerationChange efface provenance IA', () => {
    component.provenanceRemuneration.set('IA');
    component.onRemunerationChange(75000);
    expect(component.provenanceRemuneration()).toBeNull();
    expect(component.remunerationAnnuelleBrute()).toBe(75000);
  });

  it('onZoneChange efface provenance IA', () => {
    component.provenanceZone.set('IA');
    component.onZoneChange('BELGIQUE_ET_ETRANGER');
    expect(component.provenanceZone()).toBeNull();
    expect(component.zoneGeographique()).toBe('BELGIQUE_ET_ETRANGER');
  });

  // ============================================================
  // Verdicts / banner / labels
  // ============================================================

  it('verdictClass : VALIDE → ok, NULLE → critical, PARTIELLEMENT_NULLE → warn', () => {
    expect(component.verdictClass('VALIDE')).toBe('verdict-ok');
    expect(component.verdictClass('NULLE')).toBe('verdict-critical');
    expect(component.verdictClass('PARTIELLEMENT_NULLE')).toBe('verdict-warn');
  });

  it('verdictIcon : VALIDE → check_circle, NULLE → error, PARTIELLEMENT_NULLE → warning', () => {
    expect(component.verdictIcon('VALIDE')).toBe('check_circle');
    expect(component.verdictIcon('NULLE')).toBe('error');
    expect(component.verdictIcon('PARTIELLEMENT_NULLE')).toBe('warning');
  });

  it('formatEuro : 25000 → format fr-BE', () => {
    const formatted = component.formatEuro(25000);
    // fr-BE peut produire "25 000,00 €" ou similaire selon ICU — on vérifie la
    // présence de la devise et du séparateur décimal virgule.
    expect(formatted).toContain('€');
    expect(formatted).toMatch(/25.000,00/);
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
    expect(ClauseNonConcurrenceBeSectionComponent.TOOL_LABEL).toBe('CLAUSE DE NON-CONCURRENCE (BE)');
    expect(ClauseNonConcurrenceBeSectionComponent.TOOL_ICON).toBe('block');
  });

  it('getPrefillCount({}) → 0 (input vide)', () => {
    expect(ClauseNonConcurrenceBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount BELGIQUE + 3 champs → 3 (parité runtime)', () => {
    expect(ClauseNonConcurrenceBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        salaireBrutAnnuel: 80000,
        clauseNonConcurrenceDureeMois: 6,
        clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT',
      },
    })).toBe(3);
  });

  it('getPrefillCount FRANCE → 0 (gate strict)', () => {
    expect(ClauseNonConcurrenceBeSectionComponent.getPrefillCount({
      workspaceCountry: 'FRANCE',
      aiData: {
        salaireBrutAnnuel: 80000,
        clauseNonConcurrenceDureeMois: 6,
        clauseNonConcurrenceZone: 'BELGIQUE_UNIQUEMENT',
      },
    })).toBe(0);
  });
});
