import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  CATEGORY_ICONS,
  CATEGORY_MESSAGES,
  createCategoryForm,
  normalizeCategoryName,
} from './categoria-form';
import { Categoria, CategoriasService, CategoryInput } from './categorias.service';

type DialogMode = 'create' | 'edit' | 'delete' | null;

@Component({
  selector: 'app-categorias',
  imports: [ReactiveFormsModule],
  templateUrl: './categorias.html',
  styleUrl: './categorias.css',
})
export class Categorias implements OnInit {
  private readonly service = inject(CategoriasService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly form = createCategoryForm();
  readonly icons = CATEGORY_ICONS;
  readonly messages = CATEGORY_MESSAGES;
  categories: Categoria[] = [];
  dialog: DialogMode = null;
  selected?: Categoria;
  search = '';
  submitted = false;
  sending = false;
  duplicate = false;
  conflict = false;
  notice = '';
  private trigger?: HTMLElement;

  ngOnInit(): void { this.load(); }

  onSearch(event: Event): void {
    this.search = normalizeCategoryName((event.target as HTMLInputElement).value).slice(0, 40);
    this.load();
  }

  clearSearch(): void { this.search = ''; this.load(); }

  openCreate(trigger?: Event | HTMLElement): void {
    this.prepareDialog('create', undefined, trigger);
    this.form.reset({ name: '', icon: '' });
  }

  openEdit(category: Categoria, trigger?: Event | HTMLElement): void {
    this.prepareDialog('edit', category, trigger);
    this.form.reset({ name: category.name, icon: category.icon });
  }

  requestDelete(category: Categoria, trigger?: Event | HTMLElement): void {
    if (category.activeProductCount > 0) {
      this.notice = `Esta categoria possui ${category.activeProductCount} produtos ativos. `
        + 'Mova ou desative esses produtos antes de excluí-la.';
      this.changeDetector.markForCheck();
      return;
    }
    this.prepareDialog('delete', category, trigger);
  }

  save(): void {
    if (this.sending || !this.dialog || this.dialog === 'delete') return;
    this.submitted = true;
    this.duplicate = false;
    const name = normalizeCategoryName(this.form.controls.name.value);
    this.form.controls.name.setValue(name);
    if (this.form.invalid) {
      this.changeDetector.markForCheck();
      return;
    }
    this.sending = true;
    if (this.dialog === 'create') this.create({ name, icon: this.form.controls.icon.value });
    else this.update(name);
  }

  confirmDelete(): void {
    if (!this.selected || this.sending) return;
    this.sending = true;
    const category = this.selected;
    this.service.excluir(category.id, category.version).pipe(finalize(() => this.sending = false)).subscribe({
      next: () => {
        this.categories = this.categories.filter((item) => item.id !== category.id);
        this.close();
        this.notice = 'Categoria excluída com sucesso.';
      },
      error: () => {
        this.notice = 'Não foi possível excluir a categoria. Tente novamente em alguns instantes.';
        this.changeDetector.markForCheck();
      },
    });
  }

  reload(): void {
    if (!this.selected) return;
    this.service.obter(this.selected.id).subscribe({
      next: (category) => this.openEdit(category, this.trigger),
      error: () => this.notice = 'Categoria não encontrada ou indisponível para sua conta.',
    });
  }

  close(): void {
    this.dialog = null;
    this.selected = undefined;
    this.conflict = false;
    this.submitted = false;
    this.changeDetector.markForCheck();
    queueMicrotask(() => this.trigger?.focus());
  }

  @HostListener('document:keydown.escape')
  onEscape(): void { if (this.dialog) this.close(); }

  private load(): void {
    this.service.listar({ search: this.search }).subscribe({
      next: (collection) => {
        this.categories = this.sort(collection.items);
        this.changeDetector.markForCheck();
      },
      error: () => undefined,
    });
  }

  private create(input: CategoryInput): void {
    this.service.criar(input).pipe(finalize(() => this.sending = false)).subscribe({
      next: (category) => {
        this.categories = this.sort([...this.categories, category]);
        this.close();
        this.notice = 'Categoria criada com sucesso.';
      },
      error: (response) => this.handleSaveError(response, 'Não foi possível criar a categoria. '
        + 'Tente novamente em alguns instantes.'),
    });
  }

  private update(name: string): void {
    if (!this.selected || this.form.pristine) {
      this.sending = false;
      return;
    }
    const changes: Partial<CategoryInput> = {};
    if (this.form.controls.name.dirty) changes.name = name;
    if (this.form.controls.icon.dirty) changes.icon = this.form.controls.icon.value;
    this.service.atualizar(this.selected.id, changes, this.selected.version)
      .pipe(finalize(() => this.sending = false))
      .subscribe({
        next: (category) => {
          this.categories = this.sort(this.categories.map((item) => item.id === category.id ? category : item));
          this.close();
          this.notice = 'Categoria atualizada com sucesso.';
        },
        error: (response) => this.handleSaveError(response, 'Não foi possível atualizar a categoria. '
          + 'Tente novamente em alguns instantes.'),
      });
  }

  private handleSaveError(response: { error?: { code?: string } }, fallback: string): void {
    if (response?.error?.code === 'CATEGORY_NAME_ALREADY_IN_USE') this.duplicate = true;
    else if (response?.error?.code === 'CONFLICT') {
      this.conflict = true;
      this.notice = 'Esta categoria foi alterada em outro lugar. Recarregue os dados para continuar.';
    } else this.notice = fallback;
    this.changeDetector.markForCheck();
  }

  private prepareDialog(mode: DialogMode, category?: Categoria, source?: Event | HTMLElement): void {
    this.dialog = mode;
    this.selected = category;
    this.trigger = source instanceof Event ? source.currentTarget as HTMLElement : source;
    this.notice = '';
    this.duplicate = false;
    this.conflict = false;
    this.submitted = false;
    this.changeDetector.markForCheck();
  }

  private sort(categories: Categoria[]): Categoria[] {
    return categories.map((category, index) => ({ category, index }))
      .sort((left, right) => left.category.name.localeCompare(right.category.name, 'pt-BR')
        || left.index - right.index)
      .map(({ category }) => category);
  }
}
