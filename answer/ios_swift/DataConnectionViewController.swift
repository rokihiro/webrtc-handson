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
    

    @IBOutlet weak var editMessageTextField: UITextField!
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
            self.appendLogWithHead("system", value: "DataConnection opened")
            self._bEstablished = true;
            self.updateUI();
        })]
        
        // コールバックを登録(DATA受信)
        [data .on(SKWDataConnectionEventEnum.DATACONNECTION_EVENT_DATA, callback: { (obj:NSObject!) -> Void in
            let strValue:String = obj as! String
            self.appendLogWithHead("Partner", value: strValue)

        })]
        
        // コールバックを登録(チャンネルCLOSE)
        [data .on(SKWDataConnectionEventEnum.DATACONNECTION_EVENT_CLOSE, callback: { (obj:NSObject!) -> Void in
            self._data = nil
            self._bEstablished = false
            self.updateUI()
            self.appendLogWithHead("system", value:"DataConnection closed.")
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
    
    //接続を終了する
    func close(){
        if _bEstablished == false{
            return
        }
        _bEstablished = false
        
        if _data != nil {
            _data?.close()
        }
    }
    
    
    //テキストデータを送信する
    func send(data:String){
        let bResult:Bool = (_data?.send(data))!
        
        if bResult == true {
            self.appendLogWithHead("You", value: data)
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
                self.callButton.setTitle("CALL", forState: UIControlState.Normal)
            }else{
                self.callButton.setTitle("DISCONNECT", forState: UIControlState.Normal)
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
        let data:String = self.editMessageTextField.text!;
        self.send(data)
        self.editMessageTextField.text = ""
    }

    
    //////////////////////////////////////////////////////////////////
    ///////////////////// 2.6.　ハンズオンここまで  /////////////////////
    /////////////////////////////////////////////////////////////////
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    func appendLogWithMessage(strMessage:String){
        var rng = NSMakeRange((logTextView.text?.characters.count)! + 1, 0)
        logTextView.selectedRange = rng
        logTextView.replaceRange(logTextView.selectedTextRange!, withText: strMessage)
        rng = NSMakeRange(logTextView.text.characters.count + 1, 0)
        logTextView.scrollRangeToVisible(rng)
        
    }
    
    func appendLogWithHead(strHeader: String?, value strValue: String) {
        if 0 == strValue.characters.count {
            return
        }
        let mstrValue = NSMutableString()
        if nil != strHeader {
            mstrValue.appendString("[")
            mstrValue.appendString(strHeader!)
            mstrValue.appendString("] ")
        }
        if 32000 < strValue.characters.count {
//            var rng:NSRange = NSMakeRange(0, 32)
            mstrValue.appendString(strValue.substringWithRange(Range<String.Index>(start:strValue.startIndex.advancedBy(0),end:strValue.startIndex.advancedBy(32))))
            mstrValue.appendString("...")
//            rng = NSMakeRange(strValue.characters.count - 32, 32)
            mstrValue.appendString(strValue.substringWithRange(Range<String.Index>(start:strValue.startIndex.advancedBy(strValue.characters.count - 32),end:strValue.startIndex.advancedBy(32))))
        } else {
            mstrValue.appendString(strValue)
        }
        mstrValue.appendString("\n")
        self.performSelectorOnMainThread("appendLogWithMessage:", withObject: mstrValue, waitUntilDone: true)
    }
}


extension DataConnectionViewController: UINavigationControllerDelegate, UIAlertViewDelegate {
}