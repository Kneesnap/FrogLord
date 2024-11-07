package net.highwayfrogs.editor.games.konami.greatquest.script.interim;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;

/**
 * Allows writing kcParam values.
 * Created by Kneesnap on 8/23/2023.
 */
public class kcParamWriter {
    private final kcParam[] arguments;
    @Getter private int currentIndex;

    public kcParamWriter(kcParam[] arguments) {
        this.arguments = arguments;
    }

    private kcParam getOrCreateNextParam() {
        if (this.currentIndex >= this.arguments.length)
            throw new RuntimeException("There are no more argument slots left to write to.");

        kcParam param = this.arguments[this.currentIndex];
        if (param == null)
            this.arguments[this.currentIndex] = param = new kcParam();

        this.currentIndex++;
        return param;
    }

    /**
     * Writes the value to the next argument index.
     * @param value The value to write.
     */
    public void write(int value) {
        getOrCreateNextParam().setValue(value);
    }

    /**
     * Writes the value to the next argument index.
     * @param value The value to write.
     */
    public void write(float value) {
        getOrCreateNextParam().setValue(value);
    }

    /**
     * Writes the value to the next argument index.
     * @param value The value to write.
     */
    public void write(boolean value) {
        getOrCreateNextParam().setValue(value);
    }

    /**
     * Writes the value to the next argument index.
     * @param enumValue The value to write.
     */
    public <TEnum extends Enum<TEnum>> void write(TEnum enumValue) {
        if (enumValue == null)
            throw new NullPointerException("enumValue");

        getOrCreateNextParam().setValue(enumValue.ordinal());
    }

    /**
     * Writes the value to the next argument index.
     * @param value The value to write.
     */
    public void write(kcParam value) {
        if (this.currentIndex >= this.arguments.length)
            throw new RuntimeException("There are no more argument slots left to write to.");

        this.arguments[this.currentIndex++] = value;
    }

    /**
     * Writes the value to the specified argument index.
     * @param index The index to lookup.
     * @param value The value to write.
     */
    public void setArgument(int index, kcParam value) {
        if (index < 0 || index >= this.arguments.length)
            throw new RuntimeException("The argument at index " + index + " does not exist.");

        this.arguments[index] = value;
    }

    /**
     * Writes empty values to all remaining params.
     */
    public void clearRemaining() {
        while (this.arguments.length > this.currentIndex)
            getOrCreateNextParam().setValue(0);
    }

    /**
     * Returns true if there is no more room for any data to be written.
     */
    public boolean isFull() {
        return this.currentIndex >= this.arguments.length;
    }
}