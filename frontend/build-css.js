const fs = require("node:fs");
const path = require("node:path");
const { compile } = require("tailwindcss");

const root = path.resolve(__dirname, "..");
const templates = path.join(root, "src/main/resources/templates");
const output = path.join(root, "src/main/resources/static/css/app.css");

function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const filename = path.join(directory, entry.name);
    return entry.isDirectory() ? walk(filename) : [filename];
  });
}

async function main() {
  const html = walk(templates).filter((name) => name.endsWith(".html"));
  const candidates = new Set();
  for (const filename of html) {
    const source = fs.readFileSync(filename, "utf8");
    for (const match of source.matchAll(/class="([^"]+)"/g)) {
      match[1]
        .split(/\s+/)
        .filter(Boolean)
        .forEach((candidate) => candidates.add(candidate));
    }
  }
  const framework = fs.readFileSync(require.resolve("tailwindcss/index.css"), "utf8");
  const generated = await compile(framework);
  const layout = fs.readFileSync(path.join(__dirname, "layout.css"), "utf8");
  fs.writeFileSync(output, generated.build([...candidates]) + "\n" + layout + "\n");
  process.stdout.write(`Built ${path.relative(root, output)}\n`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
