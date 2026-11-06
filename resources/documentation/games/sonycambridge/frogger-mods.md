# Sharing Frogger mods as mod packages
This guide is for people who make Frogger (1997) mods in FrogLord and for players who install them.
It covers exporting a mod package from a modified game, installing a package onto a clean copy of the game,
and what FrogLord does in between.

## What a mod package is
A mod package is a `.mod` file: a zip holding a manifest, `mod.cfg`, and the files the mod changed.
It describes the mod as changes against a named retail build, by name (level codes, image names, model names),
so it holds no content of the original game and can be shared without sharing the game's files.
The format itself is described in [Mod packages](../../froglord/mod-packages.md); this guide does not repeat it.

A package replaces the original game's data in memory. Nothing is written until you save, and saving writes the
usual `modded_` copy of the executable and the MWD next to the originals, as any FrogLord save does.

## Supported builds
| Build | Export | Install |
|---|---|---|
| `pc-retail-v3.0e` | Against the fingerprint shipped with FrogLord, or against an original copy. | Yes, with a check that the open game is unmodified. |
| `pc-retail-v3.0e-vogons` | Against an original copy. | Yes, as an alias of `pc-retail-v3.0e`. There is no fingerprint for it, so the open game is not checked. |
| `psx-retail-usa` | Only against an original copy, because FrogLord ships no fingerprint for it. | Yes, without the unmodified-game check. |

A base fingerprint is a list of hashes of a retail build's content, as FrogLord re-saves it.
It lets FrogLord tell what changed without the original files. It holds hashes only, no game content.
The shipped one is `resources/games/frogger/fingerprints/pc-retail-v3.0e.cfg`.

## Exporting a mod
Open the modified game and choose **File > Export Mod Package...**.

1. **Mod details.** Name, version (`MAJOR.MINOR.PATCH`), author, and optionally a licence, homepage and description.
   Keep the package's `id` the same across releases of the mod.
2. **Base.** What the open game is compared with:
   - the fingerprint FrogLord ships for the build (the default where one exists),
   - an original, unmodified copy of the build that you select (needed for builds without a fingerprint, and the only
     way to package raw executable writes, see below),
   - none, in which case FrogLord cannot tell what changed and exports all content it can represent.
3. **Change list.** Every detected change, grouped by kind (maps, models, images, level select, demos, themes,
   gameplay, patches). Each is ticked; untick a change to leave it out of the package.
4. **Level names.** FrogLord cannot read a level's name back from the pixels of its name image, so the dialog lists the
   levels and asks for each name as text. Unchanged levels are pre-filled with the retail name. The text becomes the
   level's `name=` in the manifest, which ports and other tools use.
5. **Warnings.** Changes the format cannot carry are listed here and in the export log: for example sounds, music,
   entity scripts, the form library, new archive entries or new images. So are **unexplained executable changes**:
   1 KiB blocks of the executable that differ from the base but are not a bundled patch or data FrogLord owns,
   typically from a Noodle script that wrote to the executable directly, or hex editing. By default they are left out.
6. **Raw writes (opt-in).** With an original copy as the base, you can tick the option to package unexplained
   executable changes as `patches/raw-writes.patch`, a generated patch of the exact differing bytes, marked `raw=true`.
   FrogLord applies it on install; ports of the game ignore it. Without an original copy the option has no effect.
7. **Output.** Choose where to write the `.mod`. FrogLord writes the package and an `export.log` beside it listing what
   was exported, what was left out and why.

Before sharing, install the package onto a fresh copy of the game and play it.

## Installing a mod
Open an unmodified copy of a build the package targets and choose **File > Install Mod Package...**.

1. **Load.** Browse for the `.mod` file. The dialog shows the package's name, version, author, targets, description,
   a count of what it changes, and any validation problems. A package with errors cannot be installed.
2. **Target check.** The open build must be one of the package's targets or an alias of one.
   Otherwise the dialog says which builds the package targets and refuses to install.
3. **Base check.** Where FrogLord has a fingerprint for the build, it compares the open game with it.
   If the game already differs from retail, the dialog lists the differences and shows **Install anyway onto the
   already modified game**. Installing onto a modified copy gives the union of both mods and cannot be undone.
   A separate warning appears when the executable was saved by FrogLord before.
4. **Install.** FrogLord first resolves every name in the package against the open game. If any name does not resolve,
   it stops with the exact manifest key and changes nothing. Otherwise it applies the package and shows the result:
   every operation it performed and every warning.
5. **Save now.** Writes the `modded_` executable and MWD, the same as **File > Save**. The game is changed only in
   memory until then. If an install stopped part way, the dialog says so; reload the game before saving.

**VRAM warning.** After replacing images, FrogLord checks that the textures still fit in VRAM with its own texture
placement. On PC, images without black-is-transparent need room on texture pages without transparent textures.
When a package clears black-is-transparent on several level-select pictures, FrogLord's save cannot place `LS_ALL` in
VRAM. The dialog then says that saving will fail and disables **Save now**. Reload the game before trying another
package.

## What the installer does, in order
1. **Images.** Each listed image is replaced from its PNG, with its flags (black-is-transparent, translucent),
   in-game size and bit depth. Then the VRAM check above runs.
2. **Maps and remaps.** Each listed map (and its PC low-poly `_WIN95` twin) replaces the retail map, its texture remap is
   set from the image names in the `.remap` file, and a `timeLimit` from the level-select row is written into the map.
