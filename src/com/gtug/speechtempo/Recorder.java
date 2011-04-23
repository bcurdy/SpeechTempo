package com.gtug.speechtempo;

import java.util.ArrayList;
import java.util.Arrays;

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
import android.os.Process;
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
	private static final int frequency = 44100;
	private static final int recordingChannelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	private static final int playingChannelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	//Recording parameters
	private int recordBufferSize = AudioRecord.getMinBufferSize(frequency,
			recordingChannelConfiguration, audioEncoding);
	private AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
			frequency, recordingChannelConfiguration, audioEncoding, recordBufferSize);
	//Playback parameters
	private int playBufferSize = AudioTrack.getMinBufferSize(frequency,
			playingChannelConfiguration, audioEncoding);
	private AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
			frequency, playingChannelConfiguration, audioEncoding, playBufferSize,
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
		//Button to press to start/stop recording
		recordButton = (ImageButton) findViewById(R.id.recordButton);
		recordButton.setOnClickListener(this);
		//initial state, not recording
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
		//Launch both 
		audioRecord.startRecording();
		audioTrack.play();
		Toast.makeText(this, "recording started", Toast.LENGTH_LONG).show();
		Thread recordThread = new Thread(new Runnable() {
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
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
		recordThread.start();
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
						
					}

					public void onFftDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
				
						mVisualizerView.updateVisualizer(bytes);
					}
				}, Visualizer.getMaxCaptureRate() / 2, false, true);
	}

}

/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
class VisualizerView extends View {
	ArrayList<double[]> savewave;
	private double[] db;
	
	public VisualizerView(Context context, AttributeSet attrs) {   
	    super(context, attrs);  
	    init();
	}
	
	private byte[] mBytes;
	private Rect mRect = new Rect();

	private Paint mForePaint = new Paint();

	private void init() {
		mBytes = null;
		savewave = new ArrayList<double[]>();
		mForePaint.setStrokeWidth(1f);
		mForePaint.setAntiAlias(true);
		//mForePaint.setColor(Color.rgb(0, 128, 255));
		this.setBackgroundColor(Color.WHITE);
	}

	public void updateVisualizer(byte[] bytes) {
		mBytes = bytes;
		db = new double[(mBytes.length)/2];
		for (int i = 0; i< mBytes.length-1; i = i+2) {
			db[i/2] =  Math.sqrt((Math.pow(mBytes[i],2) + Math.pow(mBytes[i+1], 2)));
		}
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mBytes == null) {
			return;
		}
		
		savewave.add(db);
	
		mRect.set(0, 0, getWidth(), getHeight());

		
		for(int i = 0; i < savewave.size(); i++){
			double[] drawline = savewave.get(i);
			for(int j = 0 ; j<db.length; j++){
				int colorvalue = (int) (255-Math.abs(drawline[j])*20);
				if(colorvalue<0)colorvalue = 0;
				mForePaint.setColor(Color.rgb(colorvalue, colorvalue, colorvalue));
				canvas.drawPoint(getWidth()-savewave.size()+i, getHeight()-j, mForePaint); //x: max y: max
			}
					
		}
		
	}
}