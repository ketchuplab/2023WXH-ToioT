require("dotenv").config()
require("@nomicfoundation/hardhat-toolbox");
require("@nomiclabs/hardhat-ethers");
//最新版本已经废弃 参考： https://hardhat.org/hardhat-chai-matchers/docs/migrate-from-waffle
//require("@nomiclabs/hardhat-waffle"); 
require("@nomiclabs/hardhat-etherscan");
require("hardhat-gas-reporter");
 
/** @type import('hardhat/config').HardhatUserConfig */

// 测试task
task("balance", "Prints an account's balance")
  .addParam("account", "The account's address")
  .setAction(async (taskArgs) => {
    const balance = await ethers.provider.getBalance(taskArgs.account);
    console.log(ethers.utils.formatEther(balance), "ETH");
  });
  
// 你的Alchemy  Goerli网络配置
const { ALCHEMY_GOERLI_API_URL, ALCHEMY_GOERLI_PRIVATE_KEY } = process.env;
// 你的Alchemy  main 网络配置
const { ALCHEMY_MAINNET_API_URL, ALCHEMY_MAINNET_PRIVATE_KEY } = process.env;
// 本地测试网络配置
const { LOCAL_API_URL, LOCAL_PRIVATE_KEY  } = process.env;

module.exports = {
  solidity: "0.8.9",
  defaultNetwork: "localhost",   //选择默认网络
  networks: {
    goerli: {
      url: ALCHEMY_GOERLI_API_URL,
      accounts: [ALCHEMY_GOERLI_PRIVATE_KEY]
    },
    mainnet: {
      url: ALCHEMY_MAINNET_API_URL,
      accounts: [ALCHEMY_MAINNET_PRIVATE_KEY]
    },
    localhost: {
      url: LOCAL_API_URL,
      accounts: [LOCAL_PRIVATE_KEY]
    },
  },
  gasReporter: {
    currency: 'RMB',
    gasPrice: 21,
    enabled: (process.env.REPORT_GAS) ? true : false
  }
};
