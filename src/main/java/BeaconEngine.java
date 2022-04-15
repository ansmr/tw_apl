import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


public class BeaconEngine {
    private UpdateReceiver upReceiver;
    private IntentFilter intentFilter;

    private BeaconService mService;

    //現在処理中のビーコン
    private MyBeaconBean currentBeacon;

//    //現在処理中のビーコン(uuid)
//    private String nowProcessingBeacon = "";

    //ビーコンマスタ
//    private HashMap<String, HashMap<String, String>>   beaconMst = new HashMap<String, HashMap<String, String>>();
//    private HashMap<String, ResourceMappingBean> beaconMst;

    private Config config;

    //イベント通知用クラス
    protected BeaconEventNotifier eventNotifier = null;

    private Context con;

//    public void setBeaconMst(HashMap<String, ResourceMappingBean>value) {
//        this.beaconMst = value;
//    }

    public void setConfig(Config cfg) {
        this.config = cfg;
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((BeaconService.BindServiceBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };

    public void run(Context con) {
        this.con = con;

        //Beaconサービス起動
        Intent intent = new Intent(con, BeaconService.class);

        intent.putExtra("settings", this.config.settings);
        con.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        upReceiver = new UpdateReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("UPDATE_ACTION");
        con.registerReceiver(upReceiver, intentFilter);

        upReceiver.registerHandler(updateHandler);
    }

    public void stop() {
        con.unbindService(connection);
    }


    // サービスから値を受け取ったら動かしたい内容を書く
    private Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String message = bundle.getString("message");

            if (message.isEmpty()) {
                return;
            }

            EventDataBean eventdata = null;

            try {
                ObjectMapper mapper = new ObjectMapper();
                eventdata = mapper.readValue(message, EventDataBean.class);
            } catch (Exception ex) {
                Log.d("BeaconService", ex.getMessage());
            }

            //パラメータが受信できていなければ何も再生しない
            if (eventdata == null) {
                return;
            }

            //イベントタイプ毎に処理を行う
            switch (eventdata.eventtype) {
                case EventDataType.ENTER_REGION:
                    //ビーコンの範囲に入る
                    OnRangeEnter(eventdata);
                    break;

                case EventDataType.EXIT_REGION:
                    //ビーコンの範囲から出る
                    OnRangeExit(eventdata);
                    break;
            }
        }
    };


    /**
     * ビーコンの範囲に入った場合のイベント
     * @param eventdata
     */
    protected void OnRangeEnter(EventDataBean eventdata) {
        //ビーコンから受信していなければ何も再生しない
        if (eventdata.beacons == null) {
            return;
        }
        if (eventdata.beacons.size() == 0) {
            return;
        }

        //距離の近い順にソートする
        Collections.sort(eventdata.beacons, new Comparator<MyBeaconBean>() {
            public int compare(MyBeaconBean b1, MyBeaconBean b2) {
                return Double.compare(b1.distance, b2.distance);
            }
        });

        //一番近いビーコンを選択する
        MyBeaconBean    beacon = eventdata.beacons.elementAt(0);
        Log.d("BeaconService", "一番近いビーコン：" + beacon.uuid);

        //一番近いビーコンが一定距離以上離れていた場合は、ビーコンの範囲にいないと判断し、退出処理を行う
        if (beacon.distance >= this.config.settings.OutOfDistance) {
            this.OnRangeExit(eventdata);
            return;
        }

        //受信したビーコンのUUIDがマスタに存在しなかった場合、なにも処理をしない
        if (!this.config.beacons.isExistsUUID(beacon.uuid)) {
            return;
        }
//        if (!beaconMst.containsKey(beacon.uuid)) {
//            return;
//        }

        //イベントを通知する
        this.eventNotifier.onRangeEnter(beacon);
    }


    /**
     * ビーコンの範囲から出た場合のイベント
     * @param eventdata
     */
    protected void OnRangeExit(EventDataBean eventdata) {
        Log.d("BeaconService", "OnRangeExit");

        this.eventNotifier.onRangeExit();
    }


    public void addBeaconEventNotifier(BeaconEventNotifier event) {
        this.eventNotifier = event;
    }

    /**
     * 現在処理中のビーコンオブジェクトを返す
     * @return
     */
    public MyBeaconBean getCurrentBeacon() {
        return this.currentBeacon;
    }

    /**
     * 新たに領域に入ったビーコンから、次に再生すべき素材を返す
     * @param enterBeacon 新たに領域に入ったビーコン
     */
    public PlayContentsBean getPlayContentsByEnterBeacon(MyBeaconBean enterBeacon) {
        Beacon                  currentBeaconMst;
        Beacon                  enterBeaconMst;
        PlayContentsBean        result = new PlayContentsBean();



//        enterBeaconMst = this.beaconMst.get(enterBeacon.uuid);
        enterBeaconMst = this.config.beacons.getBeacon(enterBeacon.uuid);

        if (this.currentBeacon != null) {
            //ビーコンマスタの取得
//            currentBeaconMst = this.beaconMst.get(this.currentBeacon.uuid);
            currentBeaconMst = this.config.beacons.getBeacon(this.currentBeacon.uuid);

            if (this.isPlayControl()) {
                //再生順序のチェック
                if ((currentBeaconMst.priority + 1) != enterBeaconMst.priority) {
                    Beacon originNext = this.findNextBeaconMst(enterBeaconMst);
                    result.addContents(originNext.previous);
//                    result.MovieFile = originNext.previouts;
//                    result.GuidImageFile = originNext.previousGuildImage;
                    return result;
                }
            }
        }

//        result.MovieFile = enterBeaconMst.next;
//        result.GuidImageFile = enterBeaconMst.nextGuildImage;
        result.addContents(enterBeaconMst.current);
        result.addContents(enterBeaconMst.next);

        //現在のビーコンを置き換える
        this.currentBeacon = enterBeacon;

        return result;
    }


    private Beacon findNextBeaconMst(Beacon beacon) {
        Beacon     result = null;

        for (Beacon b : this.config.beacons.list) {
            if (b.priority == beacon.priority + 1) {
                result = this.config.beacons.getBeacon(b.uuid);
                break;
            }
        }

        return result;
    }

    private Boolean isPlayControl() {
        return (this.config.settings.PlayControl);
    }
}
