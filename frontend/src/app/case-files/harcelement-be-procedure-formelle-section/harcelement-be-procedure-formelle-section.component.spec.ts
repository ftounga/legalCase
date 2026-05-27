import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  HarcelementBeProcedureFormelleSectionComponent,
} from './harcelement-be-procedure-formelle-section.component';
import {
  HarcelementBeProcedureFormelleResponse,
} from '../../core/models/harcelement-be-procedure-formelle.model';

describe('HarcelementBeProcedureFormelleSectionComponent', () => {
  let component: HarcelementBeProcedureFormelleSectionComponent;
  let fixture: ComponentFixture<HarcelementBeProcedureFormelleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/harcelement-be-procedure-formelle';

  /** Construit une réponse backend par défaut (DEMANDE_FORMELLE_EN_COURS). */
  function response(
    overrides: Partial<HarcelementBeProcedureFormelleResponse> = {},
  ): HarcelementBeProcedureFormelleResponse {
    return {
      caseFileId: 'case-1',
      typeHarcelement: 'MORAL',
      etapeProcedure: 'DEMANDE_FORMELLE_EN_COURS',
      dateDepotPlainte: '2026-05-01',
      entreprisePossedeCPAP: true,
      entrepriseTaille: 'CINQUANTE_ET_PLUS',
      mesureDefavorableApres: false,
      delaiDepuisDepotJours: 10,
      checklistItems: [
        { item: 'Enquête CPAP — délai 90 jours', statut: 'EN_COURS', dateEcheance: '2026-07-30' },
        { item: 'Protection représailles — 12 mois', statut: 'ACTIF', dateEcheance: '2027-05-01' },
      ],
      representaillesPossibles: false,
      dateDebutProtectionRepresailles: '2026-05-01',
      dateFinProtectionRepresailles: '2027-05-01',
      prochainDelaiFatal: '2026-07-30',
      baseJuridique: 'Loi du 04/08/1996 art. 32bis-32sexies ; AR du 10/04/2014',
      avertissement: null,
      ...overrides,
    };
  }

  /** Aujourd'hui +N jours, ISO yyyy-MM-dd. */
  function todayPlusDays(n: number): string {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() + n);
    return d.toISOString().slice(0, 10);
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        HarcelementBeProcedureFormelleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(HarcelementBeProcedureFormelleSectionComponent);
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
    expect(component.result()!.etapeProcedure).toBe('DEMANDE_FORMELLE_EN_COURS');
    expect(component.showForm()).toBe(false);
    // Rehydratation form.
    expect(component.typeHarcelement()).toBe('MORAL');
    expect(component.etapeProcedure()).toBe('DEMANDE_FORMELLE_EN_COURS');
    expect(component.dateDepotPlainte()).toBe('2026-05-01');
    expect(component.entrepriseTaille()).toBe('CINQUANTE_ET_PLUS');
    expect(component.entreprisePossedeCPAP()).toBe(true);
    expect(component.mesureDefavorableApres()).toBe(false);
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

  it('formValid() : true avec AVANT_DEPOT sans date', () => {
    component.typeHarcelement.set('MORAL');
    component.etapeProcedure.set('AVANT_DEPOT');
    component.entrepriseTaille.set('MOINS_DE_50');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false avec DEMANDE_FORMELLE sans date', () => {
    component.typeHarcelement.set('MORAL');
    component.etapeProcedure.set('DEMANDE_FORMELLE_EN_COURS');
    component.entrepriseTaille.set('CINQUANTE_ET_PLUS');
    // pas de dateDepotPlainte
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec DEMANDE_FORMELLE + date', () => {
    component.typeHarcelement.set('MORAL');
    component.etapeProcedure.set('DEMANDE_FORMELLE_EN_COURS');
    component.entrepriseTaille.set('CINQUANTE_ET_PLUS');
    component.dateDepotPlainte.set('2026-05-01');
    expect(component.formValid()).toBe(true);
  });

  it('changer étape → AVANT_DEPOT efface dateDepotPlainte', () => {
    component.typeHarcelement.set('MORAL');
    component.entrepriseTaille.set('MOINS_DE_50');
    component.dateDepotPlainte.set('2026-04-15');
    component.onEtapeProcedureChange('AVANT_DEPOT');
    expect(component.dateDepotPlainte()).toBeNull();
  });

  // ============================================================
  // Conditionnel : dateDepotPlainte field
  // ============================================================

  it('AVANT_DEPOT → field dateDepotPlainte masqué', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.etapeProcedure.set('AVANT_DEPOT');
    fixture.detectChanges();
    expect(component.dateDepotRequired()).toBe(false);
    const dateInput = fixture.nativeElement.querySelector('input[name="dateDepotPlainte"]');
    expect(dateInput).toBeNull();
  });

  it('DEMANDE_FORMELLE_EN_COURS → field dateDepotPlainte affiché', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.etapeProcedure.set('DEMANDE_FORMELLE_EN_COURS');
    fixture.detectChanges();
    expect(component.dateDepotRequired()).toBe(true);
    const dateInput = fixture.nativeElement.querySelector('input[name="dateDepotPlainte"]');
    expect(dateInput).not.toBeNull();
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeHarcelement.set('MORAL');
    component.etapeProcedure.set('DEMANDE_FORMELLE_EN_COURS');
    component.dateDepotPlainte.set('2026-05-01');
    component.entrepriseTaille.set('CINQUANTE_ET_PLUS');
    component.entreprisePossedeCPAP.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.typeHarcelement).toBe('MORAL');
    expect(post.request.body.etapeProcedure).toBe('DEMANDE_FORMELLE_EN_COURS');
    expect(post.request.body.dateDepotPlainte).toBe('2026-05-01');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Procédure harcèlement BE analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack "invalides"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeHarcelement.set('SEXUEL');
    component.etapeProcedure.set('AVANT_DEPOT');
    component.entrepriseTaille.set('MOINS_DE_50');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'champ invalide' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'champ invalide', 'Fermer', expect.anything(),
    );
  });

  // ============================================================
  // Délai fatal — surlignage
  // ============================================================

  it('délai fatal ≤ 30 jours → classe delai-critical', () => {
    component.result.set(response({
      prochainDelaiFatal: todayPlusDays(20),
    }));
    expect(component.delaiFatalClass()).toBe('delai-critical');
  });

  it('délai fatal ≤ 90 jours → classe delai-warn', () => {
    component.result.set(response({
      prochainDelaiFatal: todayPlusDays(60),
    }));
    expect(component.delaiFatalClass()).toBe('delai-warn');
  });

  it('délai fatal > 90 jours → classe delai-ok', () => {
    component.result.set(response({
      prochainDelaiFatal: todayPlusDays(180),
    }));
    expect(component.delaiFatalClass()).toBe('delai-ok');
  });

  it('délai fatal dépassé (négatif) → classe delai-critical', () => {
    component.result.set(response({
      prochainDelaiFatal: todayPlusDays(-5),
    }));
    expect(component.delaiFatalClass()).toBe('delai-critical');
  });

  it('pas de prochainDelaiFatal → delaiFatalClass() vide', () => {
    component.result.set(response({ prochainDelaiFatal: null }));
    expect(component.delaiFatalClass()).toBe('');
    expect(component.joursRestantsDelaiFatal()).toBeNull();
  });

  // ============================================================
  // Représailles + protection 12 mois
  // ============================================================

  it('representaillesPossibles=true → badge REPRÉSAILLES PROBABLES visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      representaillesPossibles: true,
      etapeProcedure: 'MESURE_DEFAVORABLE_APRES_PLAINTE',
      mesureDefavorableApres: true,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('REPRÉSAILLES PROBABLES');
  });

  it('representaillesPossibles=true → bloc protection 12 mois rendu', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      representaillesPossibles: true,
      dateDebutProtectionRepresailles: '2026-05-01',
      dateFinProtectionRepresailles: '2027-05-01',
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.representailles-block');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('2026-05-01');
    expect(block.textContent).toContain('2027-05-01');
  });

  // ============================================================
  // Bannière avertissement
  // ============================================================

  it('avertissement non null → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Entreprise ≥ 50 sans CPAP — manquement employeur',
    }));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('CPAP');
  });

  // ============================================================
  // 5 étapes — checklist différents statuts rendus
  // ============================================================

  it('checklist : 5 étapes représentées, statuts A_FAIRE / EN_COURS / TERMINE / ACTIF', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      checklistItems: [
        { item: 'Identifier le CPAP', statut: 'A_FAIRE', dateEcheance: null },
        { item: 'Demande informelle CPAP', statut: 'EN_COURS', dateEcheance: null },
        { item: 'Enquête CPAP — 90 j', statut: 'EN_COURS', dateEcheance: '2026-08-19' },
        { item: 'Décision employeur post-enquête', statut: 'TERMINE', dateEcheance: null },
        { item: 'Protection représailles 12 mois', statut: 'ACTIF', dateEcheance: '2027-05-01' },
      ],
    }));
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.checklist-item');
    expect(items.length).toBe(5);
    // Statuts visibles
    expect(component.statutLabel('A_FAIRE')).toBe('À faire');
    expect(component.statutLabel('EN_COURS')).toBe('En cours');
    expect(component.statutLabel('TERMINE')).toBe('Terminé');
    expect(component.statutLabel('ACTIF')).toBe('Actif');
    // Icônes
    expect(component.statutIcon('A_FAIRE')).toBe('pending');
    expect(component.statutIcon('EN_COURS')).toBe('autorenew');
    expect(component.statutIcon('TERMINE')).toBe('check_circle');
    expect(component.statutIcon('ACTIF')).toBe('shield');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(HarcelementBeProcedureFormelleSectionComponent.getPrefillCount({})).toBe(0);
    expect(HarcelementBeProcedureFormelleSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { harcelementBeDetecte: true },
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

  it('toolId pour jurisprudence = harcelement-be-procedure-formelle', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('harcelement-be-procedure-formelle');
  });
});
