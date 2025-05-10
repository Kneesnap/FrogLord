<p align="center">
<img src="https://github.com/Kneesnap/FrogLord/blob/master/resources/graphics/logo-large.png?raw=true" width="50%" height="50%">
</p>

# FrogLord - The Frogger (& More) Modding Tool
## What is FrogLord?
FrogLord is a modding suite for Frogger (1997). It allows fans to create new levels, import 3D models, view unused content, and allow changing all game files.
To use this tool, you must have a copy of the game.

## Getting Started:
Download FrogLord [here](https://github.com/Kneesnap/FrogLord/releases).  
If you need any help, have questions, or want to get in touch, don't hesitate to talk to us on our [website](https://highwayfrogs.net/) or our [discord server](https://discord.gg/GSNCbCN).

## Join the Community: [![Join the discord server!](https://dcbadge.limes.pink/api/server/https://discord.gg/GSNCbCN)](https://discord.gg/GSNCbCN)
Need help? Want to find/share mods? Talk with other Frogger fans? Join our [discord server](https://discord.gg/GSNCbCN).

## Screenshots:
![MAP Viewer](/_repository/level-screenshot.png)
![MOF Viewer](/_repository/model-screenshot.png)

## Supported Games
| Name                     | # of Supported Builds         | Support Notes                                 |
|--------------------------|-------------------------------|-----------------------------------------------|
| Beast Wars: Transformers | PC: 2, PSX: 4                 | All game data viewable, only some editable.   |
| C-12 Final Resistance    | PSX: 16                       | View/edit most game data. Saving is untested. |
| Frogger He's Back        | PC: 8, PSX: 67                | Full support (Custom Levels are available.)   |
| Frogger: The Great Quest | PC: 1, PS2: 3                 | Almost full support. (Cannot import 3D mesh)  |
| Frogger Beyond           | GC: 3, PC: 1, PS2: 2, Xbox: 1 | Extremely rudimentary viewer, no modding yet. |
| Frogger Rescue           | GC: 1, PC: 1, PS2: 3          | Extremely rudimentary viewer, no modding yet. |
| Frogger Ancient Shadow   | GC: 1, PC: 1, PS2: 1, Xbox: 1 | Extremely rudimentary viewer, no modding yet. |
| MediEvil                 | PSX: 38                       | View/edit most game data. Saving is untested. |
| MediEvil II              | PSX: 16                       | View/edit most game data. Saving is untested. |
| Moon Warrior             | PSX: 1                        | View/edit most game data. Saving is untested. |

## Can I Contribute?
Yes! Pull requests are welcome.  

## Build Instructions:
### Using IntelliJ

**Setup:**
1. Select ``Git`` from the ``Check out from Version Control`` option on the main menu. (It may be ``File > New > Project from Version Control`` if you're not on the main menu.)  
2. Clone this repository. The URL is: ``https://github.com/Kneesnap/FrogLord.git``.
3. Install the Lombok IntelliJ Plugin using the steps found [here](https://projectlombok.org/setup/intellij).

**Running:** Execute the `gradle run` task.  
**Building a jar:** Execute the `gradle jar` task.  
**Building an exe:** Execute the `gradle jpackage` task.  

## Special Thanks:
 - Andy Eder (Frogger 2 Programmer, Significant FrogLord contributor)
 - Mysteli (Highway Frogs Creator, Documented demo replay file format)
 - Aluigi (QuickBMS Author, Wrote a BMS script which we analyzed to understand the MWD and MWI file formats)
 - Shakotay2 (XeNTax, Helped us figure out how 3D geometry was stored)
 - Everyone involved with Frogger's creation who we've spoken with.
 - yohoat (FrogLord Logo)
 - archᵗʰ (BW/M1 FrogLord Logos & Beast Wars game info help)