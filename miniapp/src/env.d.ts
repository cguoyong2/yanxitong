/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare const wx: {
  chooseLocation?: (options: {
    success?: (result: UniApp.ChooseLocationSuccess) => void;
    fail?: (error: unknown) => void;
  }) => void;
};
