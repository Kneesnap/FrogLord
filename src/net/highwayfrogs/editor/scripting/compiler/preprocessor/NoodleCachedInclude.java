package net.highwayfrogs.editor.scripting.compiler.preprocessor;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompiler;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeSource;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a file which gets #include'd into another script.
 */
public class NoodleCachedInclude {
    private final NoodlePreprocessor preprocessor;
    private final File file;
    private final List<NoodleToken> tokens = new ArrayList<>();
    private boolean fileHasBeenRead;

    public NoodleCachedInclude(NoodlePreprocessor preprocessor, File file) {
        this.preprocessor = preprocessor;
        this.file = file;
    }

    private String getLocalFilePath() {
        NoodleScript script = this.preprocessor.getCompileContext().getTargetScript();
        File scriptFolder = script != null ? script.getScriptFolder() : null;
        if (scriptFolder != null) {
            return Utils.toLocalPath(this.file, scriptFolder, true);
        } else {
            return this.file.getName();
        }
    }

    /**
     * Gets an updated token list.
     * @return The tokens to include from the file.
     * @throws FileNotFoundException Thrown if the file does not exist.
     */
    public synchronized List<NoodleToken> getTokens(NoodleCodeSource codeSource) throws FileNotFoundException {
        // Test if things have changed.
        if (!this.file.exists() || !this.file.isFile())
            throw new FileNotFoundException("File '" + getLocalFilePath() + "' does not exist.");

        if (!this.fileHasBeenRead) {
            this.fileHasBeenRead = true;
            this.tokens.clear();
            String fileContents = Utils.readFileText(this.file);
            NoodleCompiler.parseIntoTokens(codeSource, fileContents, this.tokens, 1);

            // Remove EOF token.
            if (this.tokens.size() > 0 && this.tokens.get(this.tokens.size() - 1).getTokenType() == NoodleTokenType.EOF)
                this.tokens.remove(this.tokens.size() - 1);
        }

        return this.tokens;
    }
}
