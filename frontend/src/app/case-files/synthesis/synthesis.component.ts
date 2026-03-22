import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { CaseFile } from '../../core/models/case-file.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { AiQuestion } from '../../core/models/ai-question.model';

@Component({
  selector: 'app-synthesis',
  standalone: true,
  imports: [
    RouterLink, DatePipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatDividerModule
  ],
  templateUrl: './synthesis.component.html',
  styleUrl: './synthesis.component.scss'
})
export class SynthesisComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  questions = signal<AiQuestion[]>([]);
  loading = signal(true);
  reAnalyzing = signal(false);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private caseFileService: CaseFileService,
    private caseAnalysisService: CaseAnalysisService,
    private aiQuestionService: AiQuestionService,
    private reAnalysisService: ReAnalysisService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loadSynthesis(id);
        this.loadQuestions(id);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Dossier introuvable', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
      }
    });
  }

  private loadSynthesis(id: string): void {
    this.caseAnalysisService.getAnalysis(id).subscribe({
      next: result => {
        this.synthesis.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private loadQuestions(id: string): void {
    this.aiQuestionService.getQuestions(id).subscribe({
      next: qs => this.questions.set(qs),
      error: () => {}
    });
  }

  hasAnsweredQuestions(): boolean {
    return this.questions().some(q => q.answerText !== null);
  }

  reAnalyze(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.reAnalyzing.set(true);
    this.reAnalysisService.reAnalyze(id).subscribe({
      next: () => {
        this.reAnalyzing.set(false);
        this.router.navigate(['/case-files', id]);
      },
      error: (err: any) => {
        this.reAnalyzing.set(false);
        if (err.status === 402) return;
        this.snackBar.open('Erreur lors du déclenchement de la re-analyse', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }
}
