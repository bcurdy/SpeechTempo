package com.gtug.speechtempo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class Recorder extends Activity implements OnClickListener {
	private static final String TAG = "Recorder";
	private boolean isRunning;
	//Audio Configuration
	private static final int frequency = 8000;
	private static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private int recordBufferSize = AudioRecord.getMinBufferSize(frequency,
			channelConfiguration, audioEncoding);
	private int playBufferSize = AudioTrack.getMinBufferSize(frequency,
			channelConfiguration, audioEncoding);
	private AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
			frequency, channelConfiguration, audioEncoding, recordBufferSize);
	private AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
			frequency, channelConfiguration, audioEncoding, playBufferSize,
			AudioTrack.MODE_STREAM);
	//Layout
	ImageButton recordButton;
	Visualizer mVisualizer;
	VisualizerView mVisualizerView;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		recordButton = (ImageButton) findViewById(R.id.imageButton1);
		recordButton.setOnClickListener(this);
		isRunning = false;
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick: " + v);
		if (isRunning) {
			stop();
		} else {
			record();
		}
	}

	public void record() {
		isRunning = true;
		audioRecord.startRecording();
		audioTrack.play();
		Toast.makeText(this, "recording started", Toast.LENGTH_LONG).show();
		Thread thread = new Thread(new Runnable() {
			public void run() {
				setupVisualizerFxAndUI();
				mVisualizer.setEnabled(true);
				Log.d("AudioStream", "Recording" );
				while (isRunning) { 
					byte[] buffer = new byte[recordBufferSize];
					int bufferReadResult = audioRecord
							.read(buffer, 0, recordBufferSize);
					audioTrack.write(buffer, 0, bufferReadResult);
				}
			}
		});
		thread.start();
	}

	public void stop() {
		isRunning = false;
		audioRecord.stop();
		audioTrack.stop();
		Toast.makeText(this, "recording stopped", Toast.LENGTH_LONG).show();
	}

	private void setupVisualizerFxAndUI() {
		// Create a VisualizerView (defined below), which will render the
		// simplified audio wave form to a Canvas.
		mVisualizerView = (VisualizerView)findViewById(R.id.view1);

		// Create the Visualizer object and attach it to our media player.
		mVisualizer = new Visualizer(audioTrack.getAudioSessionId());
		mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
		mVisualizer.setDataCaptureListener(
				new Visualizer.OnDataCaptureListener() {
					public void onWaveFormDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
						mVisualizerView.updateVisualizer(bytes);
					}

					public void onFftDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
					}
				}, Visualizer.getMaxCaptureRate() / 2, true, false);
	}

}

/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
class VisualizerView extends View {
	
	public VisualizerView(Context context, AttributeSet attrs) {   
	    super(context, attrs);  
	    init();
	}
	
	private byte[] mBytes;
	private float[] mPoints;
	private Rect mRect = new Rect();

	private Paint mForePaint = new Paint();

	private void init() {
		mBytes = null;

		mForePaint.setStrokeWidth(1f);
		mForePaint.setAntiAlias(true);
		mForePaint.setColor(Color.rgb(0, 128, 255));
	}

	public void updateVisualizer(byte[] bytes) {
		mBytes = bytes;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mBytes == null) {
			return;
		}

		if (mPoints == null || mPoints.length < mBytes.length * 4) {
			mPoints = new float[mBytes.length * 4];
		}

		mRect.set(0, 0, getWidth(), getHeight());

		for (int i = 0; i < mBytes.length - 1; i++) {
			mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
			mPoints[i * 4 + 1] = mRect.height() / 2
					+ ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
			mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
			mPoints[i * 4 + 3] = mRect.height() / 2
					+ ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2)
					/ 128;
		}

		canvas.drawLines(mPoints, mForePaint);
	}
}