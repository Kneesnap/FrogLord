package net.highwayfrogs.editor.system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Holds three values.
 * Created by Kneesnap on 12/2/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tuple3<A, B, C> {
    private A a;
    private B b;
    private C c;
}
