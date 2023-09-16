// sleep 函数
async function sleep(time) {
    return new Promise((resolve) => setTimeout(async () => {
        resolve()
    }, (time) * 1000));
}

// 随机生成邮箱
async function generateRandomEmail() {
    const domains = ["gmail.com", "yahoo.com", "hotmail.com", "outlook.com"];
    const randomDomain = domains[Math.floor(Math.random() * domains.length)];
    const randomUsername = Math.random().toString(36).substring(7); // 随机生成用户名

    return randomUsername + "@" + randomDomain;
}
// 随机指定长度字符串
function generateRandomString(length) {
    const characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    let result = "";
    for (let i = 0; i < length; i++) {
        const randomIndex = Math.floor(Math.random() * characters.length);
        result += characters.charAt(randomIndex);
    }
    return result;
}

function formatDate(d) {
    var now = new Date(parseFloat(d));
    var year = now.getFullYear();
    var month = now.getMonth() + 1;
    var date = now.getDate();
    if (month >= 1 && month <= 9) {
        month = "0" + month;
    }
    if (date >= 0 && date <= 9) {
        date = "0" + date;
    }
    var hour = now.getHours();
    var minute = now.getMinutes();
    var second = now.getSeconds();
    if (hour >= 1 && hour <= 9) {
        hour = "0" + hour;
    }
    if (minute >= 0 && minute <= 9) {
        minute = "0" + minute;
    }
    if (second >= 0 && second <= 9) {
        second = "0" + second;
    }
    return year + "-" + month + "-" + date + " " + hour + ":" + minute + ":" + second;
}

function formatPriceUSD(priceData, decimals = 4) {
    return ((priceData / 10 ** 8).toFixed(decimals));
}

function formatPriceMatic(priceData, decimals = 4) {
    return ((priceData / 10 ** 18).toFixed(decimals));
}
// 模态框相关函数
function openModal(imageUrl) {
    var modal = document.getElementById('modal');
    var modalImage = document.getElementById('modalImage');
    modalImage.src = imageUrl;
    modal.classList.add('active');
}

function closeModal() {
    var modal = document.getElementById('modal');
    modal.classList.remove('active');
}
 

// 随机指定长度字符串
function generateRandomString(length) {
    const characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    let result = "";
    for (let i = 0; i < length; i++) {
        const randomIndex = Math.floor(Math.random() * characters.length);
        result += characters.charAt(randomIndex);
    }
    return result;
}
// 加载合约 ABI
async function loadContractABI() {
    try {
        console.log('loadContractABI begin .')
        const response = await fetch("DeviceFactory.json");
        const contractABI = await response.json();
        console.log('contractABI :', contractABI)
        return contractABI['abi'];
    } catch (error) {
        console.error(error);
        throw new Error("无法加载合约 ABI。");
    }
}

//////////////////////////////////////////////////connect wallet///////////////////////////////////////////////////
async function initWeb3() {
    if (!useAuto) {
        if (typeof window.ethereum !== 'undefined') {
            // 创建一个 Web3 实例
            gWeb3 = new Web3(window.ethereum);
        } else {
            console.log("no wallet installed");
        }
    } else {
        gWeb3 = new Web3(new Web3.providers.HttpProvider(gProviderUrl));
        const privateKey = '0xac09cccccccccccccccccccc'; // 用户提供的私钥 
        // 创建以太坊账户
        const account = gWeb3.eth.accounts.privateKeyToAccount(privateKey);
        gAccountAddr = account.address;
    }
    // 设置默认发送账户
    gWeb3.eth.defaultAccount = gAccountAddr;
    gContractABI = await loadContractABI();
    await monitorEvent();
}
async function connectWallet() {
    if (typeof window.ethereum !== 'undefined') {
        // 创建一个 Web3 实例
        gWeb3 = new Web3(window.ethereum);
    } else {
        console.log("no wallet installed");
        return;
    }
    useAuto = false;
    // 请求用户授权连接钱包
    window.ethereum.enable()
        .then(() => {
            // 连接成功后，你可以使用 web3 对象进行交互操作
            // 例如，获取用户的账户地址
            gWeb3.eth.getAccounts()
                .then(accounts => {
                    const userAddress = gAccountAddr = accounts[0];
                    console.log('用户账户地址：', userAddress);

                    // 这里可以执行其他钱包相关的操作
                })
                .catch(error => {
                    console.error('获取账户地址时发生错误：', error);
                });
        })
        .catch(error => {
            console.error('连接钱包时发生错误：', error);
        });
}

//////////////////////////////////////////////////Tooltip///////////////////////////////////////////////////


//////////////////////////////////////////////////monitor event///////////////////////////////////////////////////
async function onDeviceRegisterSuccess  (devInfo) {
    console.log('=====> onDeviceRegisterSuccess',devInfo);
  }
