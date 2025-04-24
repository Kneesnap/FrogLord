# How to use Blender to create Frogger maps.
This guide details how to use Blender to create custom Frogger maps.  
> [!WARNING]
> This guide assumes FrogLord has already been setup to a point where it is able to open Frogger game data successfully.

## Step 1) Install Blender
It is believed that any version of Blender 2.8.0 or later is compatible with the plugin.  
However, the only version it has been extensively tested with is Blender v4.4.1.  
The latest version of Blender is most likely compatible, available for download [here](https://www.blender.org/download/).  

## Step 2) Exporting a map file to .ffs
Currently, it is not possible to add new maps, only replace existing ones.  
So, select the Frogger map you'd like to replace in FrogLord and click `Export to .ffs`.  
Create a new folder for your custom map.  

> [!TIP]
> Sometimes, it may be preferable to clear the map into a flat surface with the `Clear Map` button before exporting to `.ffs`.  

The `.ffs` file format is a text-based file containing all 3D geometry for a Frogger map.  
It does not contain any information about entities, paths, platforms, etc.  
Alongside the `.ffs`, all available map textures will be exported, so it is good practice to make a new folder for each `.ffs` file.  

## Step 3) Installing the Blender Add-on
Since `.ffs` is a file format made specifically for FrogLord, Blender doesn't understand it by default.  
To solve this, we've made our own Blender script/add-on, which must be installed.  
The Blender script (`frogger-map-blender-plugin.py`) can now be found inside the same folder where the `.ffs` file was saved.  

> [!IMPORTANT]
> Select one of the following options for installing the Blender add-on.

### Installation Option #1 (Loaded every time Blender is started)
Copy `frogger-map-blender-plugin.py` into `<Blender Install Folder>\scripts\addons_core`.  
Usually this will be somewhere like `C:\Program Files\Blender Foundation\Blender 4.4\4.4\scripts\addons_core`  

Next restart Blender. Afterwards, open `Edit > Preferences > Add-ons`, and enable `FrogLord Map Utility`.  

### Installation Option #2 (Goes away each reboot)
While Blender is open, navigate to the `Scripting` tab, and click `Open`.  
Open `frogger-map-blender-plugin.py`,  then press the "play" icon to run the script.  


## Step 4) Importing the map into Blender
To import the map into Blender, navigate to `File > Import > Frogger Map (.ffs)`, and click it.  
Navigate to the `.ffs` file exported earlier, and select it.  
Blender may freeze during import due to shader compilation, so it is okay if it takes a little while to finish.  

> [!NOTE]
> If the map appears very dark, this is because the "Rendered" view includes scene lighting, and there is none by default.  
> To fix this, switch to the "Material Preview" (the buttons to do this are at the top right of the viewport).

## Step 5) Making changes with Blender
Now that the map file has been imported into Blender, it's time to start making changes to the level.  
This guide does not cover how to use Blender to make changes to maps, but luckily there are many tutorials online for how to use Blender.  

> [!IMPORTANT]
> Most of Blender's features are impossible to use with Frogger.  
> Even something as ubiquitous as creating custom materials will not be compatible with Frogger.
> This is actually a good thing, because it drastically reduces/simplifies what the user needs to understand to use Blender effectively for Frogger.  
> Please read the following sections carefully.

### Supported Editing Capabilities
The first and most important thing to know about the Blender support is that when an export/import occurs, the only part of the scene which gets updated is a single object called `LevelObject`.  
Anything separate from this object can be added/removed freely, **BUT WILL NOT BE EXPORTED**.  
The solution to this is to merge any objects directly into the `LevelMesh`/`LevelObject` before exporting back to `.ffs`.  

The design philosophy for Blender support is to only support the parts of Frogger map editing that Blender is suited to support.  
For example, Blender is perfect for changing the level geometry, adding detail, etc. But it can't be used to modify entities, paths, etc.  

### Material / Texture Limitations
The only way to change which textures are available in Blender is to update the texture remap list in FrogLord, then export a new `.ffs` file.  
Do **NOT** try to create new materials directly in Blender and assume that because Blender shows the texture that FrogLord will be able to use it.  
A more advanced/capable solution will come in the future once the game has been modified to make modding easier.  

Custom materials & textures are valid for anything in the Blender scene which is not intended to be exported back to FrogLord.  

## Step 6) Transferring the changes back to FrogLord.
When the geometry changes are done, or you're ready to synchronize with FrogLord, save the `.ffs` file from Blender with `File > Export > Frogger Map (.ffs)`.  
Next, import the `.ffs` file back into FrogLord by selecting the map file and pressing the `Import .ffs` button.  

> [!IMPORTANT]
> It is strongly recommended to only make changes in one program (Blender or FrogLord, but not both) at a time, and when finished with a particular program, synchronize changes to the other program.
>
> To synchronize FrogLord changes with Blender (and delete any changes in Blender which hadn't been shared with FrogLord), export the `.ffs` file from FrogLord, then import it into Blender.  
> To synchronize Blender changes with FrogLord (and delete any changes in FrogLord which hadn't been shared with Blender), export the `.ffs` file from Blender, then import it into FrogLord.  
> Otherwise, certain kinds of changes (geometry & collision grid) will be lost.

## Important Modelling Information
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
Unfortunately, the vertex colors in the original editor (Mappy), and the lighting data were mixed together to create the vertex colors we see in-game.  
This was done when the .MAP files were created, not while the game is running.  
This is usually referred to as "baked lighting", and it creates a major problem for our ability to edit lighting.  

For now, the simplest approach to lighting is to directly paint the vertex colors, OR find some way to add lights to a scene, then bake the lighting data into vertex colors in Blender.  
There has not been any research by the community on how to do this yet.  
