# Mist SMP plugin / mod protocol

This is the contract between `mist-plugin` (Paper, server) and `mist-mod` (Fabric, client).
Both sides implement it by hand: Paper hands plugins raw `byte[]`, Fabric uses typed codecs.
Change this file and both implementations together.

Current protocol version: **1**

## Principles

- The server is the only source of truth. The client renders what it is told and never decides what a player owns.
- The mod is inert unless the server registers the `mistsmp:hello` channel. On any other server it sends nothing.
- No secrets, tokens or credentials ever cross this channel or live in the mod. This repo is public.
- The protocol version changes only when the wire format changes in a way older clients cannot read. Rendering or asset changes do not bump it.

## Channel registration

Plugin messaging only flows on channels both sides have registered.

- The plugin registers `mistsmp:hello` as an incoming channel. Paper advertises it to the client with `minecraft:register`.
- The mod registers `mistsmp:welcome` and `mistsmp:loadouts` as receivable. Fabric advertises them with `minecraft:register`. Paper will not deliver a plugin message to a client that has not registered the channel, so the plugin should wait for the hello before sending anything.

## Handshake

```
client                                  server (plugin)
  |   <-- minecraft:register [mistsmp:hello]
  |-- mistsmp:hello ------------------->|   protocol check
  |<-- mistsmp:welcome -----------------|   accepted
  |<-- mistsmp:loadouts (replaceAll) ---|   full snapshot
  |<-- mistsmp:loadouts (delta) --------|   whenever any loadout changes
```

- If the hello reports a protocol version the server does not support, the server disconnects the client with a message pointing to the update instructions.
- If no hello arrives within the server's timeout, the server disconnects the client with a message pointing to the install instructions (the mod is mandatory). A staff bypass permission may skip this check.

## Primitive types

All multi-byte integers are big-endian (Netty default).

| Type | Encoding |
|---|---|
| Boolean | 1 byte, `0x00` false, `0x01` true |
| Long | 8 bytes, signed, big-endian |
| VarInt | Minecraft VarInt: 7 data bits per byte, low bits first, high bit set means another byte follows. Max 5 bytes. |
| String | VarInt byte length, then that many UTF-8 bytes |
| UUID | Long most-significant bits, then Long least-significant bits |

## Messages

### `mistsmp:hello` (client to server)

| Field | Type | Notes |
|---|---|---|
| protocolVersion | VarInt | `1` |
| modVersion | String | e.g. `0.1.0`, for logging and support only |

### `mistsmp:welcome` (server to client)

| Field | Type | Notes |
|---|---|---|
| serverProtocolVersion | VarInt | The protocol version the server speaks |

### `mistsmp:loadouts` (server to client)

| Field | Type | Notes |
|---|---|---|
| replaceAll | Boolean | `true`: full snapshot, client discards everything first. `false`: delta. |
| entryCount | VarInt | Max 1024 |
| entries | entryCount x Entry | |

Entry:

| Field | Type | Notes |
|---|---|---|
| player | UUID | |
| cosmeticCount | VarInt | Max 16. `0` clears this player's loadout. |
| cosmetics | cosmeticCount x Cosmetic | |

Cosmetic:

| Field | Type | Notes |
|---|---|---|
| slot | VarInt | Slot wire id, see below |
| cosmeticId | String | Namespaced id, e.g. `mistsmp:cosmetic/head/top_hat`. Max 256 chars. Matches the `CosmeticSkin` key in the database and the model path in the server resource pack. |

In a delta, each listed player's entry fully replaces that player's previous loadout.

Clients skip unknown slot ids and unparseable cosmetic ids instead of failing, so the server can add slots without breaking older clients in the same protocol version.

## Slots

| Wire id | Slot | Renders when |
|---|---|---|
| 0 | HEAD | The player is wearing something in the head slot. The worn item is hidden and the cosmetic is drawn instead. Nothing on the head means no cosmetic. |
| 1 | WINGS | The player is wearing an elytra. The elytra is drawn with the cosmetic instead. |

Wire ids are permanent. Never reuse a retired id.

## Assets

Cosmetic models and textures ship in the server resource pack, not in the mod. The mod looks models up by `cosmeticId`. If the pack has no model for an id, the client renders vanilla for that slot.
