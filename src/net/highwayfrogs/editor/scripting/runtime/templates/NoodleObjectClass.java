package net.highwayfrogs.editor.scripting.runtime.templates;

/**
 * Represents a Noodle template in a form which can directly be used in Noodle scripts.
 * Created by Kneesnap on 11/19/2025.
 */
@SuppressWarnings("rawtypes")
public class NoodleObjectClass extends NoodleObjectTemplate<NoodleObjectTemplate> {
    public static final NoodleObjectClass INSTANCE = new NoodleObjectClass();

    private NoodleObjectClass() {
        super(NoodleObjectTemplate.class, "Class");
    }

    @Override
    protected void onSetup() {
        addGetter("name", (thread, template) -> thread.getStack().pushObject(template.getName()));
        addFunction("isInstance", (thread, template, args) ->
                thread.getStack().pushBoolean(template.isObjectSupported(args[0].getAsRawObject())), "value");
        // Future: newInstance(args...) should share code with the static method caller for .new().

        addStaticFunction("forName", (thread, args) ->
                thread.getStack().pushObject(thread.getEngine().getTemplateByName(args[0].getStringValue())),
                "className");
    }

    @Override
    protected void onSetupJvmWrapper() {
        // Nothing to add.
    }
}
