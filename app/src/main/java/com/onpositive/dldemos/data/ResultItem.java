package com.onpositive.dldemos.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.onpositive.dldemos.data.TypeConverters.ResultItemConverter;
import com.onpositive.dldemos.tools.Logger;

import java.io.File;
import java.util.Date;

@Entity
@TypeConverters({ResultItemConverter.class})
public class ResultItem implements Comparable<ResultItem> {
    private Date lastModified;
    @PrimaryKey
    @NonNull
    private String filePath;
    private ContentType contentType;
    private String tfLiteParentFile;
    private String thumbnailPath;
    @Ignore
    Logger log = new Logger(this.getClass());

    public ResultItem() {
    }

    public ResultItem(String filePath, ContentType ct, String thumbnailPath) {
        this.lastModified = new Date(new File(filePath).lastModified());
        this.filePath = filePath;
        this.contentType = ct;
        this.thumbnailPath = thumbnailPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        String fileName;
        if (filePath == null)
            return "";
        return filePath.substring(filePath.lastIndexOf("/") + "/".length());
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String preview) {
        this.thumbnailPath = preview;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getTfLiteParentFile() {
        return tfLiteParentFile;
    }

    public void setTfLiteParentFile(String tfLiteParentFile) {
        this.tfLiteParentFile = tfLiteParentFile;
    }

    @Override
    public int compareTo(@NonNull ResultItem o) {
        return getLastModified().compareTo(o.getLastModified());
    }
}
