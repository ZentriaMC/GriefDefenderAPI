/*
 * This file is part of GriefDefenderAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.api.event;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A cause represents the reason or initiator of an event.
 *
 * <p>For example, if a block of sand is placed where it drops, the block of
 * sand would create a falling sand entity, which then would place another block
 * of sand. The block place event for the final block of sand would have the
 * cause chain of the block of sand -&gt; falling sand entity.</p>
 *
 * <p>It is not possible to accurately describe the chain of causes in all
 * scenarios so a best effort approach is generally acceptable. For example, a
 * player might press a lever, activating a complex Redstone circuit, which
 * would then launch TNT and cause the destruction of some blocks, but tracing
 * this event would be too complicated and thus may not be attempted.</p>
 */
@SuppressWarnings("unchecked")
public final class EventCause implements Iterable<Object> {

    /**
     * Creates a new {@link Builder} to make a new {@link EventCause}.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new cause with the specified event context and cause.
     *
     * @param cause The direct object cause
     * @return The constructed cause
     */
    public static EventCause of(Object cause) {
        checkNotNull(cause, "Cause cannot be null!");
        return new EventCause(cause);
    }

    /**
     * Constructs a new cause with the specified event context and causes.
     *
     * @param cause The direct object cause
     * @param causes Other associated causes
     * @return The built cause
     */
    public static EventCause of(Object cause, Object... causes) {
        if (causes.length == 0) {
            return of(cause);
        }

        CopyOnWriteArrayList<Object> causesList = new CopyOnWriteArrayList<>(causes);
        return builder().appendAll(causesList).insert(0, cause).build();
    }

    /**
     * Constructs a new cause with the specified event context and causes.
     *
     * @param iterable The associated causes
     * @return The built cause
     */
    public static EventCause of(Iterable<Object> iterable) {
        // Fast path
        if (iterable instanceof EventCause) {
            return new EventCause(new CopyOnWriteArrayList<>(((EventCause) iterable).cause));
        }

        // Another fast path
        if (iterable instanceof CopyOnWriteArrayList || iterable instanceof ArrayList) {
            return new EventCause(new CopyOnWriteArrayList<>((Collection<Object>) iterable));
        }

        Builder builder = builder();
        for (Object cause : iterable) {
            builder.append(cause);
        }
        return builder.build();
    }

    final CopyOnWriteArrayList<Object> cause;
    private final List<Object> immutableCauses;

    /**
     * Constructs a new cause.
     *
     * @param cause The causes list
     */
    EventCause(CopyOnWriteArrayList<Object> cause) {
        this.cause = cause;
        this.immutableCauses = Collections.unmodifiableList(cause);
    }

    /**
     * Constructs a new cause.
     *
     * @param cause The cause
     */
    EventCause(Object cause) {
        this(new CopyOnWriteArrayList<>());
        this.cause.add(cause);
    }

    /**
     * Gets the root {@link Object} of this cause.
     *
     * @return The root object cause for this cause
     */
    public Object root() {
        return this.cause.get(0);
    }

