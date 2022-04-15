import android.os.*;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.*;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;

import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Beacon;


/**
 * Beacon検出サービスクラス
 */
public class BeaconService extends Service implements BootstrapNotifier , BeaconConsumer {

    public static final String TAG = org.altbeacon.beacon.service.BeaconService.class.getSimpleName();

    // iBeaconのデータを認識するためのParserフォーマット
    public static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    // BGで監視するiBeacon領域
    private RegionBootstrap regionBootstrap;
    // iBeacon検知用のマネージャー
    private BeaconManager beaconManager;
    // UUID設定用
    private Identifier identifier;
    // iBeacon領域
    private Region region;
    // 監視するiBeacon領域の名前
    private String beaconName;

    //
    private Handler handler;
    private BeaconService context;

    private Settings settings;

    //Binderクラス
    public class BindServiceBinder extends Binder {
        BeaconService getService() {
            return BeaconService.this;
        }
    }

    private final IBinder mBinder = new BindServiceBinder();

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void setupBeacon() {
        // iBeaconのデータを受信できるようにParserを設定
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));
        // BGでiBeacon領域を監視(モニタリング)するスキャン間隔を設定
        beaconManager.setBackgroundBetweenScanPeriod(this.settings.BackgroundBetweenScanPeriod);
        beaconManager.setBackgroundScanPeriod(this.settings.BackgroundScanPeriod);

        // UUIDの作成
        identifier = Identifier.parse("A56BA1E1-C06E-4C08-8467-DB6F5BD04486");
        // Beacon名の作成
        beaconName = "test";
        // major, minorの指定はしない
        region = new Region(beaconName, null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                EventDataBean eventdata = new EventDataBean();

                //イベントタイプ：領域侵入
                eventdata.eventtype = EventDataType.ENTER_REGION;

                //検出したビーコンの情報を全てActivityに通知する(JSON形式)
                //TODO 無条件で検出しているので、検出した全てをJSONに変換し、通知してしまう。絞るべき。
                for(Beacon beacon : beacons) {
                    Log.d(TAG, "UUID:" + beacon.getId1() + ", major:" + beacon.getId2() + ", minor:" + beacon.getId3() + ", Distance:" + beacon.getDistance() + ",RSSI" + beacon.getRssi() + ", TxPower" + beacon.getTxPower());

                    MyBeaconBean mybeacon = new MyBeaconBean();
                    mybeacon.uuid = beacon.getId1().toString();
                    mybeacon.distance = beacon.getDistance();
                    eventdata.beacons.add(mybeacon);
                }

                //受信したビーコンの情報をjson形式にしてActivityに通知する
                sendBroadCast(eventdata.toJsonString());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Settings stg = (Settings)intent.getSerializableExtra("settings");
        this.settings = stg;

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconManager.removeAllRangeNotifiers();
        beaconManager.removeAllMonitorNotifiers();
        Log.d(TAG, "service onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        //パラメータを取得
        Settings stg = (Settings)intent.getSerializableExtra("settings");
        this.settings = stg;

        //ビーコンのセットアップ
        this.setupBeacon();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.removeAllRangeNotifiers();

        try {
            for (Region region : beaconManager.getRangedRegions()) {
                beaconManager.stopRangingBeaconsInRegion(region);
            }

        } catch (RemoteException ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        // 領域侵入
        Log.d(TAG, "Enter Region");

        // アプリをFG起動させる
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        try {
            // レンジング開始
            beaconManager.startRangingBeaconsInRegion(region);
        } catch(RemoteException e) {
            // 例外が発生した場合
            e.printStackTrace();
        }
    }

    @Override
    public void didExitRegion(Region region) {
        // 領域退出
        Log.d(TAG, "Exit Region");
        try {
            // レンジング停止
            beaconManager.stopRangingBeaconsInRegion(region);

            //ビーコンの範囲の外に出た場合、activityに通知する
            EventDataBean   eventdata = new EventDataBean();
            eventdata.eventtype = EventDataType.EXIT_REGION;
            sendBroadCast(eventdata.toJsonString());
        } catch(RemoteException e) {
            // 例外が発生した場合
            e.printStackTrace();
        }
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        // 領域に対する状態が変化
        Log.d(TAG, "Determine State: " + i);

        try {
            if (i == 1) {
                beaconManager.startRangingBeaconsInRegion(region);
            } else {
                beaconManager.stopRangingBeaconsInRegion(region);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    public void registerHandler(Handler UpdateHandler) {
        handler = UpdateHandler;
    }


    public synchronized void sleep(long msec) {
        try {
            wait(msec);
        } catch (InterruptedException e) {
        }
    }

    protected void sendBroadCast(String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("message", message);
        broadcastIntent.setAction("UPDATE_ACTION");
        getBaseContext().sendBroadcast(broadcastIntent);
    }
}
