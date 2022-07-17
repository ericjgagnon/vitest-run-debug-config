/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_LINE_ENDING: string;
}

export default interface ImportMeta {
  readonly env: ImportMetaEnv;
}
