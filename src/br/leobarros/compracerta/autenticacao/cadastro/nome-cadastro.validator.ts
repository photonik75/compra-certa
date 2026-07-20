import { AbstractControl, ValidationErrors } from '@angular/forms';

export function nomeCadastroValidator(
  controle: AbstractControl,
): ValidationErrors | null {
  const nome = controle.value;

  return typeof nome !== 'string' || nome.trim().length === 0
    ? { required: true }
    : null;
}
