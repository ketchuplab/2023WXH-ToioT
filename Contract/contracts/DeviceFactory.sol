// SPDX-License-Identifier: MIT
pragma solidity ^0.8.4; 
import "@openzeppelin/contracts/utils/Strings.sol";
import "@openzeppelin/contracts/utils/Base64.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "hardhat/console.sol";

contract DeviceFactory is Ownable{
    struct DeviceInfo {
        string macAddr;
        address owner;  // 所属商家
        uint256 tokenBalance; // 里面存款
        uint256 regTime;  // 注册时间
        string merchant;       // 商家字符串
        string longitude;     // 经度（整数或浮点数，根据需求选择合适的数据类型）
        string latitude;      // 维度（整数或浮点数，根据需求选择合适的数据类型）
        bool bValid;   // 是否有效
    }
    struct UserInfo {
        address owner;
        uint256 tokenBalance;
        uint256 lastTransferTime;
    }
    mapping(string => DeviceInfo) public deviceDic;
    mapping(address => UserInfo) public userTokenDic;
    
    string [] deviceList;
    address [] userList;

    uint256 public constant TOKEN_AMOUNT = 0.05 ether;
    uint256 public constant TRANSFER_INTERVAL = 1 seconds;
 
    event onDeviceRegisterSuccess(DeviceInfo devInfo);
    event onReqTokenSuccess(DeviceInfo devInfo,UserInfo userInfo,uint256 tokenAmount);

    function registerDevice(string memory macAddress,string memory mcName, string memory longTD,string memory latTD, uint256 initialTokens,address to) public payable onlyOwner {
        require(deviceDic[macAddress].owner == address(0), "Device already registered");
        require(initialTokens > msg.value, "Value not enough");
        
        deviceDic[macAddress] = DeviceInfo(macAddress,to, initialTokens,block.timestamp,mcName,longTD,latTD,true);
        deviceList.push(macAddress);
        emit onDeviceRegisterSuccess(deviceDic[macAddress]);
    }

    function transferTokens(string memory macAddress, address to) public onlyOwner  {
        
        if(userTokenDic[to].owner == address(0)){
            userTokenDic[to] = UserInfo(to, 0,0);
            userList.push(to);
        }
        require(block.timestamp >= userTokenDic[to].lastTransferTime + TRANSFER_INTERVAL, "Transfer interval not reached");
        
        require(deviceDic[macAddress].tokenBalance >= TOKEN_AMOUNT, "Insufficient tokens");

        deviceDic[macAddress].tokenBalance -= TOKEN_AMOUNT;
        // Transfer tokens to the specified wallet address
        // You need to implement the token transfer mechanism here
        userTokenDic[to].tokenBalance += TOKEN_AMOUNT; 
        userTokenDic[to].lastTransferTime = block.timestamp;
        emit onReqTokenSuccess(deviceDic[macAddress],userTokenDic[to],TOKEN_AMOUNT);
    }

    function withdrawTokens() public {
        uint256 tokens = userTokenDic[msg.sender].tokenBalance;
        require(tokens > 0, "No tokens to withdraw");

        // Transfer tokens to the sender's wallet address
        // You need to implement the token transfer mechanism here
        
        userTokenDic[msg.sender].tokenBalance = 0;
        (bool sent, ) = msg.sender.call{value: userTokenDic[msg.sender].tokenBalance}("");
    }
     
    function getDevice(string memory macAddress) public view returns (DeviceInfo memory) {
        return deviceDic[macAddress];
    }
     
    function getUser(address userAddr) public view returns (UserInfo memory) {
        return userTokenDic[userAddr];
    }
           // each can get 10 items
    function getDeviceList(uint256 startIdx) public view returns (uint256,DeviceInfo[]  memory) {
        //require(startIdx < whiteUserList.length,'invalid index');
         
        uint256 endIdx = startIdx+10; 
        endIdx = endIdx > deviceList.length ?  deviceList.length : endIdx;

        require(startIdx <= endIdx,'invalid index');
        DeviceInfo[] memory tempList = new DeviceInfo[](endIdx-startIdx);

        for(uint256 idx = startIdx;idx < endIdx; ++idx){ 
            tempList[idx-startIdx] = deviceDic[deviceList[idx]];
        } 
        return (deviceList.length,tempList);
    }
           // each can get 10 items
    function getUserList(uint256 startIdx) public view returns (uint256,UserInfo[]  memory) {
        
        uint256 endIdx = startIdx+10; 
        endIdx = endIdx > userList.length ?  userList.length : endIdx;

        require(startIdx <= endIdx,'invalid index');
        UserInfo[] memory tempList = new UserInfo[](endIdx-startIdx);

        for(uint256 idx = startIdx;idx < endIdx; ++idx){ 
            tempList[idx-startIdx] = userTokenDic[userList[idx]];
        } 
        return (userList.length,tempList);
    }
}
