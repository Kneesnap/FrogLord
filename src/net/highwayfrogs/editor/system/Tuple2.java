package net.highwayfrogs.editor.system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Holds two values.
 * Created by Kneesnap on 12/9/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tuple2<A, B> {
    private A a;
    private B b;
}
