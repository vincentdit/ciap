import { Injectable } from '@angular/core';
import { EN } from './messages.en';

@Injectable({ providedIn: 'root' })
export class I18nService {
  t(key: string): string {
    return EN[key] ?? key;
  }
}
