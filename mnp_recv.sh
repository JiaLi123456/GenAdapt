
DIR="~/PATH"
LOG="LOG"

# Internet receiver 1
bash -i -l -c 'sshpass -p 000000  \
ssh -oStrictHostKeyChecking=no user@10.0.0.17 \
"cd '$DIR' && \
D-ITG-2.8.1-r1023/bin/ITGRecv -l '$DIR'/D-ITG-2.8.1-r1023/bin/LOG1" &'
