# Mist SMP client mod

The client companion for the [Mist SMP](https://mistsmp.org) Minecraft server. It renders cosmetics (hats, wings) that the server tells it about. It is required to join Mist SMP.

## For players: what this mod does

- It only does anything on the Mist SMP server. On any other server or in singleplayer it stays silent and sends nothing.
- It receives the list of cosmetics each player has equipped and draws them.
- It does not read your files, collect data, or contact any website. It only talks to the Minecraft server you are connected to, using Minecraft's own networking.
- It contains no secrets. All of the source code is in this repository, so you or anyone you trust can review exactly what it does.

Install instructions: https://mistsmp.org/mod

## For developers

- Fabric, Minecraft 26.3, Java 25.
- `docs/PROTOCOL.md` is the wire contract with the server plugin (`mist-plugin`). Change both together.
- Run a dev client: `gradlew runClient` (or the `Minecraft Client` run config IntelliJ generates on import).
- Build: `gradlew build`. The mod jar is in `build/libs/`.

This repository is public. Never commit tokens, keys, server addresses with credentials, or anything else secret. The mod has no need for any: the server is the only authority.
