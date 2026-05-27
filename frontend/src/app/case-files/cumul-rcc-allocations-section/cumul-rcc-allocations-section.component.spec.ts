import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  CumulRccAllocationsSectionComponent,
} from './cumul-rcc-allocations-section.component';
import {
  CumulRccAllocationsResponse,
} from '../../core/models/cumul-rcc-allocations.model';

describe('CumulRccAllocationsSectionComponent', () => {
  let component: CumulRccAllocationsSectionComponent;
  let fixture: ComponentFixture<CumulRccAllocationsSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/cumul-rcc-allocations';

  /**
   * Construit une réponse backend par défaut — verdict CUMUL_CONFORME
   * (cumul 2400 € ≤ rémunération nette 2600 €).
   */
  function response(
    overrides: Partial<CumulRccAllocationsResponse> = {},
  ): CumulRccAllocationsResponse {
    return {
      caseFileId: 'case-1',
      dateNaissance: '1962-04-10',
      dateDebutRcc: '2026-01-01',
      allocationChomageMensuelle: 1500,
      indemniteComplementaireMensuelle: 900,
      remunerationNetteReference: 2600,
      anneesCarriereTotale: 42,
      activiteComplementaire: 'AUCUNE',
      ageLegalPension: 65,
      verdict: 'CUMUL_CONFORME',
      conforme: true,
      ageADateDebutRcc: 63,
      cumulMensuelTotal: 2400,
      plafondRespecte: true,
      montantReductionIndemnite: 0,
      regimeDisponibilite: 'DISPONIBILITE_PASSIVE',
      ageLegalPensionAtteint: false,
      synthese: 'Cumul mensuel total 2400 € sous le plafond 2600 €.',
      baseJuridique: 'CCT n° 17 + AR 25/11/1991 + AR 03/05/2007 art. 22',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        CumulRccAllocationsSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CumulRccAllocationsSectionComponent);
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

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('CUMUL_CONFORME');
    expect(component.showForm()).toBe(false);
    expect(component.dateNaissance()).toBe('1962-04-10');
    expect(component.dateDebutRcc()).toBe('2026-01-01');
    expect(component.allocationChomageMensuelle()).toBe(1500);
    expect(component.indemniteComplementaireMensuelle()).toBe(900);
    expect(component.remunerationNetteReference()).toBe(2600);
    expect(component.anneesCarriereTotale()).toBe(42);
    expect(component.activiteComplementaire()).toBe('AUCUNE');
    expect(component.ageLegalPension()).toBe(65);
  });

  it('GET 404 → reste en mode formulaire vierge', () => {
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

  it('formValid() : true avec 7 champs requis remplis', () => {
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans dateNaissance', () => {
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans activiteComplementaire', () => {
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : ageLegalPension optionnel — sans valeur le form reste valide', () => {
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    expect(component.formValid()).toBe(true);
    expect(component.ageLegalPension()).toBeNull();
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    component.ageLegalPension.set(65);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.dateNaissance).toBe('1962-04-10');
    expect(post.request.body.dateDebutRcc).toBe('2026-01-01');
    expect(post.request.body.allocationChomageMensuelle).toBe(1500);
    expect(post.request.body.indemniteComplementaireMensuelle).toBe(900);
    expect(post.request.body.remunerationNetteReference).toBe(2600);
    expect(post.request.body.anneesCarriereTotale).toBe(42);
    expect(post.request.body.activiteComplementaire).toBe('AUCUNE');
    expect(post.request.body.ageLegalPension).toBe(65);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Cumul RCC analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'champ invalide' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'champ invalide', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 → snack générique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez réessayer.', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 404 → snack "Dossier introuvable"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE → snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    component.dateNaissance.set('1962-04-10');
    component.dateDebutRcc.set('2026-01-01');
    component.allocationChomageMensuelle.set(1500);
    component.indemniteComplementaireMensuelle.set(900);
    component.remunerationNetteReference.set(2600);
    component.anneesCarriereTotale.set(42);
    component.activiteComplementaire.set('AUCUNE');
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icône (4 cas)
  // ============================================================

  it('verdict CUMUL_CONFORME → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'CUMUL_CONFORME' }));
    expect(component.verdictLabel()).toBe('Cumul conforme aux plafonds');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict CUMUL_PLAFOND_DEPASSE → label, verdict-critical, trending_down', () => {
    component.result.set(response({
      verdict: 'CUMUL_PLAFOND_DEPASSE',
      plafondRespecte: false,
      montantReductionIndemnite: 300,
    }));
    expect(component.verdictLabel()).toBe('Plafond de cumul dépassé');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('trending_down');
  });

  it('verdict BASCULE_PENSION_LEGALE → label, verdict-warn, elderly', () => {
    component.result.set(response({
      verdict: 'BASCULE_PENSION_LEGALE',
      ageLegalPensionAtteint: true,
    }));
    expect(component.verdictLabel()).toBe('Bascule pension légale — RCC éteint');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('elderly');
  });

  it('verdict INCOMPATIBLE_ACTIVITE_NON_AUTORISEE → label, verdict-critical, gpp_bad', () => {
    component.result.set(response({
      verdict: 'INCOMPATIBLE_ACTIVITE_NON_AUTORISEE',
      activiteComplementaire: 'ACTIVITE_NON_DECLAREE',
    }));
    expect(component.verdictLabel()).toBe('Activité non autorisée — sortie du régime');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gpp_bad');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (4 cas)
  // ============================================================

  it('badge CUMUL_CONFORME = vert "CONFORME"', () => {
    expect(component.verdictBadge('CUMUL_CONFORME')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CUMUL_CONFORME')).toBe('badge-ok');
  });

  it('badge CUMUL_PLAFOND_DEPASSE = rouge "PLAFOND DÉPASSÉ"', () => {
    expect(component.verdictBadge('CUMUL_PLAFOND_DEPASSE')).toBe('PLAFOND DÉPASSÉ');
    expect(component.verdictBadgeClass('CUMUL_PLAFOND_DEPASSE')).toBe('badge-critical');
  });

  it('badge BASCULE_PENSION_LEGALE = ambre "BASCULE PENSION"', () => {
    expect(component.verdictBadge('BASCULE_PENSION_LEGALE')).toBe('BASCULE PENSION');
    expect(component.verdictBadgeClass('BASCULE_PENSION_LEGALE')).toBe('badge-warn');
  });

  it('badge INCOMPATIBLE_ACTIVITE_NON_AUTORISEE = rouge "ACTIVITÉ INTERDITE"', () => {
    expect(component.verdictBadge('INCOMPATIBLE_ACTIVITE_NON_AUTORISEE')).toBe('ACTIVITÉ INTERDITE');
    expect(component.verdictBadgeClass('INCOMPATIBLE_ACTIVITE_NON_AUTORISEE')).toBe('badge-critical');
  });

  it('CUMUL_CONFORME → badge vert dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ verdict: 'CUMUL_CONFORME' }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
  });

  it('CUMUL_PLAFOND_DEPASSE → badge rouge dans le DOM + bannière réduction', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'CUMUL_PLAFOND_DEPASSE',
      plafondRespecte: false,
      montantReductionIndemnite: 300,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('PLAFOND');
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('300');
  });

  it('ageLegalPensionAtteint=true → bannière elderly affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'BASCULE_PENSION_LEGALE',
      ageLegalPensionAtteint: true,
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('pension');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellés activiteComplementaire : 4 cas + null', () => {
    expect(component.activiteComplementaireLabel('AUCUNE')).toContain('Aucune');
    expect(component.activiteComplementaireLabel('ACTIVITE_INDEPENDANT_ACCESSOIRE')).toContain('Indépendant');
    expect(component.activiteComplementaireLabel('ACTIVITE_BENEVOLE_DECLAREE')).toContain('Bénévolat');
    expect(component.activiteComplementaireLabel('ACTIVITE_NON_DECLAREE')).toContain('non déclarée');
    expect(component.activiteComplementaireLabel(null)).toBe('—');
  });

  it('libellés regimeDisponibilite : 3 cas + null', () => {
    expect(component.regimeDisponibiliteLabel('DISPONIBILITE_ADAPTEE')).toContain('adaptée');
    expect(component.regimeDisponibiliteLabel('DISPONIBILITE_PASSIVE')).toContain('passive');
    expect(component.regimeDisponibiliteLabel('DISPENSE_RECHERCHE_EMPLOI')).toContain('Dispense');
    expect(component.regimeDisponibiliteLabel(null)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'CUMUL_CONFORME',
      avertissement: 'Vérifier les annexes AR ONEM en vigueur',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('annexes');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Cumul mensuel total 2400 € sous le plafond 2600 €.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('2400');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(CumulRccAllocationsSectionComponent.getPrefillCount({})).toBe(0);
    expect(CumulRccAllocationsSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1962-04-10',
        dateRuptureContrat: '2026-12-31',
        motifRupture: 'licenciement',
        anneesCarriereSalarie: 42,
        salaireBrutMensuel: 4200,
      },
    })).toBe(0);
  });

  // ============================================================
  // editMode → re-affiche le form
  // ============================================================

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // F-JU-03 — citation jurisprudentielle bien câblée
  // ============================================================

  it('toolId pour jurisprudence = cumul-rcc-allocations', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('cumul-rcc-allocations');
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

  it('forceExpanded → collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });

  // ============================================================
  // Handlers signals
  // ============================================================

  it('handlers mettent à jour les signals correctement', () => {
    component.onDateNaissanceChange('1962-04-10');
    expect(component.dateNaissance()).toBe('1962-04-10');

    component.onDateDebutRccChange('2026-01-01');
    expect(component.dateDebutRcc()).toBe('2026-01-01');

    component.onAllocationChomageMensuelleChange(1500);
    expect(component.allocationChomageMensuelle()).toBe(1500);

    component.onIndemniteComplementaireMensuelleChange(900);
    expect(component.indemniteComplementaireMensuelle()).toBe(900);

    component.onRemunerationNetteReferenceChange(2600);
    expect(component.remunerationNetteReference()).toBe(2600);

    component.onAnneesCarriereTotaleChange(42);
    expect(component.anneesCarriereTotale()).toBe(42);

    component.onActiviteComplementaireChange('ACTIVITE_INDEPENDANT_ACCESSOIRE');
    expect(component.activiteComplementaire()).toBe('ACTIVITE_INDEPENDANT_ACCESSOIRE');

    component.onAgeLegalPensionChange(67);
    expect(component.ageLegalPension()).toBe(67);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(CumulRccAllocationsSectionComponent.TOOL_LABEL).toBe('CUMUL RCC + ALLOCATIONS (BE)');
    expect(CumulRccAllocationsSectionComponent.TOOL_ICON).toBe('account_balance_wallet');
  });
});
