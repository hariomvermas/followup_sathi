// Vite emits only what it bundles (HTML + module assets). This app's scripts are
// plain non-module <script src> tags and its CSS is plain <link> tags, so Vite
// leaves frontend/js and frontend/css out of dist/. This copy completes dist/
// so `npm run build` output is directly servable, mirroring what the Maven
// copy-frontend execution puts into target/classes/static.
import { cp } from "node:fs/promises";

await cp(new URL("../js", import.meta.url), new URL("../dist/js", import.meta.url), {
  recursive: true,
});
await cp(new URL("../css", import.meta.url), new URL("../dist/css", import.meta.url), {
  recursive: true,
});
console.log("copied js/ and css/ into dist/");
