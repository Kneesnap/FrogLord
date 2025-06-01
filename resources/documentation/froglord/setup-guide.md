# FrogLord Setup Guide
This guide serves as a walkthrough for setting up FrogLord.  
TODO: Not specific to Frogger?

**Troubleshooting:**  
If at any point there are questions, issues, or other support required, please join the [Highway Frogs discord server](https://discord.gg/XZH9Wa5rMV).

## 1) Download & Extract FrogLord
FrogLord can be downloaded from [here](https://github.com/Kneesnap/FrogLord/releases).  
Make sure to extract the full FrogLord zip file before running FrogLord, not just the `.exe` file.  

> [!WARNING]
> Please check the file path where FrogLord is kept. No part of it (including parent folders!) may contain a semicolon ';' character!
> This is because a tool FrogLord relies on called `jpackage` has a bug that will cause FrogLord to not launch properly.
> Even worse, it will keep trying to launch FrogLord over and over causing your PC to eventually get overloaded with processes!
> If this occurs, rename FrogLord's `app` folder to something else, and all the processes will terminate.
> Because this issue occurs before any of FrogLord's code runs, we can't actually stop it until it is fixed by `jpackage`.

## 2) Run FrogLord
**On Windows Computers:**  
This is as simple as double-clicking `FrogLord.exe` after extraction.

**On Mac/Linux, or to run the legacy .jar:**  
The legacy FrogLord jar **MUST** be run with Java 8, so ensure the Java 8 Runtime (JRE) is installed on your system.  
The jar can be run with the terminal command `java -jar FrogLord.jar`.  

## 3) Select a game
Select the game you'd like to open from the dropdown list.  
For the purposes of this guide, we will select `Frogger: He's Back`, which is also known as `Frogger (1997)`.

## 4) Configuring the game
> [!WARNING]
> Each game may vary with the steps required to get the files necessary for setup.
> For example, Gamecube games would need Dolphin or some other software which can extract files from Gamecube isos.
> The following steps are written for `Frogger (1997)`.
> Some games will need to have their version selected manually. Others like `Frogger (1997)` will automatically select the version for you.

**For Windows:**  
Install Frogger to your computer as usual.  
The game files can be copied from the installation directory, usually `C:\Program Files\Hasbro Interactive\Frogger`.  

**For PlayStation:**  
The .bin/.cue files are PlayStation CD backups, and must be opened, so you can take the game files out of them.  
Software such as [PowerISO](https://www.poweriso.com/) and [ISOBuster](https://www.isobuster.com/) are able to open these, but it's also possible to use the [DuckStation](https://www.duckstation.org/) emulator's "Browse ISO" feature.  

**After obtaining the game files:**  
Show FrogLord where it can find each of the game files/folders it asks for.  
Then, click the `Load` button. If the load button cannot be clicked, check that you have provided all the necessary files and that the game version has been chosen.  

## 5) Next Steps
Now that the game has been successfully loaded into FrogLord, any changes you make can be saved.  
For PlayStation game versions, please refer to [this guide](../systems/psx/making-cd-images.md) for making custom PlayStation disc images.  
For Windows/PC game versions, replacing the game files in the installation directory will usually be sufficient to play the modded game.
