package com.onpositive.dldemos;

import android.net.Uri;
import android.os.AsyncTask;

import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class LabelDownloaderAT extends AsyncTask<Uri, Void, File> {

    private TFliteAddFragment fragment;
    private Logger logger = new Logger(LabelDownloaderAT.class);

    public LabelDownloaderAT(TFliteAddFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    protected File doInBackground(Uri... uris) {
        File labelsFile = downloadLabels(uris[0]);
        return labelsFile;
    }

    @Override
    protected void onPostExecute(File file) {
        super.onPostExecute(file);
        if (null != file) {
            fragment.setLabelsPath(file.getAbsolutePath());
            String labelsFileInfo = fragment.getContext().getResources().getString(R.string.labels_file, file.getName());
            fragment.labelsInfo.setText(labelsFileInfo);
            logger.log("onPostExecute done");
        } else {
            logger.log("onPostExecute filed. Labels file is null");
        }
    }

    private File downloadLabels(Uri uri) {
        File path = fragment.getContext().getExternalFilesDir("TFLite");
        File outFile = new File(path, Utils.getFileName(fragment.getContext(), uri));
        try {
            path.mkdirs();
            InputStream is = fragment.getContext().getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(outFile);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            logger.log("Downloaded labels file path: " + outFile.getAbsolutePath());
            return outFile;
        } catch (Exception e) {
            logger.log("File downloading failed. Error: " + e);
        }
        return null;
    }
}
