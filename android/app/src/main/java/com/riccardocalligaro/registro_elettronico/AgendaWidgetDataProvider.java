package com.riccardocalligaro.registro_elettronico;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AgendaWidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String PREFS_NAME = "FlutterSharedPreferences";
    private static final String EVENTS_KEY = "flutter.widget_agenda_events";

    private final Context context;
    private List<AgendaEvent> events = new ArrayList<>();

    public AgendaWidgetDataProvider(Context context, Intent intent) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        loadEvents();
    }

    @Override
    public void onDataSetChanged() {
        loadEvents();
    }

    @Override
    public void onDestroy() {
        events.clear();
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= events.size()) {
            return null;
        }

        AgendaEvent event = events.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.agenda_widget_item);

        // Set title
        String title = event.title != null && !event.title.isEmpty() ? event.title : event.notes;
        if (title == null || title.isEmpty()) {
            title = "Evento";
        }
        views.setTextViewText(R.id.item_event_title, title);

        // Set notes (show only if different from title)
        if (event.notes != null && !event.notes.isEmpty() && !event.notes.equals(title)) {
            views.setTextViewText(R.id.item_event_notes, event.notes);
            views.setViewVisibility(R.id.item_event_notes, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.item_event_notes, android.view.View.GONE);
        }

        // Set author
        if (event.author != null && !event.author.isEmpty()) {
            views.setTextViewText(R.id.item_event_author, capitalizeWords(event.author));
            views.setViewVisibility(R.id.item_event_author, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.item_event_author, android.view.View.GONE);
        }

        // Set indicator color based on event type
        int indicatorColor;
        if (isVerificaOrInterrogazione(event.notes)) {
            indicatorColor = Color.parseColor("#FFF44336"); // Red
            views.setImageViewResource(R.id.item_event_icon, android.R.drawable.ic_dialog_alert);
        } else if (isCompito(event.notes)) {
            indicatorColor = Color.parseColor("#FFFF9800"); // Orange
            views.setImageViewResource(R.id.item_event_icon, android.R.drawable.ic_menu_edit);
        } else {
            indicatorColor = Color.parseColor("#FF2196F3"); // Blue
            views.setImageViewResource(R.id.item_event_icon, android.R.drawable.ic_menu_my_calendar);
        }
        views.setInt(R.id.item_indicator, "setBackgroundColor", indicatorColor);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (position < events.size()) {
            return events.get(position).id;
        }
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void loadEvents() {
        events.clear();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String eventsJson = prefs.getString(EVENTS_KEY, "[]");
            JSONArray jsonArray = new JSONArray(eventsJson);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                AgendaEvent event = new AgendaEvent();
                event.id = obj.optInt("id", i);
                event.title = obj.optString("title", "");
                event.notes = obj.optString("notes", "");
                event.author = obj.optString("author", "");
                event.isFullDay = obj.optBoolean("isFullDay", false);
                event.labelColor = obj.optString("labelColor", "");
                events.add(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isVerificaOrInterrogazione(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("verifica") || lower.contains("interrogazione")
                || lower.contains("test") || lower.contains("esame");
    }

    private boolean isCompito(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("compito") || lower.contains("compiti");
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        StringBuilder sb = new StringBuilder();
        String[] words = str.toLowerCase().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(' ');
            if (!words[i].isEmpty()) {
                sb.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    sb.append(words[i].substring(1));
                }
            }
        }
        return sb.toString();
    }

    static class AgendaEvent {
        int id;
        String title;
        String notes;
        String author;
        boolean isFullDay;
        String labelColor;
    }
}
