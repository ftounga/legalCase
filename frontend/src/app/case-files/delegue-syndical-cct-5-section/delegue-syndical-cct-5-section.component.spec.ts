import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  DelegueSyndicalCct5SectionComponent,
} from './delegue-syndical-cct-5-section.component';
import {
  DelegueSyndicalCct5Response,
} from '../../core/models/delegue-syndical-cct-5.model';

describe('DelegueSyndicalCct5SectionComponent', () => {
  let component: DelegueSyndicalCct5SectionComponent;
  let fixture: ComponentFixture<DelegueSyndicalCct5SectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/delegue-syndical-cct-5';

  /**
   * Construit une réponse backend par défaut — verdict STATUT_RECONNU
   * (effectif 80 ≥ seuil 50, OS présente, désignation régulière +
   * notification employeur, ni CE ni CPPT → missions supplétives).
   */
  function response(
    overrides: Partial<DelegueSyndicalCct5Response> = {},
  ): DelegueSyndicalCct5Response {
    return {
      caseFileId: 'case-1',
      dateDesignation: '2026-01-10',
      effectifEntreprise: 80,
      seuilSectorielRequis: 50,
      presenceOsRepresentative: true,
      statutDesignation: 'DESIGNE_PAR_OS_REPRESENTATIVE',
      ceExistant: false,
      cpptExistant: false,
      verdict: 'STATUT_RECONNU',
      eligibleStatutDs: true,
      entrepriseDansChamp: true,
      designationReguliere: true,
      missionsExercables: [
        'Négociation CCT d\'entreprise (art. 3)',
        'Traitement des plaintes individuelles et collectives (art. 3)',
        'Information du personnel (art. 3)',
        'Suppléance des missions CE / CPPT (art. 24)',
      ],
      dateFinMandatIndicative: '2030-01-10',
      raison: 'STATUT_DS_RECONNU',
      synthese: 'Statut de délégué syndical CCT n° 5 reconnu — missions exerçables avec suppléance CE/CPPT.',
      baseJuridique: 'CCT n° 5 du 24/05/1971 (CNT) + AR 26/01/1972 + CCT sectorielle applicable',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        DelegueSyndicalCct5SectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DelegueSyndicalCct5SectionComponent);
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
    expect(component.result()!.verdict).toBe('STATUT_RECONNU');
    expect(component.showForm()).toBe(false);
    expect(component.dateDesignation()).toBe('2026-01-10');
    expect(component.effectifEntreprise()).toBe(80);
    expect(component.seuilSectorielRequis()).toBe(50);
    expect(component.presenceOsRepresentative()).toBe(true);
    expect(component.statutDesignation()).toBe('DESIGNE_PAR_OS_REPRESENTATIVE');
    expect(component.ceExistant()).toBe(false);
    expect(component.cpptExistant()).toBe(false);
  });

  it('GET 404 → reste en mode formulaire vierge', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
    // Seuil par défaut métier conservé.
    expect(component.seuilSectorielRequis()).toBe(50);
  });

  // ============================================================
  // Form valid
  // ============================================================

  it('formValid() : false sans champs', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec 4 champs requis remplis', () => {
    component.dateDesignation.set('2026-01-10');
    component.effectifEntreprise.set(80);
    // seuilSectorielRequis défaut 50
    component.statutDesignation.set('DESIGNE_PAR_OS_REPRESENTATIVE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si seuilSectorielRequis < 1', () => {
    component.dateDesignation.set('2026-01-10');
    component.effectifEntreprise.set(80);
    component.statutDesignation.set('DESIGNE_PAR_OS_REPRESENTATIVE');
    component.onSeuilSectorielRequisChange(0);
    // Handler force défaut 50 si null → mais 0 explicite reste 0.
    component.seuilSectorielRequis.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateDesignation', () => {
    component.effectifEntreprise.set(80);
    component.statutDesignation.set('DESIGNE_PAR_OS_REPRESENTATIVE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans effectifEntreprise', () => {
    component.dateDesignation.set('2026-01-10');
    component.statutDesignation.set('DESIGNE_PAR_OS_REPRESENTATIVE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans statutDesignation', () => {
    component.dateDesignation.set('2026-01-10');
    component.effectifEntreprise.set(80);
    expect(component.formValid()).toBe(false);
  });

  it('handler seuil null → défaut 50', () => {
    component.onSeuilSectorielRequisChange(null);
    expect(component.seuilSectorielRequis()).toBe(50);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  function fillForm() {
    component.dateDesignation.set('2026-01-10');
    component.effectifEntreprise.set(80);
    component.seuilSectorielRequis.set(50);
    component.presenceOsRepresentative.set(true);
    component.statutDesignation.set('DESIGNE_PAR_OS_REPRESENTATIVE');
    component.ceExistant.set(false);
    component.cpptExistant.set(false);
  }

  it('calculate() → POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.dateDesignation).toBe('2026-01-10');
    expect(post.request.body.effectifEntreprise).toBe(80);
    expect(post.request.body.seuilSectorielRequis).toBe(50);
    expect(post.request.body.presenceOsRepresentative).toBe(true);
    expect(post.request.body.statutDesignation).toBe('DESIGNE_PAR_OS_REPRESENTATIVE');
    expect(post.request.body.ceExistant).toBe(false);
    expect(post.request.body.cpptExistant).toBe(false);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Statut délégué syndical analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
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
    fillForm();
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
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE → snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    fillForm();
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icône (5 cas)
  // ============================================================

  it('verdict STATUT_RECONNU → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'STATUT_RECONNU' }));
    expect(component.verdictLabel()).toContain('Statut DS pleinement reconnu');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict STATUT_FRAGILE_NOTIFICATION_MANQUANTE → label, verdict-warn, warning', () => {
    component.result.set(response({
      verdict: 'STATUT_FRAGILE_NOTIFICATION_MANQUANTE',
      eligibleStatutDs: true,
      designationReguliere: true,
      statutDesignation: 'NOTIFICATION_EMPLOYEUR_MANQUANTE',
    }));
    expect(component.verdictLabel()).toContain('Statut fragile');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning');
  });

  it('verdict INELIGIBLE_ENTREPRISE_HORS_CHAMP → label, critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_ENTREPRISE_HORS_CHAMP',
      eligibleStatutDs: false,
      entrepriseDansChamp: false,
      effectifEntreprise: 30,
    }));
    expect(component.verdictLabel()).toContain('hors champ sectoriel');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict INELIGIBLE_PAS_DESIGNE_PAR_OS → label, critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_PAS_DESIGNE_PAR_OS',
      eligibleStatutDs: false,
      designationReguliere: false,
      statutDesignation: 'PAS_DESIGNE_PAR_OS',
    }));
    expect(component.verdictLabel()).toContain('pas de désignation par OS');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict A_ANALYSER → label, verdict-info, help', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      eligibleStatutDs: false,
      statutDesignation: 'DESIGNATION_CONTESTEE',
    }));
    expect(component.verdictLabel()).toContain('À analyser');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('help');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (5 cas)
  // ============================================================

  it('badge STATUT_RECONNU = vert "STATUT RECONNU"', () => {
    expect(component.verdictBadge('STATUT_RECONNU')).toBe('STATUT RECONNU');
    expect(component.verdictBadgeClass('STATUT_RECONNU')).toBe('badge-ok');
  });

  it('badge STATUT_FRAGILE_NOTIFICATION_MANQUANTE = ambre "STATUT FRAGILE"', () => {
    expect(component.verdictBadge('STATUT_FRAGILE_NOTIFICATION_MANQUANTE')).toBe('STATUT FRAGILE');
    expect(component.verdictBadgeClass('STATUT_FRAGILE_NOTIFICATION_MANQUANTE')).toBe('badge-warn');
  });

  it('badge INELIGIBLE_ENTREPRISE_HORS_CHAMP = rouge "HORS CHAMP"', () => {
    expect(component.verdictBadge('INELIGIBLE_ENTREPRISE_HORS_CHAMP')).toBe('HORS CHAMP');
    expect(component.verdictBadgeClass('INELIGIBLE_ENTREPRISE_HORS_CHAMP')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_PAS_DESIGNE_PAR_OS = rouge "PAS DÉSIGNÉ OS"', () => {
    expect(component.verdictBadge('INELIGIBLE_PAS_DESIGNE_PAR_OS')).toBe('PAS DÉSIGNÉ OS');
    expect(component.verdictBadgeClass('INELIGIBLE_PAS_DESIGNE_PAR_OS')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = info "À ANALYSER"', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-info');
  });

  // ============================================================
  // DOM integration : badge header + missions + fin mandat
  // ============================================================

  it('STATUT_RECONNU → badge vert + bloc missions + fin de mandat dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.section-badge');
    expect(badge).not.toBeNull();
    expect(badge.textContent.trim()).toBe('STATUT RECONNU');
    expect(badge.classList.contains('badge-ok')).toBe(true);

    const html = fixture.nativeElement.textContent;
    expect(html).toContain('Fin de mandat indicative');
    expect(html).toContain('2030-01-10');
    expect(html).toContain('Missions exerçables');
    expect(html).toContain('Suppléance des missions CE / CPPT');
  });

  it('INELIGIBLE_ENTREPRISE_HORS_CHAMP → badge rouge dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_ENTREPRISE_HORS_CHAMP',
      eligibleStatutDs: false,
      entrepriseDansChamp: false,
      effectifEntreprise: 30,
      missionsExercables: [],
      dateFinMandatIndicative: null,
    }));
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.section-badge');
    expect(badge.classList.contains('badge-critical')).toBe(true);
  });

  // ============================================================
  // editMode
  // ============================================================

  it('editMode() → showForm = true', () => {
    component.result.set(response());
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // Pré-fill — invariant V1 = 0
  // ============================================================

  it('getPrefillCount(...) → 0 (V1)', () => {
    expect(DelegueSyndicalCct5SectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { dateRuptureContrat: '2026-09-30', motifRupture: 'autre' },
    })).toBe(0);
  });

  // ============================================================
  // Tool metadata statique (F-177 SF-177-03b)
  // ============================================================

  it('TOOL_LABEL + TOOL_ICON exposés', () => {
    expect(DelegueSyndicalCct5SectionComponent.TOOL_LABEL).toBe('DÉLÉGUÉ SYNDICAL (BE / CCT 5)');
    expect(DelegueSyndicalCct5SectionComponent.TOOL_ICON).toBe('how_to_reg');
  });

  // ============================================================
  // Labels enums lisibles
  // ============================================================

  it('statutDesignationLabel mappe les 4 enums + fallback', () => {
    expect(component.statutDesignationLabel('DESIGNE_PAR_OS_REPRESENTATIVE')).toContain('OS représentative');
    expect(component.statutDesignationLabel('NOTIFICATION_EMPLOYEUR_MANQUANTE')).toContain('notification employeur manquante');
    expect(component.statutDesignationLabel('PAS_DESIGNE_PAR_OS')).toContain('Pas de désignation');
    expect(component.statutDesignationLabel('DESIGNATION_CONTESTEE')).toContain('contestée');
    expect(component.statutDesignationLabel(null)).toBe('—');
  });
});
