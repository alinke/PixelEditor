package com.ledpixelart.pixelesque.shapes;

import java.util.ArrayList;

import processing.core.PApplet;
import android.graphics.Point;
import android.util.Log;

import com.ledpixelart.pixelesque.PixelArt;
import com.rj.processing.mt.Cursor;

public abstract class Shape {
	public ArrayList<Point> selectedPoints = new ArrayList<Point>();
	public PixelArt art;
	public Cursor cursor;
	public int color;
	public PApplet p;
	public boolean done = false;
	public boolean isCalculating = false;
	public boolean commitOnFinish = false;
	
	boolean highlightCursorStart = false;
	boolean highlightCursorEnd = true;
	
	public Shape(PApplet p, PixelArt art, Cursor c, int color) {
		this(p,art,c);
		this.color = color;
	}
	public Shape(PApplet p, PixelArt art, Cursor c) {
		this.p = p;
		this.art = art;
		this.cursor = c;
	}
	
	/**
	 * This is teh default implementation, but I can think of plenty of times when this isn't the correct way.
	 */
	public void update() {
		if (done) {
			selectedPoints.clear();
			return;
		}
		if (selectedPoints.size() > 0) {
			Point point = selectedPoints.get(selectedPoints.size()-1);
			int[] coords = art.getDataCoordsFromXY(p, cursor.currentPoint.x, cursor.currentPoint.y);
			point.x = coords[0];
			point.y = coords[1];
		} else {
			Point point = new Point();
			int[] coords = art.getDataCoordsFromXY(p, cursor.currentPoint.x, cursor.currentPoint.y);
			point.x = coords[0];
			point.y = coords[1];
			selectedPoints.add(point);
		}
			
	}
	public boolean commit() {
		done = true;
		if (!isCalculating) {
			commitOnFinish = false;
			return true;
		} else {
			commitOnFinish = true;
			art.canvasLock();
			return false;
		}
	}
	public void cancel() {
		done = true;
	}
	
	public void lockCalculatingBrush() {
		isCalculating = true;
	}
	
	public void unlockCalculatingBrush() {
		isCalculating = false;
		if (commitOnFinish) {
			art.canvasUnlock();
			commit();
		}
	}
	
	
	public void setAllPoints() {
		if (cursor == null || art == null) return;
		for (Point point : selectedPoints) {
			art.setColor(point.x, point.y, this.color, false);
		}
		art.history.add();
		//Log.d("SHAPE", art.dumpBoard());
	}
	
	public ArrayList<Point> getSelectedPoints() {
		return selectedPoints;
	}
	
	
	public void draw(PApplet p, PixelArt pix, float topx, float topy, float boxsize) {
		if (done) return;
		float extra = 30;
		
		for (Point coords : this.getSelectedPoints()) {
			int x = coords.x; int y = coords.y;
			if (art.isValid(x,y)) {
				p.fill(255,255,255,80);
				p.rect(topx + boxsize * x, topy + boxsize * y, boxsize, boxsize);
			}
		}
		if (highlightCursorStart) {
			int[] startcoords = art.getDataCoordsFromXY(p, cursor.firstPoint.x, cursor.firstPoint.y);
			int x = startcoords[0]; int y = startcoords[1];
			if (art.isValid(x,y)) {
				p.fill(255,255,255,80);
				p.rect(topx + boxsize * x - extra, topy + boxsize * y - extra, boxsize + extra*2, boxsize + extra*2);
			}
		}
		if (highlightCursorEnd) {
			int[] endcoords = art.getDataCoordsFromXY(p, cursor.currentPoint.x, cursor.currentPoint.y);
			int x = endcoords[0]; int y = endcoords[1];
			if (art.isValid(x,y)) {
				p.fill(255,255,255,80);
				p.rect(topx + boxsize * x - extra, topy + boxsize * y - extra, boxsize + extra*2, boxsize + extra*2);
			}
		}
	}
}
