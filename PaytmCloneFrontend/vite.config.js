import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from "url";
import path from "path"
import { VitePWA } from 'vite-plugin-pwa';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(),

    VitePWA({
      registerType: "autoUpdate", // shows that the new update is available so install that. 
      manifest: {
        name: "SwiftPay",
        short_name: "SwiftPay",
        description: "SwiftPay Digital Wallet",
        start_url: "/",
        scope: "/",
        display: "standalone",
        background_color: "#ffffff",
        "theme_color": "#2563eb",
        icons: [
          {
            src: "/icons/icon-192.png",
            sizes: "192x192",
            type: "image/png"
          },
          {
            src: "/icons/icon-512.png",
            sizes: "512x512",
            type: "image/png"
          },
          {
            src: "/icons/maskable-512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "maskable"
          }
        ]
      },

      // this is by the vite-pwa plugin, which internally uses Google workbox to generate a service worker internally.
      
      /**
       * THis is the part where is defines how the caching and all should be there, and handles navigation requests.
       * 
       * Service worker intercepts every web request.
       * 
       * 0 stands for an opaque response happens when the browser fetches a cross-origin resource without CORS permission.
       * So when you are taking something from the CDNs or some external apis, they refuse the CORS and don't return
       * response body, status code and headers, but they still return the file that you can use, in that case it is marked as
       * opaque response and the status code is 0 for that case, like google fonts apis, CDNs, External APIs, External scripts.
       * 
       * So its better to cache them for faster performance.
       */

      workbox: {
        navigateFallback: "/index.html", // if any navigation fails(offline), it will server this page.

        /**
         * This defines how the dynamic requests should be handled at the runtime. Not during the build, instead when users visit the site. 
         */
        runtimeCaching: [
          {
            // urlPattern defines the cache rule.
            urlPattern: ({ request }) => request.destination === 'document',
            handler: "NetworkFirst", // first network, if offline, then cached data, as its a financial app so networkfirst is a must.
            options: {
              cacheName: "pages-cache" // name of the cache storage bucket.
            }
          },
          {
            urlPattern: ({ request }) =>
              request.destination === "style" ||
              request.destination === "script",
              // request.destination === "worker",  don't have a workder file for now that's handling some expensive operations like image processing and all, so commenting it.
            handler: "StaleWhileRevalidate",
            options: {
              cacheName: "assets-cache",
              cacheableResponse: {
                /**
                 * prevents bad responses like 404 or 500.
                 */
                statuses: [0, 200]
              },
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 30 * 24 * 60 * 60
              }
            }
          },
          {
            urlPattern: ({ request }) => request.destination === "image",
            handler: "CacheFirst",
            options: {
              cacheName: "images-cache",
              cacheableResponse: {
                statuses: [0, 200]
              },
              expiration: {
                maxEntries: 120,
                maxAgeSeconds: 30 * 24 * 60 * 60
              }
            }
          }
        ]
      },

      devOptions: {
        enabled: false
      }
    })

  ],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
})



//1. User visits website
//2. Browser reads manifest       
//3. Service worker installs
//4. Files cached
//5. App becomes installable
//6.  Works offline
