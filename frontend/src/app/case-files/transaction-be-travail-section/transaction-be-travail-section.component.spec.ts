import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  TransactionBeTravailSectionComponent,
} from './transaction-be-travail-section.component';
import {
  TransactionBeTravailResponse,
} from '../../core/models/transaction-be-travail.model';

describe('TransactionBeTravailSectionComponent', () => {
  let component: TransactionBeTravailSectionComponent;
  let fixture: ComponentFixture<TransactionBeTravailSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/transaction-be-travail';

  function response(
    overrides: Partial<TransactionBeTravailResponse> = {},
  ): TransactionBeTravailResponse {
    return {
      caseFileId: 'case-1',
      montantTransactionBrut: 15000,
      indemniteLegaleEtimee: 18000,
      concessionsEmployeurDescrites: true,
      renonciationsListees: [
        'Renonciation à l\'action en réintégration',
        'Renonciation à l\'indemnité de rupture',
      ],
      renonciationOrdrePublicDetectee: false,
      mentionContestation: true,
      verdict: 'VALIDE',
      raisonInvalidite: null,
      ratioPourcentage: 83.3,
      avertissement: null,
      checklistRenonciations: [
        { item: 'Renonciation à l\'action en réintégration', valide: true },
        { item: 'Renonciation à l\'indemnité de rupture', valide: true },
      ],
      baseJuridique: 'Art. 2044 Code civil belge + Loi 03/07/1978 art. 6',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        TransactionBeTravailSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionBeTravailSectionComponent);
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

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INVALIDE',
      raisonInvalidite: 'ABSENCE_CONCESSIONS',
      concessionsEmployeurDescrites: false,
    }));
    expect(component.result()!.verdict).toBe('INVALIDE');
    expect(component.result()!.raisonInvalidite).toBe('ABSENCE_CONCESSIONS');
    expect(component.showForm()).toBe(false);
    // Form rehydraté pour permettre l'édition.
    expect(component.montantTransactionBrut()).toBe(15000);
    expect(component.concessionsEmployeurDescrites()).toBe(false);
    expect(component.mentionContestation()).toBe(true);
    // Textarea : 2 lignes restaurées (les renonciations).
    expect(component.renonciationsRaw().split('\n').length).toBe(2);
  });

  it('GET 404 → reste en mode formulaire vierge', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
    expect(component.montantTransactionBrut()).toBeNull();
  });

  // ============================================================
  // formValid() — montant > 0 obligatoire, autres optionnels
  // ============================================================

  it('formValid : montant > 0 obligatoire', () => {
    expect(component.formValid()).toBe(false);
    component.montantTransactionBrut.set(0);
    expect(component.formValid()).toBe(false);
    component.montantTransactionBrut.set(-100);
    expect(component.formValid()).toBe(false);
    component.montantTransactionBrut.set(15000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : indemnité légale optionnelle (peut rester null)', () => {
    component.montantTransactionBrut.set(15000);
    expect(component.formValid()).toBe(true);
    // Pas d'indemnité saisie → toujours valide.
    expect(component.indemniteLegaleEtimee()).toBeNull();
  });

  it('formValid : renonciations vides autorisées (verdict A_COMPLETER côté backend)', () => {
    component.montantTransactionBrut.set(15000);
    component.renonciationsRaw.set('');
    expect(component.formValid()).toBe(true);
    expect(component.renonciationsListees()).toEqual([]);
  });

  // ============================================================
  // renonciationsListees() — parsing textarea multi-lignes
  // ============================================================

  it('renonciationsListees : 2 lignes non vides parsées', () => {
    component.renonciationsRaw.set('Ligne A\nLigne B');
    expect(component.renonciationsListees()).toEqual(['Ligne A', 'Ligne B']);
  });

  it('renonciationsListees : lignes vides et espaces filtrées', () => {
    component.renonciationsRaw.set('  Ligne A  \n\n   \nLigne B\n');
    expect(component.renonciationsListees()).toEqual(['Ligne A', 'Ligne B']);
  });

  it('renonciationsListees : texte vide → liste vide', () => {
    component.renonciationsRaw.set('');
    expect(component.renonciationsListees()).toEqual([]);
    component.renonciationsRaw.set('   \n  ');
    expect(component.renonciationsListees()).toEqual([]);
  });

  // ============================================================
  // calculate() — POST + body — utilise la forme corrigée Public
  // ============================================================

  it('calculate POST : body complet + refresh + snackbar — clé `renonciationOrdrePublicDetectee` (forme corrigée)', () => {
    component.montantTransactionBrut.set(15000);
    component.indemniteLegaleEtimee.set(18000);
    component.concessionsEmployeurDescrites.set(true);
    component.renonciationsRaw.set('Renonciation A\nRenonciation B');
    component.renonciationOrdrePublicDetectee.set(false);
    component.mentionContestation.set(true);

    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      montantTransactionBrut: 15000,
      indemniteLegaleEtimee: 18000,
      concessionsEmployeurDescrites: true,
      renonciationsListees: ['Renonciation A', 'Renonciation B'],
      renonciationOrdrePublicDetectee: false,
      mentionContestation: true,
    });
    // Garde-fou explicite : la typo « Punlic » de la mini-spec NE doit PAS exister.
    expect(req.request.body).not.toHaveProperty('renonciationOrdrePunlicDetectee');

    req.flush(response());
    expect(component.result()!.verdict).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('calculate POST 400 → snackbar erreur', () => {
    component.montantTransactionBrut.set(15000);
    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Montant invalide' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Montant invalide', 'Fermer',
      expect.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ============================================================
  // Verdict 4 états — badge classes + libellés + icônes distincts
  // ============================================================

  it('verdictClass : 4 valeurs mappées', () => {
    expect(component.verdictClass('VALIDE')).toBe('verdict-ok');
    expect(component.verdictClass('INVALIDE')).toBe('verdict-critical');
    expect(component.verdictClass('INVALIDE_PARTIELLE')).toBe('verdict-warn');
    expect(component.verdictClass('A_COMPLETER')).toBe('verdict-info');
    expect(component.verdictClass(undefined)).toBe('verdict-info');
  });

  it('verdictLabel : libellés FR alignés mini-spec', () => {
    expect(component.verdictLabel('VALIDE')).toContain('valide');
    expect(component.verdictLabel('INVALIDE')).toContain('invalide');
    expect(component.verdictLabel('INVALIDE_PARTIELLE')).toContain('artiellement');
    expect(component.verdictLabel('A_COMPLETER')).toContain('compléter');
  });

  it('verdictIcon : 4 icônes Material distinctes', () => {
    const icons = new Set([
      component.verdictIcon('VALIDE'),
      component.verdictIcon('INVALIDE'),
      component.verdictIcon('INVALIDE_PARTIELLE'),
      component.verdictIcon('A_COMPLETER'),
    ]);
    expect(icons.size).toBe(4);
  });

  it('raisonLabel : 3 raisons mappées (FR)', () => {
    expect(component.raisonLabel('ABSENCE_CONCESSIONS')).toContain('concessions');
    expect(component.raisonLabel('RENONCIATION_ORDRE_PUBLIC')).toContain('ordre public');
    expect(component.raisonLabel('LITIGE_NON_VISE')).toContain('Litige');
    expect(component.raisonLabel(null)).toBe('—');
    expect(component.raisonLabel(undefined)).toBe('—');
  });

  // ============================================================
  // Rendu DOM résultat — bannière verdict + ratio + avertissement + checklist
  // ============================================================

  it('rendu VALIDE → bannière verte + ratio affiché en mono + pas d\'avertissement', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'VALIDE',
      raisonInvalidite: null,
      ratioPourcentage: 83.3,
      avertissement: null,
    }));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-ok');
    expect(banner).not.toBeNull();

    // Pas de bannière avertissement.
    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).toBeNull();

    // Ratio rendu en JetBrains Mono.
    const monoCells = fixture.nativeElement.querySelectorAll('.bd-value--mono');
    const ratioCell = Array.from(monoCells as NodeListOf<HTMLElement>)
      .find(el => el.textContent && el.textContent.includes('83.3'));
    expect(ratioCell).toBeDefined();
  });

  it('rendu INVALIDE → bannière critical + raison ABSENCE_CONCESSIONS lisible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INVALIDE',
      raisonInvalidite: 'ABSENCE_CONCESSIONS',
      concessionsEmployeurDescrites: false,
    }));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('concessions');
  });

  it('rendu INVALIDE_PARTIELLE → bannière ambre + raison RENONCIATION_ORDRE_PUBLIC', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INVALIDE_PARTIELLE',
      raisonInvalidite: 'RENONCIATION_ORDRE_PUBLIC',
      renonciationOrdrePublicDetectee: true,
      checklistRenonciations: [
        { item: 'Renonciation à indemnité de chômage', valide: false },
        { item: 'Renonciation à indemnité de rupture', valide: false },
      ],
    }));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-warn');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('ordre public');
  });

  it('rendu A_COMPLETER → bannière info + raison LITIGE_NON_VISE', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'A_COMPLETER',
      raisonInvalidite: 'LITIGE_NON_VISE',
      mentionContestation: false,
      checklistRenonciations: [],
    }));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Litige');
  });

  it('rendu avertissement (ratio < 50 %) → bannière ambre affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'VALIDE',
      ratioPourcentage: 27.8,
      avertissement: 'Ratio transaction/indemnité < 50 % — possible lésion à signaler à la cliente',
    }));
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('Ratio');
  });

  it('rendu ratio null → ligne ratio masquée (pas d\'indemnité fournie)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'VALIDE',
      indemniteLegaleEtimee: null,
      ratioPourcentage: null,
    }));
    fixture.detectChanges();

    const html = fixture.nativeElement.innerHTML;
    expect(html).not.toContain('Ratio (transaction / indemnité)');
  });

  it('rendu checklist : items valides (✅) et invalides (❌)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      checklistRenonciations: [
        { item: 'Renonciation A', valide: true },
        { item: 'Renonciation B', valide: false },
      ],
    }));
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('.checklist-item');
    expect(items.length).toBe(2);
    expect(items[0].classList.contains('checklist-item--invalid')).toBe(false);
    expect(items[1].classList.contains('checklist-item--invalid')).toBe(true);
  });

  it('rendu checklist vide → section checklist masquée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      checklistRenonciations: [],
      renonciationsListees: [],
    }));
    fixture.detectChanges();

    const section = fixture.nativeElement.querySelector('.checklist-section');
    expect(section).toBeNull();
  });

  // ============================================================
  // getPrefillCount (parité runtime) — V1 : toujours 0
  // ============================================================

  it('static getPrefillCount délégué au helper — V1 toujours 0', () => {
    expect(TransactionBeTravailSectionComponent.getPrefillCount({})).toBe(0);
    expect(TransactionBeTravailSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { salaireBrutMensuel: 3500, dateLicenciement: '2026-03-15' },
    })).toBe(0);
    expect(TransactionBeTravailSectionComponent.getPrefillCount({
      workspaceCountry: 'FRANCE',
      aiData: { salaireBrutMensuel: 3500 },
    })).toBe(0);
  });

  // ============================================================
  // Metadata statique F-177 SF-177-03b
  // ============================================================

  it('TOOL_LABEL + TOOL_ICON exposés (F-177)', () => {
    expect(TransactionBeTravailSectionComponent.TOOL_LABEL).toContain('TRANSACTION');
    expect(TransactionBeTravailSectionComponent.TOOL_ICON).toBe('handshake');
  });
});
