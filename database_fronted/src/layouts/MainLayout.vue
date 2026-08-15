<template>
    <div class="app">
      <header class="topbar">
        <div class="container topbar-inner">
          <div class="brand" @click="goHome">
            <span class="brand-name">OSCAR</span>
          </div>
  
          <nav class="nav">
            <RouterLink
              v-for="item in NAV_ITEMS"
              :key="item.path"
              :to="item.path"
              class="nav-item"
              :class="{
                active: activePath === item.path,
                pending: navigating && pendingPath === item.path,
              }"
              @click="beginNavigation(item.path)"
            >
              {{ item.name }}
            </RouterLink>
          </nav>
  
        </div>
        <div v-if="navigating" class="route-loading-track" aria-hidden="true">
          <span class="route-loading-bar"></span>
        </div>
        <span v-if="navigating" class="route-loading-status" role="status">Loading page…</span>
      </header>
  
      <main class="page">
        <RouterView v-slot="{ Component }">
          <KeepAlive include="AnalysisView,SearchView">
            <component :is="Component" />
          </KeepAlive>
        </RouterView>
      </main>
  
      <footer class="footer">
        <div class="container footer-inner">
            <div class="footer-left">
            Copyright {{ new Date().getFullYear() }} © USC 
            </div>

            <div class="footer-right">
                <span class="sep">| </span>
                黑ICP备 16009434号-1
                <span class="sep">| </span>
                Li C Lab
            </div>
        </div>
      </footer>
        <BackToTop />
    </div>
  </template>
  
  <script setup lang="ts">
  import { computed, onBeforeUnmount, ref } from "vue";
  import { useRoute, useRouter } from "vue-router";
  import { NAV_ITEMS } from "@/config/nav";
  import BackToTop from "@/components/BackToTop.vue";
  
  const route = useRoute();
  const router = useRouter();
  const navigating = ref(false);
  const pendingPath = ref("");
  
  const goHome = () => router.push("/");

  function navPathFor(p: string, _source: unknown = "") {
    // Detail pages are destinations reached from several modules. Highlighting
    // Search or Analysis here incorrectly implies that the detail page belongs
    // to that navigation section.
    if (p.startsWith("/feature-detail") || p.startsWith("/sample/")) return "";
    if (p.startsWith("/search")) return "/search";
    if (p.startsWith("/browse") || p.startsWith("/data-browse")) return "/browse";
    if (p.startsWith("/analysis")) return "/analysis";
    if (p.startsWith("/download")) return "/download";
    if (p.startsWith("/stats")) return "/stats";
    if (p.startsWith("/contact")) return "/contact";
    if (p.startsWith("/helps")) return "/helps";
    // 其他同理
    return p;
  }

  const activePath = computed(() => navPathFor(route.path, route.query.source));

  function beginNavigation(path: string) {
    if (activePath.value === path) return;
    pendingPath.value = path;
    navigating.value = true;
  }

  const removeBeforeGuard = router.beforeEach((to) => {
    pendingPath.value = navPathFor(to.path, to.query.source);
    navigating.value = true;
    return true;
  });
  const removeAfterGuard = router.afterEach(() => {
    navigating.value = false;
    pendingPath.value = "";
  });
  const removeRouterErrorHandler = router.onError(() => {
    navigating.value = false;
    pendingPath.value = "";
  });

  onBeforeUnmount(() => {
    removeBeforeGuard();
    removeAfterGuard();
    removeRouterErrorHandler();
  });
  </script>
  
  <style scoped>
  .app { min-height: 100vh; display: flex; flex-direction: column; }

.topbar{
  position: sticky;
  background: var(--nav-bg);
  color: var(--nav-text);
  top: 0;
  z-index: 10000;
  width: 100%;
  height: 72px;
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid var(--nav-border);
  box-shadow: 0 6px 18px var(--border);
}
.topbar-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: stretch;
}
.route-loading-track {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.12);
}
.route-loading-bar {
  display: block;
  width: 38%;
  height: 100%;
  border-radius: 999px;
  background: var(--nav-active-text);
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.45);
  animation: route-loading-slide 0.9s ease-in-out infinite;
}
.route-loading-status {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
@keyframes route-loading-slide {
  from { transform: translateX(-115%); }
  to { transform: translateX(365%); }
}

.brand {
  flex: 0 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
}
.brand-name {
  color: #eaf3ef;
  font-size: 38px;
  font-weight: 900;
  letter-spacing: 1px;
  line-height: 72px;
  text-shadow: 0 1px 12px rgba(255, 255, 255, 0.18);
}

.nav {
  flex: 1 1 auto;
  display: flex;
  align-items: stretch;
  justify-content: center;
  gap: 34px;
  padding: 0 18px;

  overflow-x: auto;
  scrollbar-width: none;
}
.nav-item { font-size: 17px; font-weight: 700;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  padding: 0px 14px;
  border-radius: 10px;

  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.2px;
  white-space: nowrap;

  color: var(--nav-item-text);
}
.nav-item:hover { background: var(--nav-hover-bg); }

.nav-item.active {
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  border: 1px solid var(--nav-active-border);
  border-radius: 12px;
}
.nav-item.pending {
  background: var(--nav-hover-bg);
  color: var(--nav-active-text);
}

@media (prefers-reduced-motion: reduce) {
  .route-loading-bar { animation-duration: 1.8s; }
}

.page { flex: 1; }

.footer {
  border-top: 1px solid var(--border);
  padding: 14px 0;
  background: var(--surface-2);
}
.footer-inner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}
.footer-left{ white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.footer-right{ white-space: nowrap; opacity: .9; }
.sep{ margin: 0 8px; opacity: .7; }

@media (max-width: 768px) {
  .topbar { height: 56px; max-width: 100vw; overflow: hidden; }
  .topbar-inner { min-width: 0; max-width: calc(100vw - 24px); }
  .brand-name { font-size: 26px; line-height: 56px; }
  .brand { padding: 0 14px; }
  .nav {
    min-width: 0;
    max-width: 100%;
    justify-content: flex-start;
    gap: 8px;
    padding: 0 8px;
    -webkit-overflow-scrolling: touch;
  }
  .nav-item { flex: 0 0 auto; font-size: 13px; padding: 0 8px; border-radius: 8px; }
}
@media (max-width: 480px) {
  .brand-name { font-size: 22px; }
  .brand { padding: 0 8px; }
  .nav { gap: 4px; padding: 0 4px; }
  .nav-item { font-size: 12px; padding: 0 6px; }
}
@media (max-width: 980px){
  .footer-inner{ flex-direction: column; align-items: flex-start; }
  .footer-left, .footer-right{ white-space: normal; }
}
.muted { color: var(--muted); }
</style>
