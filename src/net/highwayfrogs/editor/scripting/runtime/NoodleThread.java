package net.highwayfrogs.editor.scripting.runtime;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject.SharedGameObject;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.scripting.NoodleScriptFunction;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.functions.NoodleFunction;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstruction;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstructionCall;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstructionCallInstance;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstructionCallStatic;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * An instance of a running script.
 * <a href="https://en.wikipedia.org/wiki/Coroutine"/>
 */
@Getter
public class NoodleThread<T extends NoodleScript> extends SharedGameObject {
    private transient final T script;
    private transient Logger logger;

    // Main State:
    private final transient NoodleStack stack;
    private final transient NoodleHeap heap;
    private transient NoodlePrimitive result; // Script return/result value.
    private transient final List<NoodlePrimitive> arguments = new ArrayList<>();
    private transient int yieldRestoredLabel = -1;

    // Jsonable Program State:
    @Setter private int position;
    private final Stack<Integer> jumpStack = new Stack<>();
    private NoodleThreadStatus status = NoodleThreadStatus.NONE; // The status of the thread.
    @Setter private transient Runnable onFinishHook;
    @Setter private transient NoodleYieldReference lastSkipSupportingDelay;

    public NoodleThread(GameInstance instance, T script) {
        super(instance);
        this.script = script;
        this.stack = new NoodleStack(this);
        this.heap = new NoodleHeap(this);
    }

    @Override
    public Logger getLogger() {
        if (this.logger == null)
            this.logger = Logger.getLogger("NoodleThread['" + this.script.getName() + "'," + Integer.toHexString(System.identityHashCode(this)) + "]");

        return this.logger;
    }

    /**
     * Gets the script engine used for this thread.
     */
    public NoodleScriptEngine getEngine() {
        return this.script.getEngine();
    }

    /**
     * Causes the thread to yield, suspending execution.
     */
    public NoodleYieldReference yieldThread() {
        if (this.status != NoodleThreadStatus.RUNNING)
            throw new NoodleRuntimeException("Tried to yield while in the %s state.", this.status);

        this.status = NoodleThreadStatus.YIELD;
        this.yieldRestoredLabel = -1;
        return new NoodleYieldReference(this);
    }

    /**
     * Shuts down the thread, and any data related to operations on the server.
     * Does NOT impact persistent thread state, only data used for the runtime execution of a thread. Thus, debugging can be done.
     */
    public void shutdown() {
        // Run shutdown hook.
        fireHeapObjectShutdownHooks();
    }

    /**
     * Starts the thread for the first time.
     * @return status of thread when it either pauses or finishes.
     */
    public NoodleThreadStatus startThread() {
        if (getStatus() != NoodleThreadStatus.NONE)
            throw new NoodleRuntimeException("Cannot startThread when thread status is %s, use resume() instead!", getStatus());
        onThreadStart();
        return resume(null);
    }

    /**
     * Called when the thread starts.
     */
    protected void onThreadStart() {

    }

    /**
     * Cancels the rest of the execution.
     */
    public void cancel() {
        if (this.status != NoodleThreadStatus.CANCELLED && this.status != NoodleThreadStatus.ERROR && this.status != NoodleThreadStatus.FINISHED) {
            onCancel();
            this.status = NoodleThreadStatus.CANCELLED;
        }
    }

    /**
     * Handles an error, effectively halting / crashing the thread.
     * @param th The error to handle.
     */
    public void handleError(Throwable th) {
        if (this.status == NoodleThreadStatus.ERROR)
            return; // Already has error, no need to handle another!

        // Cancel the thread.
        cancel();
        this.status = NoodleThreadStatus.ERROR;
        fireHeapObjectShutdownHooks();
    }

    /**
     * Completes this script with a result value.
     * @param resultValue The script's result.
     */
    public void complete(NoodlePrimitive resultValue) {
        if (this.status != NoodleThreadStatus.FINISHED) {
            this.status = NoodleThreadStatus.FINISHED;
            this.result = resultValue;
            if (resultValue != null)
                resultValue.tryIncreaseRefCount();

            fireHeapObjectShutdownHooks();
            onComplete();
        }
    }

