import path from 'path';
import * as url from 'url';
import { defineConfig } from 'vite';

const dirname = url.fileURLToPath(new url.URL('.', import.meta.url));

export default defineConfig({
  build: {
    emptyOutDir: false,
    lib: {
      entry: path.resolve(dirname, 'src/reporter.ts'),
      name: 'Vitest Intellij Plugin',
      formats: ['es'],
      fileName: () => {
        // eslint-disable-next-line no-undef
        const os = process.argv[4];
        return `reporter.${os}.js`;
      },
    },
  },
  test: {
    globals: true,
  },
});
