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
| `pc-retail-v3.0e-vogons` | Against an original copy or with no base. | Yes, as an alias of `pc-retail-v3.0e`. There is no fingerprint for it, so the open game is not checked. |
| `psx-retail-usa` | Against an original copy or with no base, because FrogLord ships no fingerprint for it. | Yes, without the unmodified-game check. |

A base fingerprint is a list of hashes of a retail build's content, as FrogLord re-saves it.
It lets FrogLord tell what changed without the original files. It holds hashes only, no game content.
The shipped one is `resources/games/frogger/fingerprints/pc-retail-v3.0e.cfg`.

## Exporting a mod
Open the modified game and choose **File > Export Mod Package...**.

1. **Mod details.** Name, version (`MAJOR.MINOR.PATCH`), author, and optionally a licence, homepage and description.
   Keep the package's `id` the same across releases of the mod; **New** makes a fresh one.
2. **Base.** What the open game is compared with, chosen in the **Base** list:
   - **Shipped fingerprint**: the fingerprint FrogLord ships for the build. This is the default where one exists.
   - **Compare with original files...**: an original, unmodified copy of the build. You select its executable; the MWD
     must sit beside it, named as the open game's MWD or as that name without the `modded_` prefix FrogLord gives saved
     files. The executable must be one FrogLord recognises as the same unmodified build. For a build without a
     fingerprint it is the only base that detects executable changes, and it is the only way to package raw writes.
   - **None: all content is exported**: FrogLord fingerprints the open game itself and treats every map, model, image,
     level-select row, demo, theme and remap as changed. This takes minutes (about four for `pc-retail-v3.0e`),
     reports every image without a unique name as an error, and cannot detect executable changes, so no gameplay keys
     or patches are exported.
3. **Analyse.** Compares the game with the base and fills the change list, the level names and the warnings.
4. **Change list.** Every detected change, grouped by kind (files, WAD children, images, level select, race select,
   demos, themes, remaps, FrogLord-owned executable regions, patch sites, unexplained executable blocks). Each is ticked;
   untick a change to leave it out of the package.
5. **Level names.** FrogLord cannot read a level's name back from the pixels of its name image, so the dialog lists the
   arcade levels and asks for each name as text. The fields start with the names entered at the last export of this
   game and are empty otherwise. The text becomes the level's `name=` in the manifest, which ports and other tools use.
6. **Warnings.** Changes the format cannot carry are listed here and in the export log: for example sounds, music,
   entity scripts, the form library, new archive entries or new images. So are **unexplained executable changes**:
   1 KiB blocks of the executable that differ from the base but are not a bundled patch or data FrogLord owns,
   typically from a Noodle script that wrote to the executable directly, or hex editing. They are listed ticked, and
   **Export** refuses until you untick them or choose raw writes.
7. **Raw writes (opt-in).** With original files as the base, you can tick the option to package unexplained
   executable changes as `patches/raw-writes.patch`, a generated patch of the exact differing bytes, marked `raw=true`.
   FrogLord applies it on install; ports of the game ignore it. With any other base the option is disabled.
8. **Output and Export.** Choose where to write the `.mod`, then **Export**. FrogLord validates the manifest, writes the
   package and writes `export.log` in the same folder, listing what was exported, what was left out and why. Another
   export into that folder replaces the log.

The dialog remembers the mod details, the output path and the level names of the last export in the game's user
configuration, in the section `[ModExport]` (level names under `[[LevelNames]]`), and fills them in next time.

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
placement, which it redoes on every save. On the PC builds, texture pages 0 to 7 are the pages for black-is-transparent
images (`pagesWhereBlackIsTransparent=0-7` in the VLO tree configuration
`resources/games/frogger/vlo/1997-11-15-pc-retail-1.0.cfg`); images without the flag go on the other pages.
The Frogger Plus+ package clears black-is-transparent on five 128×128 level-select pictures, which moves them to those
other pages, and the level-select tree then runs out of room when it reaches `org_grey`. FrogLord 1.0.2, which the mod
was made with, never placed textures again, so the original mod did not meet this. When the check fails the install
result has `saveExpectedToFail` set, and the dialog says that saving will fail and disables **Save now**. Reload the
game before trying another package.

