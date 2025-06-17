package net.highwayfrogs.editor.gui.editor;

import javafx.animation.AnimationTimer;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.lambda.TriConsumer;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents an animation timer which runs every frame.
 * Created by Kneesnap on 6/7/2024.
 */
public class MeshViewFrameTimer extends AnimationTimer {
    private final MeshViewController<?> controller;
    private final List<MeshViewFixedFrameRateTimer> registeredTaskTimers = new ArrayList<>();
    private final List<PerFrameTask<?>> perFrameTasks = new ArrayList<>();
    private String baseTitle;
    private final long[] frameTimes = new long[100];
    private int frameTimeIndex;

    public MeshViewFrameTimer(MeshViewController<?> controller) {
        this.controller = controller;
    }

    @Override
    public void handle(long now) {
        long oldFrameTime = this.frameTimes[this.frameTimeIndex];
        this.frameTimes[this.frameTimeIndex] = now;
        this.frameTimeIndex = (this.frameTimeIndex + 1) % this.frameTimes.length;

        // Tick frame-rate timers.
        for (int i = 0; i < this.registeredTaskTimers.size(); i++) {
            MeshViewFixedFrameRateTimer taskTimer = this.registeredTaskTimers.get(i);
            taskTimer.tick(now);

            // Remove timers once they no longer have any tasks.
            if (taskTimer.registeredTasks.isEmpty())
                this.registeredTaskTimers.remove(i--);
        }

        // Tick per-frame timers.
        long elapsedNanos = now - oldFrameTime;
        if (oldFrameTime != 0) {
            float deltaTimeSeconds = (float) (elapsedNanos / 100000000000D);
            for (int i = 0; i < this.perFrameTasks.size(); i++) {
                PerFrameTask<?> task = this.perFrameTasks.get(i);
                task.run(i, deltaTimeSeconds);

                // Remove timers once they no longer have any tasks.
                if (task.isCancellationScheduled())
                    this.perFrameTasks.remove(i--);
            }
        }

        if (this.frameTimeIndex == 0) {
            long elapsedNanosPerFrame = elapsedNanos / this.frameTimes.length;
            double frameRate = 1000000000D / elapsedNanosPerFrame;
            Stage stage = this.controller.getOverwrittenStage();
            if (stage != null) {
                String currentStageTitle = stage.getTitle();
                if (this.baseTitle == null || (!currentStageTitle.endsWith(")") && !currentStageTitle.contains("(FPS: ")))
                    this.baseTitle = currentStageTitle;
                if (this.baseTitle != null)
                    this.controller.getOverwrittenStage().setTitle(String.format("%s (FPS: %.3f)", this.baseTitle, frameRate));
            }
        }

        if (this.controller.getMesh() instanceof IPSXShadedMesh)
            ((IPSXShadedMesh) this.controller.getMesh()).getShadedTextureManager().getImageCache().cleanupExpiredEntries();
    }

    /**
     * Called when the timer is shutting down to no longer be active.
     */
    public void stop() {
        super.stop();

        // Restore the original title.
        Stage stage = this.controller.getOverwrittenStage();
        if (stage != null && this.baseTitle != null)
            this.controller.getOverwrittenStage().setTitle(this.baseTitle);
    }

    /**
     * Gets or creates the timer which has a given frame rate.
     * If zero is supplied, an unlimited frame-rate is used.
     * @param framesPerSecond the number of frames in a second to create a timer for
     * @return newTimer
     */
    public MeshViewFixedFrameRateTimer getOrCreateTimer(int framesPerSecond) {
        if (framesPerSecond < 0)
            throw new IllegalArgumentException(framesPerSecond + " is not a valid frame-rate!");

        int timerIndex = Utils.binarySearch(this.registeredTaskTimers, framesPerSecond, MeshViewFixedFrameRateTimer::getFramesPerSecond);
        if (timerIndex >= 0) {
            return this.registeredTaskTimers.get(timerIndex);
        } else {
            MeshViewFixedFrameRateTimer newTimer = new MeshViewFixedFrameRateTimer(this, framesPerSecond);
            this.registeredTaskTimers.add(-(timerIndex + 1), newTimer);
            return newTimer;
        }
    }

    /**
     * Adds a new task which will run every frame.
     * @param task the task to run on repeat
     * @return taskObject
     * @param <TTaskParam> data passed to the task in a memory-safe fashion.
     */
    public <TTaskParam> PerFrameTask<TTaskParam> addPerFrameTask(Consumer<Float> task) {
        if (task == null)
            throw new NullPointerException("task");

        return addPerFrameTask(null, (param, taskObj, deltaTime) -> task.accept(deltaTime));
    }

