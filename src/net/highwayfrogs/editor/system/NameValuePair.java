package net.highwayfrogs.editor.system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A simple name:value pair.
 * Created by Andy Eder.
 */
@Getter
@Setter
@AllArgsConstructor
public class NameValuePair {
    private String name;
    private String value;
}
