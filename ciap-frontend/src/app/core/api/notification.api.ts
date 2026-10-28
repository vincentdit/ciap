import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, AppNotification, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class NotificationApi {
  constructor(private http: HttpClient) {}

  list(page = 0, size = 10, q?: string): Observable<ApiResponse<PageResponse<AppNotification>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q?.trim()) params = params.set('q', q.trim());
    return this.http.get<ApiResponse<PageResponse<AppNotification>>>(`${environment.apiBaseUrl}/notifications`, { params });
  }
}
