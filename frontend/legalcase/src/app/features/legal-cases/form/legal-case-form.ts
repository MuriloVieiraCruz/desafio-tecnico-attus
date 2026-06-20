import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CreateLegalCaseRequest, UpdateLegalCaseRequest } from '../../../core/models/legal-case';
import { LegalCaseService } from '../../../core/services/legal-case.service';
import { NotificationService } from '../../../core/services/notification.service';
import { cnjValidator } from '../../../core/validators/cnj.validator';

const notFuture: ValidatorFn = (control) => {
  const value = control.value as string | null;
  if (!value) return null;
  return value > new Date().toISOString().slice(0, 10) ? { future: true } : null;
};

@Component({
  selector: 'app-legal-case-form',
  imports: [ReactiveFormsModule],
  templateUrl: './legal-case-form.html',
})
export class LegalCaseForm {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(LegalCaseService);
  private readonly notifications = inject(NotificationService);

  private id?: number;
  readonly editing = signal(false);
  readonly loading = signal(false);
  readonly submitting = signal(false);

  readonly form = this.fb.group({
    cnjNumber: ['', [Validators.required, cnjValidator]],
    plaintiff: ['', [Validators.required, Validators.maxLength(255)]],
    defendant: ['', [Validators.required, Validators.maxLength(255)]],
    court: ['', [Validators.required, Validators.maxLength(255)]],
    judicialDistrict: ['', [Validators.maxLength(255)]],
    claimValue: [null as number | null, [Validators.min(0)]],
    filingDate: [null as string | null, [notFuture]],
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = Number(idParam);
      this.editing.set(true);
      this.form.controls.cnjNumber.disable();
      this.loading.set(true);
      this.service.findById(this.id).subscribe({
        next: (c) => {
          this.form.patchValue(c);
          this.loading.set(false);
        },
        error: () => this.router.navigate(['/legal-cases']),
      });
    }
  }

  showError(name: string): boolean {
    const c = this.form.get(name);
    return !!c && c.invalid && c.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const v = this.form.getRawValue();
    const base = {
      plaintiff: v.plaintiff!,
      defendant: v.defendant!,
      court: v.court!,
      judicialDistrict: v.judicialDistrict || null,
      claimValue: v.claimValue ?? null,
      filingDate: v.filingDate || null,
    };

    const request$ = this.id
      ? this.service.update(this.id, base as UpdateLegalCaseRequest)
      : this.service.create({ cnjNumber: v.cnjNumber!, ...base } as CreateLegalCaseRequest);

    request$.subscribe({
      next: () => {
        this.notifications.success(this.id ? 'Processo atualizado.' : 'Processo cadastrado.');
        this.router.navigate(['/legal-cases']);
      },
      error: () => this.submitting.set(false),
    });
  }

  cancel(): void {
    this.router.navigate(['/legal-cases']);
  }
}