import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { StatusBadge } from '../../../core/components/status-badge/status-badge';
import { CASE_STATUSES, CASE_STATUS_LABELS, LegalCase, LegalCaseFilter } from '../../../core/models/legal-case';
import { LegalCaseService } from '../../../core/services/legal-case.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-legal-case-list',
  imports: [FormsModule, DatePipe, CurrencyPipe, StatusBadge],
  templateUrl: './legal-case-list.html',
})
export class LegalCaseList {

  private readonly service = inject(LegalCaseService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  readonly statuses = CASE_STATUSES;
  readonly statusLabels = CASE_STATUS_LABELS;

  readonly loading = signal(false);
  readonly cases = signal<LegalCase[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);

  filter: LegalCaseFilter = { status: null, party: null, court: null, filingDateFrom: null, filingDateTo: null };

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.search(this.filter, this.pageIndex(), this.pageSize(), 'createdAt,desc').subscribe({
      next: (page) => {
        this.cases.set(page.content);
        this.totalElements.set(page.page.totalElements);
        this.totalPages.set(page.page.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  clearFilters(): void {
    this.filter = { status: null, party: null, court: null, filingDateFrom: null, filingDateTo: null };
    this.applyFilters();
  }

  goToPage(index: number): void {
    if (index < 0 || index >= this.totalPages()) return;
    this.pageIndex.set(index);
    this.load();
  }

  newCase(): void {
    this.router.navigate(['/legal-cases/new']);
  }

  editCase(id: number): void {
    this.router.navigate(['/legal-cases', id, 'edit']);
  }

  deleteCase(c: LegalCase): void {
    if (!confirm(`Excluir o processo ${c.cnjNumber}?`)) return;
    this.service.delete(c.id).subscribe({
      next: () => {
        this.notifications.success('Processo excluído com sucesso.');
        if (this.cases().length === 1 && this.pageIndex() > 0) {
          this.pageIndex.update(i => i - 1);
        }
        this.load();
      },
    });
  }
}