package com.onpositive.dldemos.data.TypeConverters;

import androidx.room.TypeConverter;

import com.onpositive.dldemos.data.ContentType;

import java.util.Date;

public class ResultItemConverter {
    @TypeConverter
    public Long fromLastModified(Date date) {
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    @TypeConverter
    public Date toLastModified(Long timestamp) {
        if (timestamp == null) {
            return null;
        } else {
            return new Date(timestamp);
        }
    }

    @TypeConverter
    public String fromContentType(ContentType ct) {
        switch (ct) {
            case IMAGE:
                return "IMAGE";
            case VIDEO:
                return "VIDEO";
        }
        return null;
    }

    @TypeConverter
    public ContentType toContentType(String ct) {
        switch (ct.toLowerCase()) {
            case "image":
                return ContentType.IMAGE;
            case "video":
                return ContentType.VIDEO;
        }
        return null;
    }
}
