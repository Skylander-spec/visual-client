package dev.visual.fabric.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Ein Eintrag der Modul-Übersicht: Kachel mit Symbol, Titel und Beschreibung,
 * dahinter eine Detailseite mit An/Aus und den zugehörigen Einstellungen.
 */
public class VModule {
    public final String id;
    public final String title;
    public final String description;
    public final String icon;
    /** null = Modul hat keinen eigenen An/Aus-Schalter (z. B. reine Seiten). */
    public final BooleanSupplier enabled;
    public final Consumer<Boolean> setEnabled;
    public final List<VSetting> settings = new ArrayList<>();
    /** Statt Detailseite direkt etwas öffnen (z. B. HUD-Editor). */
    public Runnable action;

    public VModule(String id, String icon, String title, String description,
                   BooleanSupplier enabled, Consumer<Boolean> setEnabled) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.enabled = enabled;
        this.setEnabled = setEnabled;
    }

    public VModule add(VSetting setting) {
        settings.add(setting);
        return this;
    }

    public VModule onOpen(Runnable action) {
        this.action = action;
        return this;
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase(java.util.Locale.ROOT);
        return title.toLowerCase(java.util.Locale.ROOT).contains(q)
                || description.toLowerCase(java.util.Locale.ROOT).contains(q);
    }
}
