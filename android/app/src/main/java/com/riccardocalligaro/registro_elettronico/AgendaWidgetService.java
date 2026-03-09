package com.riccardocalligaro.registro_elettronico;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class AgendaWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new AgendaWidgetDataProvider(this.getApplicationContext(), intent);
    }
}
