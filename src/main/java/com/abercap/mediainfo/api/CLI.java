package com.abercap.mediainfo.api;

import org.apache.commons.cli.*;

import java.io.File;

public class CLI {
    private final static Options options;
    private final static MediaInfo mediaInfo;

    static {
        // create Options object
        options = new Options();
        options.addOption("t", false, "display current time");

        mediaInfo = new MediaInfo();
    }

    public static void main(final String[] argv) {
        CommandLineParser parser = new DefaultParser();
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

    private static String getMetadata(File mediaFile) {
        StringBuilder result = new StringBuilder();

        if (mediaInfo.open(mediaFile)) {
            String codec = mediaInfo.get(MediaInfo.StreamKind.Video, 0, "CodecID");

            if (codec != null) {
                result.append("File [" + mediaFile.getAbsolutePath() + "] uses codec: " + codec);
            } else {
                result.append("Unable to determine codec used in MP4 file.");
            }
        } else {
            result.append("Failed to open media file '" + mediaFile.getAbsolutePath() + "'.");
        }

        return result.toString();
    }
}
