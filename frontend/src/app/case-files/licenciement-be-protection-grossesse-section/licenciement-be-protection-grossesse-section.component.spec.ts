import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeProtectionGrossesseSectionComponent,
} from './licenciement-be-protection-grossesse-section.component';
import {
  LicenciementBeProtectionGrossesseResponse,
} from '../../core/models/licenciement-be-protection-grossesse.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('LicenciementBeProtectionGrossesseSectionComponent', () => {
  let component: LicenciementBeProtectionGrossesseSectionComponent;
  let fixture: ComponentFixture<LicenciementBeProtectionGrossesseSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-protection-grossesse';

  function response(
    overrides: Partial<LicenciementBeProtectionGrossesseResponse> = {},
  ): LicenciementBeProtectionGrossesseResponse {
    return {
      caseFileId: 'case-1',
      dateDebutGrossesse: '2026-01-15',
      dateAccouchement: '2026-10-15',
      dateCongeMaterniteDebut: '2026-09-01',
      dateCongeMaterniteFinale: '2027-01-15',
      dateLicenciement: '2026-03-15',
      grossesseNotifieeParEcrit: true,
      remunerationMensuelleBrute: 3500,
      motifInvoqueParEmployeur: null,
      verdict: 'PROTECTION_PRESUMEE',
      licenciementDansLaPeriodeProtegee: true,
      dateDebutProtection: '2026-01-15',
      dateFinProtection: '2027-02-15',
      indemniteForfaitaire: 21000,
      chargePreuveEmployeur: true,
      baseJuridique: 'Loi du 16/03/1971 sur la protection de la maternité, art. 40',
      formuleCalcul: '6 × 3500,00 € = 21000,00 €',
      avertissement: null,
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeProtectionGrossesseSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeProtectionGrossesseSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
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

  it('GET 200 → hydrate result + mode résultat + reset provenance IA', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('PROTECTION_PRESUMEE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceDateLicenciement()).toBeNull();
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
  // Pré-fill IA
  // ============================================================

  it('pré-fill BE : dateLicenciement + remuneration mensuelle', () => {
    component.aiData = {
      dateLicenciement: '2026-03-15',
      salaireBrutMensuel: 3500,
    } as TravailExtractedData;
    component.ngOnInit();

    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.dateLicenciement()).toBe('2026-03-15');
    expect(component.remunerationMensuelleBrute()).toBe(3500);
    expect(component.provenanceDateLicenciement()).toBe('IA');
    expect(component.provenanceRemuneration()).toBe('IA');
    // Les autres champs restent vides (pas d'IA grossesse dans cette vague).
    expect(component.dateDebutGrossesse()).toBeNull();
    expect(component.dateAccouchement()).toBeNull();
    expect(component.grossesseNotifieeParEcrit()).toBe(false);
  });

  it('pré-fill BE fallback salaire annuel → rémunération mensuelle calculée', () => {
    component.aiData = {
      dateLicenciement: '2026-03-15',
      salaireBrutAnnuel: 42000,
    } as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.remunerationMensuelleBrute()).toBe(3500);
    expect(component.provenanceRemuneration()).toBe('IA');
  });

  it('FRANCE → aucun pré-fill', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      dateLicenciement: '2026-03-15',
      salaireBrutMensuel: 3500,
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
    expect(component.dateLicenciement()).toBeNull();
    expect(component.remunerationMensuelleBrute()).toBeNull();
  });

  // ============================================================
  // formValid()
  // ============================================================

  it('formValid : dateDebutGrossesse obligatoire', () => {
    component.dateLicenciement.set('2026-03-15');
    component.remunerationMensuelleBrute.set(3500);
    expect(component.formValid()).toBe(false);
    component.dateDebutGrossesse.set('2026-01-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : dateLicenciement obligatoire', () => {
    component.dateDebutGrossesse.set('2026-01-15');
    component.remunerationMensuelleBrute.set(3500);
    expect(component.formValid()).toBe(false);
    component.dateLicenciement.set('2026-03-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : remuneration > 0 obligatoire', () => {
    component.dateDebutGrossesse.set('2026-01-15');
    component.dateLicenciement.set('2026-03-15');
    expect(component.formValid()).toBe(false);
    component.remunerationMensuelleBrute.set(0);
    expect(component.formValid()).toBe(false);
    component.remunerationMensuelleBrute.set(-100);
    expect(component.formValid()).toBe(false);
    component.remunerationMensuelleBrute.set(3500);
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // calculate() — POST + body
  // ============================================================

  it('calculate POST : body complet + refresh + snackbar', () => {
    component.dateDebutGrossesse.set('2026-01-15');
    component.dateAccouchement.set('2026-10-15');
    component.dateCongeMaterniteFinale.set('2027-01-15');
    component.dateLicenciement.set('2026-03-15');
    component.grossesseNotifieeParEcrit.set(true);
    component.remunerationMensuelleBrute.set(3500);
    component.motifInvoqueParEmployeur.set('Réorganisation');

    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      dateDebutGrossesse: '2026-01-15',
      dateAccouchement: '2026-10-15',
      dateCongeMaterniteDebut: null,
      dateCongeMaterniteFinale: '2027-01-15',
      dateLicenciement: '2026-03-15',
      grossesseNotifieeParEcrit: true,
      remunerationMensuelleBrute: 3500,
      motifInvoqueParEmployeur: 'Réorganisation',
    });

    req.flush(response());
    expect(component.result()!.verdict).toBe('PROTECTION_PRESUMEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('calculate POST 400 → snackbar erreur', () => {
    component.dateDebutGrossesse.set('2026-01-15');
    component.dateLicenciement.set('2026-03-15');
    component.remunerationMensuelleBrute.set(3500);
    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Date invalide' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Date invalide', 'Fermer',
      expect.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ============================================================
  // Verdict 4 états — badge classes + libellés
  // ============================================================

  it('verdictClass : 4 valeurs mappées', () => {
    expect(component.verdictClass('HORS_PERIODE_PROTECTION')).toBe('verdict-ok');
    expect(component.verdictClass('PROTECTION_APPLICABLE_NON_NOTIFIEE')).toBe('verdict-warn');
    expect(component.verdictClass('PROTECTION_APPLICABLE')).toBe('verdict-critical');
    expect(component.verdictClass('PROTECTION_PRESUMEE')).toBe('verdict-severe');
    expect(component.verdictClass(undefined)).toBe('verdict-info');
  });

  it('verdictLabel : libellés FR alignés mini-spec', () => {
    expect(component.verdictLabel('HORS_PERIODE_PROTECTION')).toContain('Hors période');
    expect(component.verdictLabel('PROTECTION_APPLICABLE_NON_NOTIFIEE')).toContain('non notifiée');
    expect(component.verdictLabel('PROTECTION_APPLICABLE')).toContain('Protection applicable');
    expect(component.verdictLabel('PROTECTION_PRESUMEE')).toContain('charge de preuve');
  });

  it('verdictIcon : 4 icônes Material distinctes', () => {
    const icons = new Set([
      component.verdictIcon('HORS_PERIODE_PROTECTION'),
      component.verdictIcon('PROTECTION_APPLICABLE_NON_NOTIFIEE'),
      component.verdictIcon('PROTECTION_APPLICABLE'),
      component.verdictIcon('PROTECTION_PRESUMEE'),
    ]);
    expect(icons.size).toBe(4);
  });

  // ============================================================
  // Rendu DOM résultat — bannières chargePreuve + avertissement
  // ============================================================

  it('rendu PROTECTION_PRESUMEE → bannière severe + alerte charge preuve renversée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'PROTECTION_PRESUMEE',
      chargePreuveEmployeur: true,
    }));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-severe');
    expect(banner).not.toBeNull();

    const alert = fixture.nativeElement.querySelector('.result-alert--severe');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('Charge de la preuve inversée');
  });

  it('rendu HORS_PERIODE_PROTECTION → bannière ok + pas d\'indemnité affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'HORS_PERIODE_PROTECTION',
      licenciementDansLaPeriodeProtegee: false,
      indemniteForfaitaire: 0,
      chargePreuveEmployeur: false,
    }));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-ok');
    expect(banner).not.toBeNull();
    // Pas de ligne indemnité forfaitaire dans le breakdown (cachée).
    const html = fixture.nativeElement.innerHTML;
    expect(html).not.toContain('Indemnité forfaitaire (6 × rémunération)');
  });

  it('rendu PROTECTION_APPLICABLE → bannière critical + indemnité visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'PROTECTION_APPLICABLE',
      chargePreuveEmployeur: false,
      indemniteForfaitaire: 21000,
    }));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
    // Pas d'alerte charge preuve (chargePreuveEmployeur=false).
    const alert = fixture.nativeElement.querySelector('.result-alert--severe');
    expect(alert).toBeNull();
    // Ligne indemnité présente.
    expect(fixture.nativeElement.innerHTML).toContain('Indemnité forfaitaire (6 × rémunération)');
  });

  it('rendu avertissement → bannière ambre affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'PROTECTION_APPLICABLE',
      chargePreuveEmployeur: false,
      avertissement: 'Date fin protection indéterminée',
    }));
    fixture.detectChanges();

    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    // Au moins 1 (avertissement) — pas l'alerte severe (chargePreuveEmployeur false).
    expect(alerts.length).toBeGreaterThanOrEqual(1);
    expect(alerts[0].textContent).toContain('Date fin protection indéterminée');
  });

  // ============================================================
  // getPrefillCount (parité runtime)
  // ============================================================

  it('static getPrefillCount délégué au helper', () => {
    expect(LicenciementBeProtectionGrossesseSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { dateLicenciement: '2026-03-15', salaireBrutMensuel: 3500 },
    })).toBe(2);
    expect(LicenciementBeProtectionGrossesseSectionComponent.getPrefillCount({
      workspaceCountry: 'FRANCE',
      aiData: { dateLicenciement: '2026-03-15', salaireBrutMensuel: 3500 },
    })).toBe(0);
  });
});
