import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeProtectionDelegueeSectionComponent,
} from './licenciement-be-protection-deleguee-section.component';
import {
  LicenciementBeProtectionDelegueeResponse,
} from '../../core/models/licenciement-be-protection-deleguee.model';

describe('LicenciementBeProtectionDelegueeSectionComponent', () => {
  let component: LicenciementBeProtectionDelegueeSectionComponent;
  let fixture: ComponentFixture<LicenciementBeProtectionDelegueeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-protection-deleguee';

  /**
   * Construit une réponse backend par défaut (élu protégé, dans la fenêtre,
   * indemnité 2 ans, délai 30 j toujours valable).
   */
  function response(
    overrides: Partial<LicenciementBeProtectionDelegueeResponse> = {},
  ): LicenciementBeProtectionDelegueeResponse {
    return {
      caseFileId: 'case-1',
      statutProtege: 'DELEGUE_SYNDICAL_ELU',
      ancienneteAnnees: 8,
      remunerationAnnuelleBrute: 42000,
      dateLicenciement: '2026-05-10',
      dateElectionOuMandat: '2024-05-15',
      demandeReintegrationDansTrente: false,
      employeurRefuseReintegration: false,
      circonstancesAggravantes: false,
      verdict: 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE',
      licenciementDansProtection: true,
      dateFinProtection: '2028-05-15',
      indemniteForfaitaire: 84000,
      anneesForfait: 2,
      dateLimiteDemandeReintegration: '2026-06-09',
      joursRestantsReintegration: 13,
      delaiReintegrationDepasse: false,
      baseJuridique: 'Loi du 19/03/1991 ; CCT n° 5 du 24/05/1971',
      avertissement: null,
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeProtectionDelegueeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeProtectionDelegueeSectionComponent);
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
    expect(component.result()!.verdict).toBe('LICENCIEMENT_INTERDIT_SANS_PROCEDURE');
    expect(component.showForm()).toBe(false);
    // Rehydratation form.
    expect(component.statutProtege()).toBe('DELEGUE_SYNDICAL_ELU');
    expect(component.ancienneteAnnees()).toBe(8);
    expect(component.remunerationAnnuelleBrute()).toBe(42000);
    expect(component.dateLicenciement()).toBe('2026-05-10');
    expect(component.dateElectionOuMandat()).toBe('2024-05-15');
    expect(component.demandeReintegrationDansTrente()).toBe(false);
    expect(component.employeurRefuseReintegration()).toBe(false);
    expect(component.circonstancesAggravantes()).toBe(false);
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

  it('formValid() : true avec champs requis remplis', () => {
    component.statutProtege.set('DELEGUE_SYNDICAL_ELU');
    component.ancienneteAnnees.set(5);
    component.remunerationAnnuelleBrute.set(42000);
    component.dateLicenciement.set('2026-05-10');
    component.dateElectionOuMandat.set('2024-05-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si ancienneté négative', () => {
    component.statutProtege.set('CANDIDAT_NON_ELU');
    component.ancienneteAnnees.set(-1);
    component.remunerationAnnuelleBrute.set(30000);
    component.dateLicenciement.set('2026-05-10');
    component.dateElectionOuMandat.set('2024-05-15');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si rémunération = 0', () => {
    component.statutProtege.set('CANDIDAT_NON_ELU');
    component.ancienneteAnnees.set(3);
    component.remunerationAnnuelleBrute.set(0);
    component.dateLicenciement.set('2026-05-10');
    component.dateElectionOuMandat.set('2024-05-15');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateLicenciement', () => {
    component.statutProtege.set('DELEGUE_SYNDICAL_ELU');
    component.ancienneteAnnees.set(5);
    component.remunerationAnnuelleBrute.set(42000);
    component.dateElectionOuMandat.set('2024-05-15');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateElectionOuMandat', () => {
    component.statutProtege.set('DELEGUE_SYNDICAL_ELU');
    component.ancienneteAnnees.set(5);
    component.remunerationAnnuelleBrute.set(42000);
    component.dateLicenciement.set('2026-05-10');
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // Cohérence employeurRefuseReintegration / demande
  // ============================================================

  it('décocher demande → décoche aussi refus employeur', () => {
    component.demandeReintegrationDansTrente.set(true);
    component.employeurRefuseReintegration.set(true);
    component.onDemandeReintegrationDansTrenteChange(false);
    expect(component.demandeReintegrationDansTrente()).toBe(false);
    expect(component.employeurRefuseReintegration()).toBe(false);
  });

  it('cocher demande → laisse refus libre', () => {
    component.employeurRefuseReintegration.set(false);
    component.onDemandeReintegrationDansTrenteChange(true);
    expect(component.demandeReintegrationDansTrente()).toBe(true);
    expect(component.employeurRefuseReintegration()).toBe(false);
  });

  it('employeurRefuseEditable computed reflète demande', () => {
    component.demandeReintegrationDansTrente.set(false);
    expect(component.employeurRefuseEditable()).toBe(false);
    component.demandeReintegrationDansTrente.set(true);
    expect(component.employeurRefuseEditable()).toBe(true);
  });

  // ============================================================
  // Conditionnel : checkbox refus employeur dans le DOM
  // ============================================================

  it('demande=false → checkbox refus employeur masquée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.demandeReintegrationDansTrente.set(false);
    fixture.detectChanges();
    const cb = fixture.nativeElement.querySelector('mat-checkbox[name="employeurRefuseReintegration"]');
    expect(cb).toBeNull();
  });

  it('demande=true → checkbox refus employeur affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.demandeReintegrationDansTrente.set(true);
    fixture.detectChanges();
    const cb = fixture.nativeElement.querySelector('mat-checkbox[name="employeurRefuseReintegration"]');
    expect(cb).not.toBeNull();
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.statutProtege.set('DELEGUE_SYNDICAL_ELU');
    component.ancienneteAnnees.set(8);
    component.remunerationAnnuelleBrute.set(42000);
    component.dateLicenciement.set('2026-05-10');
    component.dateElectionOuMandat.set('2024-05-15');
    component.demandeReintegrationDansTrente.set(true);
    component.employeurRefuseReintegration.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.statutProtege).toBe('DELEGUE_SYNDICAL_ELU');
    expect(post.request.body.ancienneteAnnees).toBe(8);
    expect(post.request.body.remunerationAnnuelleBrute).toBe(42000);
    expect(post.request.body.dateLicenciement).toBe('2026-05-10');
    expect(post.request.body.dateElectionOuMandat).toBe('2024-05-15');
    expect(post.request.body.demandeReintegrationDansTrente).toBe(true);
    expect(post.request.body.employeurRefuseReintegration).toBe(true);
    expect(post.request.body.circonstancesAggravantes).toBe(false);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Protection délégué BE analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack "invalides"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.statutProtege.set('CANDIDAT_NON_ELU');
    component.ancienneteAnnees.set(2);
    component.remunerationAnnuelleBrute.set(28000);
    component.dateLicenciement.set('2026-05-10');
    component.dateElectionOuMandat.set('2024-05-15');
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
    component.statutProtege.set('CANDIDAT_NON_ELU');
    component.ancienneteAnnees.set(2);
    component.remunerationAnnuelleBrute.set(28000);
    component.dateLicenciement.set('2026-05-10');
    component.dateElectionOuMandat.set('2024-05-15');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez réessayer.', 'Fermer', expect.anything(),
    );
  });

  // ============================================================
  // Verdict — label / classe / icône
  // ============================================================

  it('verdict INTERDIT → label, verdict-critical, gpp_bad', () => {
    component.result.set(response({ verdict: 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE' }));
    expect(component.verdictLabel()).toBe('Licenciement interdit sans procédure');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gpp_bad');
  });

  it('verdict HORS_PROTECTION → label, verdict-ok, verified', () => {
    component.result.set(response({
      verdict: 'HORS_PERIODE_PROTECTION',
      licenciementDansProtection: false,
      indemniteForfaitaire: null,
      anneesForfait: null,
      dateLimiteDemandeReintegration: null,
      joursRestantsReintegration: null,
    }));
    expect(component.verdictLabel()).toBe('Hors période de protection');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header
  // ============================================================

  it('INTERDIT → badge rouge LICENCIEMENT INTERDIT visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ verdict: 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE' }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('LICENCIEMENT INTERDIT');
  });

  it('HORS_PROTECTION → badge vert HORS PROTECTION visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'HORS_PERIODE_PROTECTION',
      licenciementDansProtection: false,
      indemniteForfaitaire: null,
      anneesForfait: null,
      dateLimiteDemandeReintegration: null,
      joursRestantsReintegration: null,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('HORS PROTECTION');
  });

  // ============================================================
  // Indemnité forfaitaire affichée
  // ============================================================

  it('INTERDIT + indemnité → bloc indemnité rendu avec montant + années', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE',
      indemniteForfaitaire: 84000,
      anneesForfait: 2,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.indemnite-block');
    expect(block).not.toBeNull();
    const amount = block.querySelector('.indemnite-amount');
    // Format français : 84 000,00 € (espace insécable possible)
    expect(amount.textContent).toContain('84');
    expect(amount.textContent).toContain('€');
    const detail = block.querySelector('.indemnite-detail');
    expect(detail.textContent).toContain('2');
  });

  it('INTERDIT + circonstances aggravantes → indemnité 4 ans affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE',
      circonstancesAggravantes: true,
      indemniteForfaitaire: 168000,
      anneesForfait: 4,
    }));
    fixture.detectChanges();
    const detail = fixture.nativeElement.querySelector('.indemnite-detail');
    expect(detail.textContent).toContain('4');
  });

  it('HORS_PROTECTION → pas de bloc indemnité', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'HORS_PERIODE_PROTECTION',
      licenciementDansProtection: false,
      indemniteForfaitaire: null,
      anneesForfait: null,
      dateLimiteDemandeReintegration: null,
      joursRestantsReintegration: null,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.indemnite-block');
    expect(block).toBeNull();
  });

  // ============================================================
  // Délai 30 jours — surlignage
  // ============================================================

  it('délai dépassé → classe delai-critical', () => {
    component.result.set(response({
      licenciementDansProtection: true,
      dateLimiteDemandeReintegration: '2026-04-01',
      joursRestantsReintegration: -10,
      delaiReintegrationDepasse: true,
    }));
    expect(component.delaiReintegrationClass()).toBe('delai-critical');
  });

  it('délai ≤ 7 jours → classe delai-critical', () => {
    component.result.set(response({
      licenciementDansProtection: true,
      joursRestantsReintegration: 5,
      delaiReintegrationDepasse: false,
    }));
    expect(component.delaiReintegrationClass()).toBe('delai-critical');
  });

  it('délai ≤ 30 jours → classe delai-warn', () => {
    component.result.set(response({
      licenciementDansProtection: true,
      joursRestantsReintegration: 20,
      delaiReintegrationDepasse: false,
    }));
    expect(component.delaiReintegrationClass()).toBe('delai-warn');
  });

  it('délai > 30 jours → classe delai-ok', () => {
    component.result.set(response({
      licenciementDansProtection: true,
      joursRestantsReintegration: 45,
      delaiReintegrationDepasse: false,
    }));
    expect(component.delaiReintegrationClass()).toBe('delai-ok');
  });

  it('joursRestants null → delaiReintegrationClass vide', () => {
    component.result.set(response({
      licenciementDansProtection: false,
      joursRestantsReintegration: null,
      dateLimiteDemandeReintegration: null,
    }));
    expect(component.delaiReintegrationClass()).toBe('');
  });

  it('délai 30j dépassé → bloc affiche label DÉPASSÉ', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      licenciementDansProtection: true,
      dateLimiteDemandeReintegration: '2026-04-01',
      joursRestantsReintegration: -10,
      delaiReintegrationDepasse: true,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.delai-reintegration.delai-critical');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('DÉPASSÉ');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non null → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Délai 30 jours dépassé — recours direct tribunal du travail (CJ art. 578)',
    }));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('tribunal');
  });

  // ============================================================
  // Statuts protégés — 4 cas labels
  // ============================================================

  it('libellés statut protégé : 4 cas + null', () => {
    expect(component.statutProtegeLabel('DELEGUE_SYNDICAL_ELU')).toContain('élu');
    expect(component.statutProtegeLabel('CANDIDAT_NON_ELU')).toContain('Candidat');
    expect(component.statutProtegeLabel('DELEGUE_CCT5')).toContain('CCT n° 5');
    expect(component.statutProtegeLabel('CONSEILLER_CPPT')).toContain('CPPT');
    expect(component.statutProtegeLabel(null)).toBe('—');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(LicenciementBeProtectionDelegueeSectionComponent.getPrefillCount({})).toBe(0);
    expect(LicenciementBeProtectionDelegueeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { dateLicenciement: '2026-05-10', salaireBrutMensuel: 3500 },
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

  it('toolId pour jurisprudence = licenciement-be-protection-deleguee', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('licenciement-be-protection-deleguee');
  });

  // ============================================================
  // Toggle collapse
  // ============================================================

  it('toggleCollapse() flips collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  // ============================================================
  // forceExpanded
  // ============================================================

  it('forceExpanded → collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });
});
