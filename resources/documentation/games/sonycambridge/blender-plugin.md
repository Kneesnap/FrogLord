# How to use Blender to create Frogger maps.
This guide details how to use Blender to create custom maps for the following games:  
- Frogger (1997)
- MediEvil

> [!WARNING]  
> This guide assumes FrogLord has already been setup to a point where it is able to open game data successfully.

## Step 1) Install Blender
It is believed that any version of Blender 2.8.0 or later is compatible with the plugin.  
However, the only version it has been extensively tested with is Blender v4.4.1.  
The latest version of Blender is most likely compatible, available for download [here](https://www.blender.org/download/).

## Step 2) Exporting a map file to .fs
Currently, it is not possible to add new maps, only replace existing ones.  
So, select the map you'd like to replace in FrogLord and click `Export to ...`.
The exact file format will differ depending on the game. Frogger uses `.ffs` files, MediEvil uses `.mfs` files.  
Create a new folder for your custom map.

> [!TIP]
> Sometimes, it may be preferable to clear the map into a flat surface with the `Clear Map` option before exporting.  
> Only some games such as Frogger have the "Clear Map" option.  

For simplicity, this guide will refer to these `.mfs`/`.ffs` files as just `.fs` files from now on.  
The `.fs` file format is a text-based file containing all 3D geometry for a game map.  
It does not contain any information about entities, paths, platforms, etc.  
Alongside the `.fs` file, all available map textures will be exported, so it is good practice to make a new folder for each `.fs` file.  

## Step 3) Installing the Blender Add-on
Since `.fs` is a file format made specifically for FrogLord, Blender doesn't understand it by default.  
To solve this, we've made our own Blender script/add-on, which must be installed.  
The Blender script (`<GAME-NAME>-map-blender-plugin.py`) can now be found inside the same folder where the `.fs` file was saved.

> [!IMPORTANT]
> Select one of the following options for installing the Blender add-on.

### Installation Option #1 (Loaded every time Blender is started)
Copy `<GAME-NAME>-map-blender-plugin.py` into `<Blender Install Folder>\scripts\addons_core`.  
Usually this will be somewhere like `C:\Program Files\Blender Foundation\Blender 4.4\4.4\scripts\addons_core`

Next restart Blender. Afterwards, open `Edit > Preferences > Add-ons`, and enable `FrogLord Map Support`.

### Installation Option #2 (Goes away each reboot)
While Blender is open, navigate to the `Scripting` tab, and click `Open`.  
Open `<GAME-NAME>-map-blender-plugin.py`,  then press the "play" icon to run the script.


## Step 4) Importing the map into Blender
To import the map into Blender, navigate to `File > Import > Game Map (.fs)`, and click it.  
Navigate to the `.fs` file exported earlier, and select it.  
Blender may freeze during import due to shader compilation, so it is okay if it takes a little while to finish.

> [!NOTE]  
> If the map appears very dark, this is because the "Rendered" view includes scene lighting, and there is none by default.  
> To fix this, switch to the "Material Preview" (the buttons to do this are at the top right of the viewport).

## Step 5) Making changes with Blender
Now that the map file has been imported into Blender, it's time to start making changes to the level.  

**Tutorial Video (Click to View):**  
While the following video tutorial is for Frogger, it is very similar to the steps/process for other games such as MediEvil.  
It is strongly recommended to watch this tutorial even for games other than Frogger.  
[![FrogLord Blender Tutorial Link](http://img.youtube.com/vi/5mniIS-sDQ4/0.jpg)](http://www.youtube.com/watch?v=5mniIS-sDQ4 "FrogLord Blender Tutorial")

> [!IMPORTANT]  
> Many of Blender's features are impossible to use with Frogger.  
> Even something as ubiquitous as creating custom materials will not be compatible with Frogger.  
> This is actually a good thing, because it drastically reduces/simplifies what the user needs to understand to use Blender effectively for Frogger.  
> Please read the following sections carefully.

### Supported Editing Capabilities
The first and most important thing to know about the Blender support is that when an export/import occurs, the only part of the scene which gets updated is a single object called `LevelObject`.  
Anything separate from this object can be added/removed freely, **BUT WILL NOT BE EXPORTED**.  
The solution to this is to merge any objects directly into the `LevelMesh`/`LevelObject` before exporting back to `.fs`.

The design philosophy for Blender support is to utilize its strengths to supplement the current limitations of FrogLord.  
For example, Blender is perfect for changing the level geometry, adding detail, etc. But it can't be used to modify entities, paths, etc.

### Material / Texture Limitations
It is strongly recommended not to add new materials to Blender manually.
This is because the materials created by importing a `.fs` file have many settings applied to them such as a shader graph, so they render correctly in Blender.  
Changes to this shader graph (and other material properties) will NOT apply to the game when imported back into FrogLord.  

**Instead, follow these steps:**  
1) Add the new textures to the .VLO file in FrogLord
2) Add the new textures to the map's texture remap. (The steps for this differ per-game).
3) Export the map file as a `.fs` file.
4) Import the `.fs` file into Blender, overwriting any existing mesh.
5) The new textures should be available for use in Blender as materials.

## Step 6) Transferring the changes back to FrogLord.
When the geometry changes are done, or you're ready to synchronize with FrogLord, save the `.fs` file from Blender with `File > Export > Game Map (.fs)`.  
Next, import the `.fs` file back into FrogLord by selecting the map file and pressing the `Import .fs` option (Usually available via right-click).

> [!IMPORTANT]  
> It is strongly recommended to only make changes in one program (Blender or FrogLord, but not both) at a time, and when finished with a particular program, synchronize changes to the other program.
>
> To synchronize FrogLord changes with Blender (and delete any changes in Blender which hadn't been shared with FrogLord), export the `.fs` file from FrogLord, then import it into Blender.  
> To synchronize Blender changes with FrogLord (and delete any changes in FrogLord which hadn't been shared with Blender), export the `.fs` file from Blender, then import it into FrogLord.  
> Otherwise, certain kinds of changes (geometry & collision grid) will be lost.

## Important Modelling Information
The following information applies only to Frogger.  

> [!IMPORTANT]  
> The default tile size of a map tile which can be hopped on is 16.0 x 16.0
> The game will behave strangely/incorrectly if polygons significantly larger (think >= 32.0 x 32.0) are configured for Frogger to hop on.
>
> Tiles should be aligned to 0 x 0, 16 x 16, as closely as possible, so that X=0, Z=0 overlaps with the corner of a polygon.  
> This is because Frogger is always aligned to 8.0 x 8.0 when hopping on a tile, regardless of the polygon shape.

> [!TIP]
> It is possible to move the entire map & collision grid. As long as it stays aligned to the 16.0x16.0 collision grid described above, it's perfectly valid to shift the entire map along any axis.  
> The collision grid will automatically update when imported back into FrogLord.  
> It is recommended to keep the floor of a level around Y=0 though.

## A note on lighting.
Frogger maps heavily rely on vertex coloring for map detail.  
Unfortunately, the vertex colors in the original editor (Mappy) and the lighting data were mixed together to create the vertex colors we see in-game.  
This was done when the .MAP files were created, not while the game is running.  
This is usually referred to as "baked lighting", and it creates a major problem for our ability to edit lighting.

For now, the simplest approach to lighting is to directly paint the vertex colors, OR find some way to add lights to a scene, then bake the lighting data into vertex colors in Blender.  
There has not been any research by the community on how to do this yet.  
