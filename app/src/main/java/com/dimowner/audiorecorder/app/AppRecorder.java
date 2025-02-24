/*
 * Copyright 2020 Dmytro Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.audiorecorder.app;

import android.media.projection.MediaProjection;

import com.dimowner.audiorecorder.IntArrayList;
import com.dimowner.audiorecorder.audio.recorder.RecorderContract;

import java.io.File;

public interface AppRecorder {

	void addRecordingCallback(AppRecorderCallback recorderCallback);
	void removeRecordingCallback(AppRecorderCallback recorderCallback);
	void setRecorder(RecorderContract.Recorder recorder);
	void startRecording(String filePath, int channelCount, int sampleRate, int bitrate);
	void pauseRecording();
	void resumeRecording();
	void stopRecording();
	IntArrayList getRecordingData();
	long getRecordingDuration();
	boolean isRecording();
	boolean isPaused();
	File getRecordFile();
	void release();
	void setMediaProjection(MediaProjection mediaProjection);
}
