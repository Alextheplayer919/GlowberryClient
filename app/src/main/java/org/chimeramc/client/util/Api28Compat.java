package org.chimeramc.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The handful of Java 11/17 library conveniences this project must not call directly.
 *
 * <p>The launcher ships with {@code minSdkVersion 28} and <b>core-library desugaring is off</b>, so
 * a call to a method that only exists in a newer {@code java.base} is not rewritten at build time.
 * It compiles against the API 36 SDK and then throws {@link NoSuchMethodError} on Android 9-12.
 *
 * <p>That failure mode is unusually nasty because {@code NoSuchMethodError} is an {@link Error},
 * not an {@link Exception}: a {@code catch (Exception)} around the call does <em>not</em> contain
 * it, so it unwinds the whole caller. That is exactly how {@code InputStream.readAllBytes()} in the
 * signature-rules reader killed the launch on Android 12 with "Launch failed" the instant the
 * preloader started loading.
 *
 * <p>Everything here is written against API 1-26 APIs only, and {@link #isBlank(String)} matches
 * {@link String#isBlank()} exactly (whitespace is defined by {@link Character#isWhitespace}, not by
 * a hand-rolled space/tab check) so swapping a call site cannot change behaviour.
 *
 * <p>{@code Api28CompatTest} is the guard: it scans the real source tree for the banned calls, so
 * the same mistake cannot be reintroduced. Prefer these helpers over an {@code SDK_INT} branch —
 * a branch on a library method is dead code on every device below the level, and the helper is
 * testable without a device.
 */
public final class Api28Compat {

    /**
     * {@link String#isBlank()} (API 33): true when the string is empty or contains only whitespace
     * as defined by {@link Character#isWhitespace}.
     */
    public static boolean isBlank(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** The negation of {@link #isBlank(String)}, so call sites read naturally. */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * {@link List#of(Object[])} (API 33) for the shapes used here: a small, fixed, ordered list
     * that rejects null elements and cannot be modified.
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        List<T> out = new ArrayList<>(elements.length);
        for (T element : elements) {
            out.add(requireNonNull(element));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * {@link Set#of(Object[])} (API 33): a small, fixed set. Insertion order is kept (the API 33
     * method does not promise an order, so any order is compatible; keeping the declaration order
     * makes a failure message readable).
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        Set<T> out = new LinkedHashSet<>(Math.max(4, elements.length * 2));
        for (T element : elements) {
            out.add(requireNonNull(element));
        }
        return Collections.unmodifiableSet(out);
    }

    private static <T> T requireNonNull(T value) {
        if (value == null) {
            throw new NullPointerException("element cannot be null");
        }
        return value;
    }

    private Api28Compat() {}
}
