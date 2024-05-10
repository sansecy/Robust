import subprocess

# 更改系统代码页为utf-8
# subprocess.run(['chcp', '65001'])

# 运行tasklist命令获取所有进程列表
tasklist_output = subprocess.check_output(['tasklist'])

# 使用默认系统编码解码输出
tasklist_output = tasklist_output.decode(encoding="GBK")

# 查找包含"java"的行并提取PID
# java_processes = [line.split()[1] for line in tasklist_output.split('\n') if 'java' in line.lower()]
# Extract Java process PIDs
java_processes = []
for line in tasklist_output.split('\n'):
#   print(line)
  if 'java' in line.lower():
    java_processes.append(line.split()[1])
print("java_processes_size=",len(java_processes))
# 如果找到了Java相关的进程，则终止相应进程
if java_processes:
    print("正在终止Java相关的进程...")
    for pid in java_processes:
        subprocess.run(['taskkill', '/F', '/PID', pid], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        print(f"进程 {pid} 已终止。")
else:
        print("未找到与Java相关的进程。")

# 恢复系统代码页为gbk
# subprocess.run(['chcp', '437'])
