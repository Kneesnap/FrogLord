# Mod packages
A mod package is a single file holding a manifest and the content files of a mod.
It describes a mod as **changes against a named base build** of a game, keyed by **stable names**
(level codes, image names, model names, bank names, build names), never by resource ids, archive slot positions,
VRAM coordinates or executable addresses.
A package holds only what changed, so it contains no content of the base game.

This document describes the format from the point of view of a program reading or writing packages.
Everything needed to read a package is here, plus the game's own file formats (maps, models, demos, PNG).
Where FrogLord behaves in a particular way that other programs need not copy, the text says so.
The game-specific sections described here are those of Frogger (game identifier `frogger`).

Out of scope for this format version: adding new map slots or archive entries, code beyond declared patches,
new entity types or entity scripts, sound and music replacement, FMVs, and anything that depends on where a
program places textures in VRAM.

## Container
- A package is a zip file using deflate (or stored) entries, with the extension `.mod`.
- The manifest is `mod.cfg` at the root of the zip.
- A zip whose entries all sit inside one root folder is also accepted when that folder holds `mod.cfg`.
  The folder is then treated as the package root. (People often zip a folder rather than its contents.)
- Directory entries, the `__MACOSX/` folder and `.DS_Store` files are ignored.
- Entry names are UTF-8 and case-sensitive. Every content path must:
  - use `/` as the separator (never `\`),
  - be relative (no leading `/`, no drive letter such as `C:`),
  - contain no empty, `.` or `..` segments,
  - contain no control characters.

  A reader rejects entries that break these rules. FrogLord reports them as errors and skips them.
- FrogLord writes `mod.cfg` as the first entry, then the content files sorted by path, with fixed timestamps,
  so the same package content always produces the same zip bytes. Readers must not rely on entry order.

## Manifest syntax
The manifest is a text file in FrogLord's [Config](config-files.md) dialect, restricted to the profile below.
The profile is the whole grammar; a reader does not need FrogLord's parser.

- **Encoding:** UTF-8 without a byte order mark. Lines end in LF or CRLF.
- **Comments:** `#` starts a comment anywhere on a line, including section lines and values.
  Whitespace before the comment is not part of the value.
  `##` is a literal `#` in section names, keys, values and text lines. For example `homepage=https://example.com/mods##frogger`
  is the value `https://example.com/mods#frogger`.
- **Sections:** a line starting with `[` opens a section. The number of brackets is the section's layer and must match on both sides.
  `[Name]` is layer 1 and attaches to the root. `[[Child]]` (layer 2) attaches to the most recent layer-1 section,
  `[[[Grandchild]]]` to the most recent layer-2 section, and so on. A section may not skip a layer.
  Child sections of one parent are ordered.
- **Keys:** inside a section, `key=value` lines come first. A line is a key line when it contains `=` and no space
  appears before the first `=`. Keys match `[A-Za-z0-9][A-Za-z0-9_.-]*` and are case-sensitive, so a key may start
  with a digit (the demo-table rows in `[Demos]` use numeric keys).
  A key may appear only once per section; a duplicate key is an error.
- **Text lines:** any other line inside a section is a text line. Text lines follow the keys of their section.
  The profile uses text lines only for the description in `[Mod]` and for the file list in `[Files]`.
  Blank lines carry no meaning.
  Because of the key rule, a description line such as `Score=100` would be read as a key; write `Score = 100` instead.
  A text line may not start with `[`.
- **Values:**
  - Strings are taken verbatim after `=`, up to a comment, with trailing whitespace removed. Writers put no space after `=`.
    The bare value `null` is reserved by the Config dialect (it reads as "no value"); the format never uses it.
    A value written entirely inside double quotes is a quoted literal in the Config dialect; writers do not quote values.
  - Lists are comma-separated. Spaces around items are ignored and empty items are dropped.
  - Booleans are `true` or `false`.
  - Integers are decimal. A `0x` prefix marks hexadecimal; writers only use it where a key says so.
  - Decimal numbers use `.` as the separator (`45.0`).
  - Paths are zip entry names relative to the package root, following the path rules of the container.

## Layout
```
mod.cfg                         manifest (required)
README.md                       optional long description
screenshots/*.png               optional, referenced from [Mod]
maps/<CODE>.MAP                 replaced map files, in the map format the base build uses
maps/<CODE>.remap               texture remap for that map: one image name per line, in slot order
models/<NAME>                   replaced model bytes (.XMR or .XAR), named by the base build's archive name
images/<BANK>/<name>.png        replaced images, one folder per texture bank
demos/<NAME>                    replaced demo recordings (for example ORG1DEMO.DAT)
patches/<id>.patch              mod-specific executable patches in FrogLord's .patch format
```

`<CODE>` is a level's file code (`CAV1`, `ORG1`, `SWP5`, `CAVM`, `ISLAND`, `QB`).
`<BANK>` is a texture bank's archive name without `.VLO` (`LS_ALL`, `ORG_VRAM`, `ORG_VRAM_WIN95`, `STARTNTSC`).
`<name>` is an image name from the base build's image list, or a name a FrogLord user gave a texture.
`<NAME>` for models and demos is the archive name of an existing entry.

The manifest points at each file explicitly, so the folders are a convention rather than a rule.
Everything about where a file ends up in the game is derived by the installer.

## Sections
`[Mod]` and `[Targets]` are required. Every other section is optional; an absent section, key or child means
"unchanged from the base build". Sections may appear in any order; writers use the order below.

### [Mod]
The identity of the mod.

| Key | Required | Meaning |
|---|---|---|
| `formatVersion` | yes | Integer. `1` for this document. |
| `game` | yes | Game identifier: `frogger`. |
| `id` | yes | A version 4 UUID, stable across releases of the same mod. |
| `name` | yes | Display name. |
| `version` | yes | Semantic version `MAJOR.MINOR.PATCH`, optionally with `-prerelease` and `+build` suffixes. |
| `author` | yes | Free text. |
| `licence` | no | SPDX identifier or free text. The author's statement; tools do not check it. |
| `homepage` | no | URL. |
| `icon` | no | Path of an image in the package. |
| `screenshots` | no | List of paths of images in the package. |
| `exportedBy` | no | The tool and version that wrote the package, such as `FrogLord 1.1.0`. Informational. |
| `minFrogLordVersion` | no | FrogLord refuses to install the package when it is older than this. Other programs ignore it. |

The text lines of `[Mod]` are the description. Markdown is allowed.

### [Targets]
One child per base build the package was made for.
The child name is the FrogLord version-config name of the build, which identifies the build across tools.

| Key | Meaning |
|---|---|
| `exeChecksum` | List of CRC32 values of the build's executable, decimal (or `0x` hex). Lets a program that does not know the build name recognise the build. |
| `aliases` | List of other build names the same content installs onto unchanged. |

```
[Targets]
[[pc-retail-v3.0e]]
exeChecksum=378672750,269827179
aliases=pc-retail-v3.0e-vogons
[[psx-retail-usa]]
exeChecksum=1166491607
```

A package whose content differs per target puts the differences in `[Overrides]`.

### [Gameplay]
Settings that the original game implements in code. An installer that patches the executable maps each key to a
patch; a port of the game reads the key directly.

| Key | Type | Meaning |
|---|---|---|
| `initialLevels` | list of level codes | Levels that start unlocked and selectable. Order is irrelevant. Retail: `ORG1,SUB1,FOR1,VOL1`. |
| `visibleLevels` | list of level codes | Levels that start shown on the level select but not yet selectable. Retail: `ORG2` to `ORG5`, `SUB2` to `SUB5`, `FOR2`, `VOL2`, `VOL3`. Absent means none beyond `initialLevels`. |
| `jungleSwap` | boolean, default `true` | `false` keeps `JUN1` in place after the jungle level instead of swapping in `JUN2`. |
| `frameRate` | integer | The game's tick rate. PC only. |

FrogLord implements the keys with bundled patches: `initialLevels` and `visibleLevels` with `initial-levels.patch`,
`jungleSwap=false` with `jungle-swap.patch` and `frameRate` with `fps.patch`. When exporting, it reads the values back
from the executable, which has two consequences for writers comparing a round trip:

- `jungleSwap=false` is exported when the patch's `applied` variable, set by its read-back code, reports that all three
  sites hold the patched bytes. Sites that changed in some other way are reported, and the key is left out.
- `fps.patch` stores the frame delay `1000/(fps*2)` as a single byte, and reads it back as `1000/delay/2` with integer
  division, so `frameRate=30` is written as a delay of 16 and reads back as 31. Exporting a game installed with
  `frameRate=30` gives `frameRate=31`; installing either value writes the same byte.

New keys arrive with new format versions. An unknown key in `[Gameplay]` is an error for an installer,
because it cannot honour the setting, and a warning for a port.

### [LevelSelect]
The level-select tables.

| Key | Meaning |
|---|---|
| `arcade` | Ordered list of the level codes that remain in the arcade table. Levels of the base build that are not listed are removed. Absent means the table is unchanged. |
| `race` | The same for the race table. |

Each level may have a child, named by its level code, holding the row's fields. All keys are optional; an absent key
keeps the base build's value, and rows without a child keep all their values.

| Key | Type | Meaning |
|---|---|---|
| `world` | world name | The level-select world (`ORIGINAL`, `SUBURBIA`, ...). |
| `stackPosition` | integer | Position in the world's stack. |
| `localLevelId` | integer | Level number within the world. |
| `levelsInWorld` | integer | Number of levels in the world. |
| `name` | text | The level's name as text. This is the authoritative name. |
| `name.<lang>` | text | A translation of `name`, for example `name.fr`. |
| `nameImage` | image name | The image shown as the level's name. |
| `picture` | image name | The image shown as the level's picture. |
| `timeLimit` | integer | The starting time limit of the level, in seconds. |

```
[LevelSelect]
arcade=CAV1,DES1,FOR1,JUN1,ORG1,SKY1,SUB1,SWP1,VOL3
[[ORG1]]
world=ORIGINAL
stackPosition=0
localLevelId=0
levelsInWorld=1
name=BRAMBLE BUNCH
nameImage=org3name
picture=org3pic
timeLimit=74
```

`nameImage` and `picture` must name images that the base build's level-select table can already point at.
When the package also ships `images/<BANK>/<nameImage>.png`, that image is used.
Otherwise FrogLord renders `name` into the existing name image with the game's font; a port can draw `name` itself.

### [Maps]
One child per replaced map, named by level code.

| Key | Meaning |
|---|---|
| `file` | Path of the map file. |
| `remap` | Path of the texture remap. Absent keeps the base build's remap. |
| `lowPolyFile` | PC only: path of the low-poly twin of the map (for example `CAV1_WIN95.MAP` in the base build). |
| `models` | List of every model the level needs, by archive name. |

```
[Maps]
[[CAV1]]
file=maps/CAV1.MAP
remap=maps/CAV1.remap
lowPolyFile=maps/CAV1_WIN95.MAP
models=CAV_VAMP_BAT.XMR,CAV_FROGGER.XMR
```

`models` is the complete list of models the level's model archive must hold. The installer makes the level's model
archive hold exactly these, taking each model's bytes from `[Models]` when the package ships it and otherwise from
wherever the base build holds that name. (FrogLord also keeps the entries the game requires in every level.)

A model copied into a level's model archive carries the model's own resource id, so any program can find it by id as
well as by bytes. FrogLord's older WAD-copy tool (`copyWadEntry`, used by earlier mod scripts) kept the `NULL`
placeholder id in the copied slot; FrogLord's exporter recognises such placeholder copies by their bytes and names
them after the model they match.

A remap file lists image names, one per line, in slot order. `#` comments and blank lines are allowed.
The number of names must equal the number of slots in the base build's remap, because remaps cannot grow.

### [Models]
One child per replaced model, named by its archive name.

| Key | Meaning |
|---|---|
| `file` | Path of the model file. |

```
[Models]
[[ORG_SNAKE.XMR]]
file=models/ORG_SNAKE.XMR
```

The name must exist in the base build; new model names are out of scope.
A model that a level needs but the package does not replace needs no entry.

### [Images]
One child per texture bank, holding one child per replaced image, named by image name.

| Key | Type | Meaning |
|---|---|---|
| `file` | path | The PNG. Its size is the padded size the bank stores, at most 256×256. |
| `blackIsTransparent` | boolean | Black pixels are transparent. |
| `translucent` | boolean | The image is drawn translucent. |
| `inGameWidth`, `inGameHeight` | integer | The visible size inside the padding. Absent keeps the base image's current padding. |
| `bitDepth` | `4`, `8` or `16` | The stored bit depth. Absent keeps the base image's. |

```
[Images]
[[LS_ALL]]
[[[cav1pic]]]
file=images/LS_ALL/cav1pic.png
blackIsTransparent=false
translucent=false
[[ORG_VRAM]]
[[[org_rivbed1]]]
file=images/ORG_VRAM/org_rivbed1.png
blackIsTransparent=false
translucent=false
inGameWidth=38
inGameHeight=38
```

An absent flag keeps the base image's value; FrogLord always writes both flags for a replaced image.
FrogLord's exporter writes `inGameWidth` and `inGameHeight` whenever the in-game size differs from the PNG's padded size.
When the keys are absent, the installer keeps the padding the base image has now and takes the in-game size as the PNG's
size minus that padding, so a hand-written manifest that replaces a padded image without the keys keeps its padding.
Flags apply to that bank's copy of the image only. An image present in two banks is two entries.
VRAM placement is never recorded: FrogLord recomputes it when saving, and other programs do not need it.

### [Themes]
One child per theme, named by theme name (`DESERT`, `JUNGLE`, ...).

| Key | Type | Meaning |
|---|---|---|
| `deathHeight` | decimal | The height below which Frogger dies, in world units. |

### [Demos]
| Key | Meaning |
|---|---|
| `files` | List of replaced demo file names. Each file is at `demos/<name>`. |

An optional `[[Table]]` child changes the demo table. Each key is a row index; the value is
`level,demo file,unlock level` or `skip`. The demo plays the level, and only once the unlock level is unlocked.

```
[Demos]
files=ORG1DEMO.DAT,SKY1DEMO.DAT,VOL3DEMO.DAT
[[Table]]
0=ORG1,ORG1DEMO.DAT,ORG1
1=SKY1,SKY1DEMO.DAT,SKY1
2=VOL3,VOL3DEMO.DAT,VOL3
3=skip
```

Rows not listed keep the base build's values. The table length is fixed by the build.

### [Patches]
Executable patches beyond `[Gameplay]`. One child per patch.

| Key | Meaning |
|---|---|
| `file` | Path of a patch shipped in the package. Absent means a patch bundled with FrogLord whose id is the child name. |
| `raw` | `true` when the patch was generated from executable changes the exporter could not explain. |
| any other key | An argument value of the patch, by argument name. |

```
[Patches]
[[fps]]
fps=30
[[my-fix]]
file=patches/my-fix.patch
raw=true
```

Patch files use FrogLord's `.patch` format unchanged, so they can also be run by hand.
A patch applies only to the builds its own version list names.
A port does not run patches; it reads this section only to warn about what it cannot reproduce.

### [Overrides]
Per-target differences. Each child is named after a target listed in `[Targets]` and holds sections shaped like the
root sections. For one target, the effective manifest is the root without `[Overrides]`, with that target's child
merged over it:

- keys replace keys,
- child sections merge by name, recursively, and children not in the root are added,
- a list value replaces the whole list,
- text lines replace text lines when the override has any.

`[Mod]`, `[Targets]`, `[Files]` and `[Overrides]` cannot be overridden. FrogLord resolves an alias to its target
before looking up the override.

```
[Overrides]
[[psx-retail-usa]]
[[[Maps]]]
[[[[DES1]]]]
file=maps/psx/DES1.MAP
[[[LevelSelect]]]
[[[[ORG1]]]]
timeLimit=75
```

For `psx-retail-usa`, DES1 uses `maps/psx/DES1.MAP` and keeps the root's `remap` and `models`;
ORG1 has a time limit of 75 and keeps the root's name and images. Every other target sees the root unchanged.

### [Files]
The content fingerprint. Each text line describes one content file:

```
<sha1> <size> <path>
```

`<sha1>` is the SHA-1 of the file as 40 hexadecimal digits, `<size>` its size in bytes in decimal, and `<path>` its
zip path. Fields are separated by single spaces; the path is the rest of the line. Every file of the package except
`mod.cfg` is listed exactly once, including `README.md` and screenshots. Writers list the files sorted by path.

The key `contentHash` is the SHA-1 of the lines in the order they appear, each written in canonical form
(lower-case hex, one space, decimal size, one space, path), encoded as UTF-8 and followed by a single LF.
Every line, the last one included, ends in the LF; an empty list hashes the empty string.
A reader canonicalises each line before hashing: surrounding whitespace is removed, the SHA-1 is lower-cased and the
size is rewritten as a plain decimal number (so `007` becomes `7`). Writers sort by path in plain character order,
which for the ASCII paths of a package is byte order.
A reader can detect truncation and tampering with it, and can use it as a cache key for the package.

```
[Files]
contentHash=ab245f9e4133c65413111dd6c6e47bea50edcc0c
8589210a9c036da7c5a774772b428d86a7068b6b 31 maps/CAV1.MAP
f4de7355e680cc2ea8dc10823f58c7071d1c87a2 31 maps/DES1.MAP
```

A package without `[Files]` can still be read; its contents cannot be verified.

## Identity rules
1. **Builds** are FrogLord version-config names (`pc-retail-v3.0e`, `psx-retail-usa`), with their CRC32 lists for
   programs that do not know the names. A package never addresses an executable offset.
2. **Levels** are level file codes. The PC low-poly twin of a map is a separate file of the same level (`lowPolyFile`).
3. **Images** are `(bank, name)`. Names are the base build's image-list names or names a FrogLord user assigned; both
   resolve to one texture per build. Image names differ between the PC and PlayStation image lists, so a package for
   both either uses names present in both lists or overrides per target.
4. **Models** are archive names (`ORG_SNAKE.XMR`). Their slot in a level's model archive is never stored. The model
   archives of the same theme, the same detail role (high-poly, or the PC's `_WIN95` low-poly) and the same slot count
   share one slot layout, so the installer takes the slot the name occupies in a base-build archive of that layout.
   A high-poly model name never resolves into a `_WIN95` archive.
5. **Demos** and other replaced files are archive names.
6. **Themes** and **worlds** are theme and level-select world names (`DESERT`, `ORIGINAL`).
7. **Gameplay settings** are the `[Gameplay]` keys; how a program achieves them is its own business.

Names are case-sensitive and kept exactly as the base build spells them.

## Semantics
- A package is a **replace-or-add delta against the base build**. Every content file replaces the base entry of the
  same name, every table child replaces that row's listed fields, and everything not mentioned is the base build's.
  "Add" is limited to what the base build already has room for: a model into a level's existing model slot, a row
  into an existing table position, an image name into a bank that already holds that texture.
- **Installing** means opening an unmodified copy of a target build, applying the package, and saving.
  Installing onto an already modified copy gives the union of both mods and cannot be undone; FrogLord allows it
  with a warning.
- **Exporting** means writing the package that turns a base build into the modified game.
- **Round trip:** exporting a modified game and installing the result onto a fresh copy of the base build gives the
  same tables, remaps, images, model archives and patched executable bytes.

## Versioning and validation
`formatVersion` is a single integer. A reader accepts any version up to its own and rejects a newer one.
`minFrogLordVersion` guards FrogLord installer features; a mod's `version` is for people and mod browsers.

A reader reports problems as errors (the package cannot be used as written) or warnings (it can, but something
looks wrong):

| Problem | Severity |
|---|---|
| Unreadable zip, no `mod.cfg`, a syntax error, or a duplicate key in one section | error |
| A missing required `[Mod]` key, `formatVersion` newer than supported, an `id` that is not a UUID, a `version` that is not semantic | error |
| An `id` that is a UUID but not version 4 | warning |
| No child in `[Targets]`, a malformed checksum, an override naming an unlisted target | error |
| An unknown root section, or the same child section twice under one parent | error |
| An unknown key in `[Gameplay]` | error for installers, warning for ports |
| An unknown key elsewhere, or text lines where none are expected | warning |
| A path that breaks the path rules, or names a file the package does not hold | error |
| A `[Files]` line that is malformed, lists a missing file, or disagrees in size or SHA-1; a file not listed; a `contentHash` mismatch | error |
| No `[Files]` section, or no `contentHash` | warning |
| A value of the wrong type (`true`/`false`, integer, decimal, `bitDepth` outside 4, 8, 16, a malformed demo row) | error |

## Reading a package without FrogLord
A reader needs a zip library, the manifest profile above, a PNG decoder, and the game's own file formats for the
content files (maps, models, demos). Remaps and tables arrive as names and small records rather than bytes.

From a package, with no FrogLord involved, a program gets: the base build names and checksums, the content
fingerprint, maps with their remaps as image names, the models each level needs by name, images per bank and name
with their flags, level names as text, pictures as image names, the level-select order and row fields, theme and
demo-table changes, and the gameplay settings as keys. `[Patches]` is the only section a port reads just to warn.

A reference parser for the profile, in Python:

```python
def parse_manifest(text):
    root = {"name": None, "keys": {}, "text": [], "children": []}
    stack = [root]                                  # stack[n] is the open section of layer n
    for line in text.lstrip("﻿").splitlines():
        line = strip_comment(line).strip()
        if line.startswith("["):
            layer = len(line) - len(line.lstrip("["))
            if not line.endswith("]" * layer) or line.endswith("]" * (layer + 1)) or layer > len(stack):
                raise ValueError("bad section line: " + line)
            section = {"name": line[layer:-layer].replace("##", "#"), "keys": {}, "text": [], "children": []}
            del stack[layer:]
            stack[layer - 1]["children"].append(section)
            stack.append(section)
            continue
        current = stack[-1]
        eq, space = line.find("="), line.find(" ")
        if eq > 0 and (space < 0 or space > eq) and not current["text"]:
            key, value = line[:eq], line[eq + 1:].replace("##", "#")
            if key in current["keys"]:
                raise ValueError("duplicate key: " + key)
            current["keys"][key] = value
        elif line:
            current["text"].append(line.replace("##", "#"))
    return root

def strip_comment(line):
    i = 0
    while i < len(line):
        if line[i] == "#":
            if line[i + 1:i + 2] == "#":
                i += 2
                continue
            return line[:i]
        i += 1
    return line

def child(section, name):
    return next((c for c in section["children"] if c["name"] == name), None)
```

To get one target's view, copy the root without `[Overrides]` and merge `child(child(root, "Overrides"), target)`
over it with the rules of [Overrides]. Then check `[Files]`: hash every zip entry except `mod.cfg`, compare each line,
and compare `contentHash` with the SHA-1 of the canonical lines.

(FrogLord's own parser also accepts key lines after text lines and `\` escapes in keys; writers never produce either.)

## Example
The manifest FrogLord exports for the PC form of the Frogger Plus+ Demo v1.0, abbreviated where rows repeat:

```
[Mod]
formatVersion=1
game=frogger
id=<uuid>
name=Frogger Plus+ Demo
version=1.0.0
author=Particular Mushroom, Kneesnap
exportedBy=FrogLord 1.1.0
Nine new levels built from the retail themes.

[Targets]
[[pc-retail-v3.0e]]
exeChecksum=378672750,269827179
aliases=pc-retail-v3.0e-vogons

[Gameplay]
initialLevels=ORG1,SUB1,FOR1,VOL3,CAV1,SKY1,SWP1,DES1,JUN1
visibleLevels=SUB3,SUB4,SUB5,FOR2,VOL2
jungleSwap=false

[LevelSelect]
arcade=ORG1,SUB1,FOR1,VOL3,CAV1,SKY1,SWP1,DES1,JUN1
[[CAV1]]
localLevelId=0
levelsInWorld=1
name=DAMP DAMP CAVERN
nameImage=cav1name        # unchanged pointer, listed because the image is replaced
picture=cav1pic
# ... DES1, FOR1, JUN1, SKY1, SUB1, SWP1 alike; VOL3 also localLevelId=0 ...
[[ORG1]]
localLevelId=0
levelsInWorld=1
name=BRAMBLE BUNCH
nameImage=org3name        # the wider name image; the picture stays org1pic

[Maps]
[[CAV1]]
file=maps/CAV1.MAP
models=CAV_VAMP_BAT.XMR,...
[[DES1]]
file=maps/DES1.MAP
remap=maps/DES1.remap
models=DES_CRACK.XMR,...
[[JUN1]]
file=maps/JUN1.MAP
remap=maps/JUN1.remap
models=JUN_PARROT.XAR,JUN_TOUCAN.XAR,...
[[ORG1]]
file=maps/ORG1.MAP
remap=maps/ORG1.remap
models=ORG_SNAKE.XMR,...
# ... FOR1, SKY1, SUB1, SWP1, VOL3 alike ...

[Images]
[[STARTNTSC]]
[[[froglogo]]]
file=images/STARTNTSC/froglogo.png
[[LS_ALL]]
[[[cav1pic]]]
file=images/LS_ALL/cav1pic.png
blackIsTransparent=false
translucent=false
[[[cav1name]]]
file=images/LS_ALL/cav1name.png
blackIsTransparent=true
translucent=true
# ... the other pictures and names ...
[[[org3name]]]
file=images/LS_ALL/org3name.png
blackIsTransparent=true
translucent=true
inGameWidth=128
inGameHeight=16
[[ORG_VRAM]]
[[[org_rivbed1]]]
file=images/ORG_VRAM/org_rivbed1.png
blackIsTransparent=false
translucent=false
inGameWidth=38
inGameHeight=38

[Themes]
[[DESERT]]
deathHeight=45.0

[Demos]
files=ORG1DEMO.DAT,SKY1DEMO.DAT,VOL3DEMO.DAT
[[Table]]
0=ORG1,ORG1DEMO.DAT,ORG1
1=SKY1,SKY1DEMO.DAT,SKY1
2=VOL3,VOL3DEMO.DAT,VOL3
3=skip
# ... to the last row of the table ...

[Files]
contentHash=<sha1>
<sha1> <size> maps/CAV1.MAP
...
```
