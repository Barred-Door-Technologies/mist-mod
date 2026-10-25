# Mist SMP cosmetic asset guide

How to make cosmetics that the Mist SMP client mod can render. Read the **General rules** first, then the section for the slot you are making.

| Slot | What you make |
|---|---|
| Head | 3D model |
| Chest, Legs, Feet | Flat armor textures |
| Wings | 3D model of one wing |
| Held item | 3D or flat item model |

---

## General rules

### The cosmetic key

Every cosmetic has a **key**: a short name that is used for its file names and its database entry.

- Lowercase letters, numbers and underscores only. Example: `witch_hat`, `storm_cape_chest`.
- Unique across **all** slots. A head item and a chest item can't both be called `storm`.

In the paths below, `<key>` means your cosmetic key.

### Files

- All files go under the `mistsmp` namespace: `assets/mistsmp/...`
- Textures are PNG, with width and height each a power of two (16, 32, 64, 128). Transparent pixels are fine.
- Don't use textures from another resource pack you don't own.
  - Violation of this may result in legal action against you.

### What to hand in

1. The files listed for your slot, in the folder layout shown.
2. A **display name** (what players see, e.g. "Witch Hat").
3. An optional one-line **description**.
4. The **artist credit** name you want shown.
5. A suggested **tier**: 1, 2 or 3 (simple, detailed, showpiece).
   - Please see the cosmetic page on the website or ask staff for details on the tier system

## Head

A 3D model worn on the head. It only shows while the player is wearing something in their helmet slot (any helmet, a pumpkin, a skull). The worn item is hidden and your model is shown instead.

### Files

```
assets/mistsmp/items/cosmetic/head/<key>.json
assets/mistsmp/models/cosmetic/head/<key>.json
assets/mistsmp/textures/item/cosmetic/head/<key>.png
```

Textures **must** be under `textures/item/`. Textures in other folders show up as pink and black.

The `items` file is always this, with `<key>` filled in:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "mistsmp:cosmetic/head/<key>"
  }
}
```

### Building it in Blockbench

1. Use the **Java Block/Item** project type. Fix any warnings Blockbench shows on export, because it is checking the limits Minecraft enforces.
2. Point every texture at `mistsmp:item/cosmetic/head/<key>`. If the model has more than one texture, give each its own file in the same folder.
3. Set a `particle` texture. It can be the same texture.
4. Make sure no face uses `#missing`.
5. Go to the **Display** tab and choose the **Head** slot. Blockbench shows a player head there. Position, rotate and scale the model until it sits right on that head. **The game uses exactly this placement.** If it looks right in this preview, it will look right in game.
6. Export the model JSON and the PNG.

### Checklist

- [ ] Head display slot set and previewed
- [ ] Texture paths are `mistsmp:item/cosmetic/head/...`
- [ ] No `#missing` faces
- [ ] Three files in the right folders

---

## Chest, Legs, Feet (armor skins)

Armor skins are **flat textures** painted onto the vanilla armor shape. No 3D modeling. A skin only shows while the player is wearing armor in that slot, and it replaces how that piece looks.

Vanilla armor uses two texture sheets, both **64x32**:

| Sheet | Used by |
|---|---|
| `humanoid` | Chest, Feet (and helmets) |
| `humanoid_leggings` | Legs |

Start from a copy of a vanilla armor texture (for example iron armor), so the regions are in the right place. Only paint the regions your slot uses; the rest can be transparent.

- **Chest:** body and arms regions of the `humanoid` sheet
- **Feet:** boot regions of the `humanoid` sheet
- **Legs:** the `humanoid_leggings` sheet

### Files

Chest:
```
assets/mistsmp/equipment/cosmetic/chest/<key>.json
assets/mistsmp/textures/entity/equipment/humanoid/cosmetic/chest/<key>.png
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

Feet: same as chest, with `chest` replaced by `feet` in all three places.

Legs:
```
assets/mistsmp/equipment/cosmetic/legs/<key>.json
assets/mistsmp/textures/entity/equipment/humanoid_leggings/cosmetic/legs/<key>.png
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

### Checklist

- [ ] 64x32 PNG, started from a vanilla armor layout
- [ ] Only the regions for this slot are painted
- [ ] JSON and PNG in the right folders for the slot

---

## Wings

A custom-shaped pair of wings. They only show while the player is wearing an elytra, which they replace. They fold when standing and spread when gliding, using the same motion as the vanilla elytra.

### What to build

- Build **only the left wing** (the player's left, seen from behind). The mod mirrors it to make the right wing, so both sides always match.
- The **hinge**, where the wing attaches to the player's back, goes exactly at the **center of the Blockbench grid** (8, 8, 8). The wing rotates around this point when it folds and spreads.
- Model the wing **folded**, hanging down the back, the way the vanilla elytra hangs when standing. The mod does the spreading.
- For scale: 16 Blockbench units is one block. The player's body is 8 wide, 12 tall and 4 deep. A vanilla elytra wing is about 10 wide and 20 tall.
- Keep it light. Wings are drawn for every player in view, so a few dozen cubes, not hundreds.

### Files

```
assets/mistsmp/items/cosmetic/wings/<key>.json
assets/mistsmp/models/cosmetic/wings/<key>.json
assets/mistsmp/textures/item/cosmetic/wings/<key>.png
```

The `items` file is the same as for heads, with `head` replaced by `wings`. Display settings in Blockbench don't matter for wings.

---

## Held item

A model that replaces how a held item looks.

### Files

```
assets/mistsmp/items/cosmetic/mainhand/<key>.json
assets/mistsmp/models/cosmetic/mainhand/<key>.json
assets/mistsmp/textures/item/cosmetic/mainhand/<key>.png
```

The `items` file is the same as for heads, with `head` replaced by `mainhand`.

In the Blockbench **Display** tab, set the hand slots (third person and first person, right and left), plus **GUI** and **Ground**. Players see all of these.