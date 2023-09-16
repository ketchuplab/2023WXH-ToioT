echo  ========== update oracle abi files ========
set SOURCE_CONTRACT_PATH=.\Contract\artifacts\contracts

set TARGET_CONTRACT_PATH=.\pyService\contracts
del %TARGET_CONTRACT_PATH%\DeviceFactory.sol\DeviceFactory.json 
xcopy %SOURCE_CONTRACT_PATH%\DeviceFactory.sol\DeviceFactory.json   %TARGET_CONTRACT_PATH%\DeviceFactory.sol\ 


set TARGET_CONTRACT_PATH=.\Web
del %TARGET_CONTRACT_PATH%\DeviceFactory.sol\DeviceFactory.json 
xcopy %SOURCE_CONTRACT_PATH%\DeviceFactory.sol\DeviceFactory.json   %TARGET_CONTRACT_PATH%\DeviceFactory.sol\ 


pause