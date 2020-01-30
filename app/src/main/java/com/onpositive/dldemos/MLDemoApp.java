package com.onpositive.dldemos;

import android.app.Application;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.onpositive.dldemos.data.AppDatabase;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.data.TFModelType;

public class MLDemoApp extends Application {
    private static MLDemoApp mlDemoAppInstance;
    private AppDatabase appDatabase;

    public static MLDemoApp getInstance() {
        return mlDemoAppInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mlDemoAppInstance = this;
        appDatabase = Room.databaseBuilder(this, AppDatabase.class, "mlDemoDB").addCallback(
                new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        final Handler handler = new Handler(getApplicationContext().getMainLooper());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                TFLiteItem tfLiteItem = new TFLiteItem("segmentation_model.tflite",
                                        getApplicationContext().getString(R.string.segmentation),
                                        true,
                                        TFModelType.SEGMENTATION,
                                        320,
                                        320);
                                getInstance().getDatabase().tfLiteItemDao().insert(tfLiteItem);

                                Runnable refreshRunnabel = new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.refreshTabs();
                                    }
                                };
                                handler.post(refreshRunnabel);
                            }
                        }).start();
                    }
                }
        ).allowMainThreadQueries().build();
    }

    public AppDatabase getDatabase() {
        return appDatabase;
    }
}
