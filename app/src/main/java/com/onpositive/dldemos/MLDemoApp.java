package com.onpositive.dldemos;

import android.app.Application;
import android.arch.persistence.room.Room;

import com.onpositive.dldemos.data.AppDatabase;

public class MLDemoApp extends Application {
    public static MLDemoApp mlDemoAppInstance;
    private AppDatabase appDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        mlDemoAppInstance = this;
        appDatabase = Room.databaseBuilder(this, AppDatabase.class, "mlDemoDB").allowMainThreadQueries().build();
    }

    public static MLDemoApp getInstance() {
        return mlDemoAppInstance;
    }

    public AppDatabase getDatabase() {
        return appDatabase;
    }
}
