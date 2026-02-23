# ReverseShellServer
安卓 反弹Shell

> 我使用`Aide Pro`构建，其他应该也可以

本项目试用于拥有发送广播权限[^广播权限]，而无网络权限[^网络权限]的反弹Shell获取，及特殊环境下

## 原理
`ReverseShellLoader` : 负责实际命令的执行，通过`Binder`与应用通讯  
`ReverseShellServer` : 负责创建服务器中转命令与结果，通过连接`4446`端口使用反弹Shell  
`ReverseShellBinder` : 没用、占位，但占了个寂寞  
`ReverseShellHander` : 处理广播、中转  
`ReverseShellReceiver` : 接受广播  
`Shell` : 执行命令  

1. 目标权限运行`ReverseShellLoader`
2. `ReverseShellLoader`通过广播`Binder`与`App`通信，5s超时重连
3. `ReverseShellHandler`等待从`KeyedBlockingQueue`取出命令并回传
4. `App`创建服务器并绑定端口`4446`
5. `ReverseShellServer`接受到命令后放入`KeyedBlockingQueue`
6. `ReverseShellHandler`通过`Binder`回传给`ReverseShellLoader`
7. `ReverseShellLoader`执行命令并通过`Binder`回传结果
8. `ReverseShellHandler`拿到结果放入`BlockingQueue`中
9. `ReverseShellServer`取出结果并返回客户端

> 所有都是单线程堵塞执行的，如果`ReverseShellLoader`被某一命令长期卡住，可创建`/data/local/tmp/RELOAD`重启

## 使用教程
1. 构建并启动应用
2. 在目标权限运行 `ReverseShellLoader` 
    ```bash
    /system/bin/app_process -Djava.class.path="从应用复制出来的classes.dex路径" /system/bin com.example.reverseshell.ReverseShellLoader 
    ```
3. 连接端口`4446`
> 每次执行完命令并获取结果后需重连才能执行下一个命令

反弹Shell客户端脚本（一个简单的例子，必须有`nc`命令，`termux`版本）
```bash
#!/data/data/com.termux/files/usr/bin/bash
# ANSI 颜色代码定义
RESET='\033[0m'       # 重置颜色
RED='\033[0;31m'      # 红色
GREEN='\033[0;32m'    # 绿色
YELLOW='\033[0;33m'   # 黄色
BLUE='\033[0;34m'     # 蓝色
PURPLE='\033[0;35m'   # 紫色
CYAN='\033[0;36m'     # 青色
WHITE='\033[0;37m'    # 白色

# 定义历史记录文件（可自行指定路径）
HISTFILE=~/.remote_shell_history
# 设置历史记录大小
HISTSIZE=1000
HISTFILESIZE=2000

# 读取上一次会话的历史记录（如果有）
if [ -f "$HISTFILE" ]; then
    history -r "$HISTFILE"
fi

ip=127.0.0.1
port=4446

# 读取用户名

while true
do
    # 使用 -e 开启 Readline，-i 设置默认提示
    user=$(echo USER | nc $ip $port)
    prompt=$(echo -e "\033[1m\033[38;5;196m$user@localhost>\033[0;37m")
    read -e -p "$prompt " command

    # 如果命令为空，继续下一轮
    [ -z "$command" ] && continue

    # 将命令发送到远程
    echo "$command" | nc $ip $port

    # 将命令追加到历史记录中
    # 1. 添加到当前会话的历史
    history -s "$command"
    # 2. 立即写入历史文件
    history -a
done
```

[^广播权限]: 如 `Shell(uid:2000)`, `System(uid:1000)`等  
[^网络权限]: 组权限有 `inet(3003)` 权限