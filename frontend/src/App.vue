<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const usuario = ref(null)

async function carregarUsuario() {
  if (route.path === '/login') {
    usuario.value = null
    return
  }
  const resposta = await fetch('/usuario/logado')
  usuario.value = resposta.ok ? await resposta.json() : null
  if (!resposta.ok) router.replace('/login')
}

async function sair() {
  await fetch('/usuario/logout', { method: 'POST' })
  usuario.value = null
  router.replace('/login')
}

watch(() => route.fullPath, carregarUsuario, { immediate: true })
</script>

<template>
  <nav v-if="usuario" class="menu-principal">
    <RouterLink to="/">Inicio</RouterLink>
    <RouterLink to="/carros">{{ usuario.papel === 'ADMIN' ? 'Carros' : 'Meus carros' }}</RouterLink>
    <RouterLink v-if="usuario.papel === 'ADMIN'" to="/usuarios">Usuarios</RouterLink>
    <RouterLink v-if="usuario.papel === 'USER'" to="/usuarios">Minha conta</RouterLink>
    <RouterLink to="/fotos">Minhas fotos</RouterLink>
    <RouterLink v-if="usuario.papel === 'ADMIN'" to="/logs">Logs</RouterLink>
    <span class="usuario-menu">Logado como <b>{{ usuario.nome }}</b></span>
    <button type="button" @click="sair">Sair</button>
  </nav>

  <main>
    <RouterView />
  </main>
</template>
