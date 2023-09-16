
#!/usr/bin/env python
# -*- coding:utf-8 -*-
  
import sys,os 
import requests
import json
import urllib
import time
import datetime
import random
import string 
from urllib.parse import urlparse
import math
import base64
import hmac

import sys,traceback  
from traceback import format_exception

import urllib3
import threading

urllib3.disable_warnings()

from http.server import BaseHTTPRequestHandler 
from common.czConsoleUtils import LogError,LogGreen,LogWarning
from common.czLogUtils import czLogUtils 
import common.czUtils as czUtils
#from czCfgHelper import czCfgHelper 
from web3 import Web3, HTTPProvider 
from web3.middleware import geth_poa_middleware

#全局变量，只有通过类进行.操作才能是全局的
class gSelf():
    bProduct = False
    cfgJson = '' 
    # for web3 init
    providerIdx = 0
    cfgParam = {}
    deviceCTCfg = None
    deviceCT = None
    admin = ''
    private_key = ''
    w3 = None 
    bInit = False

class czLocalDeviceService(czLogUtils,BaseHTTPRequestHandler):
     
    czMasterInst = None    
    def __init__(self, request, client_address, server):
        czLogUtils.__init__(self)
        LogWarning('--------------__init__-------------- {} '.format(gSelf.bInit))
        # 先初始化父类的构造，再初始化子类的值
        BaseHTTPRequestHandler.__init__(self, request, client_address, server)
        self.doInit()

    # load config
    def loadJsonCfg(self,cfgFilePath):
        LogGreen('loadJsonCfg:{}'.format(cfgFilePath))
        cfgJson = ''
        with open(cfgFilePath,'r',encoding = 'utf-8') as cfgFile:
                cfgJson =  json.load(cfgFile) 
                cfgFile.close()
        return cfgJson

    def doInit(self):
        if gSelf.bInit:
            return;
        gSelf.bInit = True 
        gSelf.bProduct = gSelf.providerIdx > 0
        cfgFilePath = os.getcwd()+'/{}.json'.format('czDeviceCfg')
        gSelf.cfgParam = self.loadJsonCfg(cfgFilePath)
        # 初始化所有合约地址
        self.initWeb3New() 
  
    # 获取合约通用接口
    def getContract(self,ctCfg):
        if len(ctCfg['contract']) <= 0 or len(ctCfg['abiPath']) <= 0:
            LogError('getContract error {}'.format(ctCfg))
            return ''

        contract_abi = ''
        with open(ctCfg['abiPath'], 'r') as f:
            contract_abi = json.loads(f.read())
            #print(contract_abi['abi'])
        return gSelf.w3.eth.contract(address = Web3.toChecksumAddress(ctCfg['contract']), abi = contract_abi['abi'])

    def initWeb3New(self):

        userContract = ['local','local_polygonTest','polygonTest','goerli','mainNet',]
        ctKey  = userContract[gSelf.providerIdx]
        print('initWeb3New ==> ',ctKey,gSelf.cfgParam[ctKey])
        gSelf.deviceCTCfg = gSelf.cfgParam[ctKey]['DeviceFactory']
        
        print('initWeb3New  self.deviceCTCfg ==> ',gSelf.deviceCTCfg)
        
        gSelf.admin = gSelf.cfgParam[ctKey]['admin']
        gSelf.private_key = gSelf.cfgParam[ctKey]['private_key']

        providerUrl = gSelf.cfgParam[ctKey]['provider_url']
        LogWarning('initWeb3New : {},{},{}'.format(ctKey,gSelf.admin,providerUrl))
        proxies = {"http": None,"https": None,}         # 解决代理发不出去消息的问题 
        gSelf.w3 = Web3(HTTPProvider(providerUrl,request_kwargs={
                            #"timeout": 1000,
                            "proxies": proxies
                        }))

        # 这里要解决一些问题，需要注入中间件： 参考文档： https://zhuanlan.zhihu.com/p/389359014
        gSelf.w3.middleware_onion.inject(geth_poa_middleware, layer=0)  # 注入poa中间件

        #w3 = Web3(Web3.EthereumTesterProvider())
        print('w3.isConnected : ',gSelf.w3.isConnected())
        # w3.eth.enable_unaudited_features()
 
        gSelf.deviceCT = self.getContract(gSelf.deviceCTCfg)

    #处理 get来的数据
    def do_GET(self):  
        try:    
            #print(data)
            # 回调给相应的接口处理
            response = self.onGetRequestCallback()
 
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Access-Control-Allow-Methods', '*')
            self.send_header('Access-Control-Allow-Headers', '*')
            self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate')
            self.end_headers()  
            self.wfile.write(bytes(json.dumps(response, ensure_ascii=False), 'utf-8'))
            LogGreen('do_Get over')
        except:
            import sys,traceback
            traceback.print_exc()
            LogError('do_Get json body error')

    #处理 post来的数据
    def do_POST(self):  
        self.send_response(200)
        self.end_headers()

        length = int(self.headers['Content-Length'])
        #for item in self.headers.items: 
        #print(length)
        data = ''
        try:  
            jsonData = json.loads(self.rfile.read(length)) 
            data = jsonData
            #print(data)
            # 回调给相应的接口处理
            response = self.onPostRequestCallback(data)

            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()  
            self.wfile.write(bytes(json.dumps(response, ensure_ascii=False), 'utf-8'))
            LogGreen('do_POST over')
        except:
            LogError('do_POST json body error')
   
    def onGetRequestCallback(self):
        #print('base request call back')
        #global czMasterInst
        if self.czMasterInst:
            return self.czMasterInst.onGetRequestCallback(self.path)
        
        self.doInit()

        data = {} 
        data['code'] = 0
        import urllib
        parsedPath = urllib.parse.urlparse(self.path)
        params = urllib.parse.parse_qs(parsedPath.query)
        print(self.path,parsedPath,params)
        
        if parsedPath.path.startswith("/reqToken"): 
            macAddr = params['macAddr'][0]
            walletAddr = params['walletAddr'][0]
            data['data'] = self.transferTokens(macAddr,walletAddr)
            LogGreen('{}--{}'.format(macAddr,walletAddr))
        if parsedPath.path.startswith("/getUserInfo"):  
            walletAddr = params['walletAddr'][0]
            ret,data['data'] = self.getUserInfo(walletAddr)
            data['code'] = 0 if ret else 1
            LogGreen('{}'.format(walletAddr))
        else: 
            LogError('onGetRequestCallback unknown request!!!')

        return data

    def onPostRequestCallback(self,body):
        print('base request call back')
        #global czMasterInst
        if self.czMasterInst:
            return self.czMasterInst.onPostRequestCallback(body)
        
        data = {}
        data['code'] = 0
        data['xPosList'] = []
        return data 

    def getProxy(self):
        #return {"http": None,"https": None,}
        proxies = {"http": 'http://127.0.0.1:1080',"https": 'http://127.0.0.1:1080',} 
        return proxies 
        return {"http": None,"https": None,}

    def getUserInfo(self,walletAddr): 
        userInfo = {}
        userInfo['walletAddr'] = walletAddr
        userInfo['tokenBalance'] = 0
        userInfo['tokenText'] = '0 ETH'

        try:
            userData = gSelf.deviceCT.functions.getUser(walletAddr).call() 
            
            LogGreen('getUserInfo ===========> {} '.format(userData))
            # and whatever
            userInfo['tokenBalance'] = userData[1]
            userInfo['tokenText'] = '{} ETH'.format(gSelf.w3.fromWei(userData[1] , 'ether'))
            ret = True
        except:
            ret = False
            errMsg = 'getUserInfo error: {},{},{}'.format(walletAddr,ret,traceback.format_exc())
            LogError(errMsg)  
        return ret,userInfo

    def transferTokens(self,macAddr,walletAddr): 
        try:
            #transferTokens(string memory macAddress, address to)
            transaction = gSelf.deviceCT.functions.transferTokens(macAddr,walletAddr)
            self.makeTransaction(transaction)
            # and whatever
            LogGreen('transferTokens ===========> done ')
            ret = True
        except:
            ret = False
            errMsg = 'transferTokens error: {},{},{}'.format(macAddr,ret,traceback.format_exc())
            LogError(errMsg)  
        return ret,macAddr

    def makeTransaction(self,callTrans,value = 0):
        
        transaction = callTrans.buildTransaction( # 参考python进行合约链交互： https://stackoverflow.com/questions/57580702/how-to-call-a-smart-contract-function-using-python-and-web3-py
        {
            "from": Web3.toChecksumAddress(gSelf.admin),
            'nonce': gSelf.w3.eth.getTransactionCount(gSelf.admin),
            "value": value 
        })
        signed_txn = gSelf.w3.eth.account.signTransaction(transaction, private_key = gSelf.private_key)
        tx_hash = gSelf.w3.eth.sendRawTransaction(signed_txn.rawTransaction)
        tx_receipt = gSelf.w3.eth.wait_for_transaction_receipt(tx_hash)



    # 启动服务
    @staticmethod
    def serviceThread(threadName,threadID):
        LogGreen('+++++++++++++++startServer+++++++++++++')
        from http.server import HTTPServer 
        try:  
            #listenPort = 8099
            listenPort = czUtils.getAvailablePort(18099)
            LogGreen('start local http server with port : {}'.format(listenPort))
            web_server = HTTPServer(("",listenPort),czLocalDeviceService)
            web_server.serve_forever()

        except:
            traceback.print_exc(file=open('crash-{0}.log'.format(czLogUtils.getDateYMD()), 'a'))
    
    @staticmethod
    def startServer():
        hThread = threading.Thread(target=czLocalDeviceService.serviceThread, args=("serviceThread",1))
        hThread.start()
        #time.sleep(5)
            
if __name__=='__main__':
  
    start = time.time()
    czLocalDeviceService.startServer()
    end = time.time()

    LogGreen('service online :{}s'.format(end-start))