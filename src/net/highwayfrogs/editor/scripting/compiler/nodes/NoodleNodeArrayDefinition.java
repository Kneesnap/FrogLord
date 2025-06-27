package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

@Getter
public class NoodleNodeArrayDefinition extends NoodleNode {
    @NonNull private final List<NoodleNode> arrayElements;

    public NoodleNodeArrayDefinition(NoodleCodeLocation codeLocation, List<NoodleNode> arrayElements) {
        super(NoodleNodeType.ARRAY_DEFINITION, codeLocation);
        this.arrayElements = arrayElements;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append('(').append(this.arrayElements.size()).append(" elements)[");
        for (int i = 0; i < this.arrayElements.size(); i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(this.arrayElements.get(i));
        }

        return builder.append("]").toString();
    }
}
