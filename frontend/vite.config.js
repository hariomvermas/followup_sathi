// Vite dev config for FollowUp Saathi.
//
// The app is vanilla HTML/CSS/JS with no bundler: every page is a real .html
// file and scripts are plain <script> tags using global namespaces (FSApi,
// FSUi, FSAuth, FSNav). Vite is used only as a dev server + proxy so
// `npm run dev` works; it must NOT rewrite or bundle that structure.
//
// - root stays frontend/ so /dashboard.html, /enquiries.html resolve as-is
// - build.outdir dist/ mirrors the Maven copy that serves target/classes/static
// - proxy forwards /api to the Spring Boot backend on :8080 (session cookie +
//   CSRF header flow unchanged); ws:false is enough, REST only
export default {
  root: __dirname,
  appType: "mpa",
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        ws: false,
      },
    },
  },
  build: {
    outDir: "dist",
    rollupOptions: {
      input: {
        index: "/index.html",
        login: "/login.html",
        register: "/register.html",
        onboarding: "/onboarding.html",
        dashboard: "/dashboard.html",
        enquiries: "/enquiries.html",
        enquiryDetails: "/enquiry-details.html",
        followUps: "/follow-ups.html",
        templates: "/templates.html",
        reports: "/reports.html",
        settings: "/settings.html",
      },
    },
  },
};
