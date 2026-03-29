import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { CaseDeadlinesSectionComponent } from './case-deadlines-section.component';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

function makeDeadline(dueDate: string, label = 'Prescription'): CaseDeadline {
  return { id: 'd-1', label, dueDate, createdAt: '', updatedAt: '' };
}

describe('CaseDeadlinesSectionComponent', () => {
  let fixture: ComponentFixture<CaseDeadlinesSectionComponent>;
  let component: CaseDeadlinesSectionComponent;
  let deadlineServiceSpy: jasmine.SpyObj<CaseDeadlineService>;

  beforeEach(async () => {
    deadlineServiceSpy = jasmine.createSpyObj('CaseDeadlineService',
      ['list', 'create', 'update', 'delete']);
    deadlineServiceSpy.list.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CaseDeadlinesSectionComponent, NoopAnimationsModule],
      providers: [
        { provide: CaseDeadlineService, useValue: deadlineServiceSpy },
        { provide: MatSnackBar, useValue: { open: () => {} } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDeadlinesSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    fixture.detectChanges();
  });

  it('U-01: shows empty state when no deadlines', () => {
    expect(fixture.nativeElement.querySelector('.deadlines-empty')?.textContent)
      .toContain('Aucun délai');
  });

  it('U-02: deadline in the past has class deadline--past', () => {
    expect(component.deadlineClass('2020-01-01')).toBe('deadline--past');
  });

  it('U-03: deadline within 15 days has class deadline--soon', () => {
    const soon = new Date();
    soon.setDate(soon.getDate() + 7);
    const dateStr = soon.toISOString().slice(0, 10);
    expect(component.deadlineClass(dateStr)).toBe('deadline--soon');
  });

  it('U-04: add button disabled when label or date empty', () => {
    component.newLabel = '';
    component.newDueDate = '';
    fixture.detectChanges();
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.add-deadline-form button');
    expect(btn.disabled).toBeTrue();

    component.newLabel = 'Prescription';
    fixture.detectChanges();
    expect(btn.disabled).toBeTrue(); // date still empty

    component.newDueDate = '2026-06-01';
    fixture.detectChanges();
    expect(btn.disabled).toBeFalse();
  });

  it('U-05: addDeadline calls deadlineService.create with trimmed label and dueDate', () => {
    deadlineServiceSpy.create.and.returnValue(of(makeDeadline('2026-06-01')));
    component.newLabel = '  Prescription  ';
    component.newDueDate = '2026-06-01';
    component.addDeadline();
    expect(deadlineServiceSpy.create).toHaveBeenCalledWith('cf-1', 'Prescription', '2026-06-01');
  });
});
