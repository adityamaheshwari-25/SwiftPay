import fs from "node:fs";

const apiBaseUrl = process.env.VITE_API_BASE_URL?.trim();
const commitSha = process.env.GITHUB_SHA?.trim();

if (!apiBaseUrl) {
  throw new Error("VITE_API_BASE_URL is required.");
}

const apiUrl = new URL(apiBaseUrl);
if (
  apiUrl.protocol !== "https:" ||
  apiUrl.username ||
  apiUrl.password ||
  apiUrl.search ||
  apiUrl.hash ||
  apiUrl.pathname !== "/api/v1"
) {
  throw new Error("VITE_API_BASE_URL must be an exact HTTPS URL ending in /api/v1.");
}

if (!commitSha || !/^[0-9a-f]{40,64}$/i.test(commitSha)) {
  throw new Error("GITHUB_SHA must be a 40-64 character hexadecimal commit ID.");
}

const configPath = "dist/staticwebapp.config.json";
const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
const csp = config.globalHeaders?.["Content-Security-Policy"];
const broadDirective = "connect-src 'self' https:;";

if (typeof csp !== "string" || !csp.includes(broadDirective)) {
  throw new Error("The expected baseline connect-src directive was not found.");
}

config.globalHeaders["Content-Security-Policy"] = csp.replace(
  broadDirective,
  `connect-src 'self' ${apiUrl.origin};`,
);

fs.writeFileSync(configPath, `${JSON.stringify(config, null, 2)}\n`);
fs.writeFileSync(
  "dist/deployment.json",
  `${JSON.stringify({ sha: commitSha })}\n`,
);

console.log(`Prepared production artifact ${commitSha} for API origin ${apiUrl.origin}.`);
