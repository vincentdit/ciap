import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, PageResponse, ArticleResponse, CreateArticleRequest
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class KnowledgeApi {
  private readonly base = `${environment.apiBaseUrl}/knowledge`;

  constructor(private http: HttpClient) {}

  listPublished(page = 0, size = 10, q?: string): Observable<ApiResponse<PageResponse<ArticleResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q?.trim()) params = params.set('q', q.trim());
    return this.http.get<ApiResponse<PageResponse<ArticleResponse>>>(this.base, { params });
  }

  listAll(page = 0, size = 10, q?: string): Observable<ApiResponse<PageResponse<ArticleResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q?.trim()) params = params.set('q', q.trim());
    return this.http.get<ApiResponse<PageResponse<ArticleResponse>>>(`${this.base}/all`, { params });
  }

  get(id: number): Observable<ApiResponse<ArticleResponse>> {
    return this.http.get<ApiResponse<ArticleResponse>>(`${this.base}/${id}`);
  }

  create(body: CreateArticleRequest): Observable<ApiResponse<ArticleResponse>> {
    return this.http.post<ApiResponse<ArticleResponse>>(this.base, body);
  }

  publish(id: number): Observable<ApiResponse<ArticleResponse>> {
    return this.http.post<ApiResponse<ArticleResponse>>(`${this.base}/${id}/publish`, {});
  }

  unpublish(id: number): Observable<ApiResponse<ArticleResponse>> {
    return this.http.post<ApiResponse<ArticleResponse>>(`${this.base}/${id}/unpublish`, {});
  }
}
