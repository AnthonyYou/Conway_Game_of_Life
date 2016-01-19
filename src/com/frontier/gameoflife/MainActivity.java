package com.frontier.gameoflife;

import java.util.Vector;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements Callback, OnTouchListener {

	private static final int defaultMapWidth = 160;

	private SurfaceView mainView;
	private SurfaceHolder sh;

	private ImageView settingBtn;
	private TextView generationCount;
	private TextView aliveCount;

	private int totalAliveCount, generations;

	private float cellWidth, cellHeight;
	private int mapWidth = defaultMapWidth;
	private int mapHeight;

	private Paint gridPaint, cellPaint;

	private byte[][] cells;
	private Vector<Point> bornCells;
	private Vector<Point> deadCells;

	private Handler generateHandler;

	private float menuDownX;
	private float menuDownY;
	private float menuStartTranslateX, menuStartTranslateY;

	private Handler mainHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				aliveCount.setText(String.format(
						getString(R.string.alive_count), totalAliveCount));
				generationCount.setText(String.format(
						getString(R.string.generation_count), generations));
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		startGenerateThread();

		gridPaint = new Paint();
		gridPaint.setColor(Color.BLACK);
		cellPaint = new Paint();
		cellPaint.setColor(Color.RED);

		bornCells = new Vector<Point>();
		deadCells = new Vector<Point>();
	}

	@Override
	protected void onDestroy() {
		if (generateHandler != null) {
			generateHandler.sendEmptyMessage(0);
		}
		super.onDestroy();
	}

	private void startGenerateThread() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Looper.prepare();

				generateHandler = new Handler() {

					@Override
					public void handleMessage(Message msg) {
						switch (msg.what) {
						case 0: // exit thread
							Looper.myLooper().quit();
							generateHandler = null;
							break;
						case 1: // next generation
							generate();
							drawWorld();
							generateHandler.sendEmptyMessageDelayed(1, 50);
							break;
						}
					}

				};

				Looper.loop();
			}

		}).start();
	}

	private void initView() {
		mainView = (SurfaceView) findViewById(R.id.main_view);
		sh = mainView.getHolder();
		sh.addCallback(this);

		mainView.setOnTouchListener(this);

		settingBtn = (ImageView) findViewById(R.id.setting_btn);
		settingBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				generateHandler.sendEmptyMessage(1);
			}
			
		});

		aliveCount = (TextView) findViewById(R.id.alive_count);
		aliveCount.setText(String.format(getString(R.string.alive_count), 0));

		generationCount = (TextView) findViewById(R.id.generation_count);
		generationCount.setText(String.format(
				getString(R.string.generation_count), 0));
	}

	private void calcCellWidth() {
		DisplayMetrics dm = getResources().getDisplayMetrics();
		cellWidth = dm.widthPixels / (float) mapWidth;
		mapHeight = dm.heightPixels / (int) cellWidth;
		cellHeight = dm.heightPixels / (float) mapHeight;

		cells = new byte[mapWidth][mapHeight];
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		calcCellWidth();
		drawWorld();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	private void drawWorld() {
		Canvas canvas = null;
		try {
			canvas = sh.lockCanvas();
			drawMapGrid(canvas);
			drawCells(canvas);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (canvas != null) {
				sh.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void drawMapGrid(Canvas canvas) {
		canvas.drawColor(Color.LTGRAY);
		for (int i = 0; i < mapHeight; ++i) {
			canvas.drawLine(0, cellHeight * i, mainView.getMeasuredWidth(),
					cellHeight * i, gridPaint);
		}

		for (int i = 0; i < mapWidth; ++i) {
			canvas.drawLine(cellWidth * i, 0, cellWidth * i,
					mainView.getMeasuredHeight(), gridPaint);
		}
	}

	private void drawCells(Canvas canvas) {
		float left, top, right, bottom;
		canvas.save();
		for (int i = 0; i < mapWidth; ++i) {
			for (int j = 0; j < mapHeight; ++j) {
				left = i * cellWidth + 1;
				top = j * cellHeight + 1;
				right = left + cellWidth - 1;
				bottom = top + cellHeight - 1;
				if (cells[i][j] == 1) {
					canvas.drawRect(left, top, right, bottom, cellPaint);
				}
			}
		}
		canvas.restore();
	}

	private int countAliveNeighbour(int x, int y) {
		int count = 0;
		int nx = x - 1;
		int ny = y - 1;

		// northwest neighbour
		if (nx >= 0 && ny >= 0 && cells[nx][ny] == 1) {
			++count;
		}

		// north neighbour
		nx = x;
		if (ny >= 0 && cells[nx][ny] == 1) {
			++count;
		}

		// northease neighbour
		nx = x + 1;
		if (nx < mapWidth && ny >= 0 && cells[nx][ny] == 1) {
			++count;
		}

		// west neighbour
		nx = x - 1;
		ny = y;
		if (nx >= 0 && cells[nx][ny] == 1) {
			++count;
		}

		// east neighbour
		nx = x + 1;
		if (nx < mapWidth && cells[nx][ny] == 1) {
			++count;
		}

		nx = x - 1;
		ny = y + 1;
		// southwest neighbour
		if (nx >= 0 && ny < mapHeight && cells[nx][ny] == 1) {
			++count;
		}

		nx = x;
		if (ny < mapHeight && cells[nx][ny] == 1) {
			++count;
		}

		nx = x + 1;
		if (nx < mapWidth && ny < mapHeight && cells[nx][ny] == 1) {
			++count;
		}

		return count;
	}

	private void generate() {
		bornCells.clear();
		deadCells.clear();
		for (int i = 0; i < mapWidth; ++i) {
			for (int j = 0; j < mapHeight; ++j) {
				int neighbours = countAliveNeighbour(i, j);
				if (cells[i][j] == 0) { // now dead
					if (neighbours == 3) {
						bornCells.add(new Point(i, j));
					}
				} else { // now alive
					if (neighbours < 2 || neighbours > 3) {
						deadCells.add(new Point(i, j));
					}
				}
			}
		}
		for (Point point : bornCells) {
			cells[point.x][point.y] = 1;
		}

		for (Point point : deadCells) {
			cells[point.x][point.y] = 0;
		}
		totalAliveCount += bornCells.size() - deadCells.size();
		++generations;
		mainHandler.sendEmptyMessage(0);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			int mapX = (int) (event.getX() / cellWidth);
			int mapY = (int) (event.getY() / cellHeight);
			if (mapX >= 0 && mapX < mapWidth && mapY > 0 && mapY < mapHeight) {
				if (cells[mapX][mapY] == 0) {
					++totalAliveCount;
					mainHandler.sendEmptyMessage(0);
				}
				cells[mapX][mapY] = 1;
				drawWorld();
			}
			break;
		}
		return true;
	}
}
