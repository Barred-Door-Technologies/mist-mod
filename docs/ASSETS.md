# Mist SMP cosmetic asset guide

How to make cosmetics that the Mist SMP client mod can render. Read **General rules** first, then the section for the slot you are making.

| Slot | Shows when | What you make |
|---|---|---|
| [Head](#head) | Something is worn on the head | 3D model |
| [Face](#face) | Always | 3D model (masks, glasses) |
| [Chest](#chest) | A chestplate is worn | 3D model (backpacks) or a flat armor texture (shirts) |
| [Hands](#hands) | Always | 3D gloves on both arms |
| [Legs](#legs) | Leggings are worn | Flat armor texture |
| [Feet](#feet) | Boots are worn | 3D boots or a flat armor texture |
| [Back](#back) | Wings: an elytra is worn. Capes: always | 3D wing model or a cape texture |
| [Held item](#held-item) | A matching item is in hand | 3D or flat item model |
| [Follower](#follower) | Always | 3D model that floats beside the player or walks after them |
| [Aura](#aura) | Always | Particles only, no model |

A cosmetic that replaces worn gear (head, chest, legs, feet, wings) hides that piece while it shows. The real item and its protection are unchanged; only the look is different.

---

## General rules

### The cosmetic key

Every cosmetic has a **key**: a short name used for its file names and its database entry.

- Lowercase letters, numbers, and underscores only. Example: `witch_hat`, `storm_cape`.
- Unique across **all** slots. A head item and a back item can't both be called `storm`.
- It can't change once the cosmetic is in the game.

In the paths below, `<key>` means your cosmetic key and `<slot>` means the slot's folder name: `head`, `face`, `chest`, `hands`, `legs`, `feet`, `back`, `held`, `follower`, or `aura`.

### Files

- Everything goes under `assets/mistsmp/`. The paths in this guide start there.
- Textures are PNG, with width and height each a power of two (16, 32, 64, 128). Transparent pixels are fine.
- Model textures **must** be under `textures/item/`. Textures in other folders show up as pink and black in game.
- Within that, keep them in `textures/item/cosmetic/<slot>/`. The website preview only reads the cosmetic folders, so a texture anywhere else won't show there.
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

| Field | What it does                                                                                                                                                                                                                                                                |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `frames` | A flipbook. Each entry is another model with its own `items` file. Leave it out for a single model.                                                                                                                                                                         |
| `frameTicks` | How long each frame shows. 20 ticks is one second. Default 4.                                                                                                                                                                                                               |
| `animateWhen` | When the flipbook plays: `always`, `moving`, `sprinting`, `flying` (elytra), `idle`, or `walking` (a walking follower is moving; followers only). Otherwise the first frame shows. || `motion` | Continuous spin around the vertical axis, and an up-and-down bob.                                                                                                                                                                                                           |
| `placement` | Nudges the model after it is placed: offset in pixels, rotation in degrees, and size. Useful for fine-tuning without re-exporting.                                                                                                                                          |
| `particles` | Particles that come with the cosmetic, in any slot. Same format as [auras](#particles). They show only while the cosmetic does: a hat's while a helmet is worn, wings' while an elytra is worn, and so on. Needs mod 0.9.4 or later for slots other than aura and follower. |
The sections below list each slot's extra fields. Unknown fields are ignored, and a file with a mistake is skipped (the cosmetic shows as a still model) instead of breaking anything.

### The website preview

The dashboard and the store show cosmetics on a 3D player, read straight from these same files. If a cosmetic looks right in Blockbench's preview and in game, it looks right on the website too, with nothing extra to make. A few things to know:

- Followers and auras aren't shown in the website preview yet.
- Flat items (models with the `minecraft:item/generated` or `minecraft:item/handheld` parent) show as a flat sprite on the website, without the game's thin 3D edge.
- The only vanilla parents the website understands are `minecraft:item/generated`, `minecraft:item/handheld`, and `minecraft:block/block`. A model built in Blockbench as Java Block/Item doesn't need a parent at all.
- The `items` file must be the simple form shown above. Other item model types (select, condition, and so on) won't show on the website.

### What to hand in

1. The files for your slot, in the folder layout shown (a zip of the `assets` folder is easiest).
2. A **display name** (what players see, e.g. "Witch Hat").
3. An optional one-line **description**.
4. The **artist credit** name you want shown.
5. A suggested **tier**: 1, 2, or 3 (simple, detailed, showpiece).
    - See the cosmetic page on the website or ask staff for details on the tier system.
6. For held items, which items it should apply to.
7. For auras, a description of the effect if you aren't writing the definition file yourself.
8. Optional: a picture for the store card and the dashboard. Followers and auras need one most, since the website's 3D preview doesn't show them.

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

## Hands

Gloves on both arms. They always show (there's no vanilla glove item), follow arm swings and attacks, and also show on your own arms in first person.

**Files:** two models, the right hand and the left hand:
```
items/cosmetic/hands/<key>.json
items/cosmetic/hands/<key>_left.json
models/cosmetic/hands/<key>.json
models/cosmetic/hands/<key>_left.json
textures/item/cosmetic/hands/<key>.png
```

**Placement:** built on [`feet_reference.json`](reference/feet_reference.json): the arm is the same size as a leg. Build the **right** hand: the arm is `[6,0,6]-[10,12,10]` with the hand at the bottom (y = 0) and the shoulder at the top, the front of the hand toward +Z, and -X is the outside. Then mirror it on X in Blockbench and export it as `<key>_left`. The Display tab is ignored.

- Slim (Alex) arms are 3 pixels wide instead of 4. Gloves are moved half a pixel in on slim arms so they stay centered; build for the 4-pixel arm.
- Flipbook frames each need their own `_left` file, like boots.
- Particles in the definition file default to the hand area.
- Needs mod 0.9.4 or later.

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

A small model that goes everywhere with the player. One per player. It either **flies** (floats behind the player's shoulder, catches up smoothly, and can circle them when they stand still) or **walks** (follows on the ground like a tamed wolf or cat).
**Files:** the three [model files](#model-files) in the `follower` folder, plus an optional definition file.

**Placement:** built on [`follower_reference.json`](reference/follower_reference.json), facing south (+Z). A good size is 8 to 16 pixels across (a player head is 8). The Display tab is ignored.

- Flying: center the model on `(8, 8, 8)`.
- Walking: center it on x and z = 8, with its feet on the bottom of the grid (y = 0).
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

### Walking followers

Add `"movement": "walking"` and it follows on the ground instead. It waits while the player is close, walks after them once they're further than `startDistance`, and stops when it's within `stopDistance`. It walks faster the further behind it is, steps up one block, drops down ledges, and stands on water. It doesn't find its way around walls: if one blocks it while the player is out of range, or the player gets further than `teleportDistance`, it appears behind them (while they glide or fly, it waits until they land).

```json
{
  "scale": 1.0,
  "follower": {
    "movement": "walking",
    "startDistance": 4,
    "stopDistance": 2,
    "speed": 0.2,
    "teleportDistance": 12,
    "wanderWhenIdle": { "afterTicks": 100, "radius": 2.5 }
  },
  "frames": ["mistsmp:cosmetic/follower/<key>", "mistsmp:cosmetic/follower/<key>_walk1"],
  "frameTicks": 2,
  "animateWhen": "walking"
}
```

| Field | What it does |
|---|---|
| `movement` | `flying` (the default) or `walking`. |
| `startDistance` | How far away the player gets before it starts walking after them. |
| `stopDistance` | How close it gets before it stops. |
| `speed` | Walking speed in blocks per tick (0.2 is a normal walk). It goes up to three times this to catch up. |
| `teleportDistance` | Further than this and it appears behind the player. `distance` and `side` set where. |
| `wanderWhenIdle` | After the player stands still this many ticks, it strolls to random spots within `radius` blocks of them. Leave it out to have it sit still. |

For a walk cycle, use `"animateWhen": "walking"`: the frames play only while the follower itself is moving, and the first frame (standing) shows when it stops. `orbitWhenIdle`, `height`, and `smoothing` are for flying followers and are ignored when walking. Needs mod 0.9.4 or later; older versions show it flying.
---

## Aura

Particles around the player. No model: the whole cosmetic is its definition file, plus any [custom particle](#custom-particles) textures it uses.

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

Each entry in `particles` is one effect, and a cosmetic can have several. Any slot can have particles, not just auras.

| Field | What it does |
|---|---|
| `type` | Any vanilla particle that needs no extra settings (`minecraft:end_rod`, `minecraft:cherry_leaves`, and so on), plus `minecraft:dust` and `minecraft:entity_effect`, which take a color. |
| `color` | Hex color, for dust and entity_effect. `"$tint"` uses a color the server sends instead (supporter auras use this to match the supporter's title color). |
| `size` | Dust only, 0.01 to 4. |
| `perSecond` | How many spawn each second, up to 40. Fractions work: 0.5 is one every two seconds. |
| `spread` | How far from the center a particle can appear, in blocks. |
| `area` | Where on the player they appear. `body` (around the whole player), `feet` (a ring at the feet), and `head` (a halo above) are centered on the player. `face` (in front of the face), `back` (behind the shoulders), `wings` (across the wing span behind the back), `legs`, and `hand` (the hand holding a held cosmetic's item, otherwise the main hand) sit on the body and turn with it, and tilt with the player while gliding. Leave it out to use the slot's own spot: head for head, face for face, body for chest and aura, legs for legs, feet for feet, wings for wings (back for capes), and hand for held items. Followers always use the follower. |
| `velocity` | Drift in blocks per tick, `[x, y, z]`. Some particles add their own drift on top. |
| `when` | `always`, `moving`, `sprinting`, `flying`, or `idle`. |
| `burst` | How many appear together each time one is due, 1 to 8. |
| `offset` | Moves the spawn point, in blocks, relative to the player's body: `[right, up, forward]`. `[0, 0, -0.3]` is always behind the player, whichever way they face. |
| `shape` | Where in the area they appear:

If you aren't comfortable writing this file, describe the effect (particle, color, how dense, and when) and staff will write it.

### Custom particles

You can draw your own particles. They ship in the resource pack, so new ones never need a mod update.

**Files:**
```
textures/particle/cosmetic/<name>.png
textures/particle/cosmetic/<name>_0.png, <name>_1.png, ...   (animation frames)
```

- PNG, square, with transparency. 8x8 or 16x16 is the usual size; 32x32 at most.
- Draw it in white or light gray if you want to color it with `color` (the color multiplies the texture). Draw it in its real colors and leave `color` out otherwise.
- Use a name that says what it is: `heart`, `flame`, `sparkle_gold`.

In the aura file, use `texture` (or `textures` for frames) instead of `type`:

```json
{
  "particles": [
    {
      "texture": "mistsmp:cosmetic/heart",
      "perSecond": 2,
      "spread": 0.5,
      "size": [0.12, 0.18],
      "lifetime": [30, 40],
      "gravity": -0.002,
      "sway": { "amount": 0.08, "speed": 0.6 },
      "pulse": { "amount": 0.15, "speed": 1.5 },
      "rotation": 0,
      "glow": true,
      "fadeIn": 0.15,
      "fadeOut": 0.4
    }
  ]
}
```

The texture name is the file's path under `textures/particle/` without `.png`: `textures/particle/cosmetic/heart.png` is `mistsmp:cosmetic/heart`.

All the fields in the table above work, plus these. Every one is optional except the texture. Where it says "range", you can write one number (`0.2`) or a range (`[0.1, 0.3]`), and each particle picks its own value from the range, so they don't all look identical.

**Look**

| Field | What it does |
|---|---|
| `texture` | One texture. |
| `textures` | A list of frames, in order. |
| `frames` | How frames are used: `random` (each particle picks one; the default), `life` (play once over the particle's life), or `loop` (cycle). |
| `frameTicks` | For `loop`, ticks per frame. Default 2 (20 ticks is one second). |
| `size` | Size in blocks, range. Default 0.1, at most 1. |
| `sizeEnd` | Size at the end of its life, as a multiple of `size`. 1 is no change (the default), 0 shrinks away, 2 doubles. |
| `color` | Hex color multiplied over the texture, or `"$tint"` for the supporter color. Two colors (`["#FF3355", "#FF99AA"]`) gives each particle a mix of the two. Default white (the texture as drawn). |
| `colorEnd` | Color it fades to over its life. |
| `alpha` | Opacity from 0 to 1, range. Default 1. |
| `fadeIn` | Share of its life spent fading in, 0 to 1. Default 0. |
| `fadeOut` | Share of its life spent fading out, 0 to 1. Default 0.3. |
| `glow` | `true` to ignore lighting, so it's bright in the dark (flames, sparkles). Default `false`. |
| `blend` | `translucent` (soft edges and see-through pixels; the default) or `cutout` (pixels are fully on or off). |
| `rotation` | Starting angle in degrees, range. Leave it out for a random angle; `0` keeps it upright (hearts, flames). |
| `spin` | Turning speed in degrees per tick, range. Negative turns the other way. Default 0. |

**Motion**

| Field | What it does |
|---|---|
| `lifetime` | How long it lasts in ticks, range. Default 20 to 30, at most 200. |
| `velocity` | Starting drift in blocks per tick, `[x, y, z]` (from the table above). |
| `velocityRandom` | Extra random drift, plus or minus, `[x, y, z]`. |
| `gravity` | Pull per tick. Positive falls, negative floats up. Small numbers: 0.002 is gentle. Default 0. |
| `drag` | Share of its speed kept each tick, 0 to 1. Default 0.96; 1 never slows down. |
| `collide` | `true` to stop on blocks instead of passing through. Default `false`. |
| `follow` | `true` to move along with the player. Default `false`: particles stay where they appeared, so they trail behind a moving player. |
| `orbit` | Circles the player (or the follower): `{ "radius": 0.6, "speed": 4, "rise": 0 }`. `speed` is degrees per tick (negative goes the other way), `rise` is blocks per tick up or down. Add `"follow": true` so the circle moves with the player. |
| `sway` | Side to side wobble, like a leaf or a flickering flame: `{ "amount": 0.1, "speed": 1 }`. `amount` in blocks, `speed` in wobbles per second. |
| `pulse` | Size heartbeat: `{ "amount": 0.2, "speed": 1 }`. `amount` is how much bigger and smaller (0.2 is 20 percent), `speed` in beats per second. |
| `maxAlive` | Most of this entry's particles that can exist at once for one player. Default 40, at most 100. |

Custom particles need mod 0.9.2 or later. A texture name that doesn't match a file is skipped (the rest of the aura still shows) and logged in the game's `latest.log`.