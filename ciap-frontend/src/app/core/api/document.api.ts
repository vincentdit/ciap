import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, DocumentResponse, DocumentCategory } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DocumentApi {
  private readonly base = `${environment.apiBaseUrl}/documents`;

  constructor(private http: HttpClient) {}

  listForRequest(requestId: number): Observable<ApiResponse<DocumentResponse[]>> {
    return this.http.get<ApiResponse<DocumentResponse[]>>(`${this.base}/request/${requestId}`);
  }

  upload(requestId: number, file: File, category: DocumentCategory, description?: string):
      Observable<ApiResponse<DocumentResponse>> {
    const form = new FormData();
    form.append('requestId', String(requestId));
    form.append('category', category);
    if (description) form.append('description', description);
    form.append('file', file);
    return this.http.post<ApiResponse<DocumentResponse>>(this.base, form);
  }

  /** Download bytes as a Blob so the browser can save with the original filename. */
  download(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/download`, { responseType: 'blob' });
  }
}