async function monitorEvent() {
    try { 
        // 参考： https://docs.web3js.org/guides/web3_providers_guide/examples
        // 创建合约实例 
        const web3 = new Web3(new Web3.providers.WebsocketProvider(gWsProviderUrl));
        var contractInstance = new web3.eth.Contract(gContractABI,gContractAddress);

        contractInstance.events.onDeviceRegisterSuccess({
            // filter: {myIndexedParam: [20,23], myOtherIndexedParam: '0x123456789...'}, // Using an array means OR: e.g. 20 or 23
            fromBlock: 'latest'
        }, function(error, event){ console.log(event); })
        .on('data',async function(event){
            // console.log(event); // same results as the optional callback above
            console.log(event.returnValues.devInfo); // same results as the optional callback above
            let oneDevice = event.returnValues.devInfo 
            let latitude = parseFloat(oneDevice.latitude);
            let longitude = parseFloat(oneDevice['longitude']);
            let tokenCnt = oneDevice['tokenBalance'];
            let macAddress = oneDevice['macAddr'];
            let mcName = oneDevice['merchant'];
          
            tokenCnt = gWeb3.utils.fromWei(tokenCnt, 'ether'); 
            await addOneDevice(latitude,longitude,tokenCnt,macAddress,mcName);
        })
        .on('changed', function(event){
            // remove event from local database
        })
        .on('error', console.error); 
    } catch (error) {
        console.error(error);
    }
}
//////////////////////////////////////////////////Tooltip///////////////////////////////////////////////////
async function showDevicelist(startIdx = 0) {
    try {
        await connectWallet();
        // 创建合约实例
        const contractInstance = new gWeb3.eth.Contract(gContractABI, gContractAddress); 
        let devicelist = await contractInstance.methods.getDeviceList(startIdx).call() 
       
        let totalCnt = devicelist[0]
        
       
        while(true){
           
            let len = devicelist[1].length;
            console.log('devicelist len ===> ',totalCnt,len);
        
            for(let idx = 0;idx < len; ++idx){
              let oneDevice = devicelist[1][idx]
              console.log('===> ',devicelist[0],oneDevice); 
              let latitude = parseFloat(oneDevice.latitude);
              let longitude = parseFloat(oneDevice['longitude']);
              let tokenCnt = oneDevice['tokenBalance'];
              let macAddress = oneDevice['macAddr'];
              let mcName = oneDevice['merchant'];
              //tokenCnt = tokenCnt/(10**18);
              //heatLayer.addLatLng([latitude, longitude,3]);
    
              tokenCnt = gWeb3.utils.fromWei(tokenCnt, 'ether');
              //heatLayer.addLatLng([longitude,latitude,  Math.sqrt(tokenCnt) / 5]);
              await addOneDevice(latitude,longitude,tokenCnt,macAddress,mcName);
            }
            if(startIdx+1 <  totalCnt){
                startIdx += len;
                devicelist = await contractInstance.methods.getDeviceList(startIdx).call() 
            }else{
                break;
            }

        }

        

    } catch (error) {
        console.error(error);
    }
}

async function addOneDevice(lat, lng, token, mac,name){
    heatLayer.addLatLng([lat,lng,  Math.sqrt(token) / 5]);
    L.marker([lat, lng]).addTo(map)
            .bindPopup(`
            <h3>商家: ${name}</h3> 
            <p>MAC地址: ${mac} <br>
            <strong style="color: green;">余额: ${token} ETH</strong></p>
            `)
            .openPopup();
}

async function registerOneDevice(macAddress,mcName,latitude,longitude,tokenCnt){ 
    const maxFeePerGas = '40000000000'; // Wei value
    const maxPriorityFeePerGas = '40000000000'; // Wei value
    const contractInstance = new gWeb3.eth.Contract(gContractABI, gContractAddress);
        
    let payPrice = 0;
    let opt = {
        from: gAccountAddr,
        value: payPrice,
        gas: 30000000,
        maxFeePerGas: maxFeePerGas,
        maxPriorityFeePerGas: maxPriorityFeePerGas
    }
    
    let tx = await contractInstance.methods.registerDevice(macAddress,mcName,longitude.toString(),latitude.toString(),gWeb3.utils.toWei(tokenCnt.toString(), 'ether'),gAccountAddr).send(opt);
}

//////////////////////////////////////////////////Tooltip///////////////////////////////////////////////////

function addTestButton(){

        // 添加一个自定义按钮
    var customButton = L.Control.extend({
        options: {
        position: "bottomright", // 控件位置
        },
    
        onAdd: function (map) {
        var container = L.DomUtil.create("div", "custom-button");
        container.innerHTML = "链接钱包";
    
        // 给按钮添加点击事件
        container.addEventListener("click", async function ()  {
            console.log("按钮被点击了！");
            await showDevicelist(0);
        });
    
        return container;
        },
    });
    
    // 将自定义按钮添加到地图
    map.addControl(new customButton());
}