# Mist SMP cosmetic asset guide

How to make cosmetics that the Mist SMP client mod can render. Read **General rules** first, then the section for the slot you are making.

| Slot | Shows when | What you make |
|---|---|---|
| [Head](#head) | Something is worn on the head | 3D model |
| [Face](#face) | Always | 3D model (masks, glasses) |
| [Chest](#chest) | A chestplate is worn | 3D model (backpacks) or a flat armor texture (shirts) |
| [Legs](#legs) | Leggings are worn | Flat armor texture |
| [Feet](#feet) | Boots are worn | 3D boots or a flat armor texture |
| [Back](#back) | Wings: an elytra is worn. Capes: always | 3D wing model or a cape texture |
| [Held item](#held-item) | A matching item is in hand | 3D or flat item model |
| [Follower](#follower) | Always | 3D model that floats beside the player |
| [Aura](#aura) | Always | Particles only, no model |

A cosmetic that replaces worn gear (head, chest, legs, feet, wings) hides that piece while it shows. The real item and its protection are unchanged; only the look is different.

---

## General rules

### The cosmetic key

Every cosmetic has a **key**: a short name used for its file names and its database entry.

- Lowercase letters, numbers, and underscores only. Example: `witch_hat`, `storm_cape`.
- Unique across **all** slots. A head item and a back item can't both be called `storm`.
- It can't change once the cosmetic is in the game.

In the paths below, `<key>` means your cosmetic key and `<slot>` means the slot's folder name: `head`, `face`, `chest`, `legs`, `feet`, `back`, `held`, `follower`, or `aura`.

### Files

- Everything goes under `assets/mistsmp/`. The paths in this guide start there.
- Textures are PNG, with width and height each a power of two (16, 32, 64, 128). Transparent pixels are fine.
- Model textures **must** be under `textures/item/`. Textures in other folders show up as pink and black.
- Don't use textures from another resource pack or artist that you don't own.
    - Violation of this may result in legal action against you.

### Model files

Every 3D cosmetic is three files:

```
items/cosmetic/<slot>/<key>.json
models/cosmetic/<slot>/<key>.json
textures/item/cosmetic/<slot>/<key>.png
```

The `items` file is always this, with `<slot>` and `<key>` filled in:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "mistsmp:cosmetic/<slot>/<key>"
  }
}
```

Building the model in Blockbench:

1. Use the **Java Block/Item** project type. Fix any warnings Blockbench shows on export, because it is checking the limits Minecraft enforces.
2. Point every texture at `mistsmp:item/cosmetic/<slot>/<key>`. If the model has more than one texture, give each its own file in the same folder.
3. Set a `particle` texture. It can be the same texture.
4. Make sure no face uses `#missing`.
5. Free rotations are fine. Cubes can be rotated on any axis by any angle.
6. Keep it light. Cosmetics are drawn for every player in view, so aim for a few dozen cubes, not hundreds.
7. Export the model JSON and the PNG.

Some slots are placed by the Blockbench **Display** tab and some are built on a **reference model**. Each slot section says which. Reference models are in [`docs/reference/`](reference/). Open one, build your cosmetic around the gray cubes, and **delete every cube named REFERENCE before exporting**.

Coordinates in this guide are Blockbench pixels: 16 is one block. The player faces **south** (+Z) in every reference model.

### Animated textures

A texture can animate the vanilla way, with a `.png.mcmeta` file next to the PNG. This works in every slot and needs nothing else.

### The definition file (optional)

Movement, flipbook animation, particles, and some slot settings come from an optional file:

```
cosmetics/<slot>/<key>.json
```

Without it, the cosmetic is a single still model. Every field is optional. These fields work in any slot that has a model:

```json
{
  "frames": ["mistsmp:cosmetic/head/<key>", "mistsmp:cosmetic/head/<key>_2"],
  "frameTicks": 4,
  "animateWhen": "always",
  "motion": {
    "spinDegreesPerSecond": 0,
    "bob": { "heightPixels": 0, "periodTicks": 40 }
  },
  "placement": {
    "offsetPixels": [0, 0, 0],
    "rotationDegrees": [0, 0, 0],
    "scale": 1
  }
}
```

| Field | What it does |
|---|---|
| `frames` | A flipbook. Each entry is another model with its own `items` file. Leave it out for a single model. |
| `frameTicks` | How long each frame shows. 20 ticks is one second. Default 4. |
| `animateWhen` | When the flipbook plays: `always`, `moving`, `sprinting`, `flying` (elytra), or `idle`. Otherwise the first frame shows. |
| `motion` | Continuous spin around the vertical axis, and an up-and-down bob. |
| `placement` | Nudges the model after it is placed: offset in pixels, rotation in degrees, and size. Useful for fine-tuning without re-exporting. |

The sections below list each slot's extra fields. Unknown fields are ignored, and a file with a mistake is skipped (the cosmetic shows as a still model) instead of breaking anything.

### What to hand in

1. The files for your slot, in the folder layout shown (a zip of the `assets` folder is easiest).
2. A **display name** (what players see, e.g. "Witch Hat").
3. An optional one-line **description**.
4. The **artist credit** name you want shown.
5. A suggested **tier**: 1, 2, or 3 (simple, detailed, showpiece).
    - See the cosmetic page on the website or ask staff for details on the tier system.
6. For held items, which items it should apply to.
7. For auras, a description of the effect if you aren't writing the definition file yourself.

---

## Head

A 3D model worn on the head. It shows while the player wears anything in the helmet slot (any helmet, a pumpkin, a skull) and replaces how that item looks.

**Files:** the three [model files](#model-files) in the `head` folder.

**Placement:** the Blockbench **Display** tab. Choose the **Head** slot; Blockbench shows a player head. Position, rotate, and scale the model until it sits right. **The game uses exactly this placement.** If it looks right in this preview, it looks right in game.

### Checklist

- [ ] Head display slot set and previewed
- [ ] Texture paths are `mistsmp:item/cosmetic/head/...`
- [ ] No `#missing` faces
- [ ] Three files in the right folders

---

## Face

Masks, glasses, beards: anything on the face. It always shows while equipped, and it shows **together with** a helmet or a head cosmetic.

**Files:** the three [model files](#model-files) in the `face` folder.

**Placement:** the Blockbench **Display** tab, **Head** slot, the same as hats. In that view the player's face is the model's **north** side (the way a carved pumpkin's face points). Keep it close to the face so it doesn't poke through hats.

---

## Chest

Two styles. Use one.

### 3D model (backpacks, shoulder pieces)

**Files:** the three [model files](#model-files) in the `chest` folder.

**Placement:** built on [`back_reference.json`](reference/back_reference.json). The model is attached to the player's body and follows crouching, turning, and walking. The Display tab is ignored.

- The body is `[4, 0, 8]` to `[12, 12, 12]`. Its back face is at z=8 and its front at z=12.
- The head is `[4, 12, 6]` to `[12, 20, 14]`. The arms are `[0, 0, 8]` to `[4, 12, 12]` and `[12, 0, 8]` to `[16, 12, 12]`.
- The player faces south (+Z), so backpacks go on the north side (z below 8).
- Keep surfaces a little away from z=7.75 (back) and z=12.25 (front). That's where skins draw their outer jacket layer, and a face right on it flickers.

### Flat armor texture (shirts, tunics)

A texture painted onto the vanilla chestplate shape. No 3D modeling.

**Files:**
```
equipment/cosmetic/chest/<key>.json
textures/entity/equipment/humanoid/cosmetic/chest/<key>.png
```

```json
{
  "layers": {
    "humanoid": [
      { "texture": "mistsmp:cosmetic/chest/<key>" }
    ]
  }
}
```

The PNG is **64x32**, in the vanilla armor layout. Start from a copy of a vanilla armor texture (iron is a good one) so the regions line up, and paint only the body and arm regions. The rest can be transparent.

If a chest cosmetic has both a model and a texture, the model wins.

---

## Legs

A flat texture painted onto the vanilla leggings shape.

**Files:**
```
equipment/cosmetic/legs/<key>.json
textures/entity/equipment/humanoid_leggings/cosmetic/legs/<key>.png
```

```json
{
  "layers": {
    "humanoid_leggings": [
      { "texture": "mistsmp:cosmetic/legs/<key>" }
    ]
  }
}
```

The PNG is **64x32**, in the vanilla **leggings** layout. Start from a copy of a vanilla leggings texture.

---

## Feet

Two styles. Use one.

### 3D boots

**Files:** two models, the right boot and the left boot:
```
items/cosmetic/feet/<key>.json
items/cosmetic/feet/<key>_left.json
models/cosmetic/feet/<key>.json
models/cosmetic/feet/<key>_left.json
textures/item/cosmetic/feet/<key>.png
```

**Placement:** built on [`feet_reference.json`](reference/feet_reference.json). The boots are attached to the legs and follow walking. The Display tab is ignored.

1. Build the **right** boot. The leg is `[6, 0, 6]` to `[10, 12, 10]`, the sole is at y=0, the toe points south (+Z), and the outside of the leg is the -X side.
2. Make the left boot by mirroring a copy on X in Blockbench, and save it as `<key>_left`. Both models can share one texture.

A mirrored copy is needed because the game can't flip a model without turning it inside out.

### Flat armor texture

Same as the [chest texture](#flat-armor-texture-shirts-tunics), with `chest` replaced by `feet` everywhere. It uses the `humanoid` sheet; paint only the boot regions.

If a feet cosmetic has both a model and a texture, the model wins.

---

## Back

Two styles, picked in the definition file. Wings are the default.

### Wings

Wings show while the player wears an elytra and replace it. They fold when standing and spread when gliding, with the same motion as the vanilla elytra.

**Files:** two models, the left wing and the right wing:
```
items/cosmetic/back/<key>.json
items/cosmetic/back/<key>_right.json
models/cosmetic/back/<key>.json
models/cosmetic/back/<key>_right.json
textures/item/cosmetic/back/<key>.png
```

**Placement:** built on [`wing_reference.json`](reference/wing_reference.json). The Display tab is ignored.

1. Build the **left** wing (the player's left, seen from behind, which is the +X side).
2. The **hinge**, where the wing meets the back, goes exactly at the center of the grid, `(8, 8, 8)`. The wing turns around this point.
3. Model the wing **folded**, hanging down the back the way an elytra hangs when standing. The mod does the spreading.
4. For scale, the player's body is 8 wide, 12 tall, and 4 deep. A vanilla elytra wing is about 10 wide and 20 tall.
5. Make the right wing by mirroring a copy on X, and save it as `<key>_right`.

Flipbook frames for wings are variations of the folded wing, and each frame needs its own `_right` copy too.

### Cape

A cape always shows while equipped and moves like a vanilla cape. With an elytra on, the elytra uses the cape's texture, the same as vanilla. It is hidden while a chest backpack shows, since the two would overlap.

**Files:**
```
textures/entity/cosmetic/back/<key>.png
cosmetics/back/<key>.json
```

The PNG is **64x32** in the vanilla cape layout. The definition file is required, because it is what makes this a cape:

```json
{
  "back": { "style": "cape" }
}
```

---

## Held item

Replaces how an item looks **in the hand**, in first and third person. Inventories, dropped items, and item frames still show the real item, so players always know what they are holding. Enchantment glint still shows.

**Files:** the three [model files](#model-files) in the `held` folder, plus a definition file listing the items it applies to:

```
cosmetics/held/<key>.json
```

```json
{
  "held": {
    "appliesTo": ["minecraft:diamond_sword", "#minecraft:swords"]
  }
}
```

Entries are item ids, or item tags starting with `#` (`#minecraft:swords` is every sword). Without this file the cosmetic applies to nothing.

**Placement:** the Blockbench **Display** tab. Set the hand slots: third person and first person, right and left hand.

---

## Follower

A small model that floats behind the player, catches up smoothly as they move, and can circle them when they stand still. One per player.

**Files:** the three [model files](#model-files) in the `follower` folder, plus an optional definition file.

**Placement:** built on [`follower_reference.json`](reference/follower_reference.json). Center the model on `(8, 8, 8)`, facing south (+Z). A good size is 8 to 16 pixels across (a player head is 8). The Display tab is ignored.

Optional settings, in `cosmetics/follower/<key>.json` (distances in blocks):

```json
{
  "scale": 0.6,
  "follower": {
    "distance": 0.9,
    "height": 1.6,
    "side": 0.6,
    "smoothing": 0.15,
    "orbitWhenIdle": { "afterTicks": 60, "radius": 0.9, "degreesPerSecond": 40 }
  },
  "particles": []
}
```

| Field | What it does |
|---|---|
| `scale` | Size of the model. |
| `distance` | How far behind the player it floats. |
| `height` | How high above the player's feet. |
| `side` | How far to the player's right (negative is left). |
| `smoothing` | How quickly it catches up, from 0.01 (very floaty) to 1 (instant). |
| `orbitWhenIdle` | After the player stands still this many ticks, it circles them. Leave it out for no orbit. |
| `particles` | Particles around the follower. Same format as [auras](#particles). |

The general `motion` field (spin and bob) works here too, and suits floating followers well.

---

## Aura

Particles around the player. No model and no texture: the whole cosmetic is its definition file.

**Files:**
```
cosmetics/aura/<key>.json
```

```json
{
  "particles": [
    {
      "type": "minecraft:dust",
      "color": "#7A3CFF",
      "size": 0.7,
      "perSecond": 5,
      "spread": 0.3,
      "area": "body",
      "velocity": [0, 0.02, 0],
      "when": "always"
    }
  ]
}
```

### Particles

Each entry in `particles` is one effect. An aura can have several.

| Field | What it does |
|---|---|
| `type` | Any vanilla particle that needs no extra settings (`minecraft:end_rod`, `minecraft:cherry_leaves`, and so on), plus `minecraft:dust` and `minecraft:entity_effect`, which take a color. |
| `color` | Hex color, for dust and entity_effect. `"$tint"` uses a color the server sends instead (supporter auras use this to match the supporter's title color). |
| `size` | Dust only, 0.01 to 4. |
| `perSecond` | How many spawn each second, up to 40. Fractions work: 0.5 is one every two seconds. |
| `spread` | How far from the center a particle can appear, in blocks. |
| `area` | Auras only: `body` (around the whole player), `feet` (a ring at the feet), or `head` (a halo above). |
| `velocity` | Drift in blocks per tick, `[x, y, z]`. Some particles add their own drift on top. |
| `when` | `always`, `moving`, `sprinting`, `flying`, or `idle`. |

Particles are drawn by each player's own game, so they cost the server nothing. Players' particle settings still apply, and anyone can hide other players' particles with a key (K by default). Keep `perSecond` modest; an aura is on screen for everyone nearby.

If you aren't comfortable writing this file, describe the effect (particle, color, how dense, and when) and staff will write it.