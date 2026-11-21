<script setup>
import { onMounted, ref } from 'vue'
import { api, enviarJson, lerErro } from '../api'

const usuario = ref(null)
const administrador = ref(false)
const usuarios = ref([])
const mensagem = ref('')
const erro = ref(false)
const editando = ref(false)
const form = ref({ id: null, nome: '', email: '', senha: '', papel: 'USER' })

async function carregar() {
  const resposta = await api('/usuario/logado')
  if (!resposta.ok) return
  usuario.value = await resposta.json()
  administrador.value = usuario.value.papel === 'ADMIN'
  if (administrador.value) await listar()
  else form.value = { id: usuario.value.id, nome: usuario.value.nome, email: usuario.value.email, senha: '', papel: usuario.value.papel }
}

async function listar() {
  const resposta = await api('/usuario/listarUsuarios')
  if (resposta.ok) usuarios.value = await resposta.json()
  else await exibirErro(resposta)
}

async function salvar() {
  const dados = { nome: form.value.nome, email: form.value.email, senha: form.value.senha }
  if (administrador.value) dados.papel = form.value.papel
  const resposta = form.value.id
    ? await enviarJson(`/usuario/${form.value.id}`, 'PUT', dados)
    : await enviarJson('/usuario/salvar', 'POST', dados)
  if (resposta.ok) {
    mensagem.value = form.value.id ? 'Usuário atualizado com sucesso.' : 'Usuário cadastrado com sucesso.'
    erro.value = false
    if (administrador.value) {
      limpar()
      await listar()
    } else {
      const atualizado = await resposta.json()
      form.value = { id: atualizado.id, nome: atualizado.nome, email: atualizado.email, senha: '', papel: atualizado.papel }
      usuario.value = atualizado
    }
  } else await exibirErro(resposta)
}

function editar(item) {
  form.value = { id: item.id, nome: item.nome, email: item.email, senha: '', papel: item.papel }
  editando.value = true
}

async function excluir(id) {
  if (!confirm(`Deseja realmente excluir o usuário ${id}?`)) return
  const resposta = await api(`/usuario/${id}`, { method: 'DELETE' })
  if (resposta.ok) {
    mensagem.value = 'Usuário excluído com sucesso.'
    erro.value = false
    await listar()
  } else await exibirErro(resposta)
}

function limpar() {
  form.value = { id: null, nome: '', email: '', senha: '', papel: 'USER' }
  editando.value = false
}

function formatarData(data) {
  return data ? new Date(data).toLocaleString('pt-BR') : ''
}

async function exibirErro(resposta) {
  mensagem.value = await lerErro(resposta)
  erro.value = true
}

onMounted(carregar)
</script>

<template>
  <section v-if="usuario" class="grade" :class="{ 'duas-colunas': administrador }">
    <div class="card">
      <h1>{{ administrador ? 'Gerenciar Usuários' : 'Minha conta' }}</h1>
      <p v-if="mensagem" :class="['mensagem', erro ? 'erro' : 'sucesso']">{{ mensagem }}</p>
      <form @submit.prevent="salvar">
        <label>Nome<input v-model="form.nome" required /></label>
        <label>E-mail<input v-model="form.email" type="email" required /></label>
        <label>Senha (mínimo 6 caracteres)<input v-model="form.senha" type="password" :placeholder="editando ? 'Deixe em branco para manter a atual' : ''" /></label>
        <label v-if="administrador">Papel
          <select v-model="form.papel">
            <option value="USER">Usuário</option>
            <option value="ADMIN">Administrador</option>
          </select>
        </label>
        <div class="acoes">
          <button type="submit">{{ editando ? 'Atualizar' : administrador ? 'Cadastrar' : 'Salvar alterações' }}</button>
          <button v-if="administrador && editando" type="button" class="secundario" @click="limpar">Cancelar</button>
        </div>
      </form>
    </div>
    <div v-if="administrador" class="card">
      <h2>Usuários cadastrados</h2>
      <div class="tabela-wrapper">
        <table>
          <thead><tr><th>ID</th><th>Nome</th><th>E-mail</th><th>Papel</th><th>Data de cadastro</th><th>Ações</th></tr></thead>
          <tbody>
            <tr v-for="item in usuarios" :key="item.id">
              <td>{{ item.id }}</td><td>{{ item.nome }}</td><td>{{ item.email }}</td><td>{{ item.papel }}</td><td>{{ formatarData(item.dataCadastro) }}</td>
              <td><div class="acoes"><button type="button" @click="editar(item)">Editar</button><button type="button" class="perigo" @click="excluir(item.id)">Excluir</button></div></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
