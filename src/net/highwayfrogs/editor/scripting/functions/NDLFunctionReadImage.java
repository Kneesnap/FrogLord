package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleFileTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class NDLFunctionReadImage extends NoodleFunction{
    public static final NDLFunctionReadImage INSTANCE = new NDLFunctionReadImage();

    public NDLFunctionReadImage() {
        super("readImage", "<file>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        File file = args[0].getRequiredObjectInstance(NoodleFileTemplate.INSTANCE, "file");

        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (Throwable th) {
            throw new NoodleRuntimeException(th, "Failed to load image from file '%s'.", file.getName());
        }

        return thread.getStack().pushObject(image);
    }
}