    /**
     * Gets the first <code>T</code> object of this {@link EventCause}, if available.
     *
     * @param target The class of the target type
     * @param <T> The type of object being queried for
     * @return The first element of the type, if available
     */
    public <T> Optional<T> first(Class<T> target) {
        for (Object aCause : this.cause) {
            if (target.isInstance(aCause)) {
                return Optional.of((T) aCause);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the last object instance of the {@link Class} of type
     * <code>T</code>.
     *
     * @param target The class of the target type
     * @param <T> The type of object being queried for
     * @return The last element of the type, if available
     */
    public <T> Optional<T> last(Class<T> target) {
        ListIterator<Object> iterator = this.cause.listIterator(this.cause.size());
        while (iterator.hasPrevious()) {
            Object c = iterator.previous();
            if (target.isInstance(c)) {
                return Optional.of((T) c);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the object immediately before the object that is an instance of the
     * {@link Class} passed in.
     *
     * @param clazz The class of the object
     * @return The object
     */
    public Optional<?> before(Class<?> clazz) {
        checkArgument(clazz != null, "The provided class cannot be null!");
        if (this.cause.size() == 1) {
            return Optional.empty();
        }

        ListIterator<Object> iterator = this.cause.listIterator();
        while (iterator.hasNext()) {
            Object c = iterator.next();
            if (clazz.isInstance(c) && iterator.hasPrevious()) {
                return Optional.of(iterator.previous());
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the object immediately after the object that is an instance of the
     * {@link Class} passed in.
     *
     * @param clazz The class to type check
     * @return The object after, if available
     */
    public Optional<?> after(Class<?> clazz) {
        checkArgument(clazz != null, "The provided class cannot be null!");
        if (this.cause.size() == 1) {
            return Optional.empty();
        }

        ListIterator<Object> iterator = this.cause.listIterator();
        while (iterator.hasNext()) {
            if (clazz.isInstance(iterator.next()) && iterator.hasNext()) {
                return Optional.of(iterator.next());
            }
        }

        return Optional.empty();
    }

    /**
     * Returns whether the target class matches any object of this {@link EventCause}
     * .
     * 
     * @param target The class of the target type
     * @return True if found, false otherwise
     */
    public boolean containsType(Class<?> target) {
        checkArgument(target != null, "The provided class cannot be null!");
        for (Object aCause : this.cause) {
            if (target.isInstance(aCause)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this cause contains of any of the provided {@link Object}. This
     * is the equivalent to checking based on {@link #equals(Object)} for each
     * object in this cause.
     *
     * @param object The object to check if it is contained
     * @return True if the object is contained within this cause
     */
    public boolean contains(Object object) {
        return this.cause.contains(object);
    }

    /**
     * Gets an {@link ImmutableList} of all objects that are instances of the
     * given {@link Class} type <code>T</code>.
     *
     * @param <T> The type of objects to query for
     * @param target The class of the target type
     * @return An immutable list of the objects queried
     */
    public <T> List<T> allOf(Class<T> target) {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (Object aCause : this.cause) {
            if (target.isInstance(aCause)) {
                builder.add((T) aCause);
            }
        }
        return builder.build();
    }

    /**
     * Gets an immutable {@link List} with all object causes that are not
     * instances of the provided {@link Class}.
     *
     * @param ignoredClass The class of object types to ignore
     * @return The list of objects not an instance of the provided class
     */
    public List<Object> noneOf(Class<?> ignoredClass) {
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        for (Object cause : this.cause) {
            if (!ignoredClass.isInstance(cause)) {
                builder.add(cause);
            }
        }
        return builder.build();
    }

    /**
     * Gets an {@link List} of all causes within this {@link EventCause}.
     *
     * @return An immutable list of all the causes
     */
    public List<Object> all() {
        return this.immutableCauses;
    }

    /**
     * Creates a new {@link EventCause} where the objects are added at the end of the
     * cause array of objects.
     *
     * @param additional The additional object to add
     * @return The new cause
     */
    public EventCause with(Object additional) {
        checkNotNull(additional, "No null arguments allowed!");
        return builder().from(this).append(additional).build();
    }

    /**
     * Creates a new {@link EventCause} where the objects are added at the end of the
     * cause array of objects.
     *
     * @param additional The additional object to add
     * @param additionals The remaining objects to add
     * @return The new cause
     */
    public EventCause with(Object additional, Object... additionals) {
        checkNotNull(additional, "No null arguments allowed!");
        EventCause.Builder builder = builder().from(this);
        builder.causes.add(additional);
        if (additionals.length > 0) {
            builder.causes.addAll(Arrays.asList(additionals));
        }
        return builder.build();
    }

    /**
     * Creates a new {@link EventCause} where the objects are added at the end of the
     * cause array of objects.
     *
     * @param iterable The additional objects
     * @return The new cause
     */
    public EventCause with(Iterable<Object> iterable) {
        EventCause.Builder builder = new Builder().from(this);
        for (Object o : iterable) {
            checkNotNull(o, "Cannot add null causes");
            builder.append(o);
        }
        return builder.build();
    }

    /**
     * Merges this cause with the other cause.
     *
     * @param cause The cause to merge with this
     * @return The new merged cause
     */
    public EventCause with(EventCause cause) {
        EventCause.Builder builder = builder().from(this);
        builder.causes.addAll(cause.cause);
        return builder.build();
    }

    @Override
    public Iterator<Object> iterator() {
        return this.cause.iterator();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof EventCause) {
            EventCause cause = ((EventCause) object);
            // TODO: expensive :(
            return Arrays.equals(this.cause.toArray(), cause.cause.toArray());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.cause);
    }

    @Override
    public String toString() {
        String causeString = "Cause[Stack={";
        StringJoiner joiner = new StringJoiner(", ");
        for (Object c : this.cause) {
            joiner.add(c.toString());
        }
        return causeString + joiner.toString() + "}]";
    }

    public static final class Builder {

        CopyOnWriteArrayList<Object> causes = null;

        Builder() {

        }

        private void initIfNeeded() {
            if (causes == null) {
                causes = new CopyOnWriteArrayList<>();
            }
        }

        /**
         * Appends the specified object to the cause.
         *
         * @param cause The object to append to the cause.
         * @return The modified builder, for chaining
         */
        public Builder append(Object cause) {
            checkNotNull(cause, "Cause cannot be null!");
            initIfNeeded();
            if (!this.causes.isEmpty() && this.causes.get(this.causes.size() - 1) == cause) {
                return this;
            }
            this.causes.add(cause);
            return this;
        }

        /**
         * Inserts the specified object into the cause.
         *
         * @param position The position to insert into
         * @param cause The object to insert into the cause
         * @return The modified builder, for chaining
         */
        public Builder insert(int position, Object cause) {
            checkNotNull(cause, "Cause cannot be null!");
            initIfNeeded();
            this.causes.add(position, cause);
            return this;
        }

        /**
         * Appends all specified objects onto the cause.
         *
         * @param otherCauses The objects to add onto the cause
         * @return The modified builder, for chaining
         */
        public Builder appendAll(Collection<Object> otherCauses) {
            checkNotNull(otherCauses, "Causes cannot be null!");
            if (this.causes == null) {
                this.causes = new CopyOnWriteArrayList<>(otherCauses);
            } else {
                initIfNeeded();
                this.causes.addAll(otherCauses);
            }
            return this;
        }

        /**
         * Constructs a new {@link EventCause} with information added to the builder.
         *
         * @return The built cause
         */
        public EventCause build() {
            checkState(this.causes != null && !this.causes.isEmpty(), "Cannot create an empty Cause!");
            return new EventCause(this.causes);
        }

        public Builder from(EventCause value) {
            if (this.causes == null) {
                this.causes = new CopyOnWriteArrayList<>(value.cause);
            } else {
                this.causes.addAll(value.cause);
            }
            return this;
        }

        public Builder reset() {
            this.causes = null;
            return this;
        }
    }

}