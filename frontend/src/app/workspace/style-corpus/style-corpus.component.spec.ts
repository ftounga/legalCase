import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { StyleCorpusComponent } from './style-corpus.component';
import { StyleCorpusDocumentSummary } from '../../core/models/style-corpus.model';
import { Workspace } from '../../core/models/workspace.model';

describe('StyleCorpusComponent', () => {
  let component: StyleCorpusComponent;
  let fixture: ComponentFixture<StyleCorpusComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const WS = 'ws-1';
  const CURRENT_URL = '/api/v1/workspaces/current';
  const BASE = `/api/v1/workspaces/${WS}/style-corpus/documents`;

  const workspace: Workspace = {
    id: WS,
    name: 'Cabinet Alpha',
    slug: 'alpha',
    planCode: 'PRO',
    status: 'ACTIVE',
  };

  function doc(
    overrides: Partial<StyleCorpusDocumentSummary> = {},
  ): StyleCorpusDocumentSummary {
    return {
      id: 'doc-1',
      originalFilename: 'conclusions-ref.pdf',
      status: 'DONE',
      active: true,
      createdAt: '2026-05-18T10:00:00Z',
      errorMessage: null,
      ...overrides,
    };
  }

  function el(testId: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(`[data-testid="${testId}"]`);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      imports: [
        StyleCorpusComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: MatDialog, useValue: dialogSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StyleCorpusComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  /** Monte le composant : résout le workspace puis renvoie la liste fournie. */
  function init(documents: StyleCorpusDocumentSummary[]): void {
    fixture.detectChanges();
    httpMock.expectOne(CURRENT_URL).flush(workspace);
    httpMock.expectOne(BASE).flush(documents);
    fixture.detectChanges();
  }

  it('au montage : résout le workspace puis GET la liste', () => {
    init([doc()]);
    expect(component.documents().length).toBe(1);
    expect(component.loading()).toBe(false);
    expect(el('documents-list')).toBeTruthy();
  });

  it('affiche l\'état vide quand le corpus est vide', () => {
    init([]);
    expect(component.isEmpty()).toBe(true);
    expect(el('empty-state')).toBeTruthy();
    expect(el('documents-list')).toBeFalsy();
  });

  it('affiche un document FAILED avec son errorMessage', () => {
    init([
      doc({ status: 'FAILED', errorMessage: 'Extraction impossible', active: false }),
    ]);
    const error = el('doc-error');
    expect(error).toBeTruthy();
    expect(error!.textContent).toContain('Extraction impossible');
  });

  it('upload : POST puis recharge la liste', () => {
    init([]);
    const file = new File(['x'], 'ref.pdf', { type: 'application/pdf' });

    component.onFileSelected({
      target: { files: [file], value: '' },
    } as unknown as Event);

    const post = httpMock.expectOne(BASE);
    expect(post.request.method).toBe('POST');
    post.flush({ id: 'doc-9', status: 'PENDING' });

    // Recharge déclenchée après l'upload réussi.
    const reload = httpMock.expectOne(BASE);
    expect(reload.request.method).toBe('GET');
    reload.flush([doc({ id: 'doc-9', status: 'PENDING' })]);
    fixture.detectChanges();

    expect(component.documents().length).toBe(1);
    expect(component.uploading()).toBe(false);
  });

  it('polling : recharge les documents PENDING jusqu\'à l\'état terminal', fakeAsync(() => {
    init([doc({ id: 'doc-2', status: 'PROCESSING', active: false })]);

    // Un tour de polling (3 s) : le document passe DONE.
    tick(3000);
    const poll = httpMock.expectOne(BASE);
    expect(poll.request.method).toBe('GET');
    poll.flush([doc({ id: 'doc-2', status: 'DONE', active: true })]);
    fixture.detectChanges();

    expect(component.documents()[0].status).toBe('DONE');

    // Plus de document transitoire : le polling s'arrête (aucune requête).
    tick(3000);
    httpMock.expectNone(BASE);
  }));

  it('toggle : PATCH active/inactif', () => {
    init([doc({ id: 'doc-1', status: 'DONE', active: true })]);

    component.toggleActive(doc({ id: 'doc-1', status: 'DONE', active: true }));

    const patch = httpMock.expectOne(`${BASE}/doc-1`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ active: false });
    patch.flush(doc({ id: 'doc-1', status: 'DONE', active: false }));
    fixture.detectChanges();

    expect(component.documents()[0].active).toBe(false);
  });

  it('suppression : confirmation MatDialog puis DELETE', () => {
    init([doc({ id: 'doc-1' })]);
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);

    component.confirmDelete(doc({ id: 'doc-1' }));
    expect(dialogSpy.open).toHaveBeenCalled();

    const del = httpMock.expectOne(`${BASE}/doc-1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
    fixture.detectChanges();

    expect(component.documents().length).toBe(0);
  });

  it('suppression annulée : aucune requête DELETE', () => {
    init([doc({ id: 'doc-1' })]);
    dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);

    component.confirmDelete(doc({ id: 'doc-1' }));

    httpMock.expectNone(`${BASE}/doc-1`);
    expect(component.documents().length).toBe(1);
  });

  it('erreur de chargement : écran indisponible + snackbar', () => {
    fixture.detectChanges();
    httpMock.expectOne(CURRENT_URL).flush(workspace);
    httpMock
      .expectOne(BASE)
      .flush('boom', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.unavailable()).toBe(true);
    expect(snackSpy.open).toHaveBeenCalled();
  });
});