## What the installer does, in order
1. **Images.** Each listed image is replaced from its PNG, with its flags (black-is-transparent, translucent),
   in-game size and bit depth. On PC the PNG's padding pixels are kept as packaged; on PSX the padding is regenerated
   from the in-game pixels. Then the VRAM check above runs.
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

The exporter recovers a patch's arguments by running its `[ArgsCode]` on the open executable, so read-back code must
give every read its address: each read starts from a reader reset to offset 0, so a read without an address reads the
start of the executable. `skycolor.patch` reads each of its four colors from its own address for this reason.
`nocd.patch` has no arguments and no `[ArgsCode]`; it is exported as a `[Patches]` entry without arguments.

## Debug tools
Under the **Debug** menu of an open Frogger game:

- **Generate Base Fingerprint** writes the fingerprint of the open game to a `.cfg` file. Run it on a freshly opened
  retail copy to make a fingerprint for a build FrogLord does not ship one for.
- **Compare With Base Fingerprint** compares the open game with the shipped fingerprint for its build, or with a
  fingerprint file you choose when there is none, and lists the differences in the log.
- **Check Mod Package Round-Trip** asks for a package and an unmodified executable (its MWD beside it), opens that copy,
  installs the package, exports the result against the shipped fingerprint and compares the two packages section by
  section, content hashes included. Nothing is saved. The report goes to the log.

## Noodle entry points
Scripts can run the same code through the static methods of `FroggerModUtils`:

- `exportModPackage(game, baseExeOrNull, outputOrNull, manifestOverridesOrNull)`: exports the open game and returns a
  `ModExportResult`. `baseExeOrNull` is the executable of an original copy (its MWD found as described above), or null
  to use the shipped fingerprint, or all content when the build has none. `outputOrNull` is the `.mod` to write, with
  `export.log` beside it, or null to only build the package. `manifestOverridesOrNull` is a Config holding the `[Mod]`
  details, as a manifest root or as the section itself; missing id, name, version and author are filled in.
  Unexplained executable changes are left out.
- `installModPackage(game, packageFile, allowModifiedBase)`: reads the package, compares the game with the shipped
  fingerprint and installs, returning a `ModInstallResult`. It does not save; check `isSaveExpectedToFail()` first.
- `generateBaseFingerprint(game, outputOrNull)`: returns the open game's fingerprint and writes it when a file is given.
- `compareWithBaseFingerprint(game)`: compares the open game with its build's fingerprint and returns the differences,
  or null when FrogLord ships none.
- Helpers the dialogs use: `writeModPackage(result, output)`, `validateExport(result)`, `getExportLogFile(output)`,
  `createDraftManifest(manifestOverrides)`, `createAllContentBase(game, logger)`, `findBaseMwd(game, baseExe)` and
  `loadBaseCopy(game, baseExe)`.

Noodle cannot use the unmodifiable lists some results return, such as `ModExportResult.getProblems()`: they read as
null, so calling `size()` on them stops the script. Use `hasErrors()`, `isEmpty()`, `isSuccess()`, `describe()` or
`getExportLog()` instead.

## Limitations
- A package cannot add archive entries, new model names or new map slots. Models and images replace ones the base
  build already has.
- Format version 1 carries no sounds, music, entity scripts, form library, pickup data or map books.
  The exporter reports such changes as not representable.
- A mod with variants, such as optional simplified PSX maps, is shared as separate packages, one per variant.
- FrogLord places textures in VRAM itself when saving, so placement can differ from what FrogLord 1.0.2 and a mod's
  own Noodle script produced, and a package that clears black-is-transparent on many PC images may not fit (see the
  VRAM warning). The package never records VRAM positions.

## Frogger Plus+ as the worked example
The Frogger Plus+ Demo v1.0 was first distributed as a Noodle script with loose files. Exported from the result on
`pc-retail-v3.0e`, it becomes one package: 9 replaced maps, 3 of them with new texture remaps; 22 images in 3 banks
(9 level-select pictures and 9 names in `LS_ALL`, three textures in `ORG_VRAM`, the title logo in `STARTNTSC`); an arcade table reduced to 9 rows;
a new death height for the `DESERT` theme; 3 replaced demos with their table rows; and the gameplay keys
`initialLevels`, `visibleLevels` and `jungleSwap=false`, plus `frameRate` when the script's frame-rate prompt was
answered with a rate other than retail's. The abbreviated manifest is in the
[format reference](../../froglord/mod-packages.md#example).
