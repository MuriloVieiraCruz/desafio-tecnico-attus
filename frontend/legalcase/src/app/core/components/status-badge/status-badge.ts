import { Component, computed, input } from '@angular/core';
import { CASE_STATUS_LABELS, CaseStatus } from '../../models/legal-case';

const STATUS_CLASSES: Record<CaseStatus, string> = {
  FILED: 'bg-violet-100 text-violet-700',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  SUSPENDED: 'bg-amber-100 text-amber-700',
  ARCHIVED: 'bg-slate-100 text-slate-600',
  CLOSED: 'bg-slate-200 text-slate-700',
};

@Component({
  selector: 'app-status-badge',
  template: `<span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
                  [class]="classes()">{{ label() }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<CaseStatus>();
  readonly label = computed(() => CASE_STATUS_LABELS[this.status()]);
  readonly classes = computed(() => STATUS_CLASSES[this.status()]);
}