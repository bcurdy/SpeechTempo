package com.gtug.speechtempo;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class Recorder extends Activity implements OnClickListener {
	private static final String TAG = "Recorder";
	private static final int frequency = 8000;
	private static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	ImageButton recordButton;
	Visualizer mVisualizer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		recordButton = (ImageButton) findViewById(R.id.imageButton1);
		recordButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick: " + v);
		record();
	}

	public void record() {
		int recordBufferSize = AudioRecord.getMinBufferSize(frequency,
				channelConfiguration, audioEncoding);
		int playBufferSize= AudioTrack.getMinBufferSize(frequency,
				channelConfiguration, audioEncoding);
		AudioRecord audioRecord = new AudioRecord(
				MediaRecorder.AudioSource.MIC, frequency, channelConfiguration,
				audioEncoding, recordBufferSize);
		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				frequency, channelConfiguration, audioEncoding, playBufferSize,
				AudioTrack.MODE_STREAM);

		byte[] buffer = new byte[recordBufferSize];
		audioRecord.startRecording();
		audioTrack.play();
		Toast.makeText(this, "recording started", Toast.LENGTH_LONG).show();
		while (true) {		
			int bufferReadResult = audioRecord.read(buffer, 0, recordBufferSize);
			audioTrack.write(buffer, 0, bufferReadResult);
			mVisualizer = new Visualizer(audioTrack.getAudioSessionId());
			mVisualizer.setEnabled(true);
            byte[] data = new byte[mVisualizer.getCaptureSize()];
            mVisualizer.getFft(data);
			Log.d("AudioStream", ""+data);
		}
	}
	
	class VisualizerView extends View {

		public VisualizerView(Context context) {
			super(context);
			init();
		}
		
		private void init() {
			//
		}
	}

}