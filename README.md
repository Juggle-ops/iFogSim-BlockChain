# iFogSim
A Toolkit for Modeling and Simulation of Resource Management Techniques in Internet of Things, Edge and Fog Computing Environments

# Attention: iFogSim2 new Release 
The new version of iFogSim (i.e., iFogSim2) can be accessed from <A href="https://github.com/Cloudslab/iFogSim">iFogSim2</A>

# iFogSim Tutorial Examples
 Access from <A href="https://github.com/Cloudslab/iFogSimTutorials">https://github.com/Cloudslab/iFogSimTutorials</A>

## IMPORTANT
Please check the `improv` branch for latest changes. Master branch has been left intact until complete testing.

## How to run iFogSim ?

### 环境要求
- Java 17 或更高版本
- Maven 3.6+

### 使用Maven构建和运行

1. 编译项目
```bash
mvn clean compile
```

2. 运行测试
```bash
mvn test
```

3. 打包项目
```bash
mvn package
```

4. 运行区块链节点测试
```bash
mvn test -Dtest=BlockchainNodeTest
```

5. 运行iFogSim示例
```bash
# 编译后，在IDE中运行以下示例类：
# - org.fog.test.perfeval.VRGameFog
# - org.fog.test.perfeval.DCNSFog
# - org.fog.test.perfeval.TwoApps
```

### 使用IDE运行（Eclipse/IntelliJ IDEA）

1. 导入Maven项目到IDE
2. 等待Maven依赖下载完成
3. 直接运行测试类或示例类

### 区块链功能使用示例

```java
// 创建账本和共识模块
Ledger ledger = new Ledger();
ConsensusModule consensusModule = new ConsensusModule(ledger, null);

// 创建区块链节点
List<BlockchainNode> neighbors = new ArrayList<>();
BlockchainNode node = new BlockchainNode("node1", ledger, consensusModule, neighbors);

// 启动节点（生成密钥对）
node.start();

// 创建并签名交易
Transaction transaction = new Transaction("sender", "receiver", 100.0);
transaction.sign(node.getPrivateKey(), node.getPublicKey());

// 广播交易
node.broadcastTransaction(transaction);
```

### 原始Eclipse方式（传统方法）

* Create a Java project in Eclipse. 
* Inside the project directory, initialize an empty Git repository with the following command
```
git init
```
* Add the Git repository of iFogSim as the `origin` remote.
```
git remote add origin https://github.com/Cloudslab/iFogSim
```
* Pull the contents of the repository to your machine.
```
git pull origin master
```
* Include the JARs (except the CloudSim ones) to your Eclipse project.  
* Run the example files (e.g. VRGame.java) to get started. 

# References
1. Harshit Gupta, Amir Vahid Dastjerdi , Soumya K. Ghosh, and Rajkumar Buyya, <A href="http://www.buyya.com/papers/iFogSim.pdf">iFogSim: A Toolkit for Modeling and Simulation of Resource Management Techniques in Internet of Things, Edge and Fog Computing Environments</A>, Software: Practice and Experience (SPE), Volume 47, Issue 9, Pages: 1275-1296, ISSN: 0038-0644, Wiley Press, New York, USA, September 2017.

2. Redowan Mahmud and Rajkumar Buyya, <A href="http://www.buyya.com/papers/iFogSim-Tut.pdf">Modelling and Simulation of Fog and Edge Computing Environments using iFogSim Toolkit</A>, Fog and Edge Computing: Principles and Paradigms, R. Buyya and S. Srirama (eds), 433-466pp, ISBN: 978-111-95-2498-4, Wiley Press, New York, USA, January 2019.

