package net.highwayfrogs.editor;

/**
 * Represents the entry point of FrogLord.
 * Created by Kneesnap on 4/3/2025.
 */
public class FrogLordMain {
    public static void main(String[] args) {
        // The following options must be set before JavaFX is loaded on the class-path.

        // This is unfortunately a bit complicated, and I'm not currently sure that I've diagnosed the problem right either.
        // The short of it is that without this setting set, it's possible to create race conditions in JavaFX between AnimationTimers (which run each frame), and the actual render thread.
        // In most cases this can go unnoticed, but for situations where we update the texture atlas each frame,
        // View VOL2.MAP (Lava Crush) in Frogger (PSX Build 71) and enable animation previews to see what I mean after disabling this.
        //System.setProperty("quantum.multithreaded", "false"); // WAIT... Actually, we can't tell if this fixes it or not, because it lowers the FPS drastically.

        FrogLordApplication.main(args);
    }
}
