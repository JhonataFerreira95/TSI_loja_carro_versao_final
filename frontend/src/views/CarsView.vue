<script setup>
import { onMounted, ref } from 'vue'
import { api, lerErro } from '../api'

const usuario = ref(null)
const administrador = ref(false)
const carros = ref([])
const contas = ref([])
const mensagem = ref('')
const erro = ref(false)
const editando = ref(false)
const carro = ref({ id: null, modelo: '', ano: null, preco: null, usuarioId: '' })
const imagem = ref(null)

function moeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

async function carregar() {
  const respostaUsuario = await api('/usuario/logado')
  if (!respostaUsuario.ok) return
  usuario.value = await respostaUsuario.json()
  administrador.value = usuario.value.papel === 'ADMIN'
  if (administrador.value) {
    const respostaContas = await api('/usuario/listarUsuarios')
    if (respostaContas.ok) contas.value = await respostaContas.json()
  }
  await listar()
  limpar()
}

async function listar() {
  const resposta = await api(administrador.value ? '/carro/listarCarros' : '/carro/meusCarros')
  if (resposta.ok) carros.value = await resposta.json()
  else await exibirErro(resposta)
}

async function salvar() {
  const dados = new FormData()
  dados.append('modelo', carro.value.modelo)
  dados.append('ano', carro.value.ano)
  dados.append('preco', carro.value.preco)
  if (administrador.value) dados.append('usuarioId', carro.value.usuarioId)
  if (imagem.value) dados.append('imagem', imagem.value)
  const resposta = await api(carro.value.id ? `/carro/${carro.value.id}` : '/carro/salvar', {
    method: carro.value.id ? 'PUT' : 'POST',
    body: dados,
  })
  if (resposta.ok) {
    mensagem.value = carro.value.id ? 'Carro atualizado com sucesso.' : 'Carro cadastrado com sucesso.'
    erro.value = false
    limpar()
    await listar()
  } else await exibirErro(resposta)
}

function editar(item) {
  carro.value = {
    id: item.id,
    modelo: item.modelo,
    ano: item.ano,
    preco: item.preco,
    usuarioId: item.cadastradoPor?.id || usuario.value.id,
  }
  imagem.value = null
  editando.value = true
}

async function excluir(id) {
  if (!confirm(`Deseja realmente excluir o carro ${id}?`)) return
  const resposta = await api(`/carro/${id}`, { method: 'DELETE' })
  if (resposta.ok) {
    mensagem.value = 'Carro excluído com sucesso.'
    erro.value = false
    await listar()
  } else await exibirErro(resposta)
}

async function removerImagem(id) {
  if (!confirm('Deseja realmente remover a foto deste carro?')) return
  const resposta = await api(`/carro/${id}/imagem`, { method: 'DELETE' })
  if (resposta.ok) {
    mensagem.value = 'Foto removida com sucesso.'
    erro.value = false
    await listar()
  } else await exibirErro(resposta)
}

function limpar() {
  carro.value = { id: null, modelo: '', ano: null, preco: null, usuarioId: usuario.value?.id || '' }
  imagem.value = null
  editando.value = false
}

async function exibirErro(resposta) {
  mensagem.value = await lerErro(resposta)
  erro.value = true
}

onMounted(carregar)
</script>

<template>
  <section v-if="usuario" class="grade duas-colunas">
    <div class="card">
      <h1>{{ administrador ? 'Cadastro de Carros' : 'Meus carros' }}</h1>
      <form @submit.prevent="salvar">
        <label>Modelo<input v-model="carro.modelo" required /></label>
        <label>Ano<input v-model="carro.ano" type="number" required /></label>
        <label>Preço<input v-model="carro.preco" type="number" step="0.01" min="0" required /></label>
        <label v-if="administrador">Conta associada
          <select v-model="carro.usuarioId" required>
            <option v-for="conta in contas" :key="conta.id" :value="conta.id">
              {{ conta.nome }}{{ conta.id === usuario.id ? ' (administrador)' : '' }}
            </option>
          </select>
        </label>
        <label>Imagem do computador (opcional)<input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="imagem = $event.target.files[0] || null" /></label>
        <div class="acoes">
          <button type="submit">{{ editando ? 'Atualizar' : 'Cadastrar' }}</button>
          <button v-if="editando" type="button" class="secundario" @click="limpar">Cancelar</button>
        </div>
      </form>
    </div>
    <div class="card">
      <h2>Carros cadastrados</h2>
      <p v-if="mensagem" :class="['mensagem', erro ? 'erro' : 'sucesso']">{{ mensagem }}</p>
      <div class="tabela-wrapper">
        <table>
          <thead>
            <tr><th>ID</th><th>Modelo</th><th>Ano</th><th>Preço</th><th v-if="administrador">Conta associada</th><th>Imagem</th><th v-if="administrador">Ações</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in carros" :key="item.id">
              <td>{{ item.id }}</td><td>{{ item.modelo }}</td><td>{{ item.ano }}</td><td>{{ moeda(item.preco) }}</td>
              <td v-if="administrador">{{ item.cadastradoPor?.nome || '' }}</td>
              <td><img v-if="item.imagem" :src="item.imagem" :alt="`Imagem do carro ${item.modelo}`" /></td>
              <td v-if="administrador">
                <div class="acoes">
                  <button type="button" @click="editar(item)">Editar</button>
                  <button type="button" class="perigo" @click="excluir(item.id)">Excluir</button>
                  <button v-if="item.imagem" type="button" class="secundario" @click="removerImagem(item.id)">Remover foto</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
