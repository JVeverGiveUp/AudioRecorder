package com.ptdstudio.internalsoundrecorder.audio;

import android.content.Context;

import com.dimowner.audiorecorder.audio.AudioDecoder;
import com.dimowner.audiorecorder.ARApplication;
import com.ptdstudio.internalsoundrecorder.PCMToMp3Encoder;
import com.dimowner.audiorecorder.app.info.RecordInfo;
import com.dimowner.audiorecorder.data.Prefs;
import com.dimowner.audiorecorder.data.database.LocalRepository;
import com.dimowner.audiorecorder.data.database.Record;

import java.io.File;

public class Mp3Utils {
    public static boolean updateRecordToDB(File output){
        Context context = ARApplication.Companion.getInstance();
        LocalRepository localRepository = ARApplication.getInjector().provideLocalRepository(context);
        Prefs prefs = ARApplication.getInjector().providePrefs(context);
        RecordInfo info = AudioDecoder.readRecordInfo(output);
        final Record record = localRepository.getRecord((int) prefs.getActiveRecord());
        if (record != null) {
            final Record update = new Record(
                    record.getId(),
                    record.getName(),
                    info.getDuration(),
                    record.getCreated(),
                    record.getAdded(),
                    record.getRemoved(),
                    record.getPath(),
                    info.getFormat(),
                    info.getSize(),
                    info.getSampleRate(),
                    info.getChannelCount(),
                    info.getBitrate(),
                    record.isBookmarked(),
                    record.isWaveformProcessed(),
                    record.getAmps());
            return (localRepository.updateRecord(update));
        }
        return false;
    }

    public static void convertPcm2Mp3(String outputFilePath, String inputFilePath, int chanelCount, int bitrate, int sampleRate){
        PCMToMp3Encoder.init(
                inputFilePath,
                chanelCount,
                bitrate,
                sampleRate,
                outputFilePath
        );
        PCMToMp3Encoder.encode();
        PCMToMp3Encoder.destroy();
    }
}
