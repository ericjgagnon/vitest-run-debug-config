import esbuild from 'esbuild';

esbuild.buildSync({
  entryPoints: ['src/reporter.ts'],
  bundle: true,
  platform: 'node',
  format: 'esm',
  external: ['./node_modules/*'],
  outfile: 'dist/reporter.js',
  tsconfig: 'tsconfig.json',
});