    /**
     * Fires shutdown hooks for heap objects.
     */
    protected void fireHeapObjectShutdownHooks() {
        for (NoodleObjectInstance instance : getHeap().getObjectPool()) {
            if (instance == null || instance.getTemplate() == null || instance.getObject() == null)
                continue;

            // If we are not the thread this object reference belongs to, abort!
            if (this != instance.getThread() && instance.getThread() != null)
                continue;

            NoodleObjectTemplate<Object> template = instance.getObjectTemplate();
            try {
                template.onThreadShutdown(this, instance.getObject(), instance);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Failed to run thread shutdown hook for a(n) %s object in '%s'.", template.getName(), getScript().getName());
                handleError(th);
            }
        }
    }

    /**
     * Test if this thread has completed successfully.
     * @return complete
     */
    public boolean isComplete() {
        return getStatus() == NoodleThreadStatus.FINISHED;
    }

    /**
     * Test if this thread is in a state where it is running.
     * @return isRunning
     */
    public boolean isRunning() {
        return getStatus() == NoodleThreadStatus.RUNNING;
    }

    /**
     * Test if this thread is in a state where it is running, paused, or yielded.
     * Effectively tests if the script is not completed or cancelled.
     * @return isRunning
     */
    public boolean isExecutable() {
        return getStatus() == NoodleThreadStatus.RUNNING
                || getStatus() == NoodleThreadStatus.YIELD;
    }

    /**
     * Test if this thread has stopped execution.
     * NOTE: This does not consider a currently paused (yielding) thread as stopped.
     * @return complete
     */
    public boolean isStopped() {
        return getStatus() == NoodleThreadStatus.FINISHED
                || getStatus() == NoodleThreadStatus.ERROR
                || getStatus() == NoodleThreadStatus.CANCELLED
                || getStatus() == NoodleThreadStatus.NONE;
    }

    /**
     * Resumes the thread.
     */
    public NoodleThreadStatus resume() {
        return resume(null);
    }

    /**
     * Resume execution of this thread.
     * @param resumeValue A value to continue execution with.
     */
    public NoodleThreadStatus resume(NoodlePrimitive resumeValue) {
        if (this.script == null)
            throw new NoodleRuntimeException("Cannot resume noodle thread without having an associated script!");

        if (getStatus() == NoodleThreadStatus.ERROR || getStatus() == NoodleThreadStatus.FINISHED || getStatus() == NoodleThreadStatus.CANCELLED)
            return getStatus();

        // If we're resuming a paused thread, push the value on the stack.
        if (getStatus() == NoodleThreadStatus.YIELD) {
            this.lastSkipSupportingDelay = null;
            this.yieldRestoredLabel = -1; // Discard the restoration point.
            this.stack.pushPrimitive(resumeValue);
        }

        if (getStatus() == NoodleThreadStatus.NONE)
            onStart(); // This is starting for the first time.

        this.status = NoodleThreadStatus.RUNNING;

        while (this.position < getScript().getInstructions().size()) {
            if (getStatus() != NoodleThreadStatus.RUNNING)
                return getStatus(); // If the status has changed, stop execution for now.

            NoodleInstruction instruction = getScript().getInstructions().get(this.position++);

            try {
                instruction.execute(this);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Error running script instruction: `%s`. [%s]\n%s\n\n%s", instruction, NoodleUtils.getErrorPositionText(instruction), Utils.getErrorMessagesString(th), this.buildScriptInformation());
                handleError(th);
                return this.status;
            }
        }

        complete(null);
        return this.status;
    }

    /**
     * Gets information to identify the script.
     * @return scriptError
     */
    public String buildScriptInformation() {
        StringBuilder builder = new StringBuilder();
        this.buildScriptInformation(builder);
        return builder.toString();
    }

    /**
     * Gets information for debugging the script thread.
     * @return scriptError
     */
    public void buildScriptInformation(StringBuilder builder) {
        builder.append(Utils.getSimpleName(this));
        if (this.script == null) {
            builder.append(" (NULL SCRIPT)");
            return;
        }

        // Function name.
        builder.append(" '")
                .append(this.script.getName())
                .append("':");

        // Write instruction address.
        builder.append(Constants.NEWLINE)
                .append(" - Address: ")
                .append(this.position);

        // Write current function.
        NoodleScriptFunction function = this.script.getFunctionContainingAddress(this.position);
        if (function != null) {
            builder.append(Constants.NEWLINE)
                    .append(" - Function: ");
            function.writeSignature(builder, true);
        }
    }

