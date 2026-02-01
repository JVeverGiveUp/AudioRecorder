/*
 * Copyright 2019 Dmytro Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ptdstudio.internalsoundrecorder.audio.recorder;

import static com.dimowner.audiorecorder.AppConstants.RECORDING_VISUALIZATION_INTERVAL;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.chezi.recorder.PCMFormat;
import com.chezi.recorder.listener.AudioRecordListener;
import com.chezi.recorder.utils.LameUtil;
import com.dimowner.audiorecorder.ARApplication;
import com.dimowner.audiorecorder.AppConstants;
import com.dimowner.audiorecorder.audio.recorder.RecorderContract;
import com.dimowner.audiorecorder.exception.InvalidOutputFile;
import com.dimowner.audiorecorder.exception.RecorderInitException;
import com.dimowner.audiorecorder.exception.RecordingException;
import com.dimowner.audiorecorder.util.AndroidUtils;
import com.ptdstudio.internalsoundrecorder.audio.Mp3Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class Mp3RecorderNew implements RecorderContract.Recorder {

	private AudioRecord recorder = null;

	private File recordFile = null;
	private int bufferSize = 0;
	private long updateTime = 0;
	private long durationMills = 0;

	private Thread recordingThread;

	private final AtomicBoolean isRecording = new AtomicBoolean(false);
	private final AtomicBoolean isPaused = new AtomicBoolean(false);
	private final Handler handler = new Handler();


	/** Value for recording used visualisation. */
	private int lastVal = 0;

	private int sampleRate = AppConstants.RECORD_SAMPLE_RATE_44100;
	private int bitRate = 0;


	private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

	//======================Lame Default Settings=====================
	private static final int DEFAULT_LAME_MP3_QUALITY = 5;//7


	/**
	 * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
	 */
	private static final int DEFAULT_LAME_IN_CHANNEL = 1;
	/**
	 * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
	 */
	private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;

	private static final int DEFAULT_SAMPLING_RATE = 44100;
	private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

	private static final int FRAME_COUNT = 160;

	private HandlerThread mChildHandlerThread;
	private Handler mChiHandler;

	private byte[] mMp3Buffer;
	private int channelCount = 1;

	private RecorderContract.RecorderCallback recorderCallback;

	private static class Mp3RecorderSingletonHolder {
		private static final Mp3RecorderNew singleton = new Mp3RecorderNew();

		public static Mp3RecorderNew getSingleton() {
			return Mp3RecorderSingletonHolder.singleton;
		}
	}

	public static Mp3RecorderNew getInstance() {
		return Mp3RecorderSingletonHolder.getSingleton();
	}

	private Mp3RecorderNew() { }

	@Override
	public void setRecorderCallback(RecorderContract.RecorderCallback callback) {
		recorderCallback = callback;
	}

	@SuppressLint("MissingPermission")
    @Override
	public void startRecording(String outputFile, int channelCount, int sampleRate, int bitrate) {
		//in recorder-audio lib, this init is in constructor
		initChildHandler();
		this.sampleRate = sampleRate;
//		channelCount = 1;
		this.channelCount = channelCount;
		this.bitRate = bitrate;
		recordFile = new File(outputFile);
		if (recordFile.exists() && recordFile.isFile()) {
			int channel = channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
			try {
				bufferSize = AudioRecord.getMinBufferSize(sampleRate,
						channel,
						AudioFormat.ENCODING_PCM_16BIT);
//				bufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
//						DEFAULT_CHANNEL_CONFIG,
//						DEFAULT_AUDIO_FORMAT.getAudioFormat());
				int bufferStandard = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
						DEFAULT_CHANNEL_CONFIG,
						DEFAULT_AUDIO_FORMAT.getAudioFormat());
				if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
					bufferSize = AudioRecord.getMinBufferSize(sampleRate,
							channel,
							AudioFormat.ENCODING_PCM_16BIT);
				}
				int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
				/* Get number of samples. Calculate the buffer size
				 * (round up to the factor of given frame size)
				 * 使能被整除，方便下面的周期性通知
				 * */
				int frameSize = bufferSize / bytesPerFrame;
				if (frameSize % FRAME_COUNT != 0) {
					frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
					bufferSize = frameSize * bytesPerFrame;
				}
				mMp3Buffer = new byte[(int) (7200 + (bufferSize * 2 * 1.25))];
				Log.d("Mp3Recorder", "Standard: " + DEFAULT_SAMPLING_RATE + " - " + DEFAULT_CHANNEL_CONFIG + " - " + DEFAULT_AUDIO_FORMAT.getAudioFormat()+ " - " + bufferStandard);
				Log.d("Mp3Recorder", "Modified: " + sampleRate + " - " + channel + " - " + AudioFormat.ENCODING_PCM_16BIT + " - " + bufferSize);


				recorder = new AudioRecord(
						MediaRecorder.AudioSource.MIC,
						sampleRate,
						channel,
						AudioFormat.ENCODING_PCM_16BIT,
						bufferSize
				);
//				recorder = new AudioRecord(
//						MediaRecorder.AudioSource.MIC,
//						DEFAULT_SAMPLING_RATE,
//						DEFAULT_CHANNEL_CONFIG,
//						DEFAULT_AUDIO_FORMAT.getAudioFormat(),
//						bufferSize
//				);

				recorder.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
					@Override
					public void onMarkerReached(AudioRecord recorder) {
						//do nothin
					}

					@Override
					public void onPeriodicNotification(AudioRecord recorder) {

					}
				}, mChiHandler);
				recorder.setPositionNotificationPeriod(FRAME_COUNT);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				Timber.e(e, "sampleRate = " + sampleRate + " channel = " + channel + " bufferSize = " + bufferSize);
				if (recorder != null) {
					recorder.release();
				}
			}
			if (recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED) {
				recorder.startRecording();
				initMp3Lame(sampleRate, channelCount, bitRate/1000);
//				initMp3Lame(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_LAME_MP3_BIT_RATE);
				Log.d("Mp3Recorder", "StandardLame: " + DEFAULT_SAMPLING_RATE + " - " + DEFAULT_LAME_IN_CHANNEL + " - " + DEFAULT_LAME_MP3_BIT_RATE);
				Log.d("Mp3Recorder", "ModifiedLame: " + sampleRate + " - " + channelCount + " - " +bitRate/1000);

				updateTime = System.currentTimeMillis();
				isRecording.set(true);
				recordingThread = new Thread(this::writeAudioDataToFile, "AudioRecorder Thread");

				recordingThread.start();
				scheduleRecordingTimeUpdate();
				if (recorderCallback != null) {
					recorderCallback.onStartRecord(recordFile);
				}
				isPaused.set(false);
			} else {
				Timber.e("prepare() failed");
				if (recorderCallback != null) {
					recorderCallback.onError(new RecorderInitException());
				}
			}
		} else {
			if (recorderCallback != null) {
				recorderCallback.onError(new InvalidOutputFile());
			}
		}
		//Log.d("InternalTest", "Msg: " + "sampleRate = " + sampleRate + " channel = " + channelCount + " bufferSize = " + bufferSize + " bitRate = " + bitRate);

	}

	@Override
	public void resumeRecording() {
		if (recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED) {
			if (isPaused.get()) {
				updateTime = System.currentTimeMillis();
				scheduleRecordingTimeUpdate();
				recorder.startRecording();
				if (recorderCallback != null) {
					recorderCallback.onResumeRecord();
				}
				isPaused.set(false);
			}
		}
	}

	@Override
	public void pauseRecording() {
		if (isRecording.get()) {
			recorder.stop();
			durationMills += System.currentTimeMillis() - updateTime;
			pauseRecordingTimer();

			isPaused.set(true);
			if (recorderCallback != null) {
				recorderCallback.onPauseRecord();
			}
		}
	}

	@Override
	public void stopRecording() {
		if (recorder != null) {
			isRecording.set(false);
			isPaused.set(false);
			stopRecordingTimer();
			if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
				try {
					recorder.stop();
				} catch (IllegalStateException e) {
					e.printStackTrace();
					Timber.e(e, "stopRecording() problems");
				}
			}
			durationMills = 0;
			if(recorder != null)
				recorder.release();
			recordingThread.interrupt();

			if (recorderCallback != null) {
				recorderCallback.onStopRecord(recordFile);
			}
		}
	}

	@Override
	public boolean isRecording() {
		return isRecording.get();
	}

	@Override
	public boolean isPaused() {
		return isPaused.get();
	}

	@Override
	public void setMediaProjection(MediaProjection projection) {

	}

	private void writeAudioDataToFile() {
		Log.d("Mp3Recorder", "Buffersize in writeAudioDataToFile: " + bufferSize);
		short []mPCMBuffer = new short[bufferSize];
		//byte[] data = new byte[bufferSize];
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(recordFile);
		} catch (FileNotFoundException e) {
			Timber.e(e);
			fos = null;
		}
		if (null != fos) {
			//TODO: Disable loop while pause.
			while (isRecording.get()) {
				if (!isPaused.get()) {
					int readSize = recorder.read(mPCMBuffer, 0, bufferSize);
					if (readSize > 0) {
						//below code is copy of processData(mPCMBuffer, readSize);
//						int encodedSize = LameUtil.encode(mPCMBuffer, mPCMBuffer, readSize, mMp3Buffer);
						int channelCount = this.channelCount; // Get the actual channel count
						int encodedSize = 0;
						if (channelCount == 1) {
							// Mono: same data for both channels
							encodedSize = LameUtil.encode(mPCMBuffer, mPCMBuffer, readSize, mMp3Buffer);
						} else if (channelCount == 2) {
							// Stereo: Split into left and right channels
							int samplesPerChannel = readSize / 2;
							short[] left = new short[samplesPerChannel];
							short[] right = new short[samplesPerChannel];
							for (int i = 0; i < samplesPerChannel; i++) {
								left[i] = mPCMBuffer[2 * i];
								right[i] = mPCMBuffer[2 * i + 1];
							}
							encodedSize = LameUtil.encode(left, right, samplesPerChannel, mMp3Buffer);
						}
						if (encodedSize > 0) {
							try {
								fos.write(mMp3Buffer, 0, encodedSize);
								//calculateRealVolume(mPCMBuffer, readSize);
							} catch (IOException e) {
								Timber.e(e);
								AndroidUtils.runOnUIThread(() -> {
									recorderCallback.onError(new RecordingException());
									stopRecording();
								});
							}
						}
					}
				}
			}

			recorder.release();
			recorder = null;
			/**
			 * Flush all data left in lame buffer to file
			 */
			final int flushResult = LameUtil.flush(mMp3Buffer);
			if (flushResult > 0) {
				try {
					fos.write(mMp3Buffer, 0, flushResult);
				} catch (IOException e) {
					Timber.e(e);
				} finally {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                    LameUtil.close();
				}
			}
		}
	}



	private void scheduleRecordingTimeUpdate() {
		handler.postDelayed(() -> {
			if (recorderCallback != null && recorder != null) {
				long curTime = System.currentTimeMillis();
				durationMills += curTime - updateTime;
				updateTime = curTime;
				recorderCallback.onRecordProgress(durationMills, lastVal);
				scheduleRecordingTimeUpdate();
			}
		}, RECORDING_VISUALIZATION_INTERVAL);
	}

	private void stopRecordingTimer() {
		handler.removeCallbacksAndMessages(null);
		updateTime = 0;
	}

	private void pauseRecordingTimer() {
		handler.removeCallbacksAndMessages(null);
		updateTime = 0;
	}



	private void initChildHandler() {
		if (mChildHandlerThread == null) {
			mChildHandlerThread = new HandlerThread("converMp3Thread");
			mChildHandlerThread.start();

			mChiHandler = new Handler(mChildHandlerThread.getLooper());
		}
	}

	private void initMp3Lame(int sampleRate, int channelCount, int mp3BitRate) {
		/*
		 * Initialize lame buffer
		 * mp3 sampling rate is the same as the recorded pcm sampling rate
		 * The bit rate is 32kbps
		 *
		 */
		LameUtil.init(sampleRate, channelCount,
				sampleRate, mp3BitRate/*DEFAULT_LAME_MP3_BIT_RATE*/, DEFAULT_LAME_MP3_QUALITY);
	}

	/**
	 * 此计算方法来自samsung开发范例
	 *
	 * @param buffer   buffer
	 * @param readSize readSize
	 */
	private void calculateRealVolume(short[] buffer, int readSize) {
		double sum = 0;
		for (int i = 0; i < readSize; i++) {
			// 这里没有做运算的优化，为了更加清晰的展示代码
			sum += buffer[i] * buffer[i];
		}
		if (readSize > 0) {
			double amplitude = sum / readSize;
			int volume = (int) Math.sqrt(amplitude);
		}
	}




}
