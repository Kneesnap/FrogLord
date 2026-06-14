package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKey;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A 2D canvas-based timeline panel for visualizing and editing animation keyframes.
 * Displays each animated bone as a horizontal track, with keyframe markers that can
 * be selected, dragged, added, and deleted. A scrubber bar controls the 3D view
 * playback position.
 * Created by Kneesnap on 6/12/2026.
 */
public class GreatQuestAnimationTimelinePanel extends Pane {
    private final GreatQuestAnimationEditor editor;
    private final Canvas canvas;
    private final ScrollBar horizontalScrollBar;
    private final ScrollBar verticalScrollBar;

    // Data state
    @Getter private kcCResourceTrack animation;
    @Getter private kcCResourceSkeleton skeleton;
    private final List<kcTrack> orderedTracks = new ArrayList<>();
    private final List<String> trackLabels = new ArrayList<>();

    // Selection / interaction state
    @Getter private kcTrack selectedTrack;
    @Getter private kcTrackKey<?> selectedKeyframe;
    @Getter private double scrubberTick;
    @Getter private boolean draggingScrubber;
    private boolean draggingKeyframe;
    private double keyframeDragStartCanvasX;
    private int keyframeDragOriginalTick;

    // View state
    private double pixelsPerTick;
    private double scrollOffsetTicks;
    private double scrollOffsetY;

    // Context-menu tracking
    private kcTrack contextMenuTrack;
    private double contextMenuTick;

    // ---- Layout constants ----
    public static final double LABEL_WIDTH = 130.0;
    public static final double ROW_HEIGHT = 24.0;
    public static final double RULER_HEIGHT = 20.0;
    public static final double SCROLLBAR_HEIGHT = 16.0;
    public static final double KEYFRAME_HALF_SIZE = 5.5;
    public static final double SCRUBBER_HANDLE_HALF_W = 5.0;
    public static final double SCRUBBER_HANDLE_H = 10.0;
    public static final double MIN_PIXELS_PER_TICK = 0.0001;
    public static final double MAX_PIXELS_PER_TICK = 0.5;
    /** Default zoom: 80 pixels per second of animation. */
    private static final double DEFAULT_PIXELS_PER_SECOND = 80.0;

    // ---- Colours ----
    private static final Color BG_COLOR = Color.web("#1e1e1e");
    private static final Color LABEL_BG_COLOR = Color.web("#252525");
    private static final Color RULER_BG_COLOR = Color.web("#2d2d2d");
    private static final Color ROW_COLOR_ODD = Color.web("#2a2a2a");
    private static final Color ROW_COLOR_EVEN = Color.web("#272727");
    private static final Color LABEL_TEXT_COLOR = Color.web("#cccccc");
    private static final Color RULER_TEXT_COLOR = Color.web("#aaaaaa");
    private static final Color RULER_TICK_COLOR = Color.web("#555555");
    private static final Color DIVIDER_COLOR = Color.web("#383838");
    private static final Color KEYFRAME_COLOR = Color.web("#4AADAD");
    private static final Color SELECTED_KEYFRAME_COLOR = Color.YELLOW;
    private static final Color SCRUBBER_COLOR = Color.WHITE;
    private static final Color SCRUBBER_HEAD_COLOR = Color.web("#dddddd");
    private static final Color EMPTY_TEXT_COLOR = Color.web("#555555");
    private static final Font LABEL_FONT = Font.font("System", FontWeight.NORMAL, 11);
    private static final Font LABEL_FONT_BOLD = Font.font("System", FontWeight.BOLD, 11);
    private static final Font RULER_FONT = Font.font("System", 9);

