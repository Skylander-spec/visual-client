# Shared Visual cosmetics

This service distributes Visual Client cosmetics independently of the Minecraft server.
It does not host worlds and it has not been deployed to a public address.

Run locally with Node 22 or later:

```powershell
cd cosmetics-server
npm test
npm start
```

The default address is `http://127.0.0.1:8787`. Set `VISUAL_COSMETICS_URL` in the
Minecraft process environment (or Java property `visual.cosmetics.url`) to the
service address. With no address configured, cosmetics remain local and the UI
says so. The launcher does not need code changes. All participating clients
must use the same service address.

For public operation, deploy the Dockerfile with a persistent volume at `/data`
behind HTTPS. Set `HOST=0.0.0.0` when running without Docker behind a reverse
proxy. Do not expose an unencrypted service on the public internet. The client
accepts HTTP only on localhost. Configure the proxy with a 750 KB request limit
and forward rate limits at the edge. Node's built-in limit is per socket IP;
behind a reverse proxy it is shared by all users of that proxy.

## Identity and data

The client proves Minecraft account ownership with Mojang's session service.
Its Minecraft access token goes only to `sessionserver.mojang.com`; this server
never receives it. Challenges expire in 60 seconds, cannot be reused, and are
bound to the UUID. The server issues its own 24-hour bearer token. Offline
accounts can use local cosmetics but cannot publish an unverified identity.

Published data contains only cosmetic selections and optional cape/mini-skin
PNGs (256 KB each maximum). Appearance records are publicly readable by UUID,
so clients on the same Minecraft server can render each other. No chat,
server addresses, world positions, passwords or game access tokens are stored.
The current implementation polls up to 32 visible world players every five
seconds; it is an initial service for a small community, not a high-scale CDN.

## Verification

`npm test` checks validation, unauthenticated writes, challenge replay, identity
spoofing, and retrieval by a second HTTP client. Tests inject the identity
verifier; live Mojang authentication and a two-account Minecraft session still
require an online deployment and real accounts.