3. **Level-WAD models.** Each level's model archive is made to hold the models its `models=` list names. A model goes
   into the slot it occupies in other archives of the same theme and slot layout, and keeps the model's own resource
   id, so other programs can find it by id. Unused slots are emptied, as FrogLord's map save does.
4. **Theme books.** `deathHeight` for each listed theme.
5. **Level select.** The arcade (and race) table is rebuilt in the package's order; rows not listed are removed.
   Row fields, name images and pictures are set. Where a row gives a `name` but the package ships no name image,
   FrogLord renders the name into the level's existing name image with the game's font.
6. **Demos.** Listed demo files are replaced and demo-table rows set or skipped.
7. **Patches.** The patches for the `[Gameplay]` keys, then the `[Patches]` section. Patches run last so they see the
   final data.

## Gameplay keys and the bundled patches
The `[Gameplay]` keys are settings the game implements in code. FrogLord applies them with patches bundled in
`resources/games/frogger/patches/`, which can also be run by hand from **Edit > Patches**.

| Key | Patch | Builds |
|---|---|---|
| `initialLevels`, `visibleLevels` | `initial-levels.patch` | `pc-retail-v3.0e`, `pc-retail-v3.0e-vogons`, `psx-retail-usa` |
| `jungleSwap=false` | `jungle-swap.patch` | `pc-retail-v3.0e`, `pc-retail-v3.0e-vogons`, `psx-retail-usa` |
| `frameRate` | `fps.patch` | PC builds only |

- **`initial-levels.patch`** rewrites the 15 calls with which the game's level select sets each arcade level's starting
  flags at a new game. The flags are `SEL_LF_SELECTABLE` = 2 and `SEL_LF_ZONEACCESSIBLE` = 4 (1 means completed), and
  the game assigns rather than combines them. `initialLevels` gives a level 6, so it starts unlocked;
  `visibleLevels` gives it 4, so it is shown on the level select but cannot be played yet.
  The two lists together may name at most 15 levels; unused calls repeat the first pair, which is harmless.
  `visibleLevels` requires `initialLevels`. Multiplayer levels cannot be listed.
- **`jungle-swap.patch`** stops the game replacing `JUN1` with `JUN2` after the jungle is completed, at the end of the
  game, and when a save with the jungle gold frog is loaded. Its read-back code sets the variable `applied` when every
  site already holds the patched bytes; the exporter uses that to detect `jungleSwap=false`.
- **`fps.patch`** sets the game's frame rate. It stores the delay `1000/(fps*2)` as one byte, so a rate does not always
  read back as written: 30 reads back as 31.

The PSX addresses in `initial-levels.patch` and `jungle-swap.patch` come from the Frogger Plus+ script and have not
been verified against a PSX executable. The `pc-retail-v3.0e-vogons` addresses repeat the v3.0e ones and have not
been checked against that executable either.

## Debug tools
Under the **Debug** menu of an open Frogger game:

- **Generate Base Fingerprint** writes the fingerprint of the open game to a `.cfg` file. Run it on a freshly opened
  retail copy to make a fingerprint for a build FrogLord does not ship one for.
- **Compare With Base Fingerprint** compares the open game with the shipped fingerprint for its build, or with a
  fingerprint file you choose when there is none, and lists the differences in the log.
- **Check Mod Package Round-Trip** installs a package onto a retail copy, exports the result again, and compares the two
  manifests and content hashes.

## Noodle entry points
Scripts can run the same code through `FroggerModUtils`:

- `FroggerModUtils.exportModPackage(game, baseExeOrNull, outputFile, manifestOverrides)`: exports the open game.
  `baseExeOrNull` is the executable of an original copy, or null to use the shipped fingerprint;
  `manifestOverrides` is a Config holding the `[Mod]` details.
- `FroggerModUtils.installModPackage(game, packageFile, allowModifiedBase)`: installs a package and returns the
  result. It does not save.
- `FroggerModUtils.generateBaseFingerprint(game, outputFile)`: writes the open game's fingerprint.
- `FroggerModUtils.compareWithBaseFingerprint(game)`: compares the open game with its build's fingerprint and returns
  the differences.

## Limitations
- A package cannot add archive entries, new model names or new map slots. Models and images replace ones the base
  build already has.
- Format version 1 carries no sounds, music, entity scripts, form library, pickup data or map books.
  The exporter reports such changes as not representable.
- A mod with variants, such as optional simplified PSX maps, is shared as separate packages, one per variant.
- FrogLord places textures in VRAM itself when saving, so placement can differ from what FrogLord 1.0.2 and a mod's
  own Noodle script produced. The package never records VRAM positions.

## Frogger Plus+ as the worked example
The Frogger Plus+ Demo v1.0 was first distributed as a Noodle script with loose files. Exported from the result on
`pc-retail-v3.0e`, it becomes one package: 9 replaced maps, 3 of them with new texture remaps; 22 images in 3 banks
(9 level-select pictures and 9 names in `LS_ALL`, three textures in `ORG_VRAM`, the title logo in `STARTNTSC`); an arcade table reduced to 9 rows;
a new death height for the `DESERT` theme; 3 replaced demos with their table rows; and the gameplay keys
`initialLevels`, `visibleLevels` and `jungleSwap=false`, plus `frameRate` when the script's frame-rate prompt was
answered with a rate other than retail's. The abbreviated manifest is in the
[format reference](../../froglord/mod-packages.md#example).