    public GreatQuestAnimationTimelinePanel(GreatQuestAnimationEditor editor) {
        this.editor = editor;
        this.pixelsPerTick = DEFAULT_PIXELS_PER_SECOND / 4800.0;

        // Canvas — sized and positioned via layoutChildren()
        this.canvas = new Canvas();

        // Horizontal scrollbar at the very bottom — also sized/positioned via layoutChildren()
        this.horizontalScrollBar = new ScrollBar();
        this.horizontalScrollBar.setOrientation(Orientation.HORIZONTAL);
        this.horizontalScrollBar.setMin(0);
        this.horizontalScrollBar.setMax(0);
        this.horizontalScrollBar.setValue(0);
        this.horizontalScrollBar.setUnitIncrement(4800.0 / 10.0);  // 0.1 s per arrow click
        this.horizontalScrollBar.setBlockIncrement(4800.0);          // 1 s per page

        this.horizontalScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            this.scrollOffsetTicks = newVal.doubleValue();
            redraw();
        });

        // Vertical scrollbar — shown on the right when an animation is loaded
        this.verticalScrollBar = new ScrollBar();
        this.verticalScrollBar.setOrientation(Orientation.VERTICAL);
        this.verticalScrollBar.setMin(0);
        this.verticalScrollBar.setMax(0);
        this.verticalScrollBar.setValue(0);
        this.verticalScrollBar.setUnitIncrement(ROW_HEIGHT);
        this.verticalScrollBar.setBlockIncrement(ROW_HEIGHT * 3);
        this.verticalScrollBar.setVisible(false);

        this.verticalScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            this.scrollOffsetY = newVal.doubleValue();
            redraw();
        });

        // Canvas mouse events
        this.canvas.setOnMousePressed(this::onMousePressed);
        this.canvas.setOnMouseDragged(this::onMouseDragged);
        this.canvas.setOnMouseReleased(this::onMouseReleased);
        this.canvas.setOnScroll(this::onScroll);

        getChildren().addAll(this.canvas, this.horizontalScrollBar, this.verticalScrollBar);

        // Allow the pane to shrink freely so no spurious window scrollbar appears.
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Dark background so the pane looks the same before the canvas is drawn
        setStyle("-fx-background-color: #1e1e1e;");

        // Prevent mouse/scroll events from bubbling up to the 3D view's camera handlers.
        // The Canvas handles all interactions internally; anything it doesn't consume stops here.
        setOnMousePressed(MouseEvent::consume);
        setOnMouseDragged(MouseEvent::consume);
        setOnMouseReleased(MouseEvent::consume);
        setOnMouseClicked(MouseEvent::consume);
        setOnScroll(ScrollEvent::consume);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();

        boolean hasAnimation = (this.animation != null);
        double vsbW = hasAnimation ? SCROLLBAR_HEIGHT : 0; // vertical scrollbar width (16 px)

        // Horizontal scrollbar stays invisible; only used for internal state tracking.
        this.horizontalScrollBar.setVisible(false);

        // Canvas occupies the full height but leaves room for the vertical scrollbar.
        double canvasW = Math.max(0, w - vsbW);
        this.canvas.setWidth(canvasW);
        this.canvas.setHeight(h);
        this.canvas.setLayoutX(0);
        this.canvas.setLayoutY(0);

        // Vertical scrollbar on the right edge.
        this.verticalScrollBar.setVisible(hasAnimation);
        if (hasAnimation) {
            this.verticalScrollBar.resize(vsbW, h);
            this.verticalScrollBar.setLayoutX(canvasW);
            this.verticalScrollBar.setLayoutY(0);
        }

        updateScrollBar();
        updateVerticalScrollBar();
        redraw();
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Assigns the animation and skeleton to display. Resets zoom/scroll to fit.
     */
    public void setAnimation(kcCResourceTrack animation, kcCResourceSkeleton skeleton) {
        this.animation = animation;
        this.skeleton = skeleton;
        this.selectedTrack = null;
        this.selectedKeyframe = null;
        this.scrollOffsetY = 0;
        this.verticalScrollBar.setValue(0);
        rebuildTrackList();
        fitZoom();
        requestLayout(); // Triggers vertical scrollbar show/hide.
        redraw();
    }

    /**
     * Updates the scrubber position without triggering a scrubber-moved callback.
     * Called each frame by the editor to keep the display in sync with the mesh.
     */
    public void setScrubberTick(double tick) {
        this.scrubberTick = tick;
        autoScrollToScrubber();
        redraw();
    }

    /**
     * Programmatically select (or deselect) a keyframe.
     */
    public void selectKeyframe(kcTrack track, kcTrackKey<?> key) {
        this.selectedTrack = track;
        this.selectedKeyframe = key;
        redraw();
    }

    /**
     * Called by the editor after the animation's track/key list has been structurally modified,
     * so the display can be rebuilt.
     */
    public void onAnimationModified() {
        rebuildTrackList();
        updateScrollBar();
        updateVerticalScrollBar();
        redraw();
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private void rebuildTrackList() {
        this.orderedTracks.clear();
        this.trackLabels.clear();
        if (this.animation == null)
            return;

        List<kcTrack> sorted = new ArrayList<>(this.animation.getTracks());
        sorted.sort(Comparator.comparingInt(kcTrack::getTag));

        for (kcTrack track : sorted) {
            this.orderedTracks.add(track);
            this.trackLabels.add(buildTrackLabel(track));
        }
    }

    private String buildTrackLabel(kcTrack track) {
        kcNode node = this.skeleton != null ? this.skeleton.getNodeByTag(track.getTag()) : null;
        String name = (node != null) ? node.getName() : "Tag " + track.getTag();
        return name + " [" + abbreviateType(track.getTrackControlType()) + "]";
    }

    private static String abbreviateType(kcControlType type) {
        switch (type) {
            case LINEAR_POSITION: return "LP";
            case LINEAR_ROTATION: return "LR";
            case LINEAR_SCALE: return "LS";
            case TCB_POSITION: return "TP";
            case TCB_ROTATION: return "TR";
            case TCB_SCALE: return "TS";
            case BEZIER_POSITION: return "BP";
            case BEZIER_SCALE: return "BS";
            default: return type.name();
        }
    }

    /** Zoom the timeline so the full animation fits the visible area (with a small margin). */
    private void fitZoom() {
        this.scrollOffsetTicks = 0;
        if (this.animation == null || this.animation.getMaxTick() <= 0) {
            this.pixelsPerTick = DEFAULT_PIXELS_PER_SECOND / 4800.0;
        } else {
            double timelineWidth = Math.max(1.0, getWidth() - LABEL_WIDTH);
            int maxTick = this.animation.getMaxTick();
            double candidate = timelineWidth / (maxTick * 1.15);
            this.pixelsPerTick = Math.max(MIN_PIXELS_PER_TICK, Math.min(MAX_PIXELS_PER_TICK, candidate));
        }
        updateScrollBar();
    }

    private void updateScrollBar() {
        if (this.animation == null) {
            this.horizontalScrollBar.setMax(0);
            this.horizontalScrollBar.setVisibleAmount(0);
            return;
        }
        int maxTick = Math.max(this.animation.getMaxTick(), 4800);
        double timelineWidth = Math.max(1.0, getWidth() - LABEL_WIDTH);
        double visibleTicks = timelineWidth / this.pixelsPerTick;
        double maxScroll = Math.max(0.0, maxTick * 1.1 - visibleTicks / 2.0);
        this.horizontalScrollBar.setMax(maxScroll);
        this.horizontalScrollBar.setVisibleAmount(visibleTicks);
        // Clamp current scroll value
        double clamped = Math.max(0.0, Math.min(this.scrollOffsetTicks, maxScroll));
        if (clamped != this.horizontalScrollBar.getValue())
            this.horizontalScrollBar.setValue(clamped);
    }

    private void updateVerticalScrollBar() {
        if (this.animation == null) {
            this.verticalScrollBar.setMax(0);
            this.verticalScrollBar.setVisibleAmount(0);
            return;
        }
        double trackAreaH = Math.max(1.0, this.canvas.getHeight() - RULER_HEIGHT);
        double totalContentH = this.orderedTracks.size() * ROW_HEIGHT;
        double maxScroll = Math.max(0.0, totalContentH - trackAreaH);
        this.verticalScrollBar.setMax(maxScroll);
        this.verticalScrollBar.setVisibleAmount(trackAreaH);
        double clamped = Math.max(0.0, Math.min(this.scrollOffsetY, maxScroll));
        if (clamped != this.verticalScrollBar.getValue())
            this.verticalScrollBar.setValue(clamped);
    }

    /** Scroll so the scrubber is visible if it has wandered off-screen during playback. */
    private void autoScrollToScrubber() {
        if (getWidth() <= LABEL_WIDTH) return;
        double timelineWidth = getWidth() - LABEL_WIDTH;
        double visibleTicks = timelineWidth / this.pixelsPerTick;

        if (this.scrubberTick < this.scrollOffsetTicks) {
            double newScroll = Math.max(0, this.scrubberTick - visibleTicks * 0.05);
            this.horizontalScrollBar.setValue(newScroll);
        } else if (this.scrubberTick > this.scrollOffsetTicks + visibleTicks) {
            double newScroll = this.scrubberTick - visibleTicks * 0.95;
            this.horizontalScrollBar.setValue(Math.max(0, newScroll));
        }
    }

    // =========================================================================
    // Coordinate helpers
    // =========================================================================

    /** Convert a tick value to a canvas X coordinate. */
    private double tickToX(double tick) {
        return LABEL_WIDTH + (tick - this.scrollOffsetTicks) * this.pixelsPerTick;
    }

    /** Convert a canvas X coordinate to a tick value. */
    private double xToTick(double x) {
        return this.scrollOffsetTicks + (x - LABEL_WIDTH) / this.pixelsPerTick;
    }

    /** Returns the track index for a given canvas Y, or -1 if in the ruler or out of range. */
    private int getTrackIndexAt(double y) {
        if (y < RULER_HEIGHT) return -1;
        int idx = (int) ((y - RULER_HEIGHT + this.scrollOffsetY) / ROW_HEIGHT);
        return (idx >= 0 && idx < this.orderedTracks.size()) ? idx : -1;
    }

    /** Returns the keyframe under the given canvas X for a track, or null if none. */
    private kcTrackKey<?> getKeyframeAt(kcTrack track, double x) {
        for (kcTrackKey<?> key : track.getKeyList()) {
            double kx = tickToX(key.getTick());
            if (Math.abs(kx - x) <= KEYFRAME_HALF_SIZE + 2.0)
                return key;
        }
        return null;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    /** Fully redraws the timeline onto the canvas. */
    public void redraw() {
        double w = this.canvas.getWidth();
        double h = this.canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = this.canvas.getGraphicsContext2D();

        // Background
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, w, h);

        if (this.animation == null || this.orderedTracks.isEmpty()) {
            drawEmptyMessage(gc, w, h);
            return;
        }

        // Label column background
        gc.setFill(LABEL_BG_COLOR);
        gc.fillRect(0, 0, LABEL_WIDTH, h);

        // Draw track rows and keyframes clipped to the area below the ruler so that
        // partially-visible rows at the top don't bleed into the ruler.
        gc.save();
        gc.beginPath();
        gc.rect(0, RULER_HEIGHT, w, h - RULER_HEIGHT);
        gc.clip();
        drawTrackRows(gc, w, h);
        drawKeyframes(gc, w);
        gc.restore();

        // Draw ruler on top (unclipped so it covers any row overflow at the top edge).
        drawRuler(gc, w);

        // Draw scrubber on top of everything
        drawScrubber(gc, w, h);

        // Column separator
        gc.setStroke(DIVIDER_COLOR.darker());
        gc.setLineWidth(1.0);
        gc.strokeLine(LABEL_WIDTH, 0, LABEL_WIDTH, h);
    }

    private void drawEmptyMessage(GraphicsContext gc, double w, double h) {
        gc.setFill(EMPTY_TEXT_COLOR);
        gc.setFont(LABEL_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Select an animation to edit", w / 2.0, h / 2.0 + 4.0);
    }

    private void drawTrackRows(GraphicsContext gc, double w, double h) {
        for (int i = 0; i < this.orderedTracks.size(); i++) {
            double rowY = RULER_HEIGHT + i * ROW_HEIGHT - this.scrollOffsetY;
            if (rowY + ROW_HEIGHT <= RULER_HEIGHT) continue; // row is above the clip boundary
            if (rowY >= h) break;

            // Row background
            gc.setFill((i % 2 == 0) ? ROW_COLOR_ODD : ROW_COLOR_EVEN);
            gc.fillRect(LABEL_WIDTH, rowY, w - LABEL_WIDTH, ROW_HEIGHT);

            // Label background divider line
            gc.setFill(LABEL_BG_COLOR);
            gc.fillRect(0, rowY, LABEL_WIDTH, ROW_HEIGHT);

            // Label text (bold when this track is selected)
            boolean isSelected = this.orderedTracks.get(i) == this.selectedTrack;
            gc.setFont(isSelected ? LABEL_FONT_BOLD : LABEL_FONT);
            gc.setFill(LABEL_TEXT_COLOR);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(this.trackLabels.get(i), 4.0, rowY + ROW_HEIGHT - 6.0, LABEL_WIDTH - 8.0);

            // Row bottom divider
            gc.setStroke(DIVIDER_COLOR);
            gc.setLineWidth(0.5);
            gc.strokeLine(0, rowY + ROW_HEIGHT, w, rowY + ROW_HEIGHT);
        }
    }

    private void drawRuler(GraphicsContext gc, double w) {
        gc.setFill(RULER_BG_COLOR);
        gc.fillRect(LABEL_WIDTH, 0, w - LABEL_WIDTH, RULER_HEIGHT);

        double timelineWidth = w - LABEL_WIDTH;
        double visibleTicks = timelineWidth / this.pixelsPerTick;

        // Choose a tick interval that gives roughly 80-100px between labels
        double targetTickInterval = 90.0 / this.pixelsPerTick;
        double niceIntervalTicks = niceTickInterval(targetTickInterval);

        double firstTick = Math.ceil(this.scrollOffsetTicks / niceIntervalTicks) * niceIntervalTicks;
        gc.setFont(RULER_FONT);
        gc.setFill(RULER_TEXT_COLOR);
        gc.setStroke(RULER_TICK_COLOR);
        gc.setLineWidth(1.0);
        gc.setTextAlign(TextAlignment.CENTER);

        for (double t = firstTick; t <= this.scrollOffsetTicks + visibleTicks + niceIntervalTicks; t += niceIntervalTicks) {
            double x = tickToX(t);
            if (x < LABEL_WIDTH - 1) continue;
            if (x > w + 1) break;
            gc.strokeLine(x, RULER_HEIGHT - 5.0, x, RULER_HEIGHT);
            gc.fillText(formatTick((long) t), x, RULER_HEIGHT - 7.0, 64.0);
        }
    }

    private void drawKeyframes(GraphicsContext gc, double w) {
        double h = this.canvas.getHeight();
        for (int i = 0; i < this.orderedTracks.size(); i++) {
            double rowY = RULER_HEIGHT + i * ROW_HEIGHT - this.scrollOffsetY;
            if (rowY + ROW_HEIGHT <= RULER_HEIGHT) continue;
            if (rowY >= h) break;
            kcTrack track = this.orderedTracks.get(i);
            double centerY = rowY + ROW_HEIGHT / 2.0;

            for (kcTrackKey<?> key : track.getKeyList()) {
                double kx = tickToX(key.getTick());
                // Skip off-screen keyframes
                if (kx < LABEL_WIDTH - KEYFRAME_HALF_SIZE - 1 || kx > w + KEYFRAME_HALF_SIZE + 1)
                    continue;

                boolean selected = (key == this.selectedKeyframe);
                drawKeyframeDiamond(gc, kx, centerY, selected);
            }
        }
    }

    private void drawKeyframeDiamond(GraphicsContext gc, double cx, double cy, boolean selected) {
        double r = KEYFRAME_HALF_SIZE;
        double[] px = {cx,      cx + r,  cx,      cx - r};
        double[] py = {cy - r,  cy,      cy + r,  cy};

        gc.setFill(selected ? SELECTED_KEYFRAME_COLOR : KEYFRAME_COLOR);
        gc.fillPolygon(px, py, 4);
        gc.setStroke(selected ? Color.ORANGE : KEYFRAME_COLOR.darker().darker());
        gc.setLineWidth(1.0);
        gc.strokePolygon(px, py, 4);
    }

    private void drawScrubber(GraphicsContext gc, double w, double h) {
        double sx = tickToX(this.scrubberTick);
        if (sx < LABEL_WIDTH || sx > w) return;

        // Vertical line
        gc.setStroke(SCRUBBER_COLOR);
        gc.setLineWidth(1.5);
        gc.strokeLine(sx, RULER_HEIGHT, sx, h);

        // Triangle handle at the top (pointing down)
        double[] hx = {sx - SCRUBBER_HANDLE_HALF_W, sx + SCRUBBER_HANDLE_HALF_W, sx};
        double[] hy = {0.0, 0.0, SCRUBBER_HANDLE_H};
        gc.setFill(SCRUBBER_HEAD_COLOR);
        gc.fillPolygon(hx, hy, 3);
        gc.setStroke(SCRUBBER_COLOR);
        gc.setLineWidth(1.0);
        gc.strokePolygon(hx, hy, 3);
    }

    // =========================================================================
    // Time ruler formatting
    // =========================================================================

    private static final double TICKS_PER_SECOND = 4800.0;

    private static double niceTickInterval(double targetTicks) {
        // Nice second-based values, converted to ticks
        double[] secondIntervals = {0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0, 60.0};
        for (double si : secondIntervals) {
            double ti = si * TICKS_PER_SECOND;
            if (ti >= targetTicks)
                return ti;
        }
        return 60.0 * TICKS_PER_SECOND;
    }

    private static String formatTick(long tick) {
        double seconds = tick / TICKS_PER_SECOND;
        if (seconds < 10.0)
            return String.format("%.2fs", seconds);
        else
            return String.format("%.1fs", seconds);
    }

    // =========================================================================
    // Mouse events
    // =========================================================================

    private void onMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        if (event.getButton() == MouseButton.PRIMARY) {
            // --- Check scrubber handle ---
            double sx = tickToX(this.scrubberTick);
            boolean onScrubberHandle = (Math.abs(x - sx) <= SCRUBBER_HANDLE_HALF_W + 2.0) && (y <= SCRUBBER_HANDLE_H + 2.0);
            boolean inRulerArea = (y < RULER_HEIGHT) && (x > LABEL_WIDTH);

            if (onScrubberHandle || inRulerArea) {
                this.draggingScrubber = true;
                this.editor.onTimelineScrubStart();
                double newTick = Math.max(0.0, xToTick(x));
                setScrubberTick(newTick);
                this.editor.onTimelineScrubberMoved(newTick);
                return;
            }

            // --- Click on bone label (left column) → select that track's bone ---
            int labelTrackIdx = getTrackIndexAt(y);
            if (labelTrackIdx >= 0 && x <= LABEL_WIDTH) {
                kcTrack track = this.orderedTracks.get(labelTrackIdx);
                selectKeyframe(track, null);
                this.editor.onBoneLabelClicked(track);
                return;
            }

            // --- Check keyframe hit ---
            int trackIdx = getTrackIndexAt(y);
            if (trackIdx >= 0 && x > LABEL_WIDTH) {
                kcTrack track = this.orderedTracks.get(trackIdx);
                kcTrackKey<?> hitKey = getKeyframeAt(track, x);
                if (hitKey != null) {
                    selectKeyframe(track, hitKey);
                    this.editor.onKeyframeSelected(track, hitKey);
                    // Begin drag
                    this.draggingKeyframe = true;
                    this.keyframeDragStartCanvasX = x;
                    this.keyframeDragOriginalTick = hitKey.getTick();
                } else {
                    // Click on empty space in a track → deselect
                    selectKeyframe(null, null);
                    this.editor.onKeyframeSelected(null, null);
                }
            }

        } else if (event.getButton() == MouseButton.SECONDARY) {
            // Right-click: show context menu
            int trackIdx = getTrackIndexAt(y);
            if (trackIdx >= 0 && x > LABEL_WIDTH) {
                this.contextMenuTrack = this.orderedTracks.get(trackIdx);
                this.contextMenuTick = Math.max(0.0, xToTick(x));
                kcTrackKey<?> hitKey = getKeyframeAt(this.contextMenuTrack, x);
                showContextMenu(event, hitKey);
            }
        }
    }

    private void onMouseDragged(MouseEvent event) {
        double x = event.getX();

        if (this.draggingScrubber) {
            double newTick = Math.max(0.0, xToTick(x));
            setScrubberTick(newTick);
            this.editor.onTimelineScrubberMoved(newTick);

        } else if (this.draggingKeyframe && this.selectedKeyframe != null && this.selectedTrack != null) {
            double deltaTicks = (x - this.keyframeDragStartCanvasX) / this.pixelsPerTick;
            int newTick = Math.max(0, (int) Math.round(this.keyframeDragOriginalTick + deltaTicks));
            if (newTick != this.selectedKeyframe.getTick())
                this.editor.onKeyframeMoveRequested(this.selectedTrack, this.selectedKeyframe, newTick);
        }
    }

    private void onMouseReleased(MouseEvent event) {
        if (this.draggingScrubber) {
            this.draggingScrubber = false;
            this.editor.onTimelineScrubEnd();
        }
        if (this.draggingKeyframe) {
            this.draggingKeyframe = false;
        }
    }

    private void onScroll(ScrollEvent event) {
        if (event.isControlDown()) {
            // Ctrl+Scroll → zoom time axis
            double factor = (event.getDeltaY() > 0) ? 1.2 : (1.0 / 1.2);
            double tickUnderMouse = xToTick(event.getX());
            this.pixelsPerTick = Math.max(MIN_PIXELS_PER_TICK, Math.min(MAX_PIXELS_PER_TICK, this.pixelsPerTick * factor));
            // Keep the tick under the cursor at the same screen position
            this.scrollOffsetTicks = Math.max(0.0, tickUnderMouse - (event.getX() - LABEL_WIDTH) / this.pixelsPerTick);
            updateScrollBar();
            redraw();
        } else if (event.isShiftDown() || Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY())) {
            // Shift+Scroll or horizontal trackpad swipe → pan time axis
            double delta = Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY()) ? event.getDeltaX() : event.getDeltaY();
            double deltaTicks = -delta / this.pixelsPerTick * 0.5;
            double newScroll = Math.max(0.0, this.scrollOffsetTicks + deltaTicks);
            this.horizontalScrollBar.setValue(Math.min(newScroll, this.horizontalScrollBar.getMax()));
        } else {
            // Plain vertical scroll → scroll track rows
            double newScrollY = Math.max(0.0, Math.min(this.scrollOffsetY - event.getDeltaY(), this.verticalScrollBar.getMax()));
            this.verticalScrollBar.setValue(newScrollY);
        }
        event.consume();
    }

    private void showContextMenu(MouseEvent event, kcTrackKey<?> hitKey) {
        ContextMenu menu = new ContextMenu();

        if (hitKey != null) {
            kcTrackKey<?> keyRef = hitKey;
            kcTrack trackRef = this.contextMenuTrack;
            MenuItem deleteItem = new MenuItem("Delete Keyframe");
            deleteItem.setOnAction(e -> this.editor.onKeyframeDeleteRequested(trackRef, keyRef));
            menu.getItems().add(deleteItem);
        } else {
            int targetTick = (int) this.contextMenuTick;
            kcTrack trackRef = this.contextMenuTrack;
            MenuItem addItem = new MenuItem("Add Keyframe at " + formatTick(targetTick));
            addItem.setOnAction(e -> this.editor.onKeyframeAddRequested(trackRef, targetTick));
            menu.getItems().add(addItem);
        }

        menu.show(this.canvas, event.getScreenX(), event.getScreenY());
    }
}
