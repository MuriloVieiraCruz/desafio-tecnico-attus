import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const CNJ_PATTERN = /^\d{7}-\d{2}\.\d{4}\.\d\.\d{2}\.\d{4}$/;

export const cnjValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value as string | null;
  if (!value) return null;
  if (!CNJ_PATTERN.test(value)) return { cnj: true };
  return hasValidCheckDigits(value) ? null : { cnj: true };
};

function hasValidCheckDigits(v: string): boolean {
  const base = v.slice(0, 7) + v.slice(11, 15) + v.slice(16, 17) + v.slice(18, 20) + v.slice(21, 25) + v.slice(8, 10);
  let mod = 0;
  for (const ch of base) mod = (mod * 10 + (ch.charCodeAt(0) - 48)) % 97;
  return mod === 1;
}