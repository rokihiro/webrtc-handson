//
//  DataConnectionViewController.swift
//  iOS_handon_Swift
//
//  Created by Hiroki Kato on 2016/03/23.
//  Copyright © 2016年 ntt.com. All rights reserved.
//

import Foundation
import UIKit
import AVFoundation


class DataConnectionViewController: UIViewController {
    
    private var _peer: SKWPeer?
    private var _data: SKWDataConnection?
    private var _id: String? = nil
    private var _bEstablished: Bool = false
    private var _listPeerIds: Array<String> = []
    @IBOutlet weak var idLabel: UILabel!
    @IBOutlet weak var callButton: UIButton!
    
    @IBOutlet weak var sendButton: UIButton!
    
    @IBOutlet weak var editMessage: UITextField!
    
    @IBOutlet weak var logTextView: UITextView!
    
    
    
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        self.view.backgroundColor = UIColor.whiteColor()

        
        if nil != self.navigationController {
            self.navigationController?.delegate = self
        }
        
        /////////////////////////////////////////////////////////////
        /////////////////////  3.1．サーバへ接続  /////////////////////
        ////////////////////////////////////////////////////////////
        
        //APIキー、ドメインを設定
        let option: SKWPeerOption = SKWPeerOption.init();
        option.key = ""
        option.domain = ""
        
        // Peerオブジェクトのインスタンスを生成
        _peer = SKWPeer.init(options: option);
        
        
        ///////////////////////////////////////////////////////////////
        /////////////////////  3.2．接続成功・失敗  /////////////////////
        //////////////////////////////////////////////////////////////
        
        _peer?.on(SKWPeerEventEnum.PEER_EVENT_ERROR,callback:{ (obj: NSObject!) -> Void in
            let error:SKWPeerError = obj as! SKWPeerError
            print("\(error)")
        })
        
        _peer?.on(SKWPeerEventEnum.PEER_EVENT_OPEN,callback:{ (obj: NSObject!) -> Void in
            self._id = obj as? String
            dispatch_async(dispatch_get_main_queue(), {
                self.idLabel.text = "your ID: \n\(self._id!)"
            })
        })
        
        ///////////////////////////////////////////////////////////////
        /////////////////////  3.3．相手からの着信  /////////////////////
        //////////////////////////////////////////////////////////////
        
        
        //コールバックを登録（CONNECTION)
        _peer?.on(SKWPeerEventEnum.PEER_EVENT_CONNECTION, callback: { (obj:NSObject!) -> Void in
            self._data = obj as? SKWDataConnection
            self.setDataCallbacks(self._data!)
            self._bEstablished = true
            self.updateUI()
        })
        
    }
    
    //データチャネルのコールバック処理
    func setDataCallbacks(data:SKWDataConnection){
        
        //コールバックを登録(チャンネルOPEN)
        [data .on(SKWDataConnectionEventEnum.DATACONNECTION_EVENT_OPEN, callback: { (obj:NSObject!) -> Void in
            //[self appendLogWithHead:@"system" value:@"DataConnection opened."];
            self._bEstablished = true;
            self.updateUI();
        })]
        
        // コールバックを登録(チャンネルOCLOSE)
        [data .on(SKWDataConnectionEventEnum.DATACONNECTION_EVENT_CLOSE, callback: { (obj:NSObject!) -> Void in
            let strValue:String = obj as! String
            //[self appendLogWithHead:@"Partner" value:strValue];

        })]
        
        // コールバックを登録(チャンネルOCLOSE)
        [data .on(SKWDataConnectionEventEnum.DATACONNECTION_EVENT_CLOSE, callback: { (obj:NSObject!) -> Void in
            self._data = nil
            self._bEstablished = false
            self.updateUI()
            ///[self appendLogWithHead:@"system" value:@"DataConnection closed."];
        })]
    }
    
    
    
    ///////////////////////////////////////////////////////////////////
    /////////////////////  3.4.　相手へのデータ発信　/////////////////////
    //////////////////////////////////////////////////////////////////
    
    
    func getPeerList(){
        if (_peer == nil) || (_id == nil) || (_id?.characters.count == 0) {
            return
        }
        
        _peer?.listAllPeers({ (peers:[AnyObject]!) -> Void in
            self._listPeerIds = []
            let peersArray:[String] = peers as! [String]
            for strValue:String in peersArray{
                print(strValue)
                
                if strValue == self._id{
                    continue
                }
                
                self._listPeerIds.append(strValue)
            }
            
            if self._listPeerIds.count > 0{
                self.showPeerDialog()
            }
            
        })
    }
    
    //データチャンネルを開く
    func connect(strDestId: String) {
        let options = SKWConnectOption()
        options.label = "chat"
        options.metadata = "{'message': 'hi'}"
        options.serialization = SKWSerializationEnum.SERIALIZATION_BINARY
        options.reliable = true
        
        //接続
        _data = _peer?.connectWithId(strDestId, options: options)
        setDataCallbacks(self._data!)
        self.updateUI()
    }
    
    /////////
    
    //ビデオ通話を終了する
    func close(){
        if _bEstablished == false{
            return
        }
        _bEstablished = false
        
        if _data == nil{
            _data?.close()
        }
    }
    
    
    //テキストデータを送信する
    func send(data:String){
        var bResult:Bool = (_data?.send(data))!
        
        if bResult == true {
            //[self appendLogWithHead:@"You" value:data];
        }
    }
    
    
    
    
    func showPeerDialog(){
        let vc:PeerListViewController = PeerListViewController()
        vc.items = _listPeerIds
        vc.callback = self
        
        let nc:UINavigationController = UINavigationController.init(rootViewController: vc)
        
        dispatch_async(dispatch_get_main_queue(), {
            self.presentViewController(nc, animated: true, completion: nil)
        })
        
    }
    
    
    
    //////////////////////////////////////////////////////////////////
    /////////////////////  2.6.　UIのセットアップ  /////////////////////
    /////////////////////////////////////////////////////////////////
    
    
    
    func updateUI(){
        dispatch_async(dispatch_get_main_queue()) { () -> Void in
            
            //CALLボタンのアップデート
            if self._bEstablished == false{
                self.callButton.titleLabel?.text = "Connect"
            }else{
                self.callButton.titleLabel?.text = "Disconnect"
            }
            
            //IDラベルのアップデート
            if self.idLabel == nil{
                self.idLabel.text = "your Id:"
            }else{
                self.idLabel.text = "your Id:"+self._id! as String
            }
            self.sendButton.enabled = self._bEstablished
        }
    }
    
    @IBAction func pushCallButton(sender: AnyObject) {
        if _data == nil {
            self.getPeerList()
        }else{
            self.performSelectorInBackground("close", withObject: nil)
        }
    }
    
    
    @IBAction func pushSendButton(sender: AnyObject) {
        let data:String = self.editMessage.text!;
        self.send(data)
        self.editMessage.text = ""
    }

    
    //////////////////////////////////////////////////////////////////
    ///////////////////// 2.6.　ハンズオンここまで  /////////////////////
    /////////////////////////////////////////////////////////////////
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    func appendLogWithMessage(strMessage:String){
        
        var rng = UITextRange( NSMakeRange((editMessage.text?.characters.count)! + 1, 0)
        editMessage.selectedTextRange = rng
        
        
        
    }
    
    
}


extension DataConnectionViewController: UINavigationControllerDelegate, UIAlertViewDelegate {
}