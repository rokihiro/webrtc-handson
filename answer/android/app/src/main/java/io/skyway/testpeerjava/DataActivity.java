package io.skyway.testpeerjava;

import io.skyway.Peer.*;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * テキストチャット画面
 *
 */

public class DataActivity extends Activity {
	private static final String TAG = DataActivity.class.getSimpleName();

	private Peer           	_peer;
	private DataConnection 	_data;
	private Handler 		_handler;
	private String   		_id;
	private List<String> 	_listPeerIds;
	private Boolean 		_bEstablished;

	// テキストチャット用のUI
	private Runnable     	_runAddLog;
	private List<String> 	_aryLogs;
	private EditText		_editMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Window wnd = getWindow();
		wnd.addFlags(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_data);

		_bEstablished = false;
		_handler = new Handler(Looper.getMainLooper());
		_listPeerIds = new ArrayList<>();
		Context context = getApplicationContext();

		// APIキー、ドメインを設定
		PeerOption options = new PeerOption();
        options.key = "XXXXXXXXXXXXX";
		options.domain = "localhost";

		// Peerオブジェクトのインスタンスを生成
		_peer = new Peer(context, options);

		// コールバックを登録(OPEN)
		_peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				_id = (String) object;
				_handler.post(new Runnable() {
					@Override
					public void run() {
						// 自分のIDを表示
						TextView tv = (TextView) findViewById(R.id.tvOwnId);
						tv.setText("ID【" + _id + "】");
						tv.invalidate();
					}
				});
			}
		});

		// コールバックを登録(CONNECTION)
		_peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback(){
			@Override
			public void onCallback(Object object){
				_data = (DataConnection) object;
				setDataCallback(_data);
				_bEstablished = true;
				updateUI();
			}
		});

		// コールバックを登録(ERROR)
		_peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				PeerError error = (PeerError) object;
				Log.d(TAG, "[On/Error]" + error);
			}
		});

		//
		// UIのセットアップ
		//

		// アクションボタン
		Button btnAction = (Button) findViewById(R.id.btnAction);
		if (null != btnAction){
			btnAction.setEnabled(true);
			btnAction.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					v.setEnabled(false);
					if (!_bEstablished) {
						getPeerList();
					} else {
						close();
					}
					v.setEnabled(true);
				}
			});
		}

		// データ送信ボタン
		Button btnSendData = (Button)findViewById(R.id.btnSendData);
		if (null != btnSendData){
			btnSendData.setText("Send Data");
			btnSendData.setEnabled(false);
			btnSendData.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					v.setEnabled(false);
					String data = _editMessage.getText().toString();
					send(data);
					v.setEnabled(true);
				}
			});
		}

		// 自分のピアID
		TextView tvId = (TextView)findViewById(R.id.tvOwnId);
		tvId.setTextColor(Color.BLACK);
		tvId.setBackgroundColor(Color.LTGRAY);
		tvId.setGravity(Gravity.CENTER);

		// テキスト入力フィールド
		_editMessage = (EditText)findViewById(R.id.editMessage);

		// 送受信メッセージのログ
		TextView tvLog = (TextView)findViewById(R.id.tvLog);
		tvLog.setTextColor(Color.BLACK);
		tvLog.setBackgroundColor(Color.LTGRAY);

		// UIの表示を更新
		updateUI();
	}


	// データチャンネルを開く
	private void connect(String strPeerId){
		if (null == _peer){
			return;
		}

		if (null != _data){
			_data.close();
			_data = null;
		}

		ConnectOption option = new ConnectOption();
		option.metadata = "data connection";
		option.label = "chat";
		option.serialization = DataConnection.SerializationEnum.BINARY;

		_data = _peer.connect(strPeerId, option);

		if (null != _data) {
			setDataCallback(_data);
		}
	}


    // データチャンネルを閉じる
    private void close(){
        if (false == _bEstablished){
            return;
        }

        _bEstablished = false;

        if (null != _data){
            _data.close();
        }
    }
    
	// データチャンネルのコールバック処理
	private void setDataCallback(DataConnection data){
		// コールバックを登録(チャンネルOPEN)
		data.on(DataConnection.DataEventEnum.OPEN, new OnCallback(){
			@Override
			public void onCallback(Object object){
				addLog("system","DataConnection opened");
				_bEstablished = true;
				updateUI();
			}
		});

		// コールバックを登録(データを受信)
		data.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				String strValue = (String) object;
				addLog("Partner", strValue);
			}
		});

		// コールバックを登録(チャンネルCLOSE)
		data.on(DataConnection.DataEventEnum.CLOSE, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				_data = null;
				_bEstablished = false;
				updateUI();
				addLog("system", "DataConnection closed");
			}
		});
	}

	// 接続相手を選択する
	private void getPeerList(){
		if ((null == _peer) || (null == _id) || (0 == _id.length())){
			return;
		}

		_listPeerIds.clear(); // IDリストをクリア

		// シグナリングサーバから、接続状態にあるピアの一覧を取得
		_peer.listAllPeers(new OnCallback() {
			@Override
			public void onCallback(Object object) {

				// 応答のJSONデータからピアIDを抽出
				JSONArray peers = (JSONArray) object;
				for (int i = 0; peers.length() > i; i++) {
					String strValue = "";
					try {
						strValue = peers.getString(i);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// 自分以外のIDをリストに追加
					if (0 != _id.compareToIgnoreCase(strValue)) {
						_listPeerIds.add(strValue);
					}
				}

				// IDリストをダイアログで表示
				if ((null != _listPeerIds) && (0 < _listPeerIds.size())) {
					showPeerListDialog();
				}
			}
		});
	}

	// 送受信メッセージを表示する
	private void addLog(String name, String strLog){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(name);
		sb.append("]");
		sb.append(strLog);
		sb.append("\r\n");

		String strMessage = sb.toString();

		if (null == _aryLogs){
			_aryLogs = Collections.synchronizedList(new ArrayList<String>());
		}

		_aryLogs.add(strMessage);

		if (null == _runAddLog){
			_runAddLog = new Runnable(){
				@Override
				public void run(){
					if (null == _aryLogs){
						return;
					}

					for (;;){
						if (0 >= _aryLogs.size()){
							break;
						}
						TextView tvLog = (TextView)findViewById(R.id.tvLog);
						tvLog.append(_aryLogs.get(0));
						_aryLogs.remove(0);
					}
				}
			};
		}

		_handler.post(_runAddLog);
	}

	// UIの表示を更新する
	private void updateUI() {
		_handler.post(new Runnable() {
			@Override
			public void run() {
				Button btnAction = (Button) findViewById(R.id.btnAction);
				if (null != btnAction) {
					if (false == _bEstablished) {
						btnAction.setText("Connecting");
					} else {
						btnAction.setText("Disconnect");
					}
				}

				TextView tvOwnId = (TextView) findViewById(R.id.tvOwnId);
				if (null != tvOwnId) {
					if (null == _id) {
						tvOwnId.setText("");
					} else {
						tvOwnId.setText(_id);
					}
				}

				Button btnSendData = (Button) findViewById(R.id.btnSendData);
				if (null != btnSendData) {
					btnSendData.setEnabled(_bEstablished);
				}
			}
		});
	}

	// テキストデータを送信する
	private void send(String data){
		boolean bResult =  _data.send(data);
		if(true == bResult){
			addLog("You", data);
		}
	}

	// 接続中のピアの一覧を表示する
	private void showPeerListDialog(){
		_handler.post(new Runnable() {
			@Override
			public void run() {
				FragmentManager mgr = getFragmentManager();
				ListDialogFragment dialog = new ListDialogFragment();
				dialog.setListener(
						new ListDialogFragment.PeerListDialogFragmentListener() {
							@Override
							public void onItemClick(final String item) {
								_handler.post(new Runnable() {
									@Override
									public void run() {
										connect(item);
									}
								});
							}
						});
				String[] peerIds = _listPeerIds.toArray(new String[0]);
				dialog.setItems(peerIds);
				dialog.show(mgr, "peerlist");
			}
		});
	}

	//
	// ライフサイクル・メソッド
	//

	@Override
	protected void onStart(){
		super.onStart();
		// Disable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		wnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume() {
		super.onResume();
		_handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(
						Context.INPUT_METHOD_SERVICE);
				View view = getCurrentFocus();
				if (null != view) {
					imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				}
			}
		}, 1000);
	}

	@Override
	protected void onStop(){
		// Enable Sleep and Screen Lock
		Window wnd	= getWindow();
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		super.onStop();
	}
}
