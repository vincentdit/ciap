import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { KnowledgeApi } from '../../core/api/knowledge.api';
import { AuthService } from '../../core/auth/auth.service';
import { ArticleResponse, CreateArticleRequest } from '../../core/models/api.models';
import { ListSearchComponent, PagerComponent } from '../../shared/list-controls.component';

@Component({
  selector: 'app-knowledge',
  standalone: true,
  imports: [CommonModule, FormsModule, ListSearchComponent, PagerComponent],
  template: `
    <header class="page-head">
      <div>
        <h1>Knowledge portal</h1>
        <p class="muted">Industrial guidance, technical notes, and resources.</p>
      </div>
      @if (canauthor()) {
        <button class="btn btn-primary" (click)="toggleForm()">{{ showForm() ? 'Close' : '+ New article' }}</button>
      }
    </header>

    @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
    @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

    @if (showForm()) {
      <div class="card panel form-card">
        <h3>Write an article</h3>
        <div class="field"><label>Title *</label><input [(ngModel)]="f.title" /></div>
        <div class="grid-2">
          <div class="field"><label>Category</label><input [(ngModel)]="f.category" placeholder="e.g. Quality, Energy" /></div>
          <div class="field"><label>Publish now?</label>
            <select [(ngModel)]="f.published">
              <option [ngValue]="false">Save as draft</option>
              <option [ngValue]="true">Publish</option>
            </select>
          </div>
        </div>
        <div class="field"><label>Summary</label><input [(ngModel)]="f.summary" placeholder="One-line summary" /></div>
        <div class="field"><label>Body *</label><textarea [(ngModel)]="f.body" rows="8"></textarea></div>
        <button class="btn btn-primary" [disabled]="busy() || !f.title.trim() || !f.body.trim()" (click)="create()">
          @if (busy()) { <span class="spinner"></span> Saving… } @else { Save article }
        </button>
      </div>
    }

    <app-list-search placeholder="Title, summary, or category" (queryChange)="onQuery($event)" />

    @if (canauthor()) {
      <div class="toggle-line">
        <button class="chip" [class.on]="!showAll()" (click)="setScope(false)">Published</button>
        <button class="chip" [class.on]="showAll()" (click)="setScope(true)">All (incl. drafts)</button>
      </div>
    }

    @if (loading()) {
      <div class="card load-card">Loading articles…</div>
    } @else if (articles().length === 0) {
      <div class="card empty"><p>{{ query ? 'No articles match this search.' : 'No articles yet.' }}</p></div>
    } @else {
      <div class="article-grid">
        @for (a of articles(); track a.id) {
          <article class="card art-card">
            <div class="art-head">
              @if (a.category) { <span class="pill pill-neutral">{{ a.category }}</span> }
              @if (!a.published) { <span class="pill pill-warn">Draft</span> }
            </div>
            <h3>{{ a.title }}</h3>
            @if (a.summary) { <p class="art-sum">{{ a.summary }}</p> }
            <div class="art-foot">
              <button class="btn btn-ghost sm" (click)="open(a)">{{ expanded() === a.id ? 'Hide' : 'Read' }}</button>
              @if (canpublish() && !a.published) {
                <button class="btn btn-ghost sm" (click)="publish(a)">Publish</button>
              }
            </div>
            @if (expanded() === a.id) {
              <div class="art-body">{{ fullBody() }}</div>
            }
          </article>
        }
      </div>
      <app-pager [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
  styles: [`
    .page-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1.4rem; gap: 1rem; }
    .page-head h1 { font-size: 2rem; margin-bottom: 0.2rem; }
    .page-head p { margin: 0; font-size: 0.95rem; }
    .panel { padding: 1.4rem 1.6rem; margin-bottom: 1.2rem; }
    .form-card { max-width: 680px; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }
    .toggle-line { display: flex; gap: 0.5rem; margin-bottom: 1.2rem; }
    .chip { font-size: 0.82rem; font-weight: 600; padding: 0.4rem 0.85rem; border-radius: 100px; cursor: pointer; background: var(--card); color: var(--ink-soft); border: 1px solid var(--line-2); }
    .chip.on { background: var(--teal-700); color: #fff; border-color: var(--teal-700); }
    .load-card { padding: 2rem; color: var(--ink-faint); }
    .empty { padding: 2.5rem; text-align: center; }
    .empty p { color: var(--ink-faint); margin: 0; }
    .article-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem; }
    .art-card { padding: 1.3rem 1.5rem; }
    .art-head { display: flex; gap: 0.4rem; margin-bottom: 0.6rem; }
    .art-card h3 { font-size: 1.15rem; margin-bottom: 0.4rem; }
    .art-sum { font-size: 0.9rem; color: var(--ink-soft); line-height: 1.5; margin: 0 0 0.8rem; }
    .art-foot { display: flex; gap: 0.5rem; }
    .btn.sm { padding: 0.4rem 0.75rem; font-size: 0.85rem; }
    .art-body { margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--line); font-size: 0.92rem; line-height: 1.6; white-space: pre-wrap; color: var(--ink-soft); }
  `]
})
export class KnowledgeComponent implements OnInit {
  private api = inject(KnowledgeApi);
  private auth = inject(AuthService);

  protected articles = signal<ArticleResponse[]>([]);
  protected loading = signal(true);
  protected busy = signal(false);
  protected showForm = signal(false);
  protected showAll = signal(false);
  protected expanded = signal<number | null>(null);
  protected fullBody = signal<string>('');
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);
  protected page = signal(0);
  protected totalPages = signal(1);
  protected total = signal(0);
  protected query = '';

  private readonly size = 10;
  private ticket = 0;

  protected f: CreateArticleRequest = { title: '', category: '', summary: '', body: '', published: false };

  ngOnInit(): void { this.load(0); }

  canauthor(): boolean { return this.auth.hasAnyRole('ADMIN', 'MANAGER', 'EXPERT'); }
  canpublish(): boolean { return this.auth.hasAnyRole('ADMIN', 'MANAGER'); }

  toggleForm(): void { this.showForm.set(!this.showForm()); }
  setScope(all: boolean): void { this.showAll.set(all); this.load(0); }

  onQuery(q: string): void {
    this.query = q;
    this.load(0);
  }

  load(p: number): void {
    const ticket = ++this.ticket;
    if (this.articles().length === 0) this.loading.set(true);
    const src = this.showAll() && this.canauthor()
      ? this.api.listAll(p, this.size, this.query)
      : this.api.listPublished(p, this.size, this.query);
    src.subscribe({
      next: (res) => {
        if (ticket !== this.ticket) return;
        this.articles.set(res.data.content);
        this.page.set(res.data.page);
        this.totalPages.set(Math.max(1, res.data.totalPages));
        this.total.set(res.data.totalElements);
        this.loading.set(false);
      },
      error: () => {
        if (ticket !== this.ticket) return;
        this.err.set('Could not load articles.');
        this.loading.set(false);
      }
    });
  }

  open(a: ArticleResponse): void {
    if (this.expanded() === a.id) { this.expanded.set(null); return; }
    this.api.get(a.id).subscribe({
      next: (res) => { this.fullBody.set(res.data.body ?? ''); this.expanded.set(a.id); },
      error: () => this.err.set('Could not load the article.')
    });
  }

  create(): void {
    if (!this.f.title.trim() || !this.f.body.trim()) return;
    this.busy.set(true);
    this.clear();
    this.api.create({
      title: this.f.title.trim(),
      category: this.f.category || undefined,
      summary: this.f.summary || undefined,
      body: this.f.body,
      published: this.f.published
    }).subscribe({
      next: () => {
        this.busy.set(false);
        this.msg.set('Article saved.');
        this.f = { title: '', category: '', summary: '', body: '', published: false };
        this.showForm.set(false);
        this.load(0);
      },
      error: (e) => { this.busy.set(false); this.err.set(e?.error?.message || 'Could not save the article.'); }
    });
  }

  publish(a: ArticleResponse): void {
    this.api.publish(a.id).subscribe({
      next: () => { this.msg.set('Article published.'); this.load(this.page()); },
      error: () => this.err.set('Could not publish.')
    });
  }

  private clear(): void { this.msg.set(null); this.err.set(null); }
}
