
package com.atakmap.android.util;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.FileIOProviderFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A ReservationService for a directory of files.  Supports flushing of "stale"
 * files.  Files are "freshened" when they are reserved.  Stale files are
 * reserved during flushing, to avoid deletion of files that are in use.
 **/
public class FileCache
        extends ReservationService<File> {

    private static final String TAG = "FileCache";

    //==================================
    //
    //  PUBLIC INTERFACE
    //
    //==================================

    /**
     * Creates a FileCache for the supplied directory.  The directory will be
     * created if it does not already exist.
     *
     * @param cacheDir          The directory to use for the cache.
     * @throws IllegalArgumentException If the supplied cacheDir is null, exists
     * but is not a readable directory, or can't be created.
     **/
    public FileCache(File cacheDir) {
        if (cacheDir == null) {
            throw new IllegalArgumentException("Received null cache directory");
        }
        if (FileIOProviderFactory.exists(cacheDir)) {
            if (!FileIOProviderFactory.isDirectory(cacheDir) || !cacheDir.canRead()) {
                throw new IllegalArgumentException("Invalid cache directory");
            }
        } else if (!FileIOProviderFactory.mkdirs(cacheDir)) {
            throw new IllegalArgumentException(
                    "Failed to create cache directory.");
        }
        this.cacheDir = cacheDir;
    }

    /**
     * Expunges from the cache those files that were last accessed longer ago
     * than the supplied "staleness," expressed in milliseconds.  Files that are
     * reserved are assumed to be fresh.
     * 
     * @param staleness         The maximum number of milliseconds ago a file
     *                          must have been accessed to avoid being deleted
     *                          from the cache.
     **/
    public void flushStaleCache(final long staleness) // in milliseconds
    {
        if (FileIOProviderFactory.exists(cacheDir)) {
            File[] files = FileIOProviderFactory.listFiles(cacheDir);

            if (files != null) {
                final long now = System.currentTimeMillis();

                for (File f : files) {
                    final File cacheFile = f;

                    tryWithReservation(f, new Runnable() {
                        @Override
                        public void run() {
                            long modTime = FileIOProviderFactory.lastModified(cacheFile);
                            if (Math.abs(modTime - now) > staleness)
                                FileSystemUtils.delete(cacheFile);
                        }
                    });
                }
            }
        }
    }

    public String getCachePath() {
        return cacheDir.getAbsolutePath();
    }

    //==================================
    //
    //  PROTECTED INTERFACE
    //
    //==================================

    //==================================
    //  ReservationService INTERFACE
    //==================================

    @Override
    public Reservation<File> createReservation(File f) {
        freshen(f);
        return super.createReservation(f);
    }

    //==================================
    //
    //  PRIVATE IMPLEMENTATION
    //
    //==================================

    /**
     * Attempts to update the modification date of the file.
     *
     * @param f         The file to be updated.
     **/
    private void freshen(File f) {
        if (f != null
                && FileIOProviderFactory.exists(f)
                && !f.setLastModified(System.currentTimeMillis())) {
            //
            // Hack to update the lastModified time.
            //
            RandomAccessFile raf = null;

            try {
                raf = FileIOProviderFactory.getRandomAccessFile(f, "rw");

                long length = raf.length();

                raf.setLength(length + 1);
                raf.setLength(length);
            } catch (IOException ignored) {
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    //==================================
    //  PRIVATE REPRESENTATION
    //==================================

    private final File cacheDir;
}
