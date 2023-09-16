// We require the Hardhat Runtime Environment explicitly here. This is optional
// but useful for running the script in a standalone fashion through `node <script>`.
//
// You can also run a script with `npx hardhat run <script>`. If you do that, Hardhat
// will compile your contracts, add the Hardhat Runtime Environment's members to the
// global scope, and execute the script.
const hre = require("hardhat");

const czWeb3Helper = require("../scriptUtils/czWeb3Helper");
const { getDeviceList } = require("./devices");
var json = require('../package-lock.json');
const { utils } = require("mocha");
const { stringify } = require("querystring");
 
// 方法 - 延时
async function sleep(time) {
  return new Promise((resolve) => setTimeout(async () => {
    resolve()
  }, (time) * 1000));
}
async function onDeviceRegisterSuccess  (devInfo) {
  console.log('=====> onDeviceRegisterSuccess',devInfo);
}
async function onReqTokenSuccess(devInfo,userInfo,tokenAmount){
  console.log('=====> onReqTokenSuccess',devInfo,userInfo,tokenAmount);
}
async function attachContract(contractName,contractAddr) {
  //let contractAddr = '0x5fbdb2315678afecb367f032d93f642f64180aa3'; 
  const contractFactory = await ethers.getContractFactory(contractName);
  const ctDeployer = await contractFactory.attach( contractAddr );  // The deployed contract address 
  console.log(contractName,"Contract attach to address:", ctDeployer.address);
  return ctDeployer;
} 
// deploy config contract
async function deployDeviceFactory() {
  const contractFactory = await ethers.getContractFactory("DeviceFactory");
  const deployer = await contractFactory.deploy();
  console.log("Contract slave DeviceFactory deployed to address:", deployer.address);

  return deployer;
}

async function initDeviceList(deployer) {
  const oneETH = 1000000000000000000; 
  let allWalletList = []; 
  let addressPoints = await getDeviceList();
  console.log(getDeviceList())
  console.log('============> len = ',addressPoints.length)
  for (let idx = 0; idx < addressPoints.length; idx++) {
    let element = addressPoints[idx];
    let latitude = element[0];
    let longitude = element[1];
    let tokenCnt = element[2];
    let macAddress = element[3];
    let mcName = element[4];

    // 输出每个元素的内容
    // console.log("Latitude: " + latitude);
    // console.log("Longitude: " + longitude);
    // console.log("TokenCnt: " + tokenCnt);
    // console.log("MAC Address: " + macAddress);
    // console.log("Device Name: " + mcName);

    let {privateKey, address} = await czWeb3Helper.getWalletWithMoney(deployer.provider);
    userWallet = new ethers.Wallet(privateKey, deployer.provider);
    allWalletList.push(userWallet);
    let beginBalance = await userWallet.getBalance(); 
    console.log('getWalletWithMoney:',address,beginBalance/oneETH); 
    {     
      // console.log('reg device : ',allWalletList[idx].address);
      // 模拟验证
      await deployer.registerDevice(macAddress,mcName,longitude.toString(),latitude.toString(),ethers.utils.parseEther(tokenCnt),userWallet.address);
      // 自己mint
      //await deployer.connect(userWallet).mintToWhitelist(userWallet.address,'http://www.xx.xyz/white.json');
      
      // console.log('userWallet mint done ===> ',userWallet.address);
      await sleep(2)
    } 
  } 
}
async function showWordaoBalance(deployer) {
  let masterBalance = await deployer.getMyBalance();
  console.log('WordaoBalance has: ', masterBalance);
} 
// deploy 
async function AddDeviceMaster() {

  // step1: attach contract
  const deviceFactoryDeployer = await attachContract('DeviceFactory','0x5FbDB2315678afecb367f032d93F642f64180aa3');
  
  deviceFactoryDeployer.on('onDeviceRegisterSuccess', onDeviceRegisterSuccess)
  deviceFactoryDeployer.on('onReqTokenSuccess', onReqTokenSuccess)

  await initDeviceList(deviceFactoryDeployer);

  await sleep(5)
  let devicelist = await deviceFactoryDeployer.getDeviceList(0);
  console.log('devicelist len ===> ',devicelist[0]);
  for(let idx = 0;idx < devicelist[0]; ++idx){
    // console.log('===> ',devicelist[0],devicelist[1][idx]);
  }
  let userlist = await deviceFactoryDeployer.getUserList(0);
  console.log('userlist len ===> ',userlist[0]);
  for(let idx = 0;idx < userlist[0]; ++idx){
    // console.log('===> ',userlist[0],userlist[1][idx]);
  }
  console.log("deployMaster done");
  await sleep(5)
}
async function main() {
  await deployDeviceFactory();
  await AddDeviceMaster();
}

// We recommend this pattern to be able to use async/await everywhere
// and properly handle errors.
main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});