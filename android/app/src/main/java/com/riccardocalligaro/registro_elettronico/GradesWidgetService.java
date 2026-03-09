package com.riccardocalligaro.registro_elettronico;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class GradesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GradesWidgetDataProvider(this.getApplicationContext(), intent);
    }
}
