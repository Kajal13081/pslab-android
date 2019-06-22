package io.pslab.activity;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.pslab.R;
import io.pslab.communication.AnalyticsClass;
import io.pslab.communication.ScienceLab;
import io.pslab.fragment.ChannelParametersFragment;
import io.pslab.fragment.DataAnalysisFragment;
import io.pslab.fragment.OscilloscopePlaybackFragment;
import io.pslab.fragment.TimebaseTriggerFragment;
import io.pslab.fragment.XYPlotFragment;
import io.pslab.models.OscilloscopeData;
import io.pslab.models.SensorDataBlock;
import io.pslab.others.AudioJack;
import io.pslab.others.CSVLogger;
import io.pslab.others.CustomSnackBar;
import io.pslab.others.GPSLogger;
import io.pslab.others.LocalDataLog;
import io.pslab.others.MathUtils;
import io.pslab.others.Plot2D;
import io.pslab.others.ScienceLabCommon;
import io.pslab.others.SwipeGestureDetector;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

import static io.pslab.others.MathUtils.map;

/**
 * Created by viveksb007 on 10/5/17.
 */

public class OscilloscopeActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String PREF_NAME = "OscilloscopeActivity";
    private final Object lock = new Object();
    @BindView(R.id.chart_os)
    public LineChart mChart;
    @BindView(R.id.tv_label_left_yaxis_os)
    public TextView leftYAxisLabel;
    @BindView(R.id.tv_unit_left_yaxis_os)
    public TextView leftYAxisLabelUnit;
    @BindView(R.id.tv_label_right_yaxis_os)
    public TextView rightYAxisLabel;
    @BindView(R.id.tv_unit_right_yaxis_os)
    public TextView rightYAxisLabelUnit;
    @BindView(R.id.tv_graph_label_xaxis_os)
    public TextView xAxisLabel;
    @BindView(R.id.tv_unit_xaxis_os)
    public TextView xAxisLabelUnit;
    public int samples;
    public double timeGap;
    public double timebase;
    public double xAxisScale = 875f;
    public boolean isCH1Selected;
    public boolean isCH2Selected;
    public boolean isCH3Selected;
    public boolean isMICSelected;
    public boolean isInBuiltMicSelected;
    public boolean isTriggerSelected;
    public boolean isFourierTransformSelected;
    public boolean isXYPlotSelected;
    public boolean sineFit;
    public boolean squareFit;
    public boolean viewIsClicked;
    public boolean isCH1FrequencyRequired;
    public boolean isCH2FrequencyRequired;
    public String triggerChannel;
    public String curveFittingChannel1;
    public String curveFittingChannel2;
    public String xyPlotXAxisChannel;
    public String xyPlotYAxisChannel;
    public double trigger;
    public Plot2D graph;
    @BindView(R.id.layout_dock_os1)
    LinearLayout linearLayout;
    @BindView(R.id.layout_dock_os2)
    FrameLayout frameLayout;
    @BindView(R.id.layout_chart_os)
    RelativeLayout mChartLayout;
    @BindView(R.id.button_channel_parameters_os)
    ImageButton channelParametersButton;
    @BindView(R.id.button_timebase_os)
    ImageButton timebaseButton;
    @BindView(R.id.button_data_analysis_os)
    ImageButton dataAnalysisButton;
    @BindView(R.id.button_xy_plot_os)
    ImageButton xyPlotButton;
    @BindView(R.id.tv_channel_parameters_os)
    TextView channelParametersTextView;
    @BindView(R.id.tv_timebase_tigger_os)
    TextView timebaseTiggerTextView;
    @BindView(R.id.tv_data_analysis_os)
    TextView dataAnalysisTextView;
    @BindView(R.id.tv_xy_plot_os)
    TextView xyPlotTextView;
    @BindView(R.id.img_arrow_oscilloscope)
    ImageView arrowUpDown;
    @BindView(R.id.sheet_slide_text_oscilloscope)
    TextView bottomSheetSlideText;
    @BindView(R.id.parent_layout)
    View parentLayout;
    @BindView(R.id.bottom_sheet_oscilloscope)
    LinearLayout bottomSheet;
    private Fragment channelParametersFragment;
    private Fragment timebaseTriggerFragment;
    private Fragment dataAnalysisFragment;
    private Fragment xyPlotFragment;
    private Fragment playbackFragment;
    @BindView(R.id.imageView_led_os)
    ImageView ledImageView;
    @BindView(R.id.show_guide_oscilloscope)
    TextView showText;
    private ScienceLab scienceLab;
    private int height;
    private int width;
    private XAxis x1;
    private YAxis y1;
    private YAxis y2;
    private CaptureTask captureTask;
    private CaptureTaskTwo captureTask2;
    private CaptureTaskThree captureTask3;
    private XYPlotTask xyPlotTask;
    private AudioJack audioJack = null;
    private AnalyticsClass analyticsClass;
    private CaptureAudioBuffer captureAudioBuffer;
    private CaptureAudioBufferTwo captureAudioBufferTwo;
    private Thread monitorThread;
    private volatile boolean monitor = true;
    private BottomSheetBehavior bottomSheetBehavior;
    private GestureDetector gestureDetector;
    private boolean btnLongpressed;
    private double maxAmp, maxFreq;
    private ImageView recordButton;
    private boolean isRecording = false;
    private Realm realm;
    public RealmResults<OscilloscopeData> recordedOscilloscopeData;
    private CSVLogger csvLogger;
    private GPSLogger gpsLogger;
    private long block;
    private Timer recordTimer;
    private long recordPeriod = 100;
    private String oscilloscopeCSVHeader = "Timestamp,DateTime,Channel,xData,yData,Timebase,lat,lon";
    private String loggingXdata = "";
    private String loggingYdata1 = "";
    private String loggingYdata2 = "";
    private final String KEY_LOG = "has_log";
    private final String DATA_BLOCK = "data_block";
    private int currentPosition = 0;
    private Timer playbackTimer;
    private View mainLayout;
    private double lat;
    private double lon;
    public boolean isPlaybackFourierChecked = false;

    private enum CHANNEL {CH1, CH2, CH3, MIC}

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_oscilloscope);
        ButterKnife.bind(this);

        removeStatusBar();
        setUpBottomSheet();
        parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                parentLayout.setVisibility(View.GONE);
            }
        });
        mainLayout = findViewById(R.id.oscilloscope_mail_layout);

        realm = LocalDataLog.with().getRealm();
        gpsLogger = new GPSLogger(this,
                (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        csvLogger = new CSVLogger(getString(R.string.oscilloscope));

        recordButton = findViewById(R.id.oscilloscope_record_button);

        scienceLab = ScienceLabCommon.scienceLab;
        x1 = mChart.getXAxis();
        y1 = mChart.getAxisLeft();
        y2 = mChart.getAxisRight();
        triggerChannel = CHANNEL.CH1.toString();
        trigger = 0;
        timebase = 875;
        samples = 512;
        timeGap = 2;
        sineFit = true;
        squareFit = false;
        graph = new Plot2D(this, new float[]{}, new float[]{}, 1);
        curveFittingChannel1 = "None";
        curveFittingChannel2 = "None";
        xyPlotXAxisChannel = CHANNEL.CH1.toString();
        xyPlotYAxisChannel = CHANNEL.CH2.toString();
        viewIsClicked = false;
        analyticsClass = new AnalyticsClass();
        isCH1FrequencyRequired = false;
        isCH2FrequencyRequired = false;

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        onWindowFocusChanged();

        channelParametersFragment = new ChannelParametersFragment();
        timebaseTriggerFragment = new TimebaseTriggerFragment();
        dataAnalysisFragment = new DataAnalysisFragment();
        xyPlotFragment = new XYPlotFragment();
        playbackFragment = new OscilloscopePlaybackFragment();

        if (findViewById(R.id.layout_dock_os2) != null) {
            addFragment(R.id.layout_dock_os2, channelParametersFragment);
        }

        channelParametersButton.setOnClickListener(this);
        timebaseButton.setOnClickListener(this);
        dataAnalysisButton.setOnClickListener(this);
        xyPlotButton.setOnClickListener(this);
        channelParametersTextView.setOnClickListener(this);
        timebaseTiggerTextView.setOnClickListener(this);
        dataAnalysisTextView.setOnClickListener(this);
        xyPlotTextView.setOnClickListener(this);

        chartInit();

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                //Thread to check which checkbox is enabled
                while (monitor) {
                    if (scienceLab.isConnected() && isCH1Selected && !isCH2Selected && !isCH3Selected && !isMICSelected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask = new CaptureTask();
                        captureTask.execute(CHANNEL.CH1.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isCH2Selected && !isCH1Selected && !isCH3Selected && !isMICSelected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask = new CaptureTask();
                        captureTask.execute(CHANNEL.CH2.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isCH3Selected && !isCH1Selected && !isCH2Selected && !isMICSelected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask = new CaptureTask();
                        captureTask.execute(CHANNEL.CH3.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isMICSelected && !isCH1Selected && !isCH2Selected && !isCH3Selected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask = new CaptureTask();
                        captureTask.execute(CHANNEL.MIC.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isCH1Selected && isCH2Selected && !isCH3Selected && !isMICSelected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask2 = new CaptureTaskTwo();
                        captureTask2.execute(CHANNEL.CH1.toString(), CHANNEL.CH2.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isCH3Selected && isCH2Selected && !isCH1Selected && !isMICSelected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask2 = new CaptureTaskTwo();
                        captureTask2.execute(CHANNEL.CH1.toString(), CHANNEL.CH3.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isMICSelected && isCH2Selected && !isCH3Selected && !isCH1Selected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask2 = new CaptureTaskTwo();
                        captureTask2.execute(CHANNEL.CH1.toString(), CHANNEL.MIC.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isCH2Selected && isCH3Selected && !isCH1Selected && !isMICSelected && !isXYPlotSelected && !isInBuiltMicSelected) {
                        captureTask2 = new CaptureTaskTwo();
                        captureTask2.execute(CHANNEL.CH2.toString(), CHANNEL.CH3.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isCH2Selected && isMICSelected && !isCH3Selected && !isCH1Selected && !isXYPlotSelected && !isInBuiltMicSelected) {
                        captureTask2 = new CaptureTaskTwo();
                        captureTask2.execute(CHANNEL.CH2.toString(), CHANNEL.MIC.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isCH1Selected && isCH2Selected && isCH3Selected && isMICSelected && !isXYPlotSelected && !isFourierTransformSelected && !isInBuiltMicSelected) {
                        captureTask3 = new CaptureTaskThree();
                        captureTask3.execute(CHANNEL.CH1.toString());
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (!scienceLab.isConnected() || (!isCH1Selected && !isCH2Selected && !isCH3Selected && !isMICSelected)) {
                        if (!String.valueOf(ledImageView.getTag()).equals("red")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ledImageView.setImageResource(R.drawable.red_led);
                                    ledImageView.setTag("red");
                                }
                            });
                        }
                    }

                    if (scienceLab.isConnected() && viewIsClicked && isXYPlotSelected) {
                        xyPlotTask = new XYPlotTask();
                        if (xyPlotXAxisChannel.equals(CHANNEL.CH2.toString()))
                            xyPlotTask.execute(xyPlotYAxisChannel);
                        else
                            xyPlotTask.execute(xyPlotXAxisChannel);
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (scienceLab.isConnected() && isFourierTransformSelected) {
                        if (isCH1Selected && !isCH2Selected) {
                            new FFTTask().execute(CHANNEL.CH1.toString());
                            synchronized (lock) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (!isCH1Selected && isCH2Selected) {
                            new FFTTask().execute(CHANNEL.CH2.toString());
                            synchronized (lock) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (isCH1Selected && isCH2Selected) {
                            new FFTTaskTwo().execute(CHANNEL.CH1.toString(), CHANNEL.CH2.toString());
                            synchronized (lock) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    if (isInBuiltMicSelected) {
                        if (audioJack == null)
                            audioJack = new AudioJack("input");
                        if (!isCH1Selected && !isCH2Selected && !isCH3Selected) {
                            captureAudioBuffer = new CaptureAudioBuffer(audioJack);
                            captureAudioBuffer.execute();
                            synchronized (lock) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (isCH1Selected && !isCH2Selected && !isCH3Selected) {
                            captureAudioBufferTwo = new CaptureAudioBufferTwo(audioJack, CHANNEL.CH1.toString());
                            captureAudioBufferTwo.execute();
                            synchronized (lock) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (!isCH1Selected && isCH2Selected && !isCH3Selected) {
                            captureAudioBufferTwo = new CaptureAudioBufferTwo(audioJack, CHANNEL.CH2.toString());
                            captureAudioBufferTwo.execute();
                            synchronized (lock) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (!isCH1Selected && !isCH2Selected && isCH3Selected) {
                            captureAudioBufferTwo = new CaptureAudioBufferTwo(audioJack, CHANNEL.CH3.toString());
                            captureAudioBufferTwo.execute();
                            synchronized (lock) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        if (audioJack != null) {
                            audioJack.release();
                            audioJack = null;
                        }
                    }

                }
            }
        };
        monitorThread = new Thread(runnable);
        monitorThread.start();

        ImageView guideImageView = findViewById(R.id.oscilloscope_guide_button);
        guideImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN ?
                        BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        guideImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showText.setVisibility(View.VISIBLE);
                btnLongpressed = true;
                return true;
            }
        });
        guideImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (btnLongpressed) {
                        showText.setVisibility(View.GONE);
                        btnLongpressed = false;
                    }
                }
                return true;
            }
        });

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    isRecording = false;
                    recordButton.setImageResource(R.drawable.ic_record_white);
                    CustomSnackBar.showSnackBar(mainLayout,
                            getString(R.string.csv_store_text) + " " + csvLogger.getCurrentFilePath()
                            , getString(R.string.open), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(OscilloscopeActivity.this, DataLoggerActivity.class);
                                    intent.putExtra(DataLoggerActivity.CALLER_ACTIVITY, getResources().getString(R.string.oscilloscope));
                                    startActivity(intent);
                                }
                            }, Snackbar.LENGTH_SHORT);
                } else {
                    isRecording = true;
                    recordButton.setImageResource(R.drawable.ic_record_stop_white);
                    block = System.currentTimeMillis();
                    if (gpsLogger.isGPSEnabled()) {
                        Location location = gpsLogger.getDeviceLocation();
                        if (location != null) {
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                        } else {
                            lat = 0.0;
                            lon = 0.0;
                        }
                    } else {
                        lat = 0.0;
                        lon = 0.0;
                    }
                    csvLogger = new CSVLogger(getResources().getString(R.string.oscilloscope));
                    csvLogger.prepareLogFile();
                    csvLogger.writeCSVFile(oscilloscopeCSVHeader);
                    recordSensorDataBlockID(new SensorDataBlock(block, getResources().getString(R.string.oscilloscope)));
                    CustomSnackBar.showSnackBar(mainLayout, getString(R.string.data_recording_start), null, null, Snackbar.LENGTH_SHORT);
                }
            }
        });

        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean(KEY_LOG)) {
            recordedOscilloscopeData = LocalDataLog.with()
                    .getBlockOfOscilloscopeRecords(getIntent().getExtras().getLong(DATA_BLOCK));
            setLayoutForPlayback();
        }
    }

    private void setLayoutForPlayback() {
        findViewById(R.id.layout_dock_os1).setVisibility(View.GONE);
        recordButton.setVisibility(View.GONE);
        RelativeLayout.LayoutParams lineChartParams = (RelativeLayout.LayoutParams) mChartLayout.getLayoutParams();
        RelativeLayout.LayoutParams frameLayoutParams = (RelativeLayout.LayoutParams) frameLayout.getLayoutParams();
        lineChartParams.height = height * 4 / 5;
        lineChartParams.width = width;
        mChartLayout.setLayoutParams(lineChartParams);
        frameLayoutParams.height = height / 5;
        frameLayoutParams.width = width;
        frameLayout.setLayoutParams(frameLayoutParams);
        replaceFragment(R.id.layout_dock_os2, playbackFragment, "Playback Fragment");
    }

    public void playRecordedData() {
        final Handler handler = new Handler();
        if (playbackTimer == null) {
            playbackTimer = new Timer();
        }
        playbackTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (currentPosition < recordedOscilloscopeData.size()) {
                                OscilloscopeData data = recordedOscilloscopeData.get(currentPosition);
                                if (data.getMode() == 1) {
                                    currentPosition += 1;
                                    ArrayList<Entry> entries = new ArrayList<>();
                                    String[] xData = data.getDataX().split(" ");
                                    String[] yData = data.getDataY().split(" ");

                                    if (!isPlaybackFourierChecked) {
                                        int n = Math.min(xData.length, yData.length);
                                        for (int i = 0; i < n; i++) {
                                            if (xData[i].length() > 0 && yData[i].length() > 0) {
                                                entries.add(new Entry(Float.valueOf(xData[i]), Float.valueOf(yData[i])));
                                            }
                                        }
                                        setLeftYAxisScale(16f, -16f);
                                        setRightYAxisScale(16f, -16f);
                                        setXAxisScale(data.getTimebase());
                                    } else {
                                        Complex[] yComplex = new Complex[yData.length];
                                        for (int i = 0; i < yData.length; i++) {
                                            yComplex[i] = Complex.valueOf(Double.valueOf(yData[i]));
                                        }
                                        Complex[] fftOut = fft(yComplex);
                                        int n = fftOut.length;
                                        double mA = 0;
                                        double factor = samples * timeGap * 1e-3;
                                        double mF = (n / 2 - 1) / factor;
                                        for (int i = 0; i < n / 2; i++) {
                                            float y = (float) fftOut[i].abs() / samples;
                                            if (y > mA) {
                                                mA = y;
                                            }
                                            entries.add(new Entry((float) (i / factor), y));
                                        }
                                        setLeftYAxisScale(mA, 0);
                                        setRightYAxisScale(mA, 0);
                                        setXAxisScale(mF);
                                    }
                                    LineDataSet dataSet = new LineDataSet(entries, data.getChannel());
                                    LineData lineData = new LineData(dataSet);
                                    dataSet.setDrawCircles(false);
                                    mChart.setData(lineData);
                                    mChart.notifyDataSetChanged();
                                    mChart.invalidate();

                                    ((OscilloscopePlaybackFragment) playbackFragment).setTimeBase(String.valueOf(data.getTimebase()));
                                } else if (data.getMode() == 2) {
                                    OscilloscopeData data2 = recordedOscilloscopeData.get(currentPosition + 1);
                                    currentPosition += 2;
                                    ArrayList<Entry> entries1 = new ArrayList<>();
                                    ArrayList<Entry> entries2 = new ArrayList<>();
                                    String[] xData = data.getDataX().split(" ");
                                    String[] yData1 = data.getDataY().split(" ");
                                    String[] yData2 = data2.getDataY().split(" ");

                                    if (!isPlaybackFourierChecked) {
                                        int n = Math.min(xData.length, Math.min(yData1.length, yData2.length));
                                        for (int i = 0; i < n; i++) {
                                            if (xData[i].length() > 0 && yData1[i].length() > 0 && yData2[i].length() > 0) {
                                                entries1.add(new Entry(Float.valueOf(xData[i]), Float.valueOf(yData1[i])));
                                                entries2.add(new Entry(Float.valueOf(xData[i]), Float.valueOf(yData2[i])));
                                            }
                                        }

                                        setLeftYAxisScale(16f, -16f);
                                        setRightYAxisScale(16f, -16f);
                                        setXAxisScale(data.getTimebase());
                                    } else {
                                        Complex[] yComplex1 = new Complex[yData1.length];
                                        Complex[] yComplex2 = new Complex[yData2.length];
                                        for (int i = 0; i < Math.min(yData1.length, yData2.length); i++) {
                                            yComplex1[i] = Complex.valueOf(Double.valueOf(yData1[i]));
                                            yComplex2[i] = Complex.valueOf(Double.valueOf(yData2[i]));
                                        }
                                        Complex[] fftOut1 = fft(yComplex1);
                                        Complex[] fftOut2 = fft(yComplex2);
                                        int n = Math.min(fftOut1.length, fftOut2.length);
                                        double mA = 0;

                                        float maxAmp1 = 0;
                                        float maxAmp2 = 0;
                                        double factor = samples * timeGap * 1e-3;
                                        double mF = (n / 2 - 1) / factor;
                                        for (int i = 0; i < n / 2; i++) {
                                            float y1 = (float) fftOut1[i].abs() / samples;
                                            if (y1 > maxAmp1) {
                                                maxAmp1 = y1;
                                            }
                                            entries1.add(new Entry((float) (i / factor), y1));
                                            float y2 = (float) fftOut2[i].abs() / samples;
                                            if (y2 > maxAmp2) {
                                                maxAmp2 = y2;
                                            }
                                            entries2.add(new Entry((float) (i / factor), y2));
                                        }
                                        mA = Math.max(maxAmp1, maxAmp2);
                                        setXAxisScale(mF);
                                        setLeftYAxisScale(mA, 0);
                                        setRightYAxisScale(mA, 0);
                                    }
                                    LineDataSet dataSet1 = new LineDataSet(entries1, data.getChannel());
                                    LineDataSet dataSet2 = new LineDataSet(entries2, data2.getChannel());
                                    dataSet1.setDrawCircles(false);
                                    dataSet2.setDrawCircles(false);
                                    dataSet2.setColor(Color.GREEN);
                                    dataSet2.setDrawCircles(false);
                                    List<ILineDataSet> dataSets = new ArrayList<>();
                                    dataSets.add(dataSet1);
                                    dataSets.add(dataSet2);

                                    LineData lineData = new LineData(dataSets);
                                    mChart.setData(lineData);
                                    mChart.notifyDataSetChanged();
                                    mChart.invalidate();

                                    ((OscilloscopePlaybackFragment) playbackFragment).setTimeBase(String.valueOf(data.getTimebase()));
                                }
                            } else {
                                playbackTimer.cancel();
                                playbackTimer = null;
                                ((OscilloscopePlaybackFragment) playbackFragment).resetPlayButton();
                                currentPosition = 0;
                            }
                        } catch (Exception e) {
                            playbackTimer.cancel();
                            playbackTimer = null;
                            ((OscilloscopePlaybackFragment) playbackFragment).resetPlayButton();
                            currentPosition = 0;
                        }
                    }
                });

            }
        }, 0, recordPeriod);
    }

    public void pauseData() {
        playbackTimer.cancel();
        playbackTimer = null;
    }

    private void logSingleChannelData(String channel) {
        long timestamp = System.currentTimeMillis();
        if (loggingXdata.length() > 0 && loggingYdata1.length() > 0) {
            recordSensorData(new OscilloscopeData(timestamp, block, 1, channel, loggingXdata, loggingYdata1, xAxisScale, lat, lon));
            String timeData = timestamp + "," + CSVLogger.FILE_NAME_FORMAT.format(new Date(timestamp));
            String locationData = lat + "," + lon;
            String data = timeData + "," + channel + "," + loggingXdata + "," + loggingYdata1 + "," + xAxisScale + "," + locationData;
            csvLogger.writeCSVFile(data);
        }
    }

    private void logTwoChannelData(String channel1, String channel2) {
        long timestamp = System.currentTimeMillis();
        if (loggingXdata.length() > 0 && loggingYdata1.length() > 0 && loggingYdata2.length() > 0) {
            recordSensorData(new OscilloscopeData(timestamp, block, 2, channel1, loggingXdata, loggingYdata1, xAxisScale, lat, lon));
            recordSensorData(new OscilloscopeData(timestamp + 1, block, 2, channel2, loggingXdata, loggingYdata2, xAxisScale, lat, lon));
            String timeData = timestamp + "," + CSVLogger.FILE_NAME_FORMAT.format(new Date(timestamp));
            String locationData = lat + "," + lon;
            String data = timeData + "," + channel1 + "," + loggingXdata + "," + loggingYdata1 + "," + xAxisScale + "," + locationData;
            csvLogger.writeCSVFile(data);
            data = timeData + "," + channel2 + "," + loggingXdata + "," + loggingYdata2 + "," + xAxisScale + "," + locationData;
            csvLogger.writeCSVFile(data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        removeStatusBar();
    }

    private void removeStatusBar() {
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();

            decorView.setSystemUiVisibility((View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_channel_parameters_os:
            case R.id.tv_channel_parameters_os:
                replaceFragment(R.id.layout_dock_os2, channelParametersFragment, "ChannelParametersFragment");
                clearTextBackgroundColor();
                channelParametersTextView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                break;

            case R.id.button_timebase_os:
            case R.id.tv_timebase_tigger_os:
                replaceFragment(R.id.layout_dock_os2, timebaseTriggerFragment, "TimebaseTiggerFragment");
                clearTextBackgroundColor();
                timebaseTiggerTextView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                break;

            case R.id.button_data_analysis_os:
            case R.id.tv_data_analysis_os:
                replaceFragment(R.id.layout_dock_os2, dataAnalysisFragment, "DataAnalysisFragment");
                clearTextBackgroundColor();
                dataAnalysisTextView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                break;

            case R.id.button_xy_plot_os:
            case R.id.tv_xy_plot_os:
                replaceFragment(R.id.layout_dock_os2, xyPlotFragment, "XYPlotFragment");
                clearTextBackgroundColor();
                xyPlotTextView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                break;

            default:
                break;
        }
    }

    @SuppressLint("ResourceType")
    private void clearTextBackgroundColor() {
        channelParametersTextView.setBackgroundColor(getResources().getColor(R.color.customBorderFill));
        timebaseTiggerTextView.setBackgroundColor(getResources().getColor(R.color.customBorderFill));
        dataAnalysisTextView.setBackgroundColor(getResources().getColor(R.color.customBorderFill));
        xyPlotTextView.setBackgroundColor(getResources().getColor(R.color.customBorderFill));
    }

    public void onWindowFocusChanged() {
        RelativeLayout.LayoutParams lineChartParams = (RelativeLayout.LayoutParams) mChartLayout.getLayoutParams();
        RelativeLayout.LayoutParams frameLayoutParams = (RelativeLayout.LayoutParams) frameLayout.getLayoutParams();
        if (getResources().getBoolean(R.bool.isTablet)) {
            lineChartParams.height = height * 3 / 4;
            lineChartParams.width = width * 7 / 8;
            mChartLayout.setLayoutParams(lineChartParams);
            frameLayoutParams.height = height / 4;
            frameLayoutParams.width = width * 7 / 8;
            frameLayout.setLayoutParams(frameLayoutParams);
        } else {
            lineChartParams.height = height * 2 / 3;
            lineChartParams.width = width * 5 / 6;
            mChartLayout.setLayoutParams(lineChartParams);
            frameLayoutParams.height = height / 3;
            frameLayoutParams.width = width * 5 / 6;
            frameLayout.setLayoutParams(frameLayoutParams);
        }
    }

    protected void addFragment(@IdRes int containerViewId,
                               @NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(containerViewId, fragment, "ChannelParametersFragment").commit();
    }

    protected void replaceFragment(@IdRes int containerViewId,
                                   @NonNull Fragment fragment,
                                   @NonNull String fragmentTag) {
        getSupportFragmentManager().beginTransaction()
                .replace(containerViewId, fragment, fragmentTag).commit();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        monitor = false;
        if (captureTask != null) {
            captureTask.cancel(true);
        }
        if (captureTask2 != null) {
            captureTask2.cancel(true);
        }
        if (captureTask3 != null) {
            captureTask3.cancel(true);
        }
        if (captureAudioBuffer != null) {
            captureAudioBuffer.cancel(true);
            if (audioJack != null) {
                audioJack.release();
            }
        }
        if (recordTimer != null) {
            recordTimer.cancel();
            recordTimer = null;
        }
        super.onDestroy();
    }

    public void chartInit() {
        mChart.setTouchEnabled(true);
        mChart.setHighlightPerDragEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(true);
        mChart.setScaleYEnabled(false);
        mChart.setBackgroundColor(Color.BLACK);
        mChart.getDescription().setEnabled(false);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(true);
        x1.setAvoidFirstLastClipping(true);
        x1.setAxisMinimum(0f);
        x1.setAxisMaximum(875f);

        y1.setTextColor(Color.WHITE);
        y1.setAxisMaximum(16f);
        y1.setAxisMinimum(-16f);
        y1.setDrawGridLines(true);

        y2.setAxisMaximum(16f);
        y2.setAxisMinimum(-16f);
        y2.setTextColor(Color.WHITE);
        y2.setEnabled(true);
    }

    public void setXAxisScale(double timebase) {
        x1.setAxisMinimum(0);
        x1.setAxisMaximum((float) timebase);
        if (timebase == 875f)
            xAxisLabelUnit.setText("(μs)");
        else
            xAxisLabelUnit.setText("(ms)");

        this.timebase = timebase;
        mChart.fitScreen();
        mChart.invalidate();
    }

    public void setLeftYAxisScale(double upperLimit, double lowerLimit) {
        y1.setAxisMaximum((float) upperLimit);
        y1.setAxisMinimum((float) lowerLimit);
        if (upperLimit == 500f)
            leftYAxisLabelUnit.setText("(mV)");
        else
            leftYAxisLabelUnit.setText("(V)");
        mChart.fitScreen();
        mChart.invalidate();
    }

    public void setRightYAxisScale(double upperLimit, double lowerLimit) {
        y2.setAxisMaximum((float) upperLimit);
        y2.setAxisMinimum((float) lowerLimit);
        if (upperLimit == 500f)
            rightYAxisLabelUnit.setText("(mV)");
        else
            rightYAxisLabelUnit.setText("(V)");
        mChart.fitScreen();
        mChart.invalidate();
    }

    public void setLeftYAxisLabel(String leftYAxisInput) {
        leftYAxisLabel.setText(leftYAxisInput);
    }

    public void setXAxisLabel(String xAxisInput) {
        xAxisLabel.setText(xAxisInput);
    }

    private void setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        final SharedPreferences settings = this.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Boolean isFirstTime = settings.getBoolean("OscilloscopeFirstTime", true);

        if (isFirstTime) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            parentLayout.setVisibility(View.VISIBLE);
            parentLayout.setAlpha(0.8f);
            arrowUpDown.setRotation(180);
            bottomSheetSlideText.setText(R.string.hide_guide_text);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("OscilloscopeFirstTime", false);
            editor.apply();
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private Handler handler = new Handler();
            private Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            };

            @Override
            public void onStateChanged(@NonNull final View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        handler.removeCallbacks(runnable);
                        bottomSheetSlideText.setText(R.string.hide_guide_text);
                        break;

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        handler.postDelayed(runnable, 2000);
                        break;

                    default:
                        handler.removeCallbacks(runnable);
                        bottomSheetSlideText.setText(R.string.show_guide_text);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Float value = (float) MathUtils.map((double) slideOffset, 0.0, 1.0, 0.0, 0.8);
                parentLayout.setVisibility(View.VISIBLE);
                parentLayout.setAlpha(value);
                arrowUpDown.setRotation(slideOffset * 180);
            }
        });
        gestureDetector = new GestureDetector(this, new SwipeGestureDetector(bottomSheetBehavior));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);                 //Gesture detector need this to transfer touch event to the gesture detector.
        return super.onTouchEvent(event);
    }

    public class CaptureTask extends AsyncTask<String, Void, Void> {
        ArrayList<Entry> entries;
        String analogInput;

        @Override
        protected Void doInBackground(String... params) {
            try {
                analogInput = params[0];
                if (isTriggerSelected) {
                    scienceLab.configureTrigger(0, analogInput, trigger, null, null);
                }
                // number of samples and timeGap still need to be determined
                scienceLab.captureTraces(1, samples, timeGap, analogInput, isTriggerSelected, null);
                Log.v("Sleep Time", "" + (1024 * 10 * 1e-3));
                Thread.sleep((long) (1000 * 10 * 1e-3));
                HashMap<String, double[]> data = scienceLab.fetchTrace(1); //fetching data

                double[] xData = data.get("x");
                double[] yData = data.get("y");

                String[] xString = new String[xData.length];
                String[] yString = new String[yData.length];
                entries = new ArrayList<>();
                for (int i = 0; i < xData.length; i++) {
                    xData[i] = xData[i] / ((timebase == 875) ? 1 : 1000);
                    entries.add(new Entry((float) xData[i], (float) yData[i]));

                    xString[i] = String.valueOf(xData[i]);
                    yString[i] = String.valueOf(yData[i]);
                }

                loggingXdata = String.join(" ", xString);
                loggingYdata1 = String.join(" ", yString);
                if (isRecording) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logSingleChannelData(analogInput);
                        }
                    });
                }

            } catch (NullPointerException e) {
                cancel(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!String.valueOf(ledImageView.getTag()).equals("green")) {
                ledImageView.setImageResource(R.drawable.green_led);
                ledImageView.setTag("green");
            }

            setLeftYAxisScale(16f, -16f);
            setRightYAxisScale(16f, -16f);
            setXAxisScale(xAxisScale);
            LineDataSet dataSet = new LineDataSet(entries, analogInput);
            LineData lineData = new LineData(dataSet);
            dataSet.setDrawCircles(false);
            mChart.setData(lineData);
            mChart.notifyDataSetChanged();
            mChart.invalidate();

            synchronized (lock) {
                lock.notify();
            }

        }
    }

    public class CaptureTaskTwo extends AsyncTask<String, Void, Void> {
        private ArrayList<Entry> entries1;
        private ArrayList<Entry> entries2;
        private String analogInput1, analogInput2;

        @Override
        protected Void doInBackground(String... params) {
            try {
                analogInput1 = params[0];
                analogInput2 = params[1];
                // number of samples and timeGap still need to be determined
                HashMap<String, double[]> data1;
                HashMap<String, double[]> data2;
                if (isTriggerSelected && (triggerChannel.equals(analogInput1) || triggerChannel.equals(analogInput2))) {
                    if (triggerChannel.equals(analogInput1))
                        scienceLab.configureTrigger(0, analogInput1, trigger, null, null);
                    else if (triggerChannel.equals(analogInput2))
                        scienceLab.configureTrigger(1, analogInput2, trigger, null, null);
                    scienceLab.captureTraces(1, samples, timeGap, analogInput1, isTriggerSelected, null);
                    data1 = scienceLab.fetchTrace(1); //fetching data
                    Thread.sleep((long) (1000 * 10 * 1e-3));
                    scienceLab.captureTraces(1, samples, timeGap, analogInput2, isTriggerSelected, null);
                    data2 = scienceLab.fetchTrace(1);
                    Thread.sleep((long) (1000 * 10 * 1e-3));

                } else {
                    scienceLab.captureTraces(1, samples, timeGap, analogInput1, isTriggerSelected, null);
                    data1 = scienceLab.fetchTrace(1); //fetching data
                    Thread.sleep((long) (1000 * 10 * 1e-3));
                    scienceLab.captureTraces(1, samples, timeGap, analogInput2, isTriggerSelected, null);
                    data2 = scienceLab.fetchTrace(1);
                    Thread.sleep((long) (1000 * 10 * 1e-3));
                }
                double[] xData = data1.get("x");
                double[] y1Data = data1.get("y");
                double[] y2Data = data2.get("y");

                entries1 = new ArrayList<>();
                entries2 = new ArrayList<>();

                int n = Math.min(xData.length, Math.min(y1Data.length, y2Data.length));
                String[] xString = new String[n];
                String[] y1String = new String[n];
                String[] y2String = new String[n];
                for (int i = 0; i < n; i++) {
                    xData[i] = xData[i] / ((timebase == 875) ? 1 : 1000);
                    entries1.add(new Entry((float) xData[i], (float) y1Data[i]));
                    entries2.add(new Entry((float) xData[i], (float) y2Data[i]));
                    xString[i] = String.valueOf(xData[i]);
                    y1String[i] = String.valueOf(y1Data[i]);
                    y2String[i] = String.valueOf(y2Data[i]);
                }
                loggingXdata = String.join(" ", xString);
                loggingYdata1 = String.join(" ", y1String);
                loggingYdata2 = String.join(" ", y2String);

                if (isRecording) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logTwoChannelData(analogInput1, analogInput2);
                        }
                    });
                }

            } catch (NullPointerException e) {
                cancel(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!String.valueOf(ledImageView.getTag()).equals("green")) {
                ledImageView.setImageResource(R.drawable.green_led);
                ledImageView.setTag("green");
            }

            LineDataSet dataSet1;
            LineDataSet dataSet2;
            dataSet1 = new LineDataSet(entries1, analogInput1);
            dataSet2 = new LineDataSet(entries2, analogInput2);
            dataSet2.setColor(Color.GREEN);

            dataSet1.setDrawCircles(false);
            dataSet2.setDrawCircles(false);

            List<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(dataSet1);
            dataSets.add(dataSet2);

            LineData data = new LineData(dataSets);
            setXAxisScale(xAxisScale);
            setLeftYAxisScale(16, -16);
            setRightYAxisScale(16, -16);
            mChart.setData(data);
            mChart.notifyDataSetChanged();
            mChart.invalidate();
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public class CaptureTaskThree extends AsyncTask<String, Void, Void> {
        ArrayList<Entry> entries1;
        ArrayList<Entry> entries2;
        ArrayList<Entry> entries3;
        ArrayList<Entry> entries4;
        String analogInput;

        @Override
        protected Void doInBackground(String... params) {
            try {
                // number of samples and timeGap still need to be determined
                analogInput = params[0];
                HashMap<String, double[]> data;

                if (isTriggerSelected) {
                    scienceLab.configureTrigger(CHANNEL.valueOf(triggerChannel).ordinal(), analogInput, trigger, null, null);
                }
                data = scienceLab.captureFour(samples, timeGap, analogInput, isTriggerSelected);

                double[] xData = data.get("x");
                double[] y1Data = data.get("y");
                double[] y2Data = data.get("y2");
                double[] y3Data = data.get("y3");
                double[] y4Data = data.get("y4");

                entries1 = new ArrayList<>();
                entries2 = new ArrayList<>();
                entries3 = new ArrayList<>();
                entries4 = new ArrayList<>();

                for (int i = 0; i < xData.length; i++) {
                    float xi = (float) xData[i] / ((timebase == 875) ? 1 : 1000);
                    entries1.add(new Entry(xi, (float) y1Data[i]));
                    entries2.add(new Entry(xi, (float) y2Data[i]));
                    entries3.add(new Entry(xi, (float) y3Data[i]));
                    entries4.add(new Entry(xi, (float) y4Data[i]));
                }
            } catch (NullPointerException e) {
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!String.valueOf(ledImageView.getTag()).equals("green")) {
                ledImageView.setImageResource(R.drawable.green_led);
                ledImageView.setTag("green");
            }

            LineDataSet dataSet1 = new LineDataSet(entries1, CHANNEL.CH1.toString());
            LineDataSet dataSet2 = new LineDataSet(entries2, CHANNEL.CH2.toString());
            LineDataSet dataSet3 = new LineDataSet(entries3, CHANNEL.CH3.toString());
            LineDataSet dataSet4 = new LineDataSet(entries4, CHANNEL.MIC.toString());

            dataSet1.setColor(Color.BLUE);
            dataSet2.setColor(Color.GREEN);
            dataSet3.setColor(Color.RED);
            dataSet4.setColor(Color.YELLOW);
            dataSet1.setDrawCircles(false);
            dataSet2.setDrawCircles(false);
            dataSet3.setDrawCircles(false);
            dataSet4.setDrawCircles(false);

            List<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(dataSet1);
            dataSets.add(dataSet2);
            dataSets.add(dataSet3);
            dataSets.add(dataSet4);

            LineData data = new LineData(dataSets);
            mChart.setData(data);
            mChart.notifyDataSetChanged();
            mChart.invalidate();
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public class XYPlotTask extends AsyncTask<String, Void, Void> {
        String analogInput;
        float[] xFloatData = new float[1000];
        float[] yFloatData = new float[1000];

        @Override
        protected Void doInBackground(String... params) {
            HashMap<String, double[]> data;
            if ("CH2".equals(xyPlotXAxisChannel) || "CH2".equals(xyPlotYAxisChannel)) {
                analogInput = params[0];
                data = scienceLab.captureTwo(1000, 10, analogInput, false);
                double y1Data[] = data.get("y1");
                double y2Data[] = data.get("y2");
                if ("CH2".equals(xyPlotYAxisChannel)) {
                    for (int i = 0; i < y1Data.length; i++) {
                        xFloatData[i] = (float) y1Data[i];
                        yFloatData[i] = (float) y2Data[i];
                    }
                } else {
                    for (int i = 0; i < y1Data.length; i++) {
                        xFloatData[i] = (float) y2Data[i];
                        yFloatData[i] = (float) y1Data[i];
                    }
                }
            } else {
                data = scienceLab.captureFour(1000, 10, analogInput, false);
                double[] y1Data = data.get("y");
                double[] y3Data = data.get("y3");
                double[] y4Data = data.get("y4");
                switch (xyPlotXAxisChannel) {
                    case "CH1":
                        for (int i = 0; i < y1Data.length; i++) {
                            xFloatData[i] = (float) y1Data[i];
                        }
                        break;
                    case "CH3":
                        for (int i = 0; i < y3Data.length; i++) {
                            xFloatData[i] = (float) y3Data[i];
                        }
                        break;
                    case "MIC":
                        for (int i = 0; i < y4Data.length; i++) {
                            xFloatData[i] = (float) y4Data[i];
                        }
                        break;
                }

                switch (xyPlotYAxisChannel) {
                    case "CH1":
                        for (int i = 0; i < y1Data.length; i++) {
                            yFloatData[i] = (float) y1Data[i];
                        }
                        break;
                    case "CH3":
                        for (int i = 0; i < y3Data.length; i++) {
                            yFloatData[i] = (float) y3Data[i];
                        }
                        break;
                    case "MIC":
                        for (int i = 0; i < y4Data.length; i++) {
                            yFloatData[i] = (float) y4Data[i];
                        }
                        break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            graph.plotData(xFloatData, yFloatData, 1);
        }
    }

    public class CaptureAudioBuffer extends AsyncTask<Void, Void, Void> {

        private AudioJack audioJack;
        private short[] buffer;
        private List<Entry> entries;

        public CaptureAudioBuffer(AudioJack audioJack) {
            this.audioJack = audioJack;
        }

        @Override
        protected Void doInBackground(Void... params) {
            buffer = audioJack.read();
            entries = new ArrayList<>();
            for (int i = 0; i < buffer.length; i++) {
                entries.add(new Entry(i, (float) map(buffer[i], -32768, 32767, -3, 3)));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Update chart
            LineDataSet lineDataSet = new LineDataSet(entries, "Audio Data");
            lineDataSet.setColor(Color.WHITE);
            lineDataSet.setDrawCircles(false);
            mChart.setData(new LineData(lineDataSet));
            mChart.notifyDataSetChanged();
            mChart.invalidate();
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public class CaptureAudioBufferTwo extends AsyncTask<Void, Void, Void> {
        private AudioJack audioJack;
        private short[] buffer;
        private ArrayList<Entry> entries1;
        private ArrayList<Entry> entries2;
        private String analogInput;

        public CaptureAudioBufferTwo(AudioJack audioJack, String analogInput) {
            this.audioJack = audioJack;
            this.analogInput = analogInput;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                buffer = audioJack.read();
                entries1 = new ArrayList<>();
                for (int i = 0; i < buffer.length; i++) {
                    entries1.add(new Entry(i, (float) map(buffer[i], -32768, 32767, -3, 3)));
                }

                if (isTriggerSelected) {
                    scienceLab.configureTrigger(0, analogInput, trigger, null, null);
                }
                scienceLab.captureTraces(1, samples, timeGap, analogInput, isTriggerSelected, null);
                Log.v("Sleep Time", "" + (1024 * 10 * 1e-3));
                Thread.sleep((long) (1000 * 10 * 1e-3));
                HashMap<String, double[]> data = scienceLab.fetchTrace(1); //fetching data

                double[] xData = data.get("x");
                double[] yData = data.get("y");

                entries2 = new ArrayList<>();
                for (int i = 0; i < xData.length; i++) {
                    entries2.add(new Entry((float) xData[i] / ((timebase == 875) ? 1 : 1000), (float) yData[i]));
                }
            } catch (NullPointerException e) {
                cancel(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            LineDataSet dataSet1;
            LineDataSet dataSet2;
            dataSet1 = new LineDataSet(entries1, "Audio Data");
            dataSet2 = new LineDataSet(entries2, analogInput);
            dataSet2.setColor(Color.GREEN);

            dataSet1.setDrawCircles(false);
            dataSet2.setDrawCircles(false);

            List<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(dataSet1);
            dataSets.add(dataSet2);

            LineData data = new LineData(dataSets);
            mChart.setData(data);
            mChart.notifyDataSetChanged();
            mChart.invalidate();
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public class FFTTask extends AsyncTask<String, Void, Void> {
        ArrayList<Entry> entries;
        String analogInput;

        @Override
        protected Void doInBackground(String... params) {
            try {
                analogInput = params[0];
                if (isTriggerSelected) {
                    scienceLab.configureTrigger(0, analogInput, trigger, null, null);
                }
                // number of samples and timeGap still need to be determined
                scienceLab.captureTraces(1, samples, timeGap, analogInput, isTriggerSelected, null);
                Log.v("Sleep Time", "" + (1024 * 10 * 1e-3));
                Thread.sleep((long) (1000 * 10 * 1e-3));
                HashMap<String, double[]> data = scienceLab.fetchTrace(1); //fetching data

                double[] yData = data.get("y");
                Complex[] yComplex = new Complex[yData.length];
                for (int i = 0; i < yData.length; i++) {
                    yComplex[i] = Complex.valueOf(yData[i]);
                }
                Complex[] fftOut = fft(yComplex);
                int n = fftOut.length;
                maxAmp = 0;

                entries = new ArrayList<>();
                double factor = samples * timeGap * 1e-3;
                maxFreq = (n / 2 - 1) / factor;
                for (int i = 0; i < n / 2; i++) {
                    float y = (float) fftOut[i].abs() / samples;
                    if (y > maxAmp) {
                        maxAmp = y;
                    }
                    entries.add(new Entry((float) (i / factor), y));
                }
            } catch (NullPointerException e) {
                cancel(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!String.valueOf(ledImageView.getTag()).equals("green")) {
                ledImageView.setImageResource(R.drawable.green_led);
                ledImageView.setTag("green");
            }

            LineDataSet dataSet = new LineDataSet(entries, analogInput);
            LineData lineData = new LineData(dataSet);
            dataSet.setDrawCircles(false);
            setXAxisScale(maxFreq);
            setLeftYAxisScale(maxAmp, 0);
            setRightYAxisScale(maxAmp, 0);
            mChart.setData(lineData);
            mChart.notifyDataSetChanged();
            mChart.invalidate();

            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public class FFTTaskTwo extends AsyncTask<String, Void, Void> {
        private ArrayList<Entry> entries1;
        private ArrayList<Entry> entries2;
        private String analogInput1;
        private String analogInput2;

        @Override
        protected Void doInBackground(String... params) {
            try {
                analogInput1 = params[0];
                analogInput2 = params[1];
                if (isTriggerSelected) {
                    if (triggerChannel.equals("CH1"))
                        scienceLab.configureTrigger(0, analogInput1, trigger, null, null);
                    else if (triggerChannel.equals("CH2"))
                        scienceLab.configureTrigger(1, analogInput2, trigger, null, null);
                }
                // number of samples and timeGap still need to be determined
                scienceLab.captureTraces(1, samples, timeGap, analogInput1, isTriggerSelected, null);
                Log.v("Sleep Time", "" + (1024 * 10 * 1e-3));
                Thread.sleep((long) (1000 * 10 * 1e-3));
                HashMap<String, double[]> data1 = scienceLab.fetchTrace(1); //fetching data

                scienceLab.captureTraces(1, samples, timeGap, analogInput2, isTriggerSelected, null);
                Log.v("Sleep Time", "" + (1024 * 10 * 1e-3));
                Thread.sleep((long) (1000 * 10 * 1e-3));
                HashMap<String, double[]> data2 = scienceLab.fetchTrace(1);

                double[] yData1 = data1.get("y");
                double[] yData2 = data2.get("y");
                Complex[] yComplex1 = new Complex[yData1.length];
                Complex[] yComplex2 = new Complex[yData2.length];
                for (int i = 0; i < Math.min(yData1.length, yData2.length); i++) {
                    yComplex1[i] = Complex.valueOf(yData1[i]);
                    yComplex2[i] = Complex.valueOf(yData2[i]);
                }
                Complex[] fftOut1 = fft(yComplex1);
                Complex[] fftOut2 = fft(yComplex2);
                int n = Math.min(fftOut1.length, fftOut2.length);
                maxAmp = 0;

                float maxAmp1 = 0;
                float maxAmp2 = 0;
                entries1 = new ArrayList<>();
                entries2 = new ArrayList<>();
                double factor = samples * timeGap * 1e-3;
                maxFreq = (n / 2 - 1) / factor;
                for (int i = 0; i < n / 2; i++) {
                    float y1 = (float) fftOut1[i].abs() / samples;
                    if (y1 > maxAmp1) {
                        maxAmp1 = y1;
                    }
                    entries1.add(new Entry((float) (i / factor), y1));
                    float y2 = (float) fftOut2[i].abs() / samples;
                    if (y2 > maxAmp2) {
                        maxAmp2 = y2;
                    }
                    entries2.add(new Entry((float) (i / factor), y2));
                }
                maxAmp = Math.max(maxAmp1, maxAmp2);
            } catch (NullPointerException e) {
                cancel(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!String.valueOf(ledImageView.getTag()).equals("green")) {
                ledImageView.setImageResource(R.drawable.green_led);
                ledImageView.setTag("green");
            }

            LineDataSet dataSet1 = new LineDataSet(entries1, analogInput1);
            LineDataSet dataSet2 = new LineDataSet(entries2, analogInput2);
            dataSet2.setColor(Color.GREEN);

            dataSet1.setDrawCircles(false);
            dataSet2.setDrawCircles(false);

            List<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(dataSet1);
            dataSets.add(dataSet2);

            LineData data = new LineData(dataSets);
            setXAxisScale(maxFreq);
            setLeftYAxisScale(maxAmp, 0);
            setRightYAxisScale(maxAmp, 0);
            mChart.setData(data);
            mChart.notifyDataSetChanged();
            mChart.invalidate();

            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public Complex[] fft(Complex[] input) {
        Complex[] x = input;
        int n = x.length;

        if (n == 1) return new Complex[]{x[0]};

        if (n % 2 != 0) {
            x = Arrays.copyOfRange(x, 0, x.length - 1);
        }

        Complex[] halfArray = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            halfArray[k] = x[2 * k];
        }
        Complex[] q = fft(halfArray);

        for (int k = 0; k < n / 2; k++) {
            halfArray[k] = x[2 * k + 1];
        }
        Complex[] r = fft(halfArray);

        Complex[] y = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].add(wk.multiply(r[k]));
            y[k + n / 2] = q[k].subtract(wk.multiply(r[k]));
        }
        return y;
    }

    public void recordSensorDataBlockID(SensorDataBlock block) {
        realm.beginTransaction();
        realm.copyToRealm(block);
        realm.commitTransaction();
    }

    public void recordSensorData(RealmObject sensorData) {
        realm.beginTransaction();
        realm.copyToRealm((OscilloscopeData) sensorData);
        realm.commitTransaction();
    }
}