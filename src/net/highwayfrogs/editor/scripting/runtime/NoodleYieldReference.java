package net.highwayfrogs.editor.scripting.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a reference to a yielded noodle thread.
 */
@Getter
@RequiredArgsConstructor
public class NoodleYieldReference {
    private final transient NoodleThread<?> yieldedThread;
    private boolean resumed;

    // For the json serializer.
    private NoodleYieldReference() {
        this.yieldedThread = null;
    }

    /**
     * Resumes the yielded code context.
     */
    public void resume() {
        resume(null);
    }

    /**
     * Resumes the yielded code context.
     * @param primitive The return value to resume with.
     */
    public void resume(NoodlePrimitive primitive) {
        if (this.resumed)
            throw new NoodleRuntimeException("Cannot resume thread with this yield reference, as it has already been resumed.");
        if (this.yieldedThread.getStatus() != NoodleThreadStatus.YIELD)
            throw new NoodleRuntimeException("The thread cannot be resumed, because it is in the %s state.", this.yieldedThread.getStatus());

        this.resumed = true;
        this.yieldedThread.resume(primitive);
    }
}