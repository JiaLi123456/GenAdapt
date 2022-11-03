
DIR="~/PATH"
LOG="LOG"

# Internet sender 1
bash -i -l -c 'sshpass -p 000000  \
ssh -oStrictHostKeyChecking=no user@10.0.0.1 \
"cd '$DIR' && \
D-ITG-2.8.1-r1023/bin/ITGSend -s 0.5 -t 30000 -a 10.0.0.17 -rp 10005 -E 3800 -e 1250 -T UDP -l '$DIR'/D-ITG-2.8.1-r1023/bin/1" &'
# Internet sender 2
bash -i -l -c 'sshpass -p 000000  \
ssh -oStrictHostKeyChecking=no user@10.0.0.2 \
"cd '$DIR' && \
D-ITG-2.8.1-r1023/bin/ITGSend -s 0.5 -t 30000 -a 10.0.0.17 -rp 10005 -E 3800 -e 1250 -T UDP -l '$DIR'/D-ITG-2.8.1-r1023/bin/2" &'

sleep 10

# Internet sender 3
bash -i -l -c 'sshpass -p 000000  \
ssh -oStrictHostKeyChecking=no user@10.0.0.3 \
"cd '$DIR' && \
D-ITG-2.8.1-r1023/bin/ITGSend -s 0.5 -t 20000 -a 10.0.0.17 -rp 10005 -E 3800 -e 1250 -T UDP -l '$DIR'/D-ITG-2.8.1-r1023/bin/3" &'
# Internet sender 4
bash -i -l -c 'sshpass -p 000000  \
ssh -oStrictHostKeyChecking=no user@10.0.0.4 \
"cd '$DIR' && \
D-ITG-2.8.1-r1023/bin/ITGSend -s 0.5 -t 20000 -a 10.0.0.17 -rp 10005 -E 3800 -e 1250 -T UDP -l '$DIR'/D-ITG-2.8.1-r1023/bin/4" &'

sleep 10

# Internet sender 5
bash -i -l -c 'sshpass -p 000000  \
ssh -oStrictHostKeyChecking=no user@10.0.0.5 \
"cd '$DIR' && \
D-ITG-2.8.1-r1023/bin/ITGSend -s 0.5 -t 10000 -a 10.0.0.17 -rp 10005 -E 3800 -e 1250 -T UDP -l '$DIR'/D-ITG-2.8.1-r1023/bin/5" &'
# Internet sender 6
bash -i -l -c 'sshpass -p 000000  \
ssh -oStrictHostKeyChecking=no user@10.0.0.6 \
"cd '$DIR' && \
D-ITG-2.8.1-r1023/bin/ITGSend -s 0.5 -t 10000 -a 10.0.0.17 -rp 10005 -E 3800 -e 1250 -T UDP -l '$DIR'/D-ITG-2.8.1-r1023/bin/6" &'
