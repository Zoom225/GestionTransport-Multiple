import { ValidatorFn } from '@angular/forms';

/** Les filtres vides restent autorisés ; les champs obligatoires gardent leur propre validation. */
export const differentCitiesValidator = (departure: string, arrival: string): ValidatorFn =>
  (form) => {
    const from = String(form.get(departure)?.value ?? '').trim().toUpperCase();
    const to = String(form.get(arrival)?.value ?? '').trim().toUpperCase();
    return from && to && from === to ? { sameCity: true } : null;
  };
