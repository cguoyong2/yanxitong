# Nginx

`nginx.production.example.conf` is the production-style example for:

- `/api` reverse proxy to the backend
- `/ws` WebSocket reverse proxy to the backend
- admin static assets at `/`
- confirm-screen static assets at `/confirm-screen/`
- coarse public-entry rate limits before backend Redis-backed limits

`proxy_headers.inc` contains the shared reverse proxy headers.

See `docs/production-deployment.md` for the full minimum deployment loop.
