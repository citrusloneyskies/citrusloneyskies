package com.tokyoghoul.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crash guard. Every per-tick, per-frame and per-event entry point of the mod runs inside one of these
 * guards: an unexpected error is logged once (with its stack trace) and that call is skipped, instead of
 * taking the whole game or server down with it.
 */
public final class Safe {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static Field poseDeque;
    private static boolean poseLookupDone;

    private Safe() {
    }

    /** Log a suppressed error; repeats from the same place are silenced so a broken frame can't flood the log. */
    /** Total errors caught so far (GameTests assert this stays unchanged). */
    public static final java.util.concurrent.atomic.AtomicInteger ERRORS = new java.util.concurrent.atomic.AtomicInteger();

    public static void report(String where, Throwable t) {
        ERRORS.incrementAndGet();
        StackTraceElement[] st = t.getStackTrace();
        String at = st.length > 0 ? st[0].toString() : "";
        if (REPORTED.size() < 500 && REPORTED.add(where + "|" + t.getClass().getName() + "|" + at)) {
            LOG.error("[Tokyo Ghoul] Error in {} was caught and skipped (repeats are silenced). Please report it:", where, t);
        }
    }

    public static void run(String where, Runnable r) {
        try {
            r.run();
        } catch (Exception | LinkageError | StackOverflowError | AssertionError e) {
            report(where, e);
        }
    }

    /** How deep the pose stack is, so a failed render can unwind what it pushed. */
    public static int depth(PoseStack pose) {
        Deque<?> d = deque(pose);
        return d == null ? -1 : d.size();
    }

    /** Pop everything pushed since {@code depth} was taken. */
    public static void restore(PoseStack pose, int depth) {
        Deque<?> d = deque(pose);
        if (d == null || depth < 1) return;
        try {
            while (d.size() > depth) pose.popPose();
        } catch (Exception ignored) {
        }
    }

    private static Deque<?> deque(PoseStack pose) {
        if (pose == null) return null;
        if (!poseLookupDone) {
            poseLookupDone = true;
            for (Field f : PoseStack.class.getDeclaredFields()) {
                if (Deque.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        poseDeque = f;
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
        }
        try {
            return poseDeque == null ? null : (Deque<?>) poseDeque.get(pose);
        } catch (Exception e) {
            return null;
        }
    }
}
