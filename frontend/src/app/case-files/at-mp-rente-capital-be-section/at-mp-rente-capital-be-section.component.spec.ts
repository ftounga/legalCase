import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  AtMpRenteCapitalBeSectionComponent,
} from './at-mp-rente-capital-be-section.component';
import {
  AtMpRenteCapitalBeResponse,
} from '../../core/models/at-mp-rente-capital-be.model';

describe('AtMpRenteCapitalBeSectionComponent', () => {
  let component: AtMpRenteCapitalBeSectionComponent;
  let fixture: ComponentFixture<AtMpRenteCapitalBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/at-mp-rente-capital-be';

  /**
   * Reponse backend par defaut — verdict RENTE_ANNUELLE_GE_19 (cas
   * favorable typique : taux IPP 25 pour cent, rente annuelle viagere).
   */
  function response(
    overrides: Partial<AtMpRenteCapitalBeResponse> = {},
  ): AtMpRenteCapitalBeResponse {
    return {
      caseFileId: 'case-1',
      origine: 'ACCIDENT_TRAVAIL',
      statutReconnaissance: 'RECONNU',
      dateConsolidation: '2024-06-15',
      tauxIpp: 25,
      remunerationBaseAnnuelle: 35000,
      dateNaissance: '1975-04-12',
      demandeConversionPartielle: false,
      dateDemandeConversion: null,
      ageConsolidationOverride: null,
      verdict: 'RENTE_ANNUELLE_GE_19',
      capitalisationDoffice: false,
      conversionPartiellePossible: false,
      ageConsolidationCalcule: 49,
      coefficientAge: 9.5,
      renteAnnuelle: 8750,
      capitalCapitalisation: null,
      capitalConversionPartielle: null,
      renteResiduelleApresConversion: null,
      raison: 'IPP_GE_19_RENTE_ANNUELLE_VIAGERE',
      synthese: 'Rente annuelle viagere 25 pour cent x 35 000 EUR = 8 750 EUR/an.',
      baseJuridique: 'Loi du 10/04/1971 art. 24 + AR du 21/12/1971 + AR du 24/02/2005.',
      avertissement: 'Indexation annuelle Loi 02/04/1965 art. 2.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        AtMpRenteCapitalBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AtMpRenteCapitalBeSectionComponent);
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

  it('BELGIQUE => isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE => isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE => banniere reserve droit BE + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
    expect(banner.textContent).toContain('CPAM');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 => hydrate result + mode resultat + rehydrate form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('RENTE_ANNUELLE_GE_19');
    expect(component.showForm()).toBe(false);
    expect(component.origine()).toBe('ACCIDENT_TRAVAIL');
    expect(component.statutReconnaissance()).toBe('RECONNU');
    expect(component.dateConsolidation()).toBe('2024-06-15');
    expect(component.tauxIpp()).toBe(25);
    expect(component.remunerationBaseAnnuelle()).toBe(35000);
    expect(component.dateNaissance()).toBe('1975-04-12');
    expect(component.demandeConversionPartielle()).toBe(false);
  });

  it('GET 404 => reste en mode formulaire vierge', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // Form valid
  // ============================================================

  it('formValid() : false sans champs', () => {
    expect(component.formValid()).toBe(false);
  });

  function fillForm() {
    component.origine.set('ACCIDENT_TRAVAIL');
    component.statutReconnaissance.set('RECONNU');
    component.dateConsolidation.set('2024-06-15');
    component.tauxIpp.set(25);
    component.remunerationBaseAnnuelle.set(35000);
    component.dateNaissance.set('1975-04-12');
    component.demandeConversionPartielle.set(false);
    component.dateDemandeConversion.set(null);
    component.ageConsolidationOverride.set(null);
  }

  it('formValid() : true avec les champs requis remplis (RECONNU)', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans origine', () => {
    fillForm();
    component.origine.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans statut reconnaissance', () => {
    fillForm();
    component.statutReconnaissance.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans remuneration de base', () => {
    fillForm();
    component.remunerationBaseAnnuelle.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec remuneration <= 0', () => {
    fillForm();
    component.remunerationBaseAnnuelle.set(0);
    expect(component.formValid()).toBe(false);
    component.remunerationBaseAnnuelle.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans date naissance', () => {
    fillForm();
    component.dateNaissance.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false en RECONNU sans tauxIpp', () => {
    fillForm();
    component.tauxIpp.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false en RECONNU sans dateConsolidation', () => {
    fillForm();
    component.dateConsolidation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true en EN_COURS sans taux IPP / consolidation', () => {
    fillForm();
    component.statutReconnaissance.set('EN_COURS');
    component.tauxIpp.set(null);
    component.dateConsolidation.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false avec tauxIpp hors bornes 0-100', () => {
    fillForm();
    component.tauxIpp.set(-5);
    expect(component.formValid()).toBe(false);
    component.tauxIpp.set(150);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec ageOverride hors bornes', () => {
    fillForm();
    component.ageConsolidationOverride.set(-1);
    expect(component.formValid()).toBe(false);
    component.ageConsolidationOverride.set(200);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() => POST + snack succes', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.origine).toBe('ACCIDENT_TRAVAIL');
    expect(post.request.body.statutReconnaissance).toBe('RECONNU');
    expect(post.request.body.tauxIpp).toBe(25);
    expect(post.request.body.remunerationBaseAnnuelle).toBe(35000);
    expect(post.request.body.dateNaissance).toBe('1975-04-12');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse rente AT/MP calculee', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 => snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'tauxIpp <= 100' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'tauxIpp <= 100', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 => snack generique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez reessayer.', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 404 => snack "Dossier introuvable"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE => snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    fillForm();
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icone (5 cas)
  // ============================================================

  it('verdict RENTE_ANNUELLE_GE_19 => info, payments', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('RENTE');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('payments');
  });

  it('verdict CAPITAL_FORFAITAIRE_LT_19 => info, savings', () => {
    component.result.set(response({
      verdict: 'CAPITAL_FORFAITAIRE_LT_19',
      tauxIpp: 12,
      capitalisationDoffice: true,
      renteAnnuelle: null,
      capitalCapitalisation: 45000,
    }));
    expect(component.verdictLabel()).toContain('CAPITAL UNIQUE');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('savings');
  });

  it('verdict INELIGIBLE_NON_RECONNU => critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_NON_RECONNU',
      statutReconnaissance: 'CONTESTE',
      tauxIpp: null,
      renteAnnuelle: null,
    }));
    expect(component.verdictLabel()).toContain('non reconnu');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict IPP_NON_DETERMINE => warn, hourglass_empty', () => {
    component.result.set(response({
      verdict: 'IPP_NON_DETERMINE',
      statutReconnaissance: 'EN_COURS',
      tauxIpp: null,
      dateConsolidation: null,
      renteAnnuelle: null,
    }));
    expect(component.verdictLabel()).toContain('pas encore arrete');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('hourglass_empty');
  });

  it('verdict A_QUALIFIER => neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
      tauxIpp: null,
      renteAnnuelle: null,
    }));
    expect(component.verdictLabel()).toContain('Inputs ambigus');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null => labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('account_balance');
  });

  // ============================================================
  // Badge dans header (5 cas)
  // ============================================================

  it('badge CAPITAL_FORFAITAIRE_LT_19 = info', () => {
    expect(component.verdictBadge('CAPITAL_FORFAITAIRE_LT_19')).toContain('CAPITAL');
    expect(component.verdictBadgeClass('CAPITAL_FORFAITAIRE_LT_19')).toBe('badge-info');
  });

  it('badge RENTE_ANNUELLE_GE_19 = info', () => {
    expect(component.verdictBadge('RENTE_ANNUELLE_GE_19')).toContain('RENTE');
    expect(component.verdictBadgeClass('RENTE_ANNUELLE_GE_19')).toBe('badge-info');
  });

  it('badge INELIGIBLE_NON_RECONNU = critical', () => {
    expect(component.verdictBadge('INELIGIBLE_NON_RECONNU')).toBe('NON RECONNU');
    expect(component.verdictBadgeClass('INELIGIBLE_NON_RECONNU')).toBe('badge-critical');
  });

  it('badge IPP_NON_DETERMINE = warn', () => {
    expect(component.verdictBadge('IPP_NON_DETERMINE')).toBe('IPP NON FIXE');
    expect(component.verdictBadgeClass('IPP_NON_DETERMINE')).toBe('badge-warn');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('A QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — rendu verdict
  // ============================================================

  it('RENTE_ANNUELLE_GE_19 => badge info rendu', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-info');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('RENTE');
  });

  // ============================================================
  // Libelles
  // ============================================================

  it('libelle origine : 2 cas + null', () => {
    expect(component.origineLabel('ACCIDENT_TRAVAIL')).toContain('Accident');
    expect(component.origineLabel('MALADIE_PROFESSIONNELLE')).toContain('Maladie');
    expect(component.origineLabel(null)).toBe('—');
  });

  it('libelle statut reconnaissance : 3 cas + null', () => {
    expect(component.statutReconnaissanceLabel('RECONNU')).toContain('Reconnu');
    expect(component.statutReconnaissanceLabel('CONTESTE')).toContain('Conteste');
    expect(component.statutReconnaissanceLabel('EN_COURS')).toContain('En cours');
    expect(component.statutReconnaissanceLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatDate : ISO -> jj/mm/aaaa', () => {
    expect(component.formatDate('2024-06-15')).toBe('15/06/2024');
    expect(component.formatDate(null)).toBe('—');
    expect(component.formatDate(undefined)).toBe('—');
    expect(component.formatDate('')).toBe('—');
  });

  it('formatBoolean : Oui / Non / —', () => {
    expect(component.formatBoolean(true)).toBe('Oui');
    expect(component.formatBoolean(false)).toBe('Non');
    expect(component.formatBoolean(null)).toBe('—');
    expect(component.formatBoolean(undefined)).toBe('—');
  });

  it('formatMontant : EUR fr-BE', () => {
    const v = component.formatMontant(8750);
    expect(v).toContain('8');
    expect(v).toContain('750');
    expect(v).toContain('€');
    expect(component.formatMontant(null)).toBe('—');
    expect(component.formatMontant(undefined)).toBe('—');
  });

  it('formatPourcentage : suffixe %', () => {
    expect(component.formatPourcentage(25)).toContain('25');
    expect(component.formatPourcentage(25)).toContain('%');
    expect(component.formatPourcentage(null)).toBe('—');
  });

  it('formatCoefficient : nombre formate', () => {
    const v = component.formatCoefficient(9.5);
    expect(v.replace(',', '.')).toContain('9.5');
    expect(component.formatCoefficient(null)).toBe('—');
  });

  it('formatAge : suffixe ans', () => {
    expect(component.formatAge(49)).toBe('49 ans');
    expect(component.formatAge(null)).toBe('—');
  });

  // ============================================================
  // Avertissement / synthese
  // ============================================================

  it('avertissement non vide => banniere ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Verifier indexation annuelle Loi 02/04/1965.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('indexation');
  });

  it('synthese affichee si presente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Rente annuelle 25 pour cent x 35 000 EUR.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Rente');
  });

  // ============================================================
  // F-177 SF-177-12 — parite getPrefillCount static
  // ============================================================

  it('static getPrefillCount => 0 (V1)', () => {
    expect(AtMpRenteCapitalBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(AtMpRenteCapitalBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1975-04-12',
        dateRuptureContrat: '2024-06-15',
        motifRupture: 'autre',
        anneesCarriereSalarie: 30,
        salaireBrutMensuel: 2900,
        salaireBrutAnnuel: 34800,
      },
    })).toBe(0);
  });

  // ============================================================
  // editMode => re-affiche le form
  // ============================================================

  it('editMode() => showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // F-JU-03 — citation jurisprudentielle bien cablee
  // ============================================================

  it('toolId pour jurisprudence = at-mp-rente-capital-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('at-mp-rente-capital-be');
  });

  // ============================================================
  // Toggle collapse + forceExpanded
  // ============================================================

  it('toggleCollapse() flips collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('forceExpanded => collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });

  // ============================================================
  // Handlers signals
  // ============================================================

  it('handlers mettent a jour les signals correctement', () => {
    component.onOrigineChange('ACCIDENT_TRAVAIL');
    expect(component.origine()).toBe('ACCIDENT_TRAVAIL');

    component.onStatutReconnaissanceChange('RECONNU');
    expect(component.statutReconnaissance()).toBe('RECONNU');

    component.onDateConsolidationChange('2024-06-15');
    expect(component.dateConsolidation()).toBe('2024-06-15');

    component.onTauxIppChange(25);
    expect(component.tauxIpp()).toBe(25);

    component.onTauxIppChange('30');
    expect(component.tauxIpp()).toBe(30);

    component.onTauxIppChange(null);
    expect(component.tauxIpp()).toBeNull();

    component.onRemunerationBaseAnnuelleChange(35000);
    expect(component.remunerationBaseAnnuelle()).toBe(35000);

    component.onRemunerationBaseAnnuelleChange('');
    expect(component.remunerationBaseAnnuelle()).toBeNull();

    component.onDateNaissanceChange('1975-04-12');
    expect(component.dateNaissance()).toBe('1975-04-12');

    component.onDateNaissanceChange('');
    expect(component.dateNaissance()).toBeNull();

    component.onDemandeConversionPartielleChange(true);
    expect(component.demandeConversionPartielle()).toBe(true);

    component.onDateDemandeConversionChange('2027-06-15');
    expect(component.dateDemandeConversion()).toBe('2027-06-15');

    // Off-toggle resets date.
    component.onDemandeConversionPartielleChange(false);
    expect(component.demandeConversionPartielle()).toBe(false);
    expect(component.dateDemandeConversion()).toBeNull();

    component.onAgeConsolidationOverrideChange(50);
    expect(component.ageConsolidationOverride()).toBe(50);

    component.onAgeConsolidationOverrideChange(null);
    expect(component.ageConsolidationOverride()).toBeNull();
  });
});
