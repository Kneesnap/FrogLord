# PlayStation 2 Advanced Debugging
This page is for programmers looking to debug their Frogger: The Great Quest mods on PlayStation 2.  

## Advanced Debugging
Using [PlayStation 2 TOOL machines](https://www.psdevwiki.com/ps2/PlayStation_2_Tool) (PS2 devkits, DTL-T10000), it is possible to use the original development tools originally used to make Frogger: The Great Quest.  
Most modders have no need to go this deep, and getting a PlayStation 2 TOOL is not an easy (or cheap) task.  
This is beyond the scope of what your average mod will use or benefit from, but it has been documented here regardless.  
The steps here assume you already have a DTL-T1XXXX which is properly working (eg: shows the connection screen after boot).  
To flash this from a PC, use the ComUtil tool described below, with the menu option "PlayStation2 > PS2Flash".  

### Introduction to CodeWarrior
The original developers of Frogger: The Great Quest used CodeWarrior as their IDE.  
Due to this, using the same CodeWarrior tools for debugging provides maximum compatibility with original debug symbols.  

### TOOL Setup
The TOOLs have a flash ROM inside of them, which was supposed to be updated to match whatever runtime library version is currently being used.  
Most PS2 TOOL guides will recommend you to flash version 3.0.0 from 2003, but Frogger: The Great Quest will crash when run over the network unless 2.4 is flashed.  
Technically, the original game uses 2.3.4, but 2.4.2 is the closest version available online, and it appears to work.  
In order to use the correct Runtime Libraries, the ROM for v2.4.2 must be flashed to your DTL-T10000 first.

**ROM Flash Steps (Only necessary once)**  
1) Download: [here](https://archive.org/download/ps2_sdks), in `sce_ps2_sdk_24.7z`.
2) Install [CodeWarrior ComUtil for PS2](https://archive.org/download/ps2_sdks).
3) Use ComUtil to flash `t10000-rel24.bin`.  

### Running over Network (Faster debugging)
The normal way of testing a modded game is saving it in FrogLord, then building a new ISO file, then running it in a PS2 emulator.  
This is a bit slow due to ISOs taking a while to create.  
Using a PlayStation 2 TOOL, it is possible to run the game entirely over the network, streaming game assets from your computer to the PS2 TOOL.  

**Steps:**  
 1) Create a copy of `SLUS_202.57`, `FroggerTGQNetwork.elf`.
 2) Using a hex editor, replace `the first occurance of cdrom0:\DATA.BIN` with `host0:DATA.BIN`. (Do not change the size of the file, so put an extra null byte after `.BIN` to keep it the same size.)   
 2a) Removing the backslash ensures the file path is relative to the folder `FroggerTGQNetwork.elf` is in (which should have your data.bin, sound folder, etc.)
 2b) DO NOT CHANGE THE SIZE OF THE FILE OR ADD/REMOVE BYTES!!! The new names have two fewer characters, so apply two null (00) bytes to the end of the string instead of deleting them.  
 3) Repeat the last step for `cdrom0:\SOUND\SNDCHUNK.IDX`, `cdrom0:\SOUND\SNDCHUNK.SCK`, and the occurrence of `cdrom0:` immediately followed by `.SBR`.  
 4) Remove the leading backslash from all strings matching `\SOUND\**.snd`.  
 5) Using the ComUtil program, click "Program > Download & Run" and choose `FroggerTGQNetwork.elf`. 
 6) If any of the previous steps were done wrong, when the game is run, there will likely be a message in the ComUtil console with the specific filename where the problem occurred. If the game crashes before getting there, the executable size was probably changed, start over.  

These steps only need to be done the first time. Once you have a working `FroggerTGQNetwork.elf` and the game data is in the same folder as it, you can use ComUtil without any of the other steps.

> [!NOTE]  
> Sometimes the ComUtil program will be slow/get stuck on either disconnecting or connecting.  
> This happens if the network connection is actively being used for something, such as streaming music.  
> If this happens, press the blue reset button on the unit and wait a few minutes. Kill the ComUtil.exe process if it doesn't disconnect within a minute or two.  

> [!NOTE]  
> I experienced extreme lag on the main menu with a memory card plugged in, and the game didn't seem to see any save files from my retail console.

**Saving game data with FrogLord:**  
 1) In-game, enter into either the options menu (if on main menu), or the inventory menu with select. It is important no music be playing for the disconnect to be successful.  
 2) ComUtil "PlayStation2 > Disconnect"
 3) Save data.bin and other files.
 4) ComUtil "Program > Download & Run" `FroggerTGQNetwork.elf`


### Profiling
The Metrowerks Performance Anaylsis Tools 1.1 can be used to profile the game, even on Windows 10/11.  

> [!CAUTION]  
> The user interface for the profiler is confusing.  
> No performance data will be shown until the profiling stops.  
> **DO NOT** click the stop sign to stop profiling, this actually cancels/aborts profiling.
> Instead, the proper way to stop profiling is to close the popup window which has the "Snapshot" button.  
> Only after doing this will your performance metrics be saved, and become accessible.

### Using a retail PS2 or TEST PS2 instead of a PS2 TOOL.
Using a retail and/or TEST PS2 may be feasible with [RDB](https://github.com/ps2dbg/RDB).  
However, this may require modifying the game to avoid rebooting the IOP.
