package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.NoodleConstants;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodleCachedInclude;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenString;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeSource;
import net.highwayfrogs.editor.scripting.tracking.NoodleFileCodeSource;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the '#include' preprocessor directive.
 */
public class NoodlePreprocessorDirectiveInclude extends NoodlePreprocessorDirective implements INoodleManualPreprocessor {
    private String filePath;
    private File file;

    public NoodlePreprocessorDirectiveInclude(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.INCLUDE, location);
    }

    @Override
    public void parseTokens(NoodlePreprocessorContext context) {
        NoodleToken nextToken = context.getCurrentTokenIncrement();
        if (nextToken.getTokenType() != NoodleTokenType.STRING)
            throw new NoodleSyntaxException("#include expected a file path (string), but got '%s' instead.", nextToken, nextToken);

        this.filePath = ((NoodleTokenString) nextToken).getStringData();
        final File scriptFolder = context.getCompileContext().getTargetScript().getScriptFolder();

        // Find folder of file.
        File loadFrom;
        NoodleCodeSource codeSource = nextToken.getCodeLocation().getSource();
        if (codeSource instanceof NoodleFileCodeSource) {
            loadFrom = ((NoodleFileCodeSource) codeSource).getFile().getParentFile(); // Gets the folder the current file is located in.
        } else {
            loadFrom = scriptFolder;
        }

        // Start reading from the root.
        String systemPath = this.filePath;
        if (systemPath.startsWith("/") || systemPath.startsWith("\\")) {
            systemPath = systemPath.substring(1);
            loadFrom = scriptFolder;
        }

        // Verify there is always a noodle extension. (Security & usability feature)
        if (!systemPath.endsWith("." + NoodleConstants.NOODLE_CODE_EXTENSION))
            systemPath += "." + NoodleConstants.NOODLE_CODE_EXTENSION;

        // Load file, but validate it is a valid path.
        File loadFile = new File(loadFrom, systemPath);
        boolean errorOccurred = false;
        try {
            loadFile = loadFile.getCanonicalFile();
        } catch (IOException ex) {
            errorOccurred = true;
        }

        if (errorOccurred || !FileUtils.isFileWithinParent(loadFile, scriptFolder)) // [SECURITY FEATURE] Do not allow loading scripts outside the script folder. This may not be necessary tbh.
            throw new NoodleSyntaxException("The relative file path '%s' is not accessible to Noodle scripts for security reasons.", this, this.filePath);

        if (!loadFile.exists())
            throw new NoodleSyntaxException("#include could not find the file '%s'. (Raw Path: '%s')", this, loadFile.getName(), this.filePath);

        this.file = loadFile;
    }

    @Override
    public List<NoodleToken> processAndWriteTokens(NoodlePreprocessorContext context) {
        NoodleCachedInclude include = context.getPreprocessor().getCachedInclude(this.file);
        NoodleFileCodeSource codeSource = context.getCompileContext().getCodeSource(this.file);

        // Apply tokens.
        try {
            return include.getTokens(codeSource);
        } catch (FileNotFoundException fex) {
            throw new NoodleSyntaxException(fex, "Could not include file '%s', the file did not exist.", this, this.file.getName());
        }
    }

    @Override
    public void toString(StringBuilder builder) {
        super.toString(builder);
        builder.append(" \"").append(NoodleUtils.compiledStringToCodeString(this.filePath)).append('\"');
    }

    @Override
    public void runPreprocessor(NoodlePreprocessorContext context, List<NoodleToken> tokens) {
        // Set a limit for how many includes.
        AtomicInteger includeCount = context.getCompileContext().getIncludes().computeIfAbsent(this.file, key -> new AtomicInteger());
        if (includeCount.incrementAndGet() > NoodleConstants.MAX_RECURSIVE_INCLUDES)
            throw new NoodleSyntaxException("File '%s' seems to include itself recursively (infinitely).", this, this.file.getName());

        // Run the preprocessor.
        context.getPreprocessor().runPreprocessor(context.getCompileContext(), tokens);

        // Free usage.
        includeCount.decrementAndGet();
    }
}
