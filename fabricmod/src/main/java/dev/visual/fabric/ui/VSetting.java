package dev.visual.fabric.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Eine Zeile auf einer Modul-Detailseite. */
public abstract class VSetting {
    public final String label;

    protected VSetting(String label) {
        this.label = label;
    }

    /** Schalter mit Haken rechts. */
    public static final class Toggle extends VSetting {
        public final BooleanSupplier get;
        public final Consumer<Boolean> set;

        public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(label);
            this.get = get;
            this.set = set;
        }
    }

    /** Wert mit − / + und Anzeige dazwischen (Zahlen, Farben, Stile). */
    public static final class Stepper extends VSetting {
        public final Supplier<String> display;
        public final Runnable next;
        public final Runnable prev;

        public Stepper(String label, Supplier<String> display, Runnable prev, Runnable next) {
            super(label);
            this.display = display;
            this.prev = prev;
            this.next = next;
        }
    }

    /** Reine Info-/Tastenzeile ohne Bedienung. */
    public static final class Info extends VSetting {
        public final Supplier<String> value;

        public Info(String label, Supplier<String> value) {
            super(label);
            this.value = value;
        }
    }

    /** Knopf, der etwas auslöst. */
    public static final class Action extends VSetting {
        public final Runnable run;
        public final IntSupplier tint;

        public Action(String label, Runnable run, IntSupplier tint) {
            super(label);
            this.run = run;
            this.tint = tint;
        }
    }
}
