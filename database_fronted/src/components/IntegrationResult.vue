<template>
  <OnlineBedtoolsSection :dataset-id="datasetId" :domain="domain" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { SearchResultDomain } from "@/api/searchResult";
import OnlineBedtoolsSection from "@/components/search-result/OnlineBedtoolsSection.vue";

type IntegrationPayload = {
  domain?: SearchResultDomain | string;
  primaryId?: string;
};

const props = defineProps<{ payload: IntegrationPayload }>();

const datasetId = computed(() => props.payload?.primaryId || "H_000001");
const domain = computed<SearchResultDomain>(() => {
  return props.payload?.domain === "integration" ? "integration" : "integration";
});
</script>
