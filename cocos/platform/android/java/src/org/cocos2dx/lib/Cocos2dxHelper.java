/****************************************************************************
Copyright (c) 2010-2012 cocos2d-x.org
Copyright (c) 2013-2017 Chukong Technologies Inc.

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/
package org.cocos2dx.lib;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.hardware.SensorManager;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class Cocos2dxHelper {
    // ===========================================================
    // Constants
    // ===========================================================
    private static final String PREFS_NAME = "Cocos2dxPrefsFile";
    private static final int RUNNABLES_PER_FRAME = 5;
    private static final String TAG = Cocos2dxHelper.class.getSimpleName();

    // ===========================================================
    // Fields
    // ===========================================================

    private static Cocos2dxMusic sCocos2dMusic;
    private static Cocos2dxSound sCocos2dSound;
    private static AssetManager sAssetManager;
    private static Cocos2dxAccelerometer sCocos2dxAccelerometer;
    private static boolean sAccelerometerEnabled;
    private static boolean sCompassEnabled;
    private static boolean sActivityVisible;
    private static String sPackageName;
    private static String sFileDirectory;
    private static Activity sActivity = null;
    private static Cocos2dxHelperListener sCocos2dxHelperListener;
    private static Set<OnActivityResultListener> onActivityResultListeners = new LinkedHashSet<OnActivityResultListener>();
    private static Vibrator sVibrateService = null;
    private static Context sContext = null;
    private static String sVersionName = "";
    private static String sVersionCode = "1.0.0";
    // The absolute path to the OBB if it exists, else the absolute path to the APK.
    private static String sAssetsPath = "";
    
    // The OBB file
    private static ZipResourceFile sOBBFile = null;

    public static String macAddress = "";
    public static String adId;
    private static String UUID_FILE_NAME = "app.txt";

    /**
     * Battery receiver to getting battery level.
     */
    static class BatteryReceiver extends BroadcastReceiver {
        public float sBatteryLevel = 0.0f;

        @Override
        public void onReceive(Context context, Intent intent) {
            setBatteryLevelByIntent(intent);
        }

        public void setBatteryLevelByIntent(Intent intent) {
            int current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
            float level = current * 1.0f / total;
            // clamp to 0~1
            sBatteryLevel = Math.min(Math.max(level, 0.0f), 1.0f);
        }
    }

    private static BatteryReceiver sBatteryReceiver = new BatteryReceiver();

    static void registerBatteryLevelReceiver(Context context) {
        Intent intent = context.registerReceiver(sBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        sBatteryReceiver.setBatteryLevelByIntent(intent);
    }

    static void unregisterBatteryLevelReceiver(Context context) {
        context.unregisterReceiver(sBatteryReceiver);
    }

    public static float getBatteryLevel() {
        return sBatteryReceiver.sBatteryLevel;
    }

    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_LAN  = 1;
    public static final int NETWORK_TYPE_WWAN = 2;

    public static int getNetworkType() {
        int status = NETWORK_TYPE_NONE;
        NetworkInfo networkInfo;
        try {
            ConnectivityManager connMgr = (ConnectivityManager) sActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connMgr.getActiveNetworkInfo();
        } catch (Exception e) {
            e.printStackTrace();
            return status;
        }
        if (networkInfo == null) {
            return status;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            status = NETWORK_TYPE_WWAN;
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            status = NETWORK_TYPE_LAN;
        }
        return status;
    }

    // ===========================================================
    // Constructors
    // ===========================================================

    public static void runOnGLThread(final Runnable r) {
        ((Cocos2dxActivity)sActivity).runOnGLThread(r);
    }

    private static boolean sInited = false;
    public static void init(final Activity activity) {
        sActivity = activity;
        Cocos2dxHelper.sCocos2dxHelperListener = (Cocos2dxHelperListener)activity;
        if (!sInited) {

            PackageManager pm = activity.getPackageManager();
            boolean isSupportLowLatency = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

            Log.d(TAG, "isSupportLowLatency:" + isSupportLowLatency);

            int sampleRate = 44100;
            int bufferSizeInFrames = 192;

            if (Build.VERSION.SDK_INT >= 17) {
                AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                // use reflection to remove dependence of API 17 when compiling

                // AudioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
                final Class audioManagerClass = AudioManager.class;
                Object[] parameters = new Object[]{Cocos2dxReflectionHelper.<String>getConstantValue(audioManagerClass, "PROPERTY_OUTPUT_SAMPLE_RATE")};
                final String strSampleRate = Cocos2dxReflectionHelper.<String>invokeInstanceMethod(am, "getProperty", new Class[]{String.class}, parameters);

                // AudioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
                parameters = new Object[]{Cocos2dxReflectionHelper.<String>getConstantValue(audioManagerClass, "PROPERTY_OUTPUT_FRAMES_PER_BUFFER")};
                final String strBufferSizeInFrames = Cocos2dxReflectionHelper.<String>invokeInstanceMethod(am, "getProperty", new Class[]{String.class}, parameters);

                sampleRate = Integer.parseInt(strSampleRate);
                bufferSizeInFrames = Integer.parseInt(strBufferSizeInFrames);

                Log.d(TAG, "sampleRate: " + sampleRate + ", framesPerBuffer: " + bufferSizeInFrames);
            } else {
                Log.d(TAG, "android version is lower than 17");
            }

            nativeSetAudioDeviceInfo(isSupportLowLatency, sampleRate, bufferSizeInFrames);

            final ApplicationInfo applicationInfo = activity.getApplicationInfo();
            
            Cocos2dxHelper.sPackageName = applicationInfo.packageName;
            Cocos2dxHelper.sFileDirectory = activity.getFilesDir().getAbsolutePath();
            
            Cocos2dxHelper.nativeSetApkPath(Cocos2dxHelper.getAssetsPath());
    
            Cocos2dxHelper.sCocos2dxAccelerometer = new Cocos2dxAccelerometer(activity);
            Cocos2dxHelper.sCocos2dMusic = new Cocos2dxMusic(activity);
            Cocos2dxHelper.sCocos2dSound = new Cocos2dxSound(activity);
            Cocos2dxHelper.sAssetManager = activity.getAssets();
            Cocos2dxHelper.nativeSetContext((Context)activity, Cocos2dxHelper.sAssetManager);
    
            Cocos2dxBitmap.setContext(activity);

            Cocos2dxHelper.sVibrateService = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);

            sInited = true;
            
            int versionCode = 1;
            try {
                versionCode = Cocos2dxActivity.getContext().getPackageManager().getPackageInfo(Cocos2dxHelper.getCocos2dxPackageName(), 0).versionCode;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            try {
                Cocos2dxHelper.sOBBFile = APKExpansionSupport.getAPKExpansionZipFile(Cocos2dxActivity.getContext(), versionCode, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // This function returns the absolute path to the OBB if it exists,
    // else it returns the absolute path to the APK.
    public static String getAssetsPath()
    {
        if (Cocos2dxHelper.sAssetsPath == "") {
            int versionCode = 1;
            try {
                versionCode = Cocos2dxHelper.sActivity.getPackageManager().getPackageInfo(Cocos2dxHelper.sPackageName, 0).versionCode;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            String pathToOBB = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/obb/" + Cocos2dxHelper.sPackageName + "/main." + versionCode + "." + Cocos2dxHelper.sPackageName + ".obb";
            File obbFile = new File(pathToOBB);
            if (obbFile.exists())
                Cocos2dxHelper.sAssetsPath = pathToOBB;
            else
                Cocos2dxHelper.sAssetsPath = Cocos2dxHelper.sActivity.getApplicationInfo().sourceDir;
        }
        
        return Cocos2dxHelper.sAssetsPath;
    }
    
    public static ZipResourceFile getObbFile()
    {
        return Cocos2dxHelper.sOBBFile;
    }
    
    public static Activity getActivity() {
        return sActivity;
    }
    
    public static void addOnActivityResultListener(OnActivityResultListener listener) {
        onActivityResultListeners.add(listener);
    }
    
    public static Set<OnActivityResultListener> getOnActivityResultListeners() {
        return onActivityResultListeners;
    }
    
    public static boolean isActivityVisible(){
        return sActivityVisible;
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    private static native void nativeSetApkPath(final String pApkPath);

    private static native void nativeSetEditTextDialogResult(final byte[] pBytes);

    private static native void nativeSetContext(final Context pContext, final AssetManager pAssetManager);

    private static native void nativeSetAudioDeviceInfo(boolean isSupportLowLatency, int deviceSampleRate, int audioBufferSizeInFames);

    public static String getCocos2dxPackageName() {
        return Cocos2dxHelper.sPackageName;
    }
    public static String getCocos2dxWritablePath() {
        return Cocos2dxHelper.sFileDirectory;
    }

    public static String getCurrentLanguage() {
        return Locale.getDefault().getLanguage();
    }
    
    public static String getDeviceModel(){
        return Build.MODEL;
    }

    public static AssetManager getAssetManager() {
        return Cocos2dxHelper.sAssetManager;
    }

    public static void enableAccelerometer() {
        Cocos2dxHelper.sAccelerometerEnabled = true;
        Cocos2dxHelper.sCocos2dxAccelerometer.enableAccel();
    }

    public static void enableCompass() {
        Cocos2dxHelper.sCompassEnabled = true;
        Cocos2dxHelper.sCocos2dxAccelerometer.enableCompass();
    }

    public static void setAccelerometerInterval(float interval) {
        Cocos2dxHelper.sCocos2dxAccelerometer.setInterval(interval);
    }

    public static void disableAccelerometer() {
        Cocos2dxHelper.sAccelerometerEnabled = false;
        Cocos2dxHelper.sCocos2dxAccelerometer.disable();
    }

    public static void setKeepScreenOn(boolean value) {
        ((Cocos2dxActivity)sActivity).setKeepScreenOn(value);
    }

    public static void vibrate(float duration) {
        sVibrateService.vibrate((long)(duration * 1000));
    }

 	public static String getVersion() {
 		try {
 			String version = Cocos2dxActivity.getContext().getPackageManager().getPackageInfo(Cocos2dxActivity.getContext().getPackageName(), 0).versionName;
 			return version;
 		} catch(Exception e) {
 			return "";
 		}
 	}

    public static boolean openURL(String url) { 
        boolean ret = false;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            sActivity.startActivity(i);
            ret = true;
        } catch (Exception e) {
        }
        return ret;
    }
    
    public static long[] getObbAssetFileDescriptor(final String path) {
        long[] array = new long[3];
        if (Cocos2dxHelper.sOBBFile != null) {
            AssetFileDescriptor descriptor = Cocos2dxHelper.sOBBFile.getAssetFileDescriptor(path);
            if (descriptor != null) {
                try {
                    ParcelFileDescriptor parcel = descriptor.getParcelFileDescriptor();
                    Method method = parcel.getClass().getMethod("getFd", new Class[] {});
                    array[0] = (Integer)method.invoke(parcel);
                    array[1] = descriptor.getStartOffset();
                    array[2] = descriptor.getLength();
                } catch (NoSuchMethodException e) {
                    Log.e(Cocos2dxHelper.TAG, "Accessing file descriptor directly from the OBB is only supported from Android 3.1 (API level 12) and above.");
                } catch (IllegalAccessException e) {
                    Log.e(Cocos2dxHelper.TAG, e.toString());
                } catch (InvocationTargetException e) {
                    Log.e(Cocos2dxHelper.TAG, e.toString());
                }
            }
        }
        return array;
    }

    public static void preloadBackgroundMusic(final String pPath) {
        Cocos2dxHelper.sCocos2dMusic.preloadBackgroundMusic(pPath);
    }

    public static void playBackgroundMusic(final String pPath, final boolean isLoop) {
        Cocos2dxHelper.sCocos2dMusic.playBackgroundMusic(pPath, isLoop);
    }

    public static void resumeBackgroundMusic() {
        Cocos2dxHelper.sCocos2dMusic.resumeBackgroundMusic();
    }

    public static void pauseBackgroundMusic() {
        Cocos2dxHelper.sCocos2dMusic.pauseBackgroundMusic();
    }

    public static void stopBackgroundMusic() {
        Cocos2dxHelper.sCocos2dMusic.stopBackgroundMusic();
    }

    public static void rewindBackgroundMusic() {
        Cocos2dxHelper.sCocos2dMusic.rewindBackgroundMusic();
    }

    public static boolean willPlayBackgroundMusic() {
        return Cocos2dxHelper.sCocos2dMusic.willPlayBackgroundMusic();
    }

    public static boolean isBackgroundMusicPlaying() {
        return Cocos2dxHelper.sCocos2dMusic.isBackgroundMusicPlaying();
    }

    public static float getBackgroundMusicVolume() {
        return Cocos2dxHelper.sCocos2dMusic.getBackgroundVolume();
    }

    public static void setBackgroundMusicVolume(final float volume) {
        Cocos2dxHelper.sCocos2dMusic.setBackgroundVolume(volume);
    }

    public static void preloadEffect(final String path) {
        Cocos2dxHelper.sCocos2dSound.preloadEffect(path);
    }

    public static int playEffect(final String path, final boolean isLoop, final float pitch, final float pan, final float gain) {
        return Cocos2dxHelper.sCocos2dSound.playEffect(path, isLoop, pitch, pan, gain);
    }

    public static void resumeEffect(final int soundId) {
        Cocos2dxHelper.sCocos2dSound.resumeEffect(soundId);
    }

    public static void pauseEffect(final int soundId) {
        Cocos2dxHelper.sCocos2dSound.pauseEffect(soundId);
    }

    public static void stopEffect(final int soundId) {
        Cocos2dxHelper.sCocos2dSound.stopEffect(soundId);
    }

    public static float getEffectsVolume() {
        return Cocos2dxHelper.sCocos2dSound.getEffectsVolume();
    }

    public static void setEffectsVolume(final float volume) {
        Cocos2dxHelper.sCocos2dSound.setEffectsVolume(volume);
    }

    public static void unloadEffect(final String path) {
        Cocos2dxHelper.sCocos2dSound.unloadEffect(path);
    }

    public static void pauseAllEffects() {
        Cocos2dxHelper.sCocos2dSound.pauseAllEffects();
    }

    public static void resumeAllEffects() {
        Cocos2dxHelper.sCocos2dSound.resumeAllEffects();
    }

    public static void stopAllEffects() {
        Cocos2dxHelper.sCocos2dSound.stopAllEffects();
    }

    static void setAudioFocus(boolean isAudioFocus) {
        sCocos2dMusic.setAudioFocus(isAudioFocus);
        sCocos2dSound.setAudioFocus(isAudioFocus);
    }

    public static void end() {
        Cocos2dxHelper.sCocos2dMusic.end();
        Cocos2dxHelper.sCocos2dSound.end();
    }

    public static void onResume() {
        sActivityVisible = true;
        if (Cocos2dxHelper.sAccelerometerEnabled) {
            Cocos2dxHelper.sCocos2dxAccelerometer.enableAccel();
        }
        if (Cocos2dxHelper.sCompassEnabled) {
            Cocos2dxHelper.sCocos2dxAccelerometer.enableCompass();
        }
    }

    public static void onPause() {
        sActivityVisible = false;
        if (Cocos2dxHelper.sAccelerometerEnabled) {
            Cocos2dxHelper.sCocos2dxAccelerometer.disable();
        }
    }

    public static void onEnterBackground() {
        sCocos2dSound.onEnterBackground();
        sCocos2dMusic.onEnterBackground();
    }
    
    public static void onEnterForeground() {
        sCocos2dSound.onEnterForeground();
        sCocos2dMusic.onEnterForeground();
    }
    
    public static void terminateProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private static void showDialog(final String pTitle, final String pMessage) {
        Cocos2dxHelper.sCocos2dxHelperListener.showDialog(pTitle, pMessage);
    }


    public static void setEditTextDialogResult(final String pResult) {
        try {
            final byte[] bytesUTF8 = pResult.getBytes("UTF8");

            Cocos2dxHelper.sCocos2dxHelperListener.runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    Cocos2dxHelper.nativeSetEditTextDialogResult(bytesUTF8);
                }
            });
        } catch (UnsupportedEncodingException pUnsupportedEncodingException) {
            /* Nothing. */
        }
    }

    public static int getDPI()
    {
        if (sActivity != null)
        {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = sActivity.getWindowManager();
            if (wm != null)
            {
                Display d = wm.getDefaultDisplay();
                if (d != null)
                {
                    d.getMetrics(metrics);
                    return (int)(metrics.density*160.0f);
                }
            }
        }
        return -1;
    }
    
    // ===========================================================
    // Functions for CCUserDefault
    // ===========================================================
    
    public static boolean getBoolForKey(String key, boolean defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        try {
            return settings.getBoolean(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if ( value instanceof String)
            {
                return  Boolean.parseBoolean(value.toString());
            }
            else if (value instanceof Integer)
            {
                int intValue = ((Integer) value).intValue();
                return (intValue !=  0) ;
            }
            else if (value instanceof Float)
            {
                float floatValue = ((Float) value).floatValue();
                return (floatValue != 0.0f);
            }
        }

        return defaultValue;
    }
    
    public static int getIntegerForKey(String key, int defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        try {
            return settings.getInt(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if ( value instanceof String) {
                return  Integer.parseInt(value.toString());
            }
            else if (value instanceof Float)
            {
                return ((Float) value).intValue();
            }
            else if (value instanceof Boolean)
            {
                boolean booleanValue = ((Boolean) value).booleanValue();
                if (booleanValue)
                    return 1;
            }
        }

        return defaultValue;
    }
    
    public static float getFloatForKey(String key, float defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        try {
            return settings.getFloat(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if ( value instanceof String) {
                return  Float.parseFloat(value.toString());
            }
            else if (value instanceof Integer)
            {
                return ((Integer) value).floatValue();
            }
            else if (value instanceof Boolean)
            {
                boolean booleanValue = ((Boolean) value).booleanValue();
                if (booleanValue)
                    return 1.0f;
            }
        }

        return defaultValue;
    }
    
    public static double getDoubleForKey(String key, double defaultValue) {
        // SharedPreferences doesn't support saving double value
        return getFloatForKey(key, (float) defaultValue);
    }
    
    public static String getStringForKey(String key, String defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        try {
            return settings.getString(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            
            return settings.getAll().get(key).toString();
        }
    }
    
    public static void setBoolForKey(String key, boolean value) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
    
    public static void setIntegerForKey(String key, int value) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }
    
    public static void setFloatForKey(String key, float value) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, value);
        editor.apply();
    }
    
    public static void setDoubleForKey(String key, double value) {
        // SharedPreferences doesn't support recording double value
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, (float)value);
        editor.apply();
    }
    
    public static void setStringForKey(String key, String value) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }
    
    public static void deleteValueForKey(String key) {
        SharedPreferences settings = sActivity.getSharedPreferences(Cocos2dxHelper.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.apply();
    }

    public static byte[] conversionEncoding(byte[] text, String fromCharset,String newCharset)
    {
        try {
            String str = new String(text,fromCharset);
            return str.getBytes(newCharset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    public static interface Cocos2dxHelperListener {
        public void showDialog(final String pTitle, final String pMessage);

        public void runOnGLThread(final Runnable pRunnable);
    }
 
    public static float[] getAccelValue() {
        return Cocos2dxHelper.sCocos2dxAccelerometer.accelerometerValues;
    }

    public static float[] getCompassValue() {
        return Cocos2dxHelper.sCocos2dxAccelerometer.compassFieldValues;
    }

    public static int getSDKVersion() {
        return Build.VERSION.SDK_INT;
    }

    private static String sdcardRead()
    {
        try
        {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                File sdCardDir = Environment.getExternalStorageDirectory();
                FileInputStream fis = new FileInputStream(sdCardDir.getAbsolutePath() + "/" + UUID_FILE_NAME);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder("");
                String line = null;
                while((line = br.readLine()) != null)
                {
                    sb.append(line);
                }
                return sb.toString();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static void sdcardWrite(String content)
    {
        try
        {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                File sdCardDir = Environment.getExternalStorageDirectory();
                File targetFile = new File(sdCardDir.getAbsolutePath() + "/" + UUID_FILE_NAME);
                RandomAccessFile raf = new RandomAccessFile(targetFile, "rw");
                raf.seek(targetFile.length());
                raf.write(content.getBytes());
                raf.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String getLocalMacAddressFromWifiInfo(){
        if (macAddress.isEmpty()) {
            String saveToken = sdcardRead();
            if (saveToken != null && !saveToken.isEmpty()) {
                macAddress = saveToken;
            }else {
                UUID uuid = UUID.randomUUID();
                macAddress = uuid.toString();

                sdcardWrite(uuid.toString());
            }
        }

        return macAddress;
    }

    public static String getAdId() {
        if(Cocos2dxHelper.adId == null) {
            Cocos2dxHelper.adId = "";
        }
        return Cocos2dxHelper.adId;
    }

    public static String getIMEI() {
        try {
            TelephonyManager tm = (TelephonyManager) Cocos2dxHelper.sContext.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getDeviceId();
        } catch (Exception e) {
            return "";
        }
    }

    public static int getAvailableMemory() {
        final ActivityManager activityManager = (ActivityManager)Cocos2dxHelper.sContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(info);
        return (int)(info.availMem >> 20);
    }

}