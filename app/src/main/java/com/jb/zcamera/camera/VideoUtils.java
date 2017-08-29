
package com.jb.zcamera.camera;

import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

public class VideoUtils {
    
    private static final String TAG = "VideoUtils";
    
    /**
     * Appends mp4 audio/video from {@code anotherFileName} to
     * {@code mainFileName}.
     */
    public static boolean append(String mainFileName, String anotherFileName) {
        boolean rvalue = false;
        try {
            File targetFile = new File(mainFileName);
            File anotherFile = new File(anotherFileName);
            if (targetFile.exists() && targetFile.length() > 0) {
                String tmpFileName = mainFileName + ".tmp";

                append(mainFileName, anotherFileName, tmpFileName);
                anotherFile.delete();
                targetFile.delete();
                new File(tmpFileName).renameTo(targetFile);
                rvalue = true;
            } else if (targetFile.createNewFile()) {
                copyFile(anotherFileName, mainFileName);
                anotherFile.delete();
                rvalue = true;
            }
        } catch (Exception tr) {
            Log.e("VideoUtils", "", tr);
        }
        return rvalue;
    }

    public static void append(final String firstFile, final String secondFile, final String newFile)
            throws IOException {

        final FileOutputStream fos = new FileOutputStream(new File(String.format(newFile)));
        final FileChannel fc = fos.getChannel();

        Movie movieOne = null;
        try {
            movieOne = MovieCreator.build(firstFile);
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        
        Movie movieTwo = null;
        try {
            movieTwo = MovieCreator.build(secondFile);
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        
        Movie finalMovie = null;
        if (movieOne == null && movieTwo == null) {
            finalMovie = new Movie();
        } else if (movieOne == null && movieTwo != null) {
            finalMovie = movieTwo;
        } else if (movieOne != null && movieTwo == null) {
            finalMovie = movieOne;
        } else {
            final List<Track> movieOneTracks = movieOne.getTracks();
            final List<Track> movieTwoTracks = movieTwo.getTracks();

            finalMovie = new Movie();
            for (int i = 0; i < movieOneTracks.size() || i < movieTwoTracks.size(); ++i) {
                finalMovie.addTrack(new AppendTrack(movieOneTracks.get(i), movieTwoTracks.get(i)));
            }
        }

        final Container container = new DefaultMp4Builder().build(finalMovie);
        container.writeContainer(fc);
        fc.close();
        fos.close();
    }

    public static void copyFile(final String from, final String destination) throws IOException {
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(destination);
        copy(in, out);
        in.close();
        out.close();
    }

    public static void copy(FileInputStream in, FileOutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }
}
