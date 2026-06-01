import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "@/styles/theme.css";
import "@/styles/global.css";
import "@/styles/search-result-controls.css";

createApp(App).use(router).use(ElementPlus).mount("#app");
