import { Component, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Api, Carro, Usuario } from '../../core/api';

@Component({
  selector: 'app-vehicle-management',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './vehicle-management.html',
  styleUrl: './vehicle-management.scss',
})
export class VehicleManagement implements OnInit {
  carros: Carro[] = [];
  usuarios: Usuario[] = [];
  form: FormGroup;
  editando: number | null = null;
  imagem: File | null = null;
  loading = true;
  mensagem = '';
  erro = false;
  constructor(private api: Api, private fb: FormBuilder) {
    this.form = this.fb.group({
      modelo: ['', [Validators.required, Validators.minLength(2)]],
      ano: ['', [Validators.required, Validators.min(1900), Validators.max(2100)]],
      preco: ['', [Validators.required, Validators.min(0)]],
      usuarioId: ['', Validators.required]
    });
  }
  ngOnInit() {
    this.api.listarUsuarios().subscribe(itens => {
      this.usuarios = itens;
      const usuario = this.api.usuario();
      if (usuario) this.form.patchValue({ usuarioId: usuario.id });
    });
    this.listar();
  }
  listar() {
    this.loading = true;
    this.api.listarCarros().subscribe({
      next: itens => { this.carros = itens; this.loading = false; },
      error: () => { this.erro = true; this.mensagem = 'Não foi possível listar os veículos.'; this.loading = false; }
    });
  }
  selecionarImagem(event: Event) {
    this.imagem = (event.target as HTMLInputElement).files?.[0] || null;
  }
  salvar() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const dados = new FormData();
    dados.append('modelo', this.form.value.modelo);
    dados.append('ano', this.form.value.ano);
    dados.append('preco', this.form.value.preco);
    dados.append('usuarioId', this.form.value.usuarioId);
    if (this.imagem) dados.append('imagem', this.imagem);
    this.api.salvarCarro(dados, this.editando || undefined).subscribe({
      next: () => {
        this.erro = false;
        this.mensagem = this.editando ? 'Veículo atualizado.' : 'Veículo cadastrado.';
        this.cancelar();
        this.listar();
      },
      error: resposta => { this.erro = true; this.mensagem = resposta.error?.erro || 'Falha ao salvar veículo.'; }
    });
  }
  editar(carro: Carro) {
    this.editando = carro.id;
    this.form.patchValue({
      modelo: carro.modelo,
      ano: carro.ano,
      preco: carro.preco,
      usuarioId: carro.cadastradoPor?.id
    });
  }
  excluir(id: number) {
    if (!confirm('Excluir este veículo?')) return;
    this.api.excluirCarro(id).subscribe({ next: () => this.listar(), error: resposta => {
      this.erro = true; this.mensagem = resposta.error?.erro || 'Falha ao excluir.';
    }});
  }
  removerImagem(id: number) {
    this.api.removerImagem(id).subscribe({ next: () => this.listar(), error: resposta => {
      this.erro = true; this.mensagem = resposta.error?.erro || 'Falha ao remover imagem.';
    }});
  }
  alterarStatus(carro: Carro, status: Carro['status']) {
    this.api.alterarStatusCarro(carro.id, status).subscribe({
      next: () => {
        this.erro = false;
        this.mensagem = 'Situação do veículo atualizada.';
        this.listar();
      },
      error: resposta => {
        this.erro = true;
        this.mensagem = resposta.error?.erro || 'Falha ao atualizar situação do veículo.';
      }
    });
  }
  cancelar() {
    this.editando = null;
    this.imagem = null;
    this.form.reset({ usuarioId: this.api.usuario()?.id });
  }
}
