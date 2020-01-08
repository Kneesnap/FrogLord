# FrogLord

## What is FrogLord?
FrogLord is a modding suite for Frogger (1997). It allows fans to create new levels, import 3D models, view unused content, and allow changing all game files.
To use this tool, you must have a copy of the game.

## Screenshots:
![MAP Viewer](https://i.imgur.com/R67Jhao.png)
![MOF Viewer](https://i.imgur.com/dZJwhUm.png)

## Usage:
Download: [here](https://github.com/Kneesnap/FrogLord/releases)  
Website: [here](https://highwayfrogs.net/)  
Discord: [here](https://highwayfrogs.net/thread/26/discord-group)  
If you need any help, have questions, or want to get in touch, don't hesitate to talk to us on our website or our discord.  

## Can I Contribute?
Yes! Pull requests are welcome.  

## Build Instructions:

### Using IntelliJ

**Setup:**
1. Select ``Git`` from the ``Check out from Version Control`` option on the main menu. (It may be ``File > New > Project from Version Control`` if you're not on the main menu.)  
2. Clone this repository. The URL is: ``https://github.com/Kneesnap/FrogLord.git``.
3. Install the Lombok IntelliJ Plugin using the steps found [here](https://projectlombok.org/setup/intellij).

**Running:**
1. ``Run > Run 'FrogLord GUI'``  

**Building:**
1. ``Build > Build Artifacts... > FrogLord > Build``

### Using Maven

**Setup:**
1. ``git clone https://github.com/Kneesnap/FrogLord.git``
2. ``cd FrogLord``
3. ``mvn compile`` - Verify code compiles

**Building:**
1. ``mvn package``

**Running:**
1. ``java -jar target/editor-{version}-jar-with-dependencies.jar`` 
    * `{version}` is the current release

## Special Thanks:
 - Andy Eder (Frogger 2 Programmer, Significant FrogLord contributor)
 - Xiron (Highway Frogs Creator, Documented demo replay file format)
 - Aluigi (QuickBMS Author, Wrote a BMS script which we analyzed to understand the MWD and MWI file formats)
 - Shakotay2 (XeNTax, Helped us figure out how 3D geometry was stored)
 - Everyone involved with Frogger's creation who we've spoken with.