package mobi.omegacentauri.VolumeSwipe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.util.Log;

public class VolumeController {
	private AudioManager am;
	private int extraDB = 1500;
	private short bands;
	private Equalizer eq = null;
	private int maxStreamVolume;
	private boolean shape = true;

	@SuppressLint("NewApi")
	VolumeController(Context context, float boost, boolean shape) {
		am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		maxStreamVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		this.shape = shape;
		
		VolumeSwipe.log("maxStreamVolume "+maxStreamVolume);
		
		if (Build.VERSION.SDK_INT>=9) {
			try {
				eq = new Equalizer(87654329, 0);
				bands = eq.getNumberOfBands();
				extraDB = (int)(eq.getBandLevelRange()[1] * boost) / 100;
				eq.setEnabled(extraDB>0);
				Log.v("VolumeSwipe", "equalizer OK "+extraDB);
			}
			catch(UnsupportedOperationException e) {
				Log.e("VolumeSwipe", "equalizer error:"+e.toString());
				extraDB = 0;
				eq = null;
			}
		}
	}
	
	public int getPercent() {
		return 100 * getVolume() / maxStreamVolume;
	}
	
	public int getMaxVolume() {
		return maxStreamVolume + extraDB;
	}
	
	@SuppressLint("NewApi")
	void setVolume(int v) {
		VolumeSwipe.log("Need to set to "+v);
		
		if (v > maxStreamVolume + extraDB)
			v=maxStreamVolume + extraDB;
		else if (v<0)
			v=0;

		int toSet = v <= maxStreamVolume ? v : maxStreamVolume;
		VolumeSwipe.log("Vol set to "+toSet);		
		am.setStreamVolume(AudioManager.STREAM_MUSIC, toSet, 0/*AudioManager.FLAG_SHOW_UI*/);

		if (extraDB > 0) {
			if (maxStreamVolume < v) {
				VolumeSwipe.log("Boost!");
				try {
					eq.setEnabled(true);
					int value = v-maxStreamVolume;
					for (short i=0; i<bands; i++) {
						short adj = (short)(value*100);

						if (shape) {
							int hz = eq.getCenterFreq((short)i)/1000;
							if (hz < 150)
								adj = 0;
							else if (hz < 250)
								adj = (short)(adj/2);
							else if (hz > 8000)
								adj = (short)(3*(int)adj/4);
							VolumeSwipe.log(""+i+" "+hz+" "+adj);
						}
						eq.setBandLevel((short)i, (short)adj);
					}
				}
				catch(UnsupportedOperationException e) {
					Log.e("VolumeSwipe", e.toString());
					reset();
				}
				VolumeSwipe.log("Set with boost to "+getVolume());
			}
			else {
				reset();
				VolumeSwipe.log("Set to "+getVolume());
			}
		}
	}
	
	@SuppressLint("NewApi")
	public int getVolume() {
		int volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		VolumeSwipe.log("base volume = "+volume);
		
		if (extraDB == 0 || eq == null || !eq.getEnabled())
			return volume;
		
		try {
			float total = 0;
			int count = 0;
			
			for (short i=0; i<bands; i++) {
				int hz = eq.getCenterFreq((short)i)/1000;
				if (250 <= hz && hz <= 8000) {
					total += eq.getBandLevel(i)/100;
					count++;
					VolumeSwipe.log(""+i+" "+eq.getBandLevel(i));
				}
			}
			
			if (0<count)
				volume += (int)(total/count+.5f);
		}
		catch (UnsupportedOperationException e) {
			Log.e("VolumeSwipe", e.toString());			
		}
		
		VolumeSwipe.log("total volume = "+volume);
		return volume;
	}
	
	@SuppressLint("NewApi")
	void reset() {
		if (extraDB > 0) {
			eq.setEnabled(false);
		}
	}
	
	@SuppressLint("NewApi")
	void destroy() {
		if (eq != null) {
			eq.release();
			eq = null;
		}
	}
}
