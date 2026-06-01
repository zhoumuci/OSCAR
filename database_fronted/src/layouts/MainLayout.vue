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
              :class="{ active: activePath === item.path }"
            >
              {{ item.name }}
            </RouterLink>
          </nav>
  
          <a class="github" :href="githubUrl" target="_blank" rel="noreferrer" title="GitHub">
            <!-- 简单 GitHub 图标 -->
            <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="currentColor"
                d="M12 2C6.48 2 2 6.58 2 12.26c0 4.52 2.87 8.35 6.84 9.71c.5.1.68-.22.68-.48
                c0-.24-.01-.88-.01-1.72c-2.78.62-3.37-1.38-3.37-1.38c-.45-1.2-1.11-1.52-1.11-1.52
                c-.91-.64.07-.63.07-.63c1 .07 1.53 1.06 1.53 1.06c.9 1.57 2.36 1.12 2.94.85
                c.09-.67.35-1.12.64-1.38c-2.22-.26-4.55-1.15-4.55-5.1c0-1.13.39-2.05 1.03-2.77
                c-.1-.26-.45-1.3.1-2.7c0 0 .84-.27 2.75 1.06c.8-.23 1.65-.34 2.5-.34
                c.85 0 1.7.12 2.5.34c1.9-1.33 2.74-1.06 2.74-1.06c.56 1.4.21 2.44.1 2.7
                c.64.72 1.03 1.64 1.03 2.77c0 3.96-2.34 4.84-4.57 5.1c.36.32.68.95.68 1.92
                c0 1.38-.01 2.5-.01 2.84c0 .27.18.59.69.48A10.04 10.04 0 0 0 22 12.26
                C22 6.58 17.52 2 12 2Z"
              />
            </svg>
          </a>
        </div>
      </header>
  
      <main class="page">
        <RouterView />
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
  import { computed } from "vue";
  import { useRoute, useRouter } from "vue-router";
  import { NAV_ITEMS } from "@/config/nav";
  import BackToTop from "@/components/BackToTop.vue";
  
  const route = useRoute();
  const router = useRouter();
  
  const githubUrl = "https://github.com/"; // 你后面换成课题组仓库
  const goHome = () => router.push("/");

  const activePath = computed(() => {
    const p = route.path;
    if (p.startsWith("/search")) return "/search";
    if (p.startsWith("/data-browse")) return "/data-browse";
    if (p.startsWith("/analysis")) return "/analysis";
    if (p.startsWith("/download")) return "/download";
    if (p.startsWith("/stats")) return "/stats";
    if (p.startsWith("/contact")) return "/contact";
    if (p.startsWith("/help")) return "/help";
    // 其他同理
    return p;
  });
  </script>
  
  <style scoped>
  .app { min-height: 100vh; display: flex; flex-direction: column; }

.topbar{
  position: sticky;
  background: var(--nav-bg);
  color: var(--nav-text);
  top: 0;
  z-index: 1000;
  height: 72px;
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid var(--nav-border);
  box-shadow: 0 6px 18px var(--border); /* 很轻的“抬起感” */
}
.topbar-inner {
  height: 100%;
  display: flex;
  align-items: stretch;
}

.brand {
  flex: 0 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
}
.brand-name {
  color: var(--brand-primary);
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 0.4px;
  line-height: 72px;
  text-shadow: 0 1px 8px rgba(166, 186, 177, 0.14);
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
.nav-item {
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

.github {
  flex: 0 0 72px;
  width: 84px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  border-radius: 0;
  color: var(--nav-icon);
}
.github:hover { background: var(--nav-hover-bg); }

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

@media (max-width: 980px){
  .footer-inner{ flex-direction: column; align-items: flex-start; }
  .footer-left, .footer-right{ white-space: normal; }
}
.muted { color: var(--muted); }
</style>
