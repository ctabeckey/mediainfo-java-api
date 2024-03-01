package com.abercap.mediainfo.api;

import org.apache.commons.cli.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.jna.Platform;

public class CLI {
    private static Options options;
    private static MediaInfo mediaInfo;

    static {
        try {
            int osType = Platform.getOSType();
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            final String libPrefix = osArch != null && !osArch.isEmpty()
                    ? String.format("%d %s-%s", osType, osName, osArch)
                    : String.format("%d %s", osType, osName);
            System.out.println("Loading mediainfo for [" + libPrefix + "]");

            // create Options object
            options = new Options();
        } catch(Throwable t) {
            System.err.println(t.getMessage());
        }
    }

    public static void main(final String[] argv) {
        CommandLineParser parser = new DefaultParser();
        mediaInfo = new MediaInfo();

        try {
            CommandLine cmd = parser.parse(options, argv);

            String[] fileNames = cmd.getArgs();
            if (fileNames == null || fileNames.length == 0) {
                throw new ParseException("The media file path must be specified");
            }

            for (String fileName : fileNames) {
                File mediaFile = new File(fileName);
                if (mediaFile.exists()) {
                    System.out.println(getMetadata(mediaFile));
                } else {
                    throw new ParseException("The media file [" + fileName + "] does not exist");
                }

            }
        } catch (ParseException pX) {
            System.err.println(pX.getMessage());
        } catch (Exception pX) {
            System.err.println(pX.getMessage());
        } finally {
            mediaInfo.dispose();
        }
    }

    private static String getMetadata(final File mediaFile) {
        StringBuilder result = new StringBuilder();
        final String fileIdentifier = mediaFile.getAbsolutePath();

        if (mediaInfo.open(mediaFile)) {
            String videoCodecID = mediaInfo.get(MediaInfo.StreamKind.Video, 0, "CodecID");
            String generalCodecID =  mediaInfo.get(MediaInfo.StreamKind.General, 0, "CodecID");
            Map<MediaInfo.StreamKind, List<Map<String, String>>> mediaInfoProperties = mediaInfo.snapshot();

            result.append(generalCodecID != null
                    ? "File [" + fileIdentifier + "] uses generalCodecID: " + generalCodecID
                    : "Unable to determine generalCodecID used in [" + fileIdentifier + "]");
            result.append(System.lineSeparator());
            result.append(videoCodecID != null
                    ? "File [" + fileIdentifier + "] uses videoCodecId: " + videoCodecID
                    : "Unable to determine videoCodecID used in [" + fileIdentifier + "]");
            for(MediaInfo.StreamKind streamKind : MediaInfo.StreamKind.values()) {
                result.append(System.lineSeparator());
                result.append(streamKind.toString());
                result.append(System.lineSeparator());

                AtomicInteger streamIndex = new AtomicInteger(0);
                List<Map<String, String>> streamKindStreams = mediaInfoProperties.get(streamKind);
                if (streamKindStreams != null) {
                    streamKindStreams.stream().forEach(streamMap -> {
                        int currentStreamIndex = streamIndex.getAndAdd(1);
                        result.append(System.lineSeparator());
                        result.append(streamKind + "[" + currentStreamIndex + "]");

                        streamMap.entrySet().stream().forEach(mapEntry -> {
                            result.append(System.lineSeparator());
                            result.append(mapEntry.getKey() + "=" + mapEntry.getValue());
                        });
                    });
                }
            }
        } else {
            result.append("Failed to open media file '" + mediaFile.getAbsolutePath() + "'.");
        }

        return result.toString();
    }

    public static void addLibraryPath(String pathToAdd) throws Exception {
        Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        String[] paths = (String[]) usrPathsField.get(null);

        for (String path : paths)
            if (path.equals(pathToAdd))
                return;

        String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length - 1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }
}
