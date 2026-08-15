import { shallowReactive } from "vue";

interface RegulatoryAnnotationDownloadTask {
  label: string;
  startedAt: number;
  promise: Promise<void>;
}

const activeTasks = shallowReactive(new Map<string, RegulatoryAnnotationDownloadTask>());

function taskKey(datasetId: string): string {
  return datasetId.trim().toUpperCase();
}

export function getRegulatoryAnnotationDownloadTask(
  datasetId: string,
): RegulatoryAnnotationDownloadTask | undefined {
  return activeTasks.get(taskKey(datasetId));
}

export function runRegulatoryAnnotationDownload(
  datasetId: string,
  label: string,
  download: () => Promise<void>,
): Promise<void> {
  const key = taskKey(datasetId);
  const existing = activeTasks.get(key);
  if (existing) return existing.promise;

  let promise: Promise<void>;
  promise = Promise.resolve()
    .then(download)
    .finally(() => {
      if (activeTasks.get(key)?.promise === promise) activeTasks.delete(key);
    });

  activeTasks.set(key, {
    label,
    startedAt: Date.now(),
    promise,
  });
  return promise;
}