    /**
     * Adds a new task which will run every frame.
     * @param task the task to run on repeat
     * @return timerTaskObject
     * @param <TTaskParam> data passed to the task in a memory-safe fashion.
     */
    public <TTaskParam> PerFrameTask<TTaskParam> addPerFrameTask(BiConsumer<PerFrameTask<TTaskParam>, Float> task) {
        if (task == null)
            throw new NullPointerException("task");

        return addPerFrameTask(null, (param, taskObj, deltaTime) -> task.accept(taskObj, deltaTime));
    }

    /**
     * Adds a new task which will run every so many frames.
     * @param task the task to run on repeat
     * @return timerTaskObject
     * @param <TTaskParam> data passed to the task in a memory-safe fashion.
     */
    public <TTaskParam> PerFrameTask<TTaskParam> addPerFrameTask(TTaskParam taskParam, BiConsumer<TTaskParam, Float> task) {
        if (task == null)
            throw new NullPointerException("task");

        return addPerFrameTask(taskParam, (param, taskObj, deltaTime) -> task.accept(param, deltaTime));

    }

    /**
     * Adds a new task which will run every so many frames.
     * @param task the task to run on repeat
     * @return timerTaskObject
     * @param <TTaskParam> data passed to the task in a memory-safe fashion.
     */
    public <TTaskParam> PerFrameTask<TTaskParam> addPerFrameTask(TTaskParam taskParam, TriConsumer<TTaskParam, PerFrameTask<TTaskParam>, Float> task) {
        if (task == null)
            throw new NullPointerException("task");

        PerFrameTask<TTaskParam> nextTask = new PerFrameTask<>(this, task, taskParam);
        this.perFrameTasks.add(nextTask);
        return nextTask;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PerFrameTask<TTaskParam> {
        private final MeshViewFrameTimer parent;
        private final TriConsumer<TTaskParam, PerFrameTask<TTaskParam>, Float> task;
        private final TTaskParam taskParam;
        private boolean cancellationScheduled;

        /**
         * Ticks the task. Assumes the task should be ticked.
         */
        public void run(int taskId, float deltaTime) {
            if (this.cancellationScheduled)
                return;

            try {
                this.task.accept(this.taskParam, this, deltaTime);
            } catch (Throwable th) {
                cancel();
                ILogger logger = this.parent.getLogger();
                Utils.handleError(logger, th, false, "Task %d (%s/%s) in perFrameTasks threw an error while executing, so it has been cancelled!", taskId, this.task, this.taskParam);
            }
        }

        /**
         * Cancel the task so it will not run again.
         */
        public void cancel() {
            this.cancellationScheduled = true;
        }
    }

    public static class MeshViewFixedFrameRateTimer {
        @Getter private final MeshViewFrameTimer frameTimer;
        @Getter private final int framesPerSecond;
        @Getter private final long nanosPerFrame;
        private long lastUpdateNanos = Long.MIN_VALUE;
        private final List<MeshViewFixedFrameRateTimerTask<?>> registeredTasks = new ArrayList<>();
        private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

        private MeshViewFixedFrameRateTimer(MeshViewFrameTimer frameTimer, int framesPerSecond) {
            this.frameTimer = frameTimer;
            this.framesPerSecond = framesPerSecond;
            this.nanosPerFrame = (framesPerSecond > 0) ? (NANOS_PER_SECOND / framesPerSecond) : 1;
        }

        /**
         * Called when a frame has been ticked.
         */
        private void tick(long now) {
            // Handle system time change / overflow.
            if (now < this.lastUpdateNanos) {
                this.frameTimer.getLogger().warning("Did the system time change? The last update for the task list with %d FPS was seen at timestamp %d, but it's currently %d, which is before that time.", this.framesPerSecond, this.lastUpdateNanos, now);
                this.lastUpdateNanos = now; // Make the previous time negative.
            }

            // Count number of frames.
            int frameCount = 0;
            if (this.lastUpdateNanos == Long.MIN_VALUE) {
                // From the default value to the current time.
                this.lastUpdateNanos = now;
                frameCount = 1;
            } else {
                while (now > this.lastUpdateNanos + this.nanosPerFrame) {
                    this.lastUpdateNanos += this.nanosPerFrame;
                    frameCount++;
                }
            }

            // Not enough time has elapsed.
            if (frameCount == 0)
                return;

            // Tick all the tasks currently registered.
            for (int i = 0; i < this.registeredTasks.size(); i++) {
                MeshViewFixedFrameRateTimerTask<?> timerTask = this.registeredTasks.get(i);
                timerTask.tickTaskRunIfReady(i, frameCount);

                // If a task is cancelled or an error occurred, cancel the task.
                if (timerTask.isCancellationScheduled())
                    this.registeredTasks.remove(i--);
            }
        }

        /**
         * Adds a new task which will run every so many frames.
         * @param framesBetweenActivation the number of frames between each activation
         * @param task the task to run on repeat
         * @return timerTaskObject
         * @param <TTaskParam> data passed to the task in a memory-safe fashion.
         */
        public <TTaskParam> MeshViewFixedFrameRateTimerTask<TTaskParam> addTask(int framesBetweenActivation, Runnable task) {
            if (task == null)
                throw new NullPointerException("task");

            return addTask(framesBetweenActivation, null, (frames, param) -> task.run());
        }

        /**
         * Adds a new task which will run every so many frames.
         * @param framesBetweenActivation the number of frames between each activation
         * @param task the task to run on repeat
         * @return timerTaskObject
         * @param <TTaskParam> data passed to the task in a memory-safe fashion.
         */
        public <TTaskParam> MeshViewFixedFrameRateTimerTask<TTaskParam> addTask(int framesBetweenActivation, Consumer<MeshViewFixedFrameRateTimerTask<?>> task) {
            if (task == null)
                throw new NullPointerException("task");

            return addTask(framesBetweenActivation, null, (param, frames) -> task.accept(frames));
        }

        /**
         * Adds a new task which will run every so many frames.
         * @param framesBetweenActivation the number of frames between each activation
         * @param task the task to run on repeat
         * @return timerTaskObject
         * @param <TTaskParam> data passed to the task in a memory-safe fashion.
         */
        public <TTaskParam> MeshViewFixedFrameRateTimerTask<TTaskParam> addTask(int framesBetweenActivation, TTaskParam taskParam, Consumer<TTaskParam> task) {
            if (task == null)
                throw new NullPointerException("task");

            return addTask(framesBetweenActivation, taskParam, (param, frames) -> task.accept(param));

        }

        /**
         * Adds a new task which will run every so many frames.
         * @param framesBetweenActivation the number of frames between each activation
         * @param task the task to run on repeat
         * @return timerTaskObject
         * @param <TTaskParam> data passed to the task in a memory-safe fashion.
         */
        public <TTaskParam> MeshViewFixedFrameRateTimerTask<TTaskParam> addTask(int framesBetweenActivation, TTaskParam taskParam, BiConsumer<TTaskParam, MeshViewFixedFrameRateTimerTask<TTaskParam>> task) {
            if (task == null)
                throw new NullPointerException("task");
            if (framesBetweenActivation <= 0)
                throw new IllegalArgumentException("Cannot have a frames per activation less than zero! (Got: " + framesBetweenActivation + ")");

            MeshViewFixedFrameRateTimerTask<TTaskParam> nextTask = new MeshViewFixedFrameRateTimerTask<>(this, framesBetweenActivation, task, taskParam);
            this.registeredTasks.add(nextTask);
            return nextTask;
        }
    }

    /**
     * Gets the logger to write debug information with.
     */
    public ILogger getLogger() {
        return this.controller != null ? this.controller.getLogger() : ClassNameLogger.getLogger(null, getClass());
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MeshViewFixedFrameRateTimerTask<TTaskParam> {
        private final MeshViewFixedFrameRateTimer fixedFrameRateTimer;
        private final int framesBetweenActivation;
        private final BiConsumer<TTaskParam, MeshViewFixedFrameRateTimerTask<TTaskParam>> task;
        private final TTaskParam taskParam;
        private boolean cancellationScheduled;
        private int framesUntilNextActivation;
        private int deltaFrames;

        /**
         * Ticks the task. Assumes the task should be ticked.
         */
        private void tickTaskRunIfReady(int taskId, int deltaFrames) {
            this.framesUntilNextActivation -= deltaFrames;
            this.deltaFrames = 0;
            while (this.framesUntilNextActivation <= 0) {
                this.deltaFrames++;
                this.framesUntilNextActivation += this.framesBetweenActivation;
            }

            if (this.deltaFrames == 0)
                return; // Not enough time has passed.

            try {
                this.task.accept(this.taskParam, this);
            } catch (Throwable th) {
                cancel();
                ILogger logger = this.fixedFrameRateTimer.getFrameTimer().getLogger();
                Utils.handleError(logger, th, false, "Task %d (%s/%s) in TaskList[fps=%d] threw an error while executing, so it has been cancelled!", taskId, this.task, this.taskParam, this.fixedFrameRateTimer.getFramesPerSecond());
            }
        }

        /**
         * Cancel the task so it will not run again.
         */
        public void cancel() {
            this.cancellationScheduled = true;
        }
    }
}