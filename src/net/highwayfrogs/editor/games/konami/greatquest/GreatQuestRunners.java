package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceString;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionFlag;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionTemplate;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseNumber;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseNumber.kcScriptCauseNumberOperation;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectActor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.Scanner;

/**
 * Contains various runners.
 * Created by Kneesnap on 4/22/2020.
 */
public class GreatQuestRunners {

    // This runner will replace the "Goblin Fort" level with the unused level on the PC version.
    public static void applyUnusedLevel(String[] args) throws Exception {
        File binFile = getBinFile(args);
        if (binFile == null)
            return;

        // Load main bin.
        System.out.println("Loading file...");
        DataReader reader = new DataReader(new FileSource(binFile));
        GreatQuestInstance instance = new GreatQuestInstance();
        instance.loadGame("pc-retail", binFile, null);
        instance.getMainArchive().load(reader);
        System.out.println("Loaded.");

        // TODO: Slight problem at the moment, which is that it doesn't save a 1:1 copy. We need to either get it so it saves each file correctly, so it produces a valid .bin. If we make every file a dummy file, this works.

        // Switch the level hashes, so it loads it.
        GreatQuestArchiveFile theGoblinFort = instance.getMainArchive().getFiles().get(31);
        GreatQuestArchiveFile ruinsOfJoyTown = instance.getMainArchive().getFiles().get(32);
        int goblinFortHash = theGoblinFort.getHash();
        boolean goblinFortCollision = theGoblinFort.isCollision();
        theGoblinFort.init(ruinsOfJoyTown.getFilePath(), theGoblinFort.isCompressed(), ruinsOfJoyTown.getHash(), theGoblinFort.getRawData(), ruinsOfJoyTown.isCollision());
        ruinsOfJoyTown.init(theGoblinFort.getFilePath(), ruinsOfJoyTown.isCompressed(), goblinFortHash, ruinsOfJoyTown.getRawData(), goblinFortCollision);

        System.out.println("Saving.");
        DataWriter writer = new DataWriter(new FileReceiver(new File(binFile.getParentFile(), "export.bin"), 300 * (1024 * 1024)));
        instance.getMainArchive().save(writer);
        writer.closeReceiver();
        System.out.println("Done.");
    }

    // This was a test in Rolling Rapids Creek to attempt to mod the game.
    public static void modRollingRapidsCreek(GreatQuestInstance instance) {
        GreatQuestAssetBinFile mainFile = instance.getMainArchive();

        // Modify script in rolling rapids creek.
        GreatQuestChunkedFile rollingRapidsCreek = (GreatQuestChunkedFile) mainFile.getFiles().get(16);
        kcScript script = rollingRapidsCreek.getScriptList().getScripts().get(33);

        int injectAfter = 1;

        int executionStartNumber = 1337;
        int executionNumber = executionStartNumber;
        for (int i = 0; i < 32; i++) {
            if (i == 0)
                continue; // Skip

            // Create clear flag function.
            kcScriptCauseNumber clearFlagDialogCause = new kcScriptCauseNumber(instance, kcScriptCauseNumberOperation.EQUALS, executionNumber++);
            kcScriptFunction clearFlagFunc = new kcScriptFunction(script, clearFlagDialogCause);

            // Add dialog resource.
            kcCResourceGeneric clearFlagDialog = new kcCResourceGeneric(rollingRapidsCreek, kcCResourceGenericType.STRING_RESOURCE, new kcCResourceString(instance, "Knee Flag Clear Test: " + i));
            clearFlagDialog.setName("FgClr" + Utils.padNumberString(i, 2), true);
            int clearFlagDialogHash = GreatQuestUtils.hash(clearFlagDialog.getName());
            rollingRapidsCreek.getChunks().add(clearFlagDialog);

            // Add dialog action.
            kcActionTemplate actionClearFlagDialog = (kcActionTemplate) kcActionID.DIALOG.newInstance(rollingRapidsCreek);
            actionClearFlagDialog.getArguments().add(new kcParam(clearFlagDialogHash));
            clearFlagFunc.getEffects().add(new kcScriptEffectActor(clearFlagFunc, actionClearFlagDialog, 0x68FF0A2));

            // Add clear action.
            kcActionFlag actionClearFlag = new kcActionFlag(rollingRapidsCreek, kcActionID.SET_FLAGS);
            actionClearFlag.getArguments().add(new kcParam(1 << i));
            clearFlagFunc.getEffects().add(new kcScriptEffectActor(clearFlagFunc, actionClearFlag, 0x68FF0A2));

            // Add increment function.
            // TODO

            // Created set flag function
            kcScriptCauseNumber setFlagDialogCause = new kcScriptCauseNumber(instance, kcScriptCauseNumberOperation.EQUALS, executionNumber++);
            kcScriptFunction setFlagFunc = new kcScriptFunction(script, setFlagDialogCause);

            // Add dialog resource.
            kcCResourceGeneric setFlagDialog = new kcCResourceGeneric(rollingRapidsCreek, kcCResourceGenericType.STRING_RESOURCE, new kcCResourceString(instance, "Knee Flag Set: " + i));
            setFlagDialog.setName("FgSet" + Utils.padNumberString(i, 2), true);
            int setFlagDialogHash = GreatQuestUtils.hash(setFlagDialog.getName());
            rollingRapidsCreek.getChunks().add(setFlagDialog);

            // Add dialog action.
            kcActionTemplate actionSetFlagDialog = (kcActionTemplate) kcActionID.DIALOG.newInstance(rollingRapidsCreek);
            actionSetFlagDialog.getArguments().add(new kcParam(setFlagDialogHash));
            setFlagFunc.getEffects().add(new kcScriptEffectActor(setFlagFunc, actionSetFlagDialog, 0x68FF0A2));

            // Add set flag action.
            kcActionFlag actionSetFlag = new kcActionFlag(rollingRapidsCreek, kcActionID.SET_FLAGS);
            actionSetFlag.getArguments().add(new kcParam(1 << i));
            setFlagFunc.getEffects().add(new kcScriptEffectActor(setFlagFunc, actionSetFlag, 0x68FF0A2));

            // Add increment function.
            // TODO

            // TODO: If last one, set variable to normal trigger.

            // Register functions.
            script.getFunctions().add(injectAfter++, setFlagFunc);
            script.getFunctions().add(injectAfter++, clearFlagFunc);
        }

        File outputFile = new File(instance.getMainGameFolder(), "ModdedPlayable\\data.bin");
        DataWriter writer = new DataWriter(new LargeFileReceiver(outputFile));
        mainFile.save(writer);
        writer.closeReceiver();

        System.out.println("Done.");

    }

    private static File getBinFile(String[] args) {
        String fileName;
        if (args.length > 0) {
            fileName = String.join(" ", args);
        } else {
            System.out.print("Please enter the file path to data.bin: ");
            Scanner scanner = new Scanner(System.in);
            fileName = scanner.nextLine();
            scanner.close();
        }

        File binFile = new File(fileName);
        if (!binFile.exists() || !binFile.isFile()) {
            System.out.println("That is not a valid file!");
            return null;
        }

        return binFile;
    }
}