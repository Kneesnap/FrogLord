package net.highwayfrogs.editor.system;

import lombok.Getter;
import lombok.Setter;

/**
 * A simple name:value pair.
 */
@Getter
@Setter
public class NameValuePair
{
    @Getter @Setter private String name;
    @Getter @Setter private String value;

    public NameValuePair(String name, String value)
    {
        Set(name, value);
    }

    public void Set(String name, String value)
    {
        this.name = name;
        this.value = value;
    }
}