    /**
     * Called when this thread starts running.
     */
    public void onStart() {
        // Adds the arguments to the heap.
        if (this.arguments != null)
            for (int i = 0; i < this.arguments.size(); i++)
                this.arguments.get(i).onThreadStartAsArgument(this);
    }

    /**
     * Called upon script completion.
     */
    public void onComplete() {
        // Run finish hook.
        if (this.onFinishHook != null)
            this.onFinishHook.run();
    }

    /**
     * Called upon cancelling the cutscene.
     */
    protected void onCancel() {

    }

    /**
     * Gets the last instruction. Only valid before completion of the quest.
     * @return lastInstruction
     */
    public NoodleInstruction getLastInstruction() {
        if (getPosition() <= 0 || isComplete() || getScript() == null || getPosition() > getScript().getInstructions().size())
            return null;

        return getScript().getInstructions().get(getPosition() - 1);
    }

    /**
     * Get the (Java-registered) function which the last instruction called, if there was one.
     * @return lastFunctionCall
     */
    public NoodleFunction getLastGlobalFunctionCall() {
        if (isComplete())
            return null;

        NoodleInstruction instruction = getLastInstruction();
        return (instruction instanceof NoodleInstructionCall) ? ((NoodleInstructionCall) instruction).getFunction() : null;
    }

    /**
     * Get the (Java-registered) instance function which the last instruction called, if there was one.
     * @return lastFunctionCall
     */
    public NoodleInstructionCallInstance getLastInstanceFunctionCall() {
        if (isComplete())
            return null;

        NoodleInstruction instruction = getLastInstruction();
        return (instruction instanceof NoodleInstructionCallInstance) ? (NoodleInstructionCallInstance) instruction : null;
    }

    /**
     * Get the (Java-registered) static function which the last instruction called, if there was one.
     * @return lastFunctionCall
     */
    public NoodleInstructionCallStatic getLastStaticFunctionCall() {
        if (isComplete())
            return null;

        NoodleInstruction instruction = getLastInstruction();
        return (instruction instanceof NoodleInstructionCallStatic) ? (NoodleInstructionCallStatic) instruction : null;
    }

    /**
     * Calls a script function from the thread.
     * @param function The script function to call.
     */
    public void callFunction(NoodleScriptFunction function, NoodlePrimitive[] arguments) {
       if (function == null)
           return;

       // Verify this function can be executed.
       if (function.getScript() != this.script) {
           // If you're calling a function from another script, it could be that you need to create a child thread for that script.
           throw new NoodleRuntimeException("Function '%s' cannot be called from %s's thread.", function.getName(), this.script.getName());
       }

       // Verify enough arguments were passed.
       if (function.getArgumentCount() > arguments.length)
           throw new NoodleRuntimeException("The function '%s' requires %d arguments, but %d were passed.", function.getName(), function.getArgumentCount(), arguments.length);

        // Push the current instruction address onto the stack, and jump to the instruction of the function.
        this.jumpStack.push(this.position);
        setPosition(function.getStartAddress());
        this.heap.pushFunctionContext(function, arguments);
    }

    /**
     * Attempts to return from the most recent function call.
     * @return functionCall
     */
    public boolean returnFromFunctionCall() {
        if (!this.heap.popFunctionContext())
            return false; // We're not in a function call.

        setPosition(this.jumpStack.pop());
        return true;
    }

    /**
     * Add an object instance to the thread arguments.
     * @param object the object to add
     * @return the object instance
     */
    public NoodleObjectInstance addObjectInstanceArgument(Object object) {
        NoodleObjectInstance instance = this.getHeap().getObjectInstance(object);
        if (instance == null)
            instance = new NoodleObjectInstance(this, object);

        instance.incrementRefCount();
        this.arguments.add(new NoodlePrimitive(instance));
        return instance;
    }
}