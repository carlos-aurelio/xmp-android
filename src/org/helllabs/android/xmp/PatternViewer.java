package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;

// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PatternViewer extends Viewer implements SurfaceHolder.Callback {
	//private Context context;
	private SurfaceHolder surfaceHolder;        
	private int canvasHeight, canvasWidth;
	private Paint headerPaint, headerTextPaint, notePaint, insPaint, barPaint;
	private int fontSize, fontHeight, fontWidth;
	private String[] allNotes = new String[120];
	private String[] hexByte = new String[256];
	private byte[] rowNotes = new byte[64];
	private byte[] rowInstruments = new byte[64];
	private int oldRow, oldOrd, oldDeltaX;
	private int[] modVars;
	
	private final static String[] notes = {
		"C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "
	};

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		synchronized (surfaceHolder) {
			canvasWidth = width;
			canvasHeight = height;
		}
	}
	
	@Override
	public void setup(int[] modVars) {
		this.modVars = modVars;
		
		oldRow = -1;
		oldOrd = -1;
		oldDeltaX = -1;
		
		synchronized (isDown) {
			posX = 0;
		}
	}

	@Override
	public void update(ModInterface modPlayer, Info info) {
		super.update(modPlayer, info);
		int row = info.values[2];
		int ord = info.values[0];
		int numRows = info.values[3];
		
		Canvas c = null;
		
		if (oldRow == row && oldOrd == ord && oldDeltaX == deltaX) {
			return;
		}
		
		if (numRows != 0) {		// Skip first invalid infos
			oldRow = row;
			oldOrd = ord;
			oldDeltaX = deltaX;
		}
		
		try {
			c = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {
				doDraw(c, modPlayer, info);
			}
		} finally {
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (c != null) {
				surfaceHolder.unlockCanvasAndPost(c);
			}
		}
	}

	private void doDraw(Canvas canvas, ModInterface modPlayer, Info info) {
		int lines = canvasHeight / fontHeight;
		int barLine = lines / 2 + 1;
		int barY = barLine * fontHeight;
		int channels = (int)((canvasWidth / fontWidth - 3) / 6);
		int row = info.values[2];
		int pat = info.values[1];
		int chn = modVars[3];
		int numRows = info.values[3];
		Rect rect;
		
		if (channels > chn) {
			channels = chn;
		}

		int biasX;
		
		synchronized (isDown) {
			int max = canvasWidth - (chn * 6 + 2) * fontWidth;
			biasX = deltaX + posX;
			
			if (max > 0) {
				max = 0;
			}

			if (biasX > 0) {
				biasX = posX = 0;
			}
			if (biasX < max) {
				biasX = max;
			}
		}

		// Clear screen
		canvas.drawColor(Color.BLACK);

		// Header
		rect = new Rect(0, 0, canvasWidth - 1, fontHeight - 1);
		canvas.drawRect(rect, headerPaint);
		for (int i = 0; i < chn; i++) {
			int adj = (i + 1) < 10 ? 1 : 0;
			canvas.drawText(Integer.toString(i + 1), biasX + (3 + i * 6 + 1 + adj) * fontWidth, fontSize, headerTextPaint);
		}
		
		// Current line bar
		rect = new Rect(0, barY - fontHeight + 1, canvasWidth - 1, barY);
		canvas.drawRect(rect, barPaint);
		
		// Pattern data
		for (int i = 1; i < lines; i++) {
			int lineInPattern = i + row - barLine + 1; 
			int x, y = (i + 1) * fontHeight;
			
			if (lineInPattern < 0 || lineInPattern >= numRows)
				continue;
			
			canvas.drawText(hexByte[lineInPattern], biasX, y, headerTextPaint);
			
			for (int j = 0; j < chn; j++) {	
				try {
					modPlayer.getPatternRow(pat, lineInPattern, rowNotes, rowInstruments);
				} catch (RemoteException e) { }
					
				x = biasX + (3 + j * 6) * fontWidth;
				if (rowNotes[j] > 0x80) {
					canvas.drawText("===", x, y, notePaint);
				} else if (rowNotes[j] > 0) {
					canvas.drawText(allNotes[rowNotes[j] - 1], x, y, notePaint);
				} else {
					canvas.drawText("---", x, y, notePaint);
				}
				
				x = biasX + (3 + j * 6 + 3) * fontWidth;
				if (rowInstruments[j] > 0) {
					canvas.drawText(hexByte[rowInstruments[j]], x, y, insPaint);
				} else {
					canvas.drawText("--", x, y, insPaint);
				}
			}
		}
	}


	public PatternViewer(Context context) {
		super(context);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		this.surfaceHolder = holder;
		//this.context = context;

		fontSize = getResources().getDimensionPixelSize(R.dimen.patternview_font_size);

		notePaint = new Paint();
		notePaint.setARGB(255, 140, 140, 160);
		notePaint.setTypeface(Typeface.MONOSPACE);
		notePaint.setTextSize(fontSize);
		notePaint.setAntiAlias(true);
		
		insPaint = new Paint();
		insPaint.setARGB(255, 160, 80, 80);
		insPaint.setTypeface(Typeface.MONOSPACE);
		insPaint.setTextSize(fontSize);
		insPaint.setAntiAlias(true);
		
		headerTextPaint = new Paint();
		headerTextPaint.setARGB(255, 220, 220, 220);
		headerTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		headerTextPaint.setTextSize(fontSize);
		headerTextPaint.setAntiAlias(true);
		
		headerPaint = new Paint();
		headerPaint.setARGB(255, 140, 140, 220);
		
		barPaint = new Paint();
		barPaint.setARGB(255, 40, 40, 40);
		
		fontWidth = (int)notePaint.measureText("X");
		fontHeight = fontSize * 12 / 10;
		
		for (int i = 0; i < 120; i++) {
			allNotes[i] = new String(notes[i % 12] + (i / 12));
		}
		for (int i = 0; i < 256; i++) {
			hexByte[i] = new String(String.format("%02X", i));
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {		
		surfaceHolder = holder;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}
}