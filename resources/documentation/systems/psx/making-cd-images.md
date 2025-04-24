# How to create/install PlayStation (PSX) Mods with FrogLord
The following guide explains how to create modifications to FrogLord's supported PlayStation release.

# General Guide
> [!NOTE]
> The steps here are also valid for PlayStation games other than ones supported by FrogLord.

## Step #1) Download [mkpsxiso](https://github.com/Lameguy64/mkpsxiso).
The tools downloaded here are for working with `.bin/.cue` PlayStation CD images.  
Or more specifically, just the `.bin` files, which contain all the data on a CD.  
Make sure you have the PSX CD image for the game you're looking to mod ready to go.
It must have a SINGLE `.bin` and a SINGLE `.cue` file, because `mkpsxiso` doesn't support CD images with multiple `.bin` files.  

> [!IMPORTANT]
> By default, Windows will hide common file types like `.txt`, `.bin`, `.exe`, etc.  
> If your file names look like `Frogger` instead of `Frogger.bin`, you will need to disable that feature.  
> To do so, search for `File Explorer Options` in the Windows/Start menu, then navigate to the `View` tab.  
> Make sure the `Hide extensions for known file types` checkbox is NOT ticked.

## Step #2) Extracting the CD Image
The program called `dumpsxiso.exe` is the tool which will extract all files/data from the `.bin` file into a folder.  
It will also create a special template (`.xml` file) which describes how to recreate the original CD image from the file folder.  

> [!NOTE]
> **Q:** Why do we use `dumpsxiso.exe` instead of regular ISO software?  
> **A:** Because most software can't dump `.STR` and `.XA` files correctly.  
> Also, no other software will make the `.xml` file that `mkpsxiso.exe` needs.  

> [!IMPORTANT]
> Both `dumppsxiso.exe` and `mkpsxiso.exe` are command-line applications, which will do nothing if launched by double-clicking them.
> They must be run from the command-line, where they will provide information on how to use them properly.  

## Step #3) Making Changes
After `dumpsxiso.exe` has been used, all files within the extracted files folder are valid to start changing.  
For Frogger and other Millennium Interactive games, this means replacing the `.MWD` file and the executable (`SLES##.###`, `SLUS##.###`, etc.) with ones saved by FrogLord.  
For games not directly supported by FrogLord, they will need specialized research & tools to edit their files.  
Once all files are changed to your liking, continue on to step 4.  

## Step #4) Applying Changed Files
Run `mkpsxiso.exe` from the command-line, and give it the `.xml` file previously created by `dumpsxiso.exe`.  
It should create a new PSX CD image, bootable in an emulator or physical console.  

> [!IMPORTANT]
> Both `dumppsxiso.exe` and `mkpsxiso.exe` are command-line applications, which will do nothing if launched by double-clicking them.
> They must be run from the command-line, where they will provide information on how to use them properly.

# With Source Code Changes (Frogger Only)
Frogger has its full PlayStation source code available, which has its own simplified setup for making Frogger PSX CD images.  
First, follow the instructions found in [the repository](https://github.com/HighwayFrogs/frogger-psx) to setup the source code.  
Next, save your mod with FrogLord.  
Replace `merge\FROGPSX.MWI` with FrogLord's newly saved `FROGPSX-MODIFIED.MWI`, and `build\NTSC-U (USA)\files\FROGPSX.MWD` with FrogLord's newly saved `FROGPSX-MODIFIED.MWD`.  
Next, run `clean.bat` (otherwise the .MWI won't update), and then `compile.bat`.
Upon completion, you will be asked if you'd like to create a new PSX CD image, and if so, the script will guild you through the rest.  
