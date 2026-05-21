# Mod Installation Guide (Frogger: The Great Quest)

> [!NOTE]  
> This guide assumes you are already able to play the game.  
> To run the game on Windows, follow [this guide](./pc-version-fixes.md).  
> To set up a PlayStation 2 emulator, there are many guides available online.  

## 1) Download & Setup FrogLord
Instructions: [here](../../froglord/setup-guide.md)  

> [!NOTE]  
> Make sure the SOUND/ folder is found in the same folder as data.bin.  
> Otherwise, FrogLord will be unable to load or edit sound/music.  

### If installing a mod for PlayStation 2, there is an additional step:
All files from the disc should be extracted (Not just `data.bin`), so it will be possible to create an ISO later.  
The PC version does not need this step because all the relevant game files are stored on the hard drive already.  

Here is an example of what this folder contains with the US version, other versions may look slightly different:
```
SOUND/
DATA.BIN
IOPRP234.IMG
LIBSD.IRX
MCMAN.IRX
MCSERV.IRX
PADMAN.IRX
PCMSTRM.IRX
SDRSRV.IRX
SIO2MAN.IRX
SLUS_202.57
SYSTEM.CNF
```

## 2) Install the mod.
FrogLord currently be open and display a long list of all the files in the game.  
Follow the installation instructions provided with your mod explaining how to install it.  

## 3) Save the mod.
**NOTE:** Before saving changes, it is strongly recommended to create a copy of `data.bin` called `data-original.bin` as a backup just in case things go wrong.  

FrogLord can save changes from the "File > Save" menu option, or by pressing `Ctrl + S`.  

> [!WARNING]  
> On Windows/PC, you might have trouble saving data.bin
> Usually, this is because FrogLord cannot save to the `C:\Program Files (x86)\Konami\Frogger The Great Quest\`.  
> That folder needs admin permissions.  
> Try saving somewhere else such as your desktop, then copy the file there afterward.  

## 4) PlayStation 2 Version:
In order to run the modded version on either a PlayStation 2 or PlayStation 2 emulator, a CD image must be created.
 1) Download & Open [CDGenPS2](https://www.psx-place.com/resources/cdgenps2.698/)
 2) Drag all the game files/folders you extracted earlier (including your newly saved `data.bin` file) into the CdGenPS2 file list.
 3) Click "File > Create CD" to save your new CD image. (Usually as .bin)
 4) Now you can run this on a PlayStation 2 emulator or real PlayStation 2 using any number of methods.
