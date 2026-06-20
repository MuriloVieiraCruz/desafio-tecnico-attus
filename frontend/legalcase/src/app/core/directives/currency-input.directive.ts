import { Directive, ElementRef, HostListener, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Directive({
  selector: 'input[appCurrency]',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CurrencyInputDirective),
    multi: true,
  }],
})
export class CurrencyInputDirective implements ControlValueAccessor {

  private readonly el = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private onChange: (value: number | null) => void = () => {};
  private onTouched: () => void = () => {};

  private readonly formatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

  @HostListener('input')
  handleInput(): void {
    const digits = this.el.nativeElement.value.replace(/\D/g, '');
    if (!digits) {
      this.el.nativeElement.value = '';
      this.onChange(null);
      return;
    }
    const value = Number(digits) / 100;
    this.el.nativeElement.value = this.formatter.format(value);
    this.onChange(value);
  }

  @HostListener('blur') handleBlur(): void { this.onTouched(); }

  writeValue(value: number | null): void {
    this.el.nativeElement.value = value != null ? this.formatter.format(value) : '';
  }
  registerOnChange(fn: (value: number | null) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(isDisabled: boolean): void { this.el.nativeElement.disabled = isDisabled; }
}