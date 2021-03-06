package com.onpositive.dldemos.interpreter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.onpositive.dldemos.ProgressEvent;
import com.onpositive.dldemos.ProgressListener;
import com.onpositive.dldemos.data.TFLiteItem;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;

import java.io.File;
import java.io.IOException;

public class VideoSegmentator extends ImageSegmentator {

    private ProgressListener progressListener;

    public VideoSegmentator(Activity activity, TFLiteItem tfLiteItem) throws IOException {
        super(activity, tfLiteItem);
    }

    public String getSegmentedVideoPath(String videoFilePath) {
        log.log("Video segmentation started");
        int startTime = (int) System.currentTimeMillis();
        String segmentedVideoPath = videoFilePath.substring(0, videoFilePath.lastIndexOf(".")) + "_s.mp4";

        FrameGrab grab = null;
        int processedFrameCount = 0;
        SeekableByteChannel videoOut = null;
        File inputFile = new File(videoFilePath);
        boolean isCanceled = true;
        try {
            grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(inputFile));
            Picture picture;

            videoOut = NIOUtils.writableFileChannel(segmentedVideoPath);
            DemuxerTrackMeta dtm = grab.getVideoTrack().getMeta();
            AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(videoOut, Rational.R1((int) (dtm.getTotalFrames() / dtm.getTotalDuration())));

            Matrix matrix = getFrameMatrix(dtm);
            while (null != (picture = grab.getNativeFrame())) {
                int frameSegStart = (int) System.currentTimeMillis();
                processedFrameCount++;
                Bitmap frame = AndroidUtil.toBitmap(picture);
                Bitmap frameBitmap = Bitmap.createBitmap(frame, 0, 0, frame.getWidth(), frame.getHeight(), matrix, true);
                Bitmap segmentedFrame = getSegmentedImage(frameBitmap);
                encoder.encodeImage(segmentedFrame);
                int frameSegEnd = (int) System.currentTimeMillis();
                if (progressListener != null) {
                    int progressInPercent = (int) Math.round((double) processedFrameCount / dtm.getTotalFrames() * 100);
                    int expectedMinutes = (frameSegEnd - frameSegStart) / 1000 * (dtm.getTotalFrames() - processedFrameCount) / 60;
                    ProgressEvent pe = new ProgressEvent(progressInPercent, expectedMinutes);
                    progressListener.updateProgress(pe);
                    if (pe.isCanceled()) {
                        log.log("Video segmentation canceled. Progress: " + progressInPercent + "%");
                        break;
                    }
                }
            }
            isCanceled = false;
            encoder.finish();
        } catch (IOException e) {
            log.log("Video segmentation failed:\n" + e.getMessage());
        } catch (JCodecException e) {
            log.log("Video segmentation failed:\n" + e.getMessage());
        } finally {
            NIOUtils.closeQuietly(videoOut);
            File segmentationResult = new File(segmentedVideoPath);
            if (isCanceled && segmentationResult.exists())
                segmentationResult.delete();
        }
        int segmentationTime = (int) System.currentTimeMillis() - startTime;
        log.log("\nVideo segmentation time: " + String.valueOf(segmentationTime)
                + "\nFrame count: " + processedFrameCount
                + "\nTime per frame: " + segmentationTime / processedFrameCount);
        return segmentedVideoPath;
    }

    private Matrix getFrameMatrix(DemuxerTrackMeta dtm) {
        Matrix matrix = new Matrix();
        switch (dtm.getOrientation()) {
            case D_90:
                matrix.postRotate(90);
                break;
            case D_180:
                matrix.postRotate(180);
                break;
            case D_270:
                matrix.postRotate(270);
                break;
            default:
                matrix.postRotate(0);
                break;
        }
        return matrix;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
        log.log("ProgressListener initialized");
    }
}